package de.uka.ilkd.key.rule.tracing;

import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.TermBuilder;
import de.uka.ilkd.key.rule.AbstractBuiltInRuleApp;
import de.uka.ilkd.key.rule.BuiltInRule;
import org.jspecify.annotations.Nullable;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.util.collection.ImmutableList;

public abstract class AbstractTraceRuleApp<R extends AbstractTraceRule> extends AbstractBuiltInRuleApp<R> {

    protected AbstractTraceRuleApp(R rule, @Nullable PosInOccurrence pio, @Nullable ImmutableList<PosInOccurrence> ifInsts) {
        super(rule, pio, ifInsts);
    }

    protected AbstractTraceRuleApp(R rule, @Nullable PosInOccurrence pio) {
        super(rule, pio);
    }

    protected @Nullable JTerm programTerm() {
        if (posInOccurrence() != null) {
            return TermBuilder.goBelowUpdates(
                    (JTerm) posInOccurrence().subTerm());
        }
        return null;
    }
}
