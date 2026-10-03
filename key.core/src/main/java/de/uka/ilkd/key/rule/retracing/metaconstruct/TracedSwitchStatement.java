package de.uka.ilkd.key.rule.retracing.metaconstruct;

import de.uka.ilkd.key.java.KeYJavaASTFactory;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.ast.Label;
import de.uka.ilkd.key.java.ast.PositionInfo;
import de.uka.ilkd.key.java.ast.ProgramElement;
import de.uka.ilkd.key.java.ast.Statement;
import de.uka.ilkd.key.java.ast.StatementBlock;
import de.uka.ilkd.key.java.ast.statement.Branch;
import de.uka.ilkd.key.java.ast.statement.Break;
import de.uka.ilkd.key.java.ast.statement.Case;
import de.uka.ilkd.key.java.ast.statement.Catch;
import de.uka.ilkd.key.java.ast.statement.Default;
import de.uka.ilkd.key.java.ast.statement.Else;
import de.uka.ilkd.key.java.ast.statement.Finally;
import de.uka.ilkd.key.java.ast.statement.If;
import de.uka.ilkd.key.java.ast.statement.JumpStatement;
import de.uka.ilkd.key.java.ast.statement.Switch;
import de.uka.ilkd.key.java.ast.statement.Then;
import de.uka.ilkd.key.java.ast.statement.Try;
import de.uka.ilkd.key.logic.VariableNamer;
import de.uka.ilkd.key.logic.op.ProgramSV;
import de.uka.ilkd.key.proof.retracing.TracingState;
import de.uka.ilkd.key.rule.inst.SVInstantiations;
import de.uka.ilkd.key.rule.metaconstruct.ProgramTransformer;
import de.uka.ilkd.key.rule.metaconstruct.SwitchToIf;
import org.key_project.logic.Name;
import org.key_project.logic.op.sv.SchemaVariable;

import java.util.ArrayList;
import java.util.List;

public class TracedSwitchStatement extends ProgramTransformer {

    public TracedSwitchStatement(SchemaVariable _switch) {
        super(new Name("traced-switch-statement"), (ProgramSV) _switch);
    }

    @Override
    public ProgramElement[] transform(ProgramElement pe, Services services, SVInstantiations svInst) {
        Switch sw = (Switch) pe;

        VariableNamer varNamer = services.getVariableNamer();

        Label l = varNamer.getTemporaryNameProposal("_l");
        Break newBreak = KeYJavaASTFactory.breakStatement(l);

        final var changeBreakResult = changeBreaks(sw, newBreak, true);
        sw = (Switch) changeBreakResult.result;

        StatementBlock result = collectStatements(sw, services.getTracingState().getCurrentSwitchCaseIndex());

        if (changeBreakResult.noNewBreak) {
            return new ProgramElement[] { result };
        } else {
            return new ProgramElement[] {
                    KeYJavaASTFactory.labeledStatement(l, result, PositionInfo.UNDEFINED) };
        }
    }

    // TODO refactor: below is copied from SwitchToIf, but should be refactored to avoid code duplication

    /**
     * Replaces all breaks in <code>sw</code>, whose target is sw, with <code>b</code>
     */
    private ChangeBreakResult changeBreaks(Switch sw, Break b, boolean noNewBreak) {
        int n = sw.getBranchCount();
        Branch[] branches = new Branch[n];
        for (int i = 0; i < n; i++) {
            final var branch = recChangeBreaks(sw.getBranchAt(i), b, noNewBreak);
            noNewBreak = branch.noNewBreak;
            branches[i] = (Branch) branch.result;
        }
        return new ChangeBreakResult(KeYJavaASTFactory.switchBlock(sw.getExpression(), branches),
                noNewBreak);
    }

    private ChangeBreakResult recChangeBreaks(ProgramElement p, Break b, boolean noNewBreak) {
        if (p == null) {
            return null;
        }
        if (p instanceof Break && ((Break) p).getLabel() == null) {
            return new ChangeBreakResult(b, false);
        }
        if (p instanceof Branch) {
            Statement[] s = new Statement[((Branch) p).getStatementCount()];
            for (int i = 0; i < ((Branch) p).getStatementCount(); i++) {
                final ChangeBreakResult r =
                        recChangeBreaks(((Branch) p).getStatementAt(i), b, noNewBreak);
                noNewBreak = r.noNewBreak;
                s[i] = (Statement) r.result;
            }
            if (p instanceof Case) {
                return new ChangeBreakResult(
                        KeYJavaASTFactory.caseBlock(((Case) p).getExpression(), s),
                        noNewBreak);
            }
            if (p instanceof Default) {
                return new ChangeBreakResult(
                        KeYJavaASTFactory.defaultBlock(s),
                        noNewBreak);
            }
            if (p instanceof Catch) {
                return new ChangeBreakResult(
                        KeYJavaASTFactory.catchClause(((Catch) p).getParameterDeclaration(), s),
                        noNewBreak);
            }
            if (p instanceof Finally) {
                return new ChangeBreakResult(KeYJavaASTFactory.finallyBlock(s),
                        noNewBreak);
            }
            if (p instanceof Then) {
                return new ChangeBreakResult(
                        KeYJavaASTFactory.thenBlock(s),
                        noNewBreak);
            }
            if (p instanceof Else) {
                return new ChangeBreakResult(
                        KeYJavaASTFactory.elseBlock(s),
                        noNewBreak);
            }
        }
        if (p instanceof If) {
            final ChangeBreakResult then = recChangeBreaks(((If) p).getThen(), b, noNewBreak);
            noNewBreak = then.noNewBreak;
            final ChangeBreakResult _else = recChangeBreaks(((If) p).getElse(), b, noNewBreak);
            noNewBreak = _else.noNewBreak;
            return new ChangeBreakResult(
                    KeYJavaASTFactory.ifElse(((If) p).getExpression(),
                            (Then) then.result, (Else) _else.result),
                    noNewBreak);

        }
        if (p instanceof StatementBlock) {
            Statement[] s = new Statement[((StatementBlock) p).getStatementCount()];
            for (int i = 0; i < ((StatementBlock) p).getStatementCount(); i++) {
                final ChangeBreakResult blockStmnt =
                        recChangeBreaks(((StatementBlock) p).getStatementAt(i), b, noNewBreak);
                noNewBreak = blockStmnt.noNewBreak;
                s[i] = (Statement) blockStmnt.result;
            }
            return new ChangeBreakResult(
                    KeYJavaASTFactory.block(s),
                    noNewBreak);
        }
        if (p instanceof Try) {
            int n = ((Try) p).getBranchCount();
            Branch[] branches = new Branch[n];
            for (int i = 0; i < n; i++) {
                final ChangeBreakResult branch =
                        recChangeBreaks(((Try) p).getBranchAt(i), b, noNewBreak);
                noNewBreak = branch.noNewBreak;
                branches[i] = (Branch) branch.result;
            }
            final var block = recChangeBreaks(((Try) p).getBody(), b, noNewBreak);
            noNewBreak = block.noNewBreak;
            return new ChangeBreakResult(
                    KeYJavaASTFactory
                            .tryBlock((StatementBlock) block.result, branches),
                    noNewBreak);
        }
        return new ChangeBreakResult(p, noNewBreak);
    }

    /**
     * Collects the Statements in a switch statement from branch <code>count</code> downward.
     *
     * @param s the switch statement.
     * @param count the branch where the collecting of statements starts.
     */
    private StatementBlock collectStatements(Switch s, int count) {
        List<Statement> stats = new ArrayList<>();
        outer: for (int i = count; i < s.getBranchCount(); i++) {
            for (int j = 0; j < s.getBranchAt(i).getStatementCount(); j++) {
                Statement statement = s.getBranchAt(i).getStatementAt(j);
                stats.add(statement);
                if (statement instanceof JumpStatement) {
                    // unconditional jump to outside the case (?)
                    break outer;
                }
            }
        }
        return KeYJavaASTFactory.block(stats);
    }

    record ChangeBreakResult(ProgramElement result, boolean noNewBreak) {
    }

}
