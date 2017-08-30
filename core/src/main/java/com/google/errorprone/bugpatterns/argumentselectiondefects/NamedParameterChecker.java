/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.argumentselectiondefects;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.bugpatterns.argumentselectiondefects.NamedParameterComment.MatchType;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.Commented;
import com.google.errorprone.util.Comments;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A bug checker to catch cases where a developer has annotated an argument with a comment to
 * indicate the intended parameter and the parameter name is wrong.
 *
 * @author andrewrice@google.com (Andrew Rice)
 */
@BugPattern(
  name = "NamedParameters",
  summary = "Parameter name in argument comment is incorrect",
  category = JDK,
  severity = WARNING
)
public class NamedParameterChecker extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return matchNewClassOrMethodInvocation(
        ASTHelpers.getSymbol(tree), Comments.findCommentsForArguments(tree, state), tree);
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    // TODO(b/65065109): look for the super class constructor for anonymous classes, as the
    // anonymous class constructor has synthetic parameter names.
    return matchNewClassOrMethodInvocation(
        ASTHelpers.getSymbol(tree), Comments.findCommentsForArguments(tree, state), tree);
  }

  private Description matchNewClassOrMethodInvocation(
      MethodSymbol symbol, ImmutableList<Commented<ExpressionTree>> arguments, Tree tree) {

    if (symbol == null) {
      return Description.NO_MATCH;
    }

    // if we don't have parameter names available then give up
    if (NamedParameterComment.containsSyntheticParameterName(symbol)) {
      return Description.NO_MATCH;
    }

    ImmutableList<LabelledArgument> labelledArguments =
        LabelledArgument.createFromParametersList(symbol.getParameters(), arguments);

    // Build fix
    // In general: If a comment is wrong but matches the parameter name of a different argument then
    // we suggest swapping the arguments. If a comment is wrong and matches nothing then we suggest
    // changing it

    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
    ImmutableList.Builder<String> incorrectParameterDescriptions = ImmutableList.builder();
    for (LabelledArgument labelledArgument : labelledArguments) {
      switch (labelledArgument.matchedComment().matchType()) {
        case NOT_ANNOTATED:
        case EXACT_MATCH:
          break;
        case APPROXIMATE_MATCH:
          // For now, don't be prescriptive about:
          // f(foo /* this comment contains the parameter name */);
          break;
        case BAD_MATCH:
          Comment badLabel = labelledArgument.matchedComment().comment();
          Optional<LabelledArgument> maybeGoodTarget =
              findGoodSwap(labelledArgument, labelledArguments);

          if (maybeGoodTarget.isPresent()) {
            LabelledArgument argumentWithCorrectLabel = maybeGoodTarget.get();
            fixBuilder.swap(
                labelledArgument.actualParameter().tree(),
                argumentWithCorrectLabel.actualParameter().tree());

            if (argumentWithCorrectLabel.matchedComment().matchType() == MatchType.NOT_ANNOTATED) {
              removeComment(badLabel, fixBuilder);
              addComment(argumentWithCorrectLabel, fixBuilder);
            } else {
              replaceComment(
                  badLabel,
                  argumentWithCorrectLabel.matchedComment().comment().getText(),
                  fixBuilder);
            }
          } else {
            // there were no matches so maybe the comment is wrong - suggest a fix to change it
            replaceComment(
                badLabel,
                NamedParameterComment.toCommentText(labelledArgument.parameterName()),
                fixBuilder);
          }
          incorrectParameterDescriptions.add(
              String.format(
                  "`%s` does not match formal parameter name `%s`",
                  badLabel.getText(), labelledArgument.parameterName()));
          break;
      }
    }

    if (fixBuilder.isEmpty()) {
      return Description.NO_MATCH;
    }

    return buildDescription(tree)
        .setMessage(
            "Parameters with incorrectly labelled arguments: "
                + incorrectParameterDescriptions.build().stream().collect(Collectors.joining(", ")))
        .addFix(fixBuilder.build())
        .build();
  }

  private static void addComment(
      LabelledArgument labelledArgument, SuggestedFix.Builder fixBuilder) {
    fixBuilder.prefixWith(
        labelledArgument.actualParameter().tree(),
        NamedParameterComment.toCommentText(labelledArgument.parameterName()));
  }

  /**
   * Replace the given comment with the replacementText. The replacement text is used verbatim and
   * so should begin and end with block comment delimiters
   */
  private static void replaceComment(
      Comment comment, String replacementText, SuggestedFix.Builder fixBuilder) {
    int commentStart = comment.getSourcePos(0);
    int commentEnd = commentStart + comment.getText().length();
    fixBuilder.replace(commentStart, commentEnd, replacementText);
  }

  private static void removeComment(Comment comment, SuggestedFix.Builder fixBuilder) {
    replaceComment(comment, "", fixBuilder);
  }

  /**
   * Search all arguments for a target argument which would make a good swap with the source
   * argument. A good swap would result in the label for the source argument (exactly) matching the
   * parameter name of the target argument and the label of the target argument matching the
   * parameter name of the source argument. If the target argument is not labelled then we accept
   * that too.
   */
  private static Optional<LabelledArgument> findGoodSwap(
      LabelledArgument source, ImmutableList<LabelledArgument> allArguments) {
    for (LabelledArgument target : allArguments) {
      if (source.equals(target)) {
        continue;
      }

      boolean sourceLabelMatchesTarget =
          NamedParameterComment.match(source.actualParameter(), target.parameterName()).matchType()
              == MatchType.EXACT_MATCH;

      MatchType targetCommentMatch =
          NamedParameterComment.match(target.actualParameter(), source.parameterName()).matchType();

      boolean targetLabelMatchesSource =
          targetCommentMatch == MatchType.EXACT_MATCH
              || targetCommentMatch == MatchType.NOT_ANNOTATED;

      if (sourceLabelMatchesTarget && targetLabelMatchesSource) {
        return Optional.of(target);
      }
    }
    return Optional.empty();
  }

  /** Information about an argument, the name attached to it with a comment */
  @AutoValue
  abstract static class LabelledArgument {

    abstract String parameterName();

    abstract Commented<ExpressionTree> actualParameter();

    abstract NamedParameterComment.MatchedComment matchedComment();

    static ImmutableList<LabelledArgument> createFromParametersList(
        List<VarSymbol> parameters, ImmutableList<Commented<ExpressionTree>> actualParameters) {
      return Streams.zip(
              actualParameters.stream(),
              parameters.stream(),
              (actualParameter, formalParameter) -> {
                String formalParameterName = formalParameter.getSimpleName().toString();
                return new AutoValue_NamedParameterChecker_LabelledArgument(
                    formalParameterName,
                    actualParameter,
                    NamedParameterComment.match(actualParameter, formalParameterName));
              })
          .collect(toImmutableList());
    }
  }
}
