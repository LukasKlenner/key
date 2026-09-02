package de.uka.ilkd.key.rule.tracing;

import java.util.ArrayList;
import java.util.List;

import de.uka.ilkd.key.java.JavaTools;
import de.uka.ilkd.key.java.KeYJavaASTFactory;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.ast.*;
import de.uka.ilkd.key.java.ast.expression.Expression;
import de.uka.ilkd.key.java.ast.expression.operator.New;
import de.uka.ilkd.key.java.ast.reference.ExecutionContext;
import de.uka.ilkd.key.java.ast.statement.*;
import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.ProgramElementName;
import de.uka.ilkd.key.logic.TermServices;
import de.uka.ilkd.key.logic.VariableNamer;
import de.uka.ilkd.key.logic.op.ProgramVariable;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.tracing.TraceElement;
import de.uka.ilkd.key.proof.tracing.TracingState;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.util.collection.ImmutableList;

import static de.uka.ilkd.key.rule.tracing.TraceMethodCallRule.extractExecutionContext;
import static de.uka.ilkd.key.rule.tracing.TraceSwitchRule.needsNullCheck;
import static de.uka.ilkd.key.rule.tracing.TracingRuleUtil.getTracingState;
import static de.uka.ilkd.key.rule.tracing.TracingRuleUtil.isSimpleExpression;

@NullMarked
public class TraceSwitchRuleApp extends AbstractTraceRuleApp<TraceSwitchRule> {

    enum Mode {
        SIMPLIFY_EXPRESSION,
        THROW_NPE,
        REPLACE_WITH_BRANCH
    }

    private final TermServices services;

    private @Nullable Statement resolvedReplacement;
    private int resolvedCaseIndex = -1;
    private @Nullable Mode mode;
    private boolean skippedNullCheck = false;
    private boolean instantiated = false;

    public TraceSwitchRuleApp(TraceSwitchRule rule, @Nullable PosInOccurrence pos,
                              TermServices services) {
        this(rule, pos, null, services);
    }

    private TraceSwitchRuleApp(TraceSwitchRule rule, @Nullable PosInOccurrence pos,
                               @Nullable ImmutableList<PosInOccurrence> ifInsts,
                               TermServices services) {
        super(rule, pos, ifInsts);
        this.services = services;
    }

    @Override
    public boolean complete() {
        return instantiated;
    }

    @Override
    public TraceSwitchRuleApp tryToInstantiate(Goal goal) {
        JTerm progPost = programTerm();
        if (progPost == null) {
            return this;
        }

        var active = JavaTools.getActiveStatement(progPost.javaBlock());
        if (!(active instanceof Switch sw)) {
            return this;
        }

        Services services = goal.proof().getServices();
        TracingState tracingState = getTracingState(goal);
        ExecutionContext ec = extractExecutionContext(services, progPost.javaBlock());

        if (!isSimpleExpression(sw.getExpression(), services)) {
            buildSimplifyExpression(sw, services, ec);
            this.mode = Mode.SIMPLIFY_EXPRESSION;
            instantiated = true;
            return this;
        }

        if (needsNullCheck(sw, services, ec)) {
            TraceElement next = tracingState.getNextTraceElement();
            if (next instanceof TraceElement.If) {
                buildThrowNpe(sw, services, ec);
                this.mode = Mode.THROW_NPE;
                instantiated = true;
                return this;
            }
            // next is Else — null check passed, skip the O and decode the switch bits
            this.skippedNullCheck = true;
        }

        if (!tracingState.canNextTraceElementBeASwitch(skippedNullCheck ? 1 : 0)) {
            return this;
        }

        this.resolvedCaseIndex = tracingState.getNextSwitchCaseIndex(skippedNullCheck ? 1 : 0);
        this.mode = Mode.REPLACE_WITH_BRANCH;

        buildReplaceBranch(sw, services, ec);

        instantiated = true;
        return this;
    }

    private void buildSimplifyExpression(Switch sw, Services services, ExecutionContext ec) {
        VariableNamer varNamer = services.getVariableNamer();
        ProgramElementName name = varNamer.getTemporaryNameProposal("_var");
        ProgramVariable exV =
            KeYJavaASTFactory.localVariable(name, sw.getExpression().getKeYJavaType(services, ec));
        Statement decl =
            KeYJavaASTFactory.declare(name, sw.getExpression().getKeYJavaType(services, ec));

        Switch newSwitch = KeYJavaASTFactory.switchBlock(exV, sw.getBranchList().toArray(new Branch[0]));

        this.resolvedReplacement = KeYJavaASTFactory.block(decl,
            KeYJavaASTFactory.assign(exV, sw.getExpression()),
            newSwitch);
    }

    private void buildThrowNpe(Switch sw, Services services, ExecutionContext ec) {
        VariableNamer varNamer = services.getVariableNamer();
        ProgramElementName name = varNamer.getTemporaryNameProposal("_var");
        ProgramVariable exV =
            KeYJavaASTFactory.localVariable(name, sw.getExpression().getKeYJavaType(services, ec));
        Statement decl =
            KeYJavaASTFactory.declare(name, sw.getExpression().getKeYJavaType(services, ec));

        New exception = KeYJavaASTFactory.newOperator(
            services.getJavaInfo().getKeYJavaType("java.lang.NullPointerException"));
        Throw throwNpe = KeYJavaASTFactory.throwClause(exception);

        this.resolvedReplacement = KeYJavaASTFactory.block(decl,
            KeYJavaASTFactory.assign(exV, sw.getExpression()),
            throwNpe);
    }

    private void buildReplaceBranch(Switch sw, Services services, ExecutionContext ec) {
        VariableNamer varNamer = services.getVariableNamer();

        Label label = varNamer.getTemporaryNameProposal("_l");
        Break newBreak = KeYJavaASTFactory.breakStatement(label);

        ChangeBreakResult changeBreakResult = changeBreaks(sw, newBreak, true);
        Switch swWithBreaks = (Switch) changeBreakResult.result;

        StatementBlock caseBody = collectStatements(swWithBreaks, resolvedCaseIndex);

        if (changeBreakResult.noNewBreak) {
            this.resolvedReplacement = caseBody;
        } else {
            this.resolvedReplacement =
                KeYJavaASTFactory.labeledStatement(label, caseBody, PositionInfo.UNDEFINED);
        }
    }

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
                KeYJavaASTFactory.tryBlock((StatementBlock) block.result, branches),
                noNewBreak);
        }
        return new ChangeBreakResult(p, noNewBreak);
    }

    private StatementBlock collectStatements(Switch s, int count) {
        List<Statement> stats = new ArrayList<>();
        outer:
        for (int i = count; i < s.getBranchCount(); i++) {
            for (int j = 0; j < s.getBranchAt(i).getStatementCount(); j++) {
                Statement statement = s.getBranchAt(i).getStatementAt(j);
                stats.add(statement);
                if (statement instanceof JumpStatement) {
                    break outer;
                }
            }
        }
        return KeYJavaASTFactory.block(stats);
    }

    public @Nullable Statement getResolvedReplacement() {
        return resolvedReplacement;
    }

    public int getResolvedCaseIndex() {
        return resolvedCaseIndex;
    }

    public Mode getMode() {
        assert mode != null;
        return mode;
    }

    public boolean isSkippedNullCheck() {
        return skippedNullCheck;
    }

    @Override
    public TraceSwitchRuleApp replacePos(PosInOccurrence newPos) {
        return new TraceSwitchRuleApp(builtInRule, newPos, ifInsts, services);
    }

    @Override
    public TraceSwitchRuleApp setAssumesInsts(
            ImmutableList<PosInOccurrence> ifInsts) {
        setMutable(ifInsts);
        return this;
    }

    private record ChangeBreakResult(ProgramElement result, boolean noNewBreak) {
    }
}
