package de.uka.ilkd.key.rule.retracing.conditions;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.ast.reference.FieldReference;
import de.uka.ilkd.key.logic.ProgramElementName;
import de.uka.ilkd.key.logic.op.ProgramVariable;
import de.uka.ilkd.key.rule.VariableConditionAdapter;
import de.uka.ilkd.key.rule.inst.SVInstantiations;

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.sv.SchemaVariable;

public final class IsUntracedFieldCondition extends VariableConditionAdapter {

    private final SchemaVariable fieldVar;
    private final boolean negation;

    public IsUntracedFieldCondition(SchemaVariable fieldVar, boolean negation) {
        this.fieldVar = fieldVar;
        this.negation = negation;
    }

    @Override
    public boolean check(SchemaVariable var, SyntaxElement subst, SVInstantiations svInst,
            Services services) {
        if (var != fieldVar) {
            return true;
        }
        ProgramVariable attribute;
        if (subst instanceof FieldReference fieldReference) {
            attribute = fieldReference.getProgramVariable();
        } else if (subst instanceof ProgramVariable pv) {
            attribute = pv;
        } else {
            return !negation;
        }

        ProgramElementName name = (ProgramElementName) attribute.name();
        return negation ^ name.getProgramName().startsWith("$");
    }

    @Override
    public String toString() {
        return (negation ? "\\not " : "") + "\\isUntracedField(" + fieldVar + ")";
    }
}
