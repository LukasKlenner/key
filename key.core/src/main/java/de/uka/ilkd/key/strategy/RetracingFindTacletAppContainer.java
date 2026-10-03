package de.uka.ilkd.key.strategy;

import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.rule.NoPosTacletApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.RuleAppCost;

public class RetracingFindTacletAppContainer extends FindTacletAppContainer {

    private final String tracePattern;

    RetracingFindTacletAppContainer(
            NoPosTacletApp app,
            PosInOccurrence pio,
            RuleAppCost ageFreeCost,
            boolean ageFreeCostIsRegular,
            RuleAppCost cost,
            Goal goal,
            long age,
            String tracePattern
    ) {
        super(app, pio, ageFreeCost, ageFreeCostIsRegular, cost, goal, age);
        this.tracePattern = tracePattern;
    }

    @Override
    protected boolean isStillApplicable(Goal p_goal) {
        if (!p_goal.proof().getServices().getTracingState().matchesPattern(tracePattern)) {
            return false;
        }

        return super.isStillApplicable(p_goal);
    }
}
