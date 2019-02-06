/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.isVoidType;
import static com.google.errorprone.matchers.Matchers.methodHasParameters;
import static com.google.errorprone.matchers.Matchers.methodIsNamed;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.suppliers.Suppliers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;

/** Bugpattern to detect unused declarations. */
@BugPattern(
    name = "UnusedMethod",
    altNames = {"Unused", "unused", "UnusedParameters"},
    summary = "Unused.",
    providesFix = REQUIRES_HUMAN_ATTENTION,
    severity = WARNING,
    documentSuppression = false)
public final class UnusedMethod extends BugChecker implements CompilationUnitTreeMatcher {
  private static final String GWT_JAVASCRIPT_OBJECT = "com.google.gwt.core.client.JavaScriptObject";
  private static final String EXEMPT_PREFIX = "unused";
  private static final String JUNIT_PARAMS_VALUE = "value";
  private static final String JUNIT_PARAMS_ANNOTATION_TYPE = "junitparams.Parameters";

  private static final Supplier<Type> OBJECT = Suppliers.typeFromString("java.lang.Object");

  /** Method signature of special methods. */
  private static final Matcher<MethodTree> SPECIAL_METHODS =
      anyOf(
          allOf(
              methodIsNamed("readObject"),
              methodHasParameters(isSameType("java.io.ObjectInputStream"))),
          allOf(
              methodIsNamed("writeObject"),
              methodHasParameters(isSameType("java.io.ObjectOutputStream"))),
          allOf(methodIsNamed("readObjectNoData"), methodReturns(isVoidType())),
          allOf(methodIsNamed("readResolve"), methodReturns(OBJECT)),
          allOf(methodIsNamed("writeReplace"), methodReturns(OBJECT)));


  private static final ImmutableSet<String> EXEMPTING_METHOD_ANNOTATIONS =
      ImmutableSet.of(
          "com.google.inject.Provides",
          "com.google.inject.Inject",
          "javax.inject.Inject");

  /** The set of types exempting a type that is extending or implementing them. */
  private static final ImmutableSet<String> EXEMPTING_SUPER_TYPES =
      ImmutableSet.of(
          );

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    // Map of symbols to method declarations. Initially this is a map of all of the methods. As we
    // go we remove those variables which are used.
    Map<Symbol, TreePath> unusedMethods = new HashMap<>();

    // We will skip reporting on the whole compilation if there are any native methods found.
    // Use a TreeScanner to find all local variables and fields.
    if (hasNativeMethods(tree)) {
      return Description.NO_MATCH;
    }
    AtomicBoolean ignoreUnusedMethods = new AtomicBoolean(false);

    class MethodFinder extends TreePathScanner<Void, Void> {
      @Override
      public Void visitClass(ClassTree tree, Void unused) {
        if (isSuppressed(tree) || exemptedBySuperType(getType(tree), state)) {
          return null;
        }
        return super.visitClass(tree, null);
      }

      private boolean exemptedBySuperType(Type type, VisitorState state) {
        return EXEMPTING_SUPER_TYPES.stream()
            .anyMatch(t -> isSubtype(type, Suppliers.typeFromString(t).get(state), state));
      }

      @Override
      public Void visitMethod(MethodTree tree, Void unused) {
        if (hasJUnitParamsParametersForMethodAnnotation(tree.getModifiers().getAnnotations())) {
          // Since this method uses @Parameters, there will be another method that appears to
          // be unused. Don't warn about unusedMethods at all in this case.
          ignoreUnusedMethods.set(true);
        }

        if (isSuppressed(tree)) {
          // @SuppressWarnings("unused") applies to the entire AST, not just the symbol it's bound
          // to.  Skip the whole method.
          return null;
        }

        if (isMethodSymbolEligibleForChecking(tree)) {
          unusedMethods.put(getSymbol(tree), getCurrentPath());
        }
        return super.visitMethod(tree, unused);
      }

      private boolean hasJUnitParamsParametersForMethodAnnotation(
          Collection<? extends AnnotationTree> annotations) {
        for (AnnotationTree tree : annotations) {
          JCAnnotation annotation = (JCAnnotation) tree;
          if (annotation.getAnnotationType().type != null
              && annotation
                  .getAnnotationType()
                  .type
                  .toString()
                  .equals(JUNIT_PARAMS_ANNOTATION_TYPE)) {
            if (annotation.getArguments().isEmpty()) {
              // @Parameters, which uses implicit provider methods
              return true;
            }
            for (JCExpression arg : annotation.getArguments()) {
              if (arg.getKind() != Kind.ASSIGNMENT) {
                // Implicit value annotation, e.g. @Parameters({"1"}); no exemption required.
                return false;
              }
              JCExpression var = ((JCAssign) arg).getVariable();
              if (var.getKind() == Kind.IDENTIFIER) {
                // Anything that is not @Parameters(value = ...), e.g.
                // @Parameters(source = ...) or @Parameters(method = ...)
                if (!((IdentifierTree) var).getName().contentEquals(JUNIT_PARAMS_VALUE)) {
                  return true;
                }
              }
            }
          }
        }
        return false;
      }

      private boolean isMethodSymbolEligibleForChecking(MethodTree tree) {
        if (exemptedByName(tree.getName())) {
          return false;
        }
        // Assume the method is called if annotated with a called-reflectively annotation.
        if (exemptedByAnnotation(tree.getModifiers().getAnnotations(), state)) {
          return false;
        }
        // Skip constructors and special methods.
        MethodSymbol methodSymbol = getSymbol(tree);
        if (methodSymbol == null
            || methodSymbol.getKind() == ElementKind.CONSTRUCTOR
            || SPECIAL_METHODS.matches(tree, state)) {
          return false;
        }

        // Ignore this method if the last parameter is a GWT JavaScriptObject.
        if (!tree.getParameters().isEmpty()) {
          Type lastParamType = getType(getLast(tree.getParameters()));
          if (lastParamType != null && lastParamType.toString().equals(GWT_JAVASCRIPT_OBJECT)) {
            return false;
          }
        }

        return tree.getModifiers().getFlags().contains(Modifier.PRIVATE);
      }
    }
    new MethodFinder().scan(state.getPath(), null);

    class FilterUsedMethods extends TreePathScanner<Void, Void> {
      @Override
      public Void visitMemberSelect(MemberSelectTree memberSelectTree, Void unused) {
        Symbol symbol = getSymbol(memberSelectTree);
        unusedMethods.remove(symbol);
        return super.visitMemberSelect(memberSelectTree, null);
      }

      @Override
      public Void visitMemberReference(MemberReferenceTree tree, Void unused) {
        super.visitMemberReference(tree, null);
        MethodSymbol symbol = getSymbol(tree);
        unusedMethods.remove(symbol);
        if (symbol != null) {
          symbol.getParameters().forEach(unusedMethods::remove);
        }
        return null;
      }

      @Override
      public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
        Symbol methodSymbol = getSymbol(tree);
        if (methodSymbol != null) {
          unusedMethods.remove(methodSymbol);
        }
        super.visitMethodInvocation(tree, null);
        return null;
      }
    }

    new FilterUsedMethods().scan(state.getPath(), null);

    if (ignoreUnusedMethods.get()) {
      return Description.NO_MATCH;
    }

    for (TreePath unusedPath : unusedMethods.values()) {
      Tree unusedTree = unusedPath.getLeaf();
      String message =
          String.format("Private method '%s' is never used.", ((MethodTree) unusedTree).getName());
      state.reportMatch(
          buildDescription(unusedTree)
              .addFix(SuggestedFixes.replaceIncludingComments(unusedPath, "", state))
              .setMessage(message)
              .build());
    }
    return Description.NO_MATCH;
  }

  static boolean hasNativeMethods(CompilationUnitTree tree) {
    AtomicBoolean hasAnyNativeMethods = new AtomicBoolean(false);
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitMethod(MethodTree tree, Void unused) {
        if (tree.getModifiers().getFlags().contains(Modifier.NATIVE)) {
          hasAnyNativeMethods.set(true);
        }
        return null;
      }
    }.scan(tree, null);
    return hasAnyNativeMethods.get();
  }

  /**
   * Looks at the list of {@code annotations} and see if there is any annotation which exists {@code
   * exemptingAnnotations}.
   */
  private static boolean exemptedByAnnotation(
      List<? extends AnnotationTree> annotations,
      VisitorState state) {
    for (AnnotationTree annotation : annotations) {
      if (((JCAnnotation) annotation).type != null) {
        TypeSymbol tsym = ((JCAnnotation) annotation).type.tsym;
        if (EXEMPTING_METHOD_ANNOTATIONS.contains(tsym.getQualifiedName().toString())) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean exemptedByName(Name name) {
    return Ascii.toLowerCase(name.toString()).startsWith(EXEMPT_PREFIX);
  }
}
