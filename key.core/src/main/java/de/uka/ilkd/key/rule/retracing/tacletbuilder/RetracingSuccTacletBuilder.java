package de.uka.ilkd.key.rule.retracing.tacletbuilder;

import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.rule.SuccTaclet;
import de.uka.ilkd.key.rule.retracing.AdvanceTraceInformation;
import de.uka.ilkd.key.rule.retracing.RetracingSuccTaclet;
import de.uka.ilkd.key.rule.tacletbuilder.SuccTacletBuilder;
import de.uka.ilkd.key.rule.tacletbuilder.TacletPrefixBuilder;
import org.key_project.prover.rules.ApplicationRestriction;
import org.key_project.prover.rules.TacletApplPart;
import org.key_project.prover.sequent.Sequent;

public class RetracingSuccTacletBuilder extends SuccTacletBuilder implements RetracingTacletBuilder<RetracingSuccTacletBuilder> {

    private String tracePattern;

    private AdvanceTraceInformation advanceTraceInformation;

    @Override
    public SuccTaclet getSuccTaclet() {
        if (find == null) {
            throw new TacletBuilderException(this, "No find part specified");
        }
        if (tracePattern == null) {
            throw new TacletBuilderException(this, "No trace pattern specified");
        }
        checkBoundInIfAndFind();
        final TacletPrefixBuilder prefixBuilder = new TacletPrefixBuilder(this);
        prefixBuilder.build();
        return new RetracingSuccTaclet(name,
                new TacletApplPart(assumesSeq,
                        applicationRestriction.combine(ApplicationRestriction.SUCCEDENT_POLARITY), varsNew,
                        varsNotFreeIn, varsNewDependingOn,
                        variableConditions),
                goals, ruleSets, attrs, (Sequent) find,
                prefixBuilder.getPrefixMap(),
                choices, tacletAnnotations, advanceTraceInformation, tracePattern);
    }

    @Override
    public RetracingSuccTacletBuilder setFind(Sequent findSeq) {
        super.setFind(findSeq);
        return this;
    }

    @Override
    public RetracingSuccTacletBuilder setFind(JTerm findTerm) {
        super.setFind(findTerm);
        return this;
    }

    @Override
    public RetracingSuccTacletBuilder setTracePattern(String tracePattern) {
        this.tracePattern = tracePattern;
        return this;
    }

    @Override
    public RetracingSuccTacletBuilder setAdvanceTrace(AdvanceTraceInformation advanceTraceInformation) {
        this.advanceTraceInformation = advanceTraceInformation;
        return this;
    }

}