/*
 * Copyright 2017 The Error Prone Authors.
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

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.isType;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.Arrays;
import javax.lang.model.element.Modifier;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@BugPattern(
    summary = "Overriding method is missing a call to overridden super method",
    severity = ERROR)
// TODO(eaftan): Add support for JDK methods that cannot be annotated, such as
// java.lang.Object#finalize and java.lang.Object#clone.
public class MissingSuperCall extends BugChecker
    implements AnnotationTreeMatcher, MethodTreeMatcher {

  private enum AnnotationType {
    ANDROID("android.support.annotation.CallSuper"),
    ANDROIDX("androidx.annotation.CallSuper"),
    ERROR_PRONE("com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper"),
    JSR305("javax.annotation.OverridingMethodsMustInvokeSuper"),
    FINDBUGS("edu.umd.cs.findbugs.annotations.OverrideMustInvoke");

    private final String fullyQualifiedName;

    AnnotationType(String fullyQualifiedName) {
      this.fullyQualifiedName = fullyQualifiedName;
    }

    String fullyQualifiedName() {
      return fullyQualifiedName;
    }

    String simpleName() {
      int index = fullyQualifiedName().lastIndexOf('.');
      if (index >= 0) {
        return fullyQualifiedName().substring(index + 1);
      } else {
        return fullyQualifiedName();
      }
    }
  }

  private static final Matcher<AnnotationTree> ANNOTATION_MATCHER =
      anyOf(
          Arrays.stream(AnnotationType.values())
              .map(anno -> isType(anno.fullyQualifiedName()))
              .collect(ImmutableList.toImmutableList()));

  /**
   * Prevents abstract methods from being annotated with {@code @CallSuper} et al. It doesn't make
   * sense to require overriders to call a method with no implementation.
   */
  @Override
  public Description matchAnnotation(AnnotationTree tree, VisitorState state) {
    if (!ANNOTATION_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    MethodTree methodTree = ASTHelpers.findEnclosingNode(state.getPath(), MethodTree.class);
    if (methodTree == null) {
      return Description.NO_MATCH;
    }

    MethodSymbol methodSym = ASTHelpers.getSymbol(methodTree);

    if (!methodSym.getModifiers().contains(Modifier.ABSTRACT)) {
      return Description.NO_MATCH;
    }

    // Match, find the matched annotation to use for the error message.
    Symbol annotationSym = ASTHelpers.getSymbol(tree);
    if (annotationSym == null) {
      return Description.NO_MATCH;
    }

    return buildDescription(tree)
        .setMessage(
            String.format(
                "@%s cannot be applied to an abstract method", annotationSym.getSimpleName()))
        .build();
  }

  /**
   * Matches a method that overrides a method that has been annotated with {@code @CallSuper} et
   * al., but does not call the super method.
   */
  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    MethodSymbol methodSym = ASTHelpers.getSymbol(tree);

    // Allow abstract methods.
    if (methodSym.getModifiers().contains(Modifier.ABSTRACT)) {
      return Description.NO_MATCH;
    }

    String annotatedSuperMethod = null;
    String matchedAnnotationSimpleName = null;
    for (MethodSymbol method : ASTHelpers.findSuperMethods(methodSym, state.getTypes())) {
      for (AnnotationType annotationType : AnnotationType.values()) {
        if (ASTHelpers.hasAnnotation(method, annotationType.fullyQualifiedName(), state)) {
          annotatedSuperMethod = getMethodName(method);
          matchedAnnotationSimpleName = annotationType.simpleName();
          break;
        }
      }
    }

    if (annotatedSuperMethod == null || matchedAnnotationSimpleName == null) {
      return Description.NO_MATCH;
    }

    TreeScanner<Boolean, Void> findSuper = new FindSuperTreeScanner(tree.getName().toString());
    if (findSuper.scan(tree, null)) {
      return Description.NO_MATCH;
    }

    return buildDescription(tree)
        .setMessage(
            String.format(
                "This method overrides %s, which is annotated with @%s, but does not call the "
                    + "super method",
                annotatedSuperMethod, matchedAnnotationSimpleName))
        .build();
  }

  /** Scans a tree looking for calls to a method that is overridden by the given one. */
  private static class FindSuperTreeScanner extends TreeScanner<Boolean, Void> {
    private final String overridingMethodName;

    private FindSuperTreeScanner(String overridingMethodName) {
      this.overridingMethodName = overridingMethodName;
    }

    @Override
    public Boolean visitClass(ClassTree node, Void unused) {
      // don't descend into classes
      return false;
    }

    @Override
    public Boolean visitLambdaExpression(LambdaExpressionTree node, Void unused) {
      // don't descend into lambdas
      return false;
    }

    @Override
    public Boolean visitMethodInvocation(MethodInvocationTree tree, Void unused) {
      boolean result = false;
      ExpressionTree methodSelect = tree.getMethodSelect();
      if (methodSelect instanceof MemberSelectTree memberSelect) {
        result =
            ASTHelpers.isSuper(memberSelect.getExpression())
                && memberSelect.getIdentifier().contentEquals(overridingMethodName);
      }
      return result || super.visitMethodInvocation(tree, null);
    }

    @Override
    public Boolean reduce(Boolean b1, Boolean b2) {
      return firstNonNull(b1, false) || firstNonNull(b2, false);
    }
  }

  /**
   * Given a {@link MethodSymbol}, returns a method name in the form "owningClass#methodName", e.g.
   * "java.util.function.Function#apply".
   */
  private static String getMethodName(MethodSymbol methodSym) {
    return String.format("%s#%s", methodSym.owner, methodSym.getSimpleName());
  }
}
