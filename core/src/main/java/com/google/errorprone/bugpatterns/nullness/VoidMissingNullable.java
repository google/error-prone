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

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.fixByAddingNullableAnnotation;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.hasNoExplicitType;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.isVoid;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static javax.lang.model.element.ElementKind.LOCAL_VARIABLE;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ParameterizedTypeTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnnotations;
import com.google.errorprone.matchers.Description;
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
@BugPattern(
    name = "VoidMissingNullable",
    summary = "The type Void is not annotated @Nullable",
    severity = SUGGESTION)
public class VoidMissingNullable extends BugChecker
    implements ParameterizedTypeTreeMatcher, MethodTreeMatcher, VariableTreeMatcher {
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
    for (Tree tree : parameterizedTypeTree.getTypeArguments()) {
      if (tree instanceof WildcardTree) {
        tree = ((WildcardTree) tree).getBound();
      }
      checkType(getType(tree), tree, state);
    }
    return NO_MATCH; // Any reports were made through state.reportMatch.
  }

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    MethodSymbol sym = getSymbol(tree);
    if (sym == null) {
      return NO_MATCH;
    }
    return matchType(sym.getReturnType(), sym, tree, state);
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
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
    if (sym == null) {
      return NO_MATCH;
    }
    if (sym.getKind() == LOCAL_VARIABLE) {
      return NO_MATCH; // Local variables are discussed in the comment about `var`, etc. above.
    }
    return matchType(sym.type, sym, tree, state);
  }

  private void checkType(Type type, Tree treeToAnnotate, VisitorState state) {
    if (!isVoid(type, state)) {
      return;
    }
    if (NullnessAnnotations.fromAnnotationsOn(type).orElse(null) == Nullness.NULLABLE) {
      return;
    }
    state.reportMatch(describeMatch(treeToAnnotate, state));
  }

  private Description matchType(Type type, Symbol sym, Tree treeToAnnotate, VisitorState state) {
    if (!isVoid(type, state)) {
      return NO_MATCH;
    }
    if (NullnessAnnotations.fromAnnotationsOn(sym).orElse(null) == Nullness.NULLABLE) {
      return NO_MATCH;
    }
    return describeMatch(treeToAnnotate, state);
  }

  private Description describeMatch(Tree treeToAnnotate, VisitorState state) {
    /*
     * TODO(cpovirk): For the case of type arguments, this fix makes sense only if we use a
     * @Nullable that is a type-use annotation. If non-type-use annotations, don't suggest a change?
     * Or run this refactoring as part of a suite that migrates from existing annotations to
     * type-use annotations? For now, we rely on users to patch things up.
     */
    return describeMatch(treeToAnnotate, fixByAddingNullableAnnotation(state, treeToAnnotate));
  }
}
