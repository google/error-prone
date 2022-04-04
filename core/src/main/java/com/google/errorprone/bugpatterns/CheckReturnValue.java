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
import static com.google.errorprone.util.ASTHelpers.hasDirectAnnotationWithSimpleName;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.Optional;
import java.util.stream.Stream;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@BugPattern(
    altNames = {"ResultOfMethodCallIgnored", "ReturnValueIgnored"},
    summary = "Ignored return value of method that is annotated with @CheckReturnValue",
    severity = ERROR)
public class CheckReturnValue extends AbstractReturnValueIgnored
    implements MethodTreeMatcher, ClassTreeMatcher {

  private static final String CHECK_RETURN_VALUE = "CheckReturnValue";
  private static final String CAN_IGNORE_RETURN_VALUE = "CanIgnoreReturnValue";

  private static final ImmutableSet<String> ANNOTATIONS =
      ImmutableSet.of(CHECK_RETURN_VALUE, CAN_IGNORE_RETURN_VALUE);

  private static Stream<FoundAnnotation> findAnnotation(Symbol sym) {
    return ANNOTATIONS.stream()
        .filter(annotation -> hasDirectAnnotationWithSimpleName(sym, annotation))
        .limit(1)
        .map(annotation -> FoundAnnotation.create(annotation, scope(sym)));
  }

  private static Optional<FoundAnnotation> firstAnnotation(MethodSymbol sym) {
    return ASTHelpers.enclosingElements(sym).flatMap(CheckReturnValue::findAnnotation).findFirst();
  }

  private static AnnotationScope scope(Symbol sym) {
    if (sym instanceof MethodSymbol) {
      return AnnotationScope.METHOD;
    } else if (sym instanceof ClassSymbol) {
      return AnnotationScope.CLASS;
    } else {
      return AnnotationScope.PACKAGE;
    }
  }

  static final String CHECK_ALL_CONSTRUCTORS = "CheckReturnValue:CheckAllConstructors";

  private final boolean checkAllConstructors;

  public CheckReturnValue(ErrorProneFlags flags) {
    super(flags);
    this.checkAllConstructors = flags.getBoolean(CHECK_ALL_CONSTRUCTORS).orElse(false);
  }

  /**
   * Return a matcher for method invocations in which the method being called has the
   * {@code @CheckReturnValue} annotation.
   */
  @Override
  public Matcher<ExpressionTree> specializedMatcher() {
    return (tree, state) -> {
      Optional<MethodSymbol> sym = methodSymbol(tree);
      return sym.flatMap(CheckReturnValue::firstAnnotation)
          .map(found -> found.annotation().equals(CHECK_RETURN_VALUE))
          .orElse(checkAllConstructors && sym.map(MethodSymbol::isConstructor).orElse(false));
    };
  }

  private static Optional<MethodSymbol> methodSymbol(ExpressionTree tree) {
    Symbol sym = ASTHelpers.getSymbol(tree);
    return sym instanceof MethodSymbol ? Optional.of((MethodSymbol) sym) : Optional.empty();
  }

  @Override
  public boolean isCovered(ExpressionTree tree, VisitorState state) {
    return methodSymbol(tree)
        .map(m -> (checkAllConstructors && m.isConstructor()) || firstAnnotation(m).isPresent())
        .orElse(false);
  }

  @Override
  public ImmutableMap<String, ?> getMatchMetadata(ExpressionTree tree, VisitorState state) {
    return methodSymbol(tree)
        .flatMap(CheckReturnValue::firstAnnotation)
        .map(found -> ImmutableMap.of("annotation_scope", found.scope()))
        .orElse(ImmutableMap.of());
  }

  private static final String BOTH_ERROR =
      "@CheckReturnValue and @CanIgnoreReturnValue cannot both be applied to the same %s";

  /**
   * Validate {@code @CheckReturnValue} and {@link CanIgnoreReturnValue} usage on methods.
   *
   * <p>The annotations should not both be applied to the same method.
   *
   * <p>The annotations should not be applied to void-returning methods. Doing so makes no sense,
   * because there is no return value to check.
   */
  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    MethodSymbol method = ASTHelpers.getSymbol(tree);

    boolean checkReturn = hasDirectAnnotationWithSimpleName(method, CHECK_RETURN_VALUE);
    boolean canIgnore = hasDirectAnnotationWithSimpleName(method, CAN_IGNORE_RETURN_VALUE);

    if (checkReturn && canIgnore) {
      return buildDescription(tree).setMessage(String.format(BOTH_ERROR, "method")).build();
    }

    String annotationToValidate;
    if (checkReturn) {
      annotationToValidate = CHECK_RETURN_VALUE;
    } else if (canIgnore) {
      annotationToValidate = CAN_IGNORE_RETURN_VALUE;
    } else {
      return Description.NO_MATCH;
    }
    if (method.getKind() != ElementKind.METHOD) {
      // skip constructors (which javac thinks are void-returning)
      return Description.NO_MATCH;
    }
    if (!ASTHelpers.isVoidType(method.getReturnType(), state)) {
      return Description.NO_MATCH;
    }
    String message =
        String.format("@%s may not be applied to void-returning methods", annotationToValidate);
    return buildDescription(tree).setMessage(message).build();
  }

  /**
   * Validate that at most one of {@code CheckReturnValue} and {@code CanIgnoreReturnValue} are
   * applied to a class (or interface or enum).
   */
  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (hasDirectAnnotationWithSimpleName(ASTHelpers.getSymbol(tree), CHECK_RETURN_VALUE)
        && hasDirectAnnotationWithSimpleName(ASTHelpers.getSymbol(tree), CAN_IGNORE_RETURN_VALUE)) {
      return buildDescription(tree).setMessage(String.format(BOTH_ERROR, "class")).build();
    }
    return Description.NO_MATCH;
  }

  @Override
  protected String getMessage(Name name) {
    return String.format(
        "Ignored return value of '%s', which is annotated with @CheckReturnValue", name);
  }

  @Override
  protected Description describeReturnValueIgnored(NewClassTree newClassTree, VisitorState state) {
    return checkAllConstructors
        ? buildDescription(newClassTree)
            .setMessage(
                String.format(
                    "Ignored return value of '%s', which wasn't annotated with"
                        + " @CanIgnoreReturnValue",
                    state.getSourceForNode(newClassTree.getIdentifier())))
            .build()
        : super.describeReturnValueIgnored(newClassTree, state);
  }

  @AutoValue
  abstract static class FoundAnnotation {
    static FoundAnnotation create(String annotation, AnnotationScope scope) {
      return new AutoValue_CheckReturnValue_FoundAnnotation(annotation, scope);
    }

    abstract String annotation();

    abstract AnnotationScope scope();
  }

  enum AnnotationScope {
    METHOD,
    CLASS,
    PACKAGE
  }
}
