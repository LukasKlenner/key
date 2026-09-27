package de.uka.ilkd.key.rule.conditions;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.ast.ProgramElement;
import de.uka.ilkd.key.java.ast.abstraction.KeYJavaType;
import de.uka.ilkd.key.java.ast.expression.Expression;
import de.uka.ilkd.key.java.ast.reference.ExecutionContext;
import de.uka.ilkd.key.java.ast.reference.MethodName;
import de.uka.ilkd.key.java.ast.reference.MethodReference;
import de.uka.ilkd.key.java.ast.reference.ReferencePrefix;
import de.uka.ilkd.key.logic.op.IProgramMethod;
import de.uka.ilkd.key.rule.VariableConditionAdapter;
import de.uka.ilkd.key.rule.inst.SVInstantiations;
import de.uka.ilkd.key.rule.tracing.TracingRuleUtil;

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.util.collection.ImmutableArray;

public final class IsUntracedMethodCondition extends VariableConditionAdapter {

    private final boolean negation;
    private final SchemaVariable receiver;
    private final SchemaVariable methname;
    private final SchemaVariable args;

    public IsUntracedMethodCondition(SchemaVariable receiver, SchemaVariable methname,
            SchemaVariable args, boolean negation) {
        this.negation = negation;
        this.receiver = receiver;
        this.methname = methname;
        this.args = args;
    }

    public IsUntracedMethodCondition(SchemaVariable methname, SchemaVariable args,
            boolean negation) {
        this(null, methname, args, negation);
    }

    private static ImmutableArray<Expression> toExpArray(
            ImmutableArray<? extends ProgramElement> a) {
        Expression[] result = new Expression[a.size()];
        for (int i = 0; i < a.size(); i++) {
            result[i] = (Expression) a.get(i);
        }
        return new ImmutableArray<>(result);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean check(SchemaVariable var, SyntaxElement subst, SVInstantiations svInst,
            Services services) {
        ExecutionContext ec = svInst.getContextInstantiation().activeStatementContext();
        ReferencePrefix rp = null;
        if (receiver != null) {
            rp = (ReferencePrefix) svInst.getInstantiation(receiver);
        }

        MethodName mn = (MethodName) svInst.getInstantiation(methname);

        ImmutableArray<Expression> ar =
            toExpArray((ImmutableArray<ProgramElement>) svInst.getInstantiation(args));
        if (var == args) {
            ar = toExpArray((ImmutableArray<? extends ProgramElement>) subst);
        }

        if (mn == null) {
            return false;
        }

        MethodReference mr = new MethodReference(ar, mn, rp);
        KeYJavaType prefixType;
        if (rp == null && ec != null) {
            prefixType = ec.getTypeReference().getKeYJavaType();
        } else {
            prefixType = services.getTypeConverter().getKeYJavaType((Expression) rp, ec);
        }

        IProgramMethod method;
        if (ec != null) {
            method = mr.method(services, prefixType, ec);
        } else {
            method = mr.method(services, prefixType, mr.getMethodSignature(services, ec),
                prefixType);
        }

        if (method == null) {
            return false;
        }
        return negation ^ TracingRuleUtil.isUntracedMethod(method);
    }

    @Override
    public String toString() {
        return (negation ? "\\not " : "") + "\\isUntracedMethod(" +
            (receiver != null ? receiver + ", " : "") + methname + ", " + args + ")";
    }
}
