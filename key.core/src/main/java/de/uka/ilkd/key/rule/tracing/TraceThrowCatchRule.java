package de.uka.ilkd.key.rule.tracing;

import de.uka.ilkd.key.java.JavaTools;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.ast.ProgramElement;
import de.uka.ilkd.key.java.ast.SourceElement;
import de.uka.ilkd.key.java.ast.Statement;
import de.uka.ilkd.key.java.ast.declaration.LocalVariableDeclaration;
import de.uka.ilkd.key.java.ast.declaration.Modifier;
import de.uka.ilkd.key.java.ast.declaration.ParameterDeclaration;
import de.uka.ilkd.key.java.ast.expression.PassiveExpression;
import de.uka.ilkd.key.java.ast.expression.operator.CopyAssignment;
import de.uka.ilkd.key.java.ast.statement.Catch;
import de.uka.ilkd.key.java.ast.statement.Throw;
import de.uka.ilkd.key.java.ast.statement.Try;
import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.JavaBlock;
import de.uka.ilkd.key.logic.TermBuilder;
import de.uka.ilkd.key.logic.TermServices;
import de.uka.ilkd.key.logic.op.JModality;
import de.uka.ilkd.key.logic.sort.ProgramSVSort;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.rule.IBuiltInRuleApp;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.key_project.logic.Name;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.sequent.SequentFormula;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.Pair;

import java.util.List;

import static de.uka.ilkd.key.rule.tracing.TracingRuleUtil.getThrowStatement;
import static de.uka.ilkd.key.rule.tracing.TracingRuleUtil.getTracingState;

@NullMarked
public class TraceThrowCatchRule extends AbstractTraceRule {

    private static final Name NAME = new Name("Traced Throw Catch");

    /**
     * The only instance of this class.
     */
    public static final TraceThrowCatchRule INSTANCE = new TraceThrowCatchRule();

    private TraceThrowCatchRule() {
    }

    @Override
    public boolean isApplicableImpl(SourceElement active, JavaBlock javaBlock, Services services) {
        if (!(active instanceof Try tryStatement)) {
            return false;
        }

        Throw throwStmt = getThrowStatement(tryStatement);

        if (throwStmt == null) {
            return false;
        }

        return getTracingState(services).isNextTraceElementACatch();
    }

    @Override
    public IBuiltInRuleApp createApp(@Nullable PosInOccurrence pos, TermServices services) {
        return new TraceThrowCatchRuleApp(this, pos, services);
    }

    @Override
    public ImmutableList<Goal> apply(Goal goal, RuleApp ruleApp) {
        TraceThrowCatchRuleApp app = (TraceThrowCatchRuleApp) ruleApp;
        Services services = goal.proof().getServices();
        TermBuilder tb = services.getTermBuilder();

        // --- Extract focus term and leading update ---
        JTerm focusTerm = (JTerm) app.posInOccurrence().subTerm();
        Pair<JTerm, JTerm> up = applyUpdates(focusTerm, services);
        JTerm update = up.first;
        JTerm progPost = up.second;

        // --- Extract context ---
        Try tryStatement = (Try) JavaTools.getActiveStatement(progPost.javaBlock());
        Throw throwStmt = getThrowStatement(tryStatement);
        if (throwStmt == null) {
            throw new IllegalStateException("No throw statement found");
        }

        Catch resolvedCatch = app.getResolvedCatch();
        List<Statement> catchStatement = (List<Statement>) resolvedCatch.getBody().getBody().toList();

        // Replace the try statement with the resolved catch body
        JavaBlock newJb = JavaTools.replaceStatement(
                progPost.javaBlock(), services, tryStatement, resolvedCatch.getBody());

        // --- Rebuild the modality term ---
        var modality = (JModality) progPost.op();
        JTerm newProgPost = tb.prog(modality.kind(), newJb, progPost.sub(0));


        // --- Re-apply the leading update ---
        JTerm newGoalFormula = tb.apply(update, newProgPost, null);

        ParameterDeclaration catchParam = resolvedCatch.getParameterDeclaration();
//        catchStatement.addFirst(new CopyAssignment(resolvedCatch.getParameterDeclaration(), throwStmt.getExpression()));
//        catchStatement.addFirst(new LocalVariableDeclaration(catchParam.getModifiers(), catchParam.getTypeReference(), catchParam.getVariableSpecification()));

        // --- Create the new goal ---
        Goal nextGoal = createNextGoal(goal);
        nextGoal.setBranchLabel("Trace: Throw-Catch");
        nextGoal.changeFormula(new SequentFormula(newGoalFormula), app.posInOccurrence());

        return ImmutableList.of(nextGoal);
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