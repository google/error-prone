/*
 * Copyright 2013 The Error Prone Authors.
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

package com.google.errorprone.matchers;

import com.google.errorprone.VisitorState;
import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

/**
 * Matches if the given annotation matcher matches all of or any of the annotations on the tree
 * node.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class AnnotationMatcher<T extends Tree> extends ChildMultiMatcher<T, AnnotationTree> {

  public AnnotationMatcher(MatchType matchType, Matcher<AnnotationTree> nodeMatcher) {
    super(matchType, nodeMatcher);
  }

  @Override
  protected Iterable<? extends AnnotationTree> getChildNodes(T tree, VisitorState state) {
    if (tree instanceof ClassTree) {
      return ((ClassTree) tree).getModifiers().getAnnotations();
    } else if (tree instanceof VariableTree) {
      return ((VariableTree) tree).getModifiers().getAnnotations();
    } else if (tree instanceof MethodTree) {
      return ((MethodTree) tree).getModifiers().getAnnotations();
    } else if (tree instanceof CompilationUnitTree) {
      return ((CompilationUnitTree) tree).getPackageAnnotations();
    } else if (tree instanceof AnnotatedTypeTree) {
      return ((AnnotatedTypeTree) tree).getAnnotations();
    } else if (tree instanceof PackageTree) {
      return ((PackageTree) tree).getAnnotations();
    } else {
      throw new IllegalArgumentException(
          "Cannot access annotations from tree of type " + tree.getClass());
    }
  }
}
