package de.uka.ilkd.key.rule.tracing;

import de.uka.ilkd.key.java.JavaTools;
import de.uka.ilkd.key.java.ast.Statement;
import de.uka.ilkd.key.java.ast.statement.If;
import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.TermServices;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.tracing.TraceElement;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.util.collection.ImmutableList;

import static de.uka.ilkd.key.rule.tracing.TracingRuleUtil.getTracingState;


@NullMarked
public class TraceIfRuleApp extends AbstractTraceRuleApp<TraceIfRule> {

    private final TermServices services;

    private @Nullable Statement resolvedBranchBody;
    private boolean thenBranch;

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
        return resolvedBranchBody != null;
    }

    @Override
    public TraceIfRuleApp tryToInstantiate(Goal goal) {
        JTerm progPost = programTerm();
        if (progPost == null) {
            return this;
        }

        var active = JavaTools.getActiveStatement(progPost.javaBlock());
        if (!(active instanceof If ifStmt)) {
            return this;
        }

        TraceElement next = getTracingState(goal).getNextTraceElement();

        if (next instanceof TraceElement.If) {
            this.thenBranch = true;
            this.resolvedBranchBody = ifStmt.getThen().getBody();
        } else if (next instanceof TraceElement.Else) {
            this.thenBranch = false;
            // null body when there's no else branch — the if is simply removed
            this.resolvedBranchBody = ifStmt.getElse() != null
                    ? ifStmt.getElse().getBody() : null;
        }

        return this;
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
