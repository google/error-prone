/*
 * Copyright 2017 The Error Prone Authors.
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

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import java.util.Objects;
import java.util.Optional;

/**
 * Warns against overriding toString() in a Throwable class and suggests getMessage()
 *
 * @author mariasam@google.com (Maria Sam)
 */
@BugPattern(
    name = "OverrideThrowableToString",
    summary =
        "To return a custom message with a Throwable class, one should "
            + "override getMessage() instead of toString() for Throwable.",
    category = JDK,
    severity = WARNING,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class OverrideThrowableToString extends BugChecker implements ClassTreeMatcher {

  @Override
  public Description matchClass(ClassTree classTree, VisitorState visitorState) {
    Symbol throwableClass = visitorState.getSymbolFromString("java.lang.Throwable");
    if (Objects.equals(ASTHelpers.getSymbol(classTree.getExtendsClause()), throwableClass)) {
      Optional<? extends Tree> methodTree =
          classTree.getMembers().stream()
              .filter(
                  m ->
                      m instanceof MethodTree
                          && ((MethodTree) m).getName().contentEquals("toString"))
              .findFirst();
      if (methodTree.isPresent()) {
        SuggestedFix.Builder builder = SuggestedFix.builder();
        MethodTree tree = (MethodTree) methodTree.get();
        if (!tree.getParameters().isEmpty()) {
          return Description.NO_MATCH;
        }
        String newTree =
            tree.getModifiers().toString().replaceAll("@Override[(][)]", "@Override")
                + "String getMessage()\n"
                + visitorState.getSourceForNode(tree.getBody());
        builder.replace(tree, newTree);
        return describeMatch(classTree, builder.build());
      }
    }
    return Description.NO_MATCH;
  }
}
