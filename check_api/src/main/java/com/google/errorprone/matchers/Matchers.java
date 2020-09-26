/*
 * Copyright 2011 The Error Prone Authors.
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

package com.google.errorprone.matchers;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Streams.stream;
import static com.google.errorprone.suppliers.Suppliers.BOOLEAN_TYPE;
import static com.google.errorprone.suppliers.Suppliers.INT_TYPE;
import static com.google.errorprone.suppliers.Suppliers.JAVA_LANG_BOOLEAN_TYPE;
import static com.google.errorprone.suppliers.Suppliers.STRING_TYPE;
import static com.google.errorprone.suppliers.Suppliers.typeFromClass;
import static com.google.errorprone.suppliers.Suppliers.typeFromString;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static com.google.errorprone.util.ASTHelpers.stripParentheses;
import static java.util.Objects.requireNonNull;

import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.VisitorState;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.matchers.ChildMultiMatcher.MatchType;
import com.google.errorprone.matchers.MethodVisibility.Visibility;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.matchers.method.MethodMatchers.AnyMethodMatcher;
import com.google.errorprone.matchers.method.MethodMatchers.ConstructorMatcher;
import com.google.errorprone.matchers.method.MethodMatchers.InstanceMethodMatcher;
import com.google.errorprone.matchers.method.MethodMatchers.StaticMethodMatcher;
import com.google.errorprone.predicates.TypePredicate;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.util.Name;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 * Static factory methods which make the DSL read more fluently. Since matchers are run in a tight
 * loop during compilation, performance is important. When assembling a matcher from the DSL, it's
 * best to construct it only once, by saving the resulting matcher as a static variable for example.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
public class Matchers {

  private Matchers() {}

  /** A matcher that matches any AST node. */
  public static <T extends Tree> Matcher<T> anything() {
    return (t, state) -> true;
  }

  /** A matcher that matches no AST node. */
  public static <T extends Tree> Matcher<T> nothing() {
    return (t, state) -> false;
  }

  /** Matches an AST node iff it does not match the given matcher. */
  public static <T extends Tree> Matcher<T> not(Matcher<T> matcher) {
    return (t, state) -> !matcher.matches(t, state);
  }

  /**
   * Compose several matchers together, such that the composite matches an AST node iff all the
   * given matchers do.
   */
  @SafeVarargs
  public static <T extends Tree> Matcher<T> allOf(final Matcher<? super T>... matchers) {
    return (t, state) -> {
      for (Matcher<? super T> matcher : matchers) {
        if (!matcher.matches(t, state)) {
          return false;
        }
      }
      return true;
    };
  }

  /**
   * Compose several matchers together, such that the composite matches an AST node iff all the
   * given matchers do.
   */
  public static <T extends Tree> Matcher<T> allOf(
      final Iterable<? extends Matcher<? super T>> matchers) {
    return (t, state) -> {
      for (Matcher<? super T> matcher : matchers) {
        if (!matcher.matches(t, state)) {
          return false;
        }
      }
      return true;
    };
  }

  /**
   * Compose several matchers together, such that the composite matches an AST node if any of the
   * given matchers do.
   */
  public static <T extends Tree> Matcher<T> anyOf(
      final Iterable<? extends Matcher<? super T>> matchers) {
    return (t, state) -> {
      for (Matcher<? super T> matcher : matchers) {
        if (matcher.matches(t, state)) {
          return true;
        }
      }
      return false;
    };
  }

  @SafeVarargs
  public static <T extends Tree> Matcher<T> anyOf(Matcher<? super T>... matchers) {
    // IntelliJ claims it can infer <Matcher<? super T>>, but blaze can't (b/132970194).
    return anyOf(Arrays.<Matcher<? super T>>asList(matchers));
  }

  /** Matches if an AST node is an instance of the given class. */
  public static <T extends Tree> Matcher<T> isInstance(java.lang.Class<?> klass) {
    return (t, state) -> klass.isInstance(t);
  }

  /** Matches an AST node of a given kind, for example, an Annotation or a switch block. */
  public static <T extends Tree> Matcher<T> kindIs(Kind kind) {
    return (tree, state) -> tree.getKind() == kind;
  }

  /** Matches an AST node of a given kind, for example, an Annotation or a switch block. */
  public static <T extends Tree> Matcher<T> kindAnyOf(Set<Kind> kinds) {
    return (tree, state) -> kinds.contains(tree.getKind());
  }

  /** Matches an AST node which is the same object reference as the given node. */
  public static <T extends Tree> Matcher<T> isSame(Tree t) {
    return (tree, state) -> tree == t;
  }

  /** Matches a static method. */
  public static StaticMethodMatcher staticMethod() {
    return MethodMatchers.staticMethod();
  }

  /** Matches an instance method. */
  public static InstanceMethodMatcher instanceMethod() {
    return MethodMatchers.instanceMethod();
  }

  /** Matches a static or instance method. */
  public static AnyMethodMatcher anyMethod() {
    return MethodMatchers.anyMethod();
  }

  /** Matches a constructor. */
  public static ConstructorMatcher constructor() {
    return MethodMatchers.constructor();
  }

  /**
   * Match a Tree based solely on the Symbol produced by {@link ASTHelpers#getSymbol(Tree)}.
   *
   * <p>If {@code getSymbol} returns {@code null}, the matcher returns false instead of calling
   * {@code pred}.
   */
  public static <T extends Tree> Matcher<T> symbolMatcher(BiPredicate<Symbol, VisitorState> pred) {
    return (tree, state) -> {
      Symbol sym = getSymbol(tree);
      return sym != null && pred.test(sym, state);
    };
  }

  /** Matches an AST node that represents a non-static field. */
  public static Matcher<ExpressionTree> isInstanceField() {
    return symbolMatcher(
        (symbol, state) -> symbol.getKind() == ElementKind.FIELD && !symbol.isStatic());
  }

  /** Matches an AST node that represents a local variable or parameter. */
  public static Matcher<ExpressionTree> isVariable() {
    return symbolMatcher(
        (symbol, state) ->
            symbol.getKind() == ElementKind.LOCAL_VARIABLE
                || symbol.getKind() == ElementKind.PARAMETER);
  }

  /**
   * Matches a compound assignment operator AST node which matches a given left-operand matcher, a
   * given right-operand matcher, and a specific compound assignment operator.
   *
   * @param operator Which compound assignment operator to match against.
   * @param leftOperandMatcher The matcher to apply to the left operand.
   * @param rightOperandMatcher The matcher to apply to the right operand.
   */
  public static CompoundAssignment compoundAssignment(
      Kind operator,
      Matcher<ExpressionTree> leftOperandMatcher,
      Matcher<ExpressionTree> rightOperandMatcher) {
    Set<Kind> operators = new HashSet<>(1);
    operators.add(operator);
    return new CompoundAssignment(operators, leftOperandMatcher, rightOperandMatcher);
  }

  /**
   * Matches a compound assignment operator AST node which matches a given left-operand matcher, a
   * given right-operand matcher, and is one of a set of compound assignment operators. Does not
   * match compound assignment operators.
   *
   * @param operators Which compound assignment operators to match against.
   * @param receiverMatcher The matcher to apply to the receiver.
   * @param expressionMatcher The matcher to apply to the expression.
   */
  public static CompoundAssignment compoundAssignment(
      Set<Kind> operators,
      Matcher<ExpressionTree> receiverMatcher,
      Matcher<ExpressionTree> expressionMatcher) {
    return new CompoundAssignment(operators, receiverMatcher, expressionMatcher);
  }

  /**
   * Matches when the receiver of an instance method is the same reference as a particular argument
   * to the method. For example, receiverSameAsArgument(1) would match {@code obj.method("", obj)}
   *
   * @param argNum The number of the argument to compare against (zero-based.
   */
  public static Matcher<? super MethodInvocationTree> receiverSameAsArgument(final int argNum) {
    return (t, state) -> {
      List<? extends ExpressionTree> args = t.getArguments();
      if (args.size() <= argNum) {
        return false;
      }
      ExpressionTree arg = args.get(argNum);

      JCExpression methodSelect = (JCExpression) t.getMethodSelect();
      if (methodSelect instanceof JCFieldAccess) {
        JCFieldAccess fieldAccess = (JCFieldAccess) methodSelect;
        return ASTHelpers.sameVariable(fieldAccess.getExpression(), arg);
      } else if (methodSelect instanceof JCIdent) {
        // A bare method call: "equals(foo)".  Receiver is implicitly "this".
        return "this".equals(arg.toString());
      }

      return false;
    };
  }

  public static Matcher<MethodInvocationTree> receiverOfInvocation(
      final Matcher<ExpressionTree> expressionTreeMatcher) {
    return (methodInvocationTree, state) -> {
      ExpressionTree receiver = ASTHelpers.getReceiver(methodInvocationTree);
      return receiver != null && expressionTreeMatcher.matches(receiver, state);
    };
  }

  /**
   * Matches if the given annotation matcher matches all of or any of the annotations on this tree
   * node.
   *
   * @param matchType Whether to match if the matchers match any of or all of the annotations on
   *     this tree.
   * @param annotationMatcher The annotation matcher to use.
   */
  public static <T extends Tree> MultiMatcher<T, AnnotationTree> annotations(
      MatchType matchType, Matcher<AnnotationTree> annotationMatcher) {
    return new AnnotationMatcher<>(matchType, annotationMatcher);
  }

  /** Matches a class in which any of/all of its constructors match the given constructorMatcher. */
  public static MultiMatcher<ClassTree, MethodTree> constructor(
      MatchType matchType, Matcher<MethodTree> constructorMatcher) {
    return new ConstructorOfClass(matchType, constructorMatcher);
  }

  // TODO(cushon): expunge
  public static Matcher<MethodInvocationTree> methodSelect(
      Matcher<ExpressionTree> methodSelectMatcher) {
    return new MethodInvocationMethodSelect(methodSelectMatcher);
  }

  public static Matcher<MethodInvocationTree> argument(
      final int position, final Matcher<ExpressionTree> argumentMatcher) {
    return new MethodInvocationArgument(position, argumentMatcher);
  }

  /** Matches if the given matcher matches all of/any of the arguments to this method invocation. */
  public static MultiMatcher<MethodInvocationTree, ExpressionTree> hasArguments(
      MatchType matchType, Matcher<ExpressionTree> argumentMatcher) {
    return new HasArguments(matchType, argumentMatcher);
  }

  /**
   * Matches an AST node if it is a method invocation and the given matchers match.
   *
   * @param methodSelectMatcher matcher identifying the method being called
   * @param matchType how to match method arguments with {@code methodArgumentMatcher}
   * @param methodArgumentMatcher matcher applied to each method argument
   */
  public static Matcher<ExpressionTree> methodInvocation(
      Matcher<ExpressionTree> methodSelectMatcher,
      MatchType matchType,
      Matcher<ExpressionTree> methodArgumentMatcher) {
    return new MethodInvocation(methodSelectMatcher, matchType, methodArgumentMatcher);
  }

  /**
   * Matches an AST node if it is a method invocation and the method select matches {@code
   * methodSelectMatcher}. Ignores any arguments.
   */
  public static Matcher<ExpressionTree> methodInvocation(
      final Matcher<ExpressionTree> methodSelectMatcher) {
    return (expressionTree, state) -> {
      if (!(expressionTree instanceof MethodInvocationTree)) {
        return false;
      }
      MethodInvocationTree tree = (MethodInvocationTree) expressionTree;
      return methodSelectMatcher.matches(tree.getMethodSelect(), state);
    };
  }

  public static Matcher<MethodInvocationTree> argumentCount(final int argumentCount) {
    return (t, state) -> t.getArguments().size() == argumentCount;
  }

  /**
   * Matches an AST node if its parent node is matched by the given matcher. For example, {@code
   * parentNode(kindIs(Kind.RETURN))} would match the {@code this} expression in {@code return
   * this;}
   */
  public static <T extends Tree> Matcher<T> parentNode(Matcher<Tree> treeMatcher) {
    return (tree, state) -> {
      TreePath parent = requireNonNull(state.getPath().getParentPath());
      return treeMatcher.matches(parent.getLeaf(), state.withPath(parent));
    };
  }

  /**
   * Matches an AST node if its type is a subtype of the given type.
   *
   * @param typeStr a string representation of the type, e.g., "java.util.AbstractList"
   */
  public static <T extends Tree> Matcher<T> isSubtypeOf(String typeStr) {
    return new IsSubtypeOf<>(typeStr);
  }

  /**
   * Matches an AST node if its type is a subtype of the given type.
   *
   * @param type the type to check against
   */
  public static <T extends Tree> Matcher<T> isSubtypeOf(Supplier<Type> type) {
    return new IsSubtypeOf<>(type);
  }

  /**
   * Matches an AST node if its type is a subtype of the given type.
   *
   * @param clazz a class representation of the type, e.g., Action.class.
   */
  public static <T extends Tree> Matcher<T> isSubtypeOf(Class<?> clazz) {
    return new IsSubtypeOf<>(typeFromClass(clazz));
  }

  /** Matches an AST node if it has the same erased type as the given type. */
  public static <T extends Tree> Matcher<T> isSameType(Supplier<Type> type) {
    return new IsSameType<>(type);
  }

  /** Matches an AST node if it has the same erased type as the given type. */
  public static <T extends Tree> Matcher<T> isSameType(String typeString) {
    return new IsSameType<>(typeString);
  }

  /** Matches an AST node if it has the same erased type as the given class. */
  public static <T extends Tree> Matcher<T> isSameType(Class<?> clazz) {
    return new IsSameType<>(typeFromClass(clazz));
  }

  /**
   * Match a Tree based solely on the type produced by {@link ASTHelpers#getType(Tree)}.
   *
   * <p>If {@code getType} returns {@code null}, the matcher returns false instead of calling {@code
   * pred}.
   */
  public static <T extends Tree> Matcher<T> typePredicateMatcher(TypePredicate pred) {
    return (tree, state) -> {
      Type type = getType(tree);
      return type != null && pred.apply(type, state);
    };
  }

  /** Matches an AST node if its type is an array type. */
  public static <T extends Tree> Matcher<T> isArrayType() {
    return typePredicateMatcher((type, state) -> state.getTypes().isArray(type));
  }

  /** Matches an AST node if its type is a primitive array type. */
  public static <T extends Tree> Matcher<T> isPrimitiveArrayType() {
    return typePredicateMatcher(
        (type, state) ->
            state.getTypes().isArray(type) && state.getTypes().elemtype(type).isPrimitive());
  }

  /** Matches an AST node if its type is a primitive type. */
  public static <T extends Tree> Matcher<T> isPrimitiveType() {
    return typePredicateMatcher((type, state) -> type.isPrimitive());
  }

  /** Matches an AST node if its type is either a primitive type or a {@code void} type. */
  public static <T extends Tree> Matcher<T> isPrimitiveOrVoidType() {
    return typePredicateMatcher((type, state) -> type.isPrimitiveOrVoid());
  }

  /** Matches an AST node if its type is a {@code void} type. */
  public static <T extends Tree> Matcher<T> isVoidType() {
    return typePredicateMatcher(
        (type, state) -> state.getTypes().isSameType(type, state.getSymtab().voidType));
  }

  /**
   * Matches an AST node if its type is a primitive type, or a boxed version of a primitive type.
   */
  public static <T extends Tree> Matcher<T> isPrimitiveOrBoxedPrimitiveType() {
    return typePredicateMatcher(
        (type, state) -> state.getTypes().unboxedTypeOrType(type).isPrimitive());
  }

  /** Matches an AST node if its type is a boxed primitive type. */
  public static Matcher<ExpressionTree> isBoxedPrimitiveType() {
    return typePredicateMatcher(
        (type, state) ->
            !state.getTypes().isSameType(state.getTypes().unboxedType(type), Type.noType));
  }

  /** Matches an AST node which is enclosed by a block node that matches the given matcher. */
  public static <T extends Tree> Enclosing.Block<T> enclosingBlock(Matcher<BlockTree> matcher) {
    return new Enclosing.Block<>(matcher);
  }

  /** Matches an AST node which is enclosed by a class node that matches the given matcher. */
  public static <T extends Tree> Enclosing.Class<T> enclosingClass(Matcher<ClassTree> matcher) {
    return new Enclosing.Class<>(matcher);
  }

  /** Matches an AST node which is enclosed by a method node that matches the given matcher. */
  public static <T extends Tree> Enclosing.Method<T> enclosingMethod(Matcher<MethodTree> matcher) {
    return new Enclosing.Method<>(matcher);
  }

  /**
   * Matches an AST node that is enclosed by some node that matches the given matcher.
   *
   * <p>TODO(eaftan): This could be used instead of enclosingBlock and enclosingClass.
   */
  public static Matcher<Tree> enclosingNode(Matcher<Tree> matcher) {
    return (t, state) -> {
      TreePath path = state.getPath().getParentPath();
      while (path != null) {
        Tree node = path.getLeaf();
        state = state.withPath(path);
        if (matcher.matches(node, state)) {
          return true;
        }
        path = path.getParentPath();
      }
      return false;
    };
  }

  private static boolean siblingStatement(
      int offset, Matcher<StatementTree> matcher, StatementTree statement, VisitorState state) {
    // TODO(cushon): walking arbitrarily far up to find a block tree often isn't what we want
    TreePath blockPath = state.findPathToEnclosing(BlockTree.class);
    if (blockPath == null) {
      return false;
    }
    BlockTree block = (BlockTree) blockPath.getLeaf();
    List<? extends StatementTree> statements = block.getStatements();
    int idx = statements.indexOf(statement);
    if (idx == -1) {
      return false;
    }
    idx += offset;
    if (idx < 0 || idx >= statements.size()) {
      return false;
    }
    StatementTree sibling = statements.get(idx);
    return matcher.matches(sibling, state.withPath(new TreePath(blockPath, sibling)));
  }

  /**
   * Matches a statement AST node if the following statement in the enclosing block matches the
   * given matcher.
   */
  public static <T extends StatementTree> Matcher<T> nextStatement(Matcher<StatementTree> matcher) {
    return (statement, state) -> siblingStatement(/* offset= */ 1, matcher, statement, state);
  }

  /**
   * Matches a statement AST node if the previous statement in the enclosing block matches the given
   * matcher.
   */
  public static <T extends StatementTree> Matcher<T> previousStatement(
      Matcher<StatementTree> matcher) {
    return (statement, state) -> siblingStatement(/* offset= */ -1, matcher, statement, state);
  }

  /** Matches a statement AST node if the statement is the last statement in the block. */
  public static Matcher<StatementTree> isLastStatementInBlock() {
    return (statement, state) -> {
      // TODO(cushon): walking arbitrarily far up to find a block tree often isn't what we want
      TreePath blockPath = state.findPathToEnclosing(BlockTree.class);
      if (blockPath == null) {
        return false;
      }
      BlockTree block = (BlockTree) blockPath.getLeaf();
      return getLast(block.getStatements()).equals(statement);
    };
  }

  /** Matches an AST node if it is a literal other than null. */
  public static Matcher<ExpressionTree> nonNullLiteral() {
    return (tree, state) -> {
      switch (tree.getKind()) {
        case MEMBER_SELECT:
          return ((MemberSelectTree) tree).getIdentifier().contentEquals("class");
        case INT_LITERAL:
        case LONG_LITERAL:
        case FLOAT_LITERAL:
        case DOUBLE_LITERAL:
        case BOOLEAN_LITERAL:
        case CHAR_LITERAL:
          // fall through
        case STRING_LITERAL:
          return true;
        default:
          return false;
      }
    };
  }

  /**
   * Matches a Literal AST node if it is a string literal with the given value. For example, {@code
   * stringLiteral("thing")} matches the literal {@code "thing"}
   */
  public static Matcher<ExpressionTree> stringLiteral(String value) {
    return new StringLiteral(value);
  }

  /**
   * Matches a Literal AST node if it is a string literal which matches the given {@link Pattern}.
   *
   * @see #stringLiteral(String)
   */
  public static Matcher<ExpressionTree> stringLiteral(Pattern pattern) {
    return new StringLiteral(pattern);
  }

  public static Matcher<ExpressionTree> booleanLiteral(boolean value) {
    return (expressionTree, state) ->
        expressionTree.getKind() == Kind.BOOLEAN_LITERAL
            && value == (Boolean) (((LiteralTree) expressionTree).getValue());
  }

  /**
   * Matches the boolean constant ({@link Boolean#TRUE} or {@link Boolean#FALSE}) corresponding to
   * the given value.
   */
  public static Matcher<ExpressionTree> booleanConstant(boolean value) {
    return (expressionTree, state) -> {
      if (expressionTree instanceof JCFieldAccess) {
        Symbol symbol = getSymbol(expressionTree);
        if (symbol.isStatic()
            && state.getTypes().unboxedTypeOrType(symbol.type).getTag() == TypeTag.BOOLEAN) {
          if (value) {
            return symbol.getSimpleName().contentEquals("TRUE");
          } else {
            return symbol.getSimpleName().contentEquals("FALSE");
          }
        }
      }
      return false;
    };
  }

  /**
   * Ignores any number of parenthesis wrapping an expression and then applies the passed matcher to
   * that expression. For example, the passed matcher would be applied to {@code value} in {@code
   * (((value)))}.
   */
  public static Matcher<ExpressionTree> ignoreParens(Matcher<ExpressionTree> innerMatcher) {
    return (tree, state) -> innerMatcher.matches(stripParentheses(tree), state);
  }

  public static Matcher<ExpressionTree> intLiteral(int value) {
    return (tree, state) -> {
      return tree.getKind() == Kind.INT_LITERAL
          && value == ((Integer) ((LiteralTree) tree).getValue());
    };
  }

  public static Matcher<ExpressionTree> classLiteral(
      final Matcher<? super ExpressionTree> classMatcher) {
    return (tree, state) -> {
      if (tree.getKind() == Kind.MEMBER_SELECT) {
        MemberSelectTree select = (MemberSelectTree) tree;
        return select.getIdentifier().contentEquals("class")
            && classMatcher.matches(select.getExpression(), state);
      }
      return false;
    };
  }

  /**
   * Matches an Annotation AST node if the argument to the annotation with the given name has a
   * value which matches the given matcher. For example, {@code hasArgumentWithValue("value",
   * stringLiteral("one"))} matches {@code @Thing("one")} or {@code @Thing({"one", "two"})} or
   * {@code @Thing(value = "one")}
   */
  public static Matcher<AnnotationTree> hasArgumentWithValue(
      String argumentName, Matcher<ExpressionTree> valueMatcher) {
    return new AnnotationHasArgumentWithValue(argumentName, valueMatcher);
  }

  /** Matches an Annotation AST node if an argument to the annotation does not exist. */
  public static Matcher<AnnotationTree> doesNotHaveArgument(String argumentName) {
    return new AnnotationDoesNotHaveArgument(argumentName);
  }

  public static Matcher<AnnotationTree> isType(String annotationClassName) {
    return new AnnotationType(annotationClassName);
  }

  /**
   * Matches a {@link MethodInvocation} when the arguments at the two given indices are both the
   * same variable, as determined by {@link ASTHelpers#sameVariable}.
   *
   * @param index1 the index of the first actual parameter to test
   * @param index2 the index of the second actual parameter to test
   * @throws IndexOutOfBoundsException if the given indices are invalid
   */
  public static Matcher<? super MethodInvocationTree> sameArgument(int index1, int index2) {
    return (tree, state) -> {
      List<? extends ExpressionTree> args = tree.getArguments();
      return ASTHelpers.sameVariable(args.get(index1), args.get(index2));
    };
  }

  /**
   * Determines whether an expression has an annotation of the given type. This includes annotations
   * inherited from superclasses due to @Inherited.
   *
   * @param annotationClass the binary class name of the annotation (e.g.
   *     "javax.annotation.Nullable", or "some.package.OuterClassName$InnerClassName")
   */
  public static <T extends Tree> Matcher<T> hasAnnotation(String annotationClass) {
    Supplier<Set<Name>> name =
        VisitorState.memoize(
            state -> ImmutableSet.of(state.binaryNameFromClassname(annotationClass)));
    return (T tree, VisitorState state) ->
        !ASTHelpers.annotationsAmong(ASTHelpers.getDeclaredSymbol(tree), name.get(state), state)
            .isEmpty();
  }

  /**
   * Determines if an expression has an annotation referred to by the given mirror. Accounts for
   * binary names and annotations inherited due to @Inherited.
   *
   * @param annotationMirror mirror referring to the annotation type
   */
  public static Matcher<Tree> hasAnnotation(TypeMirror annotationMirror) {
    String annotationName = annotationMirror.toString();
    return (Tree tree, VisitorState state) -> {
      JavacProcessingEnvironment javacEnv = JavacProcessingEnvironment.instance(state.context);
      TypeElement typeElem = (TypeElement) javacEnv.getTypeUtils().asElement(annotationMirror);
      String name;
      if (typeElem != null) {
        // Get the binary name if possible ($ to separate nested members). See b/36160747
        name = javacEnv.getElementUtils().getBinaryName(typeElem).toString();
      } else {
        name = annotationName;
      }
      return ASTHelpers.hasAnnotation(ASTHelpers.getDeclaredSymbol(tree), name, state);
    };
  }

  /**
   * Determines whether an expression has an annotation with the given simple name. This does not
   * include annotations inherited from superclasses due to @Inherited.
   *
   * @param simpleName the simple name of the annotation (e.g. "Nullable")
   */
  public static <T extends Tree> Matcher<T> hasAnnotationWithSimpleName(String simpleName) {
    return (tree, state) ->
        ASTHelpers.hasDirectAnnotationWithSimpleName(
            ASTHelpers.getDeclaredSymbol(tree), simpleName);
  }

  /**
   * Determines whether an expression refers to a symbol that has an annotation of the given type.
   * This includes annotations inherited from superclasses due to @Inherited.
   *
   * @param annotationClass the binary class name of the annotation (e.g.
   *     "javax.annotation.Nullable", or "some.package.OuterClassName$InnerClassName")
   */
  public static <T extends Tree> Matcher<T> symbolHasAnnotation(String annotationClass) {
    return symbolMatcher(
        (symbol, state) -> ASTHelpers.hasAnnotation(symbol, annotationClass, state));
  }

  /**
   * Determines whether an expression has an annotation of the given class. This includes
   * annotations inherited from superclasses due to @Inherited.
   *
   * @param inputClass The class of the annotation to look for (e.g, Produces.class).
   */
  public static <T extends Tree> Matcher<T> hasAnnotation(Class<? extends Annotation> inputClass) {
    return (tree, state) ->
        ASTHelpers.hasAnnotation(ASTHelpers.getDeclaredSymbol(tree), inputClass, state);
  }

  /**
   * Determines whether an expression refers to a symbol that has an annotation of the given type.
   * This includes annotations inherited from superclasses due to @Inherited.
   *
   * @param inputClass The class of the annotation to look for (e.g, Produces.class).
   */
  public static <T extends Tree> Matcher<T> symbolHasAnnotation(
      Class<? extends Annotation> inputClass) {
    return (tree, state) -> ASTHelpers.hasAnnotation(getSymbol(tree), inputClass, state);
  }

  /**
   * Matches if a method or any method it overrides has an annotation of the given type. JUnit 4's
   * {@code @Test}, {@code @Before}, and {@code @After} annotations behave this way.
   *
   * @param annotationClass the binary class name of the annotation (e.g.
   *     "javax.annotation.Nullable", or "some.package.OuterClassName$InnerClassName")
   */
  public static Matcher<MethodTree> hasAnnotationOnAnyOverriddenMethod(String annotationClass) {
    return (tree, state) -> {
      MethodSymbol methodSym = getSymbol(tree);
      if (methodSym == null) {
        return false;
      }
      if (ASTHelpers.hasAnnotation(methodSym, annotationClass, state)) {
        return true;
      }
      for (MethodSymbol method : ASTHelpers.findSuperMethods(methodSym, state.getTypes())) {
        if (ASTHelpers.hasAnnotation(method, annotationClass, state)) {
          return true;
        }
      }
      return false;
    };
  }

  /** Matches a method invocation that is known to never return null. */
  public static Matcher<ExpressionTree> methodReturnsNonNull() {
    return anyOf(
        instanceMethod().onDescendantOf("java.lang.Object").named("toString"),
        instanceMethod().onExactClass("java.lang.String"),
        staticMethod().onClass("java.lang.String"),
        instanceMethod().onExactClass("java.util.StringTokenizer").named("nextToken"));
  }

  public static Matcher<MethodTree> methodReturns(Matcher<? super Tree> returnTypeMatcher) {
    return (methodTree, state) -> {
      Tree returnTree = methodTree.getReturnType();
      // Constructors have no return type.
      return returnTree != null && returnTypeMatcher.matches(returnTree, state);
    };
  }

  public static Matcher<MethodTree> methodReturns(Supplier<Type> returnType) {
    return methodReturns(isSameType(returnType));
  }

  /** Match a method that returns a non-primitive type. */
  public static Matcher<MethodTree> methodReturnsNonPrimitiveType() {
    return methodReturns(not(isPrimitiveOrVoidType()));
  }

  /**
   * Match a method declaration with a specific name.
   *
   * @param methodName The name of the method to match, e.g., "equals"
   */
  public static Matcher<MethodTree> methodIsNamed(String methodName) {
    return (methodTree, state) -> methodTree.getName().contentEquals(methodName);
  }

  /**
   * Match a method declaration that starts with a given string.
   *
   * @param prefix The prefix.
   */
  public static Matcher<MethodTree> methodNameStartsWith(String prefix) {
    return (methodTree, state) -> methodTree.getName().toString().startsWith(prefix);
  }

  /**
   * Match a method declaration with a specific enclosing class and method name.
   *
   * @param className The fully-qualified name of the enclosing class, e.g.
   *     "com.google.common.base.Preconditions"
   * @param methodName The name of the method to match, e.g., "checkNotNull"
   */
  public static Matcher<MethodTree> methodWithClassAndName(String className, String methodName) {
    return (methodTree, state) ->
        getSymbol(methodTree).getEnclosingElement().getQualifiedName().contentEquals(className)
            && methodTree.getName().contentEquals(methodName);
  }

  /**
   * Matches an AST node that represents a method declaration, based on the list of
   * variableMatchers. Applies the variableMatcher at index n to the parameter at index n and
   * returns true iff they all match. Returns false if the number of variableMatchers provided does
   * not match the number of parameters.
   *
   * <p>If you pass no variableMatchers, this will match methods with no parameters.
   *
   * @param variableMatcher an array of matchers to apply to the parameters of the method
   */
  @SafeVarargs
  public static Matcher<MethodTree> methodHasParameters(
      final Matcher<VariableTree>... variableMatcher) {
    return methodHasParameters(ImmutableList.copyOf(variableMatcher));
  }

  /**
   * Matches an AST node that represents a method declaration, based on the list of
   * variableMatchers. Applies the variableMatcher at index n to the parameter at index n and
   * returns true iff they all match. Returns false if the number of variableMatchers provided does
   * not match the number of parameters.
   *
   * <p>If you pass no variableMatchers, this will match methods with no parameters.
   *
   * @param variableMatcher a list of matchers to apply to the parameters of the method
   */
  public static Matcher<MethodTree> methodHasParameters(
      final List<Matcher<VariableTree>> variableMatcher) {
    return (methodTree, state) -> {
      if (methodTree.getParameters().size() != variableMatcher.size()) {
        return false;
      }
      int paramIndex = 0;
      for (Matcher<VariableTree> eachVariableMatcher : variableMatcher) {
        if (!eachVariableMatcher.matches(methodTree.getParameters().get(paramIndex++), state)) {
          return false;
        }
      }
      return true;
    };
  }

  /** Matches if the given matcher matches all of/any of the parameters to this method. */
  public static MultiMatcher<MethodTree, VariableTree> methodHasParameters(
      MatchType matchType, Matcher<VariableTree> parameterMatcher) {
    return new MethodHasParameters(matchType, parameterMatcher);
  }

  public static Matcher<MethodTree> methodHasVisibility(Visibility visibility) {
    return new MethodVisibility(visibility);
  }

  public static Matcher<MethodTree> methodIsConstructor() {
    return (methodTree, state) -> getSymbol(methodTree).isConstructor();
  }

  /**
   * Matches a constructor declaration in a specific enclosing class.
   *
   * @param className The fully-qualified name of the enclosing class, e.g.
   *     "com.google.common.base.Preconditions"
   */
  public static Matcher<MethodTree> constructorOfClass(String className) {
    return (methodTree, state) -> {
      Symbol symbol = getSymbol(methodTree);
      return symbol.getEnclosingElement().getQualifiedName().contentEquals(className)
          && symbol.isConstructor();
    };
  }

  /**
   * Matches a class in which at least one method matches the given methodMatcher.
   *
   * @param methodMatcher A matcher on MethodTrees to run against all methods in this class.
   * @return True if some method in the class matches the given methodMatcher.
   */
  public static Matcher<ClassTree> hasMethod(Matcher<MethodTree> methodMatcher) {
    return (t, state) -> {
      for (Tree member : t.getMembers()) {
        if (member instanceof MethodTree && methodMatcher.matches((MethodTree) member, state)) {
          return true;
        }
      }
      return false;
    };
  }

  /**
   * Matches on the type of a VariableTree AST node.
   *
   * @param treeMatcher A matcher on the type of the variable.
   */
  public static Matcher<VariableTree> variableType(Matcher<Tree> treeMatcher) {
    return (variableTree, state) -> treeMatcher.matches(variableTree.getType(), state);
  }

  /**
   * Matches on the initializer of a VariableTree AST node.
   *
   * @param expressionTreeMatcher A matcher on the initializer of the variable.
   */
  public static Matcher<VariableTree> variableInitializer(
      Matcher<ExpressionTree> expressionTreeMatcher) {
    return (variableTree, state) -> {
      ExpressionTree initializer = variableTree.getInitializer();
      return initializer != null && expressionTreeMatcher.matches(initializer, state);
    };
  }

  /**
   * Matches if a {@link VariableTree} is a field declaration, as opposed to a local variable, enum
   * constant, parameter to a method, etc.
   */
  public static Matcher<VariableTree> isField() {
    return (variableTree, state) -> ElementKind.FIELD == getSymbol(variableTree).getKind();
  }

  /** Matches if a {@link ClassTree} is an enum declaration. */
  public static Matcher<ClassTree> isEnum() {
    return (classTree, state) -> getSymbol(classTree).getKind() == ElementKind.ENUM;
  }

  /**
   * Matches an class based on whether it is nested in another class or method.
   *
   * @param kind The kind of nesting to match, eg ANONYMOUS, LOCAL, MEMBER, TOP_LEVEL
   */
  public static Matcher<ClassTree> nestingKind(NestingKind kind) {
    return (classTree, state) -> kind == getSymbol(classTree).getNestingKind();
  }

  /**
   * Matches a binary tree if the given matchers match the operands in either order. That is,
   * returns true if either: matcher1 matches the left operand and matcher2 matches the right
   * operand or matcher2 matches the left operand and matcher1 matches the right operand
   */
  public static Matcher<BinaryTree> binaryTree(
      Matcher<ExpressionTree> matcher1, Matcher<ExpressionTree> matcher2) {
    return (t, state) ->
        null != ASTHelpers.matchBinaryTree(t, Arrays.asList(matcher1, matcher2), state);
  }

  /**
   * Matches any AST that contains an identifier with a certain property. This matcher can be used,
   * for instance, to locate identifiers with a certain name or which is defined in a certain class.
   *
   * @param nodeMatcher Which identifiers to look for
   */
  public static Matcher<Tree> hasIdentifier(Matcher<IdentifierTree> nodeMatcher) {
    return new HasIdentifier(nodeMatcher);
  }

  /** Returns true if the Tree node has the expected {@code Modifier}. */
  public static <T extends Tree> Matcher<T> hasModifier(Modifier modifier) {
    return (tree, state) -> {
      Symbol sym = getSymbol(tree);
      return sym != null && sym.getModifiers().contains(modifier);
    };
  }

  /** Matches an AST node which is an expression yielding the indicated static field access. */
  public static Matcher<ExpressionTree> staticFieldAccess() {
    return allOf(isStatic(), isSymbol(VarSymbol.class));
  }

  /** Matches an AST node that is static. */
  public static <T extends Tree> Matcher<T> isStatic() {
    return (tree, state) -> {
      Symbol sym = getSymbol(tree);
      return sym != null && sym.isStatic();
    };
  }

  /** Matches an AST node that is transient. */
  public static <T extends Tree> Matcher<T> isTransient() {
    return (tree, state) -> getSymbol(tree).getModifiers().contains(Modifier.TRANSIENT);
  }

  /**
   * Matches a {@code throw} statement where the thrown item is matched by the passed {@code
   * thrownMatcher}.
   */
  public static Matcher<StatementTree> throwStatement(
      Matcher<? super ExpressionTree> thrownMatcher) {
    return new Throws(thrownMatcher);
  }

  /**
   * Matches a {@code return} statement where the returned expression is matched by the passed
   * {@code returnedMatcher}.
   */
  public static Matcher<StatementTree> returnStatement(
      Matcher<? super ExpressionTree> returnedMatcher) {
    return new Returns(returnedMatcher);
  }

  /**
   * Matches an {@code assert} statement where the condition is matched by the passed {@code
   * conditionMatcher}.
   */
  public static Matcher<StatementTree> assertStatement(Matcher<ExpressionTree> conditionMatcher) {
    return new Asserts(conditionMatcher);
  }

  /** Matches a {@code continue} statement. */
  public static Matcher<StatementTree> continueStatement() {
    return (statementTree, state) -> statementTree instanceof ContinueTree;
  }

  /** Matches an {@link ExpressionStatementTree} based on its {@link ExpressionTree}. */
  public static Matcher<StatementTree> expressionStatement(Matcher<ExpressionTree> matcher) {
    return (statementTree, state) ->
        statementTree instanceof ExpressionStatementTree
            && matcher.matches(((ExpressionStatementTree) statementTree).getExpression(), state);
  }

  static Matcher<Tree> isSymbol(java.lang.Class<? extends Symbol> symbolClass) {
    return new IsSymbol(symbolClass);
  }

  /**
   * Converts the given matcher to one that can be applied to any tree but is only executed when run
   * on a tree of {@code type} and returns {@code false} for all other tree types.
   */
  public static <S extends T, T extends Tree> Matcher<T> toType(
      Class<S> type, Matcher<? super S> matcher) {
    return (tree, state) -> type.isInstance(tree) && matcher.matches(type.cast(tree), state);
  }

  /** Matches if this Tree is enclosed by either a synchronized block or a synchronized method. */
  public static <T extends Tree> Matcher<T> inSynchronized() {
    return (tree, state) -> {
      SynchronizedTree synchronizedTree =
          ASTHelpers.findEnclosingNode(state.getPath(), SynchronizedTree.class);
      if (synchronizedTree != null) {
        return true;
      }

      MethodTree methodTree = ASTHelpers.findEnclosingNode(state.getPath(), MethodTree.class);
      return methodTree != null
          && methodTree.getModifiers().getFlags().contains(Modifier.SYNCHRONIZED);
    };
  }

  /**
   * Matches if this ExpressionTree refers to the same variable as the one passed into the matcher.
   */
  public static Matcher<ExpressionTree> sameVariable(ExpressionTree expr) {
    return (tree, state) -> ASTHelpers.sameVariable(tree, expr);
  }

  /** Matches if the expression is provably non-null. */
  public static Matcher<ExpressionTree> isNonNull() {
    return new NullnessMatcher(Nullness.NONNULL);
  }

  /** Matches if the expression is provably null. */
  public static Matcher<ExpressionTree> isNull() {
    return new NullnessMatcher(Nullness.NULL);
  }

  /**
   * Matches an enhanced for loop if all the given matchers match.
   *
   * @param variableMatcher The matcher to apply to the variable.
   * @param expressionMatcher The matcher to apply to the expression.
   * @param statementMatcher The matcher to apply to the statement.
   */
  public static Matcher<EnhancedForLoopTree> enhancedForLoop(
      Matcher<VariableTree> variableMatcher,
      Matcher<ExpressionTree> expressionMatcher,
      Matcher<StatementTree> statementMatcher) {
    return (t, state) ->
        variableMatcher.matches(t.getVariable(), state)
            && expressionMatcher.matches(t.getExpression(), state)
            && statementMatcher.matches(t.getStatement(), state);
  }

  /** Matches if the given tree is inside a loop. */
  public static <T extends Tree> Matcher<T> inLoop() {
    return (tree, state) -> {
      TreePath path = state.getPath().getParentPath();
      while (path != null) {
        Tree node = path.getLeaf();
        switch (node.getKind()) {
          case METHOD:
          case CLASS:
            return false;
          case WHILE_LOOP:
          case FOR_LOOP:
          case ENHANCED_FOR_LOOP:
          case DO_WHILE_LOOP:
            return true;
          default:
            // continue below
        }
        path = path.getParentPath();
      }
      return false;
    };
  }

  /**
   * Matches an assignment operator AST node if both of the given matchers match.
   *
   * @param variableMatcher The matcher to apply to the variable.
   * @param expressionMatcher The matcher to apply to the expression.
   */
  public static Matcher<AssignmentTree> assignment(
      Matcher<ExpressionTree> variableMatcher, Matcher<? super ExpressionTree> expressionMatcher) {
    return (t, state) ->
        variableMatcher.matches(t.getVariable(), state)
            && expressionMatcher.matches(t.getExpression(), state);
  }

  /**
   * Matches a type cast AST node if both of the given matchers match.
   *
   * @param typeMatcher The matcher to apply to the type.
   * @param expressionMatcher The matcher to apply to the expression.
   */
  public static Matcher<TypeCastTree> typeCast(
      Matcher<Tree> typeMatcher, Matcher<ExpressionTree> expressionMatcher) {
    return (t, state) ->
        typeMatcher.matches(t.getType(), state)
            && expressionMatcher.matches(t.getExpression(), state);
  }

  /**
   * Matches an assertion AST node if the given matcher matches its condition.
   *
   * @param conditionMatcher The matcher to apply to the condition in the assertion, e.g. in "assert
   *     false", the "false" part of the statement
   */
  public static Matcher<AssertTree> assertionWithCondition(
      Matcher<ExpressionTree> conditionMatcher) {
    return (tree, state) -> conditionMatcher.matches(tree.getCondition(), state);
  }

  /**
   * Applies the given matcher recursively to all descendants of an AST node, and matches if any
   * matching descendant node is found.
   *
   * @param treeMatcher The matcher to apply recursively to the tree.
   */
  public static Matcher<Tree> contains(Matcher<Tree> treeMatcher) {
    return new Contains(treeMatcher);
  }

  /**
   * Applies the given matcher recursively to all descendants of an AST node, and matches if any
   * matching descendant node is found.
   *
   * @param clazz The type of node to be matched.
   * @param treeMatcher The matcher to apply recursively to the tree.
   */
  public static <T extends Tree, V extends Tree> Matcher<T> contains(
      Class<V> clazz, Matcher<V> treeMatcher) {
    final Matcher<Tree> contains = new Contains(toType(clazz, treeMatcher));
    return contains::matches;
  }

  /**
   * Matches if the method accepts the given number of arguments.
   *
   * @param arity the number of arguments the method should accept
   */
  public static Matcher<MethodTree> methodHasArity(int arity) {
    return (methodTree, state) -> methodTree.getParameters().size() == arity;
  }

  /**
   * Matches any node that is directly an implementation, but not extension, of the given Class.
   *
   * <p>E.x. {@code class C implements I} will match, but {@code class C extends A} will not.
   *
   * <p>Additionally, this will only match <i>direct</i> implementations of interfaces. E.g. the
   * following will not match:
   *
   * <p>{@code interface I1 {} interface I2 extends I1 {} class C implements I2 {} ...
   * isDirectImplementationOf(I1).match(\/*class tree for C*\/); // will not match }
   */
  public static Matcher<ClassTree> isDirectImplementationOf(String clazz) {
    Matcher<Tree> isProvidedType = isSameType(clazz);
    return new IsDirectImplementationOf(isProvidedType);
  }

  @SafeVarargs
  public static Matcher<Tree> hasAnyAnnotation(Class<? extends Annotation>... annotations) {
    ArrayList<Matcher<Tree>> matchers = new ArrayList<>(annotations.length);
    for (Class<? extends Annotation> annotation : annotations) {
      matchers.add(hasAnnotation(annotation));
    }
    return anyOf(matchers);
  }

  public static Matcher<Tree> hasAnyAnnotation(List<? extends TypeMirror> mirrors) {
    ArrayList<Matcher<Tree>> matchers = new ArrayList<>(mirrors.size());
    for (TypeMirror mirror : mirrors) {
      matchers.add(hasAnnotation(mirror));
    }
    return anyOf(matchers);
  }

  private static final ImmutableSet<Kind> DECLARATION =
      Sets.immutableEnumSet(Kind.LAMBDA_EXPRESSION, Kind.CLASS, Kind.ENUM, Kind.INTERFACE);

  public static boolean methodCallInDeclarationOfThrowingRunnable(VisitorState state) {
    return stream(state.getPath())
        // Find the nearest definitional context for this method invocation
        // (i.e.: the nearest surrounding class or lambda)
        .filter(t -> DECLARATION.contains(t.getKind()))
        .findFirst()
        .map(t -> isThrowingFunctionalInterface(getType(t), state))
        .orElseThrow(VerifyException::new);
  }

  public static boolean isThrowingFunctionalInterface(Type clazzType, VisitorState state) {
    return CLASSES_CONSIDERED_THROWING.get(state).stream()
        .anyMatch(t -> isSubtype(clazzType, t, state));
  }
  /**
   * {@link FunctionalInterface}s that are generally used as a lambda expression for 'a block of
   * code that's going to fail', e.g.:
   *
   * <p>{@code assertThrows(FooException.class, () -> myCodeThatThrowsAnException());
   * errorCollector.checkThrows(FooException.class, () -> myCodeThatThrowsAnException()); }
   */
  // TODO(glorioso): Consider a meta-annotation like @LikelyToThrow instead/in addition?
  private static final Supplier<ImmutableSet<Type>> CLASSES_CONSIDERED_THROWING =
      VisitorState.memoize(
          state ->
              Stream.of(
                      "org.junit.function.ThrowingRunnable",
                      "org.junit.jupiter.api.function.Executable",
                      "org.assertj.core.api.ThrowableAssert$ThrowingCallable",
                      "com.google.devtools.build.lib.testutil.MoreAsserts$ThrowingRunnable",
                      "com.google.truth.ExpectFailure.AssertionCallback",
                      "com.google.truth.ExpectFailure.DelegatedAssertionCallback",
                      "com.google.truth.ExpectFailure.StandardSubjectBuilderCallback",
                      "com.google.truth.ExpectFailure.SimpleSubjectBuilderCallback")
                  .map(state::getTypeFromString)
                  .filter(Objects::nonNull)
                  .collect(toImmutableSet()));

  private static class IsDirectImplementationOf extends ChildMultiMatcher<ClassTree, Tree> {
    public IsDirectImplementationOf(Matcher<Tree> classMatcher) {
      super(MatchType.AT_LEAST_ONE, classMatcher);
    }

    @Override
    protected Iterable<? extends Tree> getChildNodes(ClassTree classTree, VisitorState state) {
      return classTree.getImplementsClause();
    }
  }

  /** Matches an AST node whose compilation unit's package name matches the given pattern. */
  public static <T extends Tree> Matcher<T> packageMatches(Pattern pattern) {
    return (tree, state) -> pattern.matcher(getPackageFullName(state)).matches();
  }

  /** Matches an AST node whose compilation unit starts with this prefix. */
  public static <T extends Tree> Matcher<T> packageStartsWith(String prefix) {
    return (tree, state) -> getPackageFullName(state).startsWith(prefix);
  }

  private static String getPackageFullName(VisitorState state) {
    JCCompilationUnit compilationUnit = (JCCompilationUnit) state.getPath().getCompilationUnit();
    return compilationUnit.packge.fullname.toString();
  }

  private static final Matcher<ExpressionTree> STATIC_EQUALS =
      anyOf(
          allOf(
              staticMethod()
                  .onClass("android.support.v4.util.ObjectsCompat")
                  .named("equals")
                  .withParameters("java.lang.Object", "java.lang.Object"),
              Matchers::methodReturnsBoolean),
          allOf(
              staticMethod()
                  .onClass("java.util.Objects")
                  .named("equals")
                  .withParameters("java.lang.Object", "java.lang.Object"),
              Matchers::methodReturnsBoolean),
          allOf(
              staticMethod()
                  .onClass("com.google.common.base.Objects")
                  .named("equal")
                  .withParameters("java.lang.Object", "java.lang.Object"),
              Matchers::methodReturnsBoolean));

  /**
   * Matches an invocation of a recognized static object equality method such as {@link
   * java.util.Objects#equals}. These are simple facades to {@link Object#equals} that accept null
   * for either argument.
   */
  @SuppressWarnings("unchecked") // safe covariant cast
  public static <T extends ExpressionTree> Matcher<T> staticEqualsInvocation() {
    return (Matcher<T>) STATIC_EQUALS;
  }

  private static final Matcher<ExpressionTree> INSTANCE_EQUALS =
      allOf(
          instanceMethod().anyClass().named("equals").withParameters("java.lang.Object"),
          Matchers::methodReturnsBoolean);

  private static boolean methodReturnsBoolean(ExpressionTree tree, VisitorState state) {
    return ASTHelpers.isSameType(
        getSymbol(tree).type.getReturnType(), state.getSymtab().booleanType, state);
  }

  /** Matches calls to the method {@link Object#equals(Object)} or any override of that method. */
  @SuppressWarnings("unchecked") // safe covariant cast
  public static <T extends ExpressionTree> Matcher<T> instanceEqualsInvocation() {
    return (Matcher<T>) INSTANCE_EQUALS;
  }

  private static final Matcher<ExpressionTree> INSTANCE_HASHCODE =
      allOf(instanceMethod().anyClass().named("hashCode").withParameters(), isSameType(INT_TYPE));

  /** Matches calls to the method {@link Object#hashCode()} or any override of that method. */
  public static Matcher<ExpressionTree> instanceHashCodeInvocation() {
    return INSTANCE_HASHCODE;
  }

  private static final Matcher<ExpressionTree> ASSERT_EQUALS =
      staticMethod()
          .onClassAny("org.junit.Assert", "junit.framework.Assert", "junit.framework.TestCase")
          .named("assertEquals");

  /**
   * Matches calls to the method {@code org.junit.Assert#assertEquals} and corresponding methods in
   * JUnit 3.x.
   */
  public static Matcher<ExpressionTree> assertEqualsInvocation() {
    return ASSERT_EQUALS;
  }

  private static final Matcher<ExpressionTree> ASSERT_NOT_EQUALS =
      staticMethod()
          .onClassAny("org.junit.Assert", "junit.framework.Assert", "junit.framework.TestCase")
          .named("assertNotEquals");

  /**
   * Matches calls to the method {@code org.junit.Assert#assertNotEquals} and corresponding methods
   * in JUnit 3.x.
   */
  public static Matcher<ExpressionTree> assertNotEqualsInvocation() {
    return ASSERT_NOT_EQUALS;
  }

  private static final Matcher<MethodTree> EQUALS_DECLARATION =
      allOf(
          methodIsNamed("equals"),
          methodHasVisibility(Visibility.PUBLIC),
          methodHasParameters(variableType(isSameType("java.lang.Object"))),
          anyOf(methodReturns(BOOLEAN_TYPE), methodReturns(JAVA_LANG_BOOLEAN_TYPE)));

  /** Matches {@link Object#equals} method declaration. */
  public static Matcher<MethodTree> equalsMethodDeclaration() {
    return EQUALS_DECLARATION;
  }

  private static final Matcher<MethodTree> TO_STRING_DECLARATION =
      allOf(
          methodIsNamed("toString"),
          methodHasVisibility(Visibility.PUBLIC),
          methodHasParameters(),
          methodReturns(STRING_TYPE));

  /** Matches {@link Object#toString} method declaration. */
  public static Matcher<MethodTree> toStringMethodDeclaration() {
    return TO_STRING_DECLARATION;
  }

  private static final Matcher<MethodTree> HASH_CODE_DECLARATION =
      allOf(
          methodIsNamed("hashCode"),
          methodHasVisibility(Visibility.PUBLIC),
          methodHasParameters(),
          methodReturns(INT_TYPE));

  /** Matches {@code hashCode} method declaration. */
  public static Matcher<MethodTree> hashCodeMethodDeclaration() {
    return HASH_CODE_DECLARATION;
  }

  /** Method signature of serialization methods. */
  public static final Matcher<MethodTree> SERIALIZATION_METHODS =
      allOf(
          (t, s) -> isSubtype(getSymbol(t).owner.type, s.getSymtab().serializableType, s),
          anyOf(
              allOf(
                  methodIsNamed("readObject"),
                  methodHasParameters(isSameType("java.io.ObjectInputStream"))),
              allOf(
                  methodIsNamed("writeObject"),
                  methodHasParameters(isSameType("java.io.ObjectOutputStream"))),
              allOf(methodIsNamed("readObjectNoData"), methodReturns(isVoidType())),
              allOf(
                  methodIsNamed("readResolve"), methodReturns(typeFromString("java.lang.Object"))),
              allOf(
                  methodIsNamed("writeReplace"),
                  methodReturns(typeFromString("java.lang.Object")))));

  public static final Matcher<Tree> IS_INTERFACE =
      (t, s) -> {
        Symbol symbol = getSymbol(t);
        return symbol instanceof ClassSymbol && symbol.isInterface();
      };

  /**
   * Matches the {@code Tree} if it returns an expression matching {@code expressionTreeMatcher}.
   */
  public static final Matcher<StatementTree> matchExpressionReturn(
      Matcher<ExpressionTree> expressionTreeMatcher) {
    return (statement, state) -> {
      if (!(statement instanceof ReturnTree)) {
        return false;
      }
      ExpressionTree expression = ((ReturnTree) statement).getExpression();
      if (expression == null) {
        return false;
      }
      return expressionTreeMatcher.matches(expression, state);
    };
  }

  /**
   * Matches a {@link BlockTree} if it single statement block with statement matching {@code
   * statementMatcher}.
   */
  public static final Matcher<BlockTree> matchSingleStatementBlock(
      Matcher<StatementTree> statementMatcher) {
    return (blockTree, state) -> {
      if (blockTree == null) {
        return false;
      }
      List<? extends StatementTree> statements = blockTree.getStatements();
      if (statements.size() != 1) {
        return false;
      }
      return statementMatcher.matches(getOnlyElement(statements), state);
    };
  }

  /**
   * Returns a matcher for {@link MethodTree} whose implementation contains a single return
   * statement with expression matching the passed {@code expressionTreeMatcher}.
   */
  public static Matcher<MethodTree> singleStatementReturnMatcher(
      Matcher<ExpressionTree> expressionTreeMatcher) {
    Matcher<BlockTree> matcher =
        matchSingleStatementBlock(matchExpressionReturn(expressionTreeMatcher));
    return (methodTree, state) -> {
      return matcher.matches(methodTree.getBody(), state);
    };
  }
}
