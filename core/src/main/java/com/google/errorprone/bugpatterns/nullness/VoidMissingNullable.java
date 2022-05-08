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

package com.google.errorprone.bugpatterns.nullness;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.fixByAddingNullableAnnotationToReturnType;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.fixByAddingNullableAnnotationToType;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.fixByAnnotatingTypeUseOnlyLocationWithNullableAnnotation;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.isVoid;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.nullnessChecksShouldBeConservative;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasNoExplicitType;
import static com.sun.source.tree.Tree.Kind.METHOD;
import static javax.lang.model.element.ElementKind.LOCAL_VARIABLE;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ParameterizedTypeTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnnotations;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WildcardTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(summary = "The type Void is not annotated @Nullable", severity = SUGGESTION)
public class VoidMissingNullable extends BugChecker
    implements ParameterizedTypeTreeMatcher, MethodTreeMatcher, VariableTreeMatcher {
  private final boolean beingConservative;

  public VoidMissingNullable(ErrorProneFlags flags) {
    this.beingConservative = nullnessChecksShouldBeConservative(flags);
  }

  /*
   * TODO(cpovirk): Handle `Void[]`, probably mostly in casts, while avoiding `Void[].class`.
   *
   * (We're missing other cases, too, like a hypothetical `? extends Void & Foo`.)
   *
   * If we end up wanting to cover more cases, then we may want to rework this checker to be a
   * matcher of IdentifierTree and MemberSelectTree that looks at the parent in the TreePath for an
   * AnnotatedTypeTree with the appropriate annotations. However, that approach would still require
   * special cases for methods and variables: Annotations are attached to the method/variable rather
   * than the type (sensibly so for nullness declaration annotations; less sensibly so for nullness
   * type-use annotations). Thus, we'd need to look not just for AnnotatedTypeTree but for
   * MethodTree and VariableTree, as well. That might still pay off if we start caring about cases
   * like Void[], but those cases may be rare enough that we don't need to care.
   */

  @Override
  public Description matchParameterizedType(
      ParameterizedTypeTree parameterizedTypeTree, VisitorState state) {
    if (beingConservative && state.errorProneOptions().isTestOnlyTarget()) {
      return NO_MATCH;
    }

    if (beingConservative && !isInNullMarkedScope(state)) {
      return NO_MATCH;
    }

    for (Tree tree : parameterizedTypeTree.getTypeArguments()) {
      if (tree instanceof WildcardTree) {
        tree = ((WildcardTree) tree).getBound();
      }
      checkTree(tree, state);
    }
    return NO_MATCH; // Any reports were made through state.reportMatch.
  }

  /*
   * TODO(cpovirk): Consider promoting this variant of isInNullMarkedScope to live in NullnessUtils
   * alongside the main variant. But note that it may be even more expensive than the main variant,
   * and see the more ambitious alternative TODO(cpovirk): in that file.
   */
  private static boolean isInNullMarkedScope(VisitorState state) {
    for (Tree tree : state.getPath()) {
      if (tree.getKind().asInterface().equals(ClassTree.class) || tree.getKind() == METHOD) {
        Symbol enclosingElement = getSymbol(tree);
        if (tree == null) {
          continue;
        }
        return NullnessUtils.isInNullMarkedScope(enclosingElement, state);
      }
    }
    throw new AssertionError(
        "parameterized type without enclosing element: " + Iterables.toString(state.getPath()));
  }

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (beingConservative && state.errorProneOptions().isTestOnlyTarget()) {
      return NO_MATCH;
    }

    MethodSymbol sym = getSymbol(tree);
    if (!typeMatches(sym.getReturnType(), sym, state)) {
      return NO_MATCH;
    }
    if (beingConservative && !NullnessUtils.isInNullMarkedScope(sym, state)) {
      return NO_MATCH;
    }
    return describeMatch(tree, fixByAddingNullableAnnotationToReturnType(state, tree));
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    if (beingConservative && state.errorProneOptions().isTestOnlyTarget()) {
      return NO_MATCH;
    }

    if (hasNoExplicitType(tree, state)) {
      /*
       * In the case of `var`, a declaration-annotation @Nullable would be valid. But a type-use
       * @Nullable would not be. But more importantly, we expect that tools will infer the
       * "top-level" nullness of all local variables, `var` and otherwise, without ever requiring a
       * @Nullable annotation on them.
       */
      return NO_MATCH;
    }
    VarSymbol sym = getSymbol(tree);
    if (sym.getKind() == LOCAL_VARIABLE) {
      return NO_MATCH; // Local variables are discussed in the comment about `var`, etc. above.
    }
    if (!typeMatches(sym.type, sym, state)) {
      return NO_MATCH;
    }
    if (beingConservative && !NullnessUtils.isInNullMarkedScope(sym, state)) {
      return NO_MATCH;
    }
    SuggestedFix fix = fixByAddingNullableAnnotationToType(state, tree);
    if (fix.isEmpty()) {
      return NO_MATCH;
    }
    return describeMatch(tree, fix);
  }

  private void checkTree(Tree tree, VisitorState state) {
    if (!typeMatches(getType(tree), state)) {
      return;
    }
    /*
     * Redundant-looking check required for anonymous classes because JCTree.type doesn't show the
     * annotations in that case -- presumably a bug?
     *
     * TODO(cpovirk): Provide this pair of checks as NullnessAnnotations.fromAnnotationsOn(Tree),
     * which might also be useful for a hypothetical future TypeArgumentMissingNullable?
     */
    if (NullnessAnnotations.fromAnnotations(annotationsIfAnnotatedTypeTree(tree)).orElse(null)
        == Nullness.NULLABLE) {
      return;
    }
    SuggestedFix fix = fixByAnnotatingTypeUseOnlyLocationWithNullableAnnotation(state, tree);
    if (fix.isEmpty()) {
      return;
    }
    state.reportMatch(describeMatch(tree, fix));
  }

  private boolean typeMatches(Type type, Symbol sym, VisitorState state) {
    return isVoid(type, state)
        && NullnessAnnotations.fromAnnotationsOn(sym).orElse(null) != Nullness.NULLABLE;
  }

  /**
   * Like the other overload but without looking for annotations on the Symbol (like declaration
   * annotations on a method or variable).
   */
  private boolean typeMatches(Type type, VisitorState state) {
    return isVoid(type, state)
        && NullnessAnnotations.fromAnnotationsOn(type).orElse(null) != Nullness.NULLABLE;
  }

  private static ImmutableList<String> annotationsIfAnnotatedTypeTree(Tree tree) {
    if (tree instanceof AnnotatedTypeTree) {
      AnnotatedTypeTree annotated = ((AnnotatedTypeTree) tree);
      return annotated.getAnnotations().stream()
          .map(ASTHelpers::getAnnotationName)
          .collect(toImmutableList());
    }
    return ImmutableList.of();
  }
}
