package de.uka.ilkd.key.rule.tracing;

import de.uka.ilkd.key.java.JavaTools;
import de.uka.ilkd.key.java.KeYJavaASTFactory;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.ast.SourceElement;
import de.uka.ilkd.key.java.ast.Statement;
import de.uka.ilkd.key.java.ast.abstraction.KeYJavaType;
import de.uka.ilkd.key.java.ast.expression.Expression;
import de.uka.ilkd.key.java.ast.expression.operator.CopyAssignment;
import de.uka.ilkd.key.java.ast.reference.ArrayReference;
import de.uka.ilkd.key.java.ast.reference.ExecutionContext;
import de.uka.ilkd.key.java.ast.reference.FieldReference;
import de.uka.ilkd.key.java.ast.reference.MethodReference;
import de.uka.ilkd.key.java.ast.reference.ReferencePrefix;
import de.uka.ilkd.key.java.ast.reference.SuperReference;
import de.uka.ilkd.key.java.ast.reference.ThisReference;
import de.uka.ilkd.key.java.ast.reference.TypeRef;
import de.uka.ilkd.key.java.ast.reference.TypeReference;
import de.uka.ilkd.key.java.ast.statement.MethodFrame;
import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.JavaBlock;
import de.uka.ilkd.key.logic.TermBuilder;
import de.uka.ilkd.key.logic.TermServices;
import de.uka.ilkd.key.logic.op.IProgramMethod;
import de.uka.ilkd.key.logic.op.JModality;
import de.uka.ilkd.key.logic.op.ProgramVariable;
import de.uka.ilkd.key.logic.op.Transformer;
import de.uka.ilkd.key.logic.op.UpdateApplication;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.tracing.AbstractTraceRule;
import de.uka.ilkd.key.rule.IBuiltInRuleApp;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.key_project.logic.Name;
import org.key_project.prover.rules.RuleAbortException;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.sequent.SequentFormula;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.Pair;

@NullMarked
public class TraceMethodCallRule extends AbstractTraceRule {

    private static final Name NAME = new Name("Traced Method Call");

    /**
     * The only instance of this class.
     */
    public static final TraceMethodCallRule INSTANCE = new TraceMethodCallRule();

    private TraceMethodCallRule() {
    }

    @Override
    public boolean isApplicable(Goal goal, @Nullable PosInOccurrence pio) {
        if (pio == null || !pio.isTopLevel() || pio.isInAntec()) {
            return false;
        }
        if (Transformer.inTransformer(pio)) {
            return false;
        }

        Services services = goal.proof().getServices();
        Pair<JTerm, JTerm> up = applyUpdates((JTerm) pio.subTerm(), services);
        JTerm progPost = up.second;

        if (!(progPost.op() instanceof JModality)) {
            return false;
        }

        SourceElement active = JavaTools.getActiveStatement(progPost.javaBlock());

        // Check if the active statement is a method call
        MethodReference methRef = extractMethodReference(active);
        if (methRef == null) {
            return false;
        }

        // Super calls are statically resolved — no dynamic dispatch
        if (methRef.getReferencePrefix() instanceof SuperReference) {
            return false;
        }

        // Resolve the method on the static type
        ExecutionContext execContext = extractExecutionContext(services, progPost);

        if (execContext == null) {
            return false;
        }

        KeYJavaType staticType = getStaticPrefixType(
                methRef.getReferencePrefix(),
                services,
                execContext
        );
        IProgramMethod method = methRef.method(services, staticType, execContext);
        if (method == null) {
            return false;
        }

        // Static methods — no dynamic dispatch
        if (method.isStatic()) {
            return false;
        }

        // Private methods and constructors — statically bound
        if (method.isPrivate()) {
            return false;
        }

        // TODO add whitelist for methodsCall that are not traced (e.g., createArrayHelper, etc.)

        return getTracingState(goal).isNextTraceElementACall();
    }

    private static @Nullable MethodReference extractMethodReference(
            SourceElement active) {
        // Shape 1: obj.m(args);
        if (active instanceof MethodReference mr) {
            return mr;
        }
        // Shape 2: x = obj.m(args);
        if (active instanceof CopyAssignment ca
                && ca.getChildCount() >= 2
                && ca.getChildAt(1) instanceof MethodReference mr) {
            return mr;
        }
        return null;
    }

    static @Nullable ExecutionContext extractExecutionContext(Services services, JTerm progPost) {
        MethodFrame innermostFrame =
                JavaTools.getInnermostMethodFrame(progPost.javaBlock(), services);
        if (innermostFrame != null) {
            return (ExecutionContext) innermostFrame.getExecutionContext();
        }
        return null;
    }

    private static Pair<JTerm, JTerm> applyUpdates(JTerm focusTerm, TermServices services) {
        if (focusTerm.op() instanceof UpdateApplication) {
            return new Pair<>(UpdateApplication.getUpdate(focusTerm),
                    UpdateApplication.getTarget(focusTerm));
        } else {
            return new Pair<>(services.getTermBuilder().skip(), focusTerm);
        }
    }

    @Override
    public IBuiltInRuleApp createApp(@Nullable PosInOccurrence pos, TermServices services) {
        return new TraceMethodCallRuleApp(this, pos, services);
    }

    // TODO add asumme for concrete Type
    @Override
    public ImmutableList<Goal> applyImpl(Goal goal, RuleApp ruleApp) {
        TraceMethodCallRuleApp app = (TraceMethodCallRuleApp) ruleApp;
        Services services = goal.proof().getServices();
        TermBuilder tb = services.getTermBuilder();

        if (!app.complete()) {
            throw new RuleAbortException(
                    "Trace method rule app not fully instantiated");
        }

        // --- Extract focus term and leading update ---
        JTerm focusTerm = (JTerm) app.posInOccurrence().subTerm();
        Pair<JTerm, JTerm> up = applyUpdates(focusTerm, services);
        JTerm u = up.first;
        JTerm progPost = up.second;

        // --- Read everything the RuleApp already resolved ---
        IProgramMethod concreteMethod = app.getResolvedTargetMethod();
        ReferencePrefix receiver = app.getResolvedReceiver();
        MethodReference methRef = app.getMethodReference();
        ProgramVariable resultVar = app.getResultVariable();

        // --- Build the MethodBodyStatement for the concrete type ---
        // This creates: <concreteType::method>(args)
        // KeY's existing method_body_expand taclet handles the actual
        // inlining on the next proof step.
        Statement mbs = KeYJavaASTFactory.methodBody(
                resultVar,        // where to store the return value (null for void)
                receiver,         // the "this" reference
                concreteMethod,   // the resolved concrete method
                (ImmutableArray<Expression>) methRef.getArguments()
        );

        // --- Splice into the JavaBlock ---
        // Get the active statement (the method call to replace)
        SourceElement active = JavaTools.getActiveStatement(progPost.javaBlock());

        // Replace it with the MethodBodyStatement
        JavaBlock newJb = JavaTools.replaceStatement(
                progPost.javaBlock(), services, active, mbs);

        // --- Rebuild the modality term ---
        var modality = (JModality) progPost.op();
        JTerm newProgPost = tb.prog(
                modality.kind(), newJb, progPost.sub(0));

        // --- Re-apply the leading update ---
        JTerm newGoalFormula = tb.apply(u, newProgPost, null);

        // --- Single goal: trust the trace ---
        ImmutableList<Goal> result = goal.split(1);
        Goal onlyGoal = result.head();
        onlyGoal.setBranchLabel("Trace: "
                //+ concreteMethod.getDeclaringType().getName() + "."
                + concreteMethod.getName());
        onlyGoal.changeFormula(
                new SequentFormula(newGoalFormula),
                app.posInOccurrence());

        return result;
    }

    /**
     * Determines the static prefix type from the reference prefix,
     * mirroring {@code MethodCall.getStaticPrefixType}.
     */
    private @Nullable KeYJavaType getStaticPrefixType(
            @Nullable ReferencePrefix refPrefix,
            Services services,
            ExecutionContext executionContext
    ) {
        if (refPrefix == null
                || (refPrefix instanceof ThisReference
                && refPrefix.getReferencePrefix() == null)) {
            return executionContext.getTypeReference().getKeYJavaType();
        } else if (refPrefix instanceof ThisReference) {
            return ((TypeReference) refPrefix.getReferencePrefix()).getKeYJavaType();
        } else if (refPrefix instanceof TypeRef tr) {
            return tr.getKeYJavaType();
        } else if (refPrefix instanceof ProgramVariable pv) {
            return pv.getKeYJavaType();
        } else if (refPrefix instanceof FieldReference fr) {
            return fr.getProgramVariable().getKeYJavaType();
        } else if (refPrefix instanceof ArrayReference ar) {
            return ar.getKeYJavaType(services, executionContext);
        } else if (refPrefix instanceof SuperReference) {
            return services.getJavaInfo().getSuperclass(
                    executionContext.getTypeReference().getKeYJavaType());
        } else {
            throw new IllegalArgumentException(
                    "Unsupported reference prefix: " + refPrefix.getClass());
        }
    }

    @Override
    public Name name() {
        return NAME;
    }

    @Override
    public String toString() {
        return name().toString();
    }
}
