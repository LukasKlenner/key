package de.uka.ilkd.key.rule.retracing.metaconstruct;

import de.uka.ilkd.key.java.KeYJavaASTFactory;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.ast.ProgramElement;
import de.uka.ilkd.key.java.ast.abstraction.KeYJavaType;
import de.uka.ilkd.key.java.ast.reference.ExecutionContext;
import de.uka.ilkd.key.java.ast.statement.Branch;
import de.uka.ilkd.key.java.ast.statement.Switch;
import de.uka.ilkd.key.logic.ProgramElementName;
import de.uka.ilkd.key.logic.VariableNamer;
import de.uka.ilkd.key.logic.op.ProgramSV;
import de.uka.ilkd.key.logic.op.ProgramVariable;
import de.uka.ilkd.key.rule.inst.SVInstantiations;
import de.uka.ilkd.key.rule.metaconstruct.ProgramTransformer;
import org.key_project.logic.Name;
import org.key_project.logic.op.sv.SchemaVariable;

public class EvaluateSwitchArg extends ProgramTransformer {

    public EvaluateSwitchArg(SchemaVariable _switch) {
        super(new Name("evaluate-switch-arg"), (ProgramSV) _switch);
    }

    @Override
    public ProgramElement[] transform(ProgramElement pe, Services services, SVInstantiations svInst) {
        final ExecutionContext ec = svInst.getExecutionContext();
        final VariableNamer varNamer = services.getVariableNamer();

        Switch sw = (Switch) pe;

        KeYJavaType exprType = sw.getExpression().getKeYJavaType(services, ec);
        ProgramElementName exprVar = varNamer.getTemporaryNameProposal("switchArg");
        ProgramVariable pv = KeYJavaASTFactory.localVariable(exprVar, exprType);

        return new ProgramElement[] {
                KeYJavaASTFactory.declare(pv, sw.getExpression(), exprType),
                KeYJavaASTFactory.switchBlock(pv, sw.getBranchList().toArray(new Branch[0]))
        };

    }
}
