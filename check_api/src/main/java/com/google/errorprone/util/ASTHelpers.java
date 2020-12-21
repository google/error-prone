/*
 * Copyright 2012 The Error Prone Authors.
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
import static com.google.common.base.Verify.verify;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.matchers.JUnitMatchers.JUNIT4_RUN_WITH_ANNOTATION;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.sun.tools.javac.code.Scope.LookupKind.NON_RECURSIVE;
import static java.util.Objects.requireNonNull;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Streams;
import com.google.errorprone.VisitorState;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnalysis;
import com.google.errorprone.matchers.JUnitMatchers;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.TestNgMatchers;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.suppliers.Suppliers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Attribute.Compound;
import com.sun.tools.javac.code.Attribute.TypeCompound;
import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.TypeVar;
import com.sun.tools.javac.code.Type.UnionClassType;
import com.sun.tools.javac.code.Type.WildcardType;
import com.sun.tools.javac.code.TypeAnnotations;
import com.sun.tools.javac.code.TypeAnnotations.AnnotationType;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotatedType;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMemberReference;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCPackageDecl;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.FatalError;
import com.sun.tools.javac.util.Filter;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Log.DeferredDiagnosticHandler;
import com.sun.tools.javac.util.Name;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;

/** This class contains utility methods to work with the javac AST. */
public class ASTHelpers {
  /**
   * Determines whether two expressions refer to the same variable. Note that returning false
   * doesn't necessarily mean the expressions do *not* refer to the same field. We don't attempt to
   * do any complex analysis here, just catch the obvious cases.
   */
  public static boolean sameVariable(ExpressionTree expr1, ExpressionTree expr2) {
    requireNonNull(expr1);
    requireNonNull(expr2);
    // Throw up our hands if we're not comparing identifiers and/or field accesses.
    if ((expr1.getKind() != Kind.IDENTIFIER && expr1.getKind() != Kind.MEMBER_SELECT)
        || (expr2.getKind() != Kind.IDENTIFIER && expr2.getKind() != Kind.MEMBER_SELECT)) {
      return false;
    }

    Symbol sym1 = getSymbol(expr1);
    Symbol sym2 = getSymbol(expr2);
    if (sym1 == null) {
      throw new IllegalStateException("Couldn't get symbol for " + expr1);
    } else if (sym2 == null) {
      throw new IllegalStateException("Couldn't get symbol for " + expr2);
    }

    if (expr1.getKind() == Kind.IDENTIFIER && expr2.getKind() == Kind.IDENTIFIER) {
      // foo == foo?
      return sym1.equals(sym2);
    } else if (expr1.getKind() == Kind.MEMBER_SELECT && expr2.getKind() == Kind.MEMBER_SELECT) {
      // foo.baz.bar == foo.baz.bar?
      return sym1.equals(sym2)
          && sameVariable(((JCFieldAccess) expr1).selected, ((JCFieldAccess) expr2).selected);
    } else {
      // this.foo == foo?
      ExpressionTree selected = null;
      if (expr1.getKind() == Kind.IDENTIFIER) {
        selected = ((JCFieldAccess) expr2).selected;
      } else {
        selected = ((JCFieldAccess) expr1).selected;
      }
      // TODO(eaftan): really shouldn't be relying on .toString()
      return selected.toString().equals("this") && sym1.equals(sym2);
    }
  }

  /**
   * Gets the symbol declared by a tree. Returns null if {@code tree} does not declare a symbol or
   * is null.
   */
  @Nullable
  public static Symbol getDeclaredSymbol(Tree tree) {
    if (tree instanceof PackageTree) {
      return getSymbol((PackageTree) tree);
    }
    if (tree instanceof TypeParameterTree) {
      Type type = ((JCTypeParameter) tree).type;
      return type == null ? null : type.tsym;
    }
    if (tree instanceof ClassTree) {
      return getSymbol((ClassTree) tree);
    }
    if (tree instanceof MethodTree) {
      return getSymbol((MethodTree) tree);
    }
    if (tree instanceof VariableTree) {
      return getSymbol((VariableTree) tree);
    }
    return null;
  }

  /**
   * Gets the symbol for a tree. Returns null if this tree does not have a symbol because it is of
   * the wrong type, if {@code tree} is null, or if the symbol cannot be found due to a compilation
   * error.
   */
  // TODO(eaftan): refactor other code that accesses symbols to use this method
  public static Symbol getSymbol(Tree tree) {
    if (tree instanceof AnnotationTree) {
      return getSymbol(((AnnotationTree) tree).getAnnotationType());
    }
    if (tree instanceof JCFieldAccess) {
      return ((JCFieldAccess) tree).sym;
    }
    if (tree instanceof JCIdent) {
      return ((JCIdent) tree).sym;
    }
    if (tree instanceof JCMethodInvocation) {
      return ASTHelpers.getSymbol((MethodInvocationTree) tree);
    }
    if (tree instanceof JCNewClass) {
      return ASTHelpers.getSymbol((NewClassTree) tree);
    }
    if (tree instanceof MemberReferenceTree) {
      return ((JCMemberReference) tree).sym;
    }
    if (tree instanceof JCAnnotatedType) {
      return getSymbol(((JCAnnotatedType) tree).underlyingType);
    }
    if (tree instanceof ParameterizedTypeTree) {
      return getSymbol(((ParameterizedTypeTree) tree).getType());
    }
    if (tree instanceof ClassTree) {
      return getSymbol((ClassTree) tree);
    }

    return getDeclaredSymbol(tree);
  }

  /** Gets the symbol for a class. */
  public static ClassSymbol getSymbol(ClassTree tree) {
    return ((JCClassDecl) tree).sym;
  }

  /** Gets the symbol for a package. */
  public static PackageSymbol getSymbol(PackageTree tree) {
    return ((JCPackageDecl) tree).packge;
  }

  /** Gets the symbol for a method. */
  public static MethodSymbol getSymbol(MethodTree tree) {
    return ((JCMethodDecl) tree).sym;
  }

  /** Gets the method symbol for a new class. */
  @Nullable
  public static MethodSymbol getSymbol(NewClassTree tree) {
    Symbol sym = ((JCNewClass) tree).constructor;
    return sym instanceof MethodSymbol ? (MethodSymbol) sym : null;
  }

  /** Gets the symbol for a variable. */
  public static VarSymbol getSymbol(VariableTree tree) {
    return ((JCVariableDecl) tree).sym;
  }

  /** Gets the symbol for a method invocation. */
  @Nullable
  public static MethodSymbol getSymbol(MethodInvocationTree tree) {
    Symbol sym = ASTHelpers.getSymbol(tree.getMethodSelect());
    if (!(sym instanceof MethodSymbol)) {
      // Defensive. Would only occur if there are errors in the AST.
      return null;
    }
    return (MethodSymbol) sym;
  }

  /** Gets the symbol for a member reference. */
  @Nullable
  public static MethodSymbol getSymbol(MemberReferenceTree tree) {
    Symbol sym = ((JCMemberReference) tree).sym;
    return sym instanceof MethodSymbol ? (MethodSymbol) sym : null;
  }

  /* Checks whether an expression requires parentheses. */
  public static boolean requiresParentheses(ExpressionTree expression, VisitorState state) {
    switch (expression.getKind()) {
      case IDENTIFIER:
      case MEMBER_SELECT:
      case METHOD_INVOCATION:
      case ARRAY_ACCESS:
      case PARENTHESIZED:
      case NEW_CLASS:
      case MEMBER_REFERENCE:
        return false;
      case LAMBDA_EXPRESSION:
        // Parenthesizing e.g. `x -> (y -> z)` is unnecessary but helpful
        Tree parent = state.getPath().getParentPath().getLeaf();
        return parent.getKind().equals(Kind.LAMBDA_EXPRESSION)
            && stripParentheses(((LambdaExpressionTree) parent).getBody()).equals(expression);
      default: // continue below
    }
    if (expression instanceof LiteralTree) {
      if (!isSameType(getType(expression), state.getSymtab().stringType, state)) {
        return false;
      }
      // TODO(b/112139121): work around for javac's too-early constant string folding
      return state.getOffsetTokensForNode(expression).stream()
          .anyMatch(t -> t.kind() == TokenKind.PLUS);
    }
    if (expression instanceof UnaryTree) {
      Tree parent = state.getPath().getParentPath().getLeaf();
      if (!(parent instanceof MemberSelectTree)) {
        return false;
      }
      // eg. (i++).toString();
      return stripParentheses(((MemberSelectTree) parent).getExpression()).equals(expression);
    }
    return true;
  }

  /** Removes any enclosing parentheses from the tree. */
  public static Tree stripParentheses(Tree tree) {
    return tree instanceof ExpressionTree ? stripParentheses((ExpressionTree) tree) : tree;
  }

  /** Given an ExpressionTree, removes any enclosing parentheses. */
  public static ExpressionTree stripParentheses(ExpressionTree tree) {
    while (tree instanceof ParenthesizedTree) {
      tree = ((ParenthesizedTree) tree).getExpression();
    }
    return tree;
  }

  /**
   * Given a TreePath, finds the first enclosing node of the given type and returns the path from
   * the enclosing node to the top-level {@code CompilationUnitTree}.
   */
  public static <T> TreePath findPathFromEnclosingNodeToTopLevel(TreePath path, Class<T> klass) {
    if (path != null) {
      do {
        path = path.getParentPath();
      } while (path != null && !(klass.isInstance(path.getLeaf())));
    }
    return path;
  }

  /**
   * Given a TreePath, walks up the tree until it finds a node of the given type. Returns null if no
   * such node is found.
   */
  @Nullable
  public static <T> T findEnclosingNode(TreePath path, Class<T> klass) {
    path = findPathFromEnclosingNodeToTopLevel(path, klass);
    return (path == null) ? null : klass.cast(path.getLeaf());
  }

  /**
   * Find the root assignable expression of a chain of field accesses. If there is no root (i.e, a
   * bare method call or a static method call), return null.
   *
   * <p>Examples:
   *
   * <pre>{@code
   * a.trim().intern() ==> a
   * a.b.trim().intern() ==> a.b
   * this.intValue.foo() ==> this.intValue
   * this.foo() ==> this
   * intern() ==> null
   * String.format() ==> null
   * java.lang.String.format() ==> null
   * }</pre>
   */
  @Nullable
  public static ExpressionTree getRootAssignable(MethodInvocationTree methodInvocationTree) {
    if (!(methodInvocationTree instanceof JCMethodInvocation)) {
      throw new IllegalArgumentException(
          "Expected type to be JCMethodInvocation, but was " + methodInvocationTree.getClass());
    }

    // Check for bare method call, e.g. intern().
    if (((JCMethodInvocation) methodInvocationTree).getMethodSelect() instanceof JCIdent) {
      return null;
    }

    // Unwrap the field accesses until you get to an identifier.
    ExpressionTree expr = methodInvocationTree;
    while (expr instanceof JCMethodInvocation) {
      expr = ((JCMethodInvocation) expr).getMethodSelect();
      if (expr instanceof JCFieldAccess) {
        expr = ((JCFieldAccess) expr).getExpression();
      }
    }

    // We only want assignable identifiers.
    Symbol sym = getSymbol(expr);
    if (sym instanceof VarSymbol) {
      return expr;
    }
    return null;
  }

  /**
   * Gives the return type of an ExpressionTree that represents a method select.
   *
   * <p>TODO(eaftan): Are there other places this could be used?
   */
  public static Type getReturnType(ExpressionTree expressionTree) {
    if (expressionTree instanceof JCFieldAccess) {
      JCFieldAccess methodCall = (JCFieldAccess) expressionTree;
      return methodCall.type.getReturnType();
    } else if (expressionTree instanceof JCIdent) {
      JCIdent methodCall = (JCIdent) expressionTree;
      return methodCall.type.getReturnType();
    } else if (expressionTree instanceof JCMethodInvocation) {
      return getReturnType(((JCMethodInvocation) expressionTree).getMethodSelect());
    } else if (expressionTree instanceof JCMemberReference) {
      return ((JCMemberReference) expressionTree).sym.type.getReturnType();
    }
    throw new IllegalArgumentException("Expected a JCFieldAccess or JCIdent");
  }

  /**
   * Returns the type that this expression tree will evaluate to. If its a literal, an identifier,
   * or a member select this is the actual type, if it's a method invocation then it's the return
   * type of the method (after instantiating generic types), if it's a constructor then it's the
   * type of the returned class.
   *
   * <p>TODO(andrewrice) consider replacing {@code getReturnType} with this method
   *
   * @param expressionTree the tree to evaluate
   * @return the result type of this tree or null if unable to resolve it
   */
  @Nullable
  public static Type getResultType(ExpressionTree expressionTree) {
    Type type = ASTHelpers.getType(expressionTree);
    return type == null ? null : Optional.ofNullable(type.getReturnType()).orElse(type);
  }

  /**
   * Returns the type of a receiver of a method call expression. Precondition: the expressionTree
   * corresponds to a method call.
   *
   * <p>Examples:
   *
   * <pre>{@code
   * a.b.foo() ==> type of a.b
   * a.bar().foo() ==> type of a.bar()
   * this.foo() ==> type of this
   * foo() ==> type of this
   * TheClass.aStaticMethod() ==> TheClass
   * aStaticMethod() ==> type of class in which method is defined
   * }</pre>
   */
  public static Type getReceiverType(ExpressionTree expressionTree) {
    if (expressionTree instanceof JCFieldAccess) {
      JCFieldAccess methodSelectFieldAccess = (JCFieldAccess) expressionTree;
      return methodSelectFieldAccess.selected.type;
    } else if (expressionTree instanceof JCIdent) {
      JCIdent methodCall = (JCIdent) expressionTree;
      return methodCall.sym.owner.type;
    } else if (expressionTree instanceof JCMethodInvocation) {
      return getReceiverType(((JCMethodInvocation) expressionTree).getMethodSelect());
    } else if (expressionTree instanceof JCMemberReference) {
      return ((JCMemberReference) expressionTree).getQualifierExpression().type;
    }
    throw new IllegalArgumentException(
        "Expected a JCFieldAccess or JCIdent from expression " + expressionTree);
  }

  /**
   * Returns the receiver of an expression.
   *
   * <p>Examples:
   *
   * <pre>{@code
   * a.foo() ==> a
   * a.b.foo() ==> a.b
   * a.bar().foo() ==> a.bar()
   * a.b.c ==> a.b
   * a.b().c ==> a.b()
   * this.foo() ==> this
   * foo() ==> null
   * TheClass.aStaticMethod() ==> TheClass
   * aStaticMethod() ==> null
   * aStaticallyImportedMethod() ==> null
   * }</pre>
   */
  @Nullable
  public static ExpressionTree getReceiver(ExpressionTree expressionTree) {
    if (expressionTree instanceof MethodInvocationTree) {
      ExpressionTree methodSelect = ((MethodInvocationTree) expressionTree).getMethodSelect();
      if (methodSelect instanceof IdentifierTree) {
        return null;
      }
      return getReceiver(methodSelect);
    } else if (expressionTree instanceof MemberSelectTree) {
      return ((MemberSelectTree) expressionTree).getExpression();
    } else if (expressionTree instanceof MemberReferenceTree) {
      return ((MemberReferenceTree) expressionTree).getQualifierExpression();
    } else {
      throw new IllegalStateException(
          String.format(
              "Expected expression '%s' to be a method invocation or field access, but was %s",
              expressionTree, expressionTree.getKind()));
    }
  }

  /**
   * Given a BinaryTree to match against and a list of two matchers, applies the matchers to the
   * operands in both orders. If both matchers match, returns a list with the operand that matched
   * each matcher in the corresponding position.
   *
   * @param tree a BinaryTree AST node
   * @param matchers a list of matchers
   * @param state the VisitorState
   * @return a list of matched operands, or null if at least one did not match
   */
  @Nullable
  public static List<ExpressionTree> matchBinaryTree(
      BinaryTree tree, List<Matcher<ExpressionTree>> matchers, VisitorState state) {
    ExpressionTree leftOperand = tree.getLeftOperand();
    ExpressionTree rightOperand = tree.getRightOperand();
    if (matchers.get(0).matches(leftOperand, state)
        && matchers.get(1).matches(rightOperand, state)) {
      return Arrays.asList(leftOperand, rightOperand);
    } else if (matchers.get(0).matches(rightOperand, state)
        && matchers.get(1).matches(leftOperand, state)) {
      return Arrays.asList(rightOperand, leftOperand);
    }
    return null;
  }

  /**
   * Returns the method tree that matches the given symbol within the compilation unit, or null if
   * none was found.
   */
  @Nullable
  public static MethodTree findMethod(MethodSymbol symbol, VisitorState state) {
    return JavacTrees.instance(state.context).getTree(symbol);
  }

  /**
   * Returns the class tree that matches the given symbol within the compilation unit, or null if
   * none was found.
   */
  @Nullable
  public static ClassTree findClass(ClassSymbol symbol, VisitorState state) {
    return JavacTrees.instance(state.context).getTree(symbol);
  }

  // TODO(ghm): Using a comparison of tsym here appears to be a behaviour change.
  @SuppressWarnings("TypeEquals")
  @Nullable
  public static MethodSymbol findSuperMethodInType(
      MethodSymbol methodSymbol, Type superType, Types types) {
    if (methodSymbol.isStatic() || superType.equals(methodSymbol.owner.type)) {
      return null;
    }

    Scope scope = superType.tsym.members();
    for (Symbol sym : scope.getSymbolsByName(methodSymbol.name)) {
      if (sym != null
          && !sym.isStatic()
          && ((sym.flags() & Flags.SYNTHETIC) == 0)
          && methodSymbol.overrides(
              sym, (TypeSymbol) methodSymbol.owner, types, /* checkResult= */ true)) {
        return (MethodSymbol) sym;
      }
    }
    return null;
  }

  public static Set<MethodSymbol> findSuperMethods(MethodSymbol methodSymbol, Types types) {
    return findSuperMethods(methodSymbol, types, /* skipInterfaces= */ false)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  /**
   * Finds (if it exists) first (in the class hierarchy) non-interface super method of given {@code
   * method}.
   */
  public static Optional<MethodSymbol> findSuperMethod(MethodSymbol methodSymbol, Types types) {
    return findSuperMethods(methodSymbol, types, /* skipInterfaces= */ true).findFirst();
  }

  private static Stream<MethodSymbol> findSuperMethods(
      MethodSymbol methodSymbol, Types types, boolean skipInterfaces) {
    TypeSymbol owner = (TypeSymbol) methodSymbol.owner;
    Stream<Type> typeStream = types.closure(owner.type).stream();
    if (skipInterfaces) {
      typeStream = typeStream.filter(type -> !type.isInterface());
    }
    return typeStream
        .map(type -> findSuperMethodInType(methodSymbol, type, types))
        .filter(Objects::nonNull);
  }

  /**
   * Finds all methods in any superclass of {@code startClass} with a certain {@code name} that
   * match the given {@code predicate}.
   *
   * @return The (possibly empty) list of methods in any superclass that match {@code predicate} and
   *     have the given {@code name}. Results are returned least-abstract first, i.e., starting in
   *     the {@code startClass} itself, progressing through its superclasses, and finally interfaces
   *     in an unspecified order.
   */
  public static Stream<MethodSymbol> matchingMethods(
      Name name,
      java.util.function.Predicate<MethodSymbol> predicate,
      Type startClass,
      Types types) {
    Filter<Symbol> matchesMethodPredicate =
        sym -> sym instanceof MethodSymbol && predicate.test((MethodSymbol) sym);

    // Iterate over all classes and interfaces that startClass inherits from.
    return types.closure(startClass).stream()
        .flatMap(
            (Type superClass) -> {
              // Iterate over all the methods declared in superClass.
              TypeSymbol superClassSymbol = superClass.tsym;
              Scope superClassSymbols = superClassSymbol.members();
              if (superClassSymbols == null) { // Can be null if superClass is a type variable
                return Stream.empty();
              }
              return Streams.stream(
                      superClassSymbols.getSymbolsByName(
                          name, matchesMethodPredicate, NON_RECURSIVE))
                  // By definition of the filter, we know that the symbol is a MethodSymbol.
                  .map(symbol -> (MethodSymbol) symbol);
            });
  }

  /**
   * Finds all methods in any superclass of {@code startClass} with a certain {@code name} that
   * match the given {@code predicate}.
   *
   * @return The (possibly empty) set of methods in any superclass that match {@code predicate} and
   *     have the given {@code name}. The set's iteration order will be the same as the order
   *     documented in {@link #matchingMethods(Name, java.util.function.Predicate, Type, Types)}.
   */
  public static Set<MethodSymbol> findMatchingMethods(
      Name name, Predicate<MethodSymbol> predicate, Type startClass, Types types) {
    return matchingMethods(name, predicate, startClass, types).collect(toImmutableSet());
  }

  /**
   * Determines whether a method can be overridden.
   *
   * @return true if the method can be overridden.
   */
  public static boolean methodCanBeOverridden(MethodSymbol methodSymbol) {
    if (methodSymbol.getModifiers().contains(Modifier.ABSTRACT)) {
      return true;
    }

    if (methodSymbol.isStatic()
        || methodSymbol.isPrivate()
        || isFinal(methodSymbol)
        || methodSymbol.isConstructor()) {
      return false;
    }

    ClassSymbol classSymbol = (ClassSymbol) methodSymbol.owner;
    return !isFinal(classSymbol) && !classSymbol.isAnonymous();
  }

  private static boolean isFinal(Symbol symbol) {
    return (symbol.flags() & Flags.FINAL) == Flags.FINAL;
  }

  /**
   * Determines whether a symbol has an annotation of the given type. This includes annotations
   * inherited from superclasses due to {@code @Inherited}.
   *
   * @param annotationClass the binary class name of the annotation (e.g.
   *     "javax.annotation.Nullable", or "some.package.OuterClassName$InnerClassName")
   * @return true if the symbol is annotated with given type.
   */
  public static boolean hasAnnotation(Symbol sym, String annotationClass, VisitorState state) {
    if (sym == null) {
      return false;
    }
    // TODO(amalloy): unify with hasAnnotation(Symbol, Name, VisitorState)
    // normalize to non-binary names
    annotationClass = annotationClass.replace('$', '.');
    Name annotationName = state.getName(annotationClass);
    if (hasAttribute(sym, annotationName)) {
      return true;
    }
    if (sym instanceof ClassSymbol && isInherited(state, annotationClass)) {
      do {
        if (hasAttribute(sym, annotationName)) {
          return true;
        }
        sym = ((ClassSymbol) sym).getSuperclass().tsym;
      } while (sym instanceof ClassSymbol);
    }
    return false;
  }

  private static final Cache<Name, Boolean> inheritedAnnotationCache =
      Caffeine.newBuilder().maximumSize(1000).build();

  @SuppressWarnings("ConstantConditions") // IntelliJ worries unboxing our Boolean may throw NPE.
  private static boolean isInherited(VisitorState state, Name annotationName) {
    return inheritedAnnotationCache.get(
        annotationName,
        name -> {
          Symbol annotationSym = state.getSymbolFromName(name);
          if (annotationSym == null) {
            return false;
          }
          try {
            annotationSym.complete();
          } catch (CompletionFailure e) {
            /* @Inherited won't work if the annotation isn't on the classpath, but we can still
            check if it's present directly */
          }
          Symbol inheritedSym = state.getSymtab().inheritedType.tsym;
          return annotationSym.attribute(inheritedSym) != null;
        });
  }

  private static boolean isInherited(VisitorState state, String annotationName) {
    return isInherited(state, state.binaryNameFromClassname(annotationName));
  }

  private static boolean hasAttribute(Symbol sym, Name annotationName) {
    for (Compound a : sym.getRawAttributes()) {
      if (a.type.tsym.getQualifiedName().equals(annotationName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Determines which of a set of annotations are present on a symbol.
   *
   * @param sym The symbol to inspect for annotations
   * @param annotationClasses The annotations of interest to look for, Each name must be in binary
   *     form, e.g. "com.google.Foo$Bar", not "com.google.Foo.Bar".
   * @return A possibly-empty set of annotations present on the queried element.
   */
  public static Set<Name> annotationsAmong(
      Symbol sym, Set<? extends Name> annotationClasses, VisitorState state) {
    if (sym == null) {
      return ImmutableSet.of();
    }
    Set<Name> result = directAnnotationsAmong(sym, annotationClasses);
    if (!(sym instanceof ClassSymbol)) {
      return result;
    }

    Set<Name> possibleInherited = new HashSet<>();
    for (Name a : annotationClasses) {
      if (!result.contains(a) && isInherited(state, a)) {
        possibleInherited.add(a);
      }
    }
    sym = ((ClassSymbol) sym).getSuperclass().tsym;
    while (sym instanceof ClassSymbol && !possibleInherited.isEmpty()) {
      for (Name local : directAnnotationsAmong(sym, possibleInherited)) {
        result.add(local);
        possibleInherited.remove(local);
      }
      sym = ((ClassSymbol) sym).getSuperclass().tsym;
    }
    return result;
  }

  /**
   * Explicitly returns a modifiable {@code Set<Name>}, so that annotationsAmong can futz with it to
   * add inherited annotations.
   */
  private static Set<Name> directAnnotationsAmong(
      Symbol sym, Set<? extends Name> binaryAnnotationNames) {
    Set<Name> result = new HashSet<>();
    for (Compound a : sym.getRawAttributes()) {
      Name annoName = a.type.tsym.flatName();
      if (binaryAnnotationNames.contains(annoName)) {
        result.add(annoName);
      }
    }
    return result;
  }

  /**
   * Check for the presence of an annotation, considering annotation inheritance.
   *
   * @return true if the symbol is annotated with given type.
   */
  public static boolean hasAnnotation(
      Symbol sym, Class<? extends Annotation> annotationClass, VisitorState state) {
    return hasAnnotation(sym, annotationClass.getName(), state);
  }

  /**
   * Check for the presence of an annotation, considering annotation inheritance.
   *
   * @param annotationClass the binary class name of the annotation (e.g.
   *     "javax.annotation.Nullable", or "some.package.OuterClassName$InnerClassName")
   * @return true if the tree is annotated with given type.
   */
  public static boolean hasAnnotation(Tree tree, String annotationClass, VisitorState state) {
    Symbol sym = getDeclaredSymbol(tree);
    return hasAnnotation(sym, annotationClass, state);
  }

  /**
   * Check for the presence of an annotation, considering annotation inheritance.
   *
   * @return true if the tree is annotated with given type.
   */
  public static boolean hasAnnotation(
      Tree tree, Class<? extends Annotation> annotationClass, VisitorState state) {
    return hasAnnotation(tree, annotationClass.getName(), state);
  }

  /**
   * Check for the presence of an annotation with a specific simple name directly on this symbol.
   * Does *not* consider annotation inheritance.
   *
   * @param sym the symbol to check for the presence of the annotation
   * @param simpleName the simple name of the annotation to look for, e.g. "Nullable" or
   *     "CheckReturnValue"
   */
  public static boolean hasDirectAnnotationWithSimpleName(Symbol sym, String simpleName) {
    for (AnnotationMirror annotation : sym.getAnnotationMirrors()) {
      if (annotation.getAnnotationType().asElement().getSimpleName().contentEquals(simpleName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check for the presence of an annotation with a specific simple name directly on this symbol.
   * Does *not* consider annotation inheritance.
   *
   * @param tree the tree to check for the presence of the annotation
   * @param simpleName the simple name of the annotation to look for, e.g. "Nullable" or
   *     "CheckReturnValue"
   */
  public static boolean hasDirectAnnotationWithSimpleName(Tree tree, String simpleName) {
    return hasDirectAnnotationWithSimpleName(getDeclaredSymbol(tree), simpleName);
  }

  /**
   * Retrieves an annotation, considering annotation inheritance.
   *
   * @deprecated If {@code annotationClass} contains a member that is a {@code Class} or an array of
   *     them, attempting to access that member from the Error Prone checker code will result in a
   *     runtime exception. Instead, operate on {@code sym.getAnnotationMirrors()} to
   *     meta-syntactically inspect the annotation.
   */
  @Nullable
  @Deprecated
  public static <T extends Annotation> T getAnnotation(Tree tree, Class<T> annotationClass) {
    Symbol sym = getSymbol(tree);
    return sym == null ? null : getAnnotation(sym, annotationClass);
  }

  /**
   * Retrieves an annotation, considering annotation inheritance.
   *
   * @deprecated If {@code annotationClass} contains a member that is a {@code Class} or an array of
   *     them, attempting to access that member from the Error Prone checker code will result in a
   *     runtime exception. Instead, operate on {@code sym.getAnnotationMirrors()} to
   *     meta-syntactically inspect the annotation.
   */
  @Nullable
  @Deprecated
  @SuppressWarnings("deprecation")
  public static <T extends Annotation> T getAnnotation(Symbol sym, Class<T> annotationClass) {
    return sym == null ? null : sym.getAnnotation(annotationClass);
  }

  /** @return all values of the given enum type, in declaration order. */
  public static LinkedHashSet<String> enumValues(TypeSymbol enumType) {
    if (enumType.getKind() != ElementKind.ENUM) {
      throw new IllegalStateException();
    }
    Scope scope = enumType.members();
    Deque<String> values = new ArrayDeque<>();
    for (Symbol sym : scope.getSymbols()) {
      if (sym instanceof VarSymbol) {
        VarSymbol var = (VarSymbol) sym;
        if ((var.flags() & Flags.ENUM) != 0) {
          /*
           * Javac gives us the members backwards, apparently. It's worth making an effort to
           * preserve declaration order because it's useful for diagnostics (e.g. in {@link
           * MissingCasesInEnumSwitch}).
           */
          values.push(sym.name.toString());
        }
      }
    }
    return new LinkedHashSet<>(values);
  }

  /** Returns true if the given tree is a generated constructor. * */
  public static boolean isGeneratedConstructor(MethodTree tree) {
    if (!(tree instanceof JCMethodDecl)) {
      return false;
    }
    return (((JCMethodDecl) tree).mods.flags & Flags.GENERATEDCONSTR) == Flags.GENERATEDCONSTR;
  }

  /** Returns the list of all constructors defined in the class (including generated ones). */
  public static List<MethodTree> getConstructors(ClassTree classTree) {
    List<MethodTree> constructors = new ArrayList<>();
    for (Tree member : classTree.getMembers()) {
      if (member instanceof MethodTree) {
        MethodTree methodTree = (MethodTree) member;
        if (getSymbol(methodTree).isConstructor()) {
          constructors.add(methodTree);
        }
      }
    }
    return constructors;
  }

  /** Returns the list of all constructors defined in the class. */
  public static ImmutableList<MethodSymbol> getConstructors(ClassSymbol classSymbol) {
    return classSymbol.getEnclosedElements().stream()
        .filter(Symbol::isConstructor)
        .map(e -> (MethodSymbol) e)
        .collect(toImmutableList());
  }

  /**
   * Returns the {@code Type} of the given tree, or {@code null} if the type could not be
   * determined.
   */
  @Nullable
  public static Type getType(@Nullable Tree tree) {
    return tree instanceof JCTree ? ((JCTree) tree).type : null;
  }

  /**
   * Returns the {@code ClassType} for the given type {@code ClassTree} or {@code null} if the type
   * could not be determined.
   */
  @Nullable
  public static ClassType getType(@Nullable ClassTree tree) {
    Type type = getType((Tree) tree);
    return type instanceof ClassType ? (ClassType) type : null;
  }

  @Nullable
  public static String getAnnotationName(AnnotationTree tree) {
    Symbol sym = getSymbol(tree);
    return sym == null ? null : sym.name.toString();
  }

  /** Returns the erasure of the given type tree, i.e. {@code List} for {@code List<Foo>}. */
  public static Tree getErasedTypeTree(Tree tree) {
    return tree.accept(
        new SimpleTreeVisitor<Tree, Void>() {
          @Override
          public Tree visitIdentifier(IdentifierTree tree, Void unused) {
            return tree;
          }

          @Override
          public Tree visitParameterizedType(ParameterizedTypeTree tree, Void unused) {
            return tree.getType();
          }
        },
        null);
  }

  /** Return the enclosing {@code ClassSymbol} of the given symbol, or {@code null}. */
  public static ClassSymbol enclosingClass(Symbol sym) {
    // sym.owner is null in the case of module symbols.
    return sym.owner == null ? null : sym.owner.enclClass();
  }

  /** Return the enclosing {@code PackageSymbol} of the given symbol, or {@code null}. */
  public static PackageSymbol enclosingPackage(Symbol sym) {
    return sym.packge();
  }

  /** Return true if the given symbol is defined in the current package. */
  public static boolean inSamePackage(Symbol targetSymbol, VisitorState state) {
    JCCompilationUnit compilationUnit = (JCCompilationUnit) state.getPath().getCompilationUnit();
    PackageSymbol usePackage = compilationUnit.packge;
    PackageSymbol targetPackage = targetSymbol.packge();

    return targetPackage != null
        && usePackage != null
        && targetPackage.getQualifiedName().equals(usePackage.getQualifiedName());
  }

  /**
   * Returns the {@link Nullness} for an expression as determined by the nullness dataflow analysis.
   */
  public static Nullness getNullnessValue(
      ExpressionTree expr, VisitorState state, NullnessAnalysis nullnessAnalysis) {
    TreePath pathToExpr = new TreePath(state.getPath(), expr);
    return nullnessAnalysis.getNullness(pathToExpr, state.context);
  }

  /** Returns the compile-time constant value of a tree if it has one, or {@code null}. */
  @Nullable
  public static Object constValue(Tree tree) {
    if (tree == null) {
      return null;
    }
    tree = stripParentheses(tree);
    Type type = ASTHelpers.getType(tree);
    Object value;
    if (tree instanceof JCLiteral) {
      value = ((JCLiteral) tree).value;
    } else if (type != null) {
      value = type.constValue();
    } else {
      return null;
    }
    if (type.hasTag(TypeTag.BOOLEAN) && value instanceof Integer) {
      return ((Integer) value) == 1;
    }
    return value;
  }

  /** Returns the compile-time constant value of a tree if it is of type clazz, or {@code null}. */
  @Nullable
  public static <T> T constValue(Tree tree, Class<? extends T> clazz) {
    Object value = constValue(tree);
    return clazz.isInstance(value) ? clazz.cast(value) : null;
  }

  /** Return true if the given type is 'void' or 'Void'. */
  public static boolean isVoidType(Type type, VisitorState state) {
    if (type == null) {
      return false;
    }
    return type.getKind() == TypeKind.VOID
        || state.getTypes().isSameType(Suppliers.JAVA_LANG_VOID_TYPE.get(state), type);
  }

  private static final Set<TypeTag> SUBTYPE_UNDEFINED =
      EnumSet.of(TypeTag.METHOD, TypeTag.PACKAGE, TypeTag.UNKNOWN, TypeTag.ERROR, TypeTag.FORALL);

  /** Returns true if {@code erasure(s) <: erasure(t)}. */
  public static boolean isSubtype(Type s, Type t, VisitorState state) {
    if (s == null || t == null) {
      return false;
    }
    if (SUBTYPE_UNDEFINED.contains(s.getTag()) || SUBTYPE_UNDEFINED.contains(t.getTag())) {
      return false;
    }
    Types types = state.getTypes();
    return types.isSubtype(types.erasure(s), types.erasure(t));
  }

  /**
   * Returns true if {@code t} is a subtype of Throwable but not a subtype of RuntimeException or
   * Error.
   */
  public static boolean isCheckedExceptionType(Type t, VisitorState state) {
    Symtab symtab = state.getSymtab();
    return isSubtype(t, symtab.throwableType, state)
        && !isSubtype(t, symtab.runtimeExceptionType, state)
        && !isSubtype(t, symtab.errorType, state);
  }

  /** Returns true if {@code erasure(s)} is castable to {@code erasure(t)}. */
  public static boolean isCastable(Type s, Type t, VisitorState state) {
    if (s == null || t == null) {
      return false;
    }
    Types types = state.getTypes();
    return types.isCastable(types.erasure(s), types.erasure(t));
  }

  /** Returns true if {@code erasure(s) == erasure(t)}. */
  public static boolean isSameType(Type s, Type t, VisitorState state) {
    if (s == null || t == null) {
      return false;
    }
    Types types = state.getTypes();
    return types.isSameType(types.erasure(s), types.erasure(t));
  }

  /** Returns the modifiers tree of the given class, method, or variable declaration. */
  @Nullable
  public static ModifiersTree getModifiers(Tree tree) {
    if (tree instanceof ClassTree) {
      return ((ClassTree) tree).getModifiers();
    } else if (tree instanceof MethodTree) {
      return ((MethodTree) tree).getModifiers();
    } else if (tree instanceof VariableTree) {
      return ((VariableTree) tree).getModifiers();
    } else {
      return null;
    }
  }

  /**
   * Returns the upper bound of a type if it has one, or the type itself if not. Correctly handles
   * wildcards and capture variables.
   */
  public static Type getUpperBound(Type type, Types types) {
    if (type.hasTag(TypeTag.WILDCARD)) {
      return types.wildUpperBound(type);
    }

    if (type.hasTag(TypeTag.TYPEVAR) && ((TypeVar) type).isCaptured()) {
      return types.cvarUpperBound(type);
    }

    if (type.getUpperBound() != null) {
      return type.getUpperBound();
    }

    // concrete type, e.g. java.lang.String, or a case we haven't considered
    return type;
  }

  /**
   * Returns true if the leaf node in the {@link TreePath} from {@code state} sits somewhere
   * underneath a class or method that is marked as JUnit 3 or 4 test code.
   */
  public static boolean isJUnitTestCode(VisitorState state) {
    for (Tree ancestor : state.getPath()) {
      if (ancestor instanceof MethodTree
          && JUnitMatchers.hasJUnitAnnotation((MethodTree) ancestor, state)) {
        return true;
      }
      if (ancestor instanceof ClassTree
          && (JUnitMatchers.isTestCaseDescendant.matches((ClassTree) ancestor, state)
              || hasAnnotation(getSymbol(ancestor), JUNIT4_RUN_WITH_ANNOTATION, state))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if the leaf node in the {@link TreePath} from {@code state} sits somewhere
   * underneath a class or method that is marked as TestNG test code.
   */
  public static boolean isTestNgTestCode(VisitorState state) {
    for (Tree ancestor : state.getPath()) {
      if (ancestor instanceof MethodTree
          && TestNgMatchers.hasTestNgAnnotation((MethodTree) ancestor, state)) {
        return true;
      }
    }
    return false;
  }

  /** Returns an {@link AnnotationTree} with the given simple name, or {@code null}. */
  @Nullable
  public static AnnotationTree getAnnotationWithSimpleName(
      List<? extends AnnotationTree> annotations, String name) {
    for (AnnotationTree annotation : annotations) {
      if (hasSimpleName(annotation, name)) {
        return annotation;
      }
    }
    return null;
  }

  /**
   * Returns a list of {@link AnnotationTree} with the given simple name. This is useful for {@link
   * java.lang.annotation.Repeatable} annotations
   */
  public static List<AnnotationTree> getAnnotationsWithSimpleName(
      List<? extends AnnotationTree> annotations, String name) {
    List<AnnotationTree> matches = new ArrayList<>();
    for (AnnotationTree annotation : annotations) {
      if (hasSimpleName(annotation, name)) {
        matches.add(annotation);
      }
    }
    return matches;
  }

  private static boolean hasSimpleName(AnnotationTree annotation, String name) {
    Tree annotationType = annotation.getAnnotationType();
    javax.lang.model.element.Name simpleName;
    if (annotationType instanceof IdentifierTree) {
      simpleName = ((IdentifierTree) annotationType).getName();
    } else if (annotationType instanceof MemberSelectTree) {
      simpleName = ((MemberSelectTree) annotationType).getIdentifier();
    } else {
      return false;
    }
    return simpleName.contentEquals(name);
  }

  /**
   * Returns whether {@code anno} corresponds to a type annotation, or {@code null} if it could not
   * be determined.
   */
  @Nullable
  public static AnnotationType getAnnotationType(
      AnnotationTree anno, @Nullable Symbol target, VisitorState state) {
    if (target == null) {
      return null;
    }
    Symbol annoSymbol = getSymbol(anno);
    if (annoSymbol == null) {
      return null;
    }
    Compound compound = target.attribute(annoSymbol);
    if (compound == null) {
      for (TypeCompound typeCompound : target.getRawTypeAttributes()) {
        if (typeCompound.type.tsym.equals(annoSymbol)) {
          compound = typeCompound;
          break;
        }
      }
    }
    if (compound == null) {
      return null;
    }
    return TypeAnnotations.instance(state.context).annotationTargetType(compound, target);
  }

  /**
   * Extract the filename from a {@link CompilationUnitTree}, with special handling for jar files.
   * The return value is normalized to always use '/' to separate elements of the path and to always
   * have a leading '/'.
   */
  @Nullable
  public static String getFileName(CompilationUnitTree tree) {
    return getFileNameFromUri(tree.getSourceFile().toUri());
  }

  private static final CharMatcher BACKSLASH_MATCHER = CharMatcher.is('\\');

  /**
   * Extract the filename from the URI, with special handling for jar files. The return value is
   * normalized to always use '/' to separate elements of the path and to always have a leading '/'.
   */
  @Nullable
  public static String getFileNameFromUri(URI uri) {
    if (!uri.getScheme().equals("jar")) {
      return uri.getPath();
    }

    try {
      String jarEntryFileName = ((JarURLConnection) uri.toURL().openConnection()).getEntryName();
      // It's possible (though it violates the zip file spec) for paths to zip file entries to use
      // '\' as the separator. Normalize to use '/'.
      jarEntryFileName = BACKSLASH_MATCHER.replaceFrom(jarEntryFileName, '/');
      if (!jarEntryFileName.startsWith("/")) {
        jarEntryFileName = "/" + jarEntryFileName;
      }
      return jarEntryFileName;
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Given a Type ({@code base}), find the method named {@code name}, with the appropriate {@code
   * argTypes} and {@code tyargTypes} and return its MethodSymbol.
   *
   * <p>Ex:
   *
   * <pre>{@code
   * .....
   * class A {}
   * class B {
   *   public int hashCode() { return 42; }
   * }
   * .....
   *
   * MethodSymbol meth =  ASTHelpers.resolveExistingMethod(
   *    state,
   *    symbol,
   *    state.getName("hashCode"),
   *    ImmutableList.<Type>of(),
   *    ImmutableList.<Type>of());
   * }</pre>
   *
   * {@code meth} could be different MethodSymbol's depending on whether {@code symbol} represented
   * {@code B} or {@code A}. (B's hashCode method or Object#hashCode).
   *
   * @return a MethodSymbol representing the method symbol resolved from the context of this type,
   *     or {@code null} if the method could not be resolved.
   */
  @Nullable
  public static MethodSymbol resolveExistingMethod(
      VisitorState state,
      TypeSymbol base,
      Name name,
      Iterable<Type> argTypes,
      Iterable<Type> tyargTypes) {
    Resolve resolve = Resolve.instance(state.context);
    Enter enter = Enter.instance(state.context);
    Log log = Log.instance(state.context);
    DeferredDiagnosticHandler handler = new DeferredDiagnosticHandler(log);
    try {
      return resolve.resolveInternalMethod(
          /*pos*/ null,
          enter.getEnv(base),
          base.type,
          name,
          com.sun.tools.javac.util.List.from(argTypes),
          com.sun.tools.javac.util.List.from(tyargTypes));
    } catch (FatalError e) {
      // the method could not be resolved
      return null;
    } finally {
      log.popDiagnosticHandler(handler);
    }
  }

  /**
   * Returns the value of the {@code @Generated} annotation on enclosing classes, if present.
   *
   * <p>Although {@code @Generated} can be applied to non-class program elements, there are no known
   * cases of that happening, so it isn't supported here.
   */
  public static ImmutableSet<String> getGeneratedBy(VisitorState state) {
    return Streams.stream(state.getPath())
        .filter(ClassTree.class::isInstance)
        .flatMap(enclosing -> getGeneratedBy(getSymbol(enclosing), state).stream())
        .collect(toImmutableSet());
  }

  /**
   * Returns the values of the given symbol's {@code javax.annotation.Generated} or {@code
   * javax.annotation.processing.Generated} annotation, if present.
   */
  public static ImmutableSet<String> getGeneratedBy(Symbol symbol, VisitorState state) {
    checkNotNull(symbol);
    Optional<Compound> c =
        Stream.of("javax.annotation.Generated", "javax.annotation.processing.Generated")
            .map(state::getSymbolFromString)
            .filter(a -> a != null)
            .map(symbol::attribute)
            .filter(a -> a != null)
            .findFirst();
    if (!c.isPresent()) {
      return ImmutableSet.of();
    }
    Optional<Attribute> values =
        c.get().getElementValues().entrySet().stream()
            .filter(e -> e.getKey().getSimpleName().contentEquals("value"))
            .map(e -> e.getValue())
            .findAny();
    if (!values.isPresent()) {
      return ImmutableSet.of();
    }
    return MoreAnnotations.asStrings((AnnotationValue) values.get()).collect(toImmutableSet());
  }

  public static boolean isSuper(Tree tree) {
    switch (tree.getKind()) {
      case IDENTIFIER:
        return ((IdentifierTree) tree).getName().contentEquals("super");
      case MEMBER_SELECT:
        return ((MemberSelectTree) tree).getIdentifier().contentEquals("super");
      default:
        return false;
    }
  }

  /** An expression's target type, see {@link #targetType}. */
  @AutoValue
  public abstract static class TargetType {
    public abstract Type type();

    public abstract TreePath path();

    static TargetType create(Type type, TreePath path) {
      return new AutoValue_ASTHelpers_TargetType(type, path);
    }
  }

  /**
   * Implementation of unary numeric promotion rules.
   *
   * <p><a href="https://docs.oracle.com/javase/specs/jls/se9/html/jls-5.html#jls-5.6.1">JLS
   * §5.6.1</a>
   */
  @Nullable
  private static Type unaryNumericPromotion(Type type, VisitorState state) {
    Type unboxed = unboxAndEnsureNumeric(type, state);
    switch (unboxed.getTag()) {
      case BYTE:
      case SHORT:
      case CHAR:
        return state.getSymtab().intType;
      case INT:
      case LONG:
      case FLOAT:
      case DOUBLE:
        return unboxed;
      default:
        throw new AssertionError("Should not reach here: " + type);
    }
  }

  /**
   * Implementation of binary numeric promotion rules.
   *
   * <p><a href="https://docs.oracle.com/javase/specs/jls/se9/html/jls-5.html#jls-5.6.2">JLS
   * §5.6.2</a>
   */
  @Nullable
  private static Type binaryNumericPromotion(Type leftType, Type rightType, VisitorState state) {
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

  /**
   * Returns the target type of the tree at the given {@link VisitorState}'s path, or else {@code
   * null}.
   *
   * <p>For example, the target type of an assignment expression is the variable's type, and the
   * target type of a return statement is the enclosing method's type.
   */
  @Nullable
  public static TargetType targetType(VisitorState state) {
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
      return null;
    }
    return TargetType.create(type, parent);
  }

  private static boolean canHaveTargetType(Tree tree) {
    // Anything that isn't an expression can't have a target type.
    if (!(tree instanceof ExpressionTree)) {
      return false;
    }
    switch (tree.getKind()) {
      case IDENTIFIER:
      case MEMBER_SELECT:
        if (!(ASTHelpers.getSymbol(tree) instanceof VarSymbol)) {
          // If we're selecting other than a member (e.g. a type or a method) then this doesn't
          // have a target type.
          return false;
        }
        break;
      case PRIMITIVE_TYPE:
      case ARRAY_TYPE:
      case PARAMETERIZED_TYPE:
      case EXTENDS_WILDCARD:
      case SUPER_WILDCARD:
      case UNBOUNDED_WILDCARD:
      case ANNOTATED_TYPE:
      case INTERSECTION_TYPE:
      case TYPE_ANNOTATION:
        // These are all things that only appear in type uses, so they can't have a target type.
        return false;
      case ANNOTATION:
        // Annotations can only appear on elements which don't have target types.
        return false;
      default:
        // Continue.
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

    @Nullable
    @Override
    public Type visitArrayAccess(ArrayAccessTree node, Void unused) {
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

    @Nullable
    @Override
    public Type visitAssignment(AssignmentTree tree, Void unused) {
      return getType(tree.getVariable());
    }

    @Override
    public Type visitAnnotation(AnnotationTree tree, Void unused) {
      return null;
    }

    @Override
    public Type visitCase(CaseTree tree, Void unused) {
      Tree t = parent.getParentPath().getLeaf();
      // JDK 12+, t can be SwitchExpressionTree
      if (t instanceof SwitchTree) {
        SwitchTree switchTree = (SwitchTree) t;
        return getType(switchTree.getExpression());
      }
      // TODO(b/176098078): When the ErrorProne project switches to JDK 12, we should check
      // for SwitchExpressionTree.
      return null;
    }

    @Override
    public Type visitClass(ClassTree node, Void unused) {
      return null;
    }

    @Nullable
    @Override
    public Type visitCompoundAssignment(CompoundAssignmentTree tree, Void unused) {
      Type variableType = getType(tree.getVariable());
      Type expressionType = getType(tree.getExpression());
      Types types = state.getTypes();
      switch (tree.getKind()) {
        case LEFT_SHIFT_ASSIGNMENT:
        case RIGHT_SHIFT_ASSIGNMENT:
        case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
          // Shift operators perform *unary* numeric promotion on the operands, separately.
          if (tree.getExpression().equals(current)) {
            return unaryNumericPromotion(expressionType, state);
          }
          break;
        case PLUS_ASSIGNMENT:
          Type stringType = state.getSymtab().stringType;
          if (types.isSameType(stringType, variableType)) {
            return stringType;
          }
          break;
        default:
          // Fall though.
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
      Type variableType = ASTHelpers.getType(node.getVariable());
      if (state.getTypes().isArray(ASTHelpers.getType(node.getExpression()))) {
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
    public Type visitParenthesized(ParenthesizedTree node, Void unused) {
      return visit(node.getExpression(), unused);
    }

    @Nullable
    @Override
    public Type visitReturn(ReturnTree tree, Void unused) {
      for (TreePath path = parent; path != null; path = path.getParentPath()) {
        Tree enclosing = path.getLeaf();
        switch (enclosing.getKind()) {
          case METHOD:
            return getType(((MethodTree) enclosing).getReturnType());
          case LAMBDA_EXPRESSION:
            return visitLambdaExpression((LambdaExpressionTree) enclosing, null);
          default: // fall out
        }
      }
      throw new AssertionError("return not enclosed by method or lambda");
    }

    @Override
    public Type visitSynchronized(SynchronizedTree node, Void unused) {
      // The null occurs if you've asked for the type of the parentheses around the expression.
      return Objects.equals(current, node.getExpression()) ? state.getSymtab().objectType : null;
    }

    @Override
    public Type visitThrow(ThrowTree node, Void unused) {
      return ASTHelpers.getType(current);
    }

    @Override
    public Type visitTypeCast(TypeCastTree node, Void unused) {
      return getType(node.getType());
    }

    @Nullable
    @Override
    public Type visitVariable(VariableTree tree, Void unused) {
      return getType(tree.getType());
    }

    @Nullable
    @Override
    public Type visitUnary(UnaryTree tree, Void unused) {
      return getType(tree);
    }

    @Nullable
    @Override
    public Type visitBinary(BinaryTree tree, Void unused) {
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

    @Nullable
    private Type handleEqualityOperator(BinaryTree tree, Type leftType, Type rightType) {
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

    @Nullable
    @Override
    public Type visitConditionalExpression(ConditionalExpressionTree tree, Void unused) {
      return tree.getCondition().equals(current) ? state.getSymtab().booleanType : getType(tree);
    }

    @Override
    public Type visitNewClass(NewClassTree tree, Void unused) {
      if (Objects.equals(current, tree.getEnclosingExpression())) {
        return ((ClassSymbol) ASTHelpers.getSymbol(tree.getIdentifier())).owner.type;
      }
      return visitMethodInvocationOrNewClass(
          tree.getArguments(), ASTHelpers.getSymbol(tree), ((JCNewClass) tree).constructorType);
    }

    @Override
    public Type visitMethodInvocation(MethodInvocationTree tree, Void unused) {
      return visitMethodInvocationOrNewClass(
          tree.getArguments(), ASTHelpers.getSymbol(tree), ((JCMethodInvocation) tree).meth.type);
    }

    @Nullable
    private Type visitMethodInvocationOrNewClass(
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

    @Nullable
    @Override
    public Type visitSwitch(SwitchTree node, Void unused) {
      if (current == node.getExpression()) {
        return state.getTypes().unboxedTypeOrType(getType(current));
      } else {
        return null;
      }
    }

    @Nullable
    @Override
    public Type visitNewArray(NewArrayTree node, Void unused) {
      if (Objects.equals(node.getType(), current)) {
        return null;
      }
      if (node.getDimensions().contains(current)) {
        return state.getSymtab().intType;
      }
      if (node.getInitializers() != null && node.getInitializers().contains(current)) {
        return state.getTypes().elemtype(ASTHelpers.getType(node));
      }
      return null;
    }

    @Nullable
    @Override
    public Type visitMemberSelect(MemberSelectTree node, Void unused) {
      if (current.equals(node.getExpression())) {
        return ASTHelpers.getType(node.getExpression());
      }
      return null;
    }

    @Override
    public Type visitMemberReference(MemberReferenceTree node, Void unused) {
      return state.getTypes().findDescriptorType(getType(node)).getReturnType();
    }

    @Nullable
    private Type getConditionType(Tree condition) {
      if (condition != null && condition.equals(current)) {
        return state.getSymtab().booleanType;
      }
      return null;
    }
  }

  /**
   * Returns declaration annotations of the given symbol, as well as 'top-level' type annotations,
   * including :
   *
   * <ul>
   *   <li>Type annotations of the return type of a method.
   *   <li>Type annotations on the type of a formal parameter or field.
   * </ul>
   *
   * <p>One might expect this to be equivalent to information returned by {@link
   * Type#getAnnotationMirrors}, but javac doesn't associate type annotation information with types
   * for symbols completed from class files, so that approach doesn't work across compilation
   * boundaries.
   */
  public static Stream<Attribute.Compound> getDeclarationAndTypeAttributes(Symbol sym) {
    return MoreAnnotations.getDeclarationAndTypeAttributes(sym);
  }

  /**
   * Return a mirror of this annotation.
   *
   * @return an {@code AnnotationMirror} for the annotation represented by {@code annotationTree}.
   */
  public static AnnotationMirror getAnnotationMirror(AnnotationTree annotationTree) {
    return ((JCAnnotation) annotationTree).attribute;
  }

  /** Returns whether the given {@code tree} contains any comments in its source. */
  public static boolean containsComments(Tree tree, VisitorState state) {
    return state.getOffsetTokensForNode(tree).stream().anyMatch(t -> !t.comments().isEmpty());
  }

  /**
   * Returns the outermost enclosing owning class, or {@code null}. Doesn't crash on symbols that
   * aren't containing in a package, unlike {@link Symbol#outermostClass} (see b/123431414).
   */
  // TODO(b/123431414): fix javac and use Symbol.outermostClass insteads
  @Nullable
  public static ClassSymbol outermostClass(Symbol symbol) {
    ClassSymbol curr = symbol.enclClass();
    while (curr != null && curr.owner != null) {
      ClassSymbol encl = curr.owner.enclClass();
      if (encl == null) {
        break;
      }
      curr = encl;
    }
    return curr;
  }

  /** Returns whether {@code symbol} is final or effectively final. */
  public static boolean isConsideredFinal(Symbol symbol) {
    return (symbol.flags() & (Flags.FINAL | Flags.EFFECTIVELY_FINAL)) != 0;
  }

  /** Returns the exceptions thrown by {@code tree}. */
  public static ImmutableSet<Type> getThrownExceptions(Tree tree, VisitorState state) {
    ScanThrownTypes scanner = new ScanThrownTypes(state);
    scanner.scan(tree, null);
    return ImmutableSet.copyOf(scanner.getThrownTypes());
  }

  /** Scanner for determining what types are thrown by a tree. */
  public static final class ScanThrownTypes extends TreeScanner<Void, Void> {
    boolean inResources = false;
    ArrayDeque<Set<Type>> thrownTypes = new ArrayDeque<>();
    SetMultimap<VarSymbol, Type> thrownTypesByVariable = HashMultimap.create();

    private final VisitorState state;
    private final Types types;

    public ScanThrownTypes(VisitorState state) {
      this.state = state;
      this.types = state.getTypes();
      thrownTypes.push(new HashSet<>());
    }

    public Set<Type> getThrownTypes() {
      return thrownTypes.peek();
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree invocation, Void unused) {
      Type type = getType(invocation.getMethodSelect());
      if (type != null) {
        getThrownTypes().addAll(type.getThrownTypes());
      }
      return super.visitMethodInvocation(invocation, null);
    }

    @Override
    public Void visitTry(TryTree tree, Void unused) {
      thrownTypes.push(new HashSet<>());
      scanResources(tree);
      scan(tree.getBlock(), null);
      // Make two passes over the `catch` blocks: once to remove caught exceptions, and once to
      // add thrown ones. We can't do this in one step as an exception could be caught but later
      // thrown.
      for (CatchTree catchTree : tree.getCatches()) {
        Type type = getType(catchTree.getParameter());

        Set<Type> matchingTypes = new HashSet<>();
        for (Type unionMember : extractTypes(type)) {
          for (Type thrownType : getThrownTypes()) {
            if (types.isSubtype(thrownType, unionMember)) {
              matchingTypes.add(thrownType);
            }
          }
        }
        getThrownTypes().removeAll(matchingTypes);
        thrownTypesByVariable.putAll(getSymbol(catchTree.getParameter()), matchingTypes);
      }
      for (CatchTree catchTree : tree.getCatches()) {
        scan(catchTree.getBlock(), null);
      }
      scan(tree.getFinallyBlock(), null);
      Set<Type> fromBlock = thrownTypes.pop();
      getThrownTypes().addAll(fromBlock);
      return null;
    }

    public void scanResources(TryTree tree) {
      inResources = true;
      scan(tree.getResources(), null);
      inResources = false;
    }

    @Override
    public Void visitThrow(ThrowTree tree, Void unused) {
      if (tree.getExpression() instanceof IdentifierTree) {
        Symbol symbol = getSymbol(tree.getExpression());
        if (thrownTypesByVariable.containsKey(symbol)) {
          getThrownTypes().addAll(thrownTypesByVariable.get((VarSymbol) symbol));
          return super.visitThrow(tree, null);
        }
      }
      getThrownTypes().addAll(extractTypes(getType(tree.getExpression())));
      return super.visitThrow(tree, null);
    }

    @Override
    public Void visitNewClass(NewClassTree tree, Void unused) {
      MethodSymbol symbol = getSymbol(tree);
      if (symbol != null) {
        getThrownTypes().addAll(symbol.getThrownTypes());
      }
      return super.visitNewClass(tree, null);
    }

    @Override
    public Void visitVariable(VariableTree tree, Void unused) {
      if (inResources) {
        Symbol symbol = getSymbol(tree.getType());
        if (symbol instanceof ClassSymbol) {
          getCloseMethod((ClassSymbol) symbol, state)
              .ifPresent(methodSymbol -> getThrownTypes().addAll(methodSymbol.getThrownTypes()));
        }
      }
      return super.visitVariable(tree, null);
    }

    // We don't need to account for anything thrown by declarations.
    @Override
    public Void visitLambdaExpression(LambdaExpressionTree tree, Void unused) {
      return null;
    }

    @Override
    public Void visitClass(ClassTree tree, Void unused) {
      return null;
    }

    @Override
    public Void visitMethod(MethodTree tree, Void unused) {
      return null;
    }

    private static final Supplier<Type> AUTOCLOSEABLE =
        Suppliers.typeFromString("java.lang.AutoCloseable");
    private static final Supplier<Name> CLOSE =
        VisitorState.memoize(state -> state.getName("close"));

    private static Optional<MethodSymbol> getCloseMethod(ClassSymbol symbol, VisitorState state) {
      Types types = state.getTypes();
      if (!types.isAssignable(symbol.type, AUTOCLOSEABLE.get(state))) {
        return Optional.empty();
      }
      Type voidType = state.getSymtab().voidType;
      Optional<MethodSymbol> declaredCloseMethod =
          ASTHelpers.matchingMethods(
                  CLOSE.get(state),
                  s ->
                      !s.isConstructor()
                          && s.params.isEmpty()
                          && types.isSameType(s.getReturnType(), voidType),
                  symbol.type,
                  types)
              .findFirst();
      verify(
          declaredCloseMethod.isPresent(),
          "%s implements AutoCloseable but no method named close() exists, even inherited",
          symbol);

      return declaredCloseMethod;
    }

    private static ImmutableList<Type> extractTypes(@Nullable Type type) {
      if (type == null) {
        return ImmutableList.of();
      }
      if (type.isUnion()) {
        UnionClassType unionType = (UnionClassType) type;
        return ImmutableList.copyOf(unionType.getAlternativeTypes());
      }
      return ImmutableList.of(type);
    }
  }

  /** Returns the start position of the node. */
  public static int getStartPosition(Tree tree) {
    return ((JCTree) tree).getStartPosition();
  }

  /** Returns a no arg private constructor for the {@link ClassTree}. */
  public static String createPrivateConstructor(ClassTree classTree) {
    return "private " + classTree.getSimpleName() + "() {}";
  }

  private static final Matcher<Tree> IS_BUGCHECKER =
      isSubtypeOf("com.google.errorprone.bugpatterns.BugChecker");

  /** Returns {@code true} if the code is in a BugChecker class. */
  public static boolean isBugCheckerCode(VisitorState state) {
    for (Tree ancestor : state.getPath()) {
      if (IS_BUGCHECKER.matches(ancestor, state)) {
        return true;
      }
    }
    return false;
  }
}
