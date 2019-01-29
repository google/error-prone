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

import static com.google.errorprone.BugPattern.ProvidesFix.NO_FIX;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.BugPattern.StandardTags.STYLE;
import static com.google.errorprone.bugpatterns.javadoc.Utils.diagnosticPosition;
import static com.google.errorprone.bugpatterns.javadoc.Utils.getDocTreePath;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * Finds unescaped entities in Javadocs.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@BugPattern(
    name = "EscapedEntity",
    summary = "HTML entities in @code/@literal tags will appear literally in the rendered javadoc.",
    severity = WARNING,
    tags = STYLE,
    providesFix = NO_FIX,
    documentSuppression = false)
public final class EscapedEntity extends BugChecker
    implements ClassTreeMatcher, MethodTreeMatcher, VariableTreeMatcher {

  private static final Pattern HTML_ENTITY =
      Pattern.compile("&[a-z0-9]+;|&#[0-9]+;|&#x[0-9a-f]+;", Pattern.CASE_INSENSITIVE);

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    return handle(getDocTreePath(state), state);
  }

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    return handle(getDocTreePath(state), state);
  }

  @Override
  public Description matchVariable(VariableTree variableTree, VisitorState state) {
    return handle(getDocTreePath(state), state);
  }

  private Description handle(@Nullable DocTreePath path, VisitorState state) {
    if (path == null) {
      return NO_MATCH;
    }
    new Scanner(state).scan(path, null);
    return NO_MATCH;
  }

  private final class Scanner extends DocTreePathScanner<Void, Void> {
    private final VisitorState state;

    private Scanner(VisitorState state) {
      this.state = state;
    }

    @Override
    public Void visitLiteral(LiteralTree node, Void unused) {
      Matcher matcher = HTML_ENTITY.matcher(node.getBody().getBody());
      if (matcher.find()) {
        state.reportMatch(buildDescription(diagnosticPosition(getCurrentPath(), state)).build());
      }
      return super.visitLiteral(node, null);
    }
  }
}
