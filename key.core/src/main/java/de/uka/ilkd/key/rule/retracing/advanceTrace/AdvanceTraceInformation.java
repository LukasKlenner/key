package de.uka.ilkd.key.rule.retracing.advanceTrace;

/**
 * Data class that stores information about how the application of a taclet advances the current
 * {@link de.uka.ilkd.key.proof.retracing.TracingState}
 */
public sealed interface AdvanceTraceInformation permits AdvanceCatchTraceInformation, BasicAdvanceTraceInformation {

}
