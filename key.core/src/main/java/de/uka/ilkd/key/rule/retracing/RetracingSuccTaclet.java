package de.uka.ilkd.key.rule.retracing;

import de.uka.ilkd.key.rule.SuccTaclet;
import org.jspecify.annotations.NonNull;
import org.key_project.logic.ChoiceExpr;
import org.key_project.logic.Name;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.RuleSet;
import org.key_project.prover.rules.TacletAnnotation;
import org.key_project.prover.rules.TacletApplPart;
import org.key_project.prover.rules.TacletAttributes;
import org.key_project.prover.rules.TacletPrefix;
import org.key_project.prover.rules.tacletbuilder.TacletGoalTemplate;
import org.key_project.prover.sequent.Sequent;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableMap;
import org.key_project.util.collection.ImmutableSet;

public class RetracingSuccTaclet extends SuccTaclet implements RetracingTaclet {

    private final String tracePattern;

    private final AdvanceTraceInformation advanceTraceInformation;

    public RetracingSuccTaclet(
            Name name,
            TacletApplPart applPart,
            ImmutableList<TacletGoalTemplate> goalTemplates,
            ImmutableList<RuleSet> ruleSets,
            TacletAttributes attrs,
            Sequent find,
            ImmutableMap<@NonNull SchemaVariable, TacletPrefix> prefixMap,
            ChoiceExpr choices,
            ImmutableSet<TacletAnnotation> tacletAnnotations,
            AdvanceTraceInformation advanceTraceInformation,
            String tracePattern
    ) {
        super(name, applPart, goalTemplates, ruleSets, attrs, find, prefixMap, choices, tacletAnnotations);
        this.tracePattern = tracePattern;
        this.advanceTraceInformation = advanceTraceInformation;
    }

    @Override
    public String getTracePattern() {
        return tracePattern;
    }

    @Override
    public AdvanceTraceInformation getAdvanceTraceInformation() {
        return advanceTraceInformation;
    }

}
