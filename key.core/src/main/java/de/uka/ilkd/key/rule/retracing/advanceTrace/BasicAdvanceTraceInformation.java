package de.uka.ilkd.key.rule.retracing.advanceTrace;

public record BasicAdvanceTraceInformation(
        int traceElementsCount
) implements AdvanceTraceInformation{

    public BasicAdvanceTraceInformation {
        if (traceElementsCount < 0) {
            throw new IllegalArgumentException("traceElementsCount must be non-negative");
        }
    }

}
