/*
 * Copyright 2019 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getOnlyElement;

import com.google.auto.value.AutoValue;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type;

import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper for enforcing Annotations that disallow mocking.
 *
 * @author amalloy@google.com (Alan Malloy)
 */
public abstract class AbstractMockChecker extends BugChecker
    implements MethodInvocationTreeMatcher, VariableTreeMatcher {

  public AbstractMockChecker(
      TypeExtractor<VariableTree> varExtractor,
      TypeExtractor<MethodInvocationTree> methodExtractor) {
    this.varExtractor = varExtractor;
    this.methodExtractor = methodExtractor;
  }

  protected abstract Description checkMockedType(Type mockedClass, Tree tree, VisitorState state);

  /**
   * A policy for determining what classes should not be mocked.
   *
   * <p>This interface's intended use is to forbid mocking of classes you don't control, for example
   * those in the JDK itself or in a library you use.
   */
  public interface MockForbidder {

    /**
     * If the given type should not be mocked, provide an explanation why.
     *
     * @param type the type that is being mocked
     * @return the reason it should not be mocked
     */
    Optional<Reason> forbidReason(Type type, VisitorState state);
  }

  /** An explanation of what type should not be mocked, and the reason why. */
  @AutoValue
  public abstract static class Reason {

    public static Reason of(Type t, String reason) {
      return new AutoValue_AbstractMockChecker_Reason(t, reason);
    }

    /** A Type object representing the class that should not be mocked. */
    public abstract Type unmockableClass();

    /**
     * The reason this class should not be mocked, which may be as simple as "it is annotated to
     * forbid mocking" but may also provide a suggested workaround.
     */
    public abstract String reason();
  }

  /**
   * Produce a MockForbidder to use when looking for disallowed mocks, in addition to the built-in
   * checks for Annotations of type {@code T}.
   *
   * <p>This method will be called at most once for each instance of AbstractMockChecker, but of
   * course the returned object's {@link MockForbidder#forbidReason(Type, VisitorState)
   * forbidReason} method may be called many times.
   *
   * <p>The default implementation forbids nothing.
   */
  protected MockForbidder forbidder() {
    return (type, state) -> Optional.empty();
  }

  /**
   * An extension of {@link Matcher} to return, not just a boolean `matches`, but also extract some
   * type information about the Tree of interest.
   *
   * <p>This is used to identify what classes are being mocked in a Tree.
   *
   * @param <T> the type of Tree that this TypeExtractor operates on
   */
  public interface TypeExtractor<T extends Tree> {

    /**
     * Investigate the provided Tree, and return type information about it if it matches.
     *
     * @return the Type of the object being mocked, if any; Optional.empty() otherwise
     */
    Optional<Type> extract(T tree, VisitorState state);

    /**
     * Enrich this TypeExtractor with fallback behavior.
     *
     * @return a TypeExtractor which first tries {@code this.extract(t, s)}, and if that does not
     *     match, falls back to {@code other.extract(t, s)}.
     */
    default TypeExtractor<T> or(TypeExtractor<T> other) {
      return (tree, state) ->
          TypeExtractor.this
              .extract(tree, state)
              .map(Optional::of)
              .orElseGet(() -> other.extract(tree, state));
    }
  }

  /**
   * Produces an extractor which, if the tree matches, extracts the type of that tree, as given by
   * {@link ASTHelpers#getType(Tree)}.
   */
  public static <T extends Tree> TypeExtractor<T> extractType(Matcher<T> m) {
    return (tree, state) -> {
      if (m.matches(tree, state)) {
        return Optional.ofNullable(ASTHelpers.getType(tree));
      }
      return Optional.empty();
    };
  }

  /**
   * Produces an extractor which, if the tree matches, extracts the type of the first argument to
   * the method invocation.
   */
  public static TypeExtractor<MethodInvocationTree> extractFirstArg(
      Matcher<MethodInvocationTree> m) {
    return (tree, state) -> {
      if (!m.matches(tree, state)) {
        return Optional.empty();
      }
      if (tree.getArguments().size() >= 1) {
        return Optional.ofNullable(ASTHelpers.getType(tree.getArguments().get(0)));
      }
      return Optional.ofNullable(ASTHelpers.targetType(state)).map(t -> t.type());
    };
  }

  /**
   * Produces an extractor which, if the tree matches, extracts the type of the first argument whose
   * type is {@link Class} (preserving its {@code <T>} type parameter, if it has one}.
   *
   * @param m the matcher to use. It is an error for this matcher to succeed on any Tree that does
   *     not include at least one argument of type {@link Class}; if such a matcher is provided, the
   *     behavior of the returned Extractor is undefined.
   */
  public static TypeExtractor<MethodInvocationTree> extractClassArg(
      Matcher<MethodInvocationTree> m) {
    return (tree, state) -> {
      if (m.matches(tree, state)) {
        for (ExpressionTree argument : tree.getArguments()) {
          Type argumentType = ASTHelpers.getType(argument);
          if (ASTHelpers.isSameType(argumentType, state.getSymtab().classType, state)) {
            return Optional.of(argumentType);
          }
        }
        // It's undefined, so we could fall through - but an exception is less likely to surprise.
        throw new IllegalStateException();
      }
      return Optional.empty();
    };
  }

  /**
   * Creates a TypeExtractor that extracts the type of a class field if that field is annotated with
   * any one of the given annotations.
   */
  public static TypeExtractor<VariableTree> fieldAnnotatedWithOneOf(
      Stream<String> annotationClasses) {
    return extractType(
        Matchers.allOf(
            Matchers.isField(),
            Matchers.anyOf(
                annotationClasses.map(Matchers::hasAnnotation).collect(Collectors.toList()))));
  }

  /**
   * A {@link TypeExtractor} for variables that create a mock using {@code @Mockito.Mock} or
   * {@code @Mockito.Spy} annotations, extracting the type being mocked.
   */
  public static final TypeExtractor<VariableTree> MOCKING_ANNOTATION =
      fieldAnnotatedWithOneOf(Stream.of("org.mockito.Mock", "org.mockito.Spy"));

  /**
   * A {@link TypeExtractor} for method invocations that create a mock using {@code Mockito.mock},
   * {@code Mockito.spy}, or {@code EasyMock.create[...]Mock}, extracting the type being mocked.
   */
  public static final TypeExtractor<MethodInvocationTree> MOCKING_METHOD =
      extractFirstArg(
              Matchers.toType(
                  MethodInvocationTree.class,
                  Matchers.staticMethod().onClass("org.mockito.Mockito").namedAnyOf("mock", "spy")))
          .or(
              extractClassArg(
                  Matchers.toType(
                      MethodInvocationTree.class,
                      Matchers.staticMethod()
                          .onClass("org.easymock.EasyMock")
                          .withNameMatching(Pattern.compile("^create.*Mock(Builder)?$")))));

  /**
   * If type is Class<T>, returns the erasure of T. Otherwise, returns type unmodified. Returns
   * empty() when provided a raw Class as argument.
   */
  protected static Optional<Type> argFromClass(Type type, VisitorState state) {
    if (ASTHelpers.isSameType(type, state.getSymtab().classType, state)) {
      if (type.getTypeArguments().isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(state.getTypes().erasure(getOnlyElement(type.getTypeArguments())));
    }
    return Optional.of(type);
  }

  protected static String buildMessage(Type mockedClass, TypeSymbol forbiddenType) {
    return String.format(
        "Do not mock '%s'%s",
        mockedClass,
        (mockedClass.asElement().equals(forbiddenType)
            ? ""
            : " (which is-a '" + forbiddenType + "')"));
  }

  protected static String buildMessage(Type mockedClass, TypeSymbol forbiddenType, String reason) {
    return String.format("%s: %s.", buildMessage(mockedClass, forbiddenType), reason);
  }

  protected final TypeExtractor<VariableTree> varExtractor;
  protected final TypeExtractor<MethodInvocationTree> methodExtractor;
}
