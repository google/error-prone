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
import static com.google.errorprone.fixes.SuggestedFix.NO_FIX;

import com.google.errorprone.BugPattern;
import com.google.errorprone.fixes.Fix;

import com.sun.source.tree.Tree;

/**
 * Simple data object containing the information captured about an AST match.
 * Can be printed in a UI, or output in structured format for use by tools.
 * @author alexeagle@google.com (Alex Eagle)
 */
public class Description {
  /** Describes the sentinel value of the case where the match failed. */
  public static final Description NO_MATCH =
      new Description(null, "<No match>", NO_FIX, NOT_A_PROBLEM);

  private static final String UNDEFINED_CHECK_NAME = "Undefined";

  /**
   * The AST node which matched
   */
  public final Tree node;

  /**
   * The name of the check that produced the match.
   */
  public final String checkName;

  /**
   * Printed by the compiler when a match is found in interactive use.
   */
  public final String message;

  /**
   * The message, not including the check name.
   */
  public final String rawMessage;

  /**
   * Replacements to suggest in an error message or use in automated refactoring
   */
  public final Fix suggestedFix;

  /**
   * Is this a warning, error, etc.
   */
  public final BugPattern.SeverityLevel severity;

  public Description(Tree node, BugPattern pattern, String message, Fix suggestedFix) {
    this(node, pattern.name(), String.format("[%s] %s",  pattern.name(), message), message,
        suggestedFix, pattern.severity());
  }

  /** TODO(cushon): Remove this constructor and ensure that there's always a check name. */
  public Description(Tree node, String message, Fix suggestedFix,
                     BugPattern.SeverityLevel severity) {
    this(node, UNDEFINED_CHECK_NAME, message, message, suggestedFix, severity);
  }

  private Description(Tree node, String checkName, String message, String rawMessage,
      Fix suggestedFix, BugPattern.SeverityLevel severity) {
    this.checkName = checkName;
    this.message = message;
    this.rawMessage = rawMessage;
    if (suggestedFix == null) {
      throw new IllegalArgumentException("suggestedFix must not be null. Use "
          + "SuggestedFix.NO_FIX if there is no fix.");
    }
    this.suggestedFix = suggestedFix;
    this.node = node;
    this.severity = severity;
  }
}
