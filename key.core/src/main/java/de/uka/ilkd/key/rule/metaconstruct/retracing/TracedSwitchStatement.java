package de.uka.ilkd.key.rule.metaconstruct.retracing;

import de.uka.ilkd.key.java.KeYJavaASTFactory;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.ast.ProgramElement;
import de.uka.ilkd.key.java.ast.Statement;
import de.uka.ilkd.key.java.ast.StatementBlock;
import de.uka.ilkd.key.java.ast.reference.ExecutionContext;
import de.uka.ilkd.key.java.ast.statement.JumpStatement;
import de.uka.ilkd.key.java.ast.statement.Switch;
import de.uka.ilkd.key.logic.op.ProgramSV;
import de.uka.ilkd.key.proof.retracing.TraceElement;
import de.uka.ilkd.key.proof.retracing.TracingState;
import de.uka.ilkd.key.rule.inst.SVInstantiations;
import de.uka.ilkd.key.rule.metaconstruct.ProgramTransformer;
import org.key_project.logic.Name;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.util.collection.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public class TracedSwitchStatement extends ProgramTransformer {

    private final ProgramSV execContextSV;

    public TracedSwitchStatement(ProgramSV ec, SchemaVariable _switch) {
        super(new Name("traced-switch-statement"), (ProgramSV) _switch);
        this.execContextSV = ec;
    }

    @Override
    public ProgramElement[] transform(ProgramElement pe, Services services, SVInstantiations svInst) {
        Switch sw = (Switch) pe;

        ExecutionContext execContext =
                execContextSV == null ? svInst.getContextInstantiation().activeStatementContext()
                        : (ExecutionContext) svInst.getInstantiation(execContextSV);

        TracingState tracingState = services.getTracingState();

        // TOOD appends all following cases with unfixed breaks. Copy break fixing from switch to if transformation.
        return new StatementBlock[]{
                collectStatements(sw, tracingState.getCurrentSwitchCaseIndex())
        };
    }

    /**
     * Collects the Statements in a switch statement from branch <code>count</code> downward,
     * mirroring the logic in {@code SwitchToIf.collectStatements}.
     *
     * @param s     the switch statement.
     * @param count the branch where the collecting of statements starts.
     */
    private StatementBlock collectStatements(Switch s, int count) {
        List<Statement> stats = new ArrayList<>();
        outer:
        for (int i = count; i < s.getBranchCount(); i++) {
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
}
