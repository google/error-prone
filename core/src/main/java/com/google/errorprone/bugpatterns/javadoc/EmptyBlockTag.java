/*
 * Copyright 2019 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.LinkType.CUSTOM;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.bugpatterns.javadoc.Utils.diagnosticPosition;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.doctree.BlockTagTree;
import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.ThrowsTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;
import java.util.List;

/**
 * Matches block tags ({@literal @}param, {@literal @}return, {@literal @}throws,
 * {@literal @}deprecated) with an empty description.
 *
 * @author andrewash@google.com (Andrew Ash)
 */
@BugPattern(
    name = "EmptyBlockTag",
    summary = "A block tag (@param, @return, @throws, @deprecated) has an empty description.",
    severity = WARNING,
    linkType = CUSTOM,
    link = "http://google.github.io/styleguide/javaguide.html#s7.1.3-javadoc-block-tags",
    tags = StandardTags.STYLE,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION,
    documentSuppression = false)
public final class EmptyBlockTag extends BugChecker
    implements ClassTreeMatcher, MethodTreeMatcher, VariableTreeMatcher {

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    checkForEmptyBlockTags(state);
    return Description.NO_MATCH;
  }

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    checkForEmptyBlockTags(state);
    return Description.NO_MATCH;
  }

  @Override
  public Description matchVariable(VariableTree variableTree, VisitorState state) {
    checkForEmptyBlockTags(state);
    return Description.NO_MATCH;
  }

  private void checkForEmptyBlockTags(VisitorState state) {
    DocTreePath path = Utils.getDocTreePath(state);
    if (path != null) {
      new EmptyBlockTagChecker(state).scan(path, null);
    }
  }

  private final class EmptyBlockTagChecker extends DocTreePathScanner<Void, Void> {
    private final VisitorState state;

    private EmptyBlockTagChecker(VisitorState state) {
      this.state = state;
    }

    @Override
    public Void visitParam(ParamTree paramTree, Void unused) {
      reportMatchIfEmpty(paramTree, paramTree.getDescription());
      return super.visitParam(paramTree, null);
    }

    @Override
    public Void visitReturn(ReturnTree returnTree, Void unused) {
      reportMatchIfEmpty(returnTree, returnTree.getDescription());
      return super.visitReturn(returnTree, null);
    }

    @Override
    public Void visitThrows(ThrowsTree throwsTree, Void unused) {
      reportMatchIfEmpty(throwsTree, throwsTree.getDescription());
      return super.visitThrows(throwsTree, null);
    }

    @Override
    public Void visitDeprecated(DeprecatedTree deprecatedTree, Void unused) {
      reportMatchIfEmpty(deprecatedTree, deprecatedTree.getBody());
      return super.visitDeprecated(deprecatedTree, null);
    }

    private void reportMatchIfEmpty(
        BlockTagTree blockTagTree, List<? extends DocTree> description) {
      if (description.isEmpty()) {
        state.reportMatch(
            describeMatch(
                diagnosticPosition(getCurrentPath(), state),
                Utils.replace(blockTagTree, "", state)));
      }
    }
  }
}
