/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessValue.NONNULL;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessValue.NULL;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessValue.NULLABLE;
import static com.sun.tools.javac.code.TypeTag.BOOLEAN;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.io.Files;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;

import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.EqualToNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NotEqualNode;
import org.checkerframework.dataflow.cfg.node.TypeCastNode;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.lang.model.element.VariableElement;

/**
 * The {@code TransferFunction} for our nullability analysis.  This analysis determines, for all
 * variables and parameters, whether they are definitely null ({@link NullnessValue#NULL}),
 * definitely non-null ({@link NullnessValue#NONNULL}), possibly null
 * ({@link NullnessValue#NULLABLE}), or are on an infeasible path ({@link NullnessValue#BOTTOM}).
 * This analysis depends only on the code and does not take nullness annotations into account.
 *
 * <p>Each {@code visit*()} implementation provides us with information about nullability that
 * applies <i>if the expression is successfully evaluated</i> (in other words, it did not throw an
 * exception). For example, if {@code foo.toString()} is successfully evaluated, we know two things:
 *
 * <ol>
 * <li>The expression itself is non-null (because {@code toString()} is in our whitelist of methods
 *     known to return non-null values)
 * <li>{@code foo} is non-null (because it has been dereferenced without producing a {@code
 *     NullPointerException})
 * </ol>
 *
 * <p>These particular two pieces of data also demonstrate the two connected but distinct systems
 * that we use to track nullness:
 *
 * <ol>
 * <li>We compute the nullability of each expression by applying rules that may reference only the
 *     nullability of <i>subexpressions</i>. We make the result available only to superexpressions
 *     (and to the {@linkplain Analysis#getValue final output of the analysis}).
 * <li>We {@linkplain LocalVariableUpdates update} and {@linkplain LocalVariableValues read} the
 *     nullability of <i>variables</i> in a mapping that persists from node to node. This is the
 *     only exception to the rule that we propagate data from subexpression to superexpression only.
 *     The mapping is read only when visiting a {@link LocalVariableNode}. That is enough to give
 *     the {@code LocalVariableNode} a value that is then available to superexpressions.
 * </ol>
 *
 * <p>A further complication is that sometimes we know the nullability of an expression only
 * conditionally based on its result. For example, {@code foo == null} proves that {@code foo} is
 * null in the true case (such as inside {@code if (foo == null) { ... }}) and non-null in the false
 * case (such an inside an accompanying {@code else} block). This is handled by methods that accept
 * multiple {@link LocalVariableUpdates} instances, one for the true case and one for the false
 * case.
 *
 * @author deminguyen@google.com (Demi Nguyen)
 */
public class NullnessPropagationTransfer extends AbstractNullnessPropagationTransfer {
  // Literals
  /**
   * Note: A short literal appears as an int to the compiler, and the compiler can perform a
   * narrowing conversion on the literal to cast from int to short. For example, when assigning a
   * literal to a short variable, the literal does not transfer its own non-null type to the
   * variable. Instead, the variable receives the non-null type from the return value of the
   * conversion call.
   */
  @Override
  NullnessValue visitValueLiteral() {
    return NONNULL;
  }

  @Override
  NullnessValue visitNullLiteral() {
    return NULL;
  }

  @Override
  NullnessValue visitBitwiseOperation() {
    return NONNULL;
  }

  @Override
  NullnessValue visitNumericalComparison() {
    return NONNULL;
  }

  @Override
  NullnessValue visitNumericalOperation() {
    return NONNULL;
  }

  @Override
  NullnessValue visitTypeCast(TypeCastNode node, SubNodeValues inputs) {
    return hasPrimitiveType(node)
        ? NONNULL
        : inputs.valueOfSubNode(node.getOperand());
  }


  /**
   * The result of string concatenation is always non-null. If an operand is {@code null}, it is
   * converted to {@code "null"}. For more information, see
   * JLS 15.18.1 "String Concatenation Operator +", and 5.1.11, "String Conversion".
   */
  @Override
  NullnessValue visitStringConcatenate() {
    // TODO(user): Mark the inputs as dereferenced.
    return NONNULL;
  }

  @Override
  NullnessValue visitNumericalAddition() {
    return NONNULL;
  }

  @Override
  NullnessValue visitNarrowingConversion() {
    return NONNULL;
  }

  @Override
  NullnessValue visitWideningConversion() {
    return NONNULL;
  }

  @Override
  void visitEqualTo(EqualToNode node, SubNodeValues inputs, LocalVariableUpdates thenUpdates,
      LocalVariableUpdates elseUpdates) {
    handleEqualityComparison(true,
        node.getLeftOperand(),
        node.getRightOperand(),
        inputs,
        thenUpdates,
        elseUpdates);
  }

  @Override
  void visitNotEqual(NotEqualNode node, SubNodeValues inputs, LocalVariableUpdates thenUpdates,
      LocalVariableUpdates elseUpdates) {
    handleEqualityComparison(false,
        node.getLeftOperand(),
        node.getRightOperand(),
        inputs,
        thenUpdates,
        elseUpdates);
  }

  @Override
  NullnessValue visitAssignment(AssignmentNode node, SubNodeValues inputs,
      LocalVariableUpdates updates) {
    NullnessValue value = inputs.valueOfSubNode(node.getExpression());

    Node target = node.getTarget();
    if (target instanceof LocalVariableNode) {
      updates.set((LocalVariableNode) target, value);
    }

    if (target instanceof FieldAccessNode) {
      FieldAccessNode fieldAccess = (FieldAccessNode) target;
      ClassAndField targetField = tryGetFieldSymbol(target.getTree());
      setReceiverNullness(updates, fieldAccess.getReceiver(), targetField);
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
   */
  @Override
  NullnessValue visitLocalVariable(LocalVariableNode node, LocalVariableValues values) {
    return hasPrimitiveType(node) || hasNonNullConstantValue(node)
        ? NONNULL
        : values.valueOfLocalVariable(node);
  }

  /**
   * Refines the receiver of a field access to type non-null after a successful field access, and
   * refines the value of the expression as a whole to non-null if applicable (e.g., if the field
   * has a primitive type).
   *
   * <p>Note: If the field access occurs when the node is an l-value, the analysis won't call this
   * method. Instead, it will call {@link #visitAssignment}.
   */
  @Override
  NullnessValue visitFieldAccess(FieldAccessNode node, LocalVariableUpdates updates) {
    ClassAndField accessed = tryGetFieldSymbol(node.getTree());
    setReceiverNullness(updates, node.getReceiver(), accessed);
    return fieldNullness(accessed);
  }

  /**
   * Refines the receiver of a method invocation to type non-null after successful invocation, and
   * refines the value of the expression as a whole to non-null if applicable (e.g., if the method
   * returns a primitive type).
   */
  @Override
  NullnessValue visitMethodInvocation(MethodInvocationNode node, LocalVariableUpdates thenUpdates,
      LocalVariableUpdates elseUpdates, LocalVariableUpdates bothUpdates) {
    ClassAndMethod callee = tryGetMethodSymbol(node.getTree());
    setReceiverNullness(bothUpdates, node.getTarget().getReceiver(), callee);
    setUnconditionalArgumentNullness(bothUpdates, node.getArguments(), callee);
    setConditionalArgumentNullness(thenUpdates, elseUpdates, node.getArguments(), callee);
    return returnValueNullness(callee);
  }

  @Override
  NullnessValue visitObjectCreation() {
    return NONNULL;
  }

  /**
   * Refines the {@code NullnessValue} of {@code LocalVariableNode}s used in an equality
   * comparison using the greatest lower bound.
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
      LocalVariableUpdates thenUpdates,
      LocalVariableUpdates elseUpdates) {
    NullnessValue leftVal = inputs.valueOfSubNode(leftNode);
    NullnessValue rightVal = inputs.valueOfSubNode(rightNode);
    NullnessValue equalBranchValue = leftVal.greatestLowerBound(rightVal);
    LocalVariableUpdates equalBranchUpdates = equalTo ? thenUpdates : elseUpdates;
    LocalVariableUpdates notEqualBranchUpdates = equalTo ? elseUpdates : thenUpdates;

    if (leftNode instanceof LocalVariableNode) {
      LocalVariableNode localVar = (LocalVariableNode) leftNode;
      equalBranchUpdates.set(localVar, equalBranchValue);
      notEqualBranchUpdates.set(
          localVar, leftVal.greatestLowerBound(rightVal.deducedValueWhenNotEqual()));
    }

    if (rightNode instanceof LocalVariableNode) {
      LocalVariableNode localVar = (LocalVariableNode) rightNode;
      equalBranchUpdates.set(localVar, equalBranchValue);
      notEqualBranchUpdates.set(
          localVar, rightVal.greatestLowerBound(leftVal.deducedValueWhenNotEqual()));
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

  static ClassAndMethod tryGetMethodSymbol(MethodInvocationTree tree) {
    Symbol symbol = tryGetSymbol(tree.getMethodSelect());
    if (symbol instanceof MethodSymbol) {
      return ClassAndMethod.make((MethodSymbol) symbol);
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
    return null;
  }

  private static NullnessValue fieldNullness(ClassAndField accessed) {
    if (accessed == null) {
      return NULLABLE;
    }

    if (accessed.field.equals("class")) {
      return NONNULL;
    }
    if (accessed.isEnumConstant) {
      return NONNULL;
    }
    if (accessed.isPrimitive) {
      return NONNULL;
    }
    if (accessed.hasNonNullConstantValue) {
      return NONNULL;
    }

    return NULLABLE;
  }

  private static NullnessValue returnValueNullness(ClassAndMethod callee) {
    if (callee == null) {
      return NULLABLE;
    }

    if (callee.method.equals("valueOf")
        && CLASSES_WITH_NON_NULLABLE_VALUE_OF_METHODS.contains(callee.clazz)) {
      return NONNULL;
    }
    if (callee.isPrimitive) {
      return NONNULL;
    }
    if (CLASSES_WITH_ALL_NON_NULLABLE_RETURNS.contains(callee.clazz)) {
      return NONNULL;
    }
    if (METHODS_WITH_NON_NULLABLE_RETURNS.contains(callee.name())) {
      return NONNULL;
    }

    return NULLABLE;
  }

  private static void setReceiverNullness(
      LocalVariableUpdates updates, Node receiver, Member member) {
    if (!member.isStatic() && receiver instanceof LocalVariableNode) {
      updates.set((LocalVariableNode) receiver, NONNULL);
    }
  }

  /**
   * Records which arguments are guaranteed to be non-null if the method completes without
   * exception. For example, if {@code checkNotNull(foo, message)} completes successfully, then
   * {@code foo} is not null.
   */
  private static void setUnconditionalArgumentNullness(
      LocalVariableUpdates bothUpdates, List<Node> arguments, ClassAndMethod callee) {
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
  private static void setConditionalArgumentNullness(LocalVariableUpdates thenUpdates,
      LocalVariableUpdates elseUpdates, List<Node> arguments, ClassAndMethod callee) {
    Set<Integer> nullImpliesTrueParameters = NULL_IMPLIES_TRUE_PARAMETERS.get(callee.name());
    for (LocalVariableNode var : variablesAtIndexes(nullImpliesTrueParameters, arguments)) {
      elseUpdates.set(var, NONNULL);
    }
  }

  private static Iterable<LocalVariableNode> variablesAtIndexes(
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

  private static final class MemberName {
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

  static final class ClassAndMethod implements Member {
    final String clazz;
    final String method;
    final boolean isStatic;
    final boolean isPrimitive;
    final boolean isBoolean;

    private ClassAndMethod(
        String clazz, String method, boolean isStatic, boolean isPrimitive, boolean isBoolean) {
      this.clazz = clazz;
      this.method = method;
      this.isStatic = isStatic;
      this.isPrimitive = isPrimitive;
      this.isBoolean = isBoolean;
    }

    static ClassAndMethod make(MethodSymbol symbol) {
      return new ClassAndMethod(symbol.owner.getQualifiedName().toString(),
          symbol.getSimpleName().toString(), symbol.isStatic(),
          symbol.getReturnType().isPrimitive(), symbol.getReturnType().getTag() == BOOLEAN);
    }

    @Override
    public boolean isStatic() {
      return isStatic;
    }

    MemberName name() {
      return new MemberName(this.clazz, this.method);
    }
  }

  private static final class ClassAndField implements Member {
    final String clazz;
    final String field;
    final boolean isStatic;
    final boolean isPrimitive;
    final boolean isEnumConstant;
    final boolean hasNonNullConstantValue;

    private ClassAndField(String clazz, String field, boolean isStatic, boolean isPrimitive,
        boolean isEnumConstant, boolean hasNonNullConstantValue) {
      this.clazz = clazz;
      this.field = field;
      this.isStatic = isStatic;
      this.isPrimitive = isPrimitive;
      this.isEnumConstant = isEnumConstant;
      this.hasNonNullConstantValue = hasNonNullConstantValue;
    }

    static ClassAndField make(VarSymbol symbol) {
      return new ClassAndField(symbol.owner.getQualifiedName().toString(),
          symbol.getSimpleName().toString(), symbol.isStatic(), symbol.type.isPrimitive(),
          symbol.isEnum(), symbol.getConstantValue() != null);
    }

    @Override
    public boolean isStatic() {
      return isStatic;
    }
  }

  private static final ImmutableSet<MemberName> METHODS_WITH_NON_NULLABLE_RETURNS =
      ImmutableSet.of(
          // We would love to include all the methods of Files, but readFirstLine can return null.
          member(Files.class.getName(), "toString"));
  // TODO(cpovirk): respect nullness annotations (and also check them to ensure correctness!)

  private static final ImmutableSet<String> CLASSES_WITH_ALL_NON_NULLABLE_RETURNS =
      ImmutableSet.of(
          Preconditions.class.getName(),
          Verify.class.getName(),
          String.class.getName());

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
          BigDecimal.class.getName(),
          BigInteger.class.getName(),
          // TODO(cpovirk): recognize the compiler-generated valueOf() methods on Enum subclasses
          Enum.class.getName(),
          String.class.getName());

  /**
   * Maps from the names of null-rejecting methods to the indexes of the arguments that aren't
   * permitted to be null. Indexes may be negative to indicate a position relative to the end of the
   * argument list. For example, "-1" means "the final parameter."
   */
  private static final ImmutableSetMultimap<MemberName, Integer>
      REQUIRED_NON_NULL_PARAMETERS = new ImmutableSetMultimap.Builder<MemberName, Integer>()
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
  private static final ImmutableSetMultimap<MemberName, Integer>
      NULL_IMPLIES_TRUE_PARAMETERS = new ImmutableSetMultimap.Builder<MemberName, Integer>()
          .put(member(Strings.class, "isNullOrEmpty"), 0)
          .build();
}
