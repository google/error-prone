/*
 * Copyright 2025 The Error Prone Authors.
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

package com.google.errorprone.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Streams.findLast;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.streamSuperMethods;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.tree.YieldTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.WildcardType;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/** An expression's target type, see {@link #targetType}. */
@AutoValue
public abstract class TargetType {

  /**
   * Returns the target type of the tree at the given {@link VisitorState}'s path, or else {@code
   * null}.
   *
   * <p>For example, the target type of an assignment expression is the variable's type, and the
   * target type of a return statement is the enclosing method's type.
   *
   * <p>A general mental model is that the target type of an expression where the value is used is
   * the highest supertype that you could cast to and still have this compile.
   */
  public static @Nullable TargetType targetType(VisitorState state) {
    if (!canHaveTargetType(state.getPath().getLeaf())) {
      return null;
    }
    ExpressionTree current;
    TreePath parent = state.getPath();
    do {
      current = (ExpressionTree) parent.getLeaf();
      parent = parent.getParentPath();
    } while (parent != null && parent.getLeaf().getKind() == Kind.PARENTHESIZED);

    if (parent == null) {
      return null;
    }

    Type type = new TargetTypeVisitor(current, state, parent).visit(parent.getLeaf(), null);
    if (type == null) {
      Tree actualTree = null;
      if (parent.getLeaf() instanceof YieldTree) {
        actualTree = parent.getParentPath().getParentPath().getParentPath().getLeaf();
      } else if (CONSTANT_CASE_LABEL_TREE != null
          && CONSTANT_CASE_LABEL_TREE.isAssignableFrom(parent.getLeaf().getClass())) {
        actualTree = parent.getParentPath().getParentPath().getLeaf();
      }

      type = getType(TargetTypeVisitor.getSwitchExpression(actualTree));
      if (type == null) {
        return null;
      }
    }
    return create(type, parent);
  }

  public abstract Type type();

  public abstract TreePath path();

  static TargetType create(Type type, TreePath path) {
    return new AutoValue_TargetType(type, path);
  }

  private static final @Nullable Class<?> CONSTANT_CASE_LABEL_TREE = constantCaseLabelTree();

  private static @Nullable Class<?> constantCaseLabelTree() {
    try {
      return Class.forName("com.sun.source.tree.ConstantCaseLabelTree");
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  private static boolean canHaveTargetType(Tree tree) {
    // Anything that isn't an expression can't have a target type.
    if (!(tree instanceof ExpressionTree)) {
      return false;
    }
    switch (tree.getKind()) {
      case IDENTIFIER, MEMBER_SELECT -> {
        if (!(ASTHelpers.getSymbol(tree) instanceof VarSymbol)) {
          // If we're selecting other than a member (e.g. a type or a method) then this doesn't
          // have a target type.
          return false;
        }
      }
      case PRIMITIVE_TYPE,
          ARRAY_TYPE,
          PARAMETERIZED_TYPE,
          EXTENDS_WILDCARD,
          SUPER_WILDCARD,
          UNBOUNDED_WILDCARD,
          ANNOTATED_TYPE,
          INTERSECTION_TYPE,
          TYPE_ANNOTATION -> {
        // These are all things that only appear in type uses, so they can't have a target type.
        return false;
      }
      case ANNOTATION -> {
        // Annotations can only appear on elements which don't have target types.
        return false;
      }
      default -> {
        // Continue.
      }
    }
    return true;
  }

  @VisibleForTesting
  static class TargetTypeVisitor extends SimpleTreeVisitor<Type, Void> {
    private final VisitorState state;
    private final TreePath parent;
    private final ExpressionTree current;

    private TargetTypeVisitor(ExpressionTree current, VisitorState state, TreePath parent) {
      this.current = current;
      this.state = state;
      this.parent = parent;
    }

    @Override
    public @Nullable Type visitArrayAccess(ArrayAccessTree node, Void unused) {
      if (current.equals(node.getIndex())) {
        return state.getSymtab().intType;
      } else {
        return getType(node.getExpression());
      }
    }

    @Override
    public Type visitAssert(AssertTree node, Void unused) {
      return current.equals(node.getCondition())
          ? state.getSymtab().booleanType
          : state.getSymtab().stringType;
    }

    @Override
    public @Nullable Type visitAssignment(AssignmentTree tree, Void unused) {
      return getType(tree.getVariable());
    }

    @Override
    public Type visitAnnotation(AnnotationTree tree, Void unused) {
      return null;
    }

    @Override
    public @Nullable Type visitCase(CaseTree tree, Void unused) {
      Tree switchTree = parent.getParentPath().getLeaf();
      if (tree.getBody() != null && tree.getBody().equals(current)) {
        return getType(switchTree);
      }
      return getType(getSwitchExpression(switchTree));
    }

    private static @Nullable ExpressionTree getSwitchExpression(@Nullable Tree tree) {
      if (tree instanceof SwitchTree switchTree) {
        return switchTree.getExpression();
      }
      if (tree instanceof SwitchExpressionTree switchExpressionTree) {
        return switchExpressionTree.getExpression();
      }
      return null;
    }

    @Override
    public Type visitClass(ClassTree node, Void unused) {
      return null;
    }

    @Override
    public @Nullable Type visitCompoundAssignment(CompoundAssignmentTree tree, Void unused) {
      Type variableType = getType(tree.getVariable());
      Type expressionType = getType(tree.getExpression());
      Types types = state.getTypes();
      switch (tree.getKind()) {
        case LEFT_SHIFT_ASSIGNMENT, RIGHT_SHIFT_ASSIGNMENT, UNSIGNED_RIGHT_SHIFT_ASSIGNMENT -> {
          // Shift operators perform *unary* numeric promotion on the operands, separately.
          if (tree.getExpression().equals(current)) {
            return unaryNumericPromotion(expressionType, state);
          }
        }
        case PLUS_ASSIGNMENT -> {
          Type stringType = state.getSymtab().stringType;
          if (types.isSuperType(variableType, stringType)) {
            return stringType;
          }
        }
        default -> {
          // Fall though.
        }
      }
      // If we've got to here, we can only have boolean or numeric operands
      // (because the only compound assignment operator for String is +=).

      // These operands will necessarily be unboxed (and, if numeric, undergo binary numeric
      // promotion), even if the resulting expression is of boxed type. As such, report the unboxed
      // type.
      return types.unboxedTypeOrType(variableType).getTag() == TypeTag.BOOLEAN
          ? state.getSymtab().booleanType
          : binaryNumericPromotion(variableType, expressionType, state);
    }

    @Override
    public Type visitEnhancedForLoop(EnhancedForLoopTree node, Void unused) {
      Type variableType = getType(node.getVariable());
      if (state.getTypes().isArray(getType(node.getExpression()))) {
        // For iterating an array, the target type is LoopVariableType[].
        return state.getType(variableType, true, ImmutableList.of());
      }
      // For iterating an iterable, the target type is Iterable<? extends LoopVariableType>.
      variableType = state.getTypes().boxedTypeOrType(variableType);
      return state.getType(
          state.getSymtab().iterableType,
          false,
          ImmutableList.of(new WildcardType(variableType, BoundKind.EXTENDS, variableType.tsym)));
    }

    @Override
    public Type visitInstanceOf(InstanceOfTree node, Void unused) {
      return state.getSymtab().objectType;
    }

    @Override
    public Type visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree, Void unused) {
      return state.getTypes().findDescriptorType(getType(lambdaExpressionTree)).getReturnType();
    }

    @Override
    public Type visitMethod(MethodTree node, Void unused) {
      return null;
    }

    @Override
    public @Nullable Type visitReturn(ReturnTree tree, Void unused) {
      for (TreePath path = parent; path != null; path = path.getParentPath()) {
        Tree enclosing = path.getLeaf();
        switch (enclosing.getKind()) {
          case METHOD -> {
            return getType(((MethodTree) enclosing).getReturnType());
          }
          case LAMBDA_EXPRESSION -> {
            return visitLambdaExpression((LambdaExpressionTree) enclosing, null);
          }
          default -> {}
        }
      }
      throw new AssertionError("return not enclosed by method or lambda");
    }

    @Override
    public @Nullable Type visitSynchronized(SynchronizedTree node, Void unused) {
      // The null occurs if you've asked for the type of the parentheses around the expression.
      return Objects.equals(current, node.getExpression()) ? state.getSymtab().objectType : null;
    }

    @Override
    public Type visitThrow(ThrowTree node, Void unused) {
      return getType(current);
    }

    @Override
    public Type visitTypeCast(TypeCastTree node, Void unused) {
      return getType(node.getType());
    }

    @Override
    public @Nullable Type visitVariable(VariableTree tree, Void unused) {
      return getType(tree.getType());
    }

    @Override
    public @Nullable Type visitUnary(UnaryTree tree, Void unused) {
      return getType(tree);
    }

    @Override
    public @Nullable Type visitBinary(BinaryTree tree, Void unused) {
      Type leftType = checkNotNull(getType(tree.getLeftOperand()));
      Type rightType = checkNotNull(getType(tree.getRightOperand()));
      switch (tree.getKind()) {
        // The addition and subtraction operators for numeric types + and - (§15.18.2)
        case PLUS:
          // If either operand is of string type, string concatenation is performed.
          Type stringType = state.getSymtab().stringType;
          if (isSameType(stringType, leftType, state) || isSameType(stringType, rightType, state)) {
            return stringType;
          }
        // Fall through.
        case MINUS:
        // The multiplicative operators *, /, and % (§15.17)
        case MULTIPLY:
        case DIVIDE:
        case REMAINDER:
        // The numerical comparison operators <, <=, >, and >= (§15.20.1)
        case LESS_THAN:
        case LESS_THAN_EQUAL:
        case GREATER_THAN:
        case GREATER_THAN_EQUAL:
        // The integer bitwise operators &, ^, and |
        case AND:
        case XOR:
        case OR:
          if (typeIsBoolean(state.getTypes().unboxedTypeOrType(leftType))
              && typeIsBoolean(state.getTypes().unboxedTypeOrType(rightType))) {
            return state.getSymtab().booleanType;
          }
          return binaryNumericPromotion(leftType, rightType, state);
        case EQUAL_TO:
        case NOT_EQUAL_TO:
          return handleEqualityOperator(tree, leftType, rightType);
        case LEFT_SHIFT:
        case RIGHT_SHIFT:
        case UNSIGNED_RIGHT_SHIFT:
          // Shift operators perform *unary* numeric promotion on the operands, separately.
          return unaryNumericPromotion(getType(current), state);
        default:
          return getType(tree);
      }
    }

    private @Nullable Type handleEqualityOperator(BinaryTree tree, Type leftType, Type rightType) {
      Type unboxedLeft = checkNotNull(state.getTypes().unboxedTypeOrType(leftType));
      Type unboxedRight = checkNotNull(state.getTypes().unboxedTypeOrType(rightType));

      // If the operands of an equality operator are both of numeric type, or one is of numeric
      // type and the other is convertible (§5.1.8) to numeric type, binary numeric promotion is
      // performed on the operands (§5.6.2).
      if ((leftType.isNumeric() && rightType.isNumeric())
          || (leftType.isNumeric() != rightType.isNumeric()
              && (unboxedLeft.isNumeric() || unboxedRight.isNumeric()))) {
        // https://docs.oracle.com/javase/specs/jls/se9/html/jls-15.html#jls-15.21.1
        // Numerical equality.
        return binaryNumericPromotion(unboxedLeft, unboxedRight, state);
      }

      // If the operands of an equality operator are both of type boolean, or if one operand is
      // of type boolean and the other is of type Boolean, then the operation is boolean
      // equality.
      boolean leftIsBoolean = typeIsBoolean(leftType);
      boolean rightIsBoolean = typeIsBoolean(rightType);
      if ((leftIsBoolean && rightIsBoolean)
          || (leftIsBoolean != rightIsBoolean
              && (typeIsBoolean(unboxedLeft) || typeIsBoolean(unboxedRight)))) {
        return state.getSymtab().booleanType;
      }

      // If the operands of an equality operator are both of either reference type or the null
      // type, then the operation is object equality.
      return tree.getLeftOperand().equals(current) ? leftType : rightType;
    }

    private static boolean typeIsBoolean(Type type) {
      return type.getTag() == TypeTag.BOOLEAN;
    }

    @Override
    public @Nullable Type visitConditionalExpression(ConditionalExpressionTree tree, Void unused) {
      return tree.getCondition().equals(current) ? state.getSymtab().booleanType : getType(tree);
    }

    @Override
    public Type visitNewClass(NewClassTree tree, Void unused) {
      if (Objects.equals(current, tree.getEnclosingExpression())) {
        return ASTHelpers.getSymbol(tree.getIdentifier()).owner.type;
      }
      return visitMethodInvocationOrNewClass(
          tree.getArguments(), ASTHelpers.getSymbol(tree), ((JCNewClass) tree).constructorType);
    }

    @Override
    public Type visitMethodInvocation(MethodInvocationTree tree, Void unused) {
      return visitMethodInvocationOrNewClass(
          tree.getArguments(), ASTHelpers.getSymbol(tree), ((JCMethodInvocation) tree).meth.type);
    }

    private @Nullable Type visitMethodInvocationOrNewClass(
        List<? extends ExpressionTree> arguments, MethodSymbol sym, Type type) {
      int idx = arguments.indexOf(current);
      if (idx == -1) {
        return null;
      }
      if (type.getParameterTypes().size() <= idx) {
        if (!sym.isVarArgs()) {
          if ((sym.flags() & Flags.HYPOTHETICAL) != 0) {
            // HYPOTHETICAL is also used for signature-polymorphic methods
            return null;
          }
          throw new IllegalStateException(
              String.format(
                  "saw %d formal parameters and %d actual parameters on non-varargs method %s\n",
                  type.getParameterTypes().size(), arguments.size(), sym));
        }
        idx = type.getParameterTypes().size() - 1;
      }
      Type argType = type.getParameterTypes().get(idx);
      if (sym.isVarArgs() && idx == type.getParameterTypes().size() - 1) {
        argType = state.getTypes().elemtype(argType);
      }
      return argType;
    }

    @Override
    public Type visitIf(IfTree tree, Void unused) {
      return getConditionType(tree.getCondition());
    }

    @Override
    public Type visitWhileLoop(WhileLoopTree tree, Void unused) {
      return getConditionType(tree.getCondition());
    }

    @Override
    public Type visitDoWhileLoop(DoWhileLoopTree tree, Void unused) {
      return getConditionType(tree.getCondition());
    }

    @Override
    public Type visitForLoop(ForLoopTree tree, Void unused) {
      return getConditionType(tree.getCondition());
    }

    @Override
    public @Nullable Type visitSwitch(SwitchTree node, Void unused) {
      if (current == node.getExpression()) {
        return state.getTypes().unboxedTypeOrType(getType(current));
      } else {
        return null;
      }
    }

    @Override
    public @Nullable Type visitNewArray(NewArrayTree node, Void unused) {
      if (Objects.equals(node.getType(), current)) {
        return null;
      }
      if (node.getDimensions().contains(current)) {
        return state.getSymtab().intType;
      }
      if (node.getInitializers() != null && node.getInitializers().contains(current)) {
        return state.getTypes().elemtype(getType(node));
      }
      return null;
    }

    @Override
    public @Nullable Type visitMemberSelect(MemberSelectTree node, Void unused) {
      if (!current.equals(node.getExpression())) {
        return null;
      }

      if (!(ASTHelpers.getSymbol(node) instanceof MethodSymbol ms)) {
        return getType(node.getExpression());
      }
      Type typeDeclaringMethod = getEffectiveReceiverType(node, ms);
      return getType(node.getExpression()).isRaw()
          ? state.getTypes().erasure(typeDeclaringMethod)
          : typeDeclaringMethod;
    }

    private Type getEffectiveReceiverType(MemberSelectTree node, MethodSymbol ms) {
      // Warning: finicky logic. With an expression `like foo.bar().baz()`, we want the target
      // type of `foo` to be the supermost overload of `bar` which returns something on which we can
      // actually call `baz`, hence the recursion.
      ImmutableSet<MethodSymbol> superMethods =
          Stream.concat(Stream.of(ms), streamSuperMethods(ms, state.getTypes()))
              .collect(toImmutableSet());

      // Performance win: if there are no covariant return types to worry about, fast-path out.
      var superMethodReturnTypes =
          superMethods.stream()
              .map(sm -> state.getTypes().erasure(sm.getReturnType()))
              .collect(toImmutableSet());
      if (superMethodReturnTypes.size() == 1) {
        // All are compatible, so pick the last.
        return getLast(superMethods).owner.type;
      }
      var invocationTarget = targetType(state.withPath(parent.getParentPath()));
      return findLast(
              superMethods.stream()
                  .filter(
                      sms ->
                          invocationTarget == null
                              || ASTHelpers.isSubtype(
                                  sms.getReturnType(), invocationTarget.type(), state)))
          .map(m -> m.owner.type)
          .orElse(getType(node.getExpression()));
    }

    @Override
    public Type visitMemberReference(MemberReferenceTree node, Void unused) {
      return state.getTypes().findDescriptorType(getType(node)).getReturnType();
    }

    private @Nullable Type getConditionType(Tree condition) {
      if (condition != null && condition.equals(current)) {
        return state.getSymtab().booleanType;
      }
      return null;
    }
  }

  /**
   * Implementation of unary numeric promotion rules.
   *
   * <p><a href="https://docs.oracle.com/javase/specs/jls/se9/html/jls-5.html#jls-5.6.1">JLS
   * §5.6.1</a>
   */
  private static @Nullable Type unaryNumericPromotion(Type type, VisitorState state) {
    Type unboxed = unboxAndEnsureNumeric(type, state);
    return switch (unboxed.getTag()) {
      case BYTE, SHORT, CHAR -> state.getSymtab().intType;
      case INT, LONG, FLOAT, DOUBLE -> unboxed;
      default -> throw new AssertionError("Should not reach here: " + type);
    };
  }

  /**
   * Implementation of binary numeric promotion rules.
   *
   * <p><a href="https://docs.oracle.com/javase/specs/jls/se9/html/jls-5.html#jls-5.6.2">JLS
   * §5.6.2</a>
   */
  private static @Nullable Type binaryNumericPromotion(
      Type leftType, Type rightType, VisitorState state) {
    Type unboxedLeft = unboxAndEnsureNumeric(leftType, state);
    Type unboxedRight = unboxAndEnsureNumeric(rightType, state);
    Set<TypeTag> tags = EnumSet.of(unboxedLeft.getTag(), unboxedRight.getTag());
    if (tags.contains(TypeTag.DOUBLE)) {
      return state.getSymtab().doubleType;
    } else if (tags.contains(TypeTag.FLOAT)) {
      return state.getSymtab().floatType;
    } else if (tags.contains(TypeTag.LONG)) {
      return state.getSymtab().longType;
    } else {
      return state.getSymtab().intType;
    }
  }

  private static Type unboxAndEnsureNumeric(Type type, VisitorState state) {
    Type unboxed = state.getTypes().unboxedTypeOrType(type);
    checkArgument(unboxed.isNumeric(), "[%s] is not numeric", type);
    return unboxed;
  }
}
