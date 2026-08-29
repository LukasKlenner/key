package de.uka.ilkd.key.rule.tracing;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.ast.ProgramElement;
import de.uka.ilkd.key.java.ast.abstraction.KeYJavaType;
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
import de.uka.ilkd.key.java.ast.statement.Throw;
import de.uka.ilkd.key.java.ast.statement.Try;
import de.uka.ilkd.key.logic.op.IProgramMethod;
import de.uka.ilkd.key.logic.op.ProgramVariable;
import de.uka.ilkd.key.logic.op.Transformer;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.tracing.TracingState;
import org.jspecify.annotations.Nullable;
import org.key_project.prover.sequent.PosInOccurrence;

public class TracingRuleUtil {

    public static @Nullable Throw getThrowStatement(Try tryStatement) {
        if (tryStatement == null) {
            return null;
        }

        if (tryStatement.getBody().isEmpty()) {
            return null;
        }

        ProgramElement firstStatement = tryStatement.getBody().getStatementAt(0);

        if (firstStatement instanceof PassiveExpression pe) {
            firstStatement = pe.getChildAt(0);
        }

        if (!(firstStatement instanceof Throw throwStmt)) {
            return null;
        }

        if (!(throwStmt.getExpressionAt(0) instanceof PassiveExpression)) {
            return null;
        }

        return throwStmt;
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
