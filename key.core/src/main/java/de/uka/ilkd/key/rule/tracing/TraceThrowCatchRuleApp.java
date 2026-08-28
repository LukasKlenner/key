package de.uka.ilkd.key.rule.tracing;

import de.uka.ilkd.key.java.JavaTools;
import de.uka.ilkd.key.java.ast.statement.Catch;
import de.uka.ilkd.key.java.ast.statement.Throw;
import de.uka.ilkd.key.java.ast.statement.Try;
import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.TermServices;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.tracing.TraceElement;
import de.uka.ilkd.key.rule.AbstractBuiltInRuleApp;
import org.jspecify.annotations.Nullable;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.util.collection.ImmutableList;

import java.util.Objects;

import static de.uka.ilkd.key.rule.tracing.TracingRuleUtil.getTracingState;

public class TraceThrowCatchRuleApp extends AbstractTraceRuleApp<TraceThrowCatchRule> {

    private final TermServices services;

    private @Nullable Catch resolvedCatch;

    public TraceThrowCatchRuleApp(TraceThrowCatchRule rule, @Nullable PosInOccurrence pos,
                                  TermServices services) {
        this(rule, pos, null, services);
    }

    private TraceThrowCatchRuleApp(TraceThrowCatchRule rule, @Nullable PosInOccurrence pos,
                                   @Nullable ImmutableList<PosInOccurrence> ifInsts,
                                   TermServices services) {
        super(rule, pos, ifInsts);
        this.services = services;
    }

    @Override
    public boolean complete() {
        return resolvedCatch != null;
    }

    @Override
    public AbstractBuiltInRuleApp<TraceThrowCatchRule> tryToInstantiate(Goal goal) {
        JTerm progPost = programTerm();
        if (progPost == null) {
            return this;
        }

        var active = JavaTools.getActiveStatement(progPost.javaBlock());

        if (!(active instanceof Throw throwStmt)) {
            return this;
        }

        TraceElement next = getTracingState(goal).getNextTraceElement();

        if (!(next instanceof TraceElement.Catch(int catchIndex))) {
            return this;
        }

        if (catchIndex < 0 || catchIndex >= tryStmt.getBranchCount()) {
            return this;
        }

        this.resolvedCatch = (Catch) tryStmt.getBranchAt(catchIndex);

        return this;
    }

    @Override
    public TraceThrowCatchRuleApp replacePos(PosInOccurrence newPos) {
        return new TraceThrowCatchRuleApp(builtInRule, newPos, ifInsts, services);
    }

    @Override
    public TraceThrowCatchRuleApp setAssumesInsts(
            ImmutableList<PosInOccurrence> ifInsts) {
        setMutable(ifInsts);
        return this;
    }

    public Catch getResolvedCatch() {
        return Objects.requireNonNull(resolvedCatch);
    }

}
