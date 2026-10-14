package de.uka.ilkd.key.rule.retracing.tracePattern;

import de.uka.ilkd.key.proof.retracing.TraceElement;
import org.key_project.util.collection.ImmutableList;

public class NegatedTracePattern implements TracePattern {

    private final BasicTracePattern pattern;

    public NegatedTracePattern(String pattern) {
        this.pattern = new BasicTracePattern(pattern);
    }

    @Override
    public boolean matches(ImmutableList<TraceElement> elements) {
        return !pattern.matches(elements);
    }

    @Override
    public int getMatchedLength() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("NegatedTracePattern does not support getMatchedLength()");
    }

}
