package de.uka.ilkd.key.proof.retracing;

import de.uka.ilkd.key.rule.TacletApp;
import org.key_project.prover.rules.RuleApp;
import org.key_project.util.collection.ImmutableList;

public class TracingState {

    private ImmutableList<TraceElement> traceElements;

    private final FunctionDatabase functionDatabase;

    private long currentCatchCount = 0;

    private ImmutableList<TraceElement> traceElementsForCurrentRuleApp = ImmutableList.of();

    public TracingState(ImmutableList<TraceElement> traceElements, FunctionDatabase functionDatabase) {
        this.traceElements = traceElements;
        this.functionDatabase = functionDatabase;
    }

    public TracingState(TracingState other) {
        this.traceElements = ImmutableList.fromList(other.traceElements);
        this.functionDatabase = other.functionDatabase;
        this.currentCatchCount = other.currentCatchCount;
        this.traceElementsForCurrentRuleApp = other.traceElementsForCurrentRuleApp;
    }

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

    public TraceElement getNextTraceElement() {
        return traceElements.get(0);
    }

    public void updateTraceForApplication(RuleApp ruleApp) {
        if (!(ruleApp instanceof TacletApp tacletApp)) {
            return;
        }
        int n = tacletApp.taclet().getAdvanceTraceCount();
        traceElementsForCurrentRuleApp = traceElements.take(n);
        traceElements = traceElements.skip(n);
    }

    public void continueTrace() {
        traceElements = traceElements.tail();
    }

    public long getCurrentCatchCount() {
        return currentCatchCount;
    }

    public void incrementCurrentCatchCount(long catchCount) {
        currentCatchCount += catchCount;
    }

    public ImmutableList<TraceElement> getTraceElements() {
        return traceElements;
    }

    public FunctionDatabase getFunctionDatabase() {
        return functionDatabase;
    }
}
