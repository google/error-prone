/*
 * Copyright 2024 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.restrictedapi;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.MoreAnnotations;
import com.google.errorprone.util.SourcePathMatcher;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;

/** Details of a restriction declared via an annotation like {@code @RestrictedApi}. */
public record Restriction(
    String explanation,
    String link,
    ImmutableList<String> allowedPaths,
    String allowedOnPath,
    ImmutableList<TypeMirror> allowlistAnnotations,
    Optional<TypeMirror> suggestedAllowlistAnnotation,
    boolean allowedInTestonlyTargets,
    boolean warningOnlyForRefactoring) {

  private record PathRestrictions(ImmutableList<String> allowedPaths, String allowedOnPath) {
    static PathRestrictions from(AnnotationMirror attribute) {
      ImmutableList<String> allowedPaths =
          MoreAnnotations.getValue(attribute, "allowedPaths")
              .map(MoreAnnotations::asStrings)
              .orElse(Stream.empty())
              .collect(toImmutableList());

      String allowedOnPath =
          MoreAnnotations.getValue(attribute, "allowedOnPath")
              .flatMap(MoreAnnotations::asStringValue)
              .orElse("");

      return new PathRestrictions(allowedPaths, allowedOnPath);
    }

    boolean hasConflict() {
      return !allowedPaths.isEmpty() && !allowedOnPath.isEmpty();
    }
  }

  /**
   * Validates path restrictions on an annotation, returning an error message if both {@code
   * allowedPaths} and {@code allowedOnPath} are specified.
   */
  public static Optional<String> validateAnnotation(
      AnnotationMirror attribute, Symbol annotationSymbol) {
    PathRestrictions paths = PathRestrictions.from(attribute);
    if (paths.hasConflict()) {
      return Optional.of(
          String.format(
              "Do not specify both allowedPaths and allowedOnPath on @%s; prefer allowedPaths.",
              annotationSymbol.getSimpleName()));
    }
    return Optional.empty();
  }

  /** Parses a {@link Restriction} from an {@link AnnotationMirror}. */
  public static Optional<Restriction> from(AnnotationMirror attribute) {
    if (attribute == null) {
      return Optional.empty();
    }
    PathRestrictions paths = PathRestrictions.from(attribute);

    ImmutableList<TypeMirror> allowlistAnnotations =
        MoreAnnotations.getValue(attribute, "allowlistAnnotations")
            .or(() -> MoreAnnotations.getValue(attribute, "whitelistAnnotations"))
            .map(MoreAnnotations::asTypes)
            .orElse(Stream.empty())
            .collect(toImmutableList());

    Optional<TypeMirror> suggestedAllowlistAnnotation =
        MoreAnnotations.getValue(attribute, "suggestedAllowlistAnnotation")
            .or(() -> MoreAnnotations.getValue(attribute, "whitelistAnnotation"))
            .flatMap(MoreAnnotations::asTypeValue);

    boolean allowedInTestonlyTargets =
        MoreAnnotations.getValue(attribute, "allowedInTestonlyTargets")
            .map(a -> Objects.equals(a.getValue(), true))
            .orElse(false);

    boolean warningOnlyForRefactoring =
        MoreAnnotations.getValue(attribute, "warningOnlyForRefactoring")
            .map(a -> Objects.equals(a.getValue(), true))
            .orElse(false);

    String explanation =
        MoreAnnotations.getValue(attribute, "explanation")
            .flatMap(MoreAnnotations::asStringValue)
            .orElse("");

    String link =
        MoreAnnotations.getValue(attribute, "link")
            .flatMap(MoreAnnotations::asStringValue)
            .orElse("");

    return Optional.of(
        new Restriction(
            explanation,
            link,
            paths.allowedPaths(),
            paths.allowedOnPath(),
            allowlistAnnotations,
            suggestedAllowlistAnnotation,
            allowedInTestonlyTargets,
            warningOnlyForRefactoring));
  }

  /** Returns true if usage at {@code where} is exempt by path or test-only target rules. */
  public boolean isPathOrTestonlyExempt(Tree where, VisitorState state) {
    if (!allowedPaths.isEmpty() && !allowedOnPath.isEmpty()) {
      throw new IllegalArgumentException(
          String.format(
              "Cannot specify both allowedPaths and allowedOnPath on restriction of %s",
              state.getSourceForNode(where)));
    }
    if (!allowedPaths.isEmpty()) {
      if (SourcePathMatcher.create(allowedPaths).matches(state)) {
        return true;
      }
    }
    if (!allowedOnPath.isEmpty()) {
      JCCompilationUnit compilationUnit = (JCCompilationUnit) state.getPath().getCompilationUnit();
      String uriPath = compilationUnit.getSourceFile().toUri().toString();
      String fileName = ASTHelpers.getFileName(compilationUnit);
      try {
        if (Pattern.matches(allowedOnPath, uriPath)
            || (fileName != null && Pattern.matches(allowedOnPath, fileName))) {
          return true;
        }
      } catch (PatternSyntaxException e) {
        throw new IllegalArgumentException(
            String.format(
                "Invalid regex for allowedOnPath on restriction of %s",
                state.getSourceForNode(where)),
            e);
      }
    }
    return false;
  }

  /** Returns true if usage at {@code where} is permitted by an allowlist annotation. */
  public boolean isAllowlisted(Tree where, VisitorState state) {
    return !allowlistAnnotations.isEmpty()
        && selfOrEnclosing(Matchers.hasAnyAnnotation(allowlistAnnotations)).matches(where, state);
  }

  /** Returns true if usage at {@code where} is exempt by path, test-only target, or allowlist. */
  public boolean isAllowed(Tree where, VisitorState state) {
    return isPathOrTestonlyExempt(where, state) || isAllowlisted(where, state);
  }

  private static Matcher<Tree> selfOrEnclosing(Matcher<Tree> matcher) {
    return Matchers.anyOf(matcher, Matchers.enclosingNode(matcher));
  }
}
