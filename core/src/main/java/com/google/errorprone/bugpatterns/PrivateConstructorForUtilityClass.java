/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.fixes.SuggestedFixes.addMembers;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.isGeneratedConstructor;
import static com.sun.source.tree.Tree.Kind.CLASS;
import static com.sun.source.tree.Tree.Kind.METHOD;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;

/** @author gak@google.com (Gregory Kick) */
@BugPattern(
  name = "PrivateConstructorForUtilityClass",
  summary =
      "Utility classes (only static members) are not designed to be instantiated and should"
          + " be made noninstantiable with a default constructor.",
  explanation =
      "Classes that only include static members have no behavior particular to any given instance,"
          + " so instantiating them is nonsense. To prevent users from mistakenly creating"
          + " instances, the class should include a private constructor.  See Effective Java,"
          + " Second Edition - Item 4.",
  category = JDK,
  severity = SUGGESTION,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public final class PrivateConstructorForUtilityClass extends BugChecker
    implements ClassTreeMatcher {

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    if (!classTree.getKind().equals(CLASS)
        || classTree.getExtendsClause() != null
        || !classTree.getImplementsClause().isEmpty()
        || isInPrivateScope(state)) {
      return NO_MATCH;
    }

    FluentIterable<? extends Tree> nonSyntheticMembers =
        FluentIterable.from(classTree.getMembers())
            .filter(
                Predicates.not(
                    new Predicate<Tree>() {
                      @Override
                      public boolean apply(Tree tree) {
                        return tree.getKind().equals(METHOD)
                            && isGeneratedConstructor((MethodTree) tree);
                      }
                    }));
    if (nonSyntheticMembers.isEmpty()) {
      return NO_MATCH;
    }
    boolean isUtilityClass =
        nonSyntheticMembers.allMatch(
            new Predicate<Tree>() {
              @Override
              public boolean apply(Tree tree) {
                switch (tree.getKind()) {
                  case CLASS:
                    return ((ClassTree) tree).getModifiers().getFlags().contains(STATIC);
                  case METHOD:
                    return ((MethodTree) tree).getModifiers().getFlags().contains(STATIC);
                  case VARIABLE:
                    return ((VariableTree) tree).getModifiers().getFlags().contains(STATIC);
                  case BLOCK:
                    return ((BlockTree) tree).isStatic();
                  case ENUM:
                  case ANNOTATION_TYPE:
                  case INTERFACE:
                    return true;
                  default:
                    throw new AssertionError("unknown member type:" + tree.getKind());
                }
              }
            });
    if (!isUtilityClass) {
      return NO_MATCH;
    }
    return describeMatch(
        classTree, addMembers(classTree, state, "private " + classTree.getSimpleName() + "() {}"));
  }

  private static boolean isInPrivateScope(VisitorState state) {
    TreePath treePath = state.getPath();
    do {
      Tree currentLeaf = treePath.getLeaf();
      if (ClassTree.class.isInstance(currentLeaf)) {
        ClassTree currentClassTree = (ClassTree) currentLeaf;
        if (currentClassTree.getModifiers().getFlags().contains(PRIVATE)) {
          return true;
        }
      }
      treePath = treePath.getParentPath();
    } while (treePath != null);

    return false;
  }
}
