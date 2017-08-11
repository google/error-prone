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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
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

/** @author alexeagle@google.com (Alex Eagle) */
@BugPattern(
  name = "ReturnValueIgnored",
  altNames = {"ResultOfMethodCallIgnored", "CheckReturnValue"},
  summary = "Return value of this method must be used",
  explanation =
      "Certain library methods do nothing useful if their return value is ignored. "
          + "For example, String.trim() has no side effects, and you must store the return value "
          + "of String.intern() to access the interned string.  This check encodes a list of "
          + "methods in the JDK whose return value must be used and issues an error if they "
          + "are not.",
  category = JDK,
  severity = ERROR
)
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
   * forEachOrded, which are void-returning and will not be checked by AbstractReturnValueIgnored).
   */
  private static final Matcher<ExpressionTree> STREAM_METHOD =
      instanceMethod().onDescendantOf("java.util.stream.BaseStream");

  @Override
  public Matcher<? super ExpressionTree> specializedMatcher() {
    return anyOf(RETURNS_SAME_TYPE, FUNCTIONAL_METHOD, STREAM_METHOD);
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
