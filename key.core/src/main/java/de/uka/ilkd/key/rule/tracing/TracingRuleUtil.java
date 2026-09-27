package de.uka.ilkd.key.rule.tracing;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.ast.ProgramElement;
import de.uka.ilkd.key.java.ast.SourceElement;
import de.uka.ilkd.key.java.ast.Statement;
import de.uka.ilkd.key.java.ast.StatementBlock;
import de.uka.ilkd.key.java.ast.abstraction.KeYJavaType;
import de.uka.ilkd.key.java.ast.expression.Expression;
import de.uka.ilkd.key.java.ast.expression.PassiveExpression;
import de.uka.ilkd.key.java.ast.reference.ArrayReference;
import de.uka.ilkd.key.java.ast.reference.ExecutionContext;
import de.uka.ilkd.key.java.ast.reference.FieldReference;
import de.uka.ilkd.key.java.ast.reference.MethodReference;
import de.uka.ilkd.key.java.ast.reference.ReferencePrefix;
import de.uka.ilkd.key.java.ast.reference.SuperReference;
import de.uka.ilkd.key.java.ast.reference.ThisReference;
import de.uka.ilkd.key.java.ast.reference.TypeRef;
import de.uka.ilkd.key.java.ast.reference.TypeReference;
import de.uka.ilkd.key.java.ast.statement.Break;
import de.uka.ilkd.key.java.ast.statement.CatchAllStatement;
import de.uka.ilkd.key.java.ast.statement.Continue;
import de.uka.ilkd.key.java.ast.statement.LabeledStatement;
import de.uka.ilkd.key.java.ast.statement.Return;
import de.uka.ilkd.key.java.ast.statement.Throw;
import de.uka.ilkd.key.java.ast.statement.Try;
import de.uka.ilkd.key.logic.JavaBlock;
import de.uka.ilkd.key.logic.ProgramPrefix;
import de.uka.ilkd.key.logic.op.IProgramMethod;
import de.uka.ilkd.key.logic.op.ProgramVariable;
import de.uka.ilkd.key.logic.op.Transformer;
import de.uka.ilkd.key.logic.sort.ProgramSVSort;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.retracing.TracingState;
import org.jspecify.annotations.Nullable;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.util.collection.Pair;

import static de.uka.ilkd.key.java.JavaTools.getActiveStatement;

public class TracingRuleUtil {

    // Modified version of JavaTools.getActiveStatement
    public static @Nullable Pair<Try, Integer> getInnermostTryStatement(JavaBlock jb) {
        assert jb.program() != null;

        SourceElement result = jb.program().getFirstElement();

        Try lastFoundTry = null;
        int catchCount = 0;

        while ((result instanceof ProgramPrefix || result instanceof CatchAllStatement)
                && !(result instanceof StatementBlock && ((StatementBlock) result).isEmpty())) {

            if (result instanceof Try tryStmt) {
                lastFoundTry = tryStmt;
                catchCount += tryStmt.getCatchCount();
            }

            if (result instanceof LabeledStatement) {
                result = ((LabeledStatement) result).getChildAt(1);
            } else if (result instanceof CatchAllStatement) {
                result = ((CatchAllStatement) result).getBody();
            } else {
                result = result.getFirstElement();
            }
        }

        if (lastFoundTry == null) {
            return null;
        }

        return new Pair<>(lastFoundTry, catchCount - lastFoundTry.getCatchCount());
    }

    public static @Nullable Statement getTryInterruptingStatement(Try tryStatement, Services services) {
        if (tryStatement == null) {
            return null;
        }

        if (tryStatement.getBody().isEmpty()) {
            return null;
        }

        SourceElement active = getActiveStatement(tryStatement);

        if (!(active instanceof Statement firstStatement)) {
            return null;
        }

        if (IsTryInterruptingStatement(firstStatement, services)) {
            return firstStatement;
        }

        return null;
    }

    public static boolean IsTryInterruptingStatement(ProgramElement statement, Services services) {
        return (statement instanceof Throw throwStmt && throwStmt.getExpressionAt(0) instanceof PassiveExpression) ||
                (statement instanceof Return returnStmt && isSimpleExpression(returnStmt.getExpression(), services)) ||
                statement instanceof Break ||
                statement instanceof Continue;
    }

    public static boolean isSimpleExpression(Expression expression, Services services) {
        return ProgramSVSort.SIMPLEEXPRESSION.canStandFor(expression, null, services);
    }

    public static TracingState getTracingState(Goal goal) {
        return getTracingState(goal.proof().getServices());
    }

    public static TracingState getTracingState(Services services) {
        TracingState tracingState = services.getTracingState();
        if (tracingState == null) {
            throw new IllegalStateException("TracingState is not available");
        }
        return tracingState;
    }

    public static boolean isPioApplicable(@Nullable PosInOccurrence pio) {
        return pio != null && pio.isTopLevel() && !pio.isInAntec() && !Transformer.inTransformer(pio);
    }

    public static boolean isUntracedMethod(IProgramMethod method) {
        return method.getName().startsWith("$") || method.isStatic() || method.isPrivate();
    }

    public static @Nullable IProgramMethod getStaticReferencedMethod(MethodReference methodRef, Services services, ExecutionContext executionContext) {
        KeYJavaType staticPrefixType = getStaticPrefixType(methodRef, services, executionContext);
        if (staticPrefixType == null) {
            return null;
        }
        return methodRef.method(services, staticPrefixType, executionContext);
    }

    /**
     * Determines the static prefix type from the reference prefix,
     * mirroring {@code MethodCall.getStaticPrefixType}.
     */
    public static @Nullable KeYJavaType getStaticPrefixType(
            MethodReference methodRef,
            Services services,
            ExecutionContext executionContext
    ) {
        ReferencePrefix refPrefix = methodRef.getReferencePrefix();

        if (refPrefix == null
                || (refPrefix instanceof ThisReference
                && refPrefix.getReferencePrefix() == null)) {
            return executionContext.getTypeReference().getKeYJavaType();
        } else if (refPrefix instanceof ThisReference) {
            return ((TypeReference) refPrefix.getReferencePrefix()).getKeYJavaType();
        } else if (refPrefix instanceof TypeRef tr) {
            return tr.getKeYJavaType();
        } else if (refPrefix instanceof ProgramVariable pv) {
            return pv.getKeYJavaType();
        } else if (refPrefix instanceof FieldReference fr) {
            return fr.getProgramVariable().getKeYJavaType();
        } else if (refPrefix instanceof ArrayReference ar) {
            return ar.getKeYJavaType(services, executionContext);
        } else if (refPrefix instanceof SuperReference) {
            return services.getJavaInfo().getSuperclass(
                    executionContext.getTypeReference().getKeYJavaType());
        } else {
            throw new IllegalArgumentException(
                    "Unsupported reference prefix: " + refPrefix.getClass());
        }
    }
}
