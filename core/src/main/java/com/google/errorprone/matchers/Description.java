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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.fixes.Fix;

import com.sun.source.tree.Tree;

import java.util.List;

/**
 * Simple data object containing the information captured about an AST match.
 * Can be printed in a UI, or output in structured format for use by tools.
 * @author alexeagle@google.com (Alex Eagle)
 */
public class Description {
  /** Describes the sentinel value of the case where the match failed. */
  public static final Description NO_MATCH = new Description(null, "<no match>", "<no match>",
      "<no match>", ImmutableList.<Fix>of(), NOT_A_PROBLEM);

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
   * A list of fixes to suggest in an error message or use in automated refactoring.  Fixes are
   * in order of decreasing preference, from most preferred to least preferred.
   */
  public final List<Fix> fixes;

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
    this(node, UNDEFINED_CHECK_NAME, message, message, ImmutableList.of(suggestedFix), severity);
    if (suggestedFix == null) {
      throw new IllegalArgumentException("suggestedFix must not be null.");
    }

  }

  private Description(Tree node, String checkName, String rawMessage, String link,
      ImmutableList<Fix> fixes, BugPattern.SeverityLevel severity) {
    this.checkName = checkName;
    this.rawMessage = rawMessage;
    this.link = link;
    this.fixes = fixes;
    this.node = node;
    this.severity = severity;
  }

  /**
   * Construct the link text to include in the compiler error message. Returns null if there is
   * no link.
   */
  private static String linkTextForDiagnostic(String linkUrl) {
    return linkUrl == null ? null : "  (see " + linkUrl + ")";
  }

  /**
   * Returns a new builder for {@link Description}s.
   *
   * @param node the node where the error is
   * @param checker the {@code BugChecker} instance that is producing this {@code Description}
   */
  public static Builder builder(Tree node, BugChecker checker) {
    return new Builder(node, checker);
  }

  /**
   * Builder for {@code Description}s.
   */
  public static class Builder {
    private final Tree node;
    private final String name;
    private final String linkUrl;
    private final SeverityLevel severity;
    private ImmutableList.Builder<Fix> fixListBuilder = ImmutableList.builder();
    private String rawMessage;

    private Builder(Tree node, BugChecker checker) {
      Preconditions.checkNotNull(checker);
      this.node = Preconditions.checkNotNull(node);
      this.name = checker.canonicalName();
      this.linkUrl = checker.linkUrl();
      this.severity = checker.severity();
      this.rawMessage = checker.message();
    }

    /**
     * Add a suggested fix for this {@code Description}. Fixes should be added in order of
     * decreasing preference.
     *
     * @param fix A suggested fix for this problem
     */
    public Builder addFix(Fix fix) {
      if (fix == null) {
        throw new IllegalArgumentException("fix must not be null");
      }
      fixListBuilder.add(fix);
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
      return new Description(node, name, rawMessage, linkTextForDiagnostic(linkUrl),
          fixListBuilder.build(), severity);
    }
  }

}
