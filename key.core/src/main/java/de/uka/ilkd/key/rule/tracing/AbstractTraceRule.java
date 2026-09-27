package de.uka.ilkd.key.rule.tracing;

import de.uka.ilkd.key.java.JavaTools;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.ast.SourceElement;
import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.JavaBlock;
import de.uka.ilkd.key.logic.TermServices;
import de.uka.ilkd.key.logic.op.JModality;
import de.uka.ilkd.key.logic.op.UpdateApplication;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.rule.BuiltInRule;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.util.collection.Pair;

import static de.uka.ilkd.key.rule.tracing.TracingRuleUtil.getTracingState;
import static de.uka.ilkd.key.rule.tracing.TracingRuleUtil.isPioApplicable;

@NullMarked
public abstract class AbstractTraceRule implements BuiltInRule {

    @Override
    public boolean isApplicable(Goal goal, @Nullable PosInOccurrence pio) {
        if (isPioApplicable(pio)) {
            Services services = goal.proof().getServices();
            Pair<JTerm, JTerm> up = applyUpdates((JTerm) pio.subTerm(), services);
            JTerm progPost = up.second;

            if (!(progPost.op() instanceof JModality)) {
                return false;
            }

            JavaBlock javaBlock = progPost.javaBlock();
            SourceElement active = JavaTools.getActiveStatement(javaBlock);
            return isApplicableImpl(active, javaBlock, services);
        }
        return false;
    }

    protected abstract boolean isApplicableImpl(SourceElement active, JavaBlock javaBlock, Services services);

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

    protected static Pair<JTerm, JTerm> applyUpdates(JTerm focusTerm, TermServices services) {
        if (focusTerm.op() instanceof UpdateApplication) {
            return new Pair<>(UpdateApplication.getUpdate(focusTerm),
                    UpdateApplication.getTarget(focusTerm));
        } else {
            return new Pair<>(services.getTermBuilder().skip(), focusTerm);
        }
    }
}
