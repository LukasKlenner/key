package de.uka.ilkd.key.rule.tracing;

import de.uka.ilkd.key.java.JavaTools;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.ast.SourceElement;
import de.uka.ilkd.key.java.ast.Statement;
import de.uka.ilkd.key.java.ast.abstraction.PrimitiveType;
import de.uka.ilkd.key.java.ast.expression.Expression;
import de.uka.ilkd.key.java.ast.reference.ExecutionContext;
import de.uka.ilkd.key.java.ast.statement.Case;
import de.uka.ilkd.key.java.ast.statement.Default;
import de.uka.ilkd.key.java.ast.statement.Switch;
import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.JavaBlock;
import de.uka.ilkd.key.logic.TermBuilder;
import de.uka.ilkd.key.logic.TermServices;
import de.uka.ilkd.key.logic.op.JModality;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.tracing.TracingState;
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

import static de.uka.ilkd.key.rule.tracing.TraceMethodCallRule.extractExecutionContext;
import static de.uka.ilkd.key.rule.tracing.TracingRuleUtil.getTracingState;
import static de.uka.ilkd.key.rule.tracing.TracingRuleUtil.isSimpleExpression;

@NullMarked
public class TraceSwitchRule extends AbstractTraceRule {

    private static final Name NAME = new Name("Traced Switch");

    public static final TraceSwitchRule INSTANCE = new TraceSwitchRule();

    private TraceSwitchRule() {
    }

    @Override
    public boolean isApplicableImpl(SourceElement active, JavaBlock javaBlock, Services services) {
        if (!(active instanceof Switch sw)) {
            return false;
        }

        Expression expr = sw.getExpression();

        if (!isSimpleExpression(expr, services)) {
            return true;
        }

        ExecutionContext ec = extractExecutionContext(services, javaBlock);
        if (ec == null) {
            return false;
        }

        TracingState ts = getTracingState(services);

        if (needsNullCheck(sw, services, ec)) {
            return ts.isNextTraceElementAnIfOrElse();
        }

        return ts.canNextTraceElementBeASwitch();
    }

    static boolean needsNullCheck(Switch sw, Services services, ExecutionContext ec) {
        return !(sw.getExpression().getKeYJavaType(services, ec)
                .getJavaType() instanceof PrimitiveType);
    }

    @Override
    public IBuiltInRuleApp createApp(@Nullable PosInOccurrence pos, TermServices services) {
        return new TraceSwitchRuleApp(this, pos, services);
    }

    @Override
    public ImmutableList<Goal> apply(Goal goal, RuleApp ruleApp) {
        TraceSwitchRuleApp app = (TraceSwitchRuleApp) ruleApp;
        Services services = goal.proof().getServices();
        TermBuilder tb = services.getTermBuilder();

        if (!app.complete()) {
            throw new RuleAbortException("Trace switch rule app not fully instantiated");
        }

        JTerm focusTerm = (JTerm) app.posInOccurrence().subTerm();
        Pair<JTerm, JTerm> up = applyUpdates(focusTerm, services);
        JTerm update = up.first;
        JTerm progPost = up.second;

        SourceElement active = JavaTools.getActiveStatement(progPost.javaBlock());

        Statement replacement = app.getResolvedReplacement();

        JavaBlock newJb = JavaTools.replaceStatement(
                progPost.javaBlock(), services, active, replacement);

        var modality = (JModality) progPost.op();
        JTerm newProgPost = tb.prog(modality.kind(), newJb, progPost.sub(0));
        JTerm newGoalFormula = tb.apply(update, newProgPost, null);

        TraceSwitchRuleApp.Mode mode = app.getMode();
        Goal nextGoal;
        String branchLabel;

        switch (mode) {
            case SIMPLIFY_EXPRESSION -> {
                nextGoal = createNextGoal(goal, false);
                branchLabel = "Trace: switch-simplify";
            }
            case THROW_NPE -> {
                nextGoal = createNextGoal(goal, true);
                branchLabel = "Trace: switch-NPE";
            }
            case REPLACE_WITH_BRANCH -> {
                nextGoal = createNextGoal(goal, false);
                TracingState ts = getTracingState(nextGoal);
                if (app.isSkippedNullCheck()) {
                    ts.continueTrace();
                }
                ts.continueSwitchTrace();

                Switch sw = (Switch) active;
                int caseIndex = app.getResolvedCaseIndex();
                branchLabel = buildBranchLabel(sw, caseIndex);

                addSwitchAssumptions(nextGoal, sw, caseIndex, services, progPost);
            }
            default -> throw new RuleAbortException("Unknown TraceSwitchRule mode: " + mode);
        }

        nextGoal.setBranchLabel(branchLabel);
        nextGoal.changeFormula(
                new SequentFormula(newGoalFormula),
                app.posInOccurrence()
        );

        return ImmutableList.of(nextGoal);
    }

    private String buildBranchLabel(Switch sw, int caseIndex) {
        if (caseIndex < sw.getBranchCount()) {
            var branch = sw.getBranchAt(caseIndex);
            if (branch instanceof Case c) {
                return "Trace: switch-case " + c.getExpression();
            } else if (branch instanceof Default) {
                return "Trace: switch-default";
            }
        }
        return "Trace: switch-case #" + caseIndex;
    }

    private void addSwitchAssumptions(Goal goal, Switch sw, int caseIndex,
            Services services, JTerm progPost) {
        ExecutionContext ec = extractExecutionContext(services, progPost.javaBlock());
        TermBuilder tb = services.getTermBuilder();
        Expression switchExpr = sw.getExpression();
        JTerm switchExprTerm = services.getTypeConverter().convertToLogicElement(switchExpr, ec);

        var matchedBranch = sw.getBranchAt(caseIndex);

        if (matchedBranch instanceof Case c) {
            JTerm caseExprTerm = services.getTypeConverter().convertToLogicElement(c.getExpression(), ec);
            JTerm assumption = tb.equals(switchExprTerm, caseExprTerm);
            goal.addFormula(new SequentFormula(assumption), true, false);
        } else if (matchedBranch instanceof Default) {
            for (int i = 0; i < sw.getBranchCount(); i++) {
                if (sw.getBranchAt(i) instanceof Case c) {
                    JTerm caseExprTerm = services.getTypeConverter().convertToLogicElement(c.getExpression(), ec);
                    JTerm neq = tb.not(tb.equals(switchExprTerm, caseExprTerm));
                    goal.addFormula(new SequentFormula(neq), true, false);
                }
            }
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
