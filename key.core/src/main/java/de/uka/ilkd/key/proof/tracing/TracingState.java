package de.uka.ilkd.key.proof.tracing;

import org.key_project.util.collection.ImmutableList;

public class TracingState {

    private ImmutableList<TraceElement> traceElements;

    private final FunctionDatabase functionDatabase;

    private long currentCatchCount = 0;

    private static final int SWITCH_BIT_COUNT = 6; // 2^6 = 64, which is the maximum number of cases per switch

    public TracingState(ImmutableList<TraceElement> traceElements, FunctionDatabase functionDatabase) {
        this.traceElements = traceElements;
        this.functionDatabase = functionDatabase;
    }

    public TracingState(TracingState other) {
        this.traceElements = ImmutableList.fromList(other.traceElements);
        this.functionDatabase = other.functionDatabase;
        this.currentCatchCount = other.currentCatchCount;
    }

    public TraceElement getNextTraceElement() {
        return traceElements.get(0);
    }

    public boolean isNextTraceElementACall() {
        return getNextTraceElement() instanceof TraceElement.Call;
    }

    public boolean isNextTraceElementACatch() {
        return getNextTraceElement() instanceof TraceElement.Catch;
    }

    public boolean isNextTraceElementAnIfOrElse() {
        TraceElement next = getNextTraceElement();
        return next instanceof TraceElement.If || next instanceof TraceElement.Else;
    }

    public boolean canNextTraceElementBeASwitch() {
        return canNextTraceElementBeASwitch(0);
    }

    public boolean canNextTraceElementBeASwitch(int offset) {
        if (traceElements.size() < offset + SWITCH_BIT_COUNT) {
            return false;
        }
        ImmutableList<TraceElement> remaining = traceElements;
        for (int i = 0; i < offset; i++) {
            remaining = remaining.tail();
        }
        ImmutableList<TraceElement> switchBits = remaining.take(SWITCH_BIT_COUNT);
        for (TraceElement bit : switchBits) {
            if (!(bit instanceof TraceElement.If || bit instanceof TraceElement.Else)) {
                return false;
            }
        }
        return true;
    }

    public int getNextSwitchCaseIndex() {
        return getNextSwitchCaseIndex(0);
    }

    public int getNextSwitchCaseIndex(int offset) {
        if (!canNextTraceElementBeASwitch(offset)) {
            throw new IllegalStateException("Next trace elements cannot be interpreted as a switch.");
        }
        ImmutableList<TraceElement> remaining = traceElements;
        for (int i = 0; i < offset; i++) {
            remaining = remaining.tail();
        }
        ImmutableList<TraceElement> switchBits = remaining.take(SWITCH_BIT_COUNT);
        int caseIndex = 0;
        for (int i = 0; i < SWITCH_BIT_COUNT; i++) {
            TraceElement bit = switchBits.get(i);
            if (bit instanceof TraceElement.If) {
                caseIndex |= (1 << (SWITCH_BIT_COUNT - 1 - i));
            }
        }
        return caseIndex;
    }

    public boolean isAtEndOfTrace() {
        return getNextTraceElement() instanceof TraceElement.End;
    }

    public void continueTrace() {
        traceElements = traceElements.tail();
    }

    public void continueSwitchTrace() {
        for (int i = 0; i < SWITCH_BIT_COUNT; i++) {
            traceElements = traceElements.tail();
        }
    }

    public long getCurrentCatchCount() {
        return currentCatchCount;
    }

    public void incrementCurrentCatchCount(long catchCount) {
        currentCatchCount += catchCount;
    }

    public FunctionDatabase getFunctionDatabase() {
        return functionDatabase;
    }
}
