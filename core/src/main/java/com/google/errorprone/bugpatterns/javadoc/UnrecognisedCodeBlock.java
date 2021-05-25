/*
 * Copyright 2021 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.bugpatterns.javadoc.Utils.getDiagnosticPosition;
import static com.google.errorprone.bugpatterns.javadoc.Utils.getStartPosition;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.doctree.DocTree.Kind;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.tree.DCTree.DCDocComment;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/** Flags code blocks which haven't been recognised by the Javadoc parser. */
@BugPattern(
    name = "UnrecognisedCodeBlock",
    summary =
        "This {@code } tag wasn't recognised by the parser. Is it malformed somehow, perhaps with"
            + " mismatched braces?",
    severity = WARNING,
    documentSuppression = false)
public final class UnrecognisedCodeBlock extends BugChecker
    implements ClassTreeMatcher, MethodTreeMatcher, VariableTreeMatcher {
  private static final Pattern CODE_TAG = Pattern.compile("\\{@code");

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    return handle(Utils.getDocTreePath(state), state);
  }

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    return handle(Utils.getDocTreePath(state), state);
  }

  @Override
  public Description matchVariable(VariableTree variableTree, VisitorState state) {
    return handle(Utils.getDocTreePath(state), state);
  }

  private Description handle(@Nullable DocTreePath path, VisitorState state) {
    if (path == null) {
      return NO_MATCH;
    }
    ImmutableSet<Integer> recognisedCodeTags = findRecognisedCodeTags(path, state);
    ImmutableSet<Integer> codeTagStrings =
        findCodeTags(((DCDocComment) path.getDocComment()).comment);

    for (int pos : Sets.difference(codeTagStrings, recognisedCodeTags)) {
      state.reportMatch(
          buildDescription(getDiagnosticPosition(pos, path.getTreePath().getLeaf())).build());
    }

    return NO_MATCH;
  }

  private ImmutableSet<Integer> findRecognisedCodeTags(DocTreePath path, VisitorState state) {
    ImmutableSet.Builder<Integer> codeTags = ImmutableSet.builder();
    new DocTreePathScanner<Void, Void>() {
      @Override
      public Void visitLiteral(LiteralTree literalTree, Void unused) {
        if (literalTree.getKind().equals(Kind.CODE)) {
          codeTags.add(getStartPosition(literalTree, state));
        }
        return super.visitLiteral(literalTree, null);
      }
    }.scan(path, null);
    return codeTags.build();
  }

  private static ImmutableSet<Integer> findCodeTags(Comment comment) {
    Matcher matcher = CODE_TAG.matcher(comment.getText());
    ImmutableSet.Builder<Integer> codeTags = ImmutableSet.builder();
    while (matcher.find()) {
      codeTags.add(comment.getSourcePos(matcher.start()));
    }
    return codeTags.build();
  }
}
