package de.uka.ilkd.key.rule.retracing.tracePattern;

import de.uka.ilkd.key.proof.retracing.TraceElement;
import org.key_project.util.collection.ImmutableList;

public interface TracePattern {

    TracePattern ANY = new AnyTracePattern();

    boolean matches(ImmutableList<TraceElement> elements);

    int getMatchedLength() throws UnsupportedOperationException;
}
