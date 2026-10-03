package de.uka.ilkd.key.rule.retracing.tacletbuilder;

import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.rule.retracing.AdvanceTraceInformation;
import de.uka.ilkd.key.rule.retracing.RetracingRewriteTaclet;
import de.uka.ilkd.key.rule.tacletbuilder.RewriteTacletBuilder;
import de.uka.ilkd.key.rule.tacletbuilder.TacletPrefixBuilder;
import org.key_project.prover.rules.TacletApplPart;

public class RetracingRewriteTacletBuilder extends RewriteTacletBuilder<RetracingRewriteTaclet> implements RetracingTacletBuilder<RetracingRewriteTacletBuilder> {

    private String tracePattern;

    private AdvanceTraceInformation advanceTraceInformation;

    @Override
    public RetracingRewriteTaclet getRewriteTaclet() {
        if (find == null) {
            throw new TacletBuilderException(this, "No find part specified");
        }
        if (tracePattern == null) {
            throw new TacletBuilderException(this, "No trace pattern specified");
        }
        checkBoundInIfAndFind();
        TacletPrefixBuilder prefixBuilder = new TacletPrefixBuilder(this);
        prefixBuilder.build();
        return new RetracingRewriteTaclet(name,
                new TacletApplPart(assumesSeq, applicationRestriction, varsNew, varsNotFreeIn,
                        varsNewDependingOn,
                        variableConditions),
                goals, ruleSets, attrs, (JTerm) find, prefixBuilder.getPrefixMap(),
                choices, surviveSmbExec, tacletAnnotations, advanceTraceInformation, tracePattern);
    }

    @Override
    public RetracingRewriteTacletBuilder setFind(JTerm find) {
        super.setFind(find);
        return this;
    }

    @Override
    public RetracingRewriteTacletBuilder setTracePattern(String tracePattern) {
        this.tracePattern = tracePattern;
        return this;
    }

    @Override
    public RetracingRewriteTacletBuilder setAdvanceTrace(AdvanceTraceInformation advanceTraceInformation) {
        this.advanceTraceInformation = advanceTraceInformation;
        return this;
    }
}
