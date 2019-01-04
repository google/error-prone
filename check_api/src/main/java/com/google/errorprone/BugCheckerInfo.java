/*
 * Copyright 2015 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.Tree;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * An accessor for information about a single bug checker, including the metadata in the check's
 * {@code @BugPattern} annotation and the class that implements the check.
 */
public class BugCheckerInfo implements Serializable {

  /** The BugChecker class. */
  private final Class<? extends BugChecker> checker;

  /**
   * The canonical name of this check. Corresponds to the {@code name} attribute from its {@code
   * BugPattern} annotation.
   */
  private final String canonicalName;

  /**
   * Additional identifiers for this check, to be checked for in @SuppressWarnings annotations.
   * Corresponds to the canonical name plus all {@code altName}s from its {@code BugPattern}
   * annotation.
   */
  private final ImmutableSet<String> allNames;

  /** Set of String tags associated with the checker. */
  private final ImmutableSet<String> tags;

  /**
   * The error message to print in compiler diagnostics when this check triggers. Corresponds to the
   * {@code summary} attribute from its {@code BugPattern}.
   */
  private final String message;

  /** The default type of diagnostic (error or warning) to emit when this check triggers. */
  private final SeverityLevel defaultSeverity;

  /**
   * The link URL to display in the diagnostic message when this check triggers. Computed from the
   * {@code link} and {@code linkType} attributes from its {@code BugPattern}. May be null if no
   * link should be displayed.
   */
  private final String linkUrl;

  private final boolean supportsSuppressWarnings;

  /**
   * A set of suppression annotations for this check. Computed from the {@code
   * suppressionAnnotations} attributes from its {@code BugPattern}. May be empty if there are no
   * suppression annotations for this check.
   */
  private final Set<Class<? extends Annotation>> customSuppressionAnnotations;

  /** True if the check can be disabled using command-line flags. */
  private final boolean disableable;

  public static BugCheckerInfo create(Class<? extends BugChecker> checker) {
    BugPattern pattern =
        checkNotNull(
            checker.getAnnotation(BugPattern.class),
            "BugCheckers must be annotated with @BugPattern");
    checkArgument(
        !(Modifier.isAbstract(checker.getModifiers())
            || Modifier.isInterface(checker.getModifiers())),
        "%s must be a concrete class",
        checker);
    try {
      BugPatternValidator.validate(pattern);
    } catch (ValidationException e) {
      throw new IllegalStateException(e);
    }
    return new BugCheckerInfo(checker, pattern);
  }

  private BugCheckerInfo(Class<? extends BugChecker> checker, BugPattern pattern) {
    this(
        checker,
        pattern.name(),
        ImmutableSet.<String>builder().add(pattern.name()).add(pattern.altNames()).build(),
        pattern.summary(),
        pattern.severity(),
        createLinkUrl(pattern),
        Stream.of(pattern.suppressionAnnotations()).anyMatch(a -> isSuppressWarnings(a)),
        Stream.of(pattern.suppressionAnnotations())
            .filter(a -> !isSuppressWarnings(a))
            .collect(toImmutableSet()),
        ImmutableSet.copyOf(pattern.tags()),
        pattern.disableable());
  }

  private static boolean isSuppressWarnings(Class<? extends Annotation> annotation) {
    return annotation.getSimpleName().equals("SuppressWarnings");
  }

  private BugCheckerInfo(
      Class<? extends BugChecker> checker,
      String canonicalName,
      ImmutableSet<String> allNames,
      String message,
      SeverityLevel defaultSeverity,
      String linkUrl,
      boolean supportsSuppressWarnings,
      Set<Class<? extends Annotation>> customSuppressionAnnotations,
      ImmutableSet<String> tags,
      boolean disableable) {
    this.checker = checker;
    this.canonicalName = canonicalName;
    this.allNames = allNames;
    this.message = message;
    this.defaultSeverity = defaultSeverity;
    this.linkUrl = linkUrl;
    this.supportsSuppressWarnings = supportsSuppressWarnings;
    this.customSuppressionAnnotations = customSuppressionAnnotations;
    this.tags = tags;
    this.disableable = disableable;
  }

  /**
   * @return a BugCheckerInfo with the same information as this class, except that its default
   *     severity is the passed in paramter. If this checker's current defaultSeverity is the same
   *     as the argument, return this.
   */
  public BugCheckerInfo withCustomDefaultSeverity(SeverityLevel defaultSeverity) {
    if (defaultSeverity == this.defaultSeverity) {
      return this;
    }
    return new BugCheckerInfo(
        checker,
        canonicalName,
        allNames,
        message,
        defaultSeverity,
        linkUrl,
        supportsSuppressWarnings,
        customSuppressionAnnotations,
        tags,
        disableable);
  }

  private static final String URL_FORMAT = "https://errorprone.info/bugpattern/%s";

  private static String createLinkUrl(BugPattern pattern) {
    switch (pattern.linkType()) {
      case AUTOGENERATED:
        return String.format(URL_FORMAT, pattern.name());
      case CUSTOM:
        // annotation.link() must be provided.
        if (pattern.link().isEmpty()) {
          throw new IllegalStateException(
              "If linkType element of @BugPattern is CUSTOM, "
                  + "a link element must also be provided.");
        }
        return pattern.link();
      case NONE:
        return null;
    }
    throw new AssertionError(
        "Unexpected value for linkType element of @BugPattern: " + pattern.linkType());
  }

  /**
   * Returns a new builder for {@link Description}s.
   *
   * @param node the node where the error is
   * @param checker the {@code BugChecker} instance that is producing this {@code Description}
   */
  @CheckReturnValue
  public static Description.Builder buildDescriptionFromChecker(Tree node, BugChecker checker) {
    return Description.builder(
        Preconditions.checkNotNull(node),
        checker.canonicalName(),
        checker.linkUrl(),
        checker.defaultSeverity(),
        checker.message());
  }

  public String canonicalName() {
    return canonicalName;
  }

  public Set<String> allNames() {
    return allNames;
  }

  public String message() {
    return message;
  }

  public SeverityLevel defaultSeverity() {
    return defaultSeverity;
  }

  public SeverityLevel severity(Map<String, SeverityLevel> severities) {
    return firstNonNull(severities.get(canonicalName), defaultSeverity);
  }

  public String linkUrl() {
    return linkUrl;
  }

  public boolean supportsSuppressWarnings() {
    return supportsSuppressWarnings;
  }

  public Set<Class<? extends Annotation>> customSuppressionAnnotations() {
    return customSuppressionAnnotations;
  }

  public boolean disableable() {
    return disableable;
  }

  public ImmutableSet<String> getTags() {
    return tags;
  }

  public Class<? extends BugChecker> checkerClass() {
    return checker;
  }

  @Override
  public int hashCode() {
    return checker.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof BugCheckerInfo)) {
      return false;
    }
    return checker.equals(((BugCheckerInfo) o).checker);
  }

  @Override
  public String toString() {
    return canonicalName;
  }
}
