package de.uka.ilkd.key.proof.tracing;

import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.rule.BuiltInRule;
import org.jspecify.annotations.NullMarked;
import org.key_project.prover.rules.RuleApp;
import org.key_project.util.collection.ImmutableList;

@NullMarked
public abstract class AbstractTraceRule implements BuiltInRule {

    @Override
    public boolean isApplicableOnSubTerms() {
        return false;
    }

    @Override
    public ImmutableList<Goal> apply(Goal goal, RuleApp ruleApp) {
        ImmutableList<Goal> result = applyImpl(goal, ruleApp);
        getTracingState(goal).continueTrace();
        return result;
    }

    public abstract ImmutableList<Goal> applyImpl(Goal goal, RuleApp ruleApp);

    protected TracingState getTracingState(Goal goal) {
        TracingState tracingState = goal.proof().getServices().getTracingState();
        if (tracingState == null) {
            throw new IllegalStateException("TracingState is not available");
        }
        return tracingState;
    }
}
