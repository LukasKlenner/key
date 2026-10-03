package de.uka.ilkd.key.rule.retracing.tacletbuilder;

import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.rule.AntecTaclet;
import de.uka.ilkd.key.rule.retracing.AdvanceTraceInformation;
import de.uka.ilkd.key.rule.retracing.RetracingAntecTaclet;
import de.uka.ilkd.key.rule.tacletbuilder.AntecTacletBuilder;
import de.uka.ilkd.key.rule.tacletbuilder.TacletPrefixBuilder;
import org.key_project.prover.rules.ApplicationRestriction;
import org.key_project.prover.rules.TacletApplPart;
import org.key_project.prover.sequent.Sequent;

public class RetracingAntecTacletBuilder extends AntecTacletBuilder implements RetracingTacletBuilder<RetracingAntecTacletBuilder> {

    private String tracePattern;

    private AdvanceTraceInformation advanceTraceInformation;

    @Override
    public AntecTaclet getAntecTaclet() {
        if (find == null) {
            throw new TacletBuilderException(this, "No find part specified");
        }
        if (tracePattern == null) {
            throw new TacletBuilderException(this, "No trace pattern specified");
        }
        checkBoundInIfAndFind();

        TacletPrefixBuilder prefixBuilder = new TacletPrefixBuilder(this);

        prefixBuilder.build();

        return new RetracingAntecTaclet(name,
                new TacletApplPart(assumesSeq,
                        applicationRestriction.combine(ApplicationRestriction.ANTECEDENT_POLARITY), varsNew,
                        varsNotFreeIn, varsNewDependingOn,
                        variableConditions),
                goals, ruleSets, attrs, (Sequent) find,
                prefixBuilder.getPrefixMap(),
                choices, tacletAnnotations, advanceTraceInformation, tracePattern);
    }

    @Override
    public RetracingAntecTacletBuilder setFind(Sequent findSeq) {
        super.setFind(findSeq);
        return this;
    }

    @Override
    public RetracingAntecTacletBuilder setFind(JTerm findTerm) {
        super.setFind(findTerm);
        return this;
    }

    @Override
    public RetracingAntecTacletBuilder setTracePattern(String tracePattern) {
        this.tracePattern = tracePattern;
        return this;
    }

    @Override
    public RetracingAntecTacletBuilder setAdvanceTrace(AdvanceTraceInformation advanceTraceInformation) {
        this.advanceTraceInformation = advanceTraceInformation;
        return this;
    }

}
