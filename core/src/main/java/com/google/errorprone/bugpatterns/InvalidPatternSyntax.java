/*
 * Copyright 2012 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.MethodInvocationTree;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author mdempsky@google.com (Matthew Dempsky)
 */
@BugPattern(summary = "Invalid syntax used for a regular expression", severity = ERROR)
public class InvalidPatternSyntax extends AbstractPatternSyntaxChecker {

  private static final String MESSAGE_BASE = "Invalid syntax used for a regular expression: ";

  @Override
  protected final Description matchRegexLiteral(MethodInvocationTree tree, String regex) {
    try {
      Pattern.compile(regex);
      return NO_MATCH;
    } catch (PatternSyntaxException e) {
      return buildDescription(tree).setMessage(MESSAGE_BASE + e.getMessage()).build();
    }
  }
}
