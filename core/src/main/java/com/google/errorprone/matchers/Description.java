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
  public static final Description NO_MATCH = new Description(null, "<no match>", "<no match>",
      "<no match>", NO_FIX, NOT_A_PROBLEM);

  private static final String UNDEFINED_CHECK_NAME = "Undefined";

  /**
   * The AST node that matched.
   */
  public final Tree node;

  /**
   * The name of the check that produced the match.
   */
  public final String checkName;

  /**
   * The raw message, not including the check name or the link.
   */
  private final String rawMessage;

  /**
   * The link to include in the error message. May be null if there is no link.
   */
  private final String link;

  /**
   * Replacements to suggest in an error message or use in automated refactoring.
   */
  public final Fix suggestedFix;

  /**
   * Is this a warning, error, etc.?
   */
  public final BugPattern.SeverityLevel severity;

  /**
   * Returns the message to be printed by the compiler when a match is found in interactive use.
   * Includes the name of the check and a link for more information.
   */
  public String getMessage() {
    return String.format("[%s] %s", checkName, getMessageWithoutCheckName());
  }

  /**
   * Returns the message, not including the check name but including the link.
   */
  public String getMessageWithoutCheckName() {
    return link != null
        ? String.format("%s\n%s", rawMessage, link)
        : String.format("%s", rawMessage);
  }

  /** TODO(user): Remove this constructor and ensure that there's always a check name. */
  public Description(Tree node, String message, Fix suggestedFix,
                     BugPattern.SeverityLevel severity) {
    this(node, UNDEFINED_CHECK_NAME, message, message, suggestedFix, severity);
    if (suggestedFix == null) {
      throw new IllegalArgumentException("suggestedFix must not be null. Use "
          + "SuggestedFix.NO_FIX if there is no fix.");
    }

  }

  private Description(Tree node, String checkName, String rawMessage, String link,
      Fix suggestedFix, BugPattern.SeverityLevel severity) {
    this.checkName = checkName;
    this.rawMessage = rawMessage;
    this.link = link;
    this.suggestedFix = suggestedFix;
    this.node = node;
    this.severity = severity;
  }

  /**
   * Construct the link text to include in the compiler error message. Returns null if there is
   * no link.
   */
  private static String getLink(BugPattern pattern) {
    switch (pattern.linkType()) {
      case WIKI:
        return "  (see http://code.google.com/p/error-prone/wiki/" + pattern.name() + ")";
      case CUSTOM:
        // annotation.link() must be provided.
        if (pattern.link().isEmpty()) {
          throw new IllegalStateException("If linkType element of @BugPattern is CUSTOM, "
              + "a link element must also be provided.");
        }
        return  "\n  (see " + pattern.link() + ")";
      case NONE:
        return null;
      default:
        throw new IllegalStateException("Unexpected value for linkType element of @BugPattern: "
            + pattern.linkType());
    }
  }

  /**
   * Returns a new builder for {@link Description}s.
   *
   * @param node The node where the error is
   * @param pattern The BugPattern annotation for this check
   */
  public static Builder builder(Tree node, BugPattern pattern) {
    return new Builder(node, pattern);
  }

  /**
   * Builder for {@code Description}s.
   */
  public static class Builder {
    private final Tree node;
    private final BugPattern pattern;
    private Fix fix = Fix.NO_FIX;
    private String rawMessage;

    private Builder(Tree node, BugPattern pattern) {
      if (node == null) {
        throw new IllegalArgumentException("node must not be null");
      }
      if (pattern == null) {
        throw new IllegalArgumentException("pattern must not be null");
      }
      this.node = node;
      this.pattern = pattern;
      this.rawMessage = pattern.summary();
    }

    /**
     * Set a suggested fix for this {@code Description}.
     *
     * @param fix The suggested fix for this problem
     */
    public Builder setFix(Fix fix) {
      if (fix == null) {
        throw new IllegalArgumentException("fix must not be null");
      }
      this.fix = fix;
      return this;
    }

    /**
     * Set a custom error message for this {@code Description}.  The custom message will be used
     * instead of the summary field as the text for the diagnostic message.
     *
     * @param message A custom error message without the check name ("[checkname]") or link
     */
    public Builder setMessage(String message) {
      if (message == null) {
        throw new IllegalArgumentException("message must not be null");
      }
      this.rawMessage = message;
      return this;
    }

    public Description build() {
      return new Description(
          node, pattern.name(), rawMessage, getLink(pattern), fix, pattern.severity());
    }
  }

}
