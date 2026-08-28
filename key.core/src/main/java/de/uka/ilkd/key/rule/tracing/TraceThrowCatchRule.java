package de.uka.ilkd.key.rule.tracing;

import de.uka.ilkd.key.java.JavaTools;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.ast.SourceElement;
import de.uka.ilkd.key.java.ast.Statement;
import de.uka.ilkd.key.java.ast.declaration.LocalVariableDeclaration;
import de.uka.ilkd.key.java.ast.declaration.Modifier;
import de.uka.ilkd.key.java.ast.declaration.ParameterDeclaration;
import de.uka.ilkd.key.java.ast.expression.PassiveExpression;
import de.uka.ilkd.key.java.ast.expression.operator.CopyAssignment;
import de.uka.ilkd.key.java.ast.statement.Catch;
import de.uka.ilkd.key.java.ast.statement.Throw;
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
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.Pair;

import java.util.List;

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
        if (active instanceof PassiveExpression pe) {
            active = pe.getChildAt(0);
        } else {
            return false;
        }

        if (!(active instanceof Throw throwStmt)) {
            return false;
        }

        // TODO braucht man das wirklich? Wenn trace sagt nächster Schritt is ein throw dann sollte alles done sein, oder?
        // Oder kann es noch weitere seiteneffekte ohne tracing haben?
        // Gleiches bie TraceIfRule
        if (!ProgramSVSort.SIMPLEEXPRESSION.canStandFor(throwStmt.getExpression(), null, services)) {
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

        JTerm focusTerm = (JTerm) app.posInOccurrence().subTerm();
        Pair<JTerm, JTerm> up = applyUpdates(focusTerm, services);
        JTerm update = up.first;
        JTerm progPost = up.second;

        Throw throwStmt = (Throw) JavaTools.getActiveStatement(progPost.javaBlock());
        Catch resolvedCatch = app.getResolvedCatch();
        List<Statement> catchStatement = (List<Statement>) resolvedCatch.getBody().getBody().toList();

        JavaBlock newJb = JavaTools.replaceStatement(
                progPost.javaBlock(), services, , branchBody);

        var modality = (JModality) progPost.op();
        JTerm newProgPost = tb.prog(modality.kind(), newJb, progPost.sub(0));
        JTerm newGoalFormula = tb.apply(update, newProgPost, null);1

        ParameterDeclaration catchParam = resolvedCatch.getParameterDeclaration();
//        catchStatement.addFirst(new CopyAssignment(resolvedCatch.getParameterDeclaration(), throwStmt.getExpression()));
//        catchStatement.addFirst(new LocalVariableDeclaration(catchParam.getModifiers(), catchParam.getTypeReference(), catchParam.getVariableSpecification()));



        Goal nextGoal = createNextGoal(goal);
        nextGoal.setBranchLabel("Trace: Throw-Catch");
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