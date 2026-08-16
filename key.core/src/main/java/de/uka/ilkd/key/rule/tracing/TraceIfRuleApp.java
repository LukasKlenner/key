package de.uka.ilkd.key.rule.tracing;

import de.uka.ilkd.key.java.JavaTools;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.ast.Statement;
import de.uka.ilkd.key.java.ast.statement.If;
import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.TermBuilder;
import de.uka.ilkd.key.logic.TermServices;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.tracing.TraceElement;
import de.uka.ilkd.key.proof.tracing.TracingState;
import de.uka.ilkd.key.rule.AbstractBuiltInRuleApp;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.util.collection.ImmutableList;


@NullMarked
public class TraceIfRuleApp extends AbstractBuiltInRuleApp<TraceIfRule> {

    private final TermServices services;

    private @Nullable Statement resolvedBranchBody;
    private boolean thenBranch;
    private boolean resolved;

    public TraceIfRuleApp(TraceIfRule rule, @Nullable PosInOccurrence pos,
                          TermServices services) {
        this(rule, pos, null, services);
    }

    private TraceIfRuleApp(TraceIfRule rule, @Nullable PosInOccurrence pos,
                           @Nullable ImmutableList<PosInOccurrence> ifInsts,
                           TermServices services) {
        super(rule, pos, ifInsts);
        this.services = services;
    }

    @Override
    public boolean complete() {
        return resolved;
    }

    @Override
    public TraceIfRuleApp tryToInstantiate(Goal goal) {
        final Services services = goal.proof().getServices();

        JTerm progPost = programTerm();
        if (progPost == null) {
            return this;
        }

        var active = JavaTools.getActiveStatement(progPost.javaBlock());
        if (!(active instanceof If ifStmt)) {
            return this;
        }

        TracingState tracingState = services.getTracingState();
        TraceElement next = tracingState.getNextTraceElement();

        if (next instanceof TraceElement.If) {
            this.thenBranch = true;
            this.resolvedBranchBody = ifStmt.getThen().getBody();
            this.resolved = true;
        } else if (next instanceof TraceElement.Else) {
            this.thenBranch = false;
            // null body when there's no else branch — the if is simply removed
            this.resolvedBranchBody = ifStmt.getElse() != null
                    ? ifStmt.getElse().getBody() : null;
            this.resolved = true;
        }

        return this;
    }

    public @Nullable JTerm programTerm() {
        if (posInOccurrence() != null) {
            return TermBuilder.goBelowUpdates(
                    (JTerm) posInOccurrence().subTerm());
        }
        return null;
    }

    public @Nullable Statement getResolvedBranchBody() {
        return resolvedBranchBody;
    }

    public boolean isThenBranch() {
        return thenBranch;
    }

    @Override
    public TraceIfRuleApp replacePos(PosInOccurrence newPos) {
        return new TraceIfRuleApp(builtInRule, newPos, ifInsts, services);
    }

    @Override
    public TraceIfRuleApp setAssumesInsts(
            ImmutableList<PosInOccurrence> ifInsts) {
        setMutable(ifInsts);
        return this;
    }
}
