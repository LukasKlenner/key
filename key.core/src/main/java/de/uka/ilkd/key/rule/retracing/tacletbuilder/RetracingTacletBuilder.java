package de.uka.ilkd.key.rule.retracing.tacletbuilder;

import de.uka.ilkd.key.rule.retracing.tracePattern.TracePattern;
import org.key_project.logic.op.sv.SchemaVariable;

public interface RetracingTacletBuilder<T extends RetracingTacletBuilder<T>> {

    T setCatchCountVar(SchemaVariable catchCountVar);

    T setTracePattern(TracePattern tracePattern);

}
