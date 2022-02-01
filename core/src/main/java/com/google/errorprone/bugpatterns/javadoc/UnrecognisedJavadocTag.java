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
import com.sun.source.doctree.LinkTree;
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

/** Flags tags which haven't been recognised by the Javadoc parser. */
@BugPattern(
    summary =
        "This Javadoc tag wasn't recognised by the parser. Is it malformed somehow, perhaps with"
            + " mismatched braces?",
    severity = WARNING,
    documentSuppression = false)
public final class UnrecognisedJavadocTag extends BugChecker
    implements ClassTreeMatcher, MethodTreeMatcher, VariableTreeMatcher {
  private static final Pattern TAG = Pattern.compile("\\{@(code|link)");

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
    ImmutableSet<Integer> recognisedTags = findRecognisedTags(path, state);
    ImmutableSet<Integer> tagStrings = findTags(((DCDocComment) path.getDocComment()).comment);

    for (int pos : Sets.difference(tagStrings, recognisedTags)) {
      state.reportMatch(
          buildDescription(getDiagnosticPosition(pos, path.getTreePath().getLeaf())).build());
    }

    return NO_MATCH;
  }

  private ImmutableSet<Integer> findRecognisedTags(DocTreePath path, VisitorState state) {
    ImmutableSet.Builder<Integer> tags = ImmutableSet.builder();
    new DocTreePathScanner<Void, Void>() {
      @Override
      public Void visitLink(LinkTree linkTree, Void unused) {
        tags.add(getStartPosition(linkTree, state));
        return super.visitLink(linkTree, null);
      }

      @Override
      public Void visitLiteral(LiteralTree literalTree, Void unused) {
        if (literalTree.getKind().equals(Kind.CODE)) {
          tags.add(getStartPosition(literalTree, state));
        }
        return super.visitLiteral(literalTree, null);
      }
    }.scan(path, null);
    return tags.build();
  }

  private static ImmutableSet<Integer> findTags(Comment comment) {
    Matcher matcher = TAG.matcher(comment.getText());
    ImmutableSet.Builder<Integer> tags = ImmutableSet.builder();
    while (matcher.find()) {
      tags.add(comment.getSourcePos(matcher.start()));
    }
    return tags.build();
  }
}
