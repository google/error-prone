/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.JUnitMatchers.hasJUnit4TestCases;
import static com.google.errorprone.matchers.JUnitMatchers.hasJUnit4TestRunner;
import static com.google.errorprone.matchers.JUnitMatchers.isTestCaseDescendant;
import static com.google.errorprone.matchers.Matchers.anyOf;

import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

/** Base of checks for explicit and implicit leaks of the 'this' reference. */
abstract class ConstructorLeakChecker extends BugChecker implements ClassTreeMatcher {
  private static final Matcher<ClassTree> TEST_CLASS =
      anyOf(isTestCaseDescendant, hasJUnit4TestRunner, hasJUnit4TestCases);

  /**
   * For each class, visits constructors, instance variables, and instance initializers. Delegates
   * further scanning of these "constructor-scope" constructs to {@link #traverse}.
   */
  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    // TODO(b/36395371): filter here to exclude some classes (e.g. not immutable)
    if (TEST_CLASS.matches(tree, state)) {
      return NO_MATCH;
    }

    for (Tree member : tree.getMembers()) {
      if (isSuppressed(member)) {
        continue;
      }
      if ((member instanceof MethodTree
              && Matchers.methodIsConstructor().matches((MethodTree) member, state))
          || (member instanceof BlockTree && !((BlockTree) member).isStatic())
          || (member instanceof VariableTree && !Matchers.isStatic().matches(member, state))) {
        traverse(member, state);
      }
    }
    return NO_MATCH;
  }

  /**
   * Given a tree that has been determined to be at constructor scope, walks it looking for
   * problems. Emits error descriptions as it goes via {@link VisitorState#reportMatch}.
   */
  protected abstract void traverse(Tree tree, VisitorState state);
}
