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

import static com.google.errorprone.suppliers.Suppliers.BOOLEAN_TYPE;
import static com.google.errorprone.suppliers.Suppliers.INT_TYPE;
import static com.google.errorprone.suppliers.Suppliers.JAVA_LANG_BOOLEAN_TYPE;
import static com.google.errorprone.suppliers.Suppliers.STRING_TYPE;
import static com.google.errorprone.suppliers.Suppliers.typeFromClass;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.stripParentheses;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.matchers.ChildMultiMatcher.MatchType;
import com.google.errorprone.matchers.MethodVisibility.Visibility;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.matchers.method.MethodMatchers.AnyMethodMatcher;
import com.google.errorprone.matchers.method.MethodMatchers.ConstructorMatcher;
import com.google.errorprone.matchers.method.MethodMatchers.InstanceMethodMatcher;
import com.google.errorprone.matchers.method.MethodMatchers.StaticMethodMatcher;
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
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.lang.model.element.Element;
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
    return new Matcher<T>() {
      @Override
      public boolean matches(T t, VisitorState state) {
        return true;
      }
    };
  }

  /** A matcher that matches no AST node. */
  public static <T extends Tree> Matcher<T> nothing() {
    return new Matcher<T>() {
      @Override
      public boolean matches(T t, VisitorState state) {
        return false;
      }
    };
  }

  /** Matches an AST node iff it does not match the given matcher. */
  public static <T extends Tree> Matcher<T> not(final Matcher<T> matcher) {
    return new Matcher<T>() {
      @Override
      public boolean matches(T t, VisitorState state) {
        return !matcher.matches(t, state);
      }
    };
  }

  /**
   * Compose several matchers together, such that the composite matches an AST node iff all the
   * given matchers do.
   */
  @SafeVarargs
  public static <T extends Tree> Matcher<T> allOf(final Matcher<? super T>... matchers) {
    return new Matcher<T>() {
      @Override
      public boolean matches(T t, VisitorState state) {
        for (Matcher<? super T> matcher : matchers) {
          if (!matcher.matches(t, state)) {
            return false;
          }
        }
        return true;
      }
    };
  }

  /**
   * Compose several matchers together, such that the composite matches an AST node if any of the
   * given matchers do.
   */
  public static <T extends Tree> Matcher<T> anyOf(
      final Iterable<? extends Matcher<? super T>> matchers) {
    return new Matcher<T>() {
      @Override
      public boolean matches(T t, VisitorState state) {
        for (Matcher<? super T> matcher : matchers) {
          if (matcher.matches(t, state)) {
            return true;
          }
        }
        return false;
      }
    };
  }

  @SafeVarargs
  public static <T extends Tree> Matcher<T> anyOf(final Matcher<? super T>... matchers) {
    return anyOf(Arrays.<Matcher<? super T>>asList(matchers));
  }

  /** Matches if an AST node is an instance of the given class. */
  public static <T extends Tree> Matcher<T> isInstance(final java.lang.Class<?> klass) {
    return new Matcher<T>() {
      @Override
      public boolean matches(T t, VisitorState state) {
        return klass.isInstance(t);
      }
    };
  }

  /** Matches an AST node of a given kind, for example, an Annotation or a switch block. */
  public static <T extends Tree> Matcher<T> kindIs(final Kind kind) {
    return new Matcher<T>() {
      @Override
      public boolean matches(T tree, VisitorState state) {
        return tree.getKind() == kind;
      }
    };
  }

  /** Matches an AST node which is the same object reference as the given node. */
  public static <T extends Tree> Matcher<T> isSame(final Tree t) {
    return new Matcher<T>() {
      @Override
      public boolean matches(T tree, VisitorState state) {
        return tree == t;
      }
    };
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

  /** @deprecated prefer {@link MethodMatchers#instanceMethod} */
  @Deprecated
  // TODO(cushon): expunge
  public static InstanceMethod instanceMethod(
      Matcher<? super ExpressionTree> receiverMatcher, String methodName) {
    return new InstanceMethod(receiverMatcher, methodName);
  }

  /** Matches an AST node that represents a non-static field. */
  public static Matcher<ExpressionTree> isInstanceField() {
    return new Matcher<ExpressionTree>() {
      @Override
      public boolean matches(ExpressionTree expressionTree, VisitorState state) {
        Symbol symbol = ASTHelpers.getSymbol(expressionTree);
        return symbol != null && symbol.getKind() == ElementKind.FIELD && !symbol.isStatic();
      }
    };
  }

  /** Matches an AST node that represents a local variable or parameter. */
  public static Matcher<ExpressionTree> isVariable() {
    return new Matcher<ExpressionTree>() {
      @Override
      public boolean matches(ExpressionTree expressionTree, VisitorState state) {
        Symbol symbol = ASTHelpers.getSymbol(expressionTree);
        if (symbol == null) {
          return false;
        }
        return symbol.getKind() == ElementKind.LOCAL_VARIABLE
            || symbol.getKind() == ElementKind.PARAMETER;
      }
    };
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
    return new Matcher<MethodInvocationTree>() {
      @Override
      public boolean matches(MethodInvocationTree t, VisitorState state) {
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
      }
    };
  }

  public static Matcher<MethodInvocationTree> receiverOfInvocation(
      final Matcher<ExpressionTree> expressionTreeMatcher) {
    return new Matcher<MethodInvocationTree>() {
      @Override
      public boolean matches(MethodInvocationTree methodInvocationTree, VisitorState state) {
        return expressionTreeMatcher.matches(ASTHelpers.getReceiver(methodInvocationTree), state);
      }
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
    return new Matcher<ExpressionTree>() {
      @Override
      public boolean matches(ExpressionTree expressionTree, VisitorState state) {
        if (!(expressionTree instanceof MethodInvocationTree)) {
          return false;
        }
        MethodInvocationTree tree = (MethodInvocationTree) expressionTree;
        return methodSelectMatcher.matches(tree.getMethodSelect(), state);
      }
    };
  }

  public static Matcher<MethodInvocationTree> argumentCount(final int argumentCount) {
    return new Matcher<MethodInvocationTree>() {
      @Override
      public boolean matches(MethodInvocationTree t, VisitorState state) {
        return t.getArguments().size() == argumentCount;
      }
    };
  }

  /**
   * Matches an AST node if its parent node is matched by the given matcher. For example, {@code
   * parentNode(kindIs(Kind.RETURN))} would match the {@code this} expression in {@code return
   * this;}
   */
  public static Matcher<Tree> parentNode(Matcher<? extends Tree> treeMatcher) {
    @SuppressWarnings("unchecked") // Safe contravariant cast
    Matcher<Tree> matcher = (Matcher<Tree>) treeMatcher;
    return new ParentNode(matcher);
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

  /** Matches an AST node if its type is an array type. */
  public static <T extends Tree> Matcher<T> isArrayType() {
    return new Matcher<T>() {
      @Override
      public boolean matches(Tree t, VisitorState state) {
        Type type = getType(t);
        return type != null && state.getTypes().isArray(type);
      }
    };
  }

  /** Matches an AST node if its type is a primitive array type. */
  public static <T extends Tree> Matcher<T> isPrimitiveArrayType() {
    return new Matcher<T>() {
      @Override
      public boolean matches(Tree t, VisitorState state) {
        Type type = getType(t);
        return type != null
            && state.getTypes().isArray(type)
            && state.getTypes().elemtype(type).isPrimitive();
      }
    };
  }

  /** Matches an AST node if its type is a primitive type. */
  public static <T extends Tree> Matcher<T> isPrimitiveType() {
    return new Matcher<T>() {
      @Override
      public boolean matches(Tree t, VisitorState state) {
        Type type = getType(t);
        return type != null && type.isPrimitive();
      }
    };
  }

  /** Matches an AST node if its type is either a primitive type or a {@code void} type. */
  public static <T extends Tree> Matcher<T> isPrimitiveOrVoidType() {
    return new Matcher<T>() {
      @Override
      public boolean matches(T t, VisitorState state) {
        Type type = getType(t);
        return type != null && type.isPrimitiveOrVoid();
      }
    };
  }

  /** Matches an AST node if its type is a {@code void} type. */
  public static <T extends Tree> Matcher<T> isVoidType() {
    return new Matcher<T>() {
      @Override
      public boolean matches(T t, VisitorState state) {
        Type type = getType(t);
        return type != null && state.getTypes().isSameType(type, state.getSymtab().voidType);
      }
    };
  }

  /**
   * Matches an AST node if its type is a primitive type, or a boxed version of a primitive type.
   */
  public static <T extends Tree> Matcher<T> isPrimitiveOrBoxedPrimitiveType() {
    return new Matcher<T>() {
      @Override
      public boolean matches(Tree t, VisitorState state) {
        Type type = getType(t);
        return type != null && state.getTypes().unboxedTypeOrType(type).isPrimitive();
      }
    };
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
  public static <T extends Tree> Matcher<Tree> enclosingNode(final Matcher<T> matcher) {
    return new Matcher<Tree>() {
      @SuppressWarnings("unchecked") // TODO(cushon): this should take a Class<T>
      @Override
      public boolean matches(Tree t, VisitorState state) {
        TreePath path = state.getPath().getParentPath();
        while (path != null) {
          Tree node = path.getLeaf();
          state = state.withPath(path);
          if (matcher.matches((T) node, state)) {
            return true;
          }
          path = path.getParentPath();
        }
        return false;
      }
    };
  }

  /**
   * Matches a statement AST node if the following statement in the enclosing block matches the
   * given matcher.
   */
  public static <T extends StatementTree> NextStatement<T> nextStatement(
      Matcher<StatementTree> matcher) {
    return new NextStatement<>(matcher);
  }

  /**
   * Matches a statement AST node if the previous statement in the enclosing block matches the given
   * matcher.
   */
  public static <T extends StatementTree> Matcher<T> previousStatement(
      Matcher<StatementTree> matcher) {
    return (T statement, VisitorState state) -> {
      BlockTree block = state.findEnclosing(BlockTree.class);
      if (block == null) {
        return false;
      }
      List<? extends StatementTree> statements = block.getStatements();
      int idx = statements.indexOf(statement);
      if (idx <= 0) {
        // The block wrapping us doesn't contain this statement, or doesn't contain a previous
        // statement.
        return false;
      }
      return matcher.matches(statements.get(idx - 1), state);
    };
  }

  /** Matches a statement AST node if the statement is the last statement in the block. */
  public static Matcher<StatementTree> isLastStatementInBlock() {
    return new IsLastStatementInBlock<>();
  }

  /** Matches an AST node if it is a literal other than null. */
  public static Matcher<ExpressionTree> nonNullLiteral() {
    return new Matcher<ExpressionTree>() {
      @Override
      public boolean matches(ExpressionTree tree, VisitorState state) {
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

  public static Matcher<ExpressionTree> booleanLiteral(final boolean value) {
    return new Matcher<ExpressionTree>() {
      // Matcher of a boolean literal
      @Override
      public boolean matches(ExpressionTree expressionTree, VisitorState state) {
        if (expressionTree.getKind() == Tree.Kind.BOOLEAN_LITERAL) {
          return value == (Boolean) (((LiteralTree) expressionTree).getValue());
        }
        return false;
      }
    };
  }

  /**
   * Matches the boolean constant ({@link Boolean#TRUE} or {@link Boolean#FALSE}) corresponding to
   * the given value.
   */
  public static Matcher<ExpressionTree> booleanConstant(final boolean value) {
    return new Matcher<ExpressionTree>() {
      @Override
      public boolean matches(ExpressionTree expressionTree, VisitorState state) {
        if (expressionTree instanceof JCFieldAccess) {
          Symbol symbol = ASTHelpers.getSymbol(expressionTree);
          if (symbol.isStatic()
              && state.getTypes().unboxedTypeOrType(symbol.type).getTag() == TypeTag.BOOLEAN) {
            return ((value && symbol.getSimpleName().contentEquals("TRUE"))
                || symbol.getSimpleName().contentEquals("FALSE"));
          }
        }
        return false;
      }
    };
  }

  /**
   * Ignores any number of parenthesis wrapping an expression and then applies the passed matcher to
   * that expression. For example, the passed matcher would be applied to {@code value} in {@code
   * (((value)))}.
   */
  public static Matcher<ExpressionTree> ignoreParens(final Matcher<ExpressionTree> innerMatcher) {
    return new Matcher<ExpressionTree>() {
      @Override
      public boolean matches(ExpressionTree expressionTree, VisitorState state) {
        return innerMatcher.matches((ExpressionTree) stripParentheses(expressionTree), state);
      }
    };
  }

  public static Matcher<ExpressionTree> intLiteral(final int value) {
    return new Matcher<ExpressionTree>() {
      @Override
      public boolean matches(ExpressionTree expressionTree, VisitorState state) {
        if (expressionTree.getKind() == Kind.INT_LITERAL) {
          return ((Integer) ((LiteralTree) expressionTree).getValue()).equals(value);
        }
        return false;
      }
    };
  }

  public static Matcher<ExpressionTree> classLiteral(
      final Matcher<? super ExpressionTree> classMatcher) {
    return new Matcher<ExpressionTree>() {
      @Override
      public boolean matches(ExpressionTree expressionTree, VisitorState state) {
        if (expressionTree.getKind() == Kind.MEMBER_SELECT) {
          MemberSelectTree select = (MemberSelectTree) expressionTree;
          return select.getIdentifier().contentEquals("class")
              && classMatcher.matches(select.getExpression(), state);
        }
        return false;
      }
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

  public static Matcher<AnnotationTree> isType(final String annotationClassName) {
    return new AnnotationType(annotationClassName);
  }

  /**
   * Matches a MethodInvocation AST node when the arguments at the two given indices are both the
   * same identifier.
   */
  public static Matcher<? super MethodInvocationTree> sameArgument(
      final int index1, final int index2) {
    return new Matcher<MethodInvocationTree>() {
      @Override
      public boolean matches(MethodInvocationTree methodInvocationTree, VisitorState state) {
        List<? extends ExpressionTree> args = methodInvocationTree.getArguments();
        return ASTHelpers.sameVariable(args.get(index1), args.get(index2));
      }
    };
  }

  /**
   * Determines whether an expression has an annotation of the given type. This includes annotations
   * inherited from superclasses due to @Inherited.
   *
   * @param annotationClass the binary class name of the annotation (e.g.
   *     "javax.annotation.Nullable", or "some.package.OuterClassName$InnerClassName")
   */
  public static <T extends Tree> Matcher<T> hasAnnotation(final String annotationClass) {
    return new Matcher<T>() {
      @Override
      public boolean matches(T tree, VisitorState state) {
        return ASTHelpers.hasAnnotation(ASTHelpers.getDeclaredSymbol(tree), annotationClass, state);
      }
    };
  }

  /**
   * Determines if an expression has an annotation referred to by the given mirror. Accounts for
   * binary names and annotations inherited due to @Inherited.
   *
   * @param annotationMirror mirror referring to the annotation type
   */
  public static Matcher<Tree> hasAnnotation(TypeMirror annotationMirror) {
    return new Matcher<Tree>() {
      @Override
      public boolean matches(Tree tree, VisitorState state) {
        JavacProcessingEnvironment javacEnv = JavacProcessingEnvironment.instance(state.context);
        TypeElement typeElem = (TypeElement) javacEnv.getTypeUtils().asElement(annotationMirror);
        String name = annotationMirror.toString();
        if (typeElem != null) {
          // Get the binary name if possible ($ to separate nested members). See b/36160747
          name = javacEnv.getElementUtils().getBinaryName(typeElem).toString();
        }
        return ASTHelpers.hasAnnotation(ASTHelpers.getDeclaredSymbol(tree), name, state);
      }
    };
  }

  /**
   * Determines whether an expression has an annotation with the given simple name. This does not
   * include annotations inherited from superclasses due to @Inherited.
   *
   * @param simpleName the simple name of the annotation (e.g. "Nullable")
   */
  public static <T extends Tree> Matcher<T> hasAnnotationWithSimpleName(final String simpleName) {
    return new Matcher<T>() {
      @Override
      public boolean matches(T tree, VisitorState state) {
        return ASTHelpers.hasDirectAnnotationWithSimpleName(
            ASTHelpers.getDeclaredSymbol(tree), simpleName);
      }
    };
  }

  /**
   * Determines whether an expression refers to a symbol that has an annotation of the given type.
   * This includes annotations inherited from superclasses due to @Inherited.
   *
   * @param annotationClass the binary class name of the annotation (e.g.
   *     "javax.annotation.Nullable", or "some.package.OuterClassName$InnerClassName")
   */
  public static <T extends Tree> Matcher<T> symbolHasAnnotation(final String annotationClass) {
    return new Matcher<T>() {
      @Override
      public boolean matches(T tree, VisitorState state) {
        return ASTHelpers.hasAnnotation(ASTHelpers.getSymbol(tree), annotationClass, state);
      }
    };
  }

  /**
   * Determines whether an expression has an annotation of the given class. This includes
   * annotations inherited from superclasses due to @Inherited.
   *
   * @param inputClass The class of the annotation to look for (e.g, Produces.class).
   */
  public static <T extends Tree> Matcher<T> hasAnnotation(
      final Class<? extends Annotation> inputClass) {
    return new Matcher<T>() {
      @Override
      public boolean matches(T tree, VisitorState state) {
        return ASTHelpers.hasAnnotation(ASTHelpers.getDeclaredSymbol(tree), inputClass, state);
      }
    };
  }

  /**
   * Determines whether an expression refers to a symbol that has an annotation of the given type.
   * This includes annotations inherited from superclasses due to @Inherited.
   *
   * @param inputClass The class of the annotation to look for (e.g, Produces.class).
   */
  public static <T extends Tree> Matcher<T> symbolHasAnnotation(
      final Class<? extends Annotation> inputClass) {
    return new Matcher<T>() {
      @Override
      public boolean matches(T tree, VisitorState state) {
        return ASTHelpers.hasAnnotation(ASTHelpers.getSymbol(tree), inputClass, state);
      }
    };
  }

  /**
   * Matches if a method or any method it overrides has an annotation of the given type. JUnit 4's
   * {@code @Test}, {@code @Before}, and {@code @After} annotations behave this way.
   *
   * @param annotationClass the binary class name of the annotation (e.g.
   *     "javax.annotation.Nullable", or "some.package.OuterClassName$InnerClassName")
   */
  public static Matcher<MethodTree> hasAnnotationOnAnyOverriddenMethod(
      final String annotationClass) {
    return new Matcher<MethodTree>() {
      @Override
      public boolean matches(MethodTree tree, VisitorState state) {
        MethodSymbol methodSym = ASTHelpers.getSymbol(tree);
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
      }
    };
  }

  /** Matches a whitelisted method invocation that is known to never return null */
  public static Matcher<ExpressionTree> methodReturnsNonNull() {
    return anyOf(
        instanceMethod().onDescendantOf("java.lang.Object").named("toString"),
        instanceMethod().onExactClass("java.lang.String"),
        staticMethod().onClass("java.lang.String"),
        instanceMethod().onExactClass("java.util.StringTokenizer").named("nextToken"));
  }

  public static Matcher<MethodTree> methodReturns(final Matcher<? super Tree> returnTypeMatcher) {
    return new Matcher<MethodTree>() {
      @Override
      public boolean matches(MethodTree methodTree, VisitorState state) {
        Tree returnTree = methodTree.getReturnType();
        if (returnTree == null) {
          // This is a constructor, it has no return type.
          return false;
        }
        return returnTypeMatcher.matches(returnTree, state);
      }
    };
  }

  public static Matcher<MethodTree> methodReturns(final Supplier<Type> returnType) {
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
  public static Matcher<MethodTree> methodIsNamed(final String methodName) {
    return new Matcher<MethodTree>() {
      @Override
      public boolean matches(MethodTree methodTree, VisitorState state) {
        return methodTree.getName().contentEquals(methodName);
      }
    };
  }

  /**
   * Match a method declaration that starts with a given string.
   *
   * @param prefix The prefix.
   */
  public static Matcher<MethodTree> methodNameStartsWith(final String prefix) {
    return new Matcher<MethodTree>() {
      @Override
      public boolean matches(MethodTree methodTree, VisitorState state) {
        return methodTree.getName().toString().startsWith(prefix);
      }
    };
  }

  /**
   * Match a method declaration with a specific enclosing class and method name.
   *
   * @param className The fully-qualified name of the enclosing class, e.g.
   *     "com.google.common.base.Preconditions"
   * @param methodName The name of the method to match, e.g., "checkNotNull"
   */
  public static Matcher<MethodTree> methodWithClassAndName(
      final String className, final String methodName) {
    return new Matcher<MethodTree>() {
      @Override
      public boolean matches(MethodTree methodTree, VisitorState state) {
        return ASTHelpers.getSymbol(methodTree)
                .getEnclosingElement()
                .getQualifiedName()
                .contentEquals(className)
            && methodTree.getName().contentEquals(methodName);
      }
    };
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
    return new Matcher<MethodTree>() {
      @Override
      public boolean matches(MethodTree methodTree, VisitorState state) {
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
      }
    };
  }

  /** Matches if the given matcher matches all of/any of the parameters to this method. */
  public static MultiMatcher<MethodTree, VariableTree> methodHasParameters(
      MatchType matchType, Matcher<VariableTree> parameterMatcher) {
    return new MethodHasParameters(matchType, parameterMatcher);
  }

  public static Matcher<MethodTree> methodHasVisibility(final Visibility visibility) {
    return new MethodVisibility(visibility);
  }

  public static Matcher<MethodTree> methodIsConstructor() {
    return new Matcher<MethodTree>() {
      @Override
      public boolean matches(MethodTree methodTree, VisitorState state) {
        return ASTHelpers.getSymbol(methodTree).isConstructor();
      }
    };
  }

  /**
   * Matches a constructor declaration in a specific enclosing class.
   *
   * @param className The fully-qualified name of the enclosing class, e.g.
   *     "com.google.common.base.Preconditions"
   */
  public static Matcher<MethodTree> constructorOfClass(final String className) {
    return new Matcher<MethodTree>() {
      @Override
      public boolean matches(MethodTree methodTree, VisitorState state) {
        Symbol symbol = ASTHelpers.getSymbol(methodTree);
        return symbol.getEnclosingElement().getQualifiedName().contentEquals(className)
            && symbol.isConstructor();
      }
    };
  }

  /**
   * Matches a class in which at least one method matches the given methodMatcher.
   *
   * @param methodMatcher A matcher on MethodTrees to run against all methods in this class.
   * @return True if some method in the class matches the given methodMatcher.
   */
  public static Matcher<ClassTree> hasMethod(final Matcher<MethodTree> methodMatcher) {
    return new Matcher<ClassTree>() {
      @Override
      public boolean matches(ClassTree t, VisitorState state) {
        for (Tree member : t.getMembers()) {
          if (member instanceof MethodTree) {
            if (methodMatcher.matches((MethodTree) member, state)) {
              return true;
            }
          }
        }
        return false;
      }
    };
  }

  /**
   * Matches on the type of a VariableTree AST node.
   *
   * @param treeMatcher A matcher on the type of the variable.
   */
  public static Matcher<VariableTree> variableType(final Matcher<Tree> treeMatcher) {
    return new Matcher<VariableTree>() {
      @Override
      public boolean matches(VariableTree variableTree, VisitorState state) {
        return treeMatcher.matches(variableTree.getType(), state);
      }
    };
  }

  /**
   * Matches on the initializer of a VariableTree AST node.
   *
   * @param expressionTreeMatcher A matcher on the initializer of the variable.
   */
  public static Matcher<VariableTree> variableInitializer(
      final Matcher<ExpressionTree> expressionTreeMatcher) {
    return new Matcher<VariableTree>() {
      @Override
      public boolean matches(VariableTree variableTree, VisitorState state) {
        ExpressionTree initializer = variableTree.getInitializer();
        return initializer == null ? false : expressionTreeMatcher.matches(initializer, state);
      }
    };
  }

  /**
   * Matches if a {@link VariableTree} is a field declaration, as opposed to a local variable, enum
   * constant, parameter to a method, etc.
   */
  public static Matcher<VariableTree> isField() {
    return new Matcher<VariableTree>() {
      @Override
      public boolean matches(VariableTree variableTree, VisitorState state) {
        Element element = ASTHelpers.getSymbol(variableTree);
        return element.getKind() == ElementKind.FIELD;
      }
    };
  }

  /**
   * Matches an class based on whether it is nested in another class or method.
   *
   * @param kind The kind of nesting to match, eg ANONYMOUS, LOCAL, MEMBER, TOP_LEVEL
   */
  public static Matcher<ClassTree> nestingKind(final NestingKind kind) {
    return new Matcher<ClassTree>() {
      @Override
      public boolean matches(ClassTree classTree, VisitorState state) {
        ClassSymbol sym = ASTHelpers.getSymbol(classTree);
        return sym.getNestingKind() == kind;
      }
    };
  }

  /**
   * Matches an instance method that is a descendant of the instance method specified by the class
   * name and method name.
   *
   * @param fullClassName The name of the class whose instance method to match, e.g.,
   *     "java.util.Map"
   * @param methodName The name of the method to match, including arguments, e.g.,
   *     "get(java.lang.Object)"
   * @deprecated prefer {@link MethodMatchers#instanceMethod}
   */
  @Deprecated
  // TODO(cushon): expunge
  public static Matcher<ExpressionTree> isDescendantOfMethod(
      String fullClassName, String methodName) {
    return new DescendantOf(fullClassName, methodName);
  }

  /**
   * Matches a binary tree if the given matchers match the operands in either order. That is,
   * returns true if either: matcher1 matches the left operand and matcher2 matches the right
   * operand or matcher2 matches the left operand and matcher1 matches the right operand
   */
  public static Matcher<BinaryTree> binaryTree(
      final Matcher<ExpressionTree> matcher1, final Matcher<ExpressionTree> matcher2) {
    return new Matcher<BinaryTree>() {
      @Override
      public boolean matches(BinaryTree t, VisitorState state) {
        return null != ASTHelpers.matchBinaryTree(t, Arrays.asList(matcher1, matcher2), state);
      }
    };
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
  public static <T extends Tree> Matcher<T> hasModifier(final Modifier modifier) {
    return new Matcher<T>() {
      @Override
      public boolean matches(T tree, VisitorState state) {
        Symbol sym = ASTHelpers.getSymbol(tree);
        return sym != null && sym.getModifiers().contains(modifier);
      }
    };
  }

  /** Matches an AST node which is an expression yielding the indicated static field access. */
  public static Matcher<ExpressionTree> staticFieldAccess() {
    return allOf(isStatic(), isSymbol(VarSymbol.class));
  }

  /** Matches an AST node that is static. */
  public static <T extends Tree> Matcher<T> isStatic() {
    return new Matcher<T>() {
      @Override
      public boolean matches(Tree tree, VisitorState state) {
        Symbol sym = ASTHelpers.getSymbol(tree);
        return sym != null && sym.isStatic();
      }
    };
  }

  /** Matches an AST node that is transient. */
  public static <T extends Tree> Matcher<T> isTransient() {
    return new Matcher<T>() {
      @Override
      public boolean matches(Tree tree, VisitorState state) {
        Symbol sym = ASTHelpers.getSymbol(tree);
        // NOTE(yorick): there is no isTransient() on Symbol. This is what it would be.
        return sym.getModifiers().contains(Modifier.TRANSIENT);
      }
    };
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
    return new Matcher<StatementTree>() {
      @Override
      public boolean matches(StatementTree statementTree, VisitorState state) {
        return statementTree instanceof ContinueTree;
      }
    };
  }

  /** Matches an {@link ExpressionStatementTree} based on its {@link ExpressionTree}. */
  public static Matcher<StatementTree> expressionStatement(final Matcher<ExpressionTree> matcher) {
    return new Matcher<StatementTree>() {
      @Override
      public boolean matches(StatementTree statementTree, VisitorState state) {
        return statementTree instanceof ExpressionStatementTree
            && matcher.matches(((ExpressionStatementTree) statementTree).getExpression(), state);
      }
    };
  }

  static Matcher<Tree> isSymbol(java.lang.Class<? extends Symbol> symbolClass) {
    return new IsSymbol(symbolClass);
  }

  /**
   * Converts the given matcher to one that can be applied to any tree but is only executed when run
   * on a tree of {@code type} and returns {@code false} for all other tree types.
   */
  public static <S extends Tree, T extends Tree> Matcher<T> toType(
      final Class<S> type, final Matcher<? super S> matcher) {
    return new Matcher<T>() {
      @Override
      public boolean matches(T tree, VisitorState state) {
        return type.isInstance(tree) && matcher.matches(type.cast(tree), state);
      }
    };
  }

  /** Matches if this Tree is enclosed by either a synchronized block or a synchronized method. */
  public static final <T extends Tree> Matcher<T> inSynchronized() {
    return new Matcher<T>() {
      @Override
      public boolean matches(T tree, VisitorState state) {
        SynchronizedTree synchronizedTree =
            ASTHelpers.findEnclosingNode(state.getPath(), SynchronizedTree.class);
        if (synchronizedTree != null) {
          return true;
        }

        MethodTree methodTree = ASTHelpers.findEnclosingNode(state.getPath(), MethodTree.class);
        return methodTree != null
            && methodTree.getModifiers().getFlags().contains(Modifier.SYNCHRONIZED);
      }
    };
  }

  /**
   * Matches if this ExpressionTree refers to the same variable as the one passed into the matcher.
   */
  public static Matcher<ExpressionTree> sameVariable(final ExpressionTree expr) {
    return new Matcher<ExpressionTree>() {
      @Override
      public boolean matches(ExpressionTree tree, VisitorState state) {
        return ASTHelpers.sameVariable(tree, expr);
      }
    };
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
      final Matcher<VariableTree> variableMatcher,
      final Matcher<ExpressionTree> expressionMatcher,
      final Matcher<StatementTree> statementMatcher) {
    return new Matcher<EnhancedForLoopTree>() {
      @Override
      public boolean matches(EnhancedForLoopTree t, VisitorState state) {
        return variableMatcher.matches(t.getVariable(), state)
            && expressionMatcher.matches(t.getExpression(), state)
            && statementMatcher.matches(t.getStatement(), state);
      }
    };
  }

  /** Matches if the given tree is inside a loop. */
  public static <T extends Tree> Matcher<T> inLoop() {
    return new Matcher<T>() {
      @Override
      public boolean matches(Tree tree, VisitorState state) {
        TreePath path = state.getPath().getParentPath();
        Tree node = path.getLeaf();
        while (path != null) {
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
              path = path.getParentPath();
              node = path.getLeaf();
              break;
          }
        }
        return false;
      }
    };
  }

  /**
   * Matches an assignment operator AST node if both of the given matchers match.
   *
   * @param variableMatcher The matcher to apply to the variable.
   * @param expressionMatcher The matcher to apply to the expression.
   */
  public static Matcher<AssignmentTree> assignment(
      final Matcher<ExpressionTree> variableMatcher,
      final Matcher<? super ExpressionTree> expressionMatcher) {
    return new Matcher<AssignmentTree>() {
      @Override
      public boolean matches(AssignmentTree t, VisitorState state) {
        return variableMatcher.matches(t.getVariable(), state)
            && expressionMatcher.matches(t.getExpression(), state);
      }
    };
  }

  /**
   * Matches a type cast AST node if both of the given matchers match.
   *
   * @param typeMatcher The matcher to apply to the type.
   * @param expressionMatcher The matcher to apply to the expression.
   */
  public static Matcher<TypeCastTree> typeCast(
      final Matcher<Tree> typeMatcher, final Matcher<ExpressionTree> expressionMatcher) {
    return new Matcher<TypeCastTree>() {
      @Override
      public boolean matches(TypeCastTree t, VisitorState state) {
        return typeMatcher.matches(t.getType(), state)
            && expressionMatcher.matches(t.getExpression(), state);
      }
    };
  }

  /**
   * Matches an assertion AST node if the given matcher matches its condition.
   *
   * @param conditionMatcher The matcher to apply to the condition in the assertion, e.g. in "assert
   *     false", the "false" part of the statement
   */
  public static Matcher<AssertTree> assertionWithCondition(
      final Matcher<ExpressionTree> conditionMatcher) {
    return new Matcher<AssertTree>() {
      @Override
      public boolean matches(AssertTree tree, VisitorState state) {
        return conditionMatcher.matches(tree.getCondition(), state);
      }
    };
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
  public static Matcher<MethodTree> methodHasArity(final int arity) {
    return new Matcher<MethodTree>() {
      @Override
      public boolean matches(MethodTree methodTree, VisitorState state) {
        return methodTree.getParameters().size() == arity;
      }
    };
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

  private static final Matcher<MethodInvocationTree> STATIC_EQUALS =
      anyOf(
          allOf(
              staticMethod()
                  .onClass("android.support.v4.util.ObjectsCompat")
                  .named("equals")
                  .withParameters("java.lang.Object", "java.lang.Object"),
              isSameType(BOOLEAN_TYPE)),
          allOf(
              staticMethod()
                  .onClass("java.util.Objects")
                  .named("equals")
                  .withParameters("java.lang.Object", "java.lang.Object"),
              isSameType(BOOLEAN_TYPE)),
          allOf(
              staticMethod()
                  .onClass("com.google.common.base.Objects")
                  .named("equal")
                  .withParameters("java.lang.Object", "java.lang.Object"),
              isSameType(BOOLEAN_TYPE)));

  /**
   * Matches an invocation of a recognized static object equality method such as {@link
   * java.util.Objects#equals}. These are simple facades to {@link Object#equals} that accept null
   * for either argument.
   */
  public static Matcher<MethodInvocationTree> staticEqualsInvocation() {
    return STATIC_EQUALS;
  }

  private static final Matcher<ExpressionTree> INSTANCE_EQUALS =
      allOf(
          instanceMethod().anyClass().named("equals").withParameters("java.lang.Object"),
          isSameType(BOOLEAN_TYPE));

  /** Matches calls to the method {@link Object#equals(Object)} or any override of that method. */
  public static Matcher<ExpressionTree> instanceEqualsInvocation() {
    return INSTANCE_EQUALS;
  }

  private static final Matcher<ExpressionTree> ASSERT_EQUALS =
      anyOf(
          staticMethod().onClass("org.junit.Assert").named("assertEquals"),
          staticMethod().onClass("junit.framework.Assert").named("assertEquals"),
          staticMethod().onClass("junit.framework.TestCase").named("assertEquals"));

  /**
   * Matches calls to the method {@code org.junit.Assert#assertEquals} and corresponding methods in
   * JUnit 3.x.
   */
  public static Matcher<ExpressionTree> assertEqualsInvocation() {
    return ASSERT_EQUALS;
  }

  private static final Matcher<ExpressionTree> ASSERT_NOT_EQUALS =
      anyOf(
          staticMethod().onClass("org.junit.Assert").named("assertNotEquals"),
          staticMethod().onClass("junit.framework.Assert").named("assertNotEquals"),
          staticMethod().onClass("junit.framework.TestCase").named("assertNotEquals"));

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
}
