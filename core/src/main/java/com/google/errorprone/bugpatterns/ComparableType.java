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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.Signatures;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;

/** @author amesbah@google.com (Ali Mesbah) */
@BugPattern(
    name = "ComparableType",
    summary =
        "Implementing 'Comparable<T>' where T is not the same as the implementing class is"
            + " incorrect, since it violates the symmetry contract of compareTo.",
    severity = ERROR)
public class ComparableType extends BugChecker implements ClassTreeMatcher {

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    Type comparableType = state.getSymtab().comparableType;
    return tree.getImplementsClause().stream()
        .filter(impl -> isSameType(getType(impl), comparableType, state))
        .findAny()
        .map(impl -> match(tree, impl, state))
        .orElse(NO_MATCH);
  }

  Description match(ClassTree tree, Tree impl, VisitorState state) {
    Type implType = getType(impl);
    ClassType type = getType(tree);
    if (implType.getTypeArguments().isEmpty()) {
      return buildDescription(tree).setMessage("Comparable should not be raw").build();
    }
    Type comparableTypeArgument = getOnlyElement(implType.getTypeArguments());
    if (!isSameType(type, comparableTypeArgument, state)) {
      return buildDescription(tree)
          .setMessage(
              String.format(
                  "Type of Comparable (%s) is not the same as implementing class (%s).",
                  Signatures.prettyType(comparableTypeArgument), Signatures.prettyType(type)))
          .build();
    }
    return NO_MATCH;
  }
}
