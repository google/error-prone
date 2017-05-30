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
import static com.google.errorprone.matchers.Matchers.symbolHasAnnotation;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.RequiresNamedParameters;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.bugpatterns.argumentselectiondefects.NamedParameterComment.MatchType;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
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
import com.sun.tools.javac.parser.Tokens.Comment.CommentStyle;
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
  summary = "Parameter name in argument comment is missing or incorrect",
  explanation =
      "For clarity, and to avoid potentially incorrectly swapping arguments, arguments may be "
          + "explicitly matched to their parameter by preceding them with a block comment "
          + "containing the parameter name followed by an equals sign (\"=\"). Mismatches between "
          + "the name in the comment and the actual name will then cause a compilation error. If "
          + "the called method is annotated with RequiresNamedParameters then an error will occur "
          + "if any names are omitted.",
  category = JDK,
  severity = WARNING
)
public class NamedParameterChecker extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return matchNewClassOrMethodInvocation(
        ASTHelpers.getSymbol(tree), Comments.findCommentsForArguments(tree, state), tree, state);
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    return matchNewClassOrMethodInvocation(
        ASTHelpers.getSymbol(tree), Comments.findCommentsForArguments(tree, state), tree, state);
  }

  private Description matchNewClassOrMethodInvocation(
      MethodSymbol symbol,
      ImmutableList<Commented<ExpressionTree>> arguments,
      Tree tree,
      VisitorState state) {

    if (symbol == null) {
      return Description.NO_MATCH;
    }

    boolean commentsRequired = hasRequiresNamedParametersAnnotation().matches(tree, state);

    // if we don't have parameter names available then raise an error if comments required, else
    // silently ignore
    List<VarSymbol> parameters = symbol.getParameters();
    if (containsSyntheticParameterName(parameters)) {
      return commentsRequired
          ? buildDescription(tree)
              .setMessage(
                  "Method requires parameter name comments but parameter names are not available.")
              .build()
          : Description.NO_MATCH;
    }

    ImmutableList<LabelledArgument> labelledArguments =
        LabelledArgument.createFromParametersList(parameters, arguments);

    ImmutableList<LabelledArgument> incorrectlyLabelledArguments =
        labelledArguments
            .stream()
            .filter(labelledArgument -> !labelledArgument.isCorrectlyAnnotated(commentsRequired))
            .collect(toImmutableList());

    if (incorrectlyLabelledArguments.isEmpty()) {
      return Description.NO_MATCH;
    }

    // Build fix
    // In general: if a comment is missing and it should be there then we suggest adding it
    // If a comment is wrong but matches the parameter name of a different argument then we suggest
    // swapping the arguments. If a comment is wrong and matches nothing then we suggest changing it

    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
    for (LabelledArgument argumentWithBadLabel : incorrectlyLabelledArguments) {
      switch (argumentWithBadLabel.match()) {
        case NOT_ANNOTATED:
          fixBuilder.prefixWith(
              argumentWithBadLabel.actualParameter().tree(),
              NamedParameterComment.toCommentText(argumentWithBadLabel.parameterName()));
          break;
        case BAD_MATCH:
        case APPROXIMATE_MATCH:
          // we know that this has a comment because it was a bad match
          Comment badLabel = argumentWithBadLabel.label().get();
          Optional<LabelledArgument> maybeGoodTarget =
              findGoodSwap(argumentWithBadLabel, labelledArguments);

          if (maybeGoodTarget.isPresent()) {
            LabelledArgument argumentWithCorrectLabel = maybeGoodTarget.get();
            fixBuilder.swap(
                argumentWithBadLabel.actualParameter().tree(),
                argumentWithCorrectLabel.actualParameter().tree());

            Optional<Comment> correctLabel = argumentWithCorrectLabel.label();
            if (correctLabel.isPresent()) {
              replaceComment(badLabel, correctLabel.get().getText(), fixBuilder);
            } else {
              replaceComment(badLabel, "", fixBuilder);
              fixBuilder.prefixWith(
                  argumentWithCorrectLabel.actualParameter().tree(),
                  NamedParameterComment.toCommentText(argumentWithCorrectLabel.parameterName()));
            }
          } else {
            // there were no matches so maybe the comment is wrong - suggest a fix to change it
            replaceComment(
                badLabel,
                NamedParameterComment.toCommentText(argumentWithBadLabel.parameterName()),
                fixBuilder);
          }
          break;
        case EXACT_MATCH:
          throw new IllegalArgumentException(
              "There should be no good matches in the list of bad matches");
      }
    }

    return buildDescription(tree)
        .setMessage(
            "Parameters with incorrectly labelled arguments: "
                + incorrectlyLabelledArguments
                    .stream()
                    .map(NamedParameterChecker::describeIncorrectlyLabelledArgument)
                    .collect(Collectors.joining(", ")))
        .addFix(fixBuilder.build())
        .build();
  }

  private static String describeIncorrectlyLabelledArgument(LabelledArgument p) {
    switch (p.match()) {
      case NOT_ANNOTATED:
      case APPROXIMATE_MATCH:
        return String.format("%s (missing name label)", p.parameterName());
      case BAD_MATCH:
        return String.format("%s (label doesn't match parameter name)", p.parameterName());
      case EXACT_MATCH:
        // fall through
    }
    throw new IllegalArgumentException("Impossible match type in list of bad matches");
  }

  private static boolean containsSyntheticParameterName(List<VarSymbol> parameters) {
    return parameters
        .stream()
        .map(p -> p.getSimpleName().toString())
        .anyMatch(p -> p.matches("arg[0-9]"));
  }

  private static void replaceComment(
      Comment comment, String replacementText, SuggestedFix.Builder fixBuilder) {
    int commentStart = comment.getSourcePos(0);
    int commentEnd = commentStart + comment.getText().length();
    fixBuilder.replace(commentStart, commentEnd, replacementText);
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
          NamedParameterComment.match(source.actualParameter(), target.parameterName())
              == MatchType.EXACT_MATCH;

      MatchType targetCommentMatch =
          NamedParameterComment.match(target.actualParameter(), source.parameterName());

      boolean targetLabelMatchesSource =
          targetCommentMatch == MatchType.EXACT_MATCH
              || targetCommentMatch == MatchType.NOT_ANNOTATED;

      if (sourceLabelMatchesTarget && targetLabelMatchesSource) {
        return Optional.of(target);
      }
    }
    return Optional.empty();
  }

  private static Matcher<Tree> hasRequiresNamedParametersAnnotation() {
    return symbolHasAnnotation(RequiresNamedParameters.class.getCanonicalName());
  }

  /** Information about an argument, the name attached to it with a comment */
  @AutoValue
  abstract static class LabelledArgument {

    abstract String parameterName();

    abstract Commented<ExpressionTree> actualParameter();

    abstract MatchType match();

    boolean isCorrectlyAnnotated(boolean commentRequired) {
      switch (match()) {
        case EXACT_MATCH:
          return true;
        case BAD_MATCH:
        case APPROXIMATE_MATCH:
          return false;
        case NOT_ANNOTATED:
          return !commentRequired;
      }
      return false;
    }

    Optional<Comment> label() {
      return Streams.findLast(
          actualParameter()
              .beforeComments()
              .stream()
              .filter(c -> c.getStyle() == CommentStyle.BLOCK));
    }

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
