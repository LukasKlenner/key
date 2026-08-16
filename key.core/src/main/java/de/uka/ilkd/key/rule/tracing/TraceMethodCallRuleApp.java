 /* This file is part of KeY - https://key-project.org
  * KeY is licensed under the GNU General Public License Version 2
  * SPDX-License-Identifier: GPL-2.0-only */
 package de.uka.ilkd.key.rule.tracing;

 import de.uka.ilkd.key.java.JavaTools;
 import de.uka.ilkd.key.java.Services;
 import de.uka.ilkd.key.java.ast.expression.operator.CopyAssignment;
 import de.uka.ilkd.key.java.ast.reference.ExecutionContext;
 import de.uka.ilkd.key.java.ast.reference.FieldReference;
 import de.uka.ilkd.key.java.ast.reference.MethodReference;
 import de.uka.ilkd.key.java.ast.reference.ReferencePrefix;
 import de.uka.ilkd.key.java.ast.reference.ThisReference;
 import de.uka.ilkd.key.logic.JTerm;
 import de.uka.ilkd.key.logic.TermBuilder;
 import de.uka.ilkd.key.logic.TermServices;
 import de.uka.ilkd.key.logic.op.IProgramMethod;
 import de.uka.ilkd.key.logic.op.ProgramVariable;
 import de.uka.ilkd.key.proof.Goal;
 import de.uka.ilkd.key.proof.tracing.FunctionDatabase;
 import de.uka.ilkd.key.proof.tracing.TraceElement;
 import de.uka.ilkd.key.proof.tracing.TracingState;
 import de.uka.ilkd.key.rule.AbstractBuiltInRuleApp;
 import org.jspecify.annotations.NullMarked;
 import org.jspecify.annotations.Nullable;
 import org.key_project.prover.sequent.PosInOccurrence;
 import org.key_project.util.collection.ImmutableList;

 import java.util.Objects;

 import static de.uka.ilkd.key.rule.tracing.TraceMethodCallRule.extractExecutionContext;

 @NullMarked
 public class TraceMethodCallRuleApp extends AbstractBuiltInRuleApp<TraceMethodCallRule> {

     private final TermServices services;

     // --- Extracted from the sequent focus ---
     private @Nullable MethodReference methodReference;
     private @Nullable ProgramVariable resultVariable;
     private @Nullable ExecutionContext executionContext;

     // --- Resolved ---
     private @Nullable ReferencePrefix resolvedReceiver;
     private @Nullable IProgramMethod resolvedTargetMethod;

     // ========================================================================
     // Constructors
     // ========================================================================

     public TraceMethodCallRuleApp(TraceMethodCallRule rule, @Nullable PosInOccurrence pos,
                                   TermServices services) {
         this(rule, pos, null, services);
     }

     private TraceMethodCallRuleApp(TraceMethodCallRule rule, @Nullable PosInOccurrence pos,
                                    @Nullable ImmutableList<PosInOccurrence> ifInsts,
                                    TermServices services) {
         super(rule, pos, ifInsts);
         this.services = services;
     }

     // ========================================================================
     // Completeness
     // ========================================================================

     /**
      * The rule app is complete when we have successfully resolved a concrete
      * implementation from the trace.
      */
     @Override
     public boolean complete() {
         return methodReference != null &&
                 resolvedReceiver != null &&
                 resolvedTargetMethod != null;
     }

     // ========================================================================
     // Instantiation
     // ========================================================================

     @Override
     public TraceMethodCallRuleApp tryToInstantiate(Goal goal) {
         final Services services = goal.proof().getServices();

         extractMethodCall();

         if (methodReference == null) {
             return this;
         }

         this.executionContext = extractExecutionContext(services, programTerm());
         this.resolvedReceiver = resolveReceiver(services);
         this.resolvedTargetMethod = resolveTargetMethod(services);

         return this;
     }

     /**
      * Extracts the method reference, result variable, execution context, and
      * static prefix type from the active statement at the focus position.
      */
     private void extractMethodCall() {
         JTerm progPost = programTerm();
         if (progPost == null) {
             return;
         }

         var activeStatement = JavaTools.getActiveStatement(progPost.javaBlock());

         // Shape 1: obj.m(args)  — active statement is a MethodReference
         if (activeStatement instanceof MethodReference mr) {
             this.methodReference = mr;
             this.resultVariable = null;
         }
         // Shape 2: x = obj.m(args) — active statement is a CopyAssignment
         else if (activeStatement instanceof CopyAssignment ca
                 && ca.getChildCount() >= 2
                 && ca.getChildAt(1) instanceof MethodReference mr) {
             this.methodReference = mr;
             if (ca.getChildAt(0) instanceof ProgramVariable pv) {
                 this.resultVariable = pv;
             }
         }
     }

     /**
      * Determines the reference prefix (receiver) for the resolved method,
      * mirroring the logic in {@code MethodCall.transformImpl}.
      */
     private @Nullable ReferencePrefix resolveReceiver(Services services) {
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

     private @Nullable IProgramMethod resolveTargetMethod(Services services) {
         if (methodReference == null || executionContext == null) {
             return null;
         }

         TracingState tracingState = services.getTracingState();
         FunctionDatabase functionDatabase = tracingState.getFunctionDatabase();

         TraceElement nextTraceElement = tracingState.getNextTraceElement();
         if (nextTraceElement instanceof TraceElement.Call(int functionID)) {
             return functionDatabase.getProgramMethodById(functionID, services, executionContext, methodReference);
         }

         return null;
     }

     // ========================================================================
     // Term access
     // ========================================================================

     /**
      * Returns the program term (below updates) at the focus position.
      */
     public @Nullable JTerm programTerm() {
         if (posInOccurrence() != null) {
             return TermBuilder.goBelowUpdates(
                     (JTerm) posInOccurrence().subTerm());
         }
         return null;
     }

     // ========================================================================
     // Accessors for the Rule
     // ========================================================================

     public MethodReference getMethodReference() {
         return Objects.requireNonNull(methodReference);
     }

     public @Nullable ProgramVariable getResultVariable() {
         return resultVariable;
     }

     public ExecutionContext getExecutionContext() {
         return Objects.requireNonNull(executionContext);
     }

     public IProgramMethod getResolvedTargetMethod() {
         return Objects.requireNonNull(resolvedTargetMethod);
     }

     public ReferencePrefix getResolvedReceiver() {
         return Objects.requireNonNull(resolvedReceiver);
     }


     // ========================================================================
     // AbstractBuiltInRuleApp overrides
     // ========================================================================

     @Override
     public TraceMethodCallRuleApp replacePos(PosInOccurrence newPos) {
         return new TraceMethodCallRuleApp(builtInRule, newPos, ifInsts, services);
     }

     @Override
     public TraceMethodCallRuleApp setAssumesInsts(
             ImmutableList<PosInOccurrence> ifInsts) {
         setMutable(ifInsts);
         return this;
     }
 }