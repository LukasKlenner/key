package de.uka.ilkd.key.rule.retracing;

import org.key_project.logic.op.sv.SchemaVariable;

/**
 * Data class that stores information about how the application of a taclet advances the current
 * {@link de.uka.ilkd.key.proof.retracing.TracingState}
 */
public record AdvanceTraceInformation(
        int traceElementsCount,
        SchemaVariable catchCountVar
) {

    public AdvanceTraceInformation {
        if (traceElementsCount < 0) {
            throw new IllegalArgumentException("traceElementsCount must be non-negative");
        }
    }

}
