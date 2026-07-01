/*
 * Copyright 2015 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns;

import static com.google.common.collect.MoreCollectors.onlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.VisitorState.memoize;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.suppliers.Suppliers.typeFromString;
import static com.google.errorprone.util.ASTHelpers.findClass;
import static com.google.errorprone.util.ASTHelpers.getEnclosedElements;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getUpperBound;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.isStatic;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static com.sun.source.tree.Tree.Kind.EQUAL_TO;
import static com.sun.source.tree.Tree.Kind.NOT_EQUAL_TO;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.SEALED;
import static javax.lang.model.type.TypeKind.INTERSECTION;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Name;
import java.util.List;
import java.util.Objects;
import javax.lang.model.element.ElementKind;
import org.jspecify.annotations.Nullable;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "Comparison using reference equality instead of value equality",
    severity = WARNING,
    tags = StandardTags.FRAGILE_CODE)
public class ReferenceEquality extends AbstractReferenceEquality
    implements CompilationUnitTreeMatcher {
  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    /*
     * Instead of performing the checking in super.matchBinary, we perform it in
     * matchCompilationUnit, which can compute the class-hierarchy information that we want.
     */
    return NO_MATCH;
  }

  @Override
  public Description matchCompilationUnit(
      CompilationUnitTree compilationUnit, VisitorState stateForCompilationUnit) {
    ImmutableListMultimap.Builder<ClassSymbol, Type> subclassesBySuperclassBuilder =
        ImmutableListMultimap.builder();
    ImmutableList.Builder<TreePath> comparisonsBuilder = ImmutableList.builder();
    ImmutableMap.Builder<Symbol, ExpressionTree> constantFieldInitializersBuilder =
        ImmutableMap.builder();

    /*
     * We avoid SuppressibleTreePathScanner because we want to visit all *classes* to build the
     * hierarchy. That means that we have to track suppression ourselves for when we visit the
     * reference comparisons that we might report findings on.
     */
    new TreePathScanner<Void, Boolean>() {
      @Override
      public Void visitClass(ClassTree tree, Boolean isSuppressed) {
        ClassSymbol sym = getSymbol(tree);
        for (Type supertype : stateForCompilationUnit.getTypes().directSupertypes(sym.type)) {
          if (supertype.tsym instanceof ClassSymbol superSym) {
            subclassesBySuperclassBuilder.put(superSym, sym.type);
          }
        }
        return super.visitClass(tree, isSuppressed || isSuppressed(tree, stateForCompilationUnit));
      }

      @Override
      public Void visitMethod(MethodTree tree, Boolean isSuppressed) {
        return super.visitMethod(tree, isSuppressed || isSuppressed(tree, stateForCompilationUnit));
      }

      @Override
      public Void visitVariable(VariableTree tree, Boolean isSuppressed) {
        if (tree.getInitializer() != null) {
          Symbol sym = getSymbol(tree);
          if (sym.getKind() == ElementKind.FIELD && isStatic(sym) && sym.isFinal()) {
            constantFieldInitializersBuilder.put(sym, tree.getInitializer());
          }
        }
        return super.visitVariable(
            tree, isSuppressed || isSuppressed(tree, stateForCompilationUnit));
      }

      @Override
      public Void visitBinary(BinaryTree tree, Boolean isSuppressed) {
        if (!isSuppressed && (tree.getKind() == EQUAL_TO || tree.getKind() == NOT_EQUAL_TO)) {
          comparisonsBuilder.add(getCurrentPath());
        }
        return super.visitBinary(tree, isSuppressed);
      }
    }.scan(compilationUnit, false);

    ImmutableListMultimap<ClassSymbol, Type> subclassesBySuperclass =
        subclassesBySuperclassBuilder.build();
    ImmutableList<TreePath> comparisons = comparisonsBuilder.build();
    ImmutableMap<Symbol, ExpressionTree> constantFieldInitializers =
        constantFieldInitializersBuilder.buildOrThrow();

    for (TreePath path : comparisons) {
      BinaryTree binaryTree = (BinaryTree) path.getLeaf();
      if (isSentinelComparison(
              binaryTree.getLeftOperand(), stateForCompilationUnit, constantFieldInitializers)
          || isSentinelComparison(
              binaryTree.getRightOperand(), stateForCompilationUnit, constantFieldInitializers)) {
        continue;
      }
      stateForCompilationUnit.reportMatch(
          doMatchBinary(
              binaryTree,
              stateForCompilationUnit.withPath(path),
              (tree, state) -> matchArgument(tree, subclassesBySuperclass, state)));
    }

    return NO_MATCH; // Any matches were reported through reportMatch calls.
  }

  @Override
  protected boolean matchArgument(ExpressionTree tree, VisitorState state) {
    // This override is never called because we invoke doMatchBinary with a different Matcher.
    throw new AssertionError();
  }

  private static boolean matchArgument(
      ExpressionTree tree,
      ImmutableListMultimap<ClassSymbol, Type> subclassMap,
      VisitorState state) {
    Type type = ASTHelpers.getType(tree);
    if (!type.isReference()) {
      return false;
    }
    ClassTree classTree = ASTHelpers.findEnclosingNode(state.getPath(), ClassTree.class);
    if (classTree == null) {
      return false;
    }
    Type classType = ASTHelpers.getType(classTree);
    if (classType == null) {
      return false;
    }
    if (inComparisonMethod(classType, type, state)) {
      return false;
    }
    // https://github.com/google/error-prone/issues/5900
    if (isSubtype(type, JAVA_LANG_THREAD.get(state), state)) {
      return false;
    }
    if (definitelyUsesReferenceEquality(type, subclassMap, state)) {
      return false;
    }
    return true;
  }

  /**
   * True if this operand is a sentinel value (a static final field declared in the same file
   * initialized inline to a new class instance that doesn't override equals, or initialized to an
   * enum constant).
   */
  private static boolean isSentinelComparison(
      ExpressionTree tree,
      VisitorState state,
      ImmutableMap<Symbol, ExpressionTree> constantFieldInitializers) {
    Symbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null) {
      return false;
    }
    boolean finalField = sym.getKind() == ElementKind.FIELD && sym.isFinal();
    if (!finalField) {
      return false;
    }
    if (sym.getKind() == ElementKind.ENUM_CONSTANT) {
      return true;
    }
    ExpressionTree initializer = constantFieldInitializers.get(sym);
    if (initializer == null) {
      return false;
    }
    initializer = ASTHelpers.stripParentheses(initializer);
    if (initializer instanceof NewClassTree newClassTree) {
      Type instantiatedType = ASTHelpers.getType(newClassTree);
      if (instantiatedType != null && !implementsEquals(instantiatedType, state)) {
        return true;
      }
    }
    return false;
  }

  private static boolean inComparisonMethod(Type classType, Type type, VisitorState state) {
    Symtab symtab = state.getSymtab();
    // Check for lambdas implementing the Comparator.compare functional interface.
    LambdaExpressionTree lambdaTree =
        ASTHelpers.findEnclosingNode(state.getPath(), LambdaExpressionTree.class);
    if (lambdaTree != null) {
      return ASTHelpers.isSameType(ASTHelpers.getType(lambdaTree), symtab.comparatorType, state);
    }

    MethodTree methodTree = ASTHelpers.findEnclosingMethod(state);
    if (methodTree == null) {
      return false;
    }
    MethodSymbol sym = ASTHelpers.getSymbol(methodTree);
    if (sym.isStatic()) {
      return false;
    }
    if (overridesMethodOnType(classType, sym, symtab.comparatorType, "compare", state)) {
      return true;
    }
    if (overridesMethodOnType(classType, sym, symtab.comparableType, "compareTo", state)
        || overridesMethodOnType(classType, sym, symtab.objectType, "equals", state)) {
      return ASTHelpers.isSameType(type, classType, state);
    }
    return false;
  }

  private static boolean overridesMethodOnType(
      Type classType,
      MethodSymbol methodSymbol,
      Type overriddenType,
      String overriddenMethodName,
      VisitorState state) {
    Symbol overriddenMethodSymbol = getOnlyMember(overriddenType, overriddenMethodName);
    return methodSymbol.getSimpleName().contentEquals(overriddenMethodName)
        && methodSymbol.overrides(
            overriddenMethodSymbol, classType.tsym, state.getTypes(), /* checkResult= */ false);
  }

  private static Symbol getOnlyMember(Type type, String name) {
    return getEnclosedElements(type.tsym).stream()
        .filter(s -> s.getSimpleName().contentEquals(name))
        .collect(onlyElement());
  }

  /**
   * Returns {@code true} if an instance of {@code type} is guaranteed to have an {@code equals}
   * implementation that is equivalent to {@code ==}.
   *
   * <p>We can guarantee this for:
   *
   * <ul>
   *   <li>enum classes
   *   <li>{@code final} classes that inherit {@link Object#equals} instead of having a more
   *       specific implementation
   *   <li>{@code sealed} classes whose permitted subclasses all definitely use reference equality
   *       according to this method
   *   <li>{@code private} classes (or classes whose constructors are all {@code private}) whose
   *       subclasses within the compilation unit all definitely use reference equality according to
   *       this method
   * </ul>
   */
  private static boolean definitelyUsesReferenceEquality(
      Type type, ImmutableListMultimap<ClassSymbol, Type> subclassMap, VisitorState state) {
    return definitelyUsesReferenceEquality(type, subclassMap, state, 0);
  }

  private static boolean definitelyUsesReferenceEquality(
      Type type,
      ImmutableListMultimap<ClassSymbol, Type> subclassMap,
      VisitorState state,
      int depth) {
    if (depth > 1000) {
      /*
       * javac should never generate classes that form a PermittedSubclasses cycle, but just in case
       * some system does, we bail out when we have seen a chain that is implausibly long.
       */
      return false;
    }

    /*
     * If a value has static type `Class`, for example, then it uses reference equality, since
     * `Class` is a `final` class that does not override `equals`. But we also want to cover cases
     * like those of a value whose static type is `T` if `T` is declared as `T extends Class<Foo>`.
     * To do so, we look at the upper bound of the static type, transitively resolving a chain of
     * bounds (e.g., `<T extends Class<?>, U extends T>`) until we reach a fixed point.
     */
    Type previous;
    do {
      previous = type;
      type = getUpperBound(type, state.getTypes());
    } while (!state.getTypes().isSameType(type, previous));
    if (type.tsym == null) {
      return false;
    }
    if (isSubtype(type, state.getSymtab().enumSym.type, state)) {
      return true;
    }
    if (implementsEquals(type, state)) {
      return false;
    }
    if (!(type.tsym instanceof ClassSymbol sym)) {
      return false; // impossible?
    }
    List<Type> directSubclassesIfAllKnown = directSubclassesIfAllKnown(sym, state, subclassMap);
    if (directSubclassesIfAllKnown == null) {
      return false;
    }
    for (Type sub : directSubclassesIfAllKnown) {
      if (!definitelyUsesReferenceEquality(sub, subclassMap, state, depth + 1)) {
        return false;
      }
    }
    return true;
  }

  /**
   * If we are sure that we know the complete list of direct subclasses for the given class (such as
   * because it's {@code sealed} or {@code final}), returns all those subclasses. Otherwise, returns
   * {@code null}.
   */
  private static @Nullable List<Type> directSubclassesIfAllKnown(
      ClassSymbol sym, VisitorState state, ImmutableListMultimap<ClassSymbol, Type> subclassMap) {
    if (sym.type.getKind() == INTERSECTION) {
      /*
       * We could try to be smarter here, probably by looking at each component of the intersection
       * separately to see if any of them definitely use reference equality. But the important thing
       * is to avoid assuming that an intersection type is an anonymous class (since isAnonymous()
       * returns true for it) or that it has only private constructors (since it has none).
       */
      return null;
    }
    if (sym.getModifiers().contains(FINAL) || sym.isAnonymous()) {
      return ImmutableList.of();
    }
    if (sym.getModifiers().contains(SEALED)) {
      return sym.getPermittedSubclasses();
    }
    if (isDeclaredInCurrentCompilationUnitAndDirectlyExtensibleOnlyWithin(sym, state)) {
      return subclassMap.get(sym);
    }
    return null;
  }

  private static boolean isDeclaredInCurrentCompilationUnitAndDirectlyExtensibleOnlyWithin(
      ClassSymbol sym, VisitorState state) {
    if (!Objects.equals(sym.sourcefile, state.getPath().getCompilationUnit().getSourceFile())) {
      return false;
    }
    if (sym.getModifiers().contains(PRIVATE)) {
      return true;
    }
    if (sym.isInterface()) {
      return false;
    }
    for (Tree member : findClass(sym, state).getMembers()) {
      if (member instanceof MethodTree methodTree) {
        Symbol methodSym = getSymbol(methodTree);
        if (methodSym.isConstructor() && !methodSym.getModifiers().contains(PRIVATE)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Returns {@code true} if {@code type} declares or inherits an override of {@link Object#equals}.
   */
  private static boolean implementsEquals(Type type, VisitorState state) {
    Name equalsName = EQUALS.get(state);
    Symbol objectEquals = getOnlyMember(state.getSymtab().objectType, "equals");
    for (Type sup : state.getTypes().closure(type)) {
      if (isSameType(sup, state.getSymtab().objectType, state)) {
        continue;
      }
      for (Symbol sym : getEnclosedElements(sup.tsym)) {
        if (sym.getSimpleName().contentEquals(equalsName)) {
          if (sym.overrides(objectEquals, type.tsym, state.getTypes(), /* checkResult= */ false)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private static final Supplier<Name> EQUALS = memoize(state -> state.getName("equals"));
  private static final Supplier<Type> JAVA_LANG_THREAD = typeFromString("java.lang.Thread");
}
