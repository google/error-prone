/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Convert;
import javax.lang.model.type.TypeKind;

/** @author lowasser@google.com (Louis Wasserman) */
@BugPattern(
  category = Category.JDK,
  explanation =
      "StringBuilder does not have a char constructor, so instead this code creates "
          + "a StringBuilder with initial size equal to the code point of the specified char.",
  name = "StringBuilderInitWithChar",
  severity = ERROR,
  summary = "StringBuilder does not have a char constructor; this invokes the int constructor.",
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class StringBuilderInitWithChar extends BugChecker implements NewClassTreeMatcher {
  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (ASTHelpers.isSameType(
            state.getSymtab().stringBuilderType, ASTHelpers.getType(tree.getIdentifier()), state)
        && tree.getArguments().size() == 1) {
      ExpressionTree argument = tree.getArguments().get(0);
      Type type = ((JCTree) argument).type;
      if (type.getKind() == TypeKind.CHAR) {
        if (argument.getKind() == Kind.CHAR_LITERAL) {
          char ch = (Character) ((LiteralTree) argument).getValue();
          return describeMatch(
              tree,
              SuggestedFix.replace(argument, "\"" + Convert.quote(Character.toString(ch)) + "\""));
        } else {
          return describeMatch(
              tree,
              SuggestedFix.replace(
                  tree,
                  "new StringBuilder().append(" + state.getSourceForNode((JCTree) argument) + ")"));
        }
      }
    }
    return Description.NO_MATCH;
  }
}
