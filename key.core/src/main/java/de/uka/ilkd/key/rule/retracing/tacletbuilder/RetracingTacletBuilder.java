package de.uka.ilkd.key.rule.retracing.tacletbuilder;

import de.uka.ilkd.key.rule.retracing.advanceTrace.AdvanceTraceInformation;
import de.uka.ilkd.key.rule.retracing.tracePattern.TracePattern;

public interface RetracingTacletBuilder<T extends RetracingTacletBuilder<T>> {

    /**
     * sets the advance trace count
     */
    T setAdvanceTrace(AdvanceTraceInformation advanceTraceInformation);

    T setTracePattern(TracePattern tracePattern);

}
