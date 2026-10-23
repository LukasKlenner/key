package de.uka.ilkd.key.rule.retracing.tacletbuilder;

import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.rule.retracing.advanceTrace.AdvanceCatchTraceInformation;
import de.uka.ilkd.key.rule.retracing.advanceTrace.AdvanceTraceInformation;
import de.uka.ilkd.key.rule.retracing.RetracingRewriteTaclet;
import de.uka.ilkd.key.rule.retracing.tracePattern.TracePattern;
import de.uka.ilkd.key.rule.tacletbuilder.RewriteTacletBuilder;
import de.uka.ilkd.key.rule.tacletbuilder.TacletPrefixBuilder;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.TacletApplPart;

public class RetracingRewriteTacletBuilder extends RewriteTacletBuilder<RetracingRewriteTaclet> implements RetracingTacletBuilder<RetracingRewriteTacletBuilder> {

    private TracePattern tracePattern;

    private SchemaVariable catchCountVar;

    @Override
    public RetracingRewriteTaclet getRewriteTaclet() {
        if (find == null) {
            throw new TacletBuilderException(this, "No find part specified");
        }
        if (tracePattern == null) {
            throw new TacletBuilderException(this, "No trace pattern specified");
        }
        checkBoundInIfAndFind();

        AdvanceTraceInformation defaultAdvanceTraceInformation = tracePattern.getDefaultAdvanceTraceInformation();
        AdvanceTraceInformation advanceTraceInformation = catchCountVar != null ?
                new AdvanceCatchTraceInformation(catchCountVar, defaultAdvanceTraceInformation) :
                defaultAdvanceTraceInformation;

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
    public RetracingRewriteTacletBuilder setTracePattern(TracePattern tracePattern) {
        this.tracePattern = tracePattern;
        return this;
    }

    @Override
    public RetracingRewriteTacletBuilder setCatchCountVar(SchemaVariable catchCountVar) {
        this.catchCountVar = catchCountVar;
        return this;
    }
}
