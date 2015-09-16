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

import com.google.common.base.CharMatcher;
import com.google.errorprone.VisitorState;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnalysis;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Suppliers;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.TypeVar;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;

/**
 * This class contains utility methods to work with the javac AST.
 */
public class ASTHelpers {

  /**
   * Determines whether two expressions refer to the same variable. Note that returning false
   * doesn't necessarily mean the expressions do *not* refer to the same field. We don't attempt
   * to do any complex analysis here, just catch the obvious cases.
   */
  public static boolean sameVariable(ExpressionTree expr1, ExpressionTree expr2) {
    // Throw up our hands if we're not comparing identifiers and/or field accesses.
    if ((expr1.getKind() != Kind.IDENTIFIER && expr1.getKind() != Kind.MEMBER_SELECT) ||
        (expr2.getKind() != Kind.IDENTIFIER && expr2.getKind() != Kind.MEMBER_SELECT)) {
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
      return sym1.equals(sym2) &&
          sameVariable(((JCFieldAccess) expr1).selected,((JCFieldAccess) expr2).selected);
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
   * Gets the symbol for a tree. Returns null if this tree does not have a symbol because it is
   * of the wrong type, or if {@code tree} is null.
   */
  // TODO(eaftan): refactor other code that accesses symbols to use this method
  public static Symbol getSymbol(Tree tree) {
    if (tree instanceof ClassTree) {
      return getSymbol((ClassTree) tree);
    }
    if (tree instanceof MethodTree) {
      return getSymbol((MethodTree) tree);
    }
    if (tree instanceof VariableTree) {
      return getSymbol((VariableTree) tree);
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
      return ((JCNewClass) tree).constructor;
    }
    if (tree instanceof AnnotationTree) {
      return getSymbol(((AnnotationTree) tree).getAnnotationType());
    }
    return null;
  }

  /** Gets the symbol for a class. */
  public static ClassSymbol getSymbol(ClassTree tree) {
    return ((JCClassDecl) tree).sym;
  }

  /** Gets the symbol for a method. */
  public static MethodSymbol getSymbol(MethodTree tree) {
    return ((JCMethodDecl) tree).sym;
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

  /**
   * Given a TreePath, walks up the tree until it finds a node of the given type and returns
   * the path from that node to the top-level node in the path (typically a
   * {@code CompilationUnitTree}).
   */
  public static <T> TreePath findPathFromEnclosingNodeToTopLevel(TreePath path,
        Class<T> klass) {
    while (path != null && !(klass.isInstance(path.getLeaf()))) {
      path = path.getParentPath();
    }
    return path;
  }

  /**
   * Given a TreePath, walks up the tree until it finds a node of the given type.
   */
  public static <T> T findEnclosingNode(TreePath path, Class<T> klass) {
    path = findPathFromEnclosingNodeToTopLevel(path, klass);
    return (path == null) ? null : klass.cast(path.getLeaf());
  }

  /**
   * Find the root assignable expression of a chain of field accesses.  If there is no root
   * (i.e, a bare method call or a static method call), return null.
   *
   * Examples:
   *    a.trim().intern() ==> a
   *    a.b.trim().intern() ==> a.b
   *    this.intValue.foo() ==> this.intValue
   *    this.foo() ==> this
   *    intern() ==> null
   *    String.format() ==> null
   *    java.lang.String.format() ==> null
   */
  public static ExpressionTree getRootAssignable(MethodInvocationTree methodInvocationTree) {
    if (!(methodInvocationTree instanceof JCMethodInvocation)) {
      throw new IllegalArgumentException("Expected type to be JCMethodInvocation, but was "
          + methodInvocationTree.getClass());
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
   * TODO(eaftan): Are there other places this could be used?
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
   * Returns the type of a receiver of a method call expression.
   * Precondition: the expressionTree corresponds to a method call.
   *
   * Examples:
   *    a.b.foo() ==> type of a.b
   *    a.bar().foo() ==> type of a.bar()
   *    this.foo() ==> type of this
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
    }
    throw new IllegalArgumentException(
        "Expected a JCFieldAccess or JCIdent from expression " + expressionTree);
  }

  /**
   * Returns the receiver of an expression.
   *
   * Examples:
   *    a.foo() ==> a
   *    a.b.foo() ==> a.b
   *    a.bar().foo() ==> a.bar()
   *    a.b.c ==> a.b
   *    a.b().c ==> a.b()
   */
  public static ExpressionTree getReceiver(ExpressionTree expressionTree) {
    if (expressionTree instanceof MethodInvocationTree) {
      return getReceiver(((MethodInvocationTree) expressionTree).getMethodSelect());
    } else if (expressionTree instanceof MemberSelectTree) {
      return ((MemberSelectTree) expressionTree).getExpression();
    } else {
      throw new IllegalStateException(
          String.format(
              "Expected expression '%s' to be a method invocation or field access, but was %s",
              expressionTree,
              expressionTree.getKind()));
    }
  }

  /**
   * Given a BinaryTree to match against and a list of two matchers, applies the matchers to the
   * operands in both orders.  If both matchers match, returns a list with the operand that
   * matched each matcher in the corresponding position.
   *
   * @param tree a BinaryTree AST node
   * @param matchers a list of matchers
   * @param state the VisitorState
   * @return a list of matched operands, or null if at least one did not match
   */
  public static List<ExpressionTree> matchBinaryTree(BinaryTree tree,
      List<Matcher<ExpressionTree>> matchers, VisitorState state) {
    ExpressionTree leftOperand = tree.getLeftOperand();
    ExpressionTree rightOperand = tree.getRightOperand();
    if (matchers.get(0).matches(leftOperand, state) &&
        matchers.get(1).matches(rightOperand, state)) {
      return Arrays.asList(leftOperand, rightOperand);
    } else if (matchers.get(0).matches(rightOperand, state) &&
        matchers.get(1).matches(leftOperand, state)) {
      return Arrays.asList(rightOperand, leftOperand);
    }
    return null;
  }

  /**
   * A collection of Java whitespace characters, as defined by JLS 3.6.
   */
  private static final CharMatcher WHITESPACE_CHARS = CharMatcher.anyOf(" \t\f\n\r");

  /**
   * Hacky fix for poor javac 6 literal parsing.  javac 6 doesn't set the AST node start
   * position correctly when a numeric literal is preceded by -. So we scan the source
   * backwards starting at the provided start position, looking for whitespace, until we find
   * the true start position.  javac 7 gets this right.
   *
   * @return The actual start position of the literal. May be the same as the start position
   * given by the tree node itself.
   */
  public static int getActualStartPosition(JCLiteral tree, CharSequence source) {
    // This only applies to negative numeric literals.
    Object value = tree.getValue();
    if ((value instanceof Number) && (((Number) value).doubleValue() < 0)) {
      int start = tree.getStartPosition() - 1;
      while (WHITESPACE_CHARS.matches(source.charAt(start))) {
        start--;
      }
      if (source.charAt(start) == '-') {
        return start;
      }
    }
    return tree.getStartPosition();
  }

  public static Set<MethodSymbol> findSuperMethods(MethodSymbol methodSymbol, Types types) {
    Set<MethodSymbol> supers = new HashSet<>();
    if (methodSymbol.isStatic()) {
      return supers;
    }

    TypeSymbol owner = (TypeSymbol) methodSymbol.owner;
    // Iterates over an ordered list of all super classes and interfaces.
    for (Type sup : types.closure(owner.type)) {
      if (sup == owner.type) {
        continue; // Skip the owner of the method
      }
      Scope scope = sup.tsym.members();
      for (Symbol sym : scope.getSymbolsByName(methodSymbol.name)) {
        if (sym != null
            && !sym.isStatic()
            && ((sym.flags() & Flags.SYNTHETIC) == 0)
            && sym.name.contentEquals(methodSymbol.name)
            && methodSymbol.overrides(sym, owner, types, true)) {
          supers.add((MethodSymbol) sym);
        }
      }
    }
    return supers;
  }

  /**
   * Find a method in the enclosing class's superclass that this method overrides.
   *
   * @return A superclass method that is overridden by {@code method}
   */
  public static MethodSymbol findSuperMethod(MethodSymbol method, Types types) {
    TypeSymbol superClass = method.enclClass().getSuperclass().tsym;
    if (superClass == null) {
      return null;
    }
    for (Symbol sym : superClass.members().getSymbols()) {
      if (sym.name.contentEquals(method.name)
          && method.overrides(sym, superClass, types, true)) {
        return (MethodSymbol) sym;
      }
    }
    return null;
  }

  /**
   * Determines whether a symbol has an annotation of the given type.
   * This includes annotations inherited from superclasses due to @Inherited.
   *
   * @param annotationType The type of the annotation to look for (e.g, "javax.annotation.Nullable")
   */
  public static boolean hasAnnotation(Symbol sym, String annotationType, VisitorState state) {
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

  /**
   * Check for the presence of an annotation, considering annotation inheritance.
   *
   * @return true if the symbol is annotated with given type.
   */
  public static boolean hasAnnotation(Symbol sym, Class<? extends Annotation> annotationType) {
    return getAnnotation(sym, annotationType) != null;
  }

  /**
   * Check for the presence of an annotation, considering annotation inheritance.
   *
   * @return true if the tree's symbol has an annotation of the given type.
   */
  public static boolean hasAnnotation(Tree tree, Class<? extends Annotation> annotationType) {
    return getAnnotation(tree, annotationType) != null;
  }

  /**
   * Retrieve an annotation, considering annotation inheritance.
   *
   * @return the annotation of given type on the tree's symbol, or null.
   */
  public static <T extends Annotation> T getAnnotation(Tree tree, Class<T> annotationType) {
    Symbol sym = getSymbol(tree);
    return sym == null ? null : getAnnotation(sym, annotationType);
  }

  /**
   * Retrieve an annotation, considering annotation inheritance.
   *
   * @return the annotation of given type on the symbol, or null.
   */
  // Symbol#getAnnotation is not intended for internal javac use, but because error-prone is run
  // after attribution it's safe to use here.
  @SuppressWarnings("deprecation")
  public static <T extends Annotation> T getAnnotation(Symbol sym, Class<T> annotationType) {
    return sym == null ? null : sym.getAnnotation(annotationType);
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
           * preserve declaration order because it's useful for diagnostics (e.g. in
           * {@link MissingCasesInEnumSwitch}).
           */
          values.push(sym.name.toString());
        }
      }
    }
    return new LinkedHashSet<>(values);
  }

  /** Returns true if the given tree is a generated constructor. **/
  public static boolean isGeneratedConstructor(MethodTree tree) {
    if (!(tree instanceof JCMethodDecl)) {
        return false;
    }
    return (((JCMethodDecl) tree).mods.flags & Flags.GENERATEDCONSTR) != 0;
  }

  /**
   * Returns the {@code Type} for the given type {@code Tree} or {@code null} if the type could not
   * be determined. The input {@code Tree} typically comes from a method like
   * {@link VariableTree#getType()} or {@link MethodTree#getReturnType()}.
   */
  public static Type getType(Tree tree) {
    if (tree instanceof JCFieldAccess) {
      return ((JCFieldAccess) tree).sym.type;
    } else if (tree instanceof JCTree) {
      return ((JCTree) tree).type;
    } else {
      return null;
    }
  }

  public static String getAnnotationName(AnnotationTree tree) {
    Symbol sym = getSymbol(tree);
    return sym == null ? null : sym.name.toString();
  }

  private static <T extends Symbol> T enclosingSymbol(Symbol sym, Class<T> clazz) {
    while (sym != null) {
      if (clazz.isInstance(sym)) {
        return clazz.cast(sym);
      }
      sym = sym.owner;
    }
    return null;
  }

  /** Return the enclosing {@code ClassSymbol} of the given symbol, or {@code null}. */
  public static ClassSymbol enclosingClass(Symbol sym) {
    return enclosingSymbol(sym, ClassSymbol.class);
  }

  /** Return the enclosing {@code PackageSymbol} of the given symbol, or {@code null}. */
  public static PackageSymbol enclosingPackage(Symbol sym) {
    return enclosingSymbol(sym, PackageSymbol.class);
  }

  /**
   * Returns the {@link Nullness} for an expression as determined by the nullness dataflow
   * analysis.
   */
  public static Nullness getNullnessValue(ExpressionTree expr, VisitorState state,
      NullnessAnalysis nullnessAnalysis) {
    TreePath pathToExpr = new TreePath(state.getPath(), expr);
    return nullnessAnalysis.getNullness(pathToExpr, state.context);
  }

  /**
   * Returns the constant value of a tree, if it has one, or null otherwise.
   */
  public static Object constValue(JCTree tree) {
    return (tree instanceof JCLiteral)
        ? ((JCLiteral) tree).value
        : tree.type.constValue();
  }

  /**
   * Return true if the given type is 'void' or 'Void'.
   */
  public static boolean isVoidType(Type type, VisitorState state) {
    if (type == null) {
      return false;
    }
    return type.getKind() == TypeKind.VOID
        || state.getTypes().isSameType(Suppliers.JAVA_LANG_VOID_TYPE.get(state), type);
  }

  /**
   * Returns true if erasure(s) <: erasure(t).
   */
  public static boolean isSubtype(Type s, Type t, VisitorState state) {
    if (s == null || t == null) {
      return false;
    }
    Types types = state.getTypes();
    return types.isSubtype(types.erasure(s), types.erasure(t));
  }

  /** Returns true if erasure(s) == erasure(t). */
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
   * Returns the upper bound of a type if it has one, or the type itself if not.  Correctly handles
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
}
