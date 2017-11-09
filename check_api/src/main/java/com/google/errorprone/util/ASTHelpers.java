/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.matchers.JUnitMatchers.JUNIT4_RUN_WITH_ANNOTATION;
import static com.sun.tools.javac.code.Scope.LookupKind.NON_RECURSIVE;

import com.google.common.base.CharMatcher;
import com.google.common.base.Predicate;
import com.google.errorprone.VisitorState;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnalysis;
import com.google.errorprone.matchers.JUnitMatchers;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Suppliers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.TypeVar;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
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
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;

/** This class contains utility methods to work with the javac AST. */
public class ASTHelpers {
  /**
   * Determines whether two expressions refer to the same variable. Note that returning false
   * doesn't necessarily mean the expressions do *not* refer to the same field. We don't attempt to
   * do any complex analysis here, just catch the obvious cases.
   */
  public static boolean sameVariable(ExpressionTree expr1, ExpressionTree expr2) {
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
   * Gets the symbol declared by a tree. Returns null if this tree does not declare a symbol, if
   * {@code tree} is null.
   */
  public static Symbol getDeclaredSymbol(Tree tree) {
    if (tree instanceof AnnotationTree) {
      return getSymbol(((AnnotationTree) tree).getAnnotationType());
    }
    if (tree instanceof PackageTree) {
      return getSymbol((PackageTree) tree);
    }
    if (tree instanceof ParameterizedTypeTree) {
      return getSymbol(((ParameterizedTypeTree) tree).getType());
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
  public static MethodSymbol getSymbol(NewClassTree tree) {
    Symbol sym = ((JCNewClass) tree).constructor;
    return sym instanceof MethodSymbol ? (MethodSymbol) sym : null;
  }

  /** Gets the symbol for a variable. */
  public static VarSymbol getSymbol(VariableTree tree) {
    return ((JCVariableDecl) tree).sym;
  }

  /** Gets the symbol for a method invocation. */
  public static MethodSymbol getSymbol(MethodInvocationTree tree) {
    Symbol sym = ASTHelpers.getSymbol(tree.getMethodSelect());
    if (!(sym instanceof MethodSymbol)) {
      // Defensive. Would only occur if there are errors in the AST.
      return null;
    }
    return (MethodSymbol) sym;
  }

  /** Gets the symbol for a member reference. */
  public static MethodSymbol getSymbol(MemberReferenceTree tree) {
    Symbol sym = ((JCMemberReference) tree).sym;
    return sym instanceof MethodSymbol ? (MethodSymbol) sym : null;
  }

  /** Removes any enclosing parentheses from the tree. */
  public static Tree stripParentheses(Tree tree) {
    while (tree instanceof ParenthesizedTree) {
      tree = ((ParenthesizedTree) tree).getExpression();
    }
    return tree;
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
    }
    throw new IllegalArgumentException("Expected a JCFieldAccess or JCIdent");
  }

  /**
   * Returns the type that this expression tree will evaluate to. If its a literal, an identifier,
   * or a member select this is the actual type, if its a method invocation then its the return type
   * of the method (after instantiating generic types), if its a constructor then its the type of
   * the returned class.
   *
   * <p>TODO(andrewrice) consider replacing {@code getReturnType} with this method
   *
   * @param expressionTree the tree to evaluate
   * @return the result type of this tree or null if unable to resolve it
   */
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
          && sym.name.contentEquals(methodSymbol.name)
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
    return types
        .closure(owner.type)
        .stream()
        .filter(closureTypes -> skipInterfaces ? !closureTypes.isInterface() : true)
        .map(type -> findSuperMethodInType(methodSymbol, type, types))
        .filter(Objects::nonNull);
  }

  /**
   * Finds all methods in any superclass of {@code startClass} with a certain {@code name} that
   * match the given {@code predicate}.
   *
   * @return The (possibly empty) set of methods in any superclass that match {@code predicate} and
   *     have the given {@code name}.
   */
  public static Set<MethodSymbol> findMatchingMethods(
      Name name, final Predicate<MethodSymbol> predicate, Type startClass, Types types) {
    Filter<Symbol> matchesMethodPredicate =
        sym -> sym instanceof MethodSymbol && predicate.apply((MethodSymbol) sym);

    Set<MethodSymbol> matchingMethods = new HashSet<>();
    // Iterate over all classes and interfaces that startClass inherits from.
    for (Type superClass : types.closure(startClass)) {
      // Iterate over all the methods declared in superClass.
      TypeSymbol superClassSymbol = superClass.tsym;
      Scope superClassSymbols = superClassSymbol.members();
      if (superClassSymbols != null) { // Can be null if superClass is a type variable
        for (Symbol symbol :
            superClassSymbols.getSymbolsByName(name, matchesMethodPredicate, NON_RECURSIVE)) {
          // By definition of the filter, we know that the symbol is a MethodSymbol.
          matchingMethods.add((MethodSymbol) symbol);
        }
      }
    }
    return matchingMethods;
  }

  /**
   * Determines whether a symbol has an annotation of the given type. This includes annotations
   * inherited from superclasses due to {@code @Inherited}.
   *
   * @param annotationClass the binary class name of the annotation (e.g.
   *     "javax.annotation.Nullable", or "some.package.OuterClassName$InnerClassName")
   */
  public static boolean hasAnnotation(Symbol sym, String annotationClass, VisitorState state) {
    Name annotationName = state.getName(annotationClass);
    Symbol annotationSym;
    synchronized (state.context) {
      annotationSym =
          state.getSymtab().enterClass(state.inferModule(annotationName), annotationName);
    }
    try {
      annotationSym.complete();
    } catch (CompletionFailure e) {
      // @Inherited won't work if the annotation isn't on the classpath, but we can still check
      // if it's present directly
    }
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
   * @return the annotation of given type on the tree's symbol, or null.
   */
  public static boolean hasAnnotation(
      Tree tree, Class<? extends Annotation> annotationClass, VisitorState state) {
    Symbol sym = getDeclaredSymbol(tree);
    return hasAnnotation(sym, annotationClass.getName(), state);
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
   * Retrieve an annotation, considering annotation inheritance.
   *
   * @return the annotation of given type on the tree's symbol, or null.
   */
  public static <T extends Annotation> T getAnnotation(Tree tree, Class<T> annotationClass) {
    Symbol sym = getSymbol(tree);
    return sym == null ? null : getAnnotation(sym, annotationClass);
  }

  /**
   * Retrieve an annotation, considering annotation inheritance.
   *
   * @return the annotation of given type on the symbol, or null.
   */
  // Symbol#getAnnotation is not intended for internal javac use, but because error-prone is run
  // after attribution it's safe to use here.
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
          /**
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

  /**
   * Returns the {@code Type} of the given tree, or {@code null} if the type could not be
   * determined.
   */
  @Nullable
  public static Type getType(Tree tree) {
    return tree instanceof JCTree ? ((JCTree) tree).type : null;
  }

  /**
   * Returns the {@code ClassType} for the given type {@code ClassTree} or {@code null} if the type
   * could not be determined.
   */
  @Nullable
  public static ClassType getType(ClassTree tree) {
    Type type = getType((Tree) tree);
    return type instanceof ClassType ? (ClassType) type : null;
  }

  public static String getAnnotationName(AnnotationTree tree) {
    Symbol sym = getSymbol(tree);
    return sym == null ? null : sym.name.toString();
  }

  /** Return the enclosing {@code ClassSymbol} of the given symbol, or {@code null}. */
  public static ClassSymbol enclosingClass(Symbol sym) {
    return sym.owner.enclClass();
  }

  /** Return the enclosing {@code PackageSymbol} of the given symbol, or {@code null}. */
  public static PackageSymbol enclosingPackage(Symbol sym) {
    return sym.packge();
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

  /** Returns true if {@code erasure(s) <: erasure(t)}. */
  public static boolean isSubtype(Type s, Type t, VisitorState state) {
    if (s == null || t == null) {
      return false;
    }
    Types types = state.getTypes();
    return types.isSubtype(types.erasure(s), types.erasure(t));
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
          && JUnitMatchers.hasJUnitAnnotation.matches((MethodTree) ancestor, state)) {
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
   * Finds a declaration with the given name that is in scope at the current location.
   *
   * @deprecated Use {@link FindIdentifiers#findIdent} instead.
   */
  // TODO(eaftan): migrate plugin callers and delete this
  @Deprecated
  public static Symbol findIdent(String name, VisitorState state) {
    return FindIdentifiers.findIdent(name, state);
  }

  /** Returns an {@link AnnotationTree} with the given simple name, or {@code null}. */
  public static AnnotationTree getAnnotationWithSimpleName(
      List<? extends AnnotationTree> annotations, String name) {
    for (AnnotationTree annotation : annotations) {
      if (hasSimpleName(annotation, name)) {
        return annotation;
      }
    }
    return null;
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
   * <p>Do NOT call this method unless the method you're looking for is guaranteed to exist. A fatal
   * error will result otherwise. Note: a method can fail to exist if it was added in a newer
   * version of a library (you may be depending on version N of a library which added a method to a
   * class, but someone else could depend on version N-1 which didn't have that method).
   *
   * @return a MethodSymbol representing the method symbol resolved from the context of this type
   */
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
    } finally {
      log.popDiagnosticHandler(handler);
    }
  }
}
