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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.Signatures;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;

/** @author amesbah@google.com (Ali Mesbah) */
@BugPattern(
  name = "ComparableType",
  summary = " Implementing 'Comparable<T>' where T is not compatible with the implementing class.",
  category = JDK,
  severity = ERROR
)
public class ComparableType extends BugChecker implements ClassTreeMatcher {

  /** Matches if a class/interface is subtype of Comparable */
  private static final Matcher<Tree> COMPARABLE_MATCHER =
      isSubtypeOf(Comparable.class.getCanonicalName());

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {

    if (COMPARABLE_MATCHER.matches(tree, state)) {

      final Type comparableTypeArgument = getComparableTypeArgument(tree, state);

      if (comparableTypeArgument != null) {
        final Type classType = ASTHelpers.getType(tree);

        if (!ASTHelpers.isCastable(classType, comparableTypeArgument, state)) {
          return buildDescription(tree)
              .setMessage(
                  String.format(
                      "Type of Comparable (%s) is not compatible with the implementing class (%s).",
                      Signatures.prettyType(comparableTypeArgument),
                      Signatures.prettyType(classType)))
              .build();
        }
      }
    }

    return Description.NO_MATCH;
  }

  private static Type getComparableTypeArgument(ClassTree tree, VisitorState state) {
    final Type comparable =
        state
            .getTypes()
            .asSuper(ASTHelpers.getType(tree), state.getSymtab().comparableType.asElement());

    if (comparable != null && !comparable.getTypeArguments().isEmpty()) {
      return Iterables.getOnlyElement(comparable.getTypeArguments());
    }

    return null;
  }
}
