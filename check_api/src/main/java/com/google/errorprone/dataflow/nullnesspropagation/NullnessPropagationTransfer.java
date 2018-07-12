/*
 * Copyright 2014 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.dataflow.nullnesspropagation.Nullness.BOTTOM;
import static com.google.errorprone.dataflow.nullnesspropagation.Nullness.NONNULL;
import static com.google.errorprone.dataflow.nullnesspropagation.Nullness.NULL;
import static com.google.errorprone.dataflow.nullnesspropagation.Nullness.NULLABLE;
import static com.sun.tools.javac.code.TypeTag.BOOLEAN;
import static javax.lang.model.element.ElementKind.EXCEPTION_PARAMETER;
import static javax.lang.model.element.ElementKind.TYPE_PARAMETER;
import static org.checkerframework.javacutil.TreeUtils.elementFromDeclaration;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.io.Files;
import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import com.google.errorprone.dataflow.AccessPath;
import com.google.errorprone.dataflow.AccessPathStore;
import com.google.errorprone.dataflow.AccessPathValues;
import com.google.errorprone.dataflow.LocalVariableValues;
import com.google.errorprone.util.MoreAnnotations;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.Context;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.cfg.CFGBuilder;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.node.ArrayAccessNode;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.EqualToNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.FunctionalInterfaceNode;
import org.checkerframework.dataflow.cfg.node.InstanceOfNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NotEqualNode;
import org.checkerframework.dataflow.cfg.node.TypeCastNode;
import org.checkerframework.dataflow.cfg.node.VariableDeclarationNode;

/**
 * The {@code TransferFunction} for our nullability analysis. This analysis determines, for all
 * variables and parameters, whether they are definitely null ({@link Nullness#NULL}), definitely
 * non-null ({@link Nullness#NONNULL}), possibly null ({@link Nullness#NULLABLE}), or are on an
 * infeasible path ({@link Nullness#BOTTOM}). This analysis depends only on the code and does not
 * take nullness annotations into account.
 *
 * <p>Each {@code visit*()} implementation provides us with information about nullability that
 * applies <i>if the expression is successfully evaluated</i> (in other words, it did not throw an
 * exception). For example, if {@code foo.toString()} is successfully evaluated, we know two things:
 *
 * <ol>
 *   <li>The expression itself is non-null (because {@code toString()} is in our whitelist of
 *       methods known to return non-null values)
 *   <li>{@code foo} is non-null (because it has been dereferenced without producing a {@code
 *       NullPointerException})
 * </ol>
 *
 * <p>These particular two pieces of data also demonstrate the two connected but distinct systems
 * that we use to track nullness:
 *
 * <ol>
 *   <li>We compute the nullability of each expression by applying rules that may reference only the
 *       nullability of <i>subexpressions</i>. We make the result available only to superexpressions
 *       (and to the {@linkplain Analysis#getValue final output of the analysis}).
 *   <li>We {@linkplain Updates update} and {@linkplain AccessPathValues read} the nullability of
 *       <i>variables</i> and <i>access paths</i> in a mapping that persists from node to node. This
 *       is the only exception to the rule that we propagate data from subexpression to
 *       superexpression only. The mapping is read only when visiting a {@link LocalVariableNode} or
 *       {@link FieldAccessNode}. That is enough to give the Node a value that is then available to
 *       superexpressions.
 * </ol>
 *
 * <p>A further complication is that sometimes we know the nullability of an expression only
 * conditionally based on its result. For example, {@code foo == null} proves that {@code foo} is
 * null in the true case (such as inside {@code if (foo == null) { ... }}) and non-null in the false
 * case (such an inside an accompanying {@code else} block). This is handled by methods that accept
 * multiple {@link Updates} instances, one for the true case and one for the false case.
 *
 * @author deminguyen@google.com (Demi Nguyen)
 */
class NullnessPropagationTransfer extends AbstractNullnessPropagationTransfer
    implements Serializable {

  private static final long serialVersionUID = -2413953917354086984L;

  /** Matches methods that are statically known never to return null. */
  private static class ReturnValueIsNonNull implements Predicate<MethodInfo>, Serializable {

    private static final long serialVersionUID = -6277529478866058532L;

    private static final ImmutableSet<MemberName> METHODS_WITH_NON_NULLABLE_RETURNS =
        ImmutableSet.of(
            // We would love to include all the methods of Files, but readFirstLine can return null.
            member(Files.class.getName(), "toString"),
            // Some methods of Class can return null, e.g. getAnnotation, getCanonicalName
            member(Class.class.getName(), "getName"),
            member(Class.class.getName(), "getSimpleName"),
            member(Class.class.getName(), "forName"),
            member(Charset.class.getName(), "forName"));
    // TODO(cpovirk): respect nullness annotations (and also check them to ensure correctness!)

    private static final ImmutableSet<String> CLASSES_WITH_NON_NULLABLE_RETURNS =
        ImmutableSet.of(
            Optional.class.getName(),
            Preconditions.class.getName(),
            Verify.class.getName(),
            String.class.getName(),
            BigInteger.class.getName(),
            BigDecimal.class.getName(),
            UnsignedInteger.class.getName(),
            UnsignedLong.class.getName(),
            Objects.class.getName());

    private static final ImmutableSet<String> CLASSES_WITH_NON_NULLABLE_VALUE_OF_METHODS =
        ImmutableSet.of(
            // The primitive types.
            Boolean.class.getName(),
            Byte.class.getName(),
            Character.class.getName(),
            Double.class.getName(),
            Float.class.getName(),
            Integer.class.getName(),
            Long.class.getName(),
            Short.class.getName(),

            // Other types.
            // TODO(cpovirk): recognize the compiler-generated valueOf() methods on Enum subclasses
            Enum.class.getName(),
            String.class.getName());

    @Override
    public boolean apply(MethodInfo methodInfo) {
      // Any method explicitly annotated with @Nullable is assumed to be capable of returning
      // null.
      for (String annotation : methodInfo.annotations()) {
        if (annotation.endsWith(".Nullable") || annotation.endsWith(".NullableDecl")) {
          return false;
        }
      }

      if (methodInfo.method().equals("valueOf")
          && CLASSES_WITH_NON_NULLABLE_VALUE_OF_METHODS.contains(methodInfo.clazz())) {
        return true;
      }
      if (methodInfo.isPrimitive()) {
        return true;
      }
      if (methodInfo.isKnownNonNullReturning()) {
        return true;
      }
      if (CLASSES_WITH_NON_NULLABLE_RETURNS.contains(methodInfo.clazz())) {
        return true;
      }
      MemberName searchMemberName = new MemberName(methodInfo.clazz(), methodInfo.method());
      if (METHODS_WITH_NON_NULLABLE_RETURNS.contains(searchMemberName)) {
        return true;
      }

      return false;
    }
  }

  private final transient Set<VarSymbol> traversed = new HashSet<>();

  private final Nullness defaultAssumption;
  private final Predicate<MethodInfo> methodReturnsNonNull;

  /**
   * Javac context so we can {@link #fieldInitializerNullnessIfAvailable find and analyze field
   * initializers}.
   */
  private transient Context context;

  /** Compilation unit to limit evaluating field initializers to. */
  private transient CompilationUnitTree compilationUnit;

  /**
   * Constructs a {@link NullnessPropagationTransfer} instance with the built-in set of non-null
   * returning methods.
   */
  public NullnessPropagationTransfer() {
    this(NULLABLE, new ReturnValueIsNonNull());
  }

  /**
   * Constructs a {@link NullnessPropagationTransfer} instance with additional non-null returning
   * methods. The additional predicate is or'ed with the predicate for the built-in set of non-null
   * returning methods.
   */
  public NullnessPropagationTransfer(Predicate<MethodInfo> additionalNonNullReturningMethods) {
    this(NULLABLE, Predicates.or(new ReturnValueIsNonNull(), additionalNonNullReturningMethods));
  }

  /**
   * Constructor for use by subclasses.
   *
   * @param defaultAssumption used if a field or method can't be resolved as well as as the default
   *     for local variables, field and array reads in the absence of better information
   * @param methodReturnsNonNull determines whether a method's return value is known to be non-null
   */
  protected NullnessPropagationTransfer(
      Nullness defaultAssumption, Predicate<MethodInfo> methodReturnsNonNull) {
    this.defaultAssumption = defaultAssumption;
    this.methodReturnsNonNull = methodReturnsNonNull;
  }

  /**
   * Stores the given Javac context to find and analyze field initializers. Set before analyzing a
   * method and reset after.
   */
  NullnessPropagationTransfer setContext(@Nullable Context context) {
    // This is a best-effort check (similar to ArrayList iterators, for instance), no guarantee
    Preconditions.checkArgument(
        context == null || this.context == null,
        "Context already set: reset after use and don't use this class concurrently");
    this.context = context;
    // Clear traversed set just-in-case as this marks the beginning or end of analyzing a method
    this.traversed.clear();
    return this;
  }

  /**
   * Set compilation unit being analyzed, to limit analyzing field initializers to that compilation
   * unit. Analyzing initializers from other compilation units tends to fail because type
   * information is sometimes missing on nodes returned from {@link Trees}.
   */
  NullnessPropagationTransfer setCompilationUnit(@Nullable CompilationUnitTree compilationUnit) {
    this.compilationUnit = compilationUnit;
    return this;
  }

  // Literals

  @Override
  Nullness visitThisLiteral() {
    return NONNULL;
  }

  @Override
  Nullness visitSuper() {
    return NONNULL;
  }

  /**
   * Note: A short literal appears as an int to the compiler, and the compiler can perform a
   * narrowing conversion on the literal to cast from int to short. For example, when assigning a
   * literal to a short variable, the literal does not transfer its own non-null type to the
   * variable. Instead, the variable receives the non-null type from the return value of the
   * conversion call.
   */
  @Override
  Nullness visitValueLiteral() {
    return NONNULL;
  }

  @Override
  Nullness visitNullLiteral() {
    return NULL;
  }

  @Override
  Nullness visitBitwiseOperation() {
    return NONNULL;
  }

  @Override
  Nullness visitNumericalComparison() {
    return NONNULL;
  }

  @Override
  Nullness visitNumericalOperation() {
    return NONNULL;
  }

  @Override
  Nullness visitInstanceOf(
      InstanceOfNode node, SubNodeValues inputs, Updates thenUpdates, Updates elseUpdates) {
    setNonnullIfTrackable(thenUpdates, node.getOperand());
    return NONNULL; // the result of an instanceof is a primitive boolean, so it's non-null
  }

  @Override
  Nullness visitTypeCast(TypeCastNode node, SubNodeValues inputs) {
    return hasPrimitiveType(node) ? NONNULL : inputs.valueOfSubNode(node.getOperand());
  }

  /**
   * The result of string concatenation is always non-null. If an operand is {@code null}, it is
   * converted to {@code "null"}. For more information, see JLS 15.18.1 "String Concatenation
   * Operator +", and 5.1.11, "String Conversion".
   */
  @Override
  Nullness visitStringConcatenate() {
    // TODO(user): Mark the inputs as dereferenced.
    return NONNULL;
  }

  @Override
  Nullness visitStringConversion() {
    return NONNULL;
  }

  @Override
  Nullness visitNarrowingConversion() {
    return NONNULL;
  }

  @Override
  Nullness visitWideningConversion() {
    return NONNULL;
  }

  @Override
  void visitEqualTo(
      EqualToNode node, SubNodeValues inputs, Updates thenUpdates, Updates elseUpdates) {
    handleEqualityComparison(
        /* equalTo= */ true,
        node.getLeftOperand(),
        node.getRightOperand(),
        inputs,
        thenUpdates,
        elseUpdates);
  }

  @Override
  void visitNotEqual(
      NotEqualNode node, SubNodeValues inputs, Updates thenUpdates, Updates elseUpdates) {
    handleEqualityComparison(
        /* equalTo= */ false,
        node.getLeftOperand(),
        node.getRightOperand(),
        inputs,
        thenUpdates,
        elseUpdates);
  }

  @Override
  Nullness visitAssignment(AssignmentNode node, SubNodeValues inputs, Updates updates) {
    Nullness value = inputs.valueOfSubNode(node.getExpression());

    Node target = node.getTarget();
    if (target instanceof LocalVariableNode) {
      updates.set((LocalVariableNode) target, value);
    }

    if (target instanceof ArrayAccessNode) {
      setNonnullIfTrackable(updates, ((ArrayAccessNode) target).getArray());
    }

    if (target instanceof FieldAccessNode) {
      FieldAccessNode fieldAccess = (FieldAccessNode) target;
      if (!fieldAccess.isStatic()) {
        setNonnullIfTrackable(updates, fieldAccess.getReceiver());
      }
      /* NOTE: This transfer function makes the unsound assumption that the {@code fieldAccess}
       * memory cell does not alias any element of other tracked access paths.  To be sound, we
       * would have to "forget" all information about any access path that could contain an alias
       * of {@code fieldAccess} in non-terminal position (by setting it to NULLABLE) and perform a
       * weak update of {@code value} into any access path that could alias {@code fieldAccess} in
       * full (by setting its value to the join of its current value and {@code value}).
       */
      updates.set(fieldAccess, value);
    }

    /*
     * We propagate the value of the target to the value of the assignment expressions as a whole.
     * We do this regardless of whether the target is a local variable. For example:
     *
     * String s = object.field = "foo"; // Now |s| is non-null.
     *
     * It's not clear to me that this is technically correct, but it works in practice with the
     * bytecode generated by both javac and ecj.
     *
     * http://stackoverflow.com/q/12850676/28465
     */
    return value;
  }

  /**
   * Variables take their values from their past assignments (as far as they can be determined).
   * Additionally, variables of primitive type are always refined to non-null.
   *
   * <p>(This second case is rarely of interest to us. Either the variable is being used as a
   * primitive, in which case we probably wouldn't have bothered to run the nullness checker on it,
   * or it's being used as an Object, in which case the compiler generates a call to {@code valueOf}
   * (to autobox the value), which triggers {@link #visitMethodInvocation}.)
   *
   * <p>Edge case: {@code node} can be a captured local variable accessed from inside a local or
   * anonymous inner class, or possibly from inside a lambda expression (even though these manifest
   * as fields in bytecode). As of 7/2016 this analysis doesn't have any knowledge of captured local
   * variables will essentially assume whatever default is used in {@link #values}.
   */
  @Override
  Nullness visitLocalVariable(LocalVariableNode node, LocalVariableValues<Nullness> values) {
    return hasPrimitiveType(node) || hasNonNullConstantValue(node)
        ? NONNULL
        : values.valueOfLocalVariable(node, defaultAssumption);
  }

  /**
   * Refines the receiver of a field access to type non-null after a successful field access, and
   * refines the value of the expression as a whole to non-null if applicable (e.g., if the field
   * has a primitive type or the {@code store}) has a non-null value for this access path.
   *
   * <p>Note: If the field access occurs when the node is an l-value, the analysis won't call this
   * method. Instead, it will call {@link #visitAssignment}.
   */
  @Override
  Nullness visitFieldAccess(
      FieldAccessNode node, Updates updates, AccessPathValues<Nullness> store) {
    if (!node.isStatic()) {
      setNonnullIfTrackable(updates, node.getReceiver());
    }
    ClassAndField accessed = tryGetFieldSymbol(node.getTree());
    return fieldNullness(accessed, AccessPath.fromFieldAccess(node), store);
  }

  /**
   * Refines the accessed array to non-null after a successful array access.
   *
   * <p>Note: If the array access occurs when the node is an l-value, the analysis won't call this
   * method. Instead, it will call {@link #visitAssignment}.
   */
  @Override
  Nullness visitArrayAccess(ArrayAccessNode node, SubNodeValues inputs, Updates updates) {
    setNonnullIfTrackable(updates, node.getArray());
    return hasPrimitiveType(node) ? NONNULL : defaultAssumption;
  }

  /**
   * Refines the receiver of a method invocation to type non-null after successful invocation, and
   * refines the value of the expression as a whole to non-null if applicable (e.g., if the method
   * returns a primitive type).
   *
   * <p>NOTE: This transfer makes the unsound assumption that fields reachable via the actual params
   * of this method invocation are not mutated by the callee. To be sound with respect to escaping
   * mutable references in general, we would have to set to top (i.e. NULLABLE) any tracked access
   * path that could contain an alias of an actual parameter of this invocation.
   */
  @Override
  Nullness visitMethodInvocation(
      MethodInvocationNode node, Updates thenUpdates, Updates elseUpdates, Updates bothUpdates) {
    ClassAndMethod callee = tryGetMethodSymbol(node.getTree(), Types.instance(context));
    if (callee != null && !callee.isStatic) {
      setNonnullIfTrackable(bothUpdates, node.getTarget().getReceiver());
    }
    setUnconditionalArgumentNullness(bothUpdates, node.getArguments(), callee);
    setConditionalArgumentNullness(
        thenUpdates,
        elseUpdates,
        node.getArguments(),
        callee,
        Types.instance(context),
        Symtab.instance(context));
    return returnValueNullness(callee, node);
  }

  @Override
  Nullness visitObjectCreation() {
    return NONNULL;
  }

  @Override
  Nullness visitClassDeclaration() {
    return NONNULL;
  }

  @Override
  Nullness visitArrayCreation(ArrayCreationNode node, SubNodeValues inputs, Updates updates) {
    return NONNULL;
  }

  @Override
  Nullness visitMemberReference(
      FunctionalInterfaceNode node, SubNodeValues inputs, Updates updates) {
    // TODO(kmb,cpovirk): Mark object member reference receivers as non-null
    return NONNULL; // lambdas and member references are never null :)
  }

  @Override
  void visitVariableDeclaration(
      VariableDeclarationNode node, SubNodeValues inputs, Updates updates) {
    /*
     * We could try to handle primitives here instead of in visitLocalVariable, but it won't be
     * enough because we don't see method parameters here.
     */
    if (isCatchVariable(node)) {
      updates.set(node, NONNULL);
    }
  }

  private static boolean isCatchVariable(VariableDeclarationNode node) {
    return elementFromDeclaration(node.getTree()).getKind() == EXCEPTION_PARAMETER;
  }

  /**
   * Refines the {@code Nullness} of {@code LocalVariableNode}s used in an equality comparison using
   * the greatest lower bound.
   *
   * @param equalTo whether the comparison is == (false for !=)
   * @param leftNode the left-hand side of the comparison
   * @param rightNode the right-hand side of the comparison
   * @param inputs access to nullness values of the left and right nodes
   * @param thenUpdates the local variables whose nullness values should be updated if the
   *     comparison returns {@code true}
   * @param elseUpdates the local variables whose nullness values should be updated if the
   *     comparison returns {@code false}
   */
  private static void handleEqualityComparison(
      boolean equalTo,
      Node leftNode,
      Node rightNode,
      SubNodeValues inputs,
      Updates thenUpdates,
      Updates elseUpdates) {
    Nullness leftVal = inputs.valueOfSubNode(leftNode);
    Nullness rightVal = inputs.valueOfSubNode(rightNode);
    Nullness equalBranchValue = leftVal.greatestLowerBound(rightVal);
    Updates equalBranchUpdates = equalTo ? thenUpdates : elseUpdates;
    Updates notEqualBranchUpdates = equalTo ? elseUpdates : thenUpdates;
    AccessPath leftOperand = AccessPath.fromNodeIfTrackable(leftNode);
    AccessPath rightOperand = AccessPath.fromNodeIfTrackable(rightNode);

    if (leftOperand != null) {
      equalBranchUpdates.set(leftOperand, equalBranchValue);
      notEqualBranchUpdates.set(
          leftOperand, leftVal.greatestLowerBound(rightVal.deducedValueWhenNotEqual()));
    }

    if (rightOperand != null) {
      equalBranchUpdates.set(rightOperand, equalBranchValue);
      notEqualBranchUpdates.set(
          rightOperand, rightVal.greatestLowerBound(leftVal.deducedValueWhenNotEqual()));
    }
  }

  private static boolean hasPrimitiveType(Node node) {
    return node.getType().getKind().isPrimitive();
  }

  private static boolean hasNonNullConstantValue(LocalVariableNode node) {
    if (node.getElement() instanceof VariableElement) {
      VariableElement element = (VariableElement) node.getElement();
      return (element.getConstantValue() != null);
    }
    return false;
  }

  private static ClassAndField tryGetFieldSymbol(Tree tree) {
    Symbol symbol = tryGetSymbol(tree);
    if (symbol instanceof VarSymbol) {
      return ClassAndField.make((VarSymbol) symbol);
    }
    return null;
  }

  static ClassAndMethod tryGetMethodSymbol(MethodInvocationTree tree, Types types) {
    Symbol symbol = tryGetSymbol(tree.getMethodSelect());
    if (symbol instanceof MethodSymbol) {
      return ClassAndMethod.make((MethodSymbol) symbol, types);
    }
    return null;
  }

  /*
   * We can't use ASTHelpers here. It's in core, which depends on jdk8, so we can't make jdk8 depend
   * back on core.
   */

  private static Symbol tryGetSymbol(Tree tree) {
    if (tree instanceof JCIdent) {
      return ((JCIdent) tree).sym;
    }
    if (tree instanceof JCFieldAccess) {
      return ((JCFieldAccess) tree).sym;
    }
    if (tree instanceof JCVariableDecl) {
      return ((JCVariableDecl) tree).sym;
    }
    return null;
  }

  Nullness fieldNullness(
      ClassAndField accessed, @Nullable AccessPath path, AccessPathValues<Nullness> store) {
    if (accessed == null) {
      return defaultAssumption;
    }

    if (accessed.field.equals("class")) {
      return NONNULL;
    }
    if (accessed.isEnumConstant()) {
      return NONNULL;
    }
    if (accessed.isPrimitive()) { // includes <array>.length
      return NONNULL;
    }
    if (accessed.hasNonNullConstantValue()) {
      return NONNULL;
    }
    if (accessed.isStatic() && accessed.isFinal()) {
      if (CLASSES_WITH_NON_NULL_CONSTANTS.contains(accessed.clazz)) {
        return NONNULL;
      }
      // Try to evaluate initializer.
      // TODO(kmb): Consider handling final instance fields as well
      Nullness initializer = fieldInitializerNullnessIfAvailable(accessed);
      if (initializer != null) {
        return initializer;
      }
    }

    // First, check the store for a dataflow-computed nullness value and return it if it exists
    // Otherwise, check for nullness annotations on the field declaration
    // If there are none, check for nullness annotations on generic type declarations
    // If there are none, fall back to the defaultAssumption

    Nullness dataflowResult = (path == null) ? BOTTOM : store.valueOfAccessPath(path, BOTTOM);

    if (dataflowResult != BOTTOM) {
      return dataflowResult;
    }

    java.util.Optional<Nullness> declaredNullness =
        nullnessFromAnnotations(
            MoreAnnotations.getDeclarationAndTypeAttributes(accessed.symbol)
                .map(Object::toString)
                .collect(Collectors.toList()));
    return declaredNullness.orElseGet(
        () ->
            nullnessFromAnnotations(inheritedAnnotations(accessed.symbol.type))
                .orElse(defaultAssumption));
  }

  private Nullness returnValueNullness(@Nullable ClassAndMethod callee, MethodInvocationNode node) {
    if (callee == null) {
      return defaultAssumption;
    }
    java.util.Optional<Nullness> declaredNullness = nullnessFromAnnotations(callee.annotations);
    if (declaredNullness.isPresent()) {
      return declaredNullness.get();
    }

    // If there is no nullness annotation on the callee method declaration, look for applicable
    // annotations inherited from elsewhere.
    List<String> annotations =
        ImmutableList.<String>builder()
            .addAll(inheritedAnnotations(node.getTarget().getMethod().getReturnType()))
            .addAll(
                node.getType()
                    .getAnnotationMirrors()
                    .stream()
                    .map(Object::toString)
                    .collect(Collectors.toList()))
            .build();

    return nullnessFromAnnotations(annotations)
        .orElse(methodReturnsNonNull.apply(callee) ? NONNULL : NULLABLE);
  }

  // TODO(bennostein): Support jsr305 @CheckForNull?
  // See CF Manual 3.7.2 for discussion of its idiosyncracy/incompatibility
  private static final Pattern ANNOTATION_RELEVANT_TO_NULLNESS =
      Pattern.compile(".*\\.(Nullable(Decl)?|NotNull|Nonnull|NonNull)$");

  private static final Pattern NULLABLE_ANNOTATION = Pattern.compile(".*\\.Nullable(Decl)?$");

  private static java.util.Optional<Nullness> nullnessFromAnnotations(List<String> annotations) {
    return annotations
        .stream()
        .filter(annot -> ANNOTATION_RELEVANT_TO_NULLNESS.matcher(annot).matches())
        .map(annot -> NULLABLE_ANNOTATION.matcher(annot).matches() ? NULLABLE : NONNULL)
        .reduce(Nullness::greatestLowerBound);
  }

  /**
   * Gathers all type annotations that are applicable to this TypeMirror and its bounds but are not
   * applied syntactically to its declaration. This includes:
   *
   * <ul>
   *   <li>annotations on type parameters at type use, e.g. {@code List<@Nullable String> xs = ...}
   *   <li>annotations on generic type declarations, e.g. {@code class MyClass<@Nullable T> {...} }
   *   <li>bounds on the above, e.g. {@code class MyClass<T extends @Nullable MyOtherClass> {...} }
   * </ul>
   */
  private static List<String> inheritedAnnotations(TypeMirror type) {
    ImmutableSet.Builder<AnnotationMirror> inheritedAnnotations = ImmutableSet.builder();

    // Annotations on type parameters at use-site
    inheritedAnnotations.addAll(type.getAnnotationMirrors());

    if (type.getKind() == TypeKind.TYPEVAR) {
      TypeVariable typeVar = (TypeVariable) type;
      // Annotations on bounds at type variable declaration
      inheritedAnnotations.addAll(typeVar.getUpperBound().getAnnotationMirrors());
      if (typeVar.asElement().getKind() == TYPE_PARAMETER) {
        Element genericElt = ((TypeParameterElement) typeVar.asElement()).getGenericElement();
        if (genericElt.getKind().isClass() || genericElt.getKind().isInterface()) {
          ((TypeElement) genericElt)
              .getTypeParameters()
              .stream()
              .filter(
                  typeParam ->
                      typeParam.getSimpleName().equals(typeVar.asElement().getSimpleName()))
              .findFirst()
              // Annotations at class/interface type variable declaration
              .ifPresent(decl -> inheritedAnnotations.addAll(decl.getAnnotationMirrors()));
        }
      }
    }
    return inheritedAnnotations.build().stream().map(Object::toString).collect(Collectors.toList());
  }

  @Nullable
  private Nullness fieldInitializerNullnessIfAvailable(ClassAndField accessed) {
    if (!traversed.add(accessed.symbol)) {
      // Circular dependency between initializers results in null.  Note static fields can also be
      // null if they're observed before initialized, but we're ignoring that case for simplicity.
      // TODO(kmb): Try to recognize problems with initialization order
      return NULL;
    }

    try {
      JavacProcessingEnvironment javacEnv = JavacProcessingEnvironment.instance(context);
      TreePath fieldDeclPath = Trees.instance(javacEnv).getPath(accessed.symbol);
      // Skip initializers in other compilation units as analysis of such nodes can fail due to
      // missing types.
      if (fieldDeclPath == null
          || fieldDeclPath.getCompilationUnit() != compilationUnit
          || !(fieldDeclPath.getLeaf() instanceof VariableTree)) {
        return null;
      }

      ExpressionTree initializer = ((VariableTree) fieldDeclPath.getLeaf()).getInitializer();
      if (initializer == null) {
        return null;
      }

      ClassTree classTree = (ClassTree) fieldDeclPath.getParentPath().getLeaf();

      // Run flow analysis on field initializer.  This is inefficient compared to just walking
      // the initializer expression tree but it avoids duplicating the logic from this transfer
      // function into a method that operates on Javac Nodes.
      TreePath initializerPath = TreePath.getPath(fieldDeclPath, initializer);
      UnderlyingAST ast = new UnderlyingAST.CFGStatement(initializerPath.getLeaf(), classTree);
      ControlFlowGraph cfg =
          CFGBuilder.build(
              initializerPath,
              ast,
              /*assumeAssertionsEnabled=*/ false,
              /*assumeAssertionsDisabled=*/ false,
              javacEnv);
      Analysis<Nullness, AccessPathStore<Nullness>, NullnessPropagationTransfer> analysis =
          new Analysis<>(this, javacEnv);
      analysis.performAnalysis(cfg);
      return analysis.getValue(initializerPath.getLeaf());
    } finally {
      traversed.remove(accessed.symbol);
    }
  }

  private static void setNonnullIfTrackable(Updates updates, Node node) {
    if (node instanceof LocalVariableNode) {
      updates.set((LocalVariableNode) node, NONNULL);
    } else if (node instanceof FieldAccessNode) {
      updates.set((FieldAccessNode) node, NONNULL);
    } else if (node instanceof VariableDeclarationNode) {
      updates.set((VariableDeclarationNode) node, NONNULL);
    }
  }

  /**
   * Records which arguments are guaranteed to be non-null if the method completes without
   * exception. For example, if {@code checkNotNull(foo, message)} completes successfully, then
   * {@code foo} is not null.
   */
  private static void setUnconditionalArgumentNullness(
      Updates bothUpdates, List<Node> arguments, ClassAndMethod callee) {
    Set<Integer> requiredNonNullParameters = REQUIRED_NON_NULL_PARAMETERS.get(callee.name());
    for (LocalVariableNode var : variablesAtIndexes(requiredNonNullParameters, arguments)) {
      bothUpdates.set(var, NONNULL);
    }
  }

  /**
   * Records which arguments are guaranteed to be non-null only if the method completes by returning
   * {@code true} or only if the method completes by returning {@code false}. For example, if {@code
   * Strings.isNullOrEmpty(s)} returns {@code false}, then {@code s} is not null.
   */
  private static void setConditionalArgumentNullness(
      Updates thenUpdates,
      Updates elseUpdates,
      List<Node> arguments,
      ClassAndMethod callee,
      Types types,
      Symtab symtab) {
    MemberName calleeName = callee.name();
    for (LocalVariableNode var :
        variablesAtIndexes(NULL_IMPLIES_TRUE_PARAMETERS.get(calleeName), arguments)) {
      elseUpdates.set(var, NONNULL);
    }
    for (LocalVariableNode var :
        variablesAtIndexes(NONNULL_IFF_TRUE_PARAMETERS.get(calleeName), arguments)) {
      thenUpdates.set(var, NONNULL);
      elseUpdates.set(var, NULL);
    }
    for (LocalVariableNode var :
        variablesAtIndexes(NULL_IFF_TRUE_PARAMETERS.get(calleeName), arguments)) {
      thenUpdates.set(var, NULL);
      elseUpdates.set(var, NONNULL);
    }
    if (isEqualsMethod(calleeName, arguments, types, symtab)) {
      LocalVariableNode var = variablesAtIndexes(ImmutableSet.of(0), arguments).get(0);
      thenUpdates.set(var, NONNULL);
    }
  }

  private static boolean isEqualsMethod(
      MemberName calleeName, List<Node> arguments, Types types, Symtab symtab) {
    // we don't care about class name -- we're matching against Object.equals(Object)
    // this implies that non-overriding methods are assumed to be null-guaranteeing.
    // Also see http://errorprone.info/bugpattern/NonOverridingEquals
    if (!calleeName.member.equals("equals") || arguments.size() != 1) {
      return false;
    }
    if (!(getOnlyElement(arguments).getTree() instanceof JCIdent)) {
      return false;
    }
    Symbol sym = ((JCIdent) getOnlyElement(arguments).getTree()).sym;
    if (sym == null || sym.type == null) {
      return false;
    }
    return types.isSameType(sym.type, symtab.objectType)
        && (!variablesAtIndexes(ImmutableSet.of(0), arguments).isEmpty());
  }

  private static List<LocalVariableNode> variablesAtIndexes(
      Set<Integer> indexes, List<Node> arguments) {
    List<LocalVariableNode> result = new ArrayList<>();
    for (Integer i : indexes) {
      if (i < 0) {
        i = arguments.size() + i;
      }
      // TODO(cpovirk): better handling of varargs
      if (i >= 0 && i < arguments.size()) {
        Node argument = arguments.get(i);
        if (argument instanceof LocalVariableNode) {
          result.add((LocalVariableNode) argument);
        }
      }
    }
    return result;
  }

  interface Member {
    boolean isStatic();
  }

  private static MemberName member(Class<?> clazz, String member) {
    return member(clazz.getName(), member);
  }

  private static MemberName member(String clazz, String member) {
    return new MemberName(clazz, member);
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    Preconditions.checkState(context == null, "Can't serialize while analyzing a method");
    Preconditions.checkState(compilationUnit == null, "Can't serialize while analyzing a method");
    out.defaultWriteObject();
  }

  @VisibleForTesting
  static final class MemberName {
    final String clazz;
    final String member;

    MemberName(String clazz, String member) {
      this.clazz = clazz;
      this.member = member;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof MemberName) {
        MemberName other = (MemberName) obj;
        return clazz.equals(other.clazz) && member.equals(other.member);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(clazz, member);
    }
  }

  static final class ClassAndMethod implements Member, MethodInfo {
    final String clazz;
    final String method;
    final ImmutableList<String> annotations;
    final boolean isStatic;
    final boolean isPrimitive;
    final boolean isBoolean;
    final boolean isNonNullReturning;

    private ClassAndMethod(
        String clazz,
        String method,
        ImmutableList<String> annotations,
        boolean isStatic,
        boolean isPrimitive,
        boolean isBoolean,
        boolean isNonNullReturning) {
      this.clazz = clazz;
      this.method = method;
      this.annotations = annotations;
      this.isStatic = isStatic;
      this.isPrimitive = isPrimitive;
      this.isBoolean = isBoolean;
      this.isNonNullReturning = isNonNullReturning;
    }

    static ClassAndMethod make(MethodSymbol methodSymbol, @Nullable Types types) {
      // TODO(b/71812955): consider just wrapping methodSymbol instead of copying everything out.
      // TODO(b/71812955): for type variables, check for type annotations on the referenced variable
      ImmutableList<String> annotations =
          MoreAnnotations.getDeclarationAndTypeAttributes(methodSymbol)
              .map(c -> c.getAnnotationType().asElement().toString())
              .collect(toImmutableList());

      ClassSymbol clazzSymbol = (ClassSymbol) methodSymbol.owner;
      return new ClassAndMethod(
          clazzSymbol.getQualifiedName().toString(),
          methodSymbol.getSimpleName().toString(),
          annotations,
          methodSymbol.isStatic(),
          methodSymbol.getReturnType().isPrimitive(),
          methodSymbol.getReturnType().getTag() == BOOLEAN,
          knownNonNullMethod(methodSymbol, clazzSymbol, types));
    }

    private static boolean knownNonNullMethod(
        MethodSymbol methodSymbol, ClassSymbol clazzSymbol, @Nullable Types types) {
      if (types == null) {
        return false;
      }

      // Proto getters are not null
      if (methodSymbol.name.toString().startsWith("get")
          && methodSymbol.params().isEmpty()
          && !methodSymbol.isStatic()) {
        Type type = clazzSymbol.type;
        while (type != null) {
          TypeSymbol typeSymbol = type.asElement();
          if (typeSymbol == null) {
            break;
          }
          if (typeSymbol
              .getQualifiedName()
              .contentEquals("com.google.protobuf.AbstractMessageLite")) {
            return true;
          }
          type = types.supertype(type);
        }
      }
      return false;
    }

    @Override
    public boolean isStatic() {
      return isStatic;
    }

    MemberName name() {
      return new MemberName(this.clazz, this.method);
    }

    @Override
    public String clazz() {
      return clazz;
    }

    @Override
    public String method() {
      return method;
    }

    @Override
    public ImmutableList<String> annotations() {
      return annotations;
    }

    @Override
    public boolean isPrimitive() {
      return isPrimitive;
    }

    @Override
    public boolean isKnownNonNullReturning() {
      return isNonNullReturning;
    }
  }

  static final class ClassAndField implements Member {
    final VarSymbol symbol;
    final String clazz;
    final String field;

    private ClassAndField(VarSymbol symbol) {
      this.symbol = symbol;
      this.clazz = symbol.owner.getQualifiedName().toString();
      this.field = symbol.getSimpleName().toString();
    }

    static ClassAndField make(VarSymbol symbol) {
      return new ClassAndField(symbol);
    }

    @Override
    public boolean isStatic() {
      return symbol.isStatic();
    }

    public boolean isFinal() {
      return (symbol.flags() & Flags.FINAL) == Flags.FINAL;
    }

    public boolean isPrimitive() {
      return symbol.type.isPrimitive();
    }

    public boolean isEnumConstant() {
      return symbol.isEnum();
    }

    public boolean hasNonNullConstantValue() {
      return symbol.getConstValue() != null;
    }
  }

  /** Classes where we know that all static final fields are non-null. */
  @VisibleForTesting
  static final ImmutableSet<String> CLASSES_WITH_NON_NULL_CONSTANTS =
      ImmutableSet.of(
          BigInteger.class.getName(),
          BigDecimal.class.getName(),
          UnsignedInteger.class.getName(),
          UnsignedLong.class.getName(),
          StandardCharsets.class.getName());

  /**
   * Maps from the names of null-rejecting methods to the indexes of the arguments that aren't
   * permitted to be null. Indexes may be negative to indicate a position relative to the end of the
   * argument list. For example, "-1" means "the final parameter."
   */
  @VisibleForTesting
  static final ImmutableSetMultimap<MemberName, Integer> REQUIRED_NON_NULL_PARAMETERS =
      new ImmutableSetMultimap.Builder<MemberName, Integer>()
          .put(member(Objects.class, "requireNonNull"), 0)
          .put(member(Preconditions.class, "checkNotNull"), 0)
          .put(member(Verify.class, "verifyNotNull"), 0)
          .put(member("junit.framework.Assert", "assertNotNull"), -1)
          .put(member("org.junit.Assert", "assertNotNull"), -1)
          .build();

  /**
   * Maps from the names of null-querying methods to the indexes of the arguments that are compared
   * against null. Indexes may be negative to indicate a position relative to the end of the
   * argument list. For example, "-1" means "the final parameter."
   */
  @VisibleForTesting
  static final ImmutableSetMultimap<MemberName, Integer> NULL_IMPLIES_TRUE_PARAMETERS =
      new ImmutableSetMultimap.Builder<MemberName, Integer>()
          .put(member(Strings.class, "isNullOrEmpty"), 0)
          .build();

  /**
   * Maps from non-null test methods to indices of arguments that are comapred against null. These
   * methods must guarantee non-nullness if {@code true} <b>and nullness if {@code false}</b>.
   */
  private static final ImmutableSetMultimap<MemberName, Integer> NONNULL_IFF_TRUE_PARAMETERS =
      new ImmutableSetMultimap.Builder<MemberName, Integer>()
          .put(member(Objects.class, "nonNull"), 0)
          .build();

  /**
   * Maps from null test methods to indices of arguments that are comapred against null. These
   * methods must guarantee nullness if {@code true} <b>and non-nullness if {@code false}</b>.
   */
  private static final ImmutableSetMultimap<MemberName, Integer> NULL_IFF_TRUE_PARAMETERS =
      new ImmutableSetMultimap.Builder<MemberName, Integer>()
          .put(member(Objects.class, "isNull"), 0)
          .build();
}
