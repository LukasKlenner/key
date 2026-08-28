package de.uka.ilkd.key.rule.tracing;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.ast.abstraction.KeYJavaType;
import de.uka.ilkd.key.java.ast.reference.ArrayReference;
import de.uka.ilkd.key.java.ast.reference.ExecutionContext;
import de.uka.ilkd.key.java.ast.reference.FieldReference;
import de.uka.ilkd.key.java.ast.reference.MethodReference;
import de.uka.ilkd.key.java.ast.reference.ReferencePrefix;
import de.uka.ilkd.key.java.ast.reference.SuperReference;
import de.uka.ilkd.key.java.ast.reference.ThisReference;
import de.uka.ilkd.key.java.ast.reference.TypeRef;
import de.uka.ilkd.key.java.ast.reference.TypeReference;
import de.uka.ilkd.key.logic.op.IProgramMethod;
import de.uka.ilkd.key.logic.op.ProgramVariable;
import org.jspecify.annotations.Nullable;

public class TracingRuleUtil {

    public static boolean isUntracedMethod(IProgramMethod method) {
        return method.getName().startsWith("$") || method.isStatic() || method.isPrivate();
    }

    public static @Nullable IProgramMethod getStaticReferencedMethod(MethodReference methodRef, Services services, ExecutionContext executionContext) {
        KeYJavaType staticPrefixType = getStaticPrefixType(methodRef, services, executionContext);
        if (staticPrefixType == null) {
            return null;
        }
        return methodRef.method(services, staticPrefixType, executionContext);
    }

    /**
     * Determines the static prefix type from the reference prefix,
     * mirroring {@code MethodCall.getStaticPrefixType}.
     */
    public static @Nullable KeYJavaType getStaticPrefixType(
            MethodReference methodRef,
            Services services,
            ExecutionContext executionContext
    ) {
        ReferencePrefix refPrefix = methodRef.getReferencePrefix();

        if (refPrefix == null
                || (refPrefix instanceof ThisReference
                && refPrefix.getReferencePrefix() == null)) {
            return executionContext.getTypeReference().getKeYJavaType();
        } else if (refPrefix instanceof ThisReference) {
            return ((TypeReference) refPrefix.getReferencePrefix()).getKeYJavaType();
        } else if (refPrefix instanceof TypeRef tr) {
            return tr.getKeYJavaType();
        } else if (refPrefix instanceof ProgramVariable pv) {
            return pv.getKeYJavaType();
        } else if (refPrefix instanceof FieldReference fr) {
            return fr.getProgramVariable().getKeYJavaType();
        } else if (refPrefix instanceof ArrayReference ar) {
            return ar.getKeYJavaType(services, executionContext);
        } else if (refPrefix instanceof SuperReference) {
            return services.getJavaInfo().getSuperclass(
                    executionContext.getTypeReference().getKeYJavaType());
        } else {
            throw new IllegalArgumentException(
                    "Unsupported reference prefix: " + refPrefix.getClass());
        }
    }
}
