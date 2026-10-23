package de.uka.ilkd.key.rule.retracing.tracePattern;

import de.uka.ilkd.key.proof.retracing.TraceElement;
import de.uka.ilkd.key.rule.retracing.advanceTrace.AdvanceTraceInformation;
import de.uka.ilkd.key.rule.retracing.advanceTrace.NoAdvanceTraceInformation;
import org.jspecify.annotations.NullMarked;
import org.key_project.util.collection.ImmutableList;

@NullMarked
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
    public AdvanceTraceInformation getDefaultAdvanceTraceInformation() {
        return new NoAdvanceTraceInformation();
    }
}
