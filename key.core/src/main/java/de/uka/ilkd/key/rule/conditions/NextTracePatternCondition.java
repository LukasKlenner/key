package de.uka.ilkd.key.rule.conditions;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.proof.retracing.TraceElement;
import de.uka.ilkd.key.proof.retracing.TracingState;
import de.uka.ilkd.key.rule.VariableConditionAdapter;
import de.uka.ilkd.key.rule.inst.SVInstantiations;

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.util.collection.ImmutableList;

/**
 * Checks that the next trace elements match a pattern string of I/O characters.
 * E.g. "OO" matches Else,Else; "OI" matches Else,If; "I" matches If.
 * <p>
 * The special character 'S' matches a switch statement, which is represented by 6 trace elements (If/Else).
 */
public class NextTracePatternCondition extends VariableConditionAdapter {

    private final String pattern;
    private final boolean negated;

    public NextTracePatternCondition(SchemaVariable var, String pattern, boolean negated) {
        this.pattern = pattern;
        this.negated = negated;
    }

    @Override
    public boolean check(SchemaVariable var, SyntaxElement instCandidate, SVInstantiations instMap,
            Services services) {
        TracingState state = services.getTracingState();
        if (state == null) {
            return false;
        }
        return negated ^ matchesPattern(state);
    }

    private boolean matchesPattern(TracingState state) {
        ImmutableList<TraceElement> elements = state.getTraceElements();

        if (elements.size() < pattern.length()) {
            return false;
        }

        for (char c : pattern.toCharArray()) {

            // handle special switch pattern
            if (c == 'S') {
                if (elements.size() < TraceElement.SWITCH_ELEMENT_COUNT) {
                    return false;
                }

                for (TraceElement elem : elements.take(TraceElement.SWITCH_ELEMENT_COUNT)) {
                    if (!elem.isIfOrElse()) {
                        return false;
                    }
                }

                elements = elements.skip(TraceElement.SWITCH_ELEMENT_COUNT);
                continue;
            }

            TraceElement elem = elements.head();
            switch (c) {
                case 'I' -> { if (!(elem instanceof TraceElement.If)) return false; }
                case 'O' -> { if (!(elem instanceof TraceElement.Else)) return false; }
                case 'C' -> { if (!(elem instanceof TraceElement.Call)) return false; }
                case 'T' -> { if (!(elem instanceof TraceElement.Try)) return false; }
                case 'J' -> { if (!(elem instanceof TraceElement.Catch)) return false; }
                case 'E' -> { if (!(elem instanceof TraceElement.End)) return false; }
                default -> throw new IllegalStateException(
                        "Invalid trace pattern character: " + c + " in pattern " + pattern);
            }
            elements = elements.tail();
        }
        return true;
    }

    @Override
    public String toString() {
        return (negated ? "\\not " : "") + "\\nextTracePattern(" + pattern + ")";
    }
}
