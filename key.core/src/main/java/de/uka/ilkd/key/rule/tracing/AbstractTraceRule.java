package de.uka.ilkd.key.rule.tracing;

import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.tracing.TracingState;
import de.uka.ilkd.key.rule.BuiltInRule;
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class AbstractTraceRule implements BuiltInRule {

    @Override
    public boolean isApplicableOnSubTerms() {
        return false;
    }

    protected Goal createNextGoal(Goal currentGoal) {
        return createNextGoal(currentGoal, true);
    }

    protected Goal createNextGoal(Goal currentGoal, boolean continueTrace) {
        Goal nextGoal = currentGoal.split(1).head();
        if (continueTrace) {
            getTracingState(nextGoal).continueTrace();
        }
        return nextGoal;
    }

    protected TracingState getTracingState(Goal goal) {
        TracingState tracingState = goal.proof().getServices().getTracingState();
        if (tracingState == null) {
            throw new IllegalStateException("TracingState is not available");
        }
        return tracingState;
    }
}
