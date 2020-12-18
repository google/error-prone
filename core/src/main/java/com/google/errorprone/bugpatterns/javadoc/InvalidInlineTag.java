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
import static com.google.common.collect.Iterables.getFirst;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.bugpatterns.javadoc.JavadocTag.inlineTag;
import static com.google.errorprone.bugpatterns.javadoc.Utils.diagnosticPosition;
import static com.google.errorprone.bugpatterns.javadoc.Utils.getDiagnosticPosition;
import static java.util.stream.Collectors.joining;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.bugpatterns.javadoc.JavadocTag.TagType;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.FindIdentifiers;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ErroneousTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.UnknownInlineTagTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;
import com.sun.tools.javac.code.Kinds.KindSelector;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.tree.DCTree.DCDocComment;
import com.sun.tools.javac.tree.DCTree.DCInlineTag;
import com.sun.tools.javac.tree.DCTree.DCText;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Matches invalid Javadoc tags, and tries to suggest fixes.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@BugPattern(
    name = "InvalidInlineTag",
    summary = "This tag is invalid.",
    severity = WARNING,
    tags = StandardTags.STYLE,
    documentSuppression = false)
public final class InvalidInlineTag extends BugChecker
    implements ClassTreeMatcher, MethodTreeMatcher, VariableTreeMatcher {

  private static final String INVALID_TAG_IS_PARAMETER_NAME =
      "@%1$s is not a valid tag, but is a parameter name. "
          + "Use {@code %1%s} to refer to parameter names inline.";

  private static final Pattern PARAM_MATCHER = Pattern.compile("\\{?@param ([a-zA-Z0-9]+)}?");

  private static final Splitter DOT_SPLITTER = Splitter.on('.');

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    DocTreePath path = Utils.getDocTreePath(state);
    if (path != null) {
      ImmutableSet<String> parameters = ImmutableSet.of();
      new InvalidTagChecker(state, JavadocTag.VALID_CLASS_TAGS, parameters).scan(path, null);
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    DocTreePath path = Utils.getDocTreePath(state);
    if (path != null) {
      ImmutableSet<String> parameters =
          methodTree.getParameters().stream()
              .map(v -> v.getName().toString())
              .collect(toImmutableSet());
      new InvalidTagChecker(state, JavadocTag.VALID_METHOD_TAGS, parameters).scan(path, null);
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchVariable(VariableTree variableTree, VisitorState state) {
    DocTreePath path = Utils.getDocTreePath(state);
    if (path != null) {
      new InvalidTagChecker(
              state, JavadocTag.VALID_VARIABLE_TAGS, /* parameters= */ ImmutableSet.of())
          .scan(path, null);
    }
    return Description.NO_MATCH;
  }

  private final class InvalidTagChecker extends DocTreePathScanner<Void, Void> {
    private final VisitorState state;

    private final ImmutableSet<JavadocTag> validTags;
    private final ImmutableSet<String> parameters;

    private final Pattern misplacedCurly;
    private final Pattern parensRatherThanCurly;

    private final Set<DocTree> fixedTags = new HashSet<>();

    private InvalidTagChecker(
        VisitorState state, ImmutableSet<JavadocTag> validTags, ImmutableSet<String> parameters) {
      this.state = state;
      this.validTags = validTags;
      this.parameters = parameters;
      String validInlineTags =
          validTags.stream()
              .filter(tag -> tag.type() == TagType.INLINE)
              .map(JavadocTag::name)
              .collect(joining("|"));
      this.misplacedCurly = Pattern.compile(String.format("@(%s)\\{", validInlineTags));
      this.parensRatherThanCurly = Pattern.compile(String.format("\\(@(%s)", validInlineTags));
    }

    @Override
    public Void visitErroneous(ErroneousTree erroneousTree, Void unused) {
      Matcher matcher = PARAM_MATCHER.matcher(erroneousTree.getBody());
      if (matcher.find()) {
        String parameterName = matcher.group(1);
        if (parameters.contains(parameterName)) {
          String message =
              String.format(
                  "@param cannot be used inline to refer to parameters; {@code %s} is recommended",
                  parameterName);
          state.reportMatch(
              buildDescription(diagnosticPosition(getCurrentPath(), state))
                  .setMessage(message)
                  .addFix(
                      Utils.replace(
                          erroneousTree, String.format("{@code %s}", parameterName), state))
                  .build());
        }
      }
      return null;
    }

    @Override
    public Void visitText(TextTree node, Void unused) {
      handleMalformedTags(node);
      handleIncorrectParens(node);
      handleDanglingParams(node);
      return super.visitText(node, null);
    }

    private void handleMalformedTags(TextTree node) {
      String body = node.getBody();
      Matcher matcher = misplacedCurly.matcher(body);
      Comment comment = ((DCDocComment) getCurrentPath().getDocComment()).comment;
      while (matcher.find()) {
        int beforeAt = comment.getSourcePos(((DCText) node).pos + matcher.start());
        int startOfCurly = comment.getSourcePos(((DCText) node).pos + matcher.end(1));
        SuggestedFix fix =
            SuggestedFix.builder()
                .replace(beforeAt, beforeAt, "{")
                .replace(startOfCurly, startOfCurly + 1, " ")
                .build();
        state.reportMatch(
            describeMatch(
                getDiagnosticPosition(beforeAt, getCurrentPath().getTreePath().getLeaf()), fix));
      }
    }

    private void handleIncorrectParens(TextTree node) {
      String body = node.getBody();
      Matcher matcher = parensRatherThanCurly.matcher(body);
      Comment comment = ((DCDocComment) getCurrentPath().getDocComment()).comment;
      while (matcher.find()) {
        int beforeAt = comment.getSourcePos(((DCText) node).pos + matcher.start());
        SuggestedFix.Builder fix = SuggestedFix.builder().replace(beforeAt, beforeAt + 1, "{");

        Optional<Integer> found = findClosingBrace(body, matcher.start(1));
        found.ifPresent(
            pos -> {
              int closing = comment.getSourcePos(((DCText) node).pos + pos);
              fix.replace(closing, closing + 1, "}");
            });

        state.reportMatch(
            buildDescription(
                    getDiagnosticPosition(beforeAt, getCurrentPath().getTreePath().getLeaf()))
                .setMessage(
                    String.format(
                        "Curly braces should be used for inline Javadoc tags: {@%s ...}",
                        matcher.group(1)))
                .addFix(fix.build())
                .build());
      }
    }

    /** Looks for a matching closing brace, if one is found. */
    private Optional<Integer> findClosingBrace(String body, int startPos) {
      int parenDepth = 0;
      for (int pos = startPos; pos < body.length(); ++pos) {
        char c = body.charAt(pos);
        switch (c) {
          case '(':
            parenDepth++;
            continue;
          case ')':
            if (parenDepth == 0) {
              return Optional.of(pos);
            }
            parenDepth--;
            break;
          case '}':
            return Optional.empty();
          default:
            // fall out
        }
      }
      return Optional.empty();
    }

    private void handleDanglingParams(TextTree node) {
      Matcher matcher = PARAM_MATCHER.matcher(node.getBody());
      Comment comment = ((DCDocComment) getCurrentPath().getDocComment()).comment;
      while (matcher.find()) {
        int startPos = comment.getSourcePos(((DCText) node).pos + matcher.start());
        int endPos = comment.getSourcePos(((DCText) node).pos + matcher.end());
        String paramName = matcher.group(1);
        SuggestedFix fix =
            SuggestedFix.replace(startPos, endPos, String.format("{@code %s}", paramName));
        state.reportMatch(
            describeMatch(
                getDiagnosticPosition(startPos, getCurrentPath().getTreePath().getLeaf()), fix));
      }
    }

    @Override
    public Void visitUnknownInlineTag(UnknownInlineTagTree unknownInlineTagTree, Void unused) {
      String name = unknownInlineTagTree.getTagName();
      if (name.equals("param")) {
        int startPos = Utils.getStartPosition(unknownInlineTagTree, state);
        int endPos = Utils.getEndPosition(unknownInlineTagTree, state);
        CharSequence text = state.getSourceCode().subSequence(startPos, endPos);
        Matcher matcher = PARAM_MATCHER.matcher(text);
        if (matcher.find()) {
          String parameterName = matcher.group(1);
          if (parameters.contains(parameterName)) {
            String message =
                String.format(
                    "@param cannot be used inline to refer to parameters; {@code %s} is"
                        + " recommended",
                    parameterName);
            state.reportMatch(
                buildDescription(diagnosticPosition(getCurrentPath(), state))
                    .setMessage(message)
                    .addFix(
                        Utils.replace(
                            unknownInlineTagTree,
                            String.format("{@code %s}", parameterName),
                            state))
                    .build());
          }
          fixedTags.add(unknownInlineTagTree);
          return super.visitUnknownInlineTag(unknownInlineTagTree, null);
        }
      }
      if (parameters.contains(name)) {
        String message = String.format(INVALID_TAG_IS_PARAMETER_NAME, name);
        state.reportMatch(
            buildDescription(diagnosticPosition(getCurrentPath(), state))
                .setMessage(message)
                .addFix(
                    Utils.replace(unknownInlineTagTree, String.format("{@code %s}", name), state))
                .build());
        fixedTags.add(unknownInlineTagTree);
        return super.visitUnknownInlineTag(unknownInlineTagTree, null);
      }
      if (isProbablyType(name)) {
        int startPos = Utils.getStartPosition(unknownInlineTagTree, state);
        String message =
            String.format(
                "The tag {@%1$s} is not valid, and will not display or cross-link "
                    + "to the type %1$s correctly. Prefer {@link %1$s}.",
                name);
        state.reportMatch(
            buildDescription(diagnosticPosition(getCurrentPath(), state))
                .setMessage(message)
                .addFix(SuggestedFix.replace(startPos, startPos + 2, "{@link "))
                .build());
        fixedTags.add(unknownInlineTagTree);
        return super.visitUnknownInlineTag(unknownInlineTagTree, null);
      }
      reportUnknownTag(unknownInlineTagTree, inlineTag(name));
      return super.visitUnknownInlineTag(unknownInlineTagTree, null);
    }

    private boolean isProbablyType(String name) {
      Symbol typeSymbol =
          FindIdentifiers.findIdent(
              getFirst(DOT_SPLITTER.split(name), null), state, KindSelector.TYP);
      return typeSymbol instanceof TypeSymbol
          || name.chars().filter(c -> c == '.').count() >= 3
          || name.contains("#");
    }

    private void reportUnknownTag(DocTree docTree, JavadocTag tag) {
      Optional<String> bestMatch =
          Utils.getBestMatch(
              tag.name(),
              /* maxEditDistance= */ 2,
              validTags.stream()
                  .filter(t -> t.type().equals(tag.type()))
                  .map(JavadocTag::name)
                  .collect(toImmutableSet()));
      int pos = Utils.getStartPosition(docTree, state) + docTree.toString().indexOf(tag.name());
      String message = String.format("Tag name `%s` is unknown.", tag.name());
      state.reportMatch(
          bestMatch
              .map(
                  bm ->
                      buildDescription(diagnosticPosition(getCurrentPath(), state))
                          .setMessage(message + String.format(" Did you mean tag `%s`?", bm))
                          .addFix(SuggestedFix.replace(pos, pos + tag.name().length(), bm))
                          .build())
              .orElse(
                  buildDescription(diagnosticPosition(getCurrentPath(), state))
                      .setMessage(
                          message
                              + " If this is a commonly-used custom tag, please "
                              + "click 'not useful' and file a bug.")
                      .build()));
      fixedTags.add(docTree);
    }

    @Override
    public Void scan(DocTree docTree, Void unused) {
      super.scan(docTree, null);
      // Don't complain about this unknown tag if we already generated a better fix for it.
      if (fixedTags.contains(docTree)) {
        return null;
      }
      if (!(docTree instanceof DCInlineTag)) {
        return null;
      }
      JavadocTag tag = inlineTag(((DCInlineTag) docTree).getTagName());
      if (validTags.contains(tag) || JavadocTag.KNOWN_OTHER_TAGS.contains(tag)) {
        return null;
      }
      String message =
          String.format("The tag @%s is not allowed on this type of element.", tag.name());
      state.reportMatch(
          buildDescription(diagnosticPosition(getCurrentPath(), state))
              .setMessage(message)
              .addFix(Utils.replace(docTree, "", state))
              .build());
      return null;
    }
  }
}
