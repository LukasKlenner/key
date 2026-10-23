package de.uka.ilkd.key.rule.retracing.tracePattern;

import de.uka.ilkd.key.proof.retracing.TraceElement;
import de.uka.ilkd.key.rule.retracing.advanceTrace.AdvanceTraceInformation;
import org.jspecify.annotations.NullMarked;
import org.key_project.util.collection.ImmutableList;

@NullMarked
public interface TracePattern {

    TracePattern ANY = new AnyTracePattern();

    boolean matches(ImmutableList<TraceElement> elements);

    AdvanceTraceInformation getDefaultAdvanceTraceInformation();
}
