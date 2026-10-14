package de.uka.ilkd.key.rule.retracing.metaconstruct;

import de.uka.ilkd.key.java.KeYJavaASTFactory;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.ast.ProgramElement;
import de.uka.ilkd.key.java.ast.Statement;
import de.uka.ilkd.key.java.ast.abstraction.ArrayType;
import de.uka.ilkd.key.java.ast.abstraction.KeYJavaType;
import de.uka.ilkd.key.java.ast.declaration.LocalVariableDeclaration;
import de.uka.ilkd.key.java.ast.expression.ArrayInitializer;
import de.uka.ilkd.key.java.ast.expression.Expression;
import de.uka.ilkd.key.java.ast.expression.operator.CopyAssignment;
import de.uka.ilkd.key.java.ast.expression.operator.NewArray;
import de.uka.ilkd.key.java.ast.reference.ReferencePrefix;
import de.uka.ilkd.key.java.ast.reference.TypeReference;
import de.uka.ilkd.key.logic.ProgramElementName;
import de.uka.ilkd.key.logic.VariableNamer;
import de.uka.ilkd.key.logic.op.ProgramVariable;
import de.uka.ilkd.key.proof.retracing.TracingState;
import de.uka.ilkd.key.rule.inst.SVInstantiations;
import de.uka.ilkd.key.rule.metaconstruct.InitArrayCreation;
import de.uka.ilkd.key.util.Debug;
import org.key_project.logic.op.sv.SchemaVariable;

import java.util.LinkedList;

public class TracedInitArrayCreation extends InitArrayCreation {

    public TracedInitArrayCreation(SchemaVariable newObjectSV, ProgramElement newExpr) {
        super(newObjectSV, newExpr);
    }

    @Override
    protected Expression createArrayCreation(NewArray p_creationExpression) {
        return KeYJavaASTFactory.passiveExpression(super.createArrayCreation(p_creationExpression));
    }

    @Override
    protected Statement createAssignment(ReferencePrefix p_array, int p_index, Expression p_initializer, KeYJavaType p_elementType, TypeReference p_baseType) {
        if (p_initializer instanceof ArrayInitializer) {
            Debug.assertTrue(p_elementType.getJavaType() instanceof ArrayType,
                    "Very strange are arrays of type ", p_elementType.getJavaType());

            p_initializer = KeYJavaASTFactory.newArray(p_baseType,
                    ((ArrayType) p_elementType.getJavaType()).getDimension(),
                    (ArrayInitializer) p_initializer, p_elementType);
        }

        Expression indexExpr = KeYJavaASTFactory.intLiteral(p_index);
        Expression lhs = KeYJavaASTFactory.arrayFieldAccess(p_array, indexExpr);

        return KeYJavaASTFactory.assign(KeYJavaASTFactory.passiveExpression(lhs), p_initializer);
    }

    @Override
    protected void createNDimensionalArray(LinkedList<Statement> bodyStmnts, Expression resultVar,
                                           KeYJavaType arrayType, ProgramVariable[] dimensions, Services services) {
        assert dimensions.length > 0;
        bodyStmnts.add(KeYJavaASTFactory.assign(resultVar,
                KeYJavaASTFactory.methodCall(arrayType, createArrayName, dimensions[0])));

        if (dimensions.length > 1) {
            VariableNamer varNamer = services.getVariableNamer();
            TracingState tracingState = services.getTracingState();
            int currentDimension = tracingState.getCurrentDimension(dimensions.length);

            Expression[] baseDim = new Expression[dimensions.length - 1];
            System.arraycopy(dimensions, 1, baseDim, 0, dimensions.length - 1);

            TypeReference baseTypeRef = ((ArrayType) arrayType.getJavaType()).getBaseType();
            final KeYJavaType baseType = baseTypeRef.getKeYJavaType();

            for (int i = 0; i < dimensions.length - 1; i++) {
                ArrayType at = (ArrayType) baseTypeRef.getKeYJavaType().getJavaType();
                baseTypeRef = at.getBaseType();
            }

            for (int i = 0; i < currentDimension; i++) {
                final ProgramElementName name = varNamer.getTemporaryNameProposal("subArray" + i);
                final LocalVariableDeclaration arrayDecl =
                        KeYJavaASTFactory.declare(
                                name,
                                KeYJavaASTFactory.passiveExpression(
                                        KeYJavaASTFactory.newArray(baseTypeRef, dimensions.length - 1, baseDim, baseType)
                                ),
                                baseType
                        );
                CopyAssignment arrayAssign = KeYJavaASTFactory.assign(
                        KeYJavaASTFactory.passiveExpression(KeYJavaASTFactory.arrayFieldAccess((ReferencePrefix) resultVar, KeYJavaASTFactory.intLiteral(i))),
                        (ProgramVariable) arrayDecl.getVariables().get(0).getProgramVariable()
                );
                bodyStmnts.add(arrayDecl);
                bodyStmnts.add(arrayAssign);
            }

        }
    }
}
