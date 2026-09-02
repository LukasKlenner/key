package de.uka.ilkd.key.rule.conditions;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.rule.VariableConditionAdapter;
import de.uka.ilkd.key.rule.inst.SVInstantiations;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.sv.SchemaVariable;

public class IsAtEndOfTrace extends VariableConditionAdapter {

    private final boolean negated;

    public IsAtEndOfTrace(SchemaVariable var, boolean negated) {
        this.negated = negated;
    }

    @Override
    public boolean check(SchemaVariable var, SyntaxElement instCandidate, SVInstantiations instMap,
                         Services services) {
        return negated ^ (services.getTracingState().isAtEndOfTrace());
    }

    @Override
    public String toString() {
        String prefix = negated ? "\\not" : "";
        return prefix + "\\isAtEndOfTrace";
    }
}