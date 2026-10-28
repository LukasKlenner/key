package de.uka.ilkd.key.rule.retracing.metaconstruct;

import de.uka.ilkd.key.java.KeYJavaASTFactory;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.ast.ProgramElement;
import de.uka.ilkd.key.java.ast.Statement;
import de.uka.ilkd.key.java.ast.StatementBlock;
import de.uka.ilkd.key.java.ast.declaration.LocalVariableDeclaration;
import de.uka.ilkd.key.java.ast.declaration.ParameterDeclaration;
import de.uka.ilkd.key.java.ast.declaration.VariableSpecification;
import de.uka.ilkd.key.java.ast.expression.Assignment;
import de.uka.ilkd.key.java.ast.expression.Expression;
import de.uka.ilkd.key.java.ast.statement.Catch;
import de.uka.ilkd.key.logic.op.ProgramSV;
import de.uka.ilkd.key.proof.retracing.TracingState;
import de.uka.ilkd.key.rule.inst.SVInstantiations;
import de.uka.ilkd.key.rule.metaconstruct.ProgramTransformer;
import org.key_project.logic.Name;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.WeakValueLinkedHashMap;

import java.util.Objects;

public class TracedThrowStatement extends ProgramTransformer {

    private final SchemaVariable catchListVar;
    private final SchemaVariable finallyBlockVar;

    private static final WeakValueLinkedHashMap<Expression, Long> propagatedExceptionCatchIndices = new WeakValueLinkedHashMap<>();

    public TracedThrowStatement(SchemaVariable catchListVar, SchemaVariable finallyBlockVar, ProgramSV ThrowExpressionVar) {
        super(new Name("traced-throw"), ThrowExpressionVar);
        this.catchListVar = catchListVar;
        this.finallyBlockVar = finallyBlockVar;
    }

    public TracedThrowStatement(SchemaVariable catchListVar, ProgramSV ThrowExpressionVar) {
        this(catchListVar, null, ThrowExpressionVar);
    }

    @Override
    public ProgramElement[] transform(ProgramElement pe, Services services, SVInstantiations svInst) {
        Expression throwExpr = (Expression) pe;

        TracingState tracingState = services.getTracingState();

        // get the last consumed catch index from the tracing state
        long catchIndex = tracingState.getLastConsumedCatchIndex();
        long currentCatchCount = tracingState.getCurrentCatchCount();

        ImmutableArray<ProgramElement> catchListInst = svInst.getInstantiation(catchListVar);
        int localCatchCount = catchListInst.size();
        int previousCatchCount = Math.toIntExact(currentCatchCount - localCatchCount);

        if (catchIndex < previousCatchCount) {
            // Either no catch trace element has been consumed yet, or the consumed belongs to a previously handled throw.
            // I.e., the throw statement is not caught by any catch block in the current catch list and an upcoming
            // finally block will consume the next trace element(s) first.

            // propagate the exception wrapped in a passiveExpression for the next catch list in the call stack
            ImmutableList<ProgramElement> replacement = ImmutableList.singleton(KeYJavaASTFactory.throwClause(KeYJavaASTFactory.passiveExpression(throwExpr)));

            // if there is a finally block, we need to execute it before rethrowing the exception
            return replacement.prepend(getFinallyBlock(svInst)).toArray(ProgramElement.class);
        }

        int localCatchIndex = Math.toIntExact(catchIndex - previousCatchCount);
        if (localCatchIndex < 0) {
            throw new IllegalStateException("Trace element catch index is out of bounds: " + localCatchIndex + " (catch count: " + localCatchCount + ")");
        }

        if (localCatchIndex >= localCatchCount) {
            // the throw statement is not caught by any of the catch blocks in the current catch list

            // store the catch index for the propagated exception to be used in the next catch list
            propagatedExceptionCatchIndices.put(throwExpr, catchIndex);

            // propagate the exception wrapped in a passiveExpression for the next catch list in the call stack
            ImmutableList<ProgramElement> replacement = ImmutableList.singleton(KeYJavaASTFactory.throwClause(KeYJavaASTFactory.passiveExpression(throwExpr)));

            // if there is a finally block, we need to execute it before rethrowing the exception
            replacement = replacement.prepend(getFinallyBlock(svInst));

            return replacement.toArray(ProgramElement.class);
        }

        // the throw statement is caught by one of the catch blocks in the current catch list

        Catch catchCase = (Catch) catchListInst.get(localCatchIndex);
        StatementBlock catchBody = catchCase.getBody();

        // store thrown exception in a variable mirroring catch parameter
        ParameterDeclaration catchParam = catchCase.getParameterDeclaration();
        VariableSpecification exceptionVarSpec = catchParam.getVariableSpecification();
        LocalVariableDeclaration exceptionDecl = new LocalVariableDeclaration(catchParam.getTypeReference(), catchParam.getVariableSpecification());
        Assignment<?> exceptionAssignment = KeYJavaASTFactory.assign((Expression) exceptionVarSpec.getProgramVariable(), throwExpr);

        ImmutableList<ProgramElement> replacement = ImmutableList.of(exceptionDecl, exceptionAssignment, catchBody);

        // if there is a finally block, we need to execute it after the catch block
        replacement = replacement.append(getFinallyBlock(svInst));

        return replacement.toArray(ProgramElement.class);
    }

    private ImmutableList<ProgramElement> getFinallyBlock(SVInstantiations svInst) {
        if (finallyBlockVar == null) {
            return ImmutableList.nil();
        }

        ImmutableArray<Statement> finallyBlockInst = svInst.getInstantiation(finallyBlockVar);
        return ImmutableList.of(KeYJavaASTFactory.block(finallyBlockInst.toArray(new Statement[0])));
    }


    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }

        return o instanceof TracedThrowStatement that
                && Objects.equals(this.catchListVar, that.catchListVar)
                && Objects.equals(this.finallyBlockVar, that.finallyBlockVar);
    }

}
