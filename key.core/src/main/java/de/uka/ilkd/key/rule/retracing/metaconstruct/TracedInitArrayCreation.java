package de.uka.ilkd.key.rule.retracing.metaconstruct;

import de.uka.ilkd.key.java.KeYJavaASTFactory;
import de.uka.ilkd.key.java.ast.ProgramElement;
import de.uka.ilkd.key.java.ast.Statement;
import de.uka.ilkd.key.java.ast.abstraction.ArrayType;
import de.uka.ilkd.key.java.ast.abstraction.KeYJavaType;
import de.uka.ilkd.key.java.ast.expression.ArrayInitializer;
import de.uka.ilkd.key.java.ast.expression.Expression;
import de.uka.ilkd.key.java.ast.expression.operator.NewArray;
import de.uka.ilkd.key.java.ast.reference.ReferencePrefix;
import de.uka.ilkd.key.java.ast.reference.TypeReference;
import de.uka.ilkd.key.rule.metaconstruct.InitArrayCreation;
import de.uka.ilkd.key.util.Debug;
import org.key_project.logic.op.sv.SchemaVariable;

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

        return KeYJavaASTFactory.assign(lhs, KeYJavaASTFactory.passiveExpression(p_initializer));
    }
}
