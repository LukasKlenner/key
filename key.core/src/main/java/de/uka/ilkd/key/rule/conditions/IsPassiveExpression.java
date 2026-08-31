package de.uka.ilkd.key.rule.conditions;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.ast.abstraction.Type;
import de.uka.ilkd.key.java.ast.declaration.ClassDeclaration;
import de.uka.ilkd.key.java.ast.expression.PassiveExpression;
import de.uka.ilkd.key.java.ast.reference.ExecutionContext;
import de.uka.ilkd.key.logic.op.IProgramMethod;
import de.uka.ilkd.key.rule.VariableConditionAdapter;
import de.uka.ilkd.key.rule.inst.SVInstantiations;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.sv.SchemaVariable;

public class IsPassiveExpression extends VariableConditionAdapter {

    private final boolean negated;

    private final SchemaVariable expr;

    public IsPassiveExpression(SchemaVariable expr, boolean negated) {
        this.negated = negated;
        this.expr = expr;
    }

    public boolean isNegated() {
        return negated;
    }

    @Override
    public boolean check(SchemaVariable var, SyntaxElement instCandidate, SVInstantiations instMap,
                         Services services) {

        return negated ^ (instCandidate instanceof PassiveExpression);
//        ExecutionContext ec = instMap.getExecutionContext();
//
//        if (ec == null) {
//            return negated;
//        } else {
//            IProgramMethod methodContext = ec.getMethodContext();
//            boolean strictfpClass = true;
//
//            try {
//                Type t = ec.getTypeReference().getKeYJavaType().getJavaType();
//                if (t instanceof ClassDeclaration) {
//                    strictfpClass = ((ClassDeclaration) t).isStrictFp();
//                } else {
//                    strictfpClass = false;
//                }
//            } catch (NullPointerException e) {
//                strictfpClass = false;
//            }
//
//            final boolean isInStrictFp = strictfpClass || methodContext.isStrictFp();
//            return negated != isInStrictFp;
//        }
    }

    @Override
    public String toString() {
        String prefix = negated ? "\\not" : "";
        return prefix + "\\isPassiveExpression";
    }
}
