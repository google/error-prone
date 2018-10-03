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

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.FindIdentifiers;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.EndElementTree;
import com.sun.source.doctree.ErroneousTree;
import com.sun.source.doctree.StartElementTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import com.sun.source.doctree.UnknownInlineTagTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;
import com.sun.tools.javac.code.Kinds.KindSelector;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.tree.DCTree.DCBlockTag;
import com.sun.tools.javac.tree.DCTree.DCInlineTag;
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
    name = "InvalidTag",
    summary = "This tag is invalid.",
    severity = WARNING,
    tags = StandardTags.STYLE,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public final class InvalidTag extends BugChecker
    implements ClassTreeMatcher, MethodTreeMatcher, VariableTreeMatcher {

  private static final String INVALID_TAG_IS_PARAMETER_NAME =
      "@%1$s is not a valid tag, but is a parameter name. "
          + "Use {@code %1%s} to refer to parameter names inline.";

  /** Non-standard commonly-used tags which we should allow. */
  private static final ImmutableSet<String> KNOWN_OTHER_TAGS =
      ImmutableSet.of(
          "attr", // commonly used by Android
          "required",
          "hide");

  /**
   * HTML tags which imply we're showing code, and should therefore probably escape unknown block
   * tags.
   */
  private static final ImmutableSet<String> CODE_TAGS = ImmutableSet.of("code", "pre");

  // TODO(b/72685460): Split these into unknown block vs inline tags.
  private static final ImmutableSet<String> VALID_METHOD_TAGS =
      ImmutableSet.of(
          "author",
          "code",
          "deprecated",
          "docRoot",
          "exception",
          "inheritDoc",
          "link",
          "linkplain",
          "literal",
          "param",
          "return",
          "see",
          "serial",
          "since",
          "throws",
          "serialData",
          "serialField",
          "value",
          "version");

  private static final ImmutableSet<String> VALID_VARIABLE_TAGS =
      ImmutableSet.of(
          "code",
          "deprecated",
          "docRoot",
          "link",
          "linkplain",
          "literal",
          "see",
          "serial",
          "serialData",
          "serialField",
          "since",
          "value");

  private static final ImmutableSet<String> VALID_CLASS_TAGS =
      ImmutableSet.of(
          "author",
          "code",
          "deprecated",
          "docRoot",
          "inheritDoc",
          "link",
          "linkplain",
          "literal",
          "param",
          "see",
          "since",
          "value",
          "version");

  private static final Pattern PARAM_MATCHER = Pattern.compile("\\{@param (.*)}");

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    DocTreePath path = Utils.getDocTreePath(state);
    if (path != null) {
      ImmutableSet<String> parameters = ImmutableSet.of();
      new InvalidTagChecker(state, VALID_CLASS_TAGS, parameters).scan(path, null);
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
      new InvalidTagChecker(state, VALID_METHOD_TAGS, parameters).scan(path, null);
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchVariable(VariableTree variableTree, VisitorState state) {
    DocTreePath path = Utils.getDocTreePath(state);
    if (path != null) {
      new InvalidTagChecker(state, VALID_VARIABLE_TAGS, /* parameters= */ ImmutableSet.of())
          .scan(path, null);
    }
    return Description.NO_MATCH;
  }

  private final class InvalidTagChecker extends DocTreePathScanner<Void, Void> {
    private final VisitorState state;

    private final ImmutableSet<String> validTags;
    private final ImmutableSet<String> parameters;

    private final Set<DocTree> fixedTags = new HashSet<>();
    private int codeTagNestedDepth = 0;

    private InvalidTagChecker(
        VisitorState state,
        ImmutableSet<String> validTags,
        ImmutableSet<String> parameters) {
      this.state = state;
      this.validTags = validTags;
      this.parameters = parameters;
    }

    @Override
    public Void visitStartElement(StartElementTree startElementTree, Void unused) {
      if (CODE_TAGS.contains(startElementTree.getName().toString())) {
        codeTagNestedDepth++;
      }
      return super.visitStartElement(startElementTree, null);
    }

    @Override
    public Void visitEndElement(EndElementTree endElementTree, Void unused) {
      if (CODE_TAGS.contains(endElementTree.getName().toString())) {
        codeTagNestedDepth--;
      }
      return super.visitEndElement(endElementTree, null);
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
    public Void visitUnknownBlockTag(UnknownBlockTagTree unknownBlockTagTree, Void unused) {
      String tagName = unknownBlockTagTree.getTagName();
      if (KNOWN_OTHER_TAGS.contains(tagName)) {
        return super.visitUnknownBlockTag(unknownBlockTagTree, null);
      }
      if (codeTagNestedDepth > 0) {
        // If we're in a <code> tag, this is probably meant to be an annotation.
        int startPos = Utils.getStartPosition(unknownBlockTagTree, state);
        String message =
            String.format(
                "@%s is not a valid block tag. Did you mean to escape it? Annotations must be "
                    + "escaped even within <pre> and {@code } tags, otherwise they will be "
                    + "interpreted as block tags.",
                tagName);
        state.reportMatch(
            buildDescription(diagnosticPosition(getCurrentPath(), state))
                .setMessage(message)
                .addFix(SuggestedFix.replace(startPos, startPos + 1, "{@literal @}"))
                .build());
        fixedTags.add(unknownBlockTagTree);
        return super.visitUnknownBlockTag(unknownBlockTagTree, null);
      }
      if (parameters.contains(tagName)) {
        int startPos = Utils.getStartPosition(unknownBlockTagTree, state);
        String message = String.format(INVALID_TAG_IS_PARAMETER_NAME, tagName);
        state.reportMatch(
            buildDescription(diagnosticPosition(getCurrentPath(), state))
                .setMessage(message)
                .addFix(SuggestedFix.replace(startPos, startPos + 1, "@param "))
                .build());
        fixedTags.add(unknownBlockTagTree);
        return super.visitUnknownBlockTag(unknownBlockTagTree, null);
      }
      reportUnknownTag(unknownBlockTagTree, tagName);
      return super.visitUnknownBlockTag(unknownBlockTagTree, null);
    }

    @Override
    public Void visitUnknownInlineTag(UnknownInlineTagTree unknownInlineTagTree, Void unused) {
      String name = unknownInlineTagTree.getTagName();
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
      reportUnknownTag(unknownInlineTagTree, unknownInlineTagTree.getTagName());
      return super.visitUnknownInlineTag(unknownInlineTagTree, null);
    }

    private boolean isProbablyType(String name) {
      Symbol typeSymbol = FindIdentifiers.findIdent(name, state, KindSelector.TYP);
      return typeSymbol instanceof TypeSymbol
          || name.chars().filter(c -> c == '.').count() >= 3
          || name.contains("#");
    }

    private void reportUnknownTag(DocTree docTree, String tagName) {
      Optional<String> bestMatch = Utils.getBestMatch(tagName, validTags);
      int pos = Utils.getStartPosition(docTree, state) + docTree.toString().indexOf(tagName);
      String message = String.format("Tag name `%s` is unknown.", tagName);
      state.reportMatch(
          bestMatch
              .map(
                  bm ->
                      buildDescription(diagnosticPosition(getCurrentPath(), state))
                          .setMessage(message + String.format(" Did you mean tag `%s`?", bm))
                          .addFix(SuggestedFix.replace(pos, pos + tagName.length(), bm))
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
      String tagName = null;
      if (docTree instanceof DCBlockTag) {
        tagName = ((DCBlockTag) docTree).getTagName();
      }
      if (docTree instanceof DCInlineTag) {
        tagName = ((DCInlineTag) docTree).getTagName();
      }
      if (tagName != null && !validTags.contains(tagName) && !KNOWN_OTHER_TAGS.contains(tagName)) {
        String message =
            String.format("The block tag @%s is not allowed on this type of element.", tagName);
        state.reportMatch(
            buildDescription(diagnosticPosition(getCurrentPath(), state))
                .setMessage(message)
                .addFix(Utils.replace(docTree, "", state))
                .build());
      }
      return null;
    }
  }
}
