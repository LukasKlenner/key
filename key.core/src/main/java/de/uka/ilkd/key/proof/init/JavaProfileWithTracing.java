package de.uka.ilkd.key.proof.init;

import de.uka.ilkd.key.rule.BuiltInRule;
import de.uka.ilkd.key.rule.tracing.TraceIfRule;
import de.uka.ilkd.key.rule.tracing.TraceMethodCallRule;
import org.key_project.util.collection.ImmutableList;

public class JavaProfileWithTracing extends JavaProfile {

    public static final String PROFILE_ID = "Java Profile with Tracing";

    public static final JavaProfileWithTracing INSTANCE = new JavaProfileWithTracing();

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

        // must stay at the front of list according to comment in JavaProfile.initBuiltInRules()
        BuiltInRule first = builtInRules.get(0);
        return builtInRules.removeFirst(first)
                .prepend(TraceMethodCallRule.INSTANCE)
                .prepend(TraceIfRule.INSTANCE)
                .prepend(first);
    }
}
