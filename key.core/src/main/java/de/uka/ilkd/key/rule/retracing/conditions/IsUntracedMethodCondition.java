package de.uka.ilkd.key.rule.retracing.conditions;

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

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.util.collection.ImmutableArray;

public final class IsUntracedMethodCondition extends VariableConditionAdapter {

    private final boolean negation;
    private final SchemaVariable receiver;
    private final SchemaVariable methodBody;
    private final SchemaVariable args;
    private final SchemaVariable executionContext;

    public IsUntracedMethodCondition(SchemaVariable receiver, SchemaVariable methodBody,
                                     SchemaVariable args, SchemaVariable executionContext, boolean negation) {
        this.negation = negation;
        this.receiver = receiver;
        this.methodBody = methodBody;
        this.args = args;
        this.executionContext = executionContext;
    }

    public IsUntracedMethodCondition(SchemaVariable receiver, SchemaVariable methodBody,
                                     SchemaVariable args, boolean negation) {
        this(receiver, methodBody, args, null, negation);
    }

    public IsUntracedMethodCondition(SchemaVariable methodBody, SchemaVariable args,
                                     boolean negation) {
        this(null, methodBody, args, null, negation);
    }

    public IsUntracedMethodCondition(SchemaVariable executionContext, boolean negation) {
        this(null, null, null, executionContext, negation);
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

        if (executionContext != null) {
            // if an explicit execution context is given, check if its method context is an internal method
            // This is used for return statements, that are also traced in static and private methods
            ExecutionContext ec = svInst.getInstantiation(executionContext);
            return negation ^ isInternalMethod(ec.getMethodContext());
        }

        ExecutionContext ec = svInst.getContextInstantiation().activeStatementContext();
        ReferencePrefix rp = null;
        if (receiver != null) {
            rp = svInst.getInstantiation(receiver);
        }

        MethodName mn = svInst.getInstantiation(methodBody);

        ImmutableArray<Expression> ar =
                toExpArray(svInst.getInstantiation(args));
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
        return negation ^ isUntracedMethod(method);
    }

    @Override
    public String toString() {
        return (negation ? "\\not " : "") + "\\isUntracedMethod(" +
                (receiver != null ? receiver + ", " : "") + methodBody + ", " + args + ")";
    }

    private static boolean isUntracedMethod(IProgramMethod method) {
        return isInternalMethod(method) || method.isStatic() || method.isPrivate();
    }

    private static boolean isInternalMethod(IProgramMethod method) {
        return method.getName().startsWith("$");
    }
}
