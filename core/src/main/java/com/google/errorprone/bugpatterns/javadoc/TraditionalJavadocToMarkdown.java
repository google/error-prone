/*
 * Copyright 2026 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasExplicitSource;
import static com.google.errorprone.util.ASTHelpers.isGeneratedConstructor;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ModuleTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.PackageTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.bugpatterns.javadoc.TraditionalJavadocConverter.Replacement;
import com.google.errorprone.fixes.FixedPosition;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.api.JavacTrees;
import java.util.Optional;

/// An experimental tool that converts traditional Javadoc comments to Markdown Javadoc comments.
///
/// The conversion happens in three steps:
///
/// 1. [MarkdownCommentBuilder] converts the comment's doc tree into a [MarkdownComment], keeping
///    any constructs that have no faithful Markdown equivalent as HTML or as traditional inline
///    tags (both of which Markdown documentation comments support).
/// 2. [MarkdownCommentPrinter] prints the [MarkdownComment], escaping any text that would otherwise
///    be interpreted as Markdown.
/// 3. [MarkdownCommentValidator] parses the printed Markdown and checks that it has the intended
///    structure.
///
/// A comment is left unchanged if any of these steps fails.
///
/// **WARNING:** this tool is currently a prototype and may have bugs.
@BugPattern(
    summary = "Converts traditional Javadoc comments into Markdown Javadoc comments",
    severity = SUGGESTION)
public final class TraditionalJavadocToMarkdown extends BugChecker
    implements ClassTreeMatcher,
        MethodTreeMatcher,
        VariableTreeMatcher,
        PackageTreeMatcher,
        ModuleTreeMatcher {

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    return checkJavadoc(state);
  }

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    return isGeneratedConstructor(tree) ? NO_MATCH : checkJavadoc(state);
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    return switch (getSymbol(tree).getKind()) {
      case FIELD, ENUM_CONSTANT -> checkJavadoc(state);
      default -> NO_MATCH;
    };
  }

  @Override
  public Description matchPackage(PackageTree tree, VisitorState state) {
    return checkJavadoc(state);
  }

  @Override
  public Description matchModule(ModuleTree tree, VisitorState state) {
    return checkJavadoc(state);
  }

  private Description checkJavadoc(VisitorState state) {
    if (!hasExplicitSource(state.getPath().getLeaf(), state)) {
      return NO_MATCH;
    }
    JavacTrees docTrees = JavacTrees.instance(state.context);
    DocCommentTree docComment = docTrees.getDocCommentTree(state.getPath());
    if (docComment == null) {
      return NO_MATCH;
    }
    Optional<Replacement> replacement =
        TraditionalJavadocConverter.convert(
            state.getPath().getCompilationUnit(),
            state.getPath().getLeaf(),
            docComment,
            docTrees,
            state.getSourceCode().toString());
    if (replacement.isEmpty()) {
      return NO_MATCH;
    }
    Replacement r = replacement.get();
    return buildDescription(new FixedPosition(state.getPath().getLeaf(), r.commentStart()))
        .addFix(SuggestedFix.replace(r.start(), r.end(), r.text()))
        .build();
  }
}
