package de.uka.ilkd.key.proof.retracing;

import de.uka.ilkd.key.java.ast.expression.literal.IntLiteral;
import de.uka.ilkd.key.rule.TacletApp;
import de.uka.ilkd.key.rule.retracing.AdvanceTraceInformation;
import de.uka.ilkd.key.rule.retracing.RetracingTaclet;
import org.key_project.prover.rules.RuleApp;
import org.key_project.util.collection.ImmutableList;

public class TracingState {

    private ImmutableList<TraceElement> traceElements;

    private final FunctionDatabase functionDatabase;

    private long currentCatchCount = 0;

    private Long lastConsumedCatchIndex = null;

    private ImmutableList<TraceElement> traceElementsForCurrentRuleApp = ImmutableList.of();

    public TracingState(ImmutableList<TraceElement> traceElements, FunctionDatabase functionDatabase) {
        this.traceElements = traceElements;
        this.functionDatabase = functionDatabase;
    }

    public TracingState(TracingState other) {
        this.traceElements = ImmutableList.fromList(other.traceElements);
        this.functionDatabase = other.functionDatabase;
        this.currentCatchCount = other.currentCatchCount;
        this.lastConsumedCatchIndex = other.lastConsumedCatchIndex;
        this.traceElementsForCurrentRuleApp = other.traceElementsForCurrentRuleApp;
    }

    /**
     * Finds a consumed trace element of the specified type in the current rule application.
     *
     * @param type the type of the trace element to find
     * @return the found trace element
     * @throws IllegalStateException if no such trace element is found
     */
    public <T extends TraceElement> T findConsumedTraceElement(Class<T> type) {
        for (TraceElement elem : traceElementsForCurrentRuleApp) {
            if (type.isInstance(elem)) {
                return type.cast(elem);
            }
        }
        throw new IllegalStateException(
            "No " + type.getSimpleName() + " found in consumed trace elements: "
                + traceElementsForCurrentRuleApp);
    }

    /**
     * Returns the index of the switch case that was taken, based on the trace elements of the current rule application.
     * It skips the first trace element which represents the NPE check.
     */
    public int getCurrentSwitchCaseIndex() {
        ImmutableList<TraceElement> switchBits = traceElementsForCurrentRuleApp.tail();
        int caseIndex = 0;
        for (int i = 0; i < TraceElement.SWITCH_ELEMENT_COUNT; i++) {
            TraceElement bit = switchBits.get(i);
            if (bit instanceof TraceElement.If) {
                caseIndex |= (1 << (TraceElement.SWITCH_ELEMENT_COUNT - 1 - i));
            }
        }
        return caseIndex;
    }

    public void updateTraceForApplication(RuleApp ruleApp) {
        if (!(ruleApp instanceof TacletApp tacletApp) || !(tacletApp.taclet() instanceof RetracingTaclet retracingTaclet)) {
            return;
        }
        AdvanceTraceInformation advanceTraceInformation = retracingTaclet.getAdvanceTraceInformation();

        if (advanceTraceInformation == null) {
            return;
        }

        traceElementsForCurrentRuleApp = traceElements.take(advanceTraceInformation.traceElementsCount());
        traceElements = traceElements.skip(advanceTraceInformation.traceElementsCount());

        if (advanceTraceInformation.catchCountVar() != null) {
            IntLiteral catchCount = tacletApp.matchConditions().getInstantiations().getInstantiation(advanceTraceInformation.catchCountVar());
            currentCatchCount += catchCount.getValue();
        }

        for (TraceElement elem : traceElementsForCurrentRuleApp) {
            if (elem instanceof TraceElement.Catch(long catchIndex)) {
                if (isLastConsumedCatchIndexSet()) {
                    throw new IllegalStateException("Last consumed catch index is already set to " + lastConsumedCatchIndex + ", but found another catch element with index " + catchIndex);
                }
                lastConsumedCatchIndex = catchIndex;
            }
        }
    }

    public boolean matchesPattern(String tracePattern) {

        if (tracePattern.equals("*")) {
            return true;
        }

        ImmutableList<TraceElement> elements = traceElements;

        boolean negated = false;

        if (tracePattern.startsWith("!")) {
            negated = true;
            tracePattern = tracePattern.substring(1);
        }

        if (elements.size() < tracePattern.length()) {
            return negated;
        }

        for (char c : tracePattern.toCharArray()) {

            // handle special switch pattern
            if (c == 'S') {
                if (elements.size() < TraceElement.SWITCH_ELEMENT_COUNT) {
                    return negated;
                }

                for (TraceElement elem : elements.take(TraceElement.SWITCH_ELEMENT_COUNT)) {
                    if (!elem.isIfOrElse()) {
                        return negated;
                    }
                }

                elements = elements.skip(TraceElement.SWITCH_ELEMENT_COUNT);
                continue;
            }

            TraceElement elem = elements.head();
            switch (c) {
                case 'I' -> { if (!(elem instanceof TraceElement.If)) return negated; }
                case 'O' -> { if (!(elem instanceof TraceElement.Else)) return negated; }
                case 'C' -> { if (!(elem instanceof TraceElement.Call)) return negated; }
                case 'T' -> { if (!(elem instanceof TraceElement.Try)) return negated; }
                case 'J' -> { if (!(elem instanceof TraceElement.Catch)) return negated; }
                case 'E' -> { if (!(elem instanceof TraceElement.End)) return negated; }
                default -> throw new IllegalStateException(
                        "Invalid trace pattern character: " + c + " in pattern " + tracePattern);
            }
            elements = elements.tail();
        }
        return !negated;
    }

    public long getCurrentCatchCount() {
        return currentCatchCount;
    }

    public long getLastConsumedCatchIndex() {
        return lastConsumedCatchIndex;
    }

    public boolean isLastConsumedCatchIndexSet() {
        return lastConsumedCatchIndex != null;
    }

    public void clearLastConsumedCatchIndex() {
        lastConsumedCatchIndex = null;
    }

    public ImmutableList<TraceElement> getTraceElements() {
        return traceElements;
    }

    public FunctionDatabase getFunctionDatabase() {
        return functionDatabase;
    }
}
