package de.uka.ilkd.key.proof.tracing;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.ast.abstraction.KeYJavaType;
import de.uka.ilkd.key.java.ast.reference.ExecutionContext;
import de.uka.ilkd.key.java.ast.reference.MethodReference;
import de.uka.ilkd.key.logic.op.IProgramMethod;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class FunctionDatabase {

    private final Map<Integer, String> functionMap;

    private final Map<Integer, IProgramMethod> cachedMethods = new HashMap<>();

    public FunctionDatabase(Map<Integer, String> functionMap) {
        this.functionMap = functionMap;
    }

    public IProgramMethod getProgramMethodById(int id, Services services, ExecutionContext executionContext, MethodReference methodReference) {
        if (cachedMethods.containsKey(id)) {
            return cachedMethods.get(id);
        }

        String traceSignature = functionMap.get(id);

        if (traceSignature == null) {
            throw new IllegalArgumentException("No function signature found for ID: " + id);
        }

        // Parse "package.ConcreteImpl#method(String[]):void"
        String className = traceSignature.substring(0, traceSignature.indexOf('#'));

        KeYJavaType concreteType = services.getJavaInfo().getKeYJavaType(className);
        if (concreteType == null) {
            throw new IllegalArgumentException("No concrete type found for class: " + className);
        }

        IProgramMethod method = resolveMethodOnType(concreteType, services, executionContext, methodReference);
        if (method == null) {
            throw new IllegalArgumentException("No method found for signature: " + traceSignature + " on type: " + concreteType);
        }

        cachedMethods.put(id, method);
        return method;
    }

    /**
     * Resolves the method on a specific concrete type.
     */
    private @Nullable IProgramMethod resolveMethodOnType(
            KeYJavaType type, Services services, ExecutionContext executionContext, MethodReference methodReference) {
        if (executionContext != null) {
            return methodReference.method(services, type, executionContext);
        } else {
            return methodReference.method(services, type,
                    methodReference.getMethodSignature(services, null), type);
        }
    }

}
