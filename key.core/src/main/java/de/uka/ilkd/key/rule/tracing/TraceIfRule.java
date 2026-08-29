package de.uka.ilkd.key.rule.tracing;

import de.uka.ilkd.key.java.JavaTools;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.ast.SourceElement;
import de.uka.ilkd.key.java.ast.Statement;
import de.uka.ilkd.key.java.ast.expression.Expression;
import de.uka.ilkd.key.java.ast.reference.ExecutionContext;
import de.uka.ilkd.key.java.ast.statement.If;
import de.uka.ilkd.key.logic.sort.ProgramSVSort;
import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.JavaBlock;
import de.uka.ilkd.key.logic.TermBuilder;
import de.uka.ilkd.key.logic.TermServices;
import de.uka.ilkd.key.logic.op.JModality;
import de.uka.ilkd.key.logic.op.Transformer;
import de.uka.ilkd.key.logic.op.UpdateApplication;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.rule.IBuiltInRuleApp;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.key_project.logic.Name;
import org.key_project.prover.rules.RuleAbortException;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.sequent.SequentFormula;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.Pair;

import static de.uka.ilkd.key.rule.tracing.TracingRuleUtil.getTracingState;

@NullMarked
public class TraceIfRule extends AbstractTraceRule {

    private static final Name NAME = new Name("Traced If");

    public static final TraceIfRule INSTANCE = new TraceIfRule();

    private TraceIfRule() {
    }

    @Override
    public boolean isApplicableImpl(SourceElement active, JavaBlock javaBlock, Services services) {
        if (!(active instanceof If ifStmt)) {
            return false;
        }

        // TODO braucht man das wirklich? Wenn trace sagt nächster Schritt is ein throw dann sollte alles done sein, oder?
        // Oder kann es noch weitere seiteneffekte ohne tracing haben?
        if (!ProgramSVSort.SIMPLEEXPRESSION.canStandFor(ifStmt.getExpression(), null, services)) {
            return false;
        }

        return getTracingState(services).isNextTraceElementAnIfOrElse();
    }

    @Override
    public IBuiltInRuleApp createApp(@Nullable PosInOccurrence pos, TermServices services) {
        return new TraceIfRuleApp(this, pos, services);
    }

    @Override
    public ImmutableList<Goal> apply(Goal goal, RuleApp ruleApp) {
        TraceIfRuleApp app = (TraceIfRuleApp) ruleApp;
        Services services = goal.proof().getServices();
        TermBuilder tb = services.getTermBuilder();

        if (!app.complete()) {
            throw new RuleAbortException(
                    "Trace if rule app not fully instantiated");
        }

        JTerm focusTerm = (JTerm) app.posInOccurrence().subTerm();
        Pair<JTerm, JTerm> up = applyUpdates(focusTerm, services);
        JTerm update = up.first;
        JTerm progPost = up.second;

        SourceElement active = JavaTools.getActiveStatement(progPost.javaBlock());
        If ifStmt = (If) active;
        Statement branchBody = app.getResolvedBranchBody();

        JavaBlock newJb = JavaTools.replaceStatement(
                progPost.javaBlock(), services, active, branchBody);

        var modality = (JModality) progPost.op();
        JTerm newProgPost = tb.prog(modality.kind(), newJb, progPost.sub(0));
        JTerm newGoalFormula = tb.apply(update, newProgPost, null);

        Goal nextGoal = createNextGoal(goal);
        nextGoal.setBranchLabel("Trace: " + (app.isThenBranch() ? "if-then" : "if-else"));
        nextGoal.changeFormula(
                new SequentFormula(newGoalFormula),
                app.posInOccurrence()
        );

        addGuardAssumption(nextGoal, ifStmt.getExpression(), app.isThenBranch(),
                services, progPost);

        return ImmutableList.of(nextGoal);
    }

    private void addGuardAssumption(Goal goal, Expression guard, boolean isTrue,
            Services services, JTerm progPost) {
        ExecutionContext ec = TraceMethodCallRule.extractExecutionContext(services, progPost.javaBlock());
        JTerm guardTerm = services.getTypeConverter().convertToLogicElement(guard, ec);
        TermBuilder tb = services.getTermBuilder();
        JTerm boolTerm = isTrue
                ? services.getTypeConverter().getBooleanLDT().getTrueTerm()
                : services.getTypeConverter().getBooleanLDT().getFalseTerm();
        JTerm assumption = tb.equals(guardTerm, boolTerm);
        goal.addFormula(new SequentFormula(assumption), true, false);
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
