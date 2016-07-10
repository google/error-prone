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
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Suggests to remove the unchecked throws clause.
 *
 * @author yulissa@google.com (Yulissa Arroyo-Paredes)
 */
@BugPattern(
  name = "ThrowsUncheckedException",
  summary = "Unchecked exceptions do not need to be declared in the method signature.",
  category = JDK,
  severity = ERROR,
  maturity = MATURE
)
public class ThrowsUncheckedException extends BugChecker implements MethodTreeMatcher {
  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    List<ExpressionTree> runtimeExceptions = new ArrayList<>();

    for (ExpressionTree exception : tree.getThrows()) {
      Type exceptionType = ASTHelpers.getType(exception);
      if (ASTHelpers.isSubtype(exceptionType, state.getSymtab().runtimeExceptionType, state)
          || ASTHelpers.isSubtype(exceptionType, state.getSymtab().errorType, state)) {
        runtimeExceptions.add(exception);
      }
    }

    // With all runtime exceptions in a list, if all the exceptions are Runtime suggest to
    // remove all otherwise just gives description.
    Description.Builder builder = buildDescription(tree);
    if (runtimeExceptions.isEmpty()) {
      return Description.NO_MATCH;
    } else if (runtimeExceptions.size() == tree.getThrows().size()) {
      int lengthOfExceptions = 0;
      // loop is intended to find the end position for the replace() for the first exception,
      // which is why the loop starts counting from the second item in the list
      for (int i = 1; i < runtimeExceptions.size(); i++) {
        // +2 to account for space and comma
        lengthOfExceptions += runtimeExceptions.get(i).toString().length() + 2;
      }
      // the " throws " string is exactly 8 characters before the exception if formatted correctly
      // and +1 for the " " after all the exceptions
      builder.addFix(
          SuggestedFix.replace(runtimeExceptions.get(0), " ", -8, lengthOfExceptions + 1));
    }
    return builder.build();
  }
}
