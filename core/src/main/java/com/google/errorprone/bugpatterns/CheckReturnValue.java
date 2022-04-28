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
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasDirectAnnotationWithSimpleName;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.checkreturnvalue.ExternalCanIgnoreReturnValue;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.Optional;
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

  private static final ImmutableMap<String, CrvOpinion> ANNOTATIONS =
      ImmutableMap.of(
          CHECK_RETURN_VALUE,
          CrvOpinion.SHOULD_BE_CRV,
          CAN_IGNORE_RETURN_VALUE,
          CrvOpinion.SHOULD_BE_CIRV);

  private static Optional<FoundAnnotation> controllingAnnotation(
      MethodSymbol sym, VisitorState visitorState) {
    // In priority order, we want a source-local annotation on the symbol, then the external API
    // file, then the enclosing elements of sym.
    return findAnnotation(sym)
        .or(
            () ->
                asAnnotationFromConfig(sym, visitorState)
                    .or(() -> findAnnotationOnEnclosingSymbols(sym.owner)));
  }

  private static Optional<FoundAnnotation> findAnnotation(Symbol sym) {
    return ANNOTATIONS.entrySet().stream()
        .filter(annoSpec -> hasDirectAnnotationWithSimpleName(sym, annoSpec.getKey()))
        .map(annotation -> FoundAnnotation.create(scope(sym), annotation.getValue()))
        .findFirst();
  }

  private static Optional<FoundAnnotation> findAnnotationOnEnclosingSymbols(Symbol sym) {
    return ASTHelpers.enclosingElements(sym).flatMap(e -> findAnnotation(e).stream()).findFirst();
  }

  private static Optional<FoundAnnotation> asAnnotationFromConfig(
      MethodSymbol sym, VisitorState visitorState) {
    if (ExternalCanIgnoreReturnValue.externallyConfiguredCirvAnnotation(sym, visitorState)) {
      return Optional.of(
          FoundAnnotation.create(AnnotationScope.METHOD_EXTERNAL_ANNO, CrvOpinion.SHOULD_BE_CIRV));
    }
    return Optional.empty();
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
  static final String CHECK_ALL_METHODS = "CheckReturnValue:CheckAllMethods";

  private final boolean checkAllConstructors;
  private final boolean checkAllMethods;

  public CheckReturnValue(ErrorProneFlags flags) {
    super(flags);
    this.checkAllConstructors = flags.getBoolean(CHECK_ALL_CONSTRUCTORS).orElse(false);
    this.checkAllMethods = flags.getBoolean(CHECK_ALL_METHODS).orElse(false);
  }

  /**
   * Return a matcher for method invocations in which the method being called should be considered
   * must-be-used.
   */
  @Override
  public Matcher<ExpressionTree> specializedMatcher() {
    return (tree, state) -> {
      Optional<MethodSymbol> maybeMethod = methodToInspect(tree);
      if (maybeMethod.isEmpty()) {
        return false;
      }

      return crvOpinionForMethod(maybeMethod.get(), state)
          .map(CrvOpinion.SHOULD_BE_CRV::equals)
          .orElse(false);
    };
  }

  private Optional<CrvOpinion> crvOpinionForMethod(MethodSymbol sym, VisitorState state) {
    Optional<FoundAnnotation> annotationForSymbol = controllingAnnotation(sym, state);
    if (annotationForSymbol.isPresent()) {
      return annotationForSymbol.map(FoundAnnotation::checkReturnValueOpinion);
    }

    // In the event there is no opinion from annotations, we use the checker's configuration to
    // decide what the "default" for the universe is.
    if (checkAllMethods || (checkAllConstructors && sym.isConstructor())) {
      return Optional.of(CrvOpinion.SHOULD_BE_CRV);
    }
    // NB: You might consider this SHOULD_BE_CIRV (here, where the default is to not check any
    // unannotated method, and no annotation exists)
    // However, we also use this judgement in the "should be covered" part of the analysis, so we
    // want to distinguish the states of "the world is CRV-by-default, but this method is annotated"
    // from "the world is CIRV-by-default, and this method was unannotated".
    return Optional.empty();
  }

  enum CrvOpinion {
    SHOULD_BE_CRV,
    SHOULD_BE_CIRV
  }

  private static Optional<MethodSymbol> methodToInspect(ExpressionTree tree) {
    // If we're in the middle of calling an anonymous class, we want to actually look at the
    // corresponding constructor of the supertype (e.g.: if I extend a class with a @CIRV
    // constructor that I delegate to, then my anonymous class's constructor should *also* be
    // considered @CIRV).
    if (tree instanceof NewClassTree) {
      ClassTree anonymousClazz = ((NewClassTree) tree).getClassBody();
      if (anonymousClazz != null) {
        // There should be a single defined constructor in the anonymous class body
        var constructor =
            anonymousClazz.getMembers().stream()
                .filter(MethodTree.class::isInstance)
                .map(MethodTree.class::cast)
                .filter(mt -> getSymbol(mt).isConstructor())
                .findFirst();

        // and its first statement should be a super() call to the method in question.
        return constructor
            .map(MethodTree::getBody)
            .map(block -> block.getStatements().get(0))
            .map(ExpressionStatementTree.class::cast)
            .map(ExpressionStatementTree::getExpression)
            .map(MethodInvocationTree.class::cast)
            .map(ASTHelpers::getSymbol);
      }
    }
    return methodSymbol(tree);
  }

  private static Optional<MethodSymbol> methodSymbol(ExpressionTree tree) {
    Symbol sym = ASTHelpers.getSymbol(tree);
    return sym instanceof MethodSymbol ? Optional.of((MethodSymbol) sym) : Optional.empty();
  }

  @Override
  public boolean isCovered(ExpressionTree tree, VisitorState state) {
    return methodSymbol(tree).flatMap(sym -> crvOpinionForMethod(sym, state)).isPresent();
  }

  @Override
  public ImmutableMap<String, ?> getMatchMetadata(ExpressionTree tree, VisitorState state) {
    return methodSymbol(tree)
        .flatMap(sym -> controllingAnnotation(sym, state))
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
        checkAllMethods
            ? "Ignored return value of '%s', which wasn't annotated with @CanIgnoreReturnValue"
            : "Ignored return value of '%s', which is annotated with @CheckReturnValue",
        name);
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
    static FoundAnnotation create(AnnotationScope scope, CrvOpinion opinion) {
      return new AutoValue_CheckReturnValue_FoundAnnotation(scope, opinion);
    }

    abstract AnnotationScope scope();

    abstract CrvOpinion checkReturnValueOpinion();
  }

  enum AnnotationScope {
    METHOD,
    METHOD_EXTERNAL_ANNO,
    CLASS,
    PACKAGE
  }
}
