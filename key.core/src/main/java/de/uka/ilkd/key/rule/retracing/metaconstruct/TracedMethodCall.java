package de.uka.ilkd.key.rule.retracing.metaconstruct;

import de.uka.ilkd.key.java.KeYJavaASTFactory;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.ast.ProgramElement;
import de.uka.ilkd.key.java.ast.Statement;
import de.uka.ilkd.key.java.ast.abstraction.ArrayType;
import de.uka.ilkd.key.java.ast.abstraction.KeYJavaType;
import de.uka.ilkd.key.java.ast.abstraction.Type;
import de.uka.ilkd.key.java.ast.declaration.MethodDeclaration;
import de.uka.ilkd.key.java.ast.declaration.ParameterDeclaration;
import de.uka.ilkd.key.java.ast.declaration.VariableSpecification;
import de.uka.ilkd.key.java.ast.expression.ArrayInitializer;
import de.uka.ilkd.key.java.ast.expression.Expression;
import de.uka.ilkd.key.java.ast.expression.operator.NewArray;
import de.uka.ilkd.key.java.ast.reference.ExecutionContext;
import de.uka.ilkd.key.java.ast.reference.FieldReference;
import de.uka.ilkd.key.java.ast.reference.MethodReference;
import de.uka.ilkd.key.java.ast.reference.ReferencePrefix;
import de.uka.ilkd.key.java.ast.reference.SuperReference;
import de.uka.ilkd.key.java.ast.reference.ThisReference;
import de.uka.ilkd.key.java.ast.reference.TypeRef;
import de.uka.ilkd.key.java.ast.reference.TypeReference;
import de.uka.ilkd.key.java.ast.statement.MethodBodyStatement;
import de.uka.ilkd.key.logic.op.IProgramMethod;
import de.uka.ilkd.key.logic.op.IProgramVariable;
import de.uka.ilkd.key.logic.op.ProgramSV;
import de.uka.ilkd.key.logic.op.ProgramVariable;
import de.uka.ilkd.key.proof.retracing.FunctionDatabase;
import de.uka.ilkd.key.proof.retracing.TraceElement;
import de.uka.ilkd.key.proof.retracing.TracingState;
import de.uka.ilkd.key.rule.inst.SVInstantiations;
import de.uka.ilkd.key.rule.metaconstruct.ProgramTransformer;
import de.uka.ilkd.key.util.Debug;
import org.jspecify.annotations.Nullable;
import org.key_project.logic.Name;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.logic.sort.Sort;
import org.key_project.util.collection.ImmutableArray;

/**
 * Metaconstruct that resolves a method call using the recorded execution trace.
 * For traced methods (virtual dispatch), reads the Call(functionId) from the {@link TracingState}
 * and resolves the concrete method via if {@link FunctionDatabase}.
 * For untraced methods (static, private, $-prefixed), delegates to the standard
 * MethodCall logic.
 */
public final class TracedMethodCall extends ProgramTransformer {

    private final SchemaVariable resultVar;
    private final ProgramSV execContextSV;

    public TracedMethodCall(ProgramSV ec, SchemaVariable result, ProgramElement body) {
        super(new Name("traced-method-call"), body);
        this.execContextSV = ec;
        this.resultVar = result;
    }

    @Override
    public ProgramElement[] transform(
            ProgramElement pe,
            Services services,
            SVInstantiations svInst
    ) {

        ProgramVariable pvar = null;
        if (resultVar != null) {
            pvar = svInst.getInstantiation(resultVar);
        }

        ExecutionContext execContext =
                execContextSV == null ? svInst.getContextInstantiation().activeStatementContext()
                        : (ExecutionContext) svInst.getInstantiation(execContextSV);

        MethodReference methRef = (MethodReference) pe;

        ReferencePrefix refPrefix = methRef.getReferencePrefix();
        if (refPrefix == null) {
            if (execContext.getRuntimeInstance() == null) {
                refPrefix = execContext.getTypeReference();
            } else {
                refPrefix = execContext.getRuntimeInstance();
            }
        }

        KeYJavaType staticPrefixType = getStaticPrefixType(methRef.getReferencePrefix(), services, execContext);

        IProgramMethod pm = execContext == null
                ? methRef.method(services, staticPrefixType,
                methRef.getMethodSignature(services, null), staticPrefixType)
                : methRef.method(services, staticPrefixType, execContext);

        MethodDeclaration methDecl = pm.getMethodDeclaration();

        VariableSpecification[] paramSpecs = createParamSpecs(services, methDecl, methRef, execContext);
        Statement[] paramDecl = createParamAssignments(paramSpecs, methDecl);
        ImmutableArray<Expression> arguments = getVariables(paramSpecs);

        MethodBodyStatement methodBody;

        if (refPrefix instanceof SuperReference) {
            methodBody = handleSuperReference(services, execContext, methRef, pvar, arguments);
        } else {
            methodBody = KeYJavaASTFactory.methodBody(
                    pvar,
                    resolveReceiver(services, execContext, methRef),
                    resolveTargetMethod(services, execContext, methRef),
                    arguments
            );
        }

        return new ProgramElement[]{
                KeYJavaASTFactory.insertStatementInBlock(
                        paramDecl,
                        KeYJavaASTFactory.block(methodBody)
                )
        };
    }

    private @Nullable IProgramMethod resolveTargetMethod(
            Services services,
            ExecutionContext executionContext,
            MethodReference methodReference
    ) {
        if (methodReference == null || executionContext == null) {
            return null;
        }

        TracingState tracingState = services.getTracingState();
        TraceElement.Call callElement = tracingState.findConsumedTraceElement(TraceElement.Call.class);
        FunctionDatabase functionDatabase = tracingState.getFunctionDatabase();

        return functionDatabase.getProgramMethodById(callElement.functionID(), services, executionContext, methodReference);
    }

    /**
     * Determines the reference prefix (receiver) for the resolved method,
     * mirroring the logic in {@code MethodCall.transformImpl}.
     */
    private @Nullable ReferencePrefix resolveReceiver(
            Services services,
            ExecutionContext executionContext,
            MethodReference methodReference
    ) {

        ReferencePrefix prefix = methodReference.getReferencePrefix();

        switch (prefix) {
            case null -> {
                if (executionContext.getRuntimeInstance() == null) {
                    return executionContext.getTypeReference();
                } else {
                    return executionContext.getRuntimeInstance();
                }
            }
            case ThisReference ignored -> {
                return (ReferencePrefix) services.getTypeConverter().convertToProgramElement(
                        services.getTypeConverter().convertToLogicElement(
                                prefix, executionContext));
            }
            case FieldReference fr when fr.referencesOwnInstanceField() -> {
                return fr.setReferencePrefix(executionContext.getRuntimeInstance());
            }
            default -> {
            }
        }

        return prefix;
    }

    // copied from MethodCall

    private VariableSpecification[] createParamSpecs(Services services, MethodDeclaration methDecl, MethodReference methRef, ExecutionContext executionContext) {

        int params = methDecl.getParameterDeclarationCount();
        VariableSpecification[] varSpecs = new VariableSpecification[params];
        for (int i = 0; i < params; i++) {
            ParameterDeclaration parDecl = methDecl.getParameterDeclarationAt(i);
            VariableSpecification originalSpec = parDecl.getVariableSpecification();
            final ProgramVariable originalParamVar =
                    (ProgramVariable) originalSpec.getProgramVariable();

            final IProgramVariable paramVar = KeYJavaASTFactory.localVariable(services,
                    originalParamVar.getProgramElementName().toString(),
                    originalParamVar.getKeYJavaType());

            // this condition checks whether this is the last formal parameter
            // and is used
            // in the context of a method with a variable number of args.
            // see makeVariableArgument below
            if (i == params - 1 && methDecl.isVarArgMethod()
                    && (methRef.getArguments().size() != params
                    || !assignmentCompatible(methRef.getArgumentAt(i),
                    originalSpec.getType(), services, executionContext))) {
                // variable argument
                varSpecs[i] = KeYJavaASTFactory.variableSpecification(paramVar, 1,
                        makeVariableArgument(originalSpec, methDecl, methRef), originalSpec.getType());
            } else {
                // normal argument
                varSpecs[i] = KeYJavaASTFactory.variableSpecification(paramVar,
                        originalSpec.getDimensions(), methRef.getArgumentAt(i), originalSpec.getType());
            }
        }
        return varSpecs;
    }

    /**
     * make an array out of a list of arguments to a method with variable arguments.
     *
     * <pre>
     * Quote JLS 15.12.4.2 Evaluate Arguments The process of evaluating of the
     * argument list differs, depending on whether the method being invoked is a
     * fixed arity method or a variable arity method (JLS 8.4.1).
     *
     * If the method being invoked is a variable arity method (JLS 8.4.1) m, it
     * necessarily has n>0 formal parameters. The final formal parameter of m
     * necessarily has type T[] for some T, and m is necessarily being invoked
     * with k0 actual argument expressions.
     *
     * If m is being invoked with kn actual argument expressions, or, if m is
     * being invoked with k=n actual argument expressions and the type of the
     * kth argument expression is not assignment compatible with T[], then the
     * argument list (e1, ... , en-1, en, ...ek) is evaluated as if it were
     * written as (e1, ..., en-1, new T[]{en, ..., ek}).
     * </pre>
     * <p>
     * Thus, when the last formal parameter is reached and circumstances enforce wrapping arguments
     * in an array, this method creates the expected.
     *
     * @param originalSpec the original sepcification of the formal paramater
     * @param methDecl     the method declaration
     * @param methRef      the method reference
     * @return an Newarray expression conglomerating all remaining arguments may be zero.
     * @author MU
     * @since 2008-Mar
     * <p>
     * see examples/java_dl/java5/vararg.key for examples and tests.
     */
    private Expression makeVariableArgument(VariableSpecification originalSpec, MethodDeclaration methDecl, MethodReference methRef) {

        int params = methDecl.getParameterDeclarationCount();
        int args = methRef.getArguments().size();
        Expression[] exps = new Expression[args - params + 1];

        for (int i = 0; i < exps.length; i++) {
            exps[i] = methRef.getArgumentAt(params - 1 + i);
        }

        Type type = originalSpec.getType();

        // for some reason this does not work, but type itself is a KJT ???
        // KeYJavaType kjt = services.getJavaInfo().getKeYJavaType(type);
        KeYJavaType kjt = (KeYJavaType) type;
        ArrayType arrayTy = (ArrayType) kjt.getJavaType();

        TypeReference baseTyRef = arrayTy.getBaseType();

        ArrayInitializer arrayInit = KeYJavaASTFactory.arrayInitializer(exps, kjt);
        NewArray newArray = KeYJavaASTFactory.newArray(baseTyRef, 1, arrayInit, kjt);

        return newArray;
    }

    private Statement[] createParamAssignments(VariableSpecification[] specs, MethodDeclaration methDecl) {
        Statement[] paramDecl = new Statement[specs.length];
        for (int i = 0; i < specs.length; i++) {
            ParameterDeclaration parDecl = methDecl.getParameterDeclarationAt(i);
            paramDecl[i] = KeYJavaASTFactory.declare(parDecl.getModifiers(),
                    parDecl.getTypeReference(), specs[i]);
        }
        return paramDecl;
    }

    private ImmutableArray<Expression> getVariables(VariableSpecification[] varspecs) {
        Expression[] vars = new Expression[varspecs.length];
        for (int i = 0; i < varspecs.length; i++) {
            vars[i] = (Expression) varspecs[i].getProgramVariable();
        }
        return new ImmutableArray<>(vars);
    }

    /**
     * check whether an expression is assignment-compatible with the array over a type.
     * <p>
     * Quoting JLS: 15.12.4.2 Evaluate Arguments
     * <p>
     * "if m is being invoked with k=n actual argument expressions and the type of the kth argument
     * expression is not assignment compatible with T[], then the argument list"
     * <p>
     * In the absence of autoboxing this is the case if the type is a subtype.
     *
     * @param exp  expression to check
     * @param type type to check for
     * @return true iff exp is assign compatible with type
     */
    private boolean assignmentCompatible(Expression exp, Type type, Services services, ExecutionContext execContext) {
        Sort expSort = exp.getKeYJavaType(services, execContext).getSort();
        Sort typeSort = ((KeYJavaType) type).getSort();
        /* was: services.getJavaInfo().getKeYJavaType(type); */
        return expSort.extendsTrans(typeSort);
    }

    private KeYJavaType getStaticPrefixType(ReferencePrefix refPrefix, Services services, ExecutionContext execContext) {
        if (refPrefix == null
                || refPrefix instanceof ThisReference && refPrefix.getReferencePrefix() == null) {
            return execContext.getTypeReference().getKeYJavaType();
        } else if (refPrefix instanceof ThisReference) {
            return ((TypeReference) refPrefix.getReferencePrefix()).getKeYJavaType();
            // ((ProgramVariable)
            // services.getTypeConverter().convertToLogicElement(refPrefix).op()).getKeYJavaType();
        } else if (refPrefix instanceof TypeRef) {
            KeYJavaType t = ((TypeRef) refPrefix).getKeYJavaType();
            if (t == null) { // %%%
                Debug.fail();
            }
            return t;
        } else if (refPrefix instanceof ProgramVariable) {
            return ((ProgramVariable) refPrefix).getKeYJavaType();
        } else if (refPrefix instanceof FieldReference) {
            return ((FieldReference) refPrefix).getProgramVariable().getKeYJavaType();
        } else if (refPrefix instanceof SuperReference) {
            KeYJavaType st = services.getJavaInfo()
                    .getSuperclass(execContext.getTypeReference().getKeYJavaType());
            return st;
        } else {
            throw new IllegalArgumentException(
                    "Unsupported method invocation mode\n" + refPrefix.getClass());
        }
    }

    private MethodBodyStatement handleSuperReference(Services services, ExecutionContext execContext, MethodReference methRef, ProgramVariable pvar, ImmutableArray<Expression> arguments) {
        IProgramMethod superMethod = getSuperMethod(execContext, methRef, services);
        return KeYJavaASTFactory.methodBody(pvar, execContext.getRuntimeInstance(), superMethod, arguments);
    }

    private IProgramMethod getSuperMethod(ExecutionContext ex, MethodReference mr,
                                          Services services) {
        return mr.method(services, getSuperType(ex, services), ex);
    }

    private KeYJavaType getSuperType(ExecutionContext ex, Services services) {
        return services.getJavaInfo().getSuperclass(ex.getTypeReference().getKeYJavaType());
    }

}
