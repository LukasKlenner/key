package de.uka.ilkd.key.proof.retracing;

import de.uka.ilkd.key.java.ast.expression.literal.IntLiteral;
import de.uka.ilkd.key.rule.TacletApp;
import de.uka.ilkd.key.rule.retracing.RetracingTaclet;
import de.uka.ilkd.key.rule.retracing.advanceTrace.AdvanceCatchTraceInformation;
import de.uka.ilkd.key.rule.retracing.advanceTrace.AdvanceTraceInformation;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.RuleApp;
import org.key_project.util.collection.ImmutableList;

public class TracingState {

    private ImmutableList<TraceElement> traceElements;

    private final FunctionDatabase functionDatabase;

    private long currentCatchCount = 0;

    private Long lastConsumedCatchIndex = null;

    private ImmutableList<Integer> currentDimensions = ImmutableList.of();

    private boolean checkingArrayDimensions = false;

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
        this.currentDimensions = other.currentDimensions;
        this.traceElementsForCurrentRuleApp = other.traceElementsForCurrentRuleApp;
        this.checkingArrayDimensions = other.checkingArrayDimensions;
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

        // cleanup only at after the next rule application, because we still need the currentDimensions for the current array initialization
        if (!checkingArrayDimensions) {
            currentDimensions = ImmutableList.of();
        }

        int traceElementsCount = advanceTraceInformation.getTraceElementsConsumed();

        traceElementsForCurrentRuleApp = traceElements.take(traceElementsCount);
        traceElements = traceElements.skip(traceElementsCount);

        if (advanceTraceInformation instanceof AdvanceCatchTraceInformation(SchemaVariable catchCountVar, AdvanceTraceInformation additionalAdvanceTraceInformation)) {
            IntLiteral catchCount = tacletApp.matchConditions().getInstantiations().getInstantiation(catchCountVar);
            currentCatchCount += catchCount.getValue();
        }

        for (TraceElement elem : traceElementsForCurrentRuleApp) {
            switch (elem) {
                case TraceElement.Catch(long catchIndex) -> {
                    if (isLastConsumedCatchIndexSet()) {
                        throw new IllegalStateException("Last consumed catch index is already set to " + lastConsumedCatchIndex + ", but found another catch element with index " + catchIndex);
                    }
                    lastConsumedCatchIndex = catchIndex;
                }

                // Reset the array dimension checking flag when encountering an if or else statement
                // as they mark the check of the last dimension
                // we do not yet clear the currentDimensions list, because we still need to access the dimensions for the current array initialization
                case TraceElement.If() -> checkingArrayDimensions = false;
                case TraceElement.Else() -> checkingArrayDimensions = false;

                case TraceElement.Dim(int dimensions) -> {
                    checkingArrayDimensions = true;
                    currentDimensions = currentDimensions.append(dimensions);
                }
                default -> {
                }
            }
        }
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

    public int getCurrentDimension(int dimensionCount) {
        return currentDimensions.get(currentDimensions.size() + 1 - dimensionCount);
    }

}
