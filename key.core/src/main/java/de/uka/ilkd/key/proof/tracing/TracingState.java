package de.uka.ilkd.key.proof.tracing;

import org.key_project.util.collection.ImmutableList;

public class TracingState {

    private ImmutableList<TraceElement> traceElements;

    private final FunctionDatabase functionDatabase;

    private long currentCatchCount = 0;

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

    public void continueTrace() {
        traceElements = traceElements.tail();
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
