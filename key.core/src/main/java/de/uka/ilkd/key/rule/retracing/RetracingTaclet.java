package de.uka.ilkd.key.rule.retracing;

import de.uka.ilkd.key.rule.retracing.advanceTrace.AdvanceTraceInformation;
import de.uka.ilkd.key.rule.retracing.tracePattern.TracePattern;

public interface RetracingTaclet {

    TracePattern getTracePattern();

    AdvanceTraceInformation getAdvanceTraceInformation();

}
