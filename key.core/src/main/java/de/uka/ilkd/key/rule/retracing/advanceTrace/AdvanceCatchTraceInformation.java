package de.uka.ilkd.key.rule.retracing.advanceTrace;

import org.jspecify.annotations.NullMarked;
import org.key_project.logic.op.sv.SchemaVariable;

@NullMarked
public record AdvanceCatchTraceInformation(
        SchemaVariable catchCountVar,
        AdvanceTraceInformation additionalAdvanceTraceInformation
) implements AdvanceTraceInformation {

    public AdvanceCatchTraceInformation {
        if (additionalAdvanceTraceInformation instanceof AdvanceCatchTraceInformation) {
            throw new IllegalArgumentException("additionalAdvanceTraceInformation must not be an instance of AdvanceCatchTraceInformation");
        }
    }

    @Override
    public int getTraceElementsConsumed() {
        return additionalAdvanceTraceInformation.getTraceElementsConsumed();
    }
}
