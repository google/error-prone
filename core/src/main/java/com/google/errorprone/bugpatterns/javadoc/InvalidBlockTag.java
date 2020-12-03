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
import static com.google.errorprone.bugpatterns.javadoc.JavadocTag.blockTag;
import static com.google.errorprone.bugpatterns.javadoc.JavadocTag.inlineTag;
import static com.google.errorprone.bugpatterns.javadoc.Utils.diagnosticPosition;
import static com.google.errorprone.bugpatterns.javadoc.Utils.replace;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.EndElementTree;
import com.sun.source.doctree.ErroneousTree;
import com.sun.source.doctree.StartElementTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;
import com.sun.tools.javac.tree.DCTree.DCBlockTag;
import com.sun.tools.javac.tree.DCTree.DCDocComment;
import com.sun.tools.javac.tree.DCTree.DCErroneous;
import com.sun.tools.javac.util.JCDiagnostic;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Matches invalid Javadoc tags, and tries to suggest fixes.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@BugPattern(
    name = "InvalidBlockTag",
    summary = "This tag is invalid.",
    severity = WARNING,
    tags = StandardTags.STYLE,
    documentSuppression = false)
public final class InvalidBlockTag extends BugChecker
    implements ClassTreeMatcher, MethodTreeMatcher, VariableTreeMatcher {

  private static final String INVALID_TAG_IS_PARAMETER_NAME =
      "@%1$s is not a valid tag, but is a parameter name. "
          + "Use {@code %1%s} to refer to parameter names inline.";

  /**
   * HTML tags which imply we're showing code, and should therefore probably escape unknown block
   * tags.
   */
  private static final ImmutableSet<String> CODE_TAGS = ImmutableSet.of("code", "pre");

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

    private final Set<DocTree> fixedTags = new HashSet<>();
    private int codeTagNestedDepth = 0;

    private InvalidTagChecker(
        VisitorState state, ImmutableSet<JavadocTag> validTags, ImmutableSet<String> parameters) {
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
      JCDiagnostic diagnostic = ((DCErroneous) erroneousTree).diag;
      if (!diagnostic.getCode().equals("compiler.err.dc.bad.inline.tag")) {
        return null;
      }
      JavadocTag tag = inlineTag(erroneousTree.toString().replace("@", ""));
      SuggestedFix fix =
          validTags.contains(tag)
              ? replace(erroneousTree, String.format("{%s}", erroneousTree), state)
              : SuggestedFix.emptyFix();
      String message =
          String.format(
              "%s is not a valid block tag. Should it be an inline tag instead?", erroneousTree);
      state.reportMatch(
          buildDescription(diagnosticPosition(getCurrentPath(), state))
              .setMessage(message)
              .addFix(fix)
              .build());
      return null;
    }

    @Override
    public Void visitUnknownBlockTag(UnknownBlockTagTree unknownBlockTagTree, Void unused) {
      String tagName = unknownBlockTagTree.getTagName();
      JavadocTag tag = blockTag(tagName);
      if (JavadocTag.KNOWN_OTHER_TAGS.contains(tag)) {
        return super.visitUnknownBlockTag(unknownBlockTagTree, null);
      }
      if (codeTagNestedDepth > 0) {
        // If we're in a <code> tag, this is probably meant to be an annotation.
        if (parentIsErroneousCodeTag()) {
          String message =
              String.format(
                  "@%s is interpreted as a block tag here, not as a literal. Escaping "
                      + "annotations within {@code } tags is problematic; you may have to avoid "
                      + "using {@code } and escape any HTML entities manually instead.",
                  tagName);
          state.reportMatch(
              buildDescription(diagnosticPosition(getCurrentPath(), state))
                  .setMessage(message)
                  .build());
          fixedTags.add(unknownBlockTagTree);
          return super.visitUnknownBlockTag(unknownBlockTagTree, null);
        }
        int startPos = Utils.getStartPosition(unknownBlockTagTree, state);
        String message =
            String.format(
                "@%s is not a valid block tag. Did you mean to escape it? Annotations must be "
                    + "escaped even within <pre> and <code>, otherwise they will be "
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
      reportUnknownTag(unknownBlockTagTree, tag);
      return super.visitUnknownBlockTag(unknownBlockTagTree, null);
    }

    /**
     * When we have an erroneous block tag inside a {@literal @}code tag, the enclosing
     * {@literal @}code tag will fail to parse. So, we're looking for an enclosing erroneous tag.
     */
    private boolean parentIsErroneousCodeTag() {
      if (getCurrentPath().getParentPath() == null) {
        return false;
      }
      DocTree parentDoc = getCurrentPath().getParentPath().getLeaf();
      if (!(parentDoc instanceof DCDocComment)) {
        return false;
      }
      DCDocComment dcDocComment = (DCDocComment) parentDoc;
      return dcDocComment.getFullBody().stream()
          .anyMatch(
              dc -> dc instanceof DCErroneous && ((DCErroneous) dc).body.startsWith("{@code"));
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
      if (!(docTree instanceof DCBlockTag)) {
        return null;
      }
      JavadocTag tag = blockTag(((DCBlockTag) docTree).getTagName());
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
