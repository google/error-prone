/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.matchers.MultiMatcher.MatchType.ALL;
import static com.google.errorprone.matchers.MultiMatcher.MatchType.ANY;

import com.google.errorprone.VisitorState;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

import java.util.List;

/**
 * Matches if the given annotation matcher matches all of or any of the annotations on the tree
 * node.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class Annotation<T extends Tree> extends MultiMatcher<T, AnnotationTree> {

  public Annotation(MatchType matchType, Matcher<AnnotationTree> nodeMatcher) {
    super(matchType, nodeMatcher);
  }

  @Override
  public boolean matches(T tree, VisitorState state) {
    List<? extends AnnotationTree> annotations;
    if (tree instanceof ClassTree) {
      annotations = ((ClassTree) tree).getModifiers().getAnnotations();
    } else if (tree instanceof VariableTree) {
      annotations = ((VariableTree) tree).getModifiers().getAnnotations();
    } else if (tree instanceof MethodTree) {
      annotations = ((MethodTree) tree).getModifiers().getAnnotations();
    } else if (tree instanceof CompilationUnitTree) {
      annotations = ((CompilationUnitTree) tree).getPackageAnnotations();
    } else {
      throw new IllegalArgumentException("Cannot access annotations from tree of type "
          + tree.getClass());
    }

    for (AnnotationTree annotation : annotations) {
      boolean matches = nodeMatcher.matches(annotation, state);
      if (matchType == ANY && matches) {
        matchingNode = annotation;
        return true;
      }
      if (matchType == ALL && !matches) {
        return false;
      }
    }
    return matchType == ALL && annotations.size() >= 1;
  }
}
