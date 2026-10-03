package de.uka.ilkd.key.proof.init;

import de.uka.ilkd.key.rule.BuiltInRule;
import de.uka.ilkd.key.rule.LoopApplyHeadRule;
import de.uka.ilkd.key.rule.LoopScopeInvariantRule;
import de.uka.ilkd.key.rule.WhileInvariantRule;
import org.key_project.util.collection.ImmutableList;

public class JavaProfileWithTracing extends JavaProfile {

    public static final String PROFILE_ID = "Java Profile with Tracing";

    private static final JavaProfileWithTracing INSTANCE = new JavaProfileWithTracing();

    public static JavaProfileWithTracing getInstance() {
        return INSTANCE;
    }

    private JavaProfileWithTracing() {
        super();
    }

    @Override
    public String ident() {
        return PROFILE_ID;
    }

    @Override
    public boolean supportsParallelAutomode() {
        return false;
    }

    @Override
    public String displayName() {
        return PROFILE_ID;
    }

    @Override
    public String description() {
        return "Java programs annotated to follow a prerecorded execution path";
    }

    @Override
    protected ImmutableList<BuiltInRule> initBuiltInRules() {
        ImmutableList<BuiltInRule> builtInRules = super.initBuiltInRules();

        // Remove loop-related built-in rules — traced proofs use loopUnwind + trace if-taclets instead
        // TODO muss man das wirklich entfernen?
        builtInRules = builtInRules
                .removeFirst(WhileInvariantRule.INSTANCE)
                .removeFirst(LoopScopeInvariantRule.INSTANCE)
                .removeFirst(LoopApplyHeadRule.INSTANCE);

        return builtInRules;
    }

}
