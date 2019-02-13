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
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.packageStartsWith;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import javax.lang.model.element.Modifier;

/** @author alexeagle@google.com (Alex Eagle) */
@BugPattern(
    name = "ReturnValueIgnored",
    altNames = {"ResultOfMethodCallIgnored", "CheckReturnValue"},
    summary = "Return value of this method must be used",
    severity = ERROR)
public class ReturnValueIgnored extends AbstractReturnValueIgnored {
  /**
   * A set of types which this checker should examine method calls on.
   *
   * <p>There are also some high-priority return value ignored checks in FindBugs for various
   * threading constructs which do not return the same type as the receiver. This check does not
   * deal with them, since the fix is less straightforward. See a list of the FindBugs checks here:
   * http://code.google.com/searchframe#Fccnll6ERQ0/trunk/findbugs/src/java/edu/umd/cs/findbugs/ba/CheckReturnAnnotationDatabase.java
   */
  private static final Set<String> typesToCheck =
      new HashSet<>(
          Arrays.asList(
              "java.lang.String",
              "java.math.BigInteger",
              "java.math.BigDecimal",
              "java.nio.file.Path"));

  /**
   * Matches method invocations in which the method being called is on an instance of a type in the
   * typesToCheck set and returns the same type (e.g. String.trim() returns a String).
   */
  private static final Matcher<ExpressionTree> RETURNS_SAME_TYPE =
      allOf(methodReceiverHasType(typesToCheck), methodReturnsSameTypeAsReceiver());

  /**
   * This matcher allows the following methods in {@code java.time}:
   *
   * <ul>
   *   <li>any methods named {@code parse}
   *   <li>any static method named {@code of}
   *   <li>any static method named {@code from}
   *   <li>any instance method starting with {@code append} on {@link
   *       java.time.format.DateTimeFormatterBuilder}
   *   <li>{@link java.time.temporal.ChronoField#checkValidIntValue}
   *   <li>{@link java.time.temporal.ChronoField#checkValidValue}
   * </ul>
   */
  private static final Matcher<ExpressionTree> ALLOWED_JAVA_TIME_METHODS =
      anyOf(
          staticMethod().anyClass().named("parse"),
          instanceMethod().anyClass().named("parse"),
          staticMethod().anyClass().named("of"),
          staticMethod().anyClass().named("from"),
          staticMethod().onClass("java.time.ZoneId").named("ofOffset"),
          instanceMethod()
              .onExactClass("java.time.format.DateTimeFormatterBuilder")
              .withNameMatching(Pattern.compile("^append.*")),
          instanceMethod()
              .onExactClass("java.time.temporal.ChronoField")
              .named("checkValidIntValue"),
          instanceMethod().onExactClass("java.time.temporal.ChronoField").named("checkValidValue"));

  /**
   * {@link java.time} types are immutable. The only methods we allow ignoring the return value on
   * are the {@code parse}-style APIs since folks often use it for validation.
   */
  private static final Matcher<ExpressionTree> JAVA_TIME_TYPES =
      (tree, state) -> {
        if (packageStartsWith("java.time").matches(tree, state)) {
          return false;
        }
        Symbol symbol = ASTHelpers.getSymbol(tree);
        if (symbol instanceof MethodSymbol) {
          MethodSymbol methodSymbol = (MethodSymbol) symbol;
          if (methodSymbol.owner.packge().getQualifiedName().toString().startsWith("java.time")
              && methodSymbol.getModifiers().contains(Modifier.PUBLIC)) {
            if (ALLOWED_JAVA_TIME_METHODS.matches(tree, state)) {
              return false;
            }
            return true;
          }
        }
        return false;
      };

  /**
   * Methods in {@link java.util.function} are pure, and their returnvalues should not be discarded.
   */
  private static final Matcher<ExpressionTree> FUNCTIONAL_METHOD =
      (tree, state) -> {
        Symbol symbol = ASTHelpers.getSymbol(tree);
        return symbol instanceof MethodSymbol
            && ((MethodSymbol) symbol)
                .owner
                .packge()
                .getQualifiedName()
                .contentEquals("java.util.function");
      };

  /**
   * The return value of stream methods should always be checked (except for forEach and
   * forEachOrdered, which are void-returning and won't be checked by AbstractReturnValueIgnored).
   */
  private static final Matcher<ExpressionTree> STREAM_METHOD =
      instanceMethod().onDescendantOf("java.util.stream.BaseStream");

  /**
   * The return values of {@link java.util.Arrays} methods should always be checked (except for
   * void-returning ones, which won't be checked by AbstractReturnValueIgnored).
   */
  private static final Matcher<ExpressionTree> ARRAYS_METHODS =
      staticMethod().onClass("java.util.Arrays");

  @Override
  public Matcher<? super ExpressionTree> specializedMatcher() {
    return anyOf(
        RETURNS_SAME_TYPE, FUNCTIONAL_METHOD, STREAM_METHOD, ARRAYS_METHODS, JAVA_TIME_TYPES);
  }

  /** Matches method invocations that return the same type as the receiver object. */
  private static Matcher<ExpressionTree> methodReturnsSameTypeAsReceiver() {
    return new Matcher<ExpressionTree>() {
      @Override
      public boolean matches(ExpressionTree expressionTree, VisitorState state) {
        return isSameType(
            ASTHelpers.getReceiverType(expressionTree),
            ASTHelpers.getReturnType(expressionTree),
            state);
      }
    };
  }

  /** Matches method calls whose receiver objects are of a type included in the set. */
  private static Matcher<ExpressionTree> methodReceiverHasType(final Set<String> typeSet) {
    return new Matcher<ExpressionTree>() {
      @Override
      public boolean matches(ExpressionTree expressionTree, VisitorState state) {
        Type receiverType = ASTHelpers.getReceiverType(expressionTree);
        return typeSet.contains(receiverType.toString());
      }
    };
  }
}
