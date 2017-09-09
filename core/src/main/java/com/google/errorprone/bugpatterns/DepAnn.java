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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.sun.tools.javac.code.Flags.DEPRECATED;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;

/**
 * Matches the behaviour of the javac dep-ann Xlint warning.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(
  name = "DepAnn",
  altNames = "dep-ann",
  summary = "Deprecated item is not annotated with @Deprecated",
  explanation =
      "A declaration has the `@deprecated` Javadoc tag but no `@Deprecated` annotation. "
          + "Please add an `@Deprecated` annotation to this declaration in addition to the "
          + "`@deprecated` tag in the Javadoc.",
  category = JDK,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class DepAnn extends BugChecker
    implements MethodTreeMatcher, ClassTreeMatcher, VariableTreeMatcher {

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    return checkDeprecatedAnnotation(methodTree, state);
  }

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    return checkDeprecatedAnnotation(classTree, state);
  }

  @Override
  public Description matchVariable(VariableTree variableTree, VisitorState state) {
    return checkDeprecatedAnnotation(variableTree, state);
  }

  /**
   * Reports a dep-ann error for a declaration if: (1) javadoc contains the deprecated javadoc tag
   * (2) the declaration is not annotated with {@link java.lang.Deprecated}
   */
  @SuppressWarnings("javadoc")
  private Description checkDeprecatedAnnotation(Tree tree, VisitorState state) {
    Symbol symbol = ASTHelpers.getSymbol(tree);

    // (1)
    // javac sets the DEPRECATED bit in flags if the Javadoc contains @deprecated
    if ((symbol.flags() & DEPRECATED) == 0) {
      return Description.NO_MATCH;
    }

    // (2)
    if (symbol.attribute(state.getSymtab().deprecatedType.tsym) != null) {
      return Description.NO_MATCH;
    }

    return describeMatch(tree, SuggestedFix.prefixWith(tree, "@Deprecated\n"));
  }
}
