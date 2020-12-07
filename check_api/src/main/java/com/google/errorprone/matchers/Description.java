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

package com.google.errorprone.matchers;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.fixes.Fix;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
          null, "<no match>", "<no match>", "<no match>", ImmutableList.<Fix>of(), SUGGESTION, null);

  /** The position of the match. */
  public final DiagnosticPosition position;

  /** The name of the check that produced the match. */
  public final String checkName;

  /** The raw message, not including the check name or the link. */
  private final String rawMessage;

  /** The raw link URL for the check. May be null if there is no link. */
  @Nullable private final String linkUrl;

  private final Map<String, Object> metadata;

  /**
   * A list of fixes to suggest in an error message or use in automated refactoring. Fixes are in
   * order of decreasing preference, from most preferred to least preferred.
   */
  public final List<Fix> fixes;

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

  @Nullable
  public <T> T getMetadata(String key) {
    @SuppressWarnings("unchecked")
    T res = (T) metadata.get(key);
    return res;
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

  private Description(
      DiagnosticPosition position,
      String checkName,
      String rawMessage,
      @Nullable String linkUrl,
      List<Fix> fixes,
      SeverityLevel severity,
      Map<String, Object> metadata) {
    this.position = position;
    this.checkName = checkName;
    this.rawMessage = rawMessage;
    this.linkUrl = linkUrl;
    this.fixes = ImmutableList.copyOf(fixes);
    this.severity = severity;
    this.metadata = metadata;
  }

  /** Internal-only. Has no effect if applied to a Description within a BugChecker. */
  @CheckReturnValue
  public Description applySeverityOverride(SeverityLevel severity) {
    return new Description(position, checkName, rawMessage, linkUrl, fixes, severity, metadata);
  }

  /**
   * Construct the link text to include in the compiler error message. Returns null if there is no
   * link.
   */
  @Nullable
  private static String linkTextForDiagnostic(String linkUrl) {
    return isNullOrEmpty(linkUrl) ? null : "  (see " + linkUrl + ")";
  }

  /** Returns a new builder for {@link Description}s. */
  public static Builder builder(
      Tree node, String name, @Nullable String link, SeverityLevel severity, String message) {
    return new Builder((DiagnosticPosition) node, name, link, severity, message);
  }

  /** Returns a new builder for {@link Description}s. */
  public static Builder builder(
      DiagnosticPosition position,
      String name,
      @Nullable String link,
      SeverityLevel severity,
      String message) {
    return new Builder(position, name, link, severity, message);
  }

  /** Returns a new builder for {@link Description}s. */
  public static Builder builder(
      JCTree tree, String name, @Nullable String link, SeverityLevel severity, String message) {
    return new Builder(tree, name, link, severity, message);
  }

  /** Builder for {@code Description}s. */
  public static class Builder {
    private final DiagnosticPosition position;
    private final String name;
    private String linkUrl;
    private final SeverityLevel severity;
    private final ImmutableList.Builder<Fix> fixListBuilder = ImmutableList.builder();
    private String rawMessage;
    private Map<String, Object> metadata;

    private Builder(
        DiagnosticPosition position,
        String name,
        @Nullable String linkUrl,
        SeverityLevel severity,
        String rawMessage) {
      this.position = Preconditions.checkNotNull(position);
      this.name = Preconditions.checkNotNull(name);
      this.linkUrl = linkUrl;
      this.severity = Preconditions.checkNotNull(severity);
      this.rawMessage = Preconditions.checkNotNull(rawMessage);
      this.metadata = new HashMap<>();
    }

    /**
     * Adds a suggested fix for this {@code Description}. Fixes should be added in order of
     * decreasing preference. Adding an empty fix is a no-op.
     *
     * @param fix a suggested fix for this problem
     * @throws NullPointerException if {@code fix} is {@code null}
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
     * @throws NullPointerException if {@code fix} is {@code null}
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
     * @throws NullPointerException if {@code fixes} or any of its elements are {@code null}
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
      checkNotNull(message, "message must not be null");
      this.rawMessage = message;
      return this;
    }

    /**
     * Set a custom link URL. The custom URL will be used instead of the default one which forms
     * part of the {@code @}BugPattern.
     */
    public Builder setLinkUrl(String linkUrl) {
      checkNotNull(linkUrl, "linkUrl must not be null");
      this.linkUrl = linkUrl;
      return this;
    }

    public Builder addMetadata(String key, Object value) {
      checkNotNull(key, "metadata key must not be null");
      checkNotNull(value, "metadata value must not be null");
      metadata.put(key, value);
      return this;
    }

    public Description build() {
      return new Description(position, name, rawMessage, linkUrl, fixListBuilder.build(), severity, Collections.unmodifiableMap(metadata));
    }
  }
}
