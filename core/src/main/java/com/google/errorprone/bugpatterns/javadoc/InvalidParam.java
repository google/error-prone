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

package com.google.errorprone.bugpatterns.javadoc;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.bugpatterns.javadoc.Utils.diagnosticPosition;
import static com.google.errorprone.bugpatterns.javadoc.Utils.getBestMatch;
import static com.google.errorprone.bugpatterns.javadoc.Utils.getDocComment;
import static com.google.errorprone.bugpatterns.javadoc.Utils.getDocTreePath;
import static com.google.errorprone.bugpatterns.javadoc.Utils.replace;
import static com.google.errorprone.names.LevenshteinEditDistance.getEditDistance;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTree.Kind;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;
import com.sun.tools.javac.tree.DCTree.DCDocComment;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Matches incorrect Javadoc {@literal @}param tags.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
// TODO(ghm): Split this into the @param part and the @code part; the former is always right that
// there's a mistake, but the latter is based on a heuristic.
@BugPattern(
    name = "InvalidParam",
    summary = "This @param tag doesn't refer to a parameter of the method.",
    severity = WARNING,
    documentSuppression = false)
public final class InvalidParam extends BugChecker implements ClassTreeMatcher, MethodTreeMatcher {

  private static final Pattern POSSIBLE_PARAMETER = Pattern.compile("[a-z][A-Za-z0-9]*");

  /** Names which are often used in {@literal @}code blocks, and shouldn't be checked. */
  private static final ImmutableSet<String> EXCLUSIONS = ImmutableSet.of("true", "false");

  /**
   * Heuristic for the relative edit distance below which we report maybe-param {@literal @code}s.
   */
  private static final double LIKELY_PARAMETER_THRESHOLD = 0.25;

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    DocTreePath path = getDocTreePath(state);
    if (path != null) {
      ImmutableSet<String> parameters = ImmutableSet.of();
      ImmutableSet<String> typeParameters =
          classTree.getTypeParameters().stream()
              .map(t -> t.getName().toString())
              .collect(toImmutableSet());
      new ParamsChecker(state, classTree, parameters, typeParameters).scan(path, null);
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    DocTreePath path = getDocTreePath(state);
    if (path != null) {
      ImmutableSet<String> parameters =
          methodTree.getParameters().stream()
              .map(v -> v.getName().toString())
              .collect(toImmutableSet());
      ImmutableSet<String> typeParameters =
          methodTree.getTypeParameters().stream()
              .map(t -> t.getName().toString())
              .collect(toImmutableSet());
      new ParamsChecker(state, methodTree, parameters, typeParameters).scan(path, null);
    }
    return Description.NO_MATCH;
  }

  /** Checks that documented parameters match the method's parameter list. */
  private final class ParamsChecker extends DocTreePathScanner<Void, Void> {
    private final VisitorState state;

    private final ImmutableSet<String> documentedParameters;
    private final ImmutableSet<String> documentedTypeParameters;

    private final ImmutableSet<String> parameters;
    private final ImmutableSet<String> typeParameters;

    private ParamsChecker(
        VisitorState state,
        Tree tree,
        ImmutableSet<String> parameters,
        ImmutableSet<String> typeParameters) {
      this.state = state;
      DCDocComment dcDocComment = getDocComment(state, tree);
      this.documentedParameters =
          extractDocumentedParams(dcDocComment, /* isTypeParameter= */ false);
      this.documentedTypeParameters =
          extractDocumentedParams(dcDocComment, /* isTypeParameter= */ true);
      this.parameters = parameters;
      this.typeParameters = typeParameters;
    }

    @Override
    public Void visitParam(ParamTree paramTree, Void unused) {
      ImmutableSet<String> paramNames = paramTree.isTypeParameter() ? typeParameters : parameters;
      if (!paramNames.contains(paramTree.getName().toString())) {
        ImmutableSet<String> documentedParamNames =
            paramTree.isTypeParameter() ? documentedTypeParameters : documentedParameters;
        Set<String> undocumentedParameters = Sets.difference(paramNames, documentedParamNames);
        Optional<String> bestMatch =
            getBestMatch(
                paramTree.getName().toString(), /* maxEditDistance= */ 5, undocumentedParameters);
        String message = String.format("Parameter name `%s` is unknown.", paramTree.getName());
        state.reportMatch(
            bestMatch
                .map(
                    bm ->
                        buildDescription(diagnosticPosition(getCurrentPath(), state))
                            .setMessage(message + String.format(" Did you mean %s?", bm))
                            .addFix(replace(paramTree.getName(), bm, state))
                            .build())
                .orElse(
                    buildDescription(diagnosticPosition(getCurrentPath(), state))
                        .setMessage(message)
                        .addFix(replace(paramTree, "", state))
                        .build()));
      }
      return super.visitParam(paramTree, null);
    }

    @Override
    public Void visitLiteral(LiteralTree node, Void unused) {
      if (node.getKind() != Kind.CODE) {
        return super.visitLiteral(node, null);
      }
      String body = node.getBody().getBody();
      if (!POSSIBLE_PARAMETER.matcher(body).matches() || EXCLUSIONS.contains(body)) {
        return super.visitLiteral(node, null);
      }
      String bestMatch = null;
      int minDistance = Integer.MAX_VALUE;
      for (String parameter : parameters) {
        int distance = getEditDistance(body, parameter);
        if (distance < minDistance) {
          bestMatch = parameter;
          minDistance = distance;
        }
      }
      if (bestMatch != null
          && !bestMatch.equals(body)
          && (double) minDistance / body.length() <= LIKELY_PARAMETER_THRESHOLD) {
        String message =
            String.format(
                "`%s` is very close to the parameter `%s`. "
                    + "Did you mean to reference the parameter?",
                body, bestMatch);
        state.reportMatch(
            buildDescription(diagnosticPosition(getCurrentPath(), state))
                .setMessage(message)
                .addFix(replace(node.getBody(), bestMatch, state))
                .build());
      }
      return super.visitLiteral(node, null);
    }
  }

  private static ImmutableSet<String> extractDocumentedParams(
      DCDocComment docCommentTree, boolean isTypeParameter) {
    ImmutableSet.Builder<String> parameters = ImmutableSet.builder();
    for (DocTree docTree : docCommentTree.getBlockTags()) {
      if (!(docTree instanceof ParamTree)) {
        continue;
      }
      ParamTree paramTree = (ParamTree) docTree;
      if (paramTree.isTypeParameter() == isTypeParameter) {
        parameters.add(paramTree.getName().getName().toString());
      }
    }
    return parameters.build();
  }
}
