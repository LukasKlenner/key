package de.uka.ilkd.key.rule.retracing.tracePattern;

import de.uka.ilkd.key.proof.retracing.TraceElement;
import org.key_project.util.collection.ImmutableList;

class AnyTracePattern implements TracePattern {

    @Override
    public boolean matches(ImmutableList<TraceElement> elements) {
        return true;
    }

    @Override
    public int getMatchedLength() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("AnyTracePattern does not support getMatchedLength.");
    }

}
