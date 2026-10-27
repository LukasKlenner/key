package de.uka.ilkd.key.proof.retracing;

import de.uka.ilkd.key.java.ast.expression.literal.IntLiteral;
import de.uka.ilkd.key.rule.TacletApp;
import de.uka.ilkd.key.rule.retracing.RetracingTaclet;
import de.uka.ilkd.key.rule.retracing.advanceTrace.AdvanceCatchTraceInformation;
import de.uka.ilkd.key.rule.retracing.advanceTrace.AdvanceTraceInformation;
import org.jspecify.annotations.NullMarked;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.RuleApp;
import org.key_project.util.collection.ImmutableList;

@NullMarked
public class TracingState {

    private final ImmutableList<TraceElement> traceElements;

    private final FunctionDatabase functionDatabase;

    /**
     * The number of catch blocks that have been encountered so far.
     */
    private final long currentCatchCount;

    /**
     * The index the last catch trace element that was consumed during any rule application.
     * Initially -1 to indicate that no catch trace element has been consumed yet.
     */
    private final long lastConsumedCatchIndex;

    /**
     * Indicates whether the previous trace element were array dimension trace elements, which means that the current
     * rule application is also checking array dimensions.
     */
    private final boolean checkingArrayDimensions;

    /**
     * A list of the previously encountered array dimensions.
     */
    private final ImmutableList<Integer> currentDimensions;

    /**
     * The trace element that have been consumed during the current rule application.
     */
    private final ImmutableList<TraceElement> traceElementsForCurrentRuleApp;

    public TracingState(
            ImmutableList<TraceElement> traceElements,
            FunctionDatabase functionDatabase,
            long currentCatchCount,
            long lastConsumedCatchIndex,
            ImmutableList<Integer> currentDimensions,
            boolean checkingArrayDimensions,
            ImmutableList<TraceElement> traceElementsForCurrentRuleApp
    ) {
        this.traceElements = traceElements;
        this.functionDatabase = functionDatabase;
        this.currentCatchCount = currentCatchCount;
        this.lastConsumedCatchIndex = lastConsumedCatchIndex;
        this.currentDimensions = currentDimensions;
        this.checkingArrayDimensions = checkingArrayDimensions;
        this.traceElementsForCurrentRuleApp = traceElementsForCurrentRuleApp;
    }

    /**
     * Creates a new initial TracingState with the given trace elements and function database.
     */
    public TracingState(ImmutableList<TraceElement> traceElements, FunctionDatabase functionDatabase) {
        this(traceElements, functionDatabase, 0, -1, ImmutableList.of(), false, ImmutableList.of());
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

    /**
     * Given the application of a rule, this method creates and returns a new TracingState, that reflects the updated
     * state after the rule has been applied.
     */
    public TracingState updateTraceForApplication(RuleApp ruleApp) {
        if (!(ruleApp instanceof TacletApp tacletApp) || !(tacletApp.taclet() instanceof RetracingTaclet retracingTaclet)) {
            return this;
        }
        AdvanceTraceInformation advanceTraceInformation = retracingTaclet.getAdvanceTraceInformation();

        ImmutableList<Integer> newDimensions = currentDimensions;
        // cleanup only after the next rule application, because we still need the currentDimensions for the current array initialization
        if (!checkingArrayDimensions) {
            newDimensions = ImmutableList.of();
        }

        int traceElementsCount = advanceTraceInformation.getTraceElementsConsumed();

        ImmutableList<TraceElement> consumedTraceElements = traceElements.take(traceElementsCount);
        ImmutableList<TraceElement> remainingTraceElements = traceElements.skip(traceElementsCount);

        long newCatchCount = currentCatchCount;
        if (advanceTraceInformation instanceof AdvanceCatchTraceInformation(
                SchemaVariable catchCountVar, AdvanceTraceInformation additionalAdvanceTraceInformation
        )) {
            IntLiteral catchCount = tacletApp.matchConditions().getInstantiations().getInstantiation(catchCountVar);
            newCatchCount += catchCount.getValue();
        }

        long updatedLastConsumedCatchIndex = this.lastConsumedCatchIndex;
        boolean updatedCheckingArrayDimensions = this.checkingArrayDimensions;
        for (TraceElement elem : consumedTraceElements) {
            switch (elem) {
                case TraceElement.Catch(long catchIndex) -> updatedLastConsumedCatchIndex = catchIndex;

                // Reset the array dimension checking flag when encountering an if or else statement
                // as they mark the check of the last dimension
                // we do not yet clear the currentDimensions list, because we still need to access the dimensions for the current array initialization
                case TraceElement.If() -> updatedCheckingArrayDimensions = false;
                case TraceElement.Else() -> updatedCheckingArrayDimensions = false;

                case TraceElement.Dim(int dimensions) -> {
                    updatedCheckingArrayDimensions = true;
                    newDimensions = currentDimensions.append(dimensions);
                }
                default -> {
                }
            }
        }

        return new TracingState(
                remainingTraceElements,
                functionDatabase,
                newCatchCount,
                updatedLastConsumedCatchIndex,
                newDimensions,
                updatedCheckingArrayDimensions,
                consumedTraceElements
        );
    }

    public long getCurrentCatchCount() {
        return currentCatchCount;
    }

    public long getLastConsumedCatchIndex() {
        return lastConsumedCatchIndex;
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
