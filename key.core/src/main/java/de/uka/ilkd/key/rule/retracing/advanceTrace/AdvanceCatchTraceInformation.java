package de.uka.ilkd.key.rule.retracing.advanceTrace;

import org.key_project.logic.op.sv.SchemaVariable;

public record AdvanceCatchTraceInformation(
        SchemaVariable catchCountVar
) implements AdvanceTraceInformation {
}
