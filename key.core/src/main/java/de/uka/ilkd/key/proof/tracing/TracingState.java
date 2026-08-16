package de.uka.ilkd.key.proof.tracing;

import org.key_project.util.collection.ImmutableList;

import java.util.Map;

public class TracingState {

    private ImmutableList<TraceElement> traceElements;

    private final FunctionDatabase functionDatabase;

    public TracingState() {
        this.functionDatabase = new FunctionDatabase(Map.of(
                1, "Inheritance#main(String[]):void",
                2, "C#getNumber(int):int"
        ));
        this.traceElements = ImmutableList.of(
                new TraceElement.Else(),
                new TraceElement.Call(2)
        );
    }

    public TracingState(TracingState other) {
        this.traceElements = ImmutableList.fromList(other.traceElements);
        this.functionDatabase = other.functionDatabase;
    }

    public TraceElement getNextTraceElement() {
        return traceElements.get(0);
    }

    public boolean isNextTraceElementACall() {
        return getNextTraceElement() instanceof TraceElement.Call;
    }

    public boolean isNextTraceElementAnIfOrElse() {
        TraceElement next = getNextTraceElement();
        return next instanceof TraceElement.If || next instanceof TraceElement.Else;
    }

    public void continueTrace() {
        traceElements = traceElements.tail();
    }

    public FunctionDatabase getFunctionDatabase() {
        return functionDatabase;
    }
}
