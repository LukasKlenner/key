package de.uka.ilkd.key.rule.tracing;

import de.uka.ilkd.key.java.JavaTools;
import de.uka.ilkd.key.java.KeYJavaASTFactory;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.ast.ProgramElement;
import de.uka.ilkd.key.java.ast.SourceElement;
import de.uka.ilkd.key.java.ast.Statement;
import de.uka.ilkd.key.java.ast.StatementBlock;
import de.uka.ilkd.key.java.ast.declaration.LocalVariableDeclaration;
import de.uka.ilkd.key.java.ast.declaration.ParameterDeclaration;
import de.uka.ilkd.key.java.ast.declaration.VariableSpecification;
import de.uka.ilkd.key.java.ast.expression.Expression;
import de.uka.ilkd.key.java.ast.expression.PassiveExpression;
import de.uka.ilkd.key.java.ast.expression.operator.CopyAssignment;
import de.uka.ilkd.key.java.ast.statement.Catch;
import de.uka.ilkd.key.java.ast.statement.Finally;
import de.uka.ilkd.key.java.ast.statement.Throw;
import de.uka.ilkd.key.java.ast.statement.Try;
import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.JavaBlock;
import de.uka.ilkd.key.logic.TermBuilder;
import de.uka.ilkd.key.logic.TermServices;
import de.uka.ilkd.key.logic.op.JModality;
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

import static de.uka.ilkd.key.rule.tracing.TracingRuleUtil.getInnermostTryStatement;
import static de.uka.ilkd.key.rule.tracing.TracingRuleUtil.getTracingState;
import static de.uka.ilkd.key.rule.tracing.TracingRuleUtil.getTryInterruptingStatement;

@NullMarked
public class TraceTryRule extends AbstractTraceRule {

    private static final Name NAME = new Name("Traced Try");

    /**
     * The only instance of this class.
     */
    public static final TraceTryRule INSTANCE = new TraceTryRule();

    private TraceTryRule() {
    }

    @Override
    public boolean isApplicableImpl(SourceElement active, JavaBlock javaBlock, Services services) {
        Pair<Try, Integer> tryStatementWithCatchCount = getInnermostTryStatement(javaBlock);

        if (tryStatementWithCatchCount == null) {
            return false;
        }

        Try tryStmt = tryStatementWithCatchCount.first;

        // We trace empty try statments to advance the global catch counter
        if (tryStmt.getBody().isEmpty()) {
            return true;
        }

        // Always apply tracing if the try is interrupted (throw, return, etc.) to advance the global catch counter
        // We don't check for the next trace elemnent here
        return getTryInterruptingStatement(tryStatementWithCatchCount.first, services) != null;
    }

    @Override
    public IBuiltInRuleApp createApp(@Nullable PosInOccurrence pos, TermServices services) {
        return new TraceTryRuleApp(this, pos, services);
    }

    @Override
    public ImmutableList<Goal> apply(Goal goal, RuleApp ruleApp) {
        TraceTryRuleApp app = (TraceTryRuleApp) ruleApp;
        Services services = goal.proof().getServices();
        TermBuilder tb = services.getTermBuilder();

        // --- Extract focus term and leading update ---
        JTerm focusTerm = (JTerm) app.posInOccurrence().subTerm();
        Pair<JTerm, JTerm> up = applyUpdates(focusTerm, services);
        JTerm update = up.first;
        JTerm progPost = up.second;

        Try tryStatement = app.getTryStatement();
        Finally finallyBranch = app.getFinallyBranch();

        // update total catch count
        getTracingState(goal).incrementCurrentCatchCount(tryStatement.getCatchCount());

        Statement replacement;

        if (tryStatement.getBody().isEmpty()) {
            if (finallyBranch != null) {
                replacement = finallyBranch.getBody();
            } else {
                replacement = KeYJavaASTFactory.block();
            }
        } else {
            // something interrupted the try statement, e.g. a throw, break, return, etc.

            Statement tryInterruptingStatement = app.getTryInterruptingStatement();
            Catch resolvedCatch = app.getResolvedCatch();

            if (resolvedCatch != null) {
                // --- store thrown exception in variable mirroring catch parameter ---
                Throw throwStmt = app.getThrowStatement();
                ParameterDeclaration catchParam = resolvedCatch.getParameterDeclaration();
                VariableSpecification exceptionVarSpec = catchParam.getVariableSpecification();
                LocalVariableDeclaration exceptionDecl = new LocalVariableDeclaration(catchParam.getTypeReference(), catchParam.getVariableSpecification());
                PassiveExpression throwExpr = (PassiveExpression) throwStmt.getExpression();
                CopyAssignment exceptionAssignment = KeYJavaASTFactory.assign((Expression) exceptionVarSpec.getProgramVariable(), throwExpr.getExpressionAt(0));

                StatementBlock catchBlock = KeYJavaASTFactory.block(exceptionDecl, exceptionAssignment);
                catchBlock = KeYJavaASTFactory.insertStatementInBlock(catchBlock, resolvedCatch.getBody());

                if (finallyBranch != null) {
                    replacement = KeYJavaASTFactory.tryBlock(catchBlock, finallyBranch);
                } else {
                    replacement = catchBlock;
                }
            } else {
                // --- no catch block; execute finally if present and rethrow exception ---
                if (finallyBranch != null) {
                    replacement = KeYJavaASTFactory.insertStatementInBlock(finallyBranch.getBody(), KeYJavaASTFactory.block(tryInterruptingStatement));
                } else {
                    replacement = tryInterruptingStatement;
                }
            }
        }
        
        // --- Replace the try statement with the resolved catch body ---
        JavaBlock newJb = JavaTools.replaceStatement(
                progPost.javaBlock(), services, tryStatement, replacement);

        // --- Rebuild the modality term ---
        var modality = (JModality) progPost.op();
        JTerm newProgPost = tb.prog(modality.kind(), newJb, progPost.sub(0));

        // --- Re-apply the leading update ---
        JTerm newGoalFormula = tb.apply(update, newProgPost, null);

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