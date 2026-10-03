package de.uka.ilkd.key.rule.retracing.conditions;

import de.uka.ilkd.key.java.KeYJavaASTFactory;
import de.uka.ilkd.key.java.ast.ProgramElement;
import de.uka.ilkd.key.rule.inst.SVInstantiations;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.VariableCondition;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.util.collection.ImmutableArray;

public class CatchCountCondition implements VariableCondition {

    private final SchemaVariable catchCountVar;
    private final SchemaVariable catchListVar;

    public CatchCountCondition(SchemaVariable catchCountVar, SchemaVariable catchListVar, boolean negated) {
        this.catchCountVar = catchCountVar;
        this.catchListVar = catchListVar;
    }

    @Override
    public @Nullable MatchResultInfo check(
            @Nullable SchemaVariable var,
            @Nullable SyntaxElement instCandidate,
            @NonNull MatchResultInfo matchCond,
            @NonNull LogicServices services
    ) {
        ImmutableArray<ProgramElement> catchListInst = matchCond.getInstantiations().getInstantiation(catchListVar);
        int catchCount = catchListInst.size();

        final var inst = (SVInstantiations) matchCond.getInstantiations();
        return matchCond.setInstantiations(
                inst.add(catchCountVar, KeYJavaASTFactory.intLiteral(catchCount), services)
        );
    }
}
