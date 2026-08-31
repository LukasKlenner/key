package de.uka.ilkd.key.rule.tracing;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.ast.ProgramElement;
import de.uka.ilkd.key.java.ast.Statement;
import de.uka.ilkd.key.java.ast.statement.Catch;
import de.uka.ilkd.key.java.ast.statement.Finally;
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
import org.key_project.util.collection.Pair;

import java.util.Objects;

import static de.uka.ilkd.key.rule.tracing.TracingRuleUtil.getInnermostTryStatement;
import static de.uka.ilkd.key.rule.tracing.TracingRuleUtil.getTracingState;

public class TraceTryRuleApp extends AbstractTraceRuleApp<TraceTryRule> {

    private final TermServices services;

    private @Nullable Try tryStmt;

    private @Nullable Statement tryInterruptingStatement;

    private @Nullable Catch resolvedCatch;

    private @Nullable Finally finallyBranch;

    public TraceTryRuleApp(TraceTryRule rule, @Nullable PosInOccurrence pos,
                           TermServices services) {
        this(rule, pos, null, services);
    }

    private TraceTryRuleApp(TraceTryRule rule, @Nullable PosInOccurrence pos,
                            @Nullable ImmutableList<PosInOccurrence> ifInsts,
                            TermServices services) {
        super(rule, pos, ifInsts);
        this.services = services;
    }

    @Override
    public boolean complete() {
        return tryInterruptingStatement != null;
    }

    @Override
    public AbstractBuiltInRuleApp<TraceTryRule> tryToInstantiate(Goal goal) {
        Services services = goal.proof().getServices();
        JTerm progPost = programTerm();
        if (progPost == null) {
            return this;
        }

        Pair<Try, Integer> tryStatementWithCatchCount = getInnermostTryStatement(progPost.javaBlock());
        if (tryStatementWithCatchCount == null) {
            return this;
        }
        tryStmt = tryStatementWithCatchCount.first;
        Integer parentCatchCount = tryStatementWithCatchCount.second;

        tryInterruptingStatement = TracingRuleUtil.getTryInterruptingStatement(tryStmt, services);

        if (tryInterruptingStatement == null) {
            return this;
        }

        this.finallyBranch = tryStmt.getFinallyBranch();

        TraceElement next = getTracingState(goal).getNextTraceElement();

        if (!(next instanceof TraceElement.Catch(int recordedCatchIndex))) {
            return this;
        }

        long currentCatchCount = getTracingState(goal).getCurrentCatchCount() + parentCatchCount;
        int catchIndex = Math.toIntExact(recordedCatchIndex - currentCatchCount);

        if (catchIndex >= tryStmt.getBranchCount()) {
            return this;
        }

        this.resolvedCatch = (Catch) tryStmt.getBranchAt(catchIndex);

        return this;
    }

    @Override
    public TraceTryRuleApp replacePos(PosInOccurrence newPos) {
        return new TraceTryRuleApp(builtInRule, newPos, ifInsts, services);
    }

    @Override
    public TraceTryRuleApp setAssumesInsts(
            ImmutableList<PosInOccurrence> ifInsts) {
        setMutable(ifInsts);
        return this;
    }

    public Try getTryStatement() {
        return Objects.requireNonNull(tryStmt);
    }

    public Statement getTryInterruptingStatement() {
        return Objects.requireNonNull(tryInterruptingStatement);
    }

    public boolean hasThrowStatement() {
        return tryInterruptingStatement instanceof Throw;
    }

    public Throw getThrowStatement() {
        return (Throw) tryInterruptingStatement;
    }

    public @Nullable Catch getResolvedCatch() {
        return resolvedCatch;
    }

    public @Nullable Finally getFinallyBranch() {
        return finallyBranch;
    }

}
