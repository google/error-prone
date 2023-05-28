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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.RestrictedApi;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import java.util.List;
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
          null,
          "<no match>",
          "<no match>",
          "<no match>",
          ImmutableList.<Fix>of(),
          Optional.of(SUGGESTION));

  /** The position of the match. */
  public final DiagnosticPosition position;

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
  private final Optional<BugPattern.SeverityLevel> severity;

  public BugPattern.SeverityLevel severity() {
    return severity.get();
  }

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

  private Description(
      DiagnosticPosition position,
      String checkName,
      String rawMessage,
      @Nullable String linkUrl,
      List<Fix> fixes,
      Optional<SeverityLevel> severity) {
    this.position = position;
    this.checkName = checkName;
    this.rawMessage = rawMessage;
    this.linkUrl = linkUrl;
    this.fixes = ImmutableList.copyOf(fixes);
    this.severity = severity;
  }

  /** Internal-only. */
  @CheckReturnValue
  public Description applySeverityOverride(SeverityLevel severity) {
    return new Description(
        position,
        checkName,
        rawMessage,
        linkUrl,
        fixes,
        Optional.of(this.severity.orElse(severity)));
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
  @RestrictedApi(
      explanation = "Use describeMatch or buildDescription on BugChecker instead.",
      link = "",
      allowedOnPath =
          ".*/java/com/google/devtools/staticanalysis/errorprone/pluggabletype/LatticeAdapter.java"
              + "|.*/java/com/google/devtools/staticanalysis/errorprone/pluggabletype/LatticeInfo.java"
              + "|.*/third_party/java_src/error_prone/project/check_api/src/main/java/com/google/errorprone/bugpatterns/BugChecker.java")
  public static Builder builder(Tree node, String name, @Nullable String link, String message) {
    return new Builder((DiagnosticPosition) node, name, link, message);
  }

  /** Returns a new builder for {@link Description}s. */
  @RestrictedApi(
      explanation = "Use describeMatch or buildDescription on BugChecker instead.",
      link = "",
      allowedOnPath =
          ".*/third_party/java_src/error_prone/project/check_api/src/main/java/com/google/errorprone/bugpatterns/BugChecker.java")
  public static Builder builder(
      DiagnosticPosition position, String name, @Nullable String link, String message) {
    return new Builder(position, name, link, message);
  }

  /** Returns a new builder for {@link Description}s. */
  @RestrictedApi(
      explanation = "Use describeMatch or buildDescription on BugChecker instead.",
      link = "",
      allowedOnPath =
          ".*/third_party/java_src/error_prone/project/check_api/src/main/java/com/google/errorprone/bugpatterns/BugChecker.java"
              + "|.*/third_party/java_src/error_prone/project/core/src/main/java/com/google/errorprone/refaster/RefasterScanner.java")
  public static Builder builder(JCTree tree, String name, @Nullable String link, String message) {
    return new Builder(tree, name, link, message);
  }

  /** Builder for {@code Description}s. */
  public static class Builder {
    private final DiagnosticPosition position;
    private final String name;
    private String linkUrl;
    private Optional<SeverityLevel> severity = Optional.empty();
    private final ImmutableList.Builder<Fix> fixListBuilder = ImmutableList.builder();
    private String rawMessage;

    private Builder(
        DiagnosticPosition position, String name, @Nullable String linkUrl, String rawMessage) {
      this.position = Preconditions.checkNotNull(position);
      this.name = Preconditions.checkNotNull(name);
      this.linkUrl = linkUrl;
      this.rawMessage = Preconditions.checkNotNull(rawMessage);
    }

    /**
     * Adds a suggested fix for this {@code Description}. Fixes should be added in order of
     * decreasing preference. Adding an empty fix is a no-op.
     *
     * @param fix a suggested fix for this problem
     * @throws NullPointerException if {@code fix} is {@code null}
     */
    @CanIgnoreReturnValue
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
     * @deprecated prefer referring to empty fixes using {@link SuggestedFix#emptyFix()}.
     */
    @CanIgnoreReturnValue
    @Deprecated
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
    @CanIgnoreReturnValue
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
    @CanIgnoreReturnValue
    public Builder setMessage(String message) {
      checkNotNull(message, "message must not be null");
      this.rawMessage = message;
      return this;
    }

    /**
     * Set a custom link URL. The custom URL will be used instead of the default one which forms
     * part of the {@code @}BugPattern.
     */
    @CanIgnoreReturnValue
    public Builder setLinkUrl(String linkUrl) {
      checkNotNull(linkUrl, "linkUrl must not be null");
      this.linkUrl = linkUrl;
      return this;
    }

    @RestrictedApi(
        explanation =
            "Prefer to set a single default severity using @BugPattern. Overriding the severity for"
                + " individual Descriptions causes any command line options to be ignored, which is"
                + " potentially very confusing.",
        link = "",
        allowedOnPath =
            ".*/third_party/java_src/error_prone/project/check_api/src/main/java/com/google/errorprone/matchers/Description.java$|"
                + ".*/java/com/google/devtools/javatools/staticanalysis/xlang/java/BugCheckerUsingXlang.java$|"
                + ".*/java/com/google/devtools/staticanalysis/errorprone/RestrictedInheritanceChecker.java$|"
                + ".*/java/com/google/devtools/staticanalysis/errorprone/pluggabletype/LatticeAdapter.java$|"
                + ".*/third_party/java_src/error_prone/project/core/src/main/java/com/google/errorprone/bugpatterns/RestrictedApiChecker.java$|"
                + ".*/third_party/java_src/error_prone/project/core/src/main/java/com/google/errorprone/refaster/RefasterScanner.java$")
    @CanIgnoreReturnValue
    public Builder overrideSeverity(SeverityLevel severity) {
      checkNotNull(severity, "severity must not be null");
      this.severity = Optional.of(severity);
      return this;
    }

    public Description build() {
      return new Description(position, name, rawMessage, linkUrl, fixListBuilder.build(), severity);
    }
  }
}
