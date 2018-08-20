/* Copyright 2017 The Error Prone Authors.
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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.common.collect.Lists;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import java.util.Comparator;
import java.util.List;

/**
 * @author sulku@google.com (Marsela Sulku)
 * @author mariasam@google.com (Maria Sam)
 */
@BugPattern(
    name = "ComparableAndComparator",
    summary = "Class should not implement both `Comparable` and `Comparator`",
    category = JDK,
    severity = WARNING)
public class ComparableAndComparator extends BugChecker implements ClassTreeMatcher {
  private static final String COMPARABLE = Comparable.class.getCanonicalName();
  private static final String COMPARATOR = Comparator.class.getCanonicalName();

  /** Matches if a class/interface is subtype of Comparable */
  private static final Matcher<Tree> COMPARABLE_MATCHER = isSubtypeOf(COMPARABLE);

  /** Matches if a class/interface is subtype of Comparator */
  private static final Matcher<Tree> COMPARATOR_MATCHER = isSubtypeOf(COMPARATOR);

  /** Matches if a class/interface is subtype of Comparable _and_ Comparator */
  private static final Matcher<Tree> COMPARABLE_AND_COMPARATOR_MATCHER =
      allOf(COMPARABLE_MATCHER, COMPARATOR_MATCHER);

  private static boolean matchAnySuperType(ClassTree tree, VisitorState state) {
    List<Tree> superTypes = Lists.<Tree>newArrayList(tree.getImplementsClause());
    Tree superClass = tree.getExtendsClause();
    if (superClass != null) {
      superTypes.add(superClass);
    }

    return superTypes.stream()
        .anyMatch(superType -> COMPARABLE_AND_COMPARATOR_MATCHER.matches(superType, state));
  }

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (COMPARABLE_AND_COMPARATOR_MATCHER.matches(tree, state)) {
      // Filter out inherited case to not warn again
      if (matchAnySuperType(tree, state)) {
        return Description.NO_MATCH;
      }

      // enums already implement Comparable and are simultaneously allowed to implement Comparator
      ClassSymbol symbol = getSymbol(tree);
      if (symbol.isEnum()) {
        return Description.NO_MATCH;
      }

      return describeMatch(tree);
    }
    return Description.NO_MATCH;
  }
}
