package de.uka.ilkd.key.rule.retracing.advanceTrace;

public record NoAdvanceTraceInformation() implements AdvanceTraceInformation {

    @Override
    public int getTraceElementsConsumed() {
        return 0;
    }
}
