package de.uka.ilkd.key.rule.tracing;

import de.uka.ilkd.key.java.JavaTools;
import de.uka.ilkd.key.java.KeYJavaASTFactory;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.ast.Statement;
import de.uka.ilkd.key.java.ast.StatementBlock;
import de.uka.ilkd.key.java.ast.statement.LoopStatement;
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
public class TraceLoopRuleApp extends AbstractBuiltInRuleApp<TraceLoopRule> {

    private final TermServices services;

    private @Nullable Statement resolvedReplacement;
    private boolean enterBody;
    private boolean resolved;

    public TraceLoopRuleApp(TraceLoopRule rule, @Nullable PosInOccurrence pos,
                            TermServices services) {
        this(rule, pos, null, services);
    }

    private TraceLoopRuleApp(TraceLoopRule rule, @Nullable PosInOccurrence pos,
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
    public TraceLoopRuleApp tryToInstantiate(Goal goal) {
        final Services services = goal.proof().getServices();

        JTerm progPost = programTerm();
        if (progPost == null) {
            return this;
        }

        var active = JavaTools.getActiveStatement(progPost.javaBlock());
        if (!(active instanceof LoopStatement loop)) {
            return this;
        }

        TracingState tracingState = services.getTracingState();
        TraceElement next = tracingState.getNextTraceElement();

        if (next instanceof TraceElement.If) {
            // Enter loop body: unwind one iteration
            // while(guard) { body } → { body; while(guard) { body } }
            this.enterBody = true;
            StatementBlock unrolled = KeYJavaASTFactory.block(
                    loop.getBody(),
                    (Statement) loop
            );
            this.resolvedReplacement = unrolled;
            this.resolved = true;
        } else if (next instanceof TraceElement.Else) {
            // Exit loop: guard is false, remove the loop
            this.enterBody = false;
            this.resolvedReplacement = null;
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

    public @Nullable Statement getResolvedReplacement() {
        return resolvedReplacement;
    }

    public boolean isEnterBody() {
        return enterBody;
    }

    @Override
    public TraceLoopRuleApp replacePos(PosInOccurrence newPos) {
        return new TraceLoopRuleApp(builtInRule, newPos, ifInsts, services);
    }

    @Override
    public TraceLoopRuleApp setAssumesInsts(
            ImmutableList<PosInOccurrence> ifInsts) {
        setMutable(ifInsts);
        return this;
    }
}
