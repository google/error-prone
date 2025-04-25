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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.MoreCollectors.onlyElement;
import static com.google.common.collect.Streams.stream;
import static com.google.common.collect.Streams.zip;
import static com.google.errorprone.VisitorState.memoize;
import static com.google.errorprone.matchers.JUnitMatchers.JUNIT4_RUN_WITH_ANNOTATION;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.sun.tools.javac.code.Scope.LookupKind.NON_RECURSIVE;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.CharMatcher;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.InlineMe;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnalysis;
import com.google.errorprone.matchers.JUnitMatchers;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.TestNgMatchers;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.suppliers.Suppliers;
import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Attribute.Compound;
import com.sun.tools.javac.code.Attribute.TypeCompound;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
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
import com.sun.tools.javac.parser.JavaTokenizer;
import com.sun.tools.javac.parser.ScannerFactory;
import com.sun.tools.javac.parser.Tokens.Token;
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
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.FatalError;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Position;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URI;
import java.nio.CharBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import org.jspecify.annotations.Nullable;

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
    if ((!(expr1 instanceof IdentifierTree) && !(expr1 instanceof MemberSelectTree))
        || (!(expr2 instanceof IdentifierTree) && !(expr2 instanceof MemberSelectTree))) {
      return false;
    }

    Symbol sym1 = getSymbol(expr1);
    Symbol sym2 = getSymbol(expr2);
    if (sym1 == null) {
      throw new IllegalStateException("Couldn't get symbol for " + expr1);
    } else if (sym2 == null) {
      throw new IllegalStateException("Couldn't get symbol for " + expr2);
    }

    if (expr1 instanceof IdentifierTree && expr2 instanceof IdentifierTree) {
      // foo == foo?
      return sym1.equals(sym2);
    } else if (expr1 instanceof MemberSelectTree && expr2 instanceof MemberSelectTree) {
      // foo.baz.bar == foo.baz.bar?
      return sym1.equals(sym2)
          && sameVariable(((JCFieldAccess) expr1).selected, ((JCFieldAccess) expr2).selected);
    } else {
      // this.foo == foo?
      ExpressionTree selected;
      if (expr1 instanceof IdentifierTree) {
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
  public static @Nullable Symbol getDeclaredSymbol(Tree tree) {
    if (tree instanceof PackageTree packageTree) {
      return getSymbol(packageTree);
    }
    if (tree instanceof TypeParameterTree) {
      Type type = ((JCTypeParameter) tree).type;
      return type == null ? null : type.tsym;
    }
    if (tree instanceof ClassTree classTree) {
      return getSymbol(classTree);
    }
    if (tree instanceof MethodTree methodTree) {
      return getSymbol(methodTree);
    }
    if (tree instanceof VariableTree variableTree) {
      return getSymbol(variableTree);
    }
    return null;
  }

  /**
   * Gets the symbol for a tree. Returns null if this tree does not have a symbol because it is of
   * the wrong type, if {@code tree} is null, or if the symbol cannot be found due to a compilation
   * error.
   */
  public static @Nullable Symbol getSymbol(Tree tree) {
    if (tree instanceof AnnotationTree annotationTree) {
      return getSymbol(annotationTree.getAnnotationType());
    }
    if (tree instanceof JCFieldAccess jcFieldAccess) {
      return jcFieldAccess.sym;
    }
    if (tree instanceof JCIdent jcIdent) {
      // You might reasonably expect that IdentifierTrees always have a non-null symbol, but a few
      // cases don't, including module names and identifiers resolved from Javadoc (sometimes).
      return jcIdent.sym;
    }
    if (tree instanceof JCMethodInvocation jcMethodInvocation) {
      return getSymbol(jcMethodInvocation);
    }
    if (tree instanceof JCNewClass jcNewClass) {
      return getSymbol(jcNewClass);
    }
    if (tree instanceof MemberReferenceTree memberReferenceTree) {
      return getSymbol(memberReferenceTree);
    }
    if (tree instanceof JCAnnotatedType jcAnnotatedType) {
      return getSymbol(jcAnnotatedType.underlyingType);
    }
    if (tree instanceof ParameterizedTypeTree parameterizedTypeTree) {
      return getSymbol(parameterizedTypeTree.getType());
    }
    if (tree instanceof ClassTree classTree) {
      return getSymbol(classTree);
    }

    return getDeclaredSymbol(tree);
  }

  /** Gets the symbol for a class. */
  public static ClassSymbol getSymbol(ClassTree tree) {
    return checkNotNull(((JCClassDecl) tree).sym, "%s had a null ClassSymbol", tree);
  }

  /** Gets the symbol for a package. */
  public static PackageSymbol getSymbol(PackageTree tree) {
    return checkNotNull(((JCPackageDecl) tree).packge, "%s had a null PackageSymbol", tree);
  }

  /** Gets the symbol for a method. */
  public static MethodSymbol getSymbol(MethodTree tree) {
    return checkNotNull(((JCMethodDecl) tree).sym, "%s had a null MethodSymbol", tree);
  }

  /** Gets the method symbol for a new class. */
  public static MethodSymbol getSymbol(NewClassTree tree) {
    Symbol sym = ((JCNewClass) tree).constructor;
    if (!(sym instanceof MethodSymbol methodSymbol)) {
      // Defensive. Would only occur if there are errors in the AST.
      throw new IllegalArgumentException(tree.toString());
    }
    return methodSymbol;
  }

  /** Gets the symbol for a variable. */
  public static VarSymbol getSymbol(VariableTree tree) {
    return checkNotNull(((JCVariableDecl) tree).sym, "%s had a null VariableTree", tree);
  }

  /** Gets the symbol for a method invocation. */
  public static MethodSymbol getSymbol(MethodInvocationTree tree) {
    Symbol sym = ASTHelpers.getSymbol(tree.getMethodSelect());
    if (!(sym instanceof MethodSymbol)) {
      // Defensive. Would only occur if there are errors in the AST.
      throw new IllegalArgumentException(tree.toString());
    }
    return (MethodSymbol) sym.baseSymbol();
  }

  /** Gets the symbol for a member reference. */
  public static MethodSymbol getSymbol(MemberReferenceTree tree) {
    Symbol sym = ((JCMemberReference) tree).sym;
    if (!(sym instanceof MethodSymbol)) {
      // Defensive. Would only occur if there are errors in the AST.
      throw new IllegalArgumentException(tree.toString());
    }
    return (MethodSymbol) sym.baseSymbol();
  }

  /**
   * Returns whether this symbol is safe to remove. That is, if it cannot be accessed from outside
   * its own compilation unit.
   *
   * <p>For variables this just means that one of the enclosing elements is private; for methods, it
   * also means that this symbol is not an override.
   */
  public static boolean canBeRemoved(Symbol symbol, VisitorState state) {
    if (symbol instanceof MethodSymbol methodSymbol
        && !findSuperMethods(methodSymbol, state.getTypes()).isEmpty()) {
      return false;
    }
    return isEffectivelyPrivate(symbol);
  }

  /** See {@link #canBeRemoved(Symbol, VisitorState)}. */
  public static boolean canBeRemoved(VarSymbol symbol) {
    return isEffectivelyPrivate(symbol);
  }

  /** See {@link #canBeRemoved(Symbol, VisitorState)}. */
  public static boolean canBeRemoved(ClassSymbol symbol) {
    return isEffectivelyPrivate(symbol);
  }

  /** Returns whether this symbol or any of its owners are private. */
  public static boolean isEffectivelyPrivate(Symbol symbol) {
    return enclosingElements(symbol)
        .anyMatch(
            s -> {
              if (s.isPrivate()) {
                return true;
              }
              if (s instanceof ClassSymbol) {
                // Anonymous classes. (Note: packages can be anonymous too.)
                if (s.isAnonymous()) {
                  return true;
                }
                // Local classes have a method as an owner.
                if (s.owner instanceof MethodSymbol) {
                  return true;
                }
              }
              return false;
            });
  }

  /** Checks whether an expression requires parentheses. */
  public static boolean requiresParentheses(ExpressionTree expression, VisitorState state) {
    switch (expression.getKind()) {
      case IDENTIFIER,
          MEMBER_SELECT,
          METHOD_INVOCATION,
          ARRAY_ACCESS,
          PARENTHESIZED,
          NEW_CLASS,
          MEMBER_REFERENCE -> {
        return false;
      }
      case LAMBDA_EXPRESSION -> {
        // Parenthesizing e.g. `x -> (y -> z)` is unnecessary but helpful
        Tree parent = state.getPath().getParentPath().getLeaf();
        return parent instanceof LambdaExpressionTree lambdaExpressionTree
            && stripParentheses(lambdaExpressionTree.getBody()).equals(expression);
      }
      default -> {
        // continue below
      }
    }
    if (expression instanceof LiteralTree) {
      if (!isSameType(getType(expression), state.getSymtab().stringType, state)) {
        return false;
      }
      // TODO(b/112139121): work around for javac's too-early constant string folding
      return state.getOffsetTokensForNode(expression).stream()
          .anyMatch(t -> t.kind() == TokenKind.PLUS);
    }
    if (expression instanceof UnaryTree unaryTree) {
      Tree parent = state.getPath().getParentPath().getLeaf();
      if (parent instanceof TypeCastTree castTree
          && !castTree.getType().getKind().equals(Kind.PRIMITIVE_TYPE)) {
        // unary plus and minus require parens when used with non-primitive casts
        // see https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.16
        switch (unaryTree.getKind()) {
          case UNARY_PLUS, UNARY_MINUS -> {
            return true;
          }
          default -> {}
        }
      }
      if (!(parent instanceof MemberSelectTree memberSelectTree)) {
        return false;
      }
      // eg. (i++).toString();
      return stripParentheses(memberSelectTree.getExpression()).equals(expression);
    }
    return true;
  }

  /** Removes any enclosing parentheses from the tree. */
  public static Tree stripParentheses(Tree tree) {
    return tree instanceof ExpressionTree expressionTree ? stripParentheses(expressionTree) : tree;
  }

  /** Given an ExpressionTree, removes any enclosing parentheses. */
  public static ExpressionTree stripParentheses(ExpressionTree tree) {
    while (tree instanceof ParenthesizedTree pt) {
      tree = pt.getExpression();
    }
    return tree;
  }

  /**
   * Given a TreePath, finds the first enclosing node of the given type and returns the path from
   * the enclosing node to the top-level {@code CompilationUnitTree}.
   */
  public static <T> @Nullable TreePath findPathFromEnclosingNodeToTopLevel(
      TreePath path, Class<T> klass) {
    if (path != null) {
      do {
        path = path.getParentPath();
      } while (path != null && !klass.isInstance(path.getLeaf()));
    }
    return path;
  }

  /**
   * Returns a stream of the owner hierarchy starting from {@code sym}, as described by {@link
   * Symbol#owner}. Returns {@code sym} itself first, followed by its owners, closest first, up to
   * the owning package and possibly module.
   */
  public static Stream<Symbol> enclosingElements(Symbol sym) {
    return Stream.iterate(sym, Symbol::getEnclosingElement).takeWhile(s -> s != null);
  }

  /**
   * Given a TreePath, walks up the tree until it finds a node of the given type. Returns null if no
   * such node is found.
   */
  public static <T> @Nullable T findEnclosingNode(TreePath path, Class<T> klass) {
    path = findPathFromEnclosingNodeToTopLevel(path, klass);
    return (path == null) ? null : klass.cast(path.getLeaf());
  }

  /** Finds the enclosing {@link MethodTree}. Returns {@code null} if no such node found. */
  public static @Nullable MethodTree findEnclosingMethod(VisitorState state) {
    for (Tree parent : state.getPath()) {
      switch (parent.getKind()) {
        case METHOD -> {
          return (MethodTree) parent;
        }
        case CLASS, LAMBDA_EXPRESSION -> {
          return null;
        }
        default -> {}
      }
    }
    return null;
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
  public static @Nullable ExpressionTree getRootAssignable(
      MethodInvocationTree methodInvocationTree) {
    if (!(methodInvocationTree instanceof JCMethodInvocation jCMethodInvocation)) {
      throw new IllegalArgumentException(
          "Expected type to be JCMethodInvocation, but was " + methodInvocationTree.getClass());
    }

    // Check for bare method call, e.g. intern().
    if (jCMethodInvocation.getMethodSelect() instanceof JCIdent) {
      return null;
    }

    // Unwrap the field accesses until you get to an identifier.
    ExpressionTree expr = methodInvocationTree;
    while (expr instanceof JCMethodInvocation) {
      expr = ((JCMethodInvocation) expr).getMethodSelect();
      if (expr instanceof JCFieldAccess jCFieldAccess) {
        expr = jCFieldAccess.getExpression();
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
    if (expressionTree instanceof JCFieldAccess methodCall) {
      return methodCall.type.getReturnType();
    } else if (expressionTree instanceof JCIdent methodCall) {
      return methodCall.type.getReturnType();
    } else if (expressionTree instanceof JCMethodInvocation jCMethodInvocation) {
      return getReturnType(jCMethodInvocation.getMethodSelect());
    } else if (expressionTree instanceof JCMemberReference jCMemberReference) {
      return jCMemberReference.sym.type.getReturnType();
    }
    throw new IllegalArgumentException("Expected a JCFieldAccess or JCIdent");
  }

  /**
   * Returns the type that this expression tree will evaluate to. If it's a literal, an identifier,
   * or a member select this is the actual type, if it's a method invocation then it's the return
   * type of the method (after instantiating generic types), if it's a constructor then it's the
   * type of the returned class.
   *
   * <p>TODO(andrewrice) consider replacing {@code getReturnType} with this method
   *
   * @param expressionTree the tree to evaluate
   * @return the result type of this tree or null if unable to resolve it
   */
  public static @Nullable Type getResultType(ExpressionTree expressionTree) {
    Type type = ASTHelpers.getType(expressionTree);
    return type == null ? null : (type.getReturnType() == null ? type : type.getReturnType());
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
    if (expressionTree instanceof JCFieldAccess methodSelectFieldAccess) {
      return methodSelectFieldAccess.selected.type;
    } else if (expressionTree instanceof JCIdent methodCall) {
      return methodCall.sym.owner.type;
    } else if (expressionTree instanceof JCMethodInvocation jCMethodInvocation) {
      return getReceiverType(jCMethodInvocation.getMethodSelect());
    } else if (expressionTree instanceof JCMemberReference jCMemberReference) {
      return jCMemberReference.getQualifierExpression().type;
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
  public static @Nullable ExpressionTree getReceiver(ExpressionTree expressionTree) {
    if (expressionTree instanceof MethodInvocationTree methodInvocationTree) {
      ExpressionTree methodSelect = methodInvocationTree.getMethodSelect();
      if (methodSelect instanceof IdentifierTree) {
        return null;
      }
      return getReceiver(methodSelect);
    } else if (expressionTree instanceof MemberSelectTree memberSelectTree) {
      return memberSelectTree.getExpression();
    } else if (expressionTree instanceof MemberReferenceTree memberReferenceTree) {
      return memberReferenceTree.getQualifierExpression();
    } else {
      throw new IllegalStateException(
          String.format(
              "Expected expression '%s' to be a method invocation or field access, but was %s",
              expressionTree, expressionTree.getKind()));
    }
  }

  /**
   * Returns a {@link Stream} of {@link ExpressionTree}s resulting from calling {@link
   * #getReceiver(ExpressionTree)} repeatedly until no receiver exists.
   *
   * <p>For example, give {@code foo().bar().baz()}, returns a stream of {@code [foo().bar(),
   * foo()]}.
   *
   * <p>This can be more convenient than manually traversing up a tree, as it handles the
   * termination condition automatically. Typical uses cases would include traversing fluent call
   * chains.
   */
  public static Stream<ExpressionTree> streamReceivers(ExpressionTree tree) {
    return stream(
        new AbstractIterator<ExpressionTree>() {
          private ExpressionTree current = tree;

          @Override
          protected ExpressionTree computeNext() {
            if (current instanceof MethodInvocationTree
                || current instanceof MemberSelectTree
                || current instanceof MemberReferenceTree) {
              current = getReceiver(current);
              return current == null ? endOfData() : current;
            }
            return endOfData();
          }
        });
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
  public static @Nullable List<ExpressionTree> matchBinaryTree(
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
  public static @Nullable MethodTree findMethod(MethodSymbol symbol, VisitorState state) {
    return JavacTrees.instance(state.context).getTree(symbol);
  }

  /**
   * Returns the class tree that matches the given symbol within the compilation unit, or null if
   * none was found.
   */
  public static @Nullable ClassTree findClass(ClassSymbol symbol, VisitorState state) {
    return JavacTrees.instance(state.context).getTree(symbol);
  }

  // TODO(ghm): Using a comparison of tsym here appears to be a behaviour change.
  @SuppressWarnings("TypeEquals")
  public static @Nullable MethodSymbol findSuperMethodInType(
      MethodSymbol methodSymbol, Type superType, Types types) {
    if (methodSymbol.isStatic() || superType.equals(methodSymbol.owner.type)) {
      return null;
    }

    Scope scope = superType.tsym.members();
    for (Symbol sym : scope.getSymbolsByName(methodSymbol.name)) {
      if (sym != null
          && !isStatic(sym)
          && ((sym.flags() & Flags.SYNTHETIC) == 0)
          && methodSymbol.overrides(
              sym, (TypeSymbol) methodSymbol.owner, types, /* checkResult= */ true)) {
        return (MethodSymbol) sym;
      }
    }
    return null;
  }

  /**
   * Finds supermethods of {@code methodSymbol}, not including {@code methodSymbol} itself, and
   * including interfaces.
   */
  public static Set<MethodSymbol> findSuperMethods(MethodSymbol methodSymbol, Types types) {
    return findSuperMethods(methodSymbol, types, /* skipInterfaces= */ false)
        .collect(toCollection(LinkedHashSet::new));
  }

  /** See {@link #findSuperMethods(MethodSymbol, Types)}. */
  public static Stream<MethodSymbol> streamSuperMethods(MethodSymbol methodSymbol, Types types) {
    return findSuperMethods(methodSymbol, types, /* skipInterfaces= */ false);
  }

  static Stream<MethodSymbol> findSuperMethods(
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
   * Finds (if it exists) first (in the class hierarchy) non-interface super method of given {@code
   * method}.
   */
  public static Optional<MethodSymbol> findSuperMethod(MethodSymbol methodSymbol, Types types) {
    return findSuperMethods(methodSymbol, types, /* skipInterfaces= */ true).findFirst();
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
      Name name, Predicate<MethodSymbol> predicate, Type startClass, Types types) {
    Predicate<Symbol> matchesMethodPredicate =
        sym -> sym instanceof MethodSymbol methodSymbol && predicate.test(methodSymbol);

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
              return stream(
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
  public static ImmutableSet<MethodSymbol> findMatchingMethods(
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
   * Flag for record types, canonical record constructors and type members that are part of a
   * record's state vector. Can be replaced by {@code com.sun.tools.javac.code.Flags.RECORD} once
   * the minimum JDK version is 14.
   */
  private static final long RECORD_FLAG = 1L << 61;

  /**
   * Returns whether the given {@link Symbol} is a record, a record's canonical constructor or a
   * member that is part of a record's state vector.
   *
   * <p>Health warning: some things are flagged within a compilation, but won't be flagged across
   * compilation boundaries, like canonical constructors.
   */
  public static boolean isRecord(Symbol symbol) {
    return (symbol.flags() & RECORD_FLAG) == RECORD_FLAG;
  }

  /** Finds the canonical constructor on a record. */
  public static MethodSymbol canonicalConstructor(ClassSymbol record, VisitorState state) {
    var fieldTypes =
        record.getRecordComponents().stream().map(rc -> rc.type).collect(toImmutableList());
    return stream(record.members().getSymbols(s -> s.getKind() == CONSTRUCTOR))
        .map(c -> (MethodSymbol) c)
        .filter(
            c ->
                c.getParameters().size() == fieldTypes.size()
                    && zip(
                            c.getParameters().stream(),
                            fieldTypes.stream(),
                            (a, b) -> isSameType(a.type, b, state))
                        .allMatch(x -> x))
        .collect(onlyElement());
  }

  /**
   * Determines whether a symbol has an annotation of the given type. This includes annotations
   * inherited from superclasses due to {@code @Inherited}.
   *
   * @param annotationClass the binary class name of the annotation (e.g.
   *     "org.jspecify.annotations.Nullable", or "some.package.OuterClassName$InnerClassName")
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
    if (sym instanceof ClassSymbol cs && isInherited(state, annotationClass)) {
      for (sym = cs.getSuperclass().tsym;
          sym instanceof ClassSymbol cs2;
          sym = cs2.getSuperclass().tsym) {
        if (hasAttribute(sym, annotationName)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Check for the presence of an annotation, considering annotation inheritance.
   *
   * @return true if the symbol is annotated with given type.
   * @deprecated prefer {@link #hasAnnotation(Symbol, String, VisitorState)} to avoid needing a
   *     runtime dependency on the annotation class, and to prevent issues if there is skew between
   *     the definition of the annotation on the runtime and compile-time classpaths
   */
  @InlineMe(
      replacement = "ASTHelpers.hasAnnotation(sym, annotationClass.getName(), state)",
      imports = {"com.google.errorprone.util.ASTHelpers"})
  @Deprecated
  public static boolean hasAnnotation(
      Symbol sym, Class<? extends Annotation> annotationClass, VisitorState state) {
    return hasAnnotation(sym, annotationClass.getName(), state);
  }

  /**
   * Check for the presence of an annotation, considering annotation inheritance.
   *
   * @param annotationClass the binary class name of the annotation (e.g.
   *     "org.jspecify.annotations.Nullable", or "some.package.OuterClassName$InnerClassName")
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
   * @deprecated prefer {@link #hasAnnotation(Symbol, String, VisitorState)} to avoid needing a
   *     runtime dependency on the annotation class, and to prevent issues if there is skew between
   *     the definition of the annotation on the runtime and compile-time classpaths
   */
  @InlineMe(
      replacement = "ASTHelpers.hasAnnotation(tree, annotationClass.getName(), state)",
      imports = {"com.google.errorprone.util.ASTHelpers"})
  @Deprecated
  public static boolean hasAnnotation(
      Tree tree, Class<? extends Annotation> annotationClass, VisitorState state) {
    return hasAnnotation(tree, annotationClass.getName(), state);
  }

  private static final Supplier<Cache<Name, Boolean>> inheritedAnnotationCache =
      VisitorState.memoize(unusedState -> Caffeine.newBuilder().maximumSize(1000).build());

  @SuppressWarnings("ConstantConditions") // IntelliJ worries unboxing our Boolean may throw NPE.
  private static boolean isInherited(VisitorState state, Name annotationName) {

    return inheritedAnnotationCache
        .get(state)
        .get(
            annotationName,
            name -> {
              if (name.equals(NULL_MARKED_NAME.get(state))) {
                /*
                 * We avoid searching for @Inherited on NullMarked not just because we already know
                 * the answer but also because the search would cause issues under --release 8 on
                 * account of NullMarked's use of @Target(MODULE, ...).
                 */
                return false;
              }
              Symbol annotationSym = state.getSymbolFromName(annotationName);
              if (annotationSym == null) {
                return false;
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
   * Check for the presence of an annotation with the given simple name directly on this symbol or
   * its type. (If the given symbol is a method symbol, the type searched for annotations is its
   * return type.)
   *
   * <p>This method looks only a annotations that are directly present. It does <b>not</b> consider
   * annotation inheritance (see JLS 9.6.4.3).
   */
  public static boolean hasDirectAnnotationWithSimpleName(Symbol sym, String simpleName) {
    if (sym instanceof MethodSymbol methodSymbol) {
      return hasDirectAnnotationWithSimpleName(methodSymbol, simpleName);
    }
    if (sym instanceof VarSymbol varSymbol) {
      return hasDirectAnnotationWithSimpleName(varSymbol, simpleName);
    }
    return hasDirectAnnotationWithSimpleName(sym.getAnnotationMirrors().stream(), simpleName);
  }

  public static boolean hasDirectAnnotationWithSimpleName(MethodSymbol sym, String simpleName) {
    return hasDirectAnnotationWithSimpleName(
        Streams.concat(
            sym.getAnnotationMirrors().stream(),
            sym.getReturnType().getAnnotationMirrors().stream()),
        simpleName);
  }

  public static boolean hasDirectAnnotationWithSimpleName(VarSymbol sym, String simpleName) {
    return hasDirectAnnotationWithSimpleName(
        Streams.concat(
            sym.getAnnotationMirrors().stream(), sym.asType().getAnnotationMirrors().stream()),
        simpleName);
  }

  private static boolean hasDirectAnnotationWithSimpleName(
      Stream<? extends AnnotationMirror> annotations, String simpleName) {
    return annotations.anyMatch(
        annotation ->
            annotation.getAnnotationType().asElement().getSimpleName().contentEquals(simpleName));
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
   * Returns true if any of the given tree is a declaration annotated with an annotation with the
   * simple name {@code @UsedReflectively} or {@code @Keep}, or any annotations meta-annotated with
   * an annotation with that simple name.
   *
   * <p>This indicates the annotated element is used (e.g. by reflection, or referenced by generated
   * code) and should not be removed.
   */
  public static boolean shouldKeep(Tree tree) {
    ModifiersTree modifiers = getModifiers(tree);
    if (modifiers == null) {
      return false;
    }
    for (AnnotationTree annotation : modifiers.getAnnotations()) {
      Type annotationType = getType(annotation);
      if (annotationType == null) {
        continue;
      }
      TypeSymbol tsym = annotationType.tsym;
      if (tsym.getSimpleName().contentEquals(USED_REFLECTIVELY)
          || tsym.getSimpleName().contentEquals(KEEP)) {
        return true;
      }
      if (ANNOTATIONS_CONSIDERED_KEEP.contains(tsym.getQualifiedName().toString())) {
        return true;
      }
      if (hasDirectAnnotationWithSimpleName(tsym, USED_REFLECTIVELY)
          || hasDirectAnnotationWithSimpleName(tsym, KEEP)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Additional annotations which can't be annotated with {@code @Keep}, but should be treated as
   * though they are.
   */
  private static final ImmutableSet<String> ANNOTATIONS_CONSIDERED_KEEP =
      ImmutableSet.of(
          "org.apache.beam.sdk.transforms.DoFn.ProcessElement", "org.junit.jupiter.api.Nested");

  private static final String USED_REFLECTIVELY = "UsedReflectively";

  private static final String KEEP = "Keep";

  /**
   * Retrieves an annotation, considering annotation inheritance.
   *
   * @deprecated If {@code annotationClass} contains a member that is a {@code Class} or an array of
   *     them, attempting to access that member from the Error Prone checker code will result in a
   *     runtime exception. Instead, operate on {@code getSymbol(tree).getAnnotationMirrors()} to
   *     meta-syntactically inspect the annotation. Note that this method (and the {@code
   *     getSymbol}-based replacement suggested above) looks for annotations not just on the given
   *     tree (such as a {@link MethodTree}) but also on the symbol referred to by the given tree
   *     (such as on the {@link MethodSymbol} that is being called by the given {@link
   *     MethodInvocationTree}). If you want to examine annotations only on the given tree, then use
   *     {@link #getAnnotations} (or a direct call to a {@code getAnnotations} method declared on a
   *     specific {@link Tree} subclass) instead.
   */
  @Deprecated
  public static <T extends Annotation> @Nullable T getAnnotation(
      Tree tree, Class<T> annotationClass) {
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
  @Deprecated
  @SuppressWarnings("deprecation")
  public static <T extends Annotation> @Nullable T getAnnotation(
      Symbol sym, Class<T> annotationClass) {
    return sym == null ? null : sym.getAnnotation(annotationClass);
  }

  /**
   * @return all values of the given enum type, in declaration order.
   */
  public static LinkedHashSet<String> enumValues(TypeSymbol enumType) {
    if (enumType.getKind() != ElementKind.ENUM) {
      throw new IllegalStateException();
    }
    Scope scope = enumType.members();
    Deque<String> values = new ArrayDeque<>();
    for (Symbol sym : scope.getSymbols()) {
      if (sym instanceof VarSymbol var) {
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

  /** Returns true if the given tree is a generated constructor. */
  public static boolean isGeneratedConstructor(MethodTree tree) {
    if (!(tree instanceof JCMethodDecl jCMethodDecl)) {
      return false;
    }
    return (jCMethodDecl.mods.flags & Flags.GENERATEDCONSTR) == Flags.GENERATEDCONSTR;
  }

  /** Returns the list of all constructors defined in the class (including generated ones). */
  public static List<MethodTree> getConstructors(ClassTree classTree) {
    List<MethodTree> constructors = new ArrayList<>();
    for (Tree member : classTree.getMembers()) {
      if (member instanceof MethodTree methodTree) {
        if (getSymbol(methodTree).isConstructor()) {
          constructors.add(methodTree);
        }
      }
    }
    return constructors;
  }

  /**
   * A wrapper for {@link Symbol#getEnclosedElements} to avoid binary compatibility issues for
   * covariant overrides in subtypes of {@link Symbol}.
   */
  public static List<Symbol> getEnclosedElements(Symbol symbol) {
    return symbol.getEnclosedElements();
  }

  /** Returns the list of all constructors defined in the class. */
  public static ImmutableList<MethodSymbol> getConstructors(ClassSymbol classSymbol) {
    return getEnclosedElements(classSymbol).stream()
        .filter(Symbol::isConstructor)
        .map(e -> (MethodSymbol) e)
        .collect(toImmutableList());
  }

  /**
   * Returns the {@code Type} of the given tree, or {@code null} if the type could not be
   * determined.
   */
  public static @Nullable Type getType(@Nullable Tree tree) {
    return tree instanceof JCTree jCTree ? jCTree.type : null;
  }

  /**
   * Returns the {@code ClassType} for the given type {@code ClassTree} or {@code null} if the type
   * could not be determined.
   */
  public static @Nullable ClassType getType(@Nullable ClassTree tree) {
    Type type = getType((Tree) tree);
    return type instanceof ClassType classType ? classType : null;
  }

  public static @Nullable String getAnnotationName(AnnotationTree tree) {
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
  public static @Nullable ClassSymbol enclosingClass(Symbol sym) {
    // sym.owner is null in the case of module symbols.
    return sym.owner == null ? null : sym.owner.enclClass();
  }

  /**
   * Return the enclosing {@code PackageSymbol} of the given symbol, or {@code null}.
   *
   * <p>Prefer this to {@link Symbol#packge}, which throws a {@link NullPointerException} for
   * symbols that are not contained by a package: https://bugs.openjdk.java.net/browse/JDK-8231911
   */
  public static @Nullable PackageSymbol enclosingPackage(Symbol sym) {
    Symbol curr = sym;
    while (curr != null) {
      if (curr.getKind().equals(ElementKind.PACKAGE)) {
        return (PackageSymbol) curr;
      }
      curr = curr.owner;
    }
    return null;
  }

  /** Return true if the given symbol is defined in the current package. */
  public static boolean inSamePackage(Symbol targetSymbol, VisitorState state) {
    JCCompilationUnit compilationUnit = (JCCompilationUnit) state.getPath().getCompilationUnit();
    PackageSymbol usePackage = compilationUnit.packge;
    PackageSymbol targetPackage = enclosingPackage(targetSymbol);

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
  public static @Nullable Object constValue(Tree tree) {
    if (tree == null) {
      return null;
    }
    tree = stripParentheses(tree);
    Type type = ASTHelpers.getType(tree);
    Object value;
    if (tree instanceof JCLiteral jCLiteral) {
      value = jCLiteral.value;
    } else if (type != null) {
      value = type.constValue();
    } else {
      return null;
    }
    if (type.hasTag(TypeTag.BOOLEAN) && value instanceof Integer integer) {
      return integer == 1;
    }
    if (type.hasTag(TypeTag.CHAR) && value instanceof Integer) {
      return (char) (int) value;
    }
    return value;
  }

  /** Returns the compile-time constant value of a tree if it is of type clazz, or {@code null}. */
  public static <T> @Nullable T constValue(Tree tree, Class<? extends T> clazz) {
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

  private static final ImmutableSet<TypeTag> SUBTYPE_UNDEFINED =
      Sets.immutableEnumSet(TypeTag.METHOD, TypeTag.PACKAGE, TypeTag.ERROR, TypeTag.FORALL);

  /** Returns true if {@code erasure(s) <: erasure(t)}. */
  public static boolean isSubtype(Type s, Type t, VisitorState state) {
    if (s == null || t == null) {
      return false;
    }
    if (SUBTYPE_UNDEFINED.contains(s.getTag())) {
      return false;
    }
    if (t == state.getSymtab().unknownType) {
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
  public static @Nullable ModifiersTree getModifiers(Tree tree) {
    if (tree instanceof ClassTree classTree) {
      return classTree.getModifiers();
    }
    if (tree instanceof MethodTree methodTree) {
      return methodTree.getModifiers();
    }
    if (tree instanceof VariableTree variableTree) {
      return variableTree.getModifiers();
    }
    if (tree instanceof ModifiersTree modifiersTree) {
      return modifiersTree;
    }
    return null;
  }

  /** Returns the annotations of the given tree, or an empty list. */
  public static List<? extends AnnotationTree> getAnnotations(Tree tree) {
    if (tree instanceof TypeParameterTree typeParameterTree) {
      return typeParameterTree.getAnnotations();
    }
    if (tree instanceof ModuleTree moduleTree) {
      return moduleTree.getAnnotations();
    }
    if (tree instanceof PackageTree packageTree) {
      return packageTree.getAnnotations();
    }
    if (tree instanceof NewArrayTree newArrayTree) {
      return newArrayTree.getAnnotations();
    }
    if (tree instanceof AnnotatedTypeTree annotatedTypeTree) {
      return annotatedTypeTree.getAnnotations();
    }
    if (tree instanceof ModifiersTree modifiersTree) {
      return modifiersTree.getAnnotations();
    }
    ModifiersTree modifiersTree = getModifiers(tree);
    return modifiersTree == null ? ImmutableList.of() : modifiersTree.getAnnotations();
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
      if (ancestor instanceof MethodTree methodTree
          && JUnitMatchers.hasJUnitAnnotation(methodTree, state)) {
        return true;
      }
      if (ancestor instanceof ClassTree classTree
          && (JUnitMatchers.isTestCaseDescendant.matches(classTree, state)
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
      if (ancestor instanceof MethodTree methodTree
          && TestNgMatchers.hasTestNgAnnotation(methodTree, state)) {
        return true;
      }
      if (ancestor instanceof ClassTree classTree
          && TestNgMatchers.hasTestNgAnnotation(classTree)) {
        return true;
      }
    }
    return false;
  }

  /** Returns an {@link AnnotationTree} with the given simple name, or {@code null}. */
  public static @Nullable AnnotationTree getAnnotationWithSimpleName(
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
    if (annotationType instanceof IdentifierTree identifierTree) {
      simpleName = identifierTree.getName();
    } else if (annotationType instanceof MemberSelectTree memberSelectTree) {
      simpleName = memberSelectTree.getIdentifier();
    } else {
      return false;
    }
    return simpleName.contentEquals(name);
  }

  /**
   * Returns whether {@code anno} corresponds to a type annotation, or {@code null} if it could not
   * be determined.
   */
  public static @Nullable AnnotationType getAnnotationType(
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
    return annotationTargetType(TypeAnnotations.instance(state.context), anno, compound, target);
  }

  private static AnnotationType annotationTargetType(
      TypeAnnotations typeAnnotations,
      AnnotationTree tree,
      Compound compound,
      @Nullable Symbol target) {
    try {
      try {
        // the JCTree argument was added in JDK 21
        return (AnnotationType)
            TypeAnnotations.class
                .getMethod("annotationTargetType", JCTree.class, Compound.class, Symbol.class)
                .invoke(typeAnnotations, tree, compound, target);
      } catch (NoSuchMethodException e1) {
        return (AnnotationType)
            TypeAnnotations.class
                .getMethod("annotationTargetType", Compound.class, Symbol.class)
                .invoke(typeAnnotations, compound, target);
      }
    } catch (ReflectiveOperationException e) {
      throw new LinkageError(e.getMessage(), e);
    }
  }

  /**
   * Extract the filename from a {@link CompilationUnitTree}, with special handling for jar files.
   * The return value is normalized to always use '/' to separate elements of the path and to always
   * have a leading '/'.
   */
  public static @Nullable String getFileName(CompilationUnitTree tree) {
    return getFileNameFromUri(tree.getSourceFile().toUri());
  }

  private static final CharMatcher BACKSLASH_MATCHER = CharMatcher.is('\\');

  /**
   * Extract the filename from the URI, with special handling for jar files. The return value is
   * normalized to always use '/' to separate elements of the path and to always have a leading '/'.
   */
  public static @Nullable String getFileNameFromUri(URI uri) {
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
  public static @Nullable MethodSymbol resolveExistingMethod(
      VisitorState state,
      TypeSymbol base,
      Name name,
      Iterable<Type> argTypes,
      Iterable<Type> tyargTypes) {
    Resolve resolve = Resolve.instance(state.context);
    Enter enter = Enter.instance(state.context);
    Log log = Log.instance(state.context);
    Log.DiagnosticHandler handler = ErrorProneLog.deferredDiagnosticHandler(log);
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
    return stream(state.getPath())
        .filter(ClassTree.class::isInstance)
        .flatMap(enclosing -> getGeneratedBy(getSymbol(enclosing)).stream())
        .collect(toImmutableSet());
  }

  /**
   * Returns the values of the given symbol's {@code Generated} annotations, if present. If the
   * annotation doesn't have {@code values} set, returns the string name of the annotation itself.
   */
  public static ImmutableSet<String> getGeneratedBy(Symbol symbol) {
    checkNotNull(symbol);
    return symbol.getRawAttributes().stream()
        .filter(attribute -> attribute.type.tsym.getSimpleName().contentEquals("Generated"))
        .flatMap(ASTHelpers::generatedValues)
        .collect(toImmutableSet());
  }

  /**
   * @deprecated TODO(ghm): delete after a JavaBuilder release
   */
  @Deprecated
  public static ImmutableSet<String> getGeneratedBy(Symbol symbol, VisitorState state) {
    return getGeneratedBy(symbol);
  }

  private static Stream<String> generatedValues(Attribute.Compound attribute) {
    return attribute.getElementValues().entrySet().stream()
        .filter(e -> e.getKey().getSimpleName().contentEquals("value"))
        .findFirst()
        .map(e -> MoreAnnotations.asStrings(e.getValue()))
        .orElseGet(() -> Stream.of(attribute.type.tsym.getQualifiedName().toString()));
  }

  public static boolean isSuper(Tree tree) {
    return switch (tree.getKind()) {
      case IDENTIFIER -> ((IdentifierTree) tree).getName().contentEquals("super");
      case MEMBER_SELECT -> ((MemberSelectTree) tree).getIdentifier().contentEquals("super");
      default -> false;
    };
  }

  /**
   * Attempts to detect whether we're in a static-initializer-like context: that includes direct
   * assignments to static fields, assignments to enum fields, being contained within an expression
   * which is ultimately assigned to a static field.
   *
   * <p>This is very much a heuristic, and not fool-proof.
   */
  public static boolean isInStaticInitializer(VisitorState state) {
    return stream(state.getPath())
        .anyMatch(
            tree ->
                (tree instanceof VariableTree && variableIsStaticFinal((VarSymbol) getSymbol(tree)))
                    || (tree instanceof AssignmentTree assignmentTree
                        && getSymbol(assignmentTree.getVariable()) instanceof VarSymbol varSymbol
                        && variableIsStaticFinal(varSymbol)));
  }

  /**
   * @deprecated use TargetType.targetType directly
   */
  @Deprecated
  public static @Nullable TargetType targetType(VisitorState state) {
    return TargetType.targetType(state);
  }

  /**
   * Whether the variable is (or should be regarded as) static final.
   *
   * <p>We regard instance fields within enums as "static final", as they will only have a finite
   * number of instances tied to an (effectively) static final enum value.
   */
  public static boolean variableIsStaticFinal(VarSymbol var) {
    return (var.isStatic() || var.owner.isEnum()) && var.getModifiers().contains(Modifier.FINAL);
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
  public static @Nullable ClassSymbol outermostClass(Symbol symbol) {
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

        Set<Type> caughtTypes = new HashSet<>();
        Set<Type> capturedTypes = new HashSet<>();
        for (Type unionMember : extractTypes(type)) {
          for (Type thrownType : getThrownTypes()) {
            // If the thrown type is a subtype of the caught type, we caught it, and it doesn't flow
            // through to any subsequent catches.
            if (types.isSubtype(thrownType, unionMember)) {
              caughtTypes.add(thrownType);
              capturedTypes.add(thrownType);
            }
            // If our caught type is a subtype of a thrown type, we caught something, but didn't
            // remove it from the list of things the try {} block throws.
            if (types.isSubtype(unionMember, thrownType)) {
              capturedTypes.add(unionMember);
            }
          }
        }
        getThrownTypes().removeAll(caughtTypes);
        thrownTypesByVariable.putAll(getSymbol(catchTree.getParameter()), capturedTypes);
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
      for (Tree resource : tree.getResources()) {
        Symbol symbol = getType(resource).tsym;

        if (symbol instanceof ClassSymbol classSymbol) {
          getCloseMethod(classSymbol, state)
              .ifPresent(methodSymbol -> getThrownTypes().addAll(methodSymbol.getThrownTypes()));
        }
      }
      scan(tree.getResources(), null);
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
      getThrownTypes().addAll(getSymbol(tree).getThrownTypes());
      return super.visitNewClass(tree, null);
    }

    @Override
    public Void visitVariable(VariableTree tree, Void unused) {
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

  /** Returns true if the symbol is static. Returns {@code false} for module symbols. */
  public static boolean isStatic(Symbol symbol) {
    return switch (symbol.getKind()) {
      case MODULE -> false;
      default -> symbol.isStatic();
    };
  }

  /**
   * Returns true if the given method symbol is public (both the method and the enclosing class) and
   * does <i>not</i> have a super-method (i.e., it is not an {@code @Override}).
   *
   * <p>This method is useful (in part) for determining whether to suggest API improvements or not.
   */
  public static boolean methodIsPublicAndNotAnOverride(MethodSymbol method, VisitorState state) {
    // don't match non-public APIs
    Symbol symbol = method;
    while (symbol != null && !(symbol instanceof PackageSymbol)) {
      if (!symbol.getModifiers().contains(Modifier.PUBLIC)) {
        return false;
      }
      symbol = symbol.owner;
    }

    // don't match overrides (even "effective overrides")
    if (!findSuperMethods(method, state.getTypes()).isEmpty()) {
      return false;
    }
    return true;
  }

  /**
   * Returns true if the given method symbol is abstract.
   *
   * <p><b>Note:</b> this API does not consider interface {@code default} methods to be abstract.
   */
  public static boolean isAbstract(MethodSymbol method) {
    return method.getModifiers().contains(Modifier.ABSTRACT);
  }

  public static EnumSet<Flags.Flag> asFlagSet(long flags) {
    flags &= ~(Flags.ANONCONSTR_BASED | POTENTIALLY_AMBIGUOUS);
    return Flags.asFlagSet(flags);
  }

  // Removed in JDK 21 by JDK-8026369
  public static final long POTENTIALLY_AMBIGUOUS = 1L << 48;

  /** Returns true if the given source code contains comments. */
  public static boolean stringContainsComments(CharSequence source, Context context) {
    JavaTokenizer tokenizer =
        new JavaTokenizer(ScannerFactory.instance(context), CharBuffer.wrap(source)) {};
    for (Token token = tokenizer.readToken();
        token.kind != TokenKind.EOF;
        token = tokenizer.readToken()) {
      if (token.comments != null && !token.comments.isEmpty()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the mapping between type variables and their instantiations in the given type. For
   * example, the instantiation of {@code Map<K, V>} as {@code Map<String, Integer>} would be
   * represented as a {@code TypeSubstitution} from {@code [K, V]} to {@code [String, Integer]}.
   */
  public static ImmutableListMultimap<Symbol.TypeVariableSymbol, Type> getTypeSubstitution(
      Type type, Symbol sym) {
    ImmutableListMultimap.Builder<Symbol.TypeVariableSymbol, Type> result =
        ImmutableListMultimap.builder();
    class Visitor extends Types.DefaultTypeVisitor<Void, Type> {

      @Override
      public Void visitMethodType(Type.MethodType t, Type other) {
        scan(t.getParameterTypes(), other.getParameterTypes());
        scan(t.getThrownTypes(), other.getThrownTypes());
        scan(t.getReturnType(), other.getReturnType());
        return null;
      }

      @Override
      public Void visitClassType(ClassType t, Type other) {
        scan(t.getTypeArguments(), other.getTypeArguments());
        return null;
      }

      @Override
      public Void visitTypeVar(TypeVar t, Type other) {
        result.put((Symbol.TypeVariableSymbol) t.asElement(), other);
        return null;
      }

      @Override
      public Void visitForAll(Type.ForAll t, Type other) {
        scan(t.getParameterTypes(), other.getParameterTypes());
        scan(t.getThrownTypes(), other.getThrownTypes());
        scan(t.getReturnType(), other.getReturnType());
        return null;
      }

      @Override
      public Void visitWildcardType(WildcardType t, Type type) {
        if (type instanceof WildcardType other) {
          scan(t.getExtendsBound(), other.getExtendsBound());
          scan(t.getSuperBound(), other.getSuperBound());
        }
        return null;
      }

      @Override
      public Void visitArrayType(ArrayType t, Type type) {
        if (type instanceof ArrayType other) {
          scan(t.getComponentType(), other.getComponentType());
        }
        return null;
      }

      @Override
      public Void visitType(Type t, Type other) {
        return null;
      }

      private void scan(Collection<Type> from, Collection<Type> to) {
        Streams.forEachPair(from.stream(), to.stream(), this::scan);
      }

      private void scan(Type from, Type to) {
        if (from != null) {
          from.accept(this, to);
        }
      }
    }
    sym.asType().accept(new Visitor(), type);
    return result.build();
  }

  /** Returns whether this is a {@code var} or a lambda parameter that has no explicit type. */
  public static boolean hasImplicitType(VariableTree tree, VisitorState state) {
    /*
     * For lambda expression parameters without an explicit type, both
     * `JCVariableDecl#declaredUsingVar()` and `#isImplicitlyTyped()` may be false. So instead we
     * check whether the variable's type is explicitly represented in the source code.
     */
    return !hasExplicitSource(tree.getType(), state);
  }

  /** Returns whether the given tree has an explicit source code representation. */
  public static boolean hasExplicitSource(Tree tree, VisitorState state) {
    return getStartPosition(tree) != Position.NOPOS && state.getEndPosition(tree) != Position.NOPOS;
  }

  /** Returns {@code true} if this symbol was declared in Kotlin source. */
  public static boolean isKotlin(Symbol symbol, VisitorState state) {
    return hasAnnotation(symbol.enclClass(), "kotlin.Metadata", state);
  }

  /**
   * Returns whether {@code existingMethod} has an overload (or "nearly" an overload) with the given
   * {@code targetMethodName}, and only a single parameter of type {@code onlyParameterType}.
   */
  public static boolean hasOverloadWithOnlyOneParameter(
      MethodSymbol existingMethod,
      Name targetMethodName,
      Type onlyParameterType,
      VisitorState state) {
    @Nullable MethodTree t = state.findEnclosing(MethodTree.class);
    @Nullable MethodSymbol enclosingMethod = t == null ? null : getSymbol(t);

    return hasMatchingMethods(
        targetMethodName,
        input ->
            !input.equals(existingMethod)
                // Make sure we're not currently *inside* that overload, to avoid
                // creating an infinite loop.
                && !input.equals(enclosingMethod)
                && (enclosingMethod == null
                    || !enclosingMethod.overrides(
                        input, (TypeSymbol) input.owner, state.getTypes(), true))
                && input.isStatic() == existingMethod.isStatic()
                && input.getParameters().size() == 1
                && isSameType(input.getParameters().get(0).asType(), onlyParameterType, state)
                && isSameType(input.getReturnType(), existingMethod.getReturnType(), state),
        enclosingClass(existingMethod).asType(),
        state.getTypes());
  }

  // Adapted from findMatchingMethods(); but this short-circuits
  private static boolean hasMatchingMethods(
      Name name, Predicate<MethodSymbol> predicate, Type startClass, Types types) {
    Predicate<Symbol> matchesMethodPredicate =
        sym -> sym instanceof MethodSymbol methodSymbol && predicate.test(methodSymbol);

    // Iterate over all classes and interfaces that startClass inherits from.
    for (Type superClass : types.closure(startClass)) {
      // Iterate over all the methods declared in superClass.
      TypeSymbol superClassSymbol = superClass.tsym;
      Scope superClassSymbols = superClassSymbol.members();
      if (superClassSymbols != null) { // Can be null if superClass is a type variable
        if (!Iterables.isEmpty(
            superClassSymbols.getSymbolsByName(name, matchesMethodPredicate, NON_RECURSIVE))) {
          return true;
        }
      }
    }
    return false;
  }

  public static Optional<? extends CaseTree> getSwitchDefault(SwitchTree switchTree) {
    return switchTree.getCases().stream().filter(c -> isSwitchDefault(c)).findFirst();
  }

  /** Returns whether {@code caseTree} is the default case of a switch statement. */
  public static boolean isSwitchDefault(CaseTree caseTree) {
    if (!caseTree.getExpressions().isEmpty()) {
      return false;
    }
    List<? extends Tree> labels = caseTree.getLabels();
    return labels.isEmpty()
        || (labels.size() == 1
            // DEFAULT_CASE_LABEL is in Java 21, so we're stuck stringifying for now.
            && getOnlyElement(labels).getKind().name().equals("DEFAULT_CASE_LABEL"));
  }

  private static final Supplier<Name> NULL_MARKED_NAME =
      memoize(state -> state.getName("org.jspecify.annotations.NullMarked"));

  private ASTHelpers() {}
}
