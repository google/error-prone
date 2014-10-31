/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.dataflow.DataFlow.expressionDataflow;
import static com.google.errorprone.fixes.SuggestedFix.replace;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.common.base.Joiner;
import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

/**
 * @author deminguyen@google.com (Demi Nguyen)
 */
@RunWith(JUnit4.class)
public class NullnessPropagationTest {

  private CompilationTestHelper compilationHelper;

  /**
   * This method triggers the {@code BugPattern} used to test nullness propagation
   *
   * @param obj Variable whose nullness value is being checked
   */
  public static void triggerNullnessChecker(Object obj) {}

  /*
   * Methods that should never be called. These methods exist to force tests that pass a primitive
   * to decide whether they are testing (a) that primitive itself is null in which case they should
   * call triggerNullnessCheckerOnPrimitive or (b) that the result of autoboxing the primitive is
   * null, in which case it should call triggerNullnessCheckerOnBoxed. Of course, in either case,
   * the value should never be null. (The analysis isn't yet smart enough to detect this in all
   * cases.)
   *
   * Any call to these methods will produce a special error.
   */

  public static void triggerNullnessChecker(boolean b) {}

  public static void triggerNullnessChecker(byte b) {}

  public static void triggerNullnessChecker(char c) {}

  public static void triggerNullnessChecker(double d) {}

  public static void triggerNullnessChecker(float f) {}

  public static void triggerNullnessChecker(int i) {}

  public static void triggerNullnessChecker(long l) {}

  public static void triggerNullnessChecker(short s) {}

  /**
   * This method also triggers the {@code BugPattern} used to test nullness propagation, but it is
   * intended to be used only in the rare case of testing the result of boxing a primitive.
   */
  public static void triggerNullnessCheckerOnBoxed(Object obj) {}

  /*
   * These methods also trigger the {@code BugPattern} used to test nullness propagation, but they
   * are careful not to autobox their inputs.
   */

  public static void triggerNullnessCheckerOnPrimitive(boolean b) {}

  public static void triggerNullnessCheckerOnPrimitive(byte b) {}

  public static void triggerNullnessCheckerOnPrimitive(char c) {}

  public static void triggerNullnessCheckerOnPrimitive(double d) {}

  public static void triggerNullnessCheckerOnPrimitive(float f) {}

  public static void triggerNullnessCheckerOnPrimitive(int i) {}

  public static void triggerNullnessCheckerOnPrimitive(long l) {}

  public static void triggerNullnessCheckerOnPrimitive(short s) {}

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(new NullnessPropagationChecker());
  }

  @Test
  public void testTransferFunctions1() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(
        compilationHelper.fileManager().sources(
            getClass(), "NullnessPropagationTransferCases1.java"));
  }

  @Test
  public void testTransferFunctions2() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(
        compilationHelper.fileManager().sources(
            getClass(), "NullnessPropagationTransferCases2.java"));
  }

  @Test
  public void testTransferFunctions3() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(
        compilationHelper.fileManager().sources(
            getClass(), "NullnessPropagationTransferCases3.java"));
  }

  @Test
  public void testTransferFunctions4() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(
        compilationHelper.fileManager().sources(
            getClass(), "NullnessPropagationTransferCases4.java"));
  }

  @Test
  public void testTransferFunctions5() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(
        compilationHelper.fileManager().sources(
            getClass(), "NullnessPropagationTransferCases5.java"));
  }

  @Test
  public void testTransferFunctions6() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(
        compilationHelper.fileManager().sources(
            getClass(), "NullnessPropagationTransferCases6.java"));
  }

  @Test
  public void testTransferFunctions7() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(
        compilationHelper.fileManager().sources(
            getClass(), "NullnessPropagationTransferCases7.java"));
  }

  /**
   * BugPattern to test dataflow analysis using nullness propagation
   */
  @BugPattern(name = "NullnessPropagationChecker",
      summary = "Test checker for NullnessPropagationTest",
      explanation = "Outputs an error for each call to triggerNullnessChecker, describing its "
          + "argument as nullable or non-null",
      category = JDK, severity = ERROR, maturity = EXPERIMENTAL)
  public static final class NullnessPropagationChecker
      extends BugChecker implements MethodInvocationTreeMatcher {
    private static final NullnessPropagationTransfer NULLNESS_PROPAGATION =
        new NullnessPropagationTransfer();

    private static final String AMBIGUOUS_CALL_MESSAGE = "AMBIGUOUS CALL: use "
        + "triggerNullnessCheckerOnPrimitive if you want to test the primitive for nullness";

    private static final Matcher<ExpressionTree> TRIGGER_CALL_MATCHER = anyOf(
        staticMethod(NullnessPropagationTest.class.getName(), "triggerNullnessCheckerOnPrimitive"),
        staticMethod(NullnessPropagationTest.class.getName(), "triggerNullnessCheckerOnBoxed"),
        staticMethod(
            NullnessPropagationTest.class.getName(), "triggerNullnessChecker(java.lang.Object)"));

    private static final Matcher<ExpressionTree> AMBIGUOUS_CALL_FALLBACK_MATCHER =
        staticMethod(NullnessPropagationTest.class.getName(), "triggerNullnessChecker");

    @Override
    public Description matchMethodInvocation(
        MethodInvocationTree methodInvocation, VisitorState state) {
      if (!TRIGGER_CALL_MATCHER.matches(methodInvocation, state)) {
        if (AMBIGUOUS_CALL_FALLBACK_MATCHER.matches(methodInvocation, state)) {
          return buildDescription(methodInvocation)
              .setMessage(AMBIGUOUS_CALL_MESSAGE).build();
        }
        return NO_MATCH;
      }

      TreePath root = state.getPath();
      List<Object> values = new ArrayList<>();
      for (Tree arg : methodInvocation.getArguments()) {
        TreePath argPath = new TreePath(root, arg);
        values.add(expressionDataflow(argPath, state.context, NULLNESS_PROPAGATION));
      }

      String fixString = "(" + Joiner.on(", ").join(values) + ")";
      return describeMatch(methodInvocation, replace(methodInvocation, fixString));
    }
  }
}
