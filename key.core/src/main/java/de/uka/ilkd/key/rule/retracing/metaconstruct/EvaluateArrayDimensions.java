package de.uka.ilkd.key.rule.retracing.metaconstruct;

import de.uka.ilkd.key.java.KeYJavaASTFactory;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.ast.ProgramElement;
import de.uka.ilkd.key.java.ast.Statement;
import de.uka.ilkd.key.java.ast.abstraction.KeYJavaType;
import de.uka.ilkd.key.java.ast.expression.Expression;
import de.uka.ilkd.key.java.ast.expression.PassiveExpression;
import de.uka.ilkd.key.java.ast.expression.operator.CopyAssignment;
import de.uka.ilkd.key.java.ast.expression.operator.NewArray;
import de.uka.ilkd.key.java.ast.reference.ExecutionContext;
import de.uka.ilkd.key.logic.ProgramElementName;
import de.uka.ilkd.key.logic.VariableNamer;
import de.uka.ilkd.key.logic.op.ProgramVariable;
import de.uka.ilkd.key.rule.inst.SVInstantiations;
import de.uka.ilkd.key.rule.metaconstruct.ProgramTransformer;
import org.key_project.util.collection.ImmutableArray;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EvaluateArrayDimensions extends ProgramTransformer {

    public EvaluateArrayDimensions(ProgramElement body) {
        super("#evaluate-array-dimensions", body);
    }

    @Override
    public ProgramElement[] transform(ProgramElement pe, Services services,
                                      SVInstantiations svInst) {
        final ExecutionContext ec = svInst.getExecutionContext();
        final VariableNamer varNamer = services.getVariableNamer();

        if (!(pe instanceof CopyAssignment assignment)) {
            throw new IllegalStateException("Array dimensions can only be evaluated in the context of a CopyAssignment.");
        }

        final NewArray newArray = (NewArray) assignment.getChildAt(1);

        if (newArray.getArrayInitializer() != null) {
            // nothing to do
            return new ProgramElement[]{
                    KeYJavaASTFactory.assign(assignment.getExpressionAt(0), KeYJavaASTFactory.passiveExpression(newArray))
            };
        }

        final List<Statement> stmts = new ArrayList<>();

        // evaluate each dimension expression into a fresh local variable
        final ImmutableArray<Expression> dimExprs = newArray.getArguments();
        final ProgramVariable[] dimVars = new ProgramVariable[dimExprs.size()];
        for (int i = 0; i < dimExprs.size(); i++) {
            Expression dimeExpression = dimExprs.get(i);
            KeYJavaType dimType = dimeExpression.getKeYJavaType(services, ec);
            ProgramElementName name = varNamer.getTemporaryNameProposal("dim" + i);
            ProgramVariable pv = KeYJavaASTFactory.localVariable(name, dimType);

            stmts.add(KeYJavaASTFactory.declare(pv, dimeExpression, dimType));
            dimVars[i] = pv;
        }

        // if (dimVar < 0) throw new NegativeArraySizeException() for each dim
        final KeYJavaType nasType = services.getJavaInfo()
                .getKeYJavaType("java.lang.NegativeArraySizeException");
        for (ProgramVariable dimVar : dimVars) {
            final Expression negCheck =
                    KeYJavaASTFactory.lessThanZeroOperator(dimVar);
            final Statement throwNas = KeYJavaASTFactory.throwClause(
                    KeYJavaASTFactory.newOperator(nasType));
            stmts.add(KeYJavaASTFactory.ifThen(negCheck, throwNas));
        }

        // create the new array expression with the evaluated dimensions
        final PassiveExpression resultArray = KeYJavaASTFactory.passiveExpression(KeYJavaASTFactory.newArray(
                newArray.getTypeReference(),
                newArray.getDimensions(),
                dimVars,
                newArray.getKeYJavaType()
        ));

        // assemble final block
        final Statement[] res = new Statement[stmts.size() + 1];
        final Iterator<Statement> it = stmts.iterator();
        for (int i = 0; i < stmts.size(); i++) {
            res[i] = it.next();
        }
        res[res.length - 1] =
                KeYJavaASTFactory.assign(assignment.getExpressionAt(0), resultArray);

        return new ProgramElement[]{KeYJavaASTFactory.block(res)};
    }

}
