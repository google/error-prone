/*
 * Copyright 2014 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.findLast;
import static com.google.common.collect.Streams.stream;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.util.ASTHelpers.findSuperMethods;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static java.util.stream.Stream.concat;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.lang.model.element.Modifier;

/**
 * Verifies that methods marked {@link com.google.errorprone.annotations.ForOverride} are only
 * called from the defining class.
 *
 * <p>Specifically, all calls to the method have to occur within the context of the outermost class
 * where the method is defined.
 */
@BugPattern(
    name = "ForOverride",
    summary =
        "Method annotated @ForOverride must be protected or package-private and only invoked from "
            + "declaring class, or from an override of the method",
    severity = ERROR)
public class ForOverrideChecker extends BugChecker
    implements MethodInvocationTreeMatcher, MethodTreeMatcher {

  private static final String FOR_OVERRIDE = "com.google.errorprone.annotations.ForOverride";
  private static final String MESSAGE_BASE = "Method annotated @ForOverride ";

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    MethodSymbol method = ASTHelpers.getSymbol(tree);
    if (method == null) {
      return Description.NO_MATCH;
    }
    Type currentClass = getOutermostClass(state);

    if (method.isStatic() || method.isConstructor() || currentClass == null) {
      return Description.NO_MATCH;
    }
    // allow super.foo() calls to @ForOverride methods from overriding methods
    if (isSuperCall(currentClass, tree, state)) {
      MethodTree currentMethod = findDirectMethod(state.getPath());
      // currentMethod might be null if we are in a field initializer
      if (currentMethod != null) {
        // MethodSymbol.overrides doesn't check that names match, so we need to do that first.
        if (currentMethod.getName().equals(method.name)) {
          MethodSymbol currentMethodSymbol = ASTHelpers.getSymbol(currentMethod);
          if (currentMethodSymbol.overrides(
              method, (TypeSymbol) method.owner, state.getTypes(), /* checkResult= */ true)) {
            return Description.NO_MATCH;
          }
        }
      }
    }

    ImmutableList<MethodSymbol> overriddenMethods = getOverriddenMethods(state, method);

    for (Symbol overriddenMethod : overriddenMethods) {
      Type declaringClass = ASTHelpers.outermostClass(overriddenMethod).asType();
      if (!ASTHelpers.isSameType(declaringClass, currentClass, state)) {
        String customMessage =
            MESSAGE_BASE
                + "must not be invoked directly "
                + "(except by the declaring class, "
                + declaringClass
                + ")";
        return buildDescription(tree).setMessage(customMessage).build();
      }
    }

    return Description.NO_MATCH;
  }

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    MethodSymbol method = ASTHelpers.getSymbol(tree);

    if (method.isStatic() || method.isConstructor()) {
      return Description.NO_MATCH;
    }

    if (method.getModifiers().contains(Modifier.PUBLIC)
        || method.getModifiers().contains(Modifier.PRIVATE)) {
      ImmutableList<MethodSymbol> overriddenMethods = getOverriddenMethods(state, method);

      if (!overriddenMethods.isEmpty()) {
        MethodSymbol nearestForOverrideMethod = overriddenMethods.get(0);
        String customMessage = "must have protected or package-private visibility";
        if (nearestForOverrideMethod.equals(method)) {
          // The method itself is @ForOverride but is too visible
          customMessage = MESSAGE_BASE + customMessage;

        } else {
          // The method overrides an @ForOverride method and expands its visibility
          customMessage =
              String.format(
                  "Method overrides @ForOverride method %s.%s, so it %s",
                  nearestForOverrideMethod.enclClass(), nearestForOverrideMethod, customMessage);
        }
        return buildDescription(tree).setMessage(customMessage).build();
      }
    }

    return Description.NO_MATCH;
  }

  /**
   * Returns the method that 'directly' contains the leaf element of the given path.
   *
   * <p>By 'direct', we mean that if the leaf is part of a field initializer of a class, then it is
   * considered to not be part of any method.
   */
  @Nullable
  private static MethodTree findDirectMethod(TreePath path) {
    while (true) {
      path = path.getParentPath();
      if (path != null) {
        Tree leaf = path.getLeaf();
        if (leaf instanceof MethodTree) {
          return (MethodTree) leaf;
        }
        // if we find a ClassTree before a MethodTree, we must be an initializer
        if (leaf instanceof ClassTree) {
          return null;
        }
      } else {
        return null;
      }
    }
  }

  /** Returns true if this method invocation is of the form {@code super.foo()} */
  private static boolean isSuperCall(Type type, MethodInvocationTree tree, VisitorState state) {
    if (tree.getMethodSelect().getKind() == Kind.MEMBER_SELECT) {
      MemberSelectTree select = (MemberSelectTree) tree.getMethodSelect();
      if (select.getExpression().getKind() == Kind.IDENTIFIER) {
        IdentifierTree ident = (IdentifierTree) select.getExpression();
        return ident.getName().contentEquals("super");
      } else if (select.getExpression().getKind() == Kind.MEMBER_SELECT) {
        MemberSelectTree subSelect = (MemberSelectTree) select.getExpression();

        return subSelect.getIdentifier().contentEquals("super")
            && ASTHelpers.isSameType(ASTHelpers.getType(subSelect.getExpression()), type, state);
      }
    }
    return false;
  }

  /**
   * Get overridden @ForOverride methods.
   *
   * @param state the VisitorState
   * @param method the method to find overrides for
   * @return a list of methods annotated @ForOverride that the method overrides, including the
   *     method itself if it has the annotation
   */
  private static ImmutableList<MethodSymbol> getOverriddenMethods(
      VisitorState state, MethodSymbol method) {
    // Static methods cannot override, only overload.
    if (method.isStatic()) {
      throw new IllegalArgumentException(
          "getOverriddenMethods may not be called on a static method");
    }

    return concat(Stream.of(method), findSuperMethods(method, state.getTypes()).stream())
        .filter(member -> hasAnnotation(member, FOR_OVERRIDE, state))
        .collect(toImmutableList());
  }

  /** Get the outermost class/interface/enum of an element, or null if none. */
  private static Type getOutermostClass(VisitorState state) {
    return findLast(
            stream(state.getPath())
                .filter(t -> t instanceof ClassTree)
                .map(t -> ASTHelpers.getType(t)))
        .orElse(null);
  }
}
