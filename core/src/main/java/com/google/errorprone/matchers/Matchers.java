/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.MethodVisibility.Visibility;
import com.google.errorprone.matchers.MultiMatcher.MatchType;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
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
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCNewClass;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;

/**
 * Static factory methods which make the DSL read more fluently.
 * Since matchers are run in a tight loop during compilation, performance is important. When assembling a matcher
 * from the DSL, it's best to construct it only once, by saving the resulting matcher as a static variable for example.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
public class Matchers {
  private Matchers() {}

  /**
   * A matcher that matches any AST node.
   */
  public static <T extends Tree> Matcher<T> anything() {
    return new Matcher<T>() {
      @Override
      public boolean matches(T t, VisitorState state) {
        return true;
      }
    };
  }

  /**
   * A matcher that matches no AST node.
   */
  public static <T extends Tree> Matcher<T> nothing() {
    return new Matcher<T>() {
      @Override
      public boolean matches(T t, VisitorState state) {
        return false;
      }
    };
  }

  /**
   * Matches an AST node iff it does not match the given matcher.
   */
  public static <T extends Tree> Matcher<T> not(final Matcher<T> matcher) {
    return new Matcher<T>() {
      @Override
      public boolean matches(T t, VisitorState state) {
        return !matcher.matches(t, state);
      }
    };
  }

  /**
   * Compose several matchers together, such that the composite matches an AST node iff all the given matchers do.
   */
  @SafeVarargs
  public static <T extends Tree> Matcher<T> allOf(final Matcher<? super T>... matchers) {
    return new Matcher<T>() {
      @Override public boolean matches(T t, VisitorState state) {
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
   * Compose several matchers together, such that the composite matches an AST node if any of the given matchers do.
   */
  @SafeVarargs
  public static <T extends Tree> Matcher<T> anyOf(final Matcher<? super T>... matchers) {
    return new Matcher<T>() {
      @Override public boolean matches(T t, VisitorState state) {
        for (Matcher<? super T> matcher : matchers) {
          if (matcher.matches(t, state)) {
            return true;
          }
        }
        return false;
      }
    };
  }

  /**
   * Matches if an AST node is an instance of the given class.
   */
  public static <T extends Tree> Matcher<T> isInstance(final java.lang.Class<?> klass) {
    return new Matcher<T>() {
      @Override
      public boolean matches(T t, VisitorState state) {
        return klass.isInstance(t);
      }
    };
  }

  /**
   * Matches an AST node of a given kind, for example, an Annotation or a switch block.
   */
  public static <T extends Tree> Matcher<T> kindIs(final Kind kind) {
    return new Matcher<T>() {
      @Override public boolean matches(T tree, VisitorState state) {
        return tree.getKind() == kind;
      }
    };
  }

  /**
   * Matches an AST node which is the same object reference as the given node.
   */
   public static <T extends Tree> Matcher<T> isSame(final Tree t) {
    return new Matcher<T>() {
      @Override public boolean matches(T tree, VisitorState state) {
        return tree == t;
      }
    };
  }

  /**
   * Matches an AST node which is an expression yielding the indicated static method.
   * You can use "*" wildcard instead of any of the arguments.
   * @param fullClassName fully-qualified name like "java.util.regex.Pattern"
   * @param methodName either name or full signature of the static method which is a member of the
   * class, like "compile" or "compile(java.lang.String)"
   */
  public static StaticMethod staticMethod(String fullClassName, String methodName) {
    return new StaticMethod(fullClassName, methodName);
  }

  /**
   * Matches an AST node which is an expression yielding the indicated non-static method.
   * @param receiverMatcher Used to determine if the part of the expression before the dot matches.
   * @param methodName The name of the method to match, e.g., "equals"
   */
  public static InstanceMethod instanceMethod(Matcher<? super ExpressionTree> receiverMatcher, String methodName) {
    return new InstanceMethod(receiverMatcher, methodName);
  }

  /**
   * Matches an AST node which is an expression yielding the indicated non-static method.
   * @param receiverMatcher Used to determine if the part of the expression before the dot matches.
   */
  public static InstanceMethod methodReceiver(Matcher<? super ExpressionTree> receiverMatcher) {
    return InstanceMethod.methodReceiverMatcher(receiverMatcher);
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
   * Matches when the receiver of an instance method is the same reference as a particular argument to the method.
   * For example, receiverSameAsArgument(1) would match {@code obj.method("", obj)}
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

  /**
   * Matches if the given annotation matcher matches all of or any of the annotations on this tree
   * node.
   *
   * @param matchType Whether to match if the matchers match any of or all of the annotations on
   * this tree.
   * @param annotationMatcher The annotation matcher to use.
   */
  public static <T extends Tree> MultiMatcher<T, AnnotationTree> annotations(MatchType matchType,
      Matcher<AnnotationTree> annotationMatcher) {
    return new Annotation<>(matchType, annotationMatcher);
  }

  /**
   * Matches a constructor with the given class name and parameter types.
   */
  public static Constructor constructor(String className, List<String> parameterTypes) {
    return new Constructor(className, parameterTypes);
  }

  /**
   * Matches a class in which any of/all of its constructors match the given constructorMatcher.
   */
  public static MultiMatcher<ClassTree, MethodTree> constructor(MatchType matchType,
      Matcher<MethodTree> constructorMatcher) {
    return new ConstructorOfClass(matchType, constructorMatcher);
  }

  public static Matcher<MethodInvocationTree> methodSelect(Matcher<ExpressionTree> methodSelectMatcher) {
    return new MethodInvocationMethodSelect(methodSelectMatcher);
  }

  public static Matcher<ExpressionTree> expressionMethodSelect(Matcher<ExpressionTree> methodSelectMatcher) {
    return new ExpressionMethodSelect(methodSelectMatcher);
  }

  public static Matcher<MethodInvocationTree> argument(
      final int position, final Matcher<ExpressionTree> argumentMatcher) {
    return new MethodInvocationArgument(position, argumentMatcher);
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
   * Matches an AST node if its parent node is matched by the given matcher.
   * For example, {@code parentNode(kindIs(Kind.RETURN))}
   * would match the {@code this} expression in {@code return this;}
   */
  public static Matcher<Tree> parentNode(Matcher<? extends Tree> treeMatcher) {
    @SuppressWarnings("unchecked")  // Safe contravariant cast
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
  public static <T extends Tree> Matcher<T> isSubtypeOf(Type type) {
    return new IsSubtypeOf<>(type);
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
   * Matches an AST node if its type is castable to the given type.
   *
   * @param typeString a string representation of the type, e.g., "java.util.Set"
   */
  public static <T extends Tree> Matcher<T> isCastableTo(String typeString) {
    return new IsCastableTo<>(typeString);
  }

  /**
   * Matches an AST node if its type is castable to the given type.
   *
   * @param typeSupplier a supplier of the type to check against
   */
  public static <T extends Tree> Matcher<T> isCastableTo(Supplier<Type> typeSupplier) {
    return new IsCastableTo<>(typeSupplier);
  }

  /**
   * Matches an AST node if its type is the same as the given type.
   *
   * @param type the type to check against
   */
  public static <T extends Tree> Matcher<T> isSameType(Type type) {
    return new IsSameType<>(type);
  }

  /**
   * Matches an AST node if its type is the same as the given type.
   *
   * @param type the type to check against
   */
  public static <T extends Tree> Matcher<T> isSameType(Supplier<Type> type) {
    return new IsSameType<>(type);
  }

  /**
   * Matches an AST node if its type is the same as the given type.
   *
   * @param typeString the type to check against
   */
  public static <T extends Tree> Matcher<T> isSameType(String typeString) {
    return new IsSameType<>(typeString);
  }

  /**
   * Matches an AST node if its type is the same as the type of the given tree.
   *
   * @param tree an AST node whose type to check against
   */
  public static <T extends Tree> Matcher<T> isSameType(Tree tree) {
    return new IsSameType<>(tree);
  }

  /**
   * Matches an AST node if its type is an array type.
   */
  public static <T extends Tree> Matcher<T> isArrayType() {
    return new Matcher<T>() {
      @Override public boolean matches(Tree t, VisitorState state) {
        return state.getTypes().isArray(((JCTree) t).type);
      }
    };
  }

  /**
   * Matches an AST node if its type is a primitive array type.
   */
  public static <T extends Tree> Matcher<T> isPrimitiveArrayType() {
    return new Matcher<T>() {
      @Override public boolean matches(Tree t, VisitorState state) {
        Type type = ((JCTree) t).type;
        return state.getTypes().isArray(type) && state.getTypes().elemtype(type).isPrimitive();
      }
    };
  }

  /**
   * Matches an AST node if its type is a primitive type.
   */
  public static <T extends Tree> Matcher<T> isPrimitiveType() {
    return new Matcher<T>() {
      @Override public boolean matches(Tree t, VisitorState state) {
        return ((JCTree) t).type.isPrimitive();
      }
    };
  }

  /**
   * Matches an AST node which is enclosed by a block node that matches the given matcher.
   */
  public static <T extends Tree> Enclosing.Block<T> enclosingBlock(Matcher<BlockTree> matcher) {
    return new Enclosing.Block<>(matcher);
  }

  /**
   * Matches an AST node which is enclosed by a class node that matches the given matcher.
   */
  public static <T extends Tree> Enclosing.Class<T> enclosingClass(Matcher<ClassTree> matcher) {
    return new Enclosing.Class<>(matcher);
  }

  /**
   * Matches an AST node which is enclosed by a method node that matches the given matcher.
   */
  public static <T extends Tree> Enclosing.Method<T> enclosingMethod(Matcher<MethodTree> matcher) {
    return new Enclosing.Method<>(matcher);
  }

  /**
   * Matches an AST node that is enclosed by some node that matches the given matcher.
   *
   * TODO(user): This could be used instead of enclosingBlock and enclosingClass.
   */
  public static <T extends Tree> Matcher<Tree> enclosingNode(final Matcher<T> matcher) {
    return new Matcher<Tree>() {
      @SuppressWarnings("unchecked")  // TODO(user): this should take a Class<T>
      @Override
      public boolean matches(Tree t, VisitorState state) {
        TreePath path = state.getPath().getParentPath();
        while (path != null) {
          Tree node = path.getLeaf();
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
   * Matches a block AST node if the last statement in the block matches the given matcher.
   */
  public static Matcher<List<StatementTree>> lastStatement(Matcher<StatementTree> matcher) {
    return new LastStatement(matcher);
  }

  /**
   * Matches a statement AST node if the following statement in the enclosing block matches the given matcher.
   */
  public static <T extends StatementTree> NextStatement<T> nextStatement(
      Matcher<StatementTree> matcher) {
    return new NextStatement<>(matcher);
  }

  /**
   * Matches an AST node if it is a literal other than null.
   */
  public static Matcher<ExpressionTree> nonNullLiteral() {
    return new Matcher<ExpressionTree>() {
      @Override
      public boolean matches(ExpressionTree tree, VisitorState state) {
        switch (tree.getKind()) {
          case MEMBER_SELECT:
            return ((MemberSelectTree) tree).getIdentifier().toString().equals("class");
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
   * Matches a Literal AST node if it is a string literal with the given value.
   * For example, {@code stringLiteral("thing")} matches the literal {@code "thing"}
   */
  public static Matcher<ExpressionTree> stringLiteral(String value) {
    return new StringLiteral(value);
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

  /**
   * Matches an Annotation AST node if the argument to the annotation with the given name has a value which
   * matches the given matcher.
   * For example, {@code hasArgumentWithValue("value", stringLiteral("one"))} matches {@code @Thing("one")}
   * or {@code @Thing({"one", "two"})} or {@code @Thing(value = "one")}
   */
  public static Matcher<AnnotationTree> hasArgumentWithValue(
      String argumentName, Matcher<ExpressionTree> valueMatcher) {
    return new AnnotationHasArgumentWithValue(argumentName, valueMatcher);
  }

  public static Matcher<AnnotationTree> isType(final String annotationClassName) {
    return new AnnotationType(annotationClassName);
  }

  /**
   * Matches a MethodInvocation AST node when the arguments at the two given indices are both the same identifier.
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
   * Determines whether an expression has an annotation of the given type.
   * This includes annotations inherited from superclasses due to @Inherited.
   *
   * @param annotationType The type of the annotation to look for (e.g, "javax.annotation.Nullable")
   */
  public static <T extends Tree> Matcher<T> hasAnnotation(final String annotationType) {
    return new Matcher<T>() {
      @Override
      public boolean matches (T tree, VisitorState state) {
        Symbol sym = ASTHelpers.getSymbol(tree);
        Symbol annotationSym = state.getSymbolFromString(annotationType);
        Symbol inheritedSym = state.getSymtab().inheritedType.tsym;

        if ((sym == null) || (annotationSym == null)) {
          return false;
        }
        if ((sym instanceof ClassSymbol) && (annotationSym.attribute(inheritedSym) != null)) {
          while (sym != null) {
            if (sym.attribute(annotationSym) != null) {
              return true;
            }
            sym = ((ClassSymbol) sym).getSuperclass().tsym;
          }
          return false;
        } else {
          return sym.attribute(annotationSym) != null;
        }
      }
    };
  }

  /**
   * Matches if a method or any method it overrides has an annotation of the given type.
   * JUnit 4's {@code @Test}, {@code @Before}, and {@code @After} annotations behave this way.
   *
   * @param annotationType The type of the annotation to look for (e.g, "org.junit.Test")
   */
  public static Matcher<MethodTree> hasAnnotationOnAnyOverriddenMethod(
      final String annotationType) {
    return new Matcher<MethodTree>() {
      @Override
      public boolean matches(MethodTree tree, VisitorState state) {
        MethodSymbol methodSym = ASTHelpers.getSymbol(tree);
        Symbol annotationSym = state.getSymbolFromString(annotationType);
        if ((methodSym == null) || (annotationSym == null)) {
          return false;
        }

        Set<MethodSymbol> allMethods = ASTHelpers.findSuperMethods(methodSym, state.getTypes());
        allMethods.add(methodSym);

        for (MethodSymbol method : allMethods) {
          if (method.attribute(annotationSym) != null) {
            return true;
          }
        }

        return false;
      }
    };
  }

  /**
   * Matches a whitelisted method invocation that is known to never return null
   */
  public static Matcher<ExpressionTree> methodReturnsNonNull() {
    return anyOf(
        expressionMethodSelect((isDescendantOfMethod("java.lang.Object", "toString()"))),
        expressionMethodSelect(methodReceiver(isSameType("java.lang.String"))),
        expressionMethodSelect(staticMethod("java.lang.String", "*")),
        expressionMethodSelect(instanceMethod(isSameType("java.util.StringTokenizer"), "nextToken"))
    );
  }

  public static Matcher<MethodTree> methodReturns(final Type returnType) {
    return new Matcher<MethodTree>() {
      @Override
      public boolean matches(MethodTree methodTree, VisitorState state) {
        Tree returnTree = methodTree.getReturnType();
        if (returnTree == null) {
          // This is a constructor, it has no return type.
          return false;
        }
        Type methodReturnType = ASTHelpers.getType(returnTree);
        if (methodReturnType == null) {
          return false;
        }
        return state.getTypes().isSameType(methodReturnType, returnType);
      }
    };
  }

  public static Matcher<MethodTree> methodReturns(final Supplier<Type> returnType) {
    return new Matcher<MethodTree>() {
      @Override
      public boolean matches(MethodTree methodTree, VisitorState state) {
        return methodReturns(returnType.get(state)).matches(methodTree, state);
      }
    };
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
        return methodTree.getName().toString().equals(methodName);
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
   * Matches an AST node that represents a method declaration, based on the list of
   * variableMatchers.  Applies the variableMatcher at index n to the parameter at index n
   * and returns true iff they all match.  Returns false if the number of variableMatchers provided
   * does not match the number of parameters.
   *
   * <p>If you pass no variableMatchers, this will match methods with no parameters.
   *
   * @param variableMatcher an array of matchers to apply to the parameters of the method
   */
  @SafeVarargs
  public static Matcher<MethodTree> methodHasParameters(final Matcher<VariableTree>... variableMatcher) {
    return new Matcher<MethodTree>() {
      @Override
      public boolean matches(MethodTree methodTree, VisitorState state) {
        if (methodTree.getParameters().size() != variableMatcher.length) {
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

  /**
   * Matches if the given matcher matches all of/any of the parameters to this method.
   */
  public static MultiMatcher<MethodTree, VariableTree> methodHasParameters(MatchType matchType,
      Matcher<VariableTree> parameterMatcher) {
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
            if (methodMatcher.matches((MethodTree)member, state)) {
              return true;
            }
          }
        }
        return false;
      }
    };
  }

  public static Matcher<VariableTree> variableType(final Matcher<Tree> treeMatcher) {
    return new Matcher<VariableTree>() {
      @Override
      public boolean matches(VariableTree variableTree, VisitorState state) {
        return treeMatcher.matches(variableTree.getType(), state);
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
   * Matches an instance method that is a descendant of the instance method specified by the
   * class name and method name.
   *
   * @param fullClassName The name of the class whose instance method to match, e.g.,
   * "java.util.Map"
   * @param methodName The name of the method to match, including arguments, e.g.,
   * "get(java.lang.Object)"
   */
  public static Matcher<ExpressionTree> isDescendantOfMethod(String fullClassName, String methodName) {
    return new DescendantOf(fullClassName, methodName);
  }

  /**
   * Matches a binary tree if the given matchers match the operands in either order.  That is,
   * returns true if either:
   *   matcher1 matches the left operand and matcher2 matches the right operand
   * or
   *   matcher2 matches the left operand and matcher1 matches the right operand
   */
  public static Matcher<BinaryTree> binaryTree(final Matcher<ExpressionTree> matcher1,
      final Matcher<ExpressionTree> matcher2) {
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
   * @param matchType Whether to match if the matchers match any of or all of the identifiers on
   * this tree.
   * @param nodeMatcher Which identifiers to look for
   */
  public static MultiMatcher<Tree, IdentifierTree> hasIdentifier(MatchType matchType,
      Matcher<IdentifierTree> nodeMatcher) {
    return new HasIdentifier(matchType, nodeMatcher);
  }

  /**
   * Returns true if the expression is a member access on an instance, rather than a static type.
   * Supports member method invocations and field accesses.
   */
  public static Matcher<ExpressionTree> selectedIsInstance() {
    return new Matcher<ExpressionTree>() {
      @Override
      public boolean matches(ExpressionTree expr, VisitorState state) {
        if (!(expr instanceof JCFieldAccess)) {
          // TODO(user): throw IllegalArgumentException?
          return false;
        }
        JCExpression selected = ((JCFieldAccess) expr).getExpression();
        if (selected instanceof JCNewClass) {
          return true;
        }
        Symbol sym = ASTHelpers.getSymbol(selected);
        return sym instanceof VarSymbol;
      }
    };
  }

  /**
   * Returns true if the Tree node has the expected {@code Modifier}.
   */
  public static <T extends Tree> Matcher<T> hasModifier(final Modifier modifier) {
    return new Matcher<T>() {
      @Override
      public boolean matches(T tree, VisitorState state) {
        Symbol sym = ASTHelpers.getSymbol(tree);
        return sym != null && sym.getModifiers().contains(modifier);
      }
    };
  }

  /**
   * Matches an AST node which is an expression yielding the indicated static field access.
   */
  public static Matcher<ExpressionTree> staticFieldAccess() {
    return allOf(isStatic(), isSymbol(VarSymbol.class));
  }

  public static Matcher<Tree> isStatic() {
    return new IsStatic();
  }

  static Matcher<Tree> isSymbol(java.lang.Class<? extends Symbol> symbolClass) {
    return new IsSymbol(symbolClass);
  }

  /**
   * Safely adapts a matcher on a subtype of Tree into a matcher on Tree.  Fails if the tree
   * node passed in is not an instance of the subtype, or the if the matcher does not match.
   *
   * @param returnTypeParam Type parameter of the Matcher that will be returned
   * @param matcherTypeParam Type parameter of the Matcher passed in
   * @param matcher The matcher to apply to the tree node
   */
  public static <S extends Tree, T extends S> Matcher<S> adaptMatcherType(
      final Class<S> returnTypeParam, final Class<T> matcherTypeParam, final Matcher<T> matcher) {
    return new Matcher<S>() {
      @Override
      public boolean matches(S tree, VisitorState state) {
        if (matcherTypeParam.isInstance(tree)) {
          return matcher.matches(matcherTypeParam.cast(tree), state);
        }
        return false;
      }
    };
  }

  /**
   * Matches if this Tree is enclosed by either a synchronized block or a synchronized method.
   */
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
   * Matches if this ExpressionTree refers to the same variable as the one passed into the
   * matcher.
   */
  public static Matcher<ExpressionTree> sameVariable(final ExpressionTree expr) {
    return new Matcher<ExpressionTree>() {
      @Override
      public boolean matches(ExpressionTree tree, VisitorState state) {
        return ASTHelpers.sameVariable(tree, expr);
      }
    };
  }
}
