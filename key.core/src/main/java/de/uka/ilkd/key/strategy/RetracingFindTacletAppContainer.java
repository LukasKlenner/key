package de.uka.ilkd.key.strategy;

import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.rule.NoPosTacletApp;
import de.uka.ilkd.key.rule.retracing.tracePattern.TracePattern;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.RuleAppCost;

public class RetracingFindTacletAppContainer extends FindTacletAppContainer {

    private final TracePattern tracePattern;

    RetracingFindTacletAppContainer(
            NoPosTacletApp app,
            PosInOccurrence pio,
            RuleAppCost ageFreeCost,
            boolean ageFreeCostIsRegular,
            RuleAppCost cost,
            Goal goal,
            long age,
            TracePattern tracePattern
    ) {
        super(app, pio, ageFreeCost, ageFreeCostIsRegular, cost, goal, age);
        this.tracePattern = tracePattern;
    }

    @Override
    protected boolean isStillApplicable(Goal p_goal) {
        if (!tracePattern.matches(p_goal.proof().getServices().getTracingState().getTraceElements())) {
            return false;
        }

        return super.isStillApplicable(p_goal);
    }
}
