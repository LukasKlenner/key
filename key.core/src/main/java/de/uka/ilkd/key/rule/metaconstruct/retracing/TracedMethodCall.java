package de.uka.ilkd.key.rule.metaconstruct.retracing;

import de.uka.ilkd.key.java.KeYJavaASTFactory;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.ast.ProgramElement;
import de.uka.ilkd.key.java.ast.expression.Expression;
import de.uka.ilkd.key.java.ast.reference.ExecutionContext;
import de.uka.ilkd.key.java.ast.reference.FieldReference;
import de.uka.ilkd.key.java.ast.reference.MethodReference;
import de.uka.ilkd.key.java.ast.reference.ReferencePrefix;
import de.uka.ilkd.key.java.ast.reference.ThisReference;
import de.uka.ilkd.key.java.ast.statement.MethodBodyStatement;
import de.uka.ilkd.key.logic.op.IProgramMethod;
import de.uka.ilkd.key.logic.op.ProgramSV;
import de.uka.ilkd.key.logic.op.ProgramVariable;
import de.uka.ilkd.key.proof.retracing.FunctionDatabase;
import de.uka.ilkd.key.proof.retracing.TraceElement;
import de.uka.ilkd.key.proof.retracing.TracingState;
import de.uka.ilkd.key.rule.inst.SVInstantiations;
import de.uka.ilkd.key.rule.metaconstruct.ProgramTransformer;
import org.jspecify.annotations.Nullable;
import org.key_project.logic.Name;
import org.key_project.logic.op.sv.SchemaVariable;
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

        return new MethodBodyStatement[]{
                KeYJavaASTFactory.methodBody(
                        pvar,
                        resolveReceiver(services, execContext, methRef),
                        resolveTargetMethod(services, execContext, methRef),
                        (ImmutableArray<Expression>) methRef.getArguments()
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
}
