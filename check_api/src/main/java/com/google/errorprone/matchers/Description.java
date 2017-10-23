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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.fixes.Fix;
import com.sun.source.tree.Tree;
import java.util.List;
import java.util.Optional;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

/**
 * Simple data object containing the information captured about an AST match. Can be printed in a
 * UI, or output in structured format for use by tools.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
public class Description {
  /** Describes the sentinel value of the case where the match failed. */
  public static final Description NO_MATCH =
      new Description(
          null, "<no match>", "<no match>", "<no match>", ImmutableList.<Fix>of(), SUGGESTION);

  private static final String UNDEFINED_CHECK_NAME = "Undefined";

  /** The AST node that matched. */
  public final Tree node;

  /** The name of the check that produced the match. */
  public final String checkName;

  /** The raw message, not including the check name or the link. */
  private final String rawMessage;

  /** The raw link URL for the check. May be null if there is no link. */
  @Nullable private final String linkUrl;
  /**
   * A list of fixes to suggest in an error message or use in automated refactoring. Fixes are in
   * order of decreasing preference, from most preferred to least preferred.
   */
  public final ImmutableList<Fix> fixes;

  /** Is this a warning, error, etc.? */
  public final BugPattern.SeverityLevel severity;

  /**
   * Returns the message to be printed by the compiler when a match is found in interactive use.
   * Includes the name of the check and a link for more information.
   */
  public String getMessage() {
    return String.format("[%s] %s", checkName, getMessageWithoutCheckName());
  }

  /** Returns a link associated with this finding or null if there is no link. */
  @Nullable
  public String getLink() {
    return linkUrl;
  }

  /** Returns the raw message, not including a link or check name. */
  public String getRawMessage() {
    return rawMessage;
  }

  /** Returns the message, not including the check name but including the link. */
  public String getMessageWithoutCheckName() {
    return linkUrl != null
        ? String.format("%s\n%s", rawMessage, linkTextForDiagnostic(linkUrl))
        : String.format("%s", rawMessage);
  }

  /** TODO(cushon): Remove this constructor and ensure that there's always a check name. */
  @Deprecated
  public Description(
      Tree node, String message, Fix suggestedFix, BugPattern.SeverityLevel severity) {
    this(node, UNDEFINED_CHECK_NAME, message, message, ImmutableList.of(suggestedFix), severity);
    if (suggestedFix == null) {
      throw new IllegalArgumentException("suggestedFix must not be null.");
    }
  }

  private Description(
      Tree node,
      String checkName,
      String rawMessage,
      String linkUrl,
      ImmutableList<Fix> fixes,
      SeverityLevel severity) {
    this.node = node;
    this.checkName = checkName;
    this.rawMessage = rawMessage;
    this.linkUrl = linkUrl;
    this.fixes = fixes;
    this.severity = severity;
  }

  /** Internal-only. Has no effect if applied to a Description within a BugChecker. */
  @CheckReturnValue
  public Description applySeverityOverride(SeverityLevel severity) {
    return new Description(node, checkName, rawMessage, linkUrl, fixes, severity);
  }

  @CheckReturnValue
  public Description filterFixes(Predicate<? super Fix> predicate) {
    return new Description(
        node,
        checkName,
        rawMessage,
        linkUrl,
        ImmutableList.copyOf(Iterables.filter(fixes, predicate)),
        severity);
  }

  /**
   * Construct the link text to include in the compiler error message. Returns null if there is no
   * link.
   */
  private static String linkTextForDiagnostic(String linkUrl) {
    return isNullOrEmpty(linkUrl) ? null : "  (see " + linkUrl + ")";
  }

  /** Returns a new builder for {@link Description}s. */
  public static Builder builder(
      Tree node, String name, @Nullable String link, SeverityLevel severity, String message) {
    return new Builder(node, name, link, severity, message);
  }

  /** Builder for {@code Description}s. */
  public static class Builder {
    private final Tree node;
    private final String name;
    private final String linkUrl;
    private final SeverityLevel severity;
    private final ImmutableList.Builder<Fix> fixListBuilder = ImmutableList.builder();
    private String rawMessage;

    private Builder(
        Tree node,
        String name,
        @Nullable String linkUrl,
        SeverityLevel severity,
        String rawMessage) {
      this.node = Preconditions.checkNotNull(node);
      this.name = Preconditions.checkNotNull(name);
      this.linkUrl = linkUrl;
      this.severity = Preconditions.checkNotNull(severity);
      this.rawMessage = Preconditions.checkNotNull(rawMessage);
    }

    /**
     * Adds a suggested fix for this {@code Description}. Fixes should be added in order of
     * decreasing preference. Adding an empty fix is a no-op.
     *
     * @param fix a suggested fix for this problem
     * @throws IllegalArgumentException if {@code fix} is {@code null}
     */
    public Builder addFix(Fix fix) {
      checkNotNull(fix, "fix must not be null");
      if (!fix.isEmpty()) {
        fixListBuilder.add(fix);
      }
      return this;
    }

    /**
     * Adds a suggested fix for this {@code Description} if {@code fix} is present. Fixes should be
     * added in order of decreasing preference. Adding an empty fix is a no-op.
     *
     * @param fix a suggested fix for this problem
     * @throws IllegalArgumentException if {@code fix} is {@code null}
     */
    public Builder addFix(Optional<? extends Fix> fix) {
      checkNotNull(fix, "fix must not be null");
      fix.ifPresent(this::addFix);
      return this;
    }

    /**
     * Add each fix in order.
     *
     * @param fixes a list of suggested fixes for this problem
     * @throws IllegalArgumentException if {@code fixes} or any of its elements are {@code null}
     */
    public Builder addAllFixes(List<? extends Fix> fixes) {
      checkNotNull(fixes, "fixes must not be null");
      for (Fix fix : fixes) {
        addFix(fix);
      }
      return this;
    }

    /**
     * Set a custom error message for this {@code Description}. The custom message will be used
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
      return new Description(node, name, rawMessage, linkUrl, fixListBuilder.build(), severity);
    }
  }
}
