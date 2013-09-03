/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

package com.google.errorprone.matchers;

import static com.google.errorprone.BugPattern.SeverityLevel.NOT_A_PROBLEM;

import com.google.errorprone.BugPattern;
import com.google.errorprone.fixes.SuggestedFix;
import com.sun.source.tree.Tree;

/**
 * Simple data object containing the information captured about an AST match.
 * Can be printed in a UI, or output in structured format for use by tools.
 * @author alexeagle@google.com (Alex Eagle)
 */
public class Description {
  /** Describes the sentinel value of the case where the match failed. */
  public static final Description NO_MATCH =
      new Description(null, "<No match>", null, NOT_A_PROBLEM);

  /**
   * The AST node which matched
   */
  public Tree node;

  /**
   * Printed by the compiler when a match is found in interactive use.
   */
  public String message;

  /**
   * Replacements to suggest in an error message or use in automated refactoring
   */
  public SuggestedFix suggestedFix;

  /**
   * Is this a warning, error, etc.
   */
  public BugPattern.SeverityLevel severity;

  public Description(Tree node, String message, SuggestedFix suggestedFix,
                     BugPattern.SeverityLevel severity) {
    this.message = message;
    this.suggestedFix = suggestedFix;
    this.node = node;
    this.severity = severity;
  }
}
