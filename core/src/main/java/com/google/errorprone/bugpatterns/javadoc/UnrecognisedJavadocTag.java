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
import static com.google.errorprone.bugpatterns.javadoc.Utils.diagnosticPosition;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.doctree.ErroneousTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;
import org.jspecify.annotations.Nullable;
import org.safere.Matcher;
import org.safere.Pattern;

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
    new DocTreePathScanner<Void, Void>() {
      @Override
      public Void visitErroneous(ErroneousTree node, Void unused) {
        String body = node.getBody();
        Matcher matcher = TAG.matcher(body);
        if (matcher.lookingAt()) {
          int end = body.indexOf('}');
          String tag =
              body.substring(0, end == -1 ? body.length() : end).replaceAll("\\R", " ").trim();
          state.reportMatch(
              buildDescription(diagnosticPosition(getCurrentPath(), state))
                  .setMessage(
                      "This Javadoc tag '%s' wasn't recognised by the parser. Is it malformed"
                          + " somehow, perhaps with mismatched braces?",
                      tag)
                  .build());
        }
        return super.visitErroneous(node, null);
      }
    }.scan(path, null);
    return NO_MATCH;
  }
}
