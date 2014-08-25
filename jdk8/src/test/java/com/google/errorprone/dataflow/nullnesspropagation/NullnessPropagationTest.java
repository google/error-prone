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
import static com.google.errorprone.CompilationTestHelper.sources;
import static com.google.errorprone.dataflow.DataFlow.dataflow;
import static com.google.errorprone.fixes.SuggestedFix.replace;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.findEnclosingNode;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

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
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.util.Name;

import org.checkerframework.dataflow.analysis.Analysis;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  public static void triggerNullnessChecker(Object obj) {
  }

  public static void triggerNullnessCheckerOnInt(int i) {
  }

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(new NullnessPropagationChecker());
  }
  
  @Test
  public void testTransferFunctions1() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(
        sources(getClass(), "NullnessPropagationTransferCases1.java"));
  }
  
  @Test
  public void testTransferFunctions2() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(
        sources(getClass(), "NullnessPropagationTransferCases2.java"));
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
    private static final Matcher<ExpressionTree> TRIGGER_CALL_MATCHER = anyOf(
        staticMethod(NullnessPropagationTest.class.getName(), "triggerNullnessCheckerOnInt"),
        staticMethod(NullnessPropagationTest.class.getName(), "triggerNullnessChecker"));
    
    private final Map<MethodTree, Analysis<NullnessValue, ?, ?>> resultCache = new HashMap<>();
    
    @Override
    public Description matchMethodInvocation(
        MethodInvocationTree methodInvocation, VisitorState state) {
      if (!TRIGGER_CALL_MATCHER.matches(methodInvocation, state)) {
        return NO_MATCH;
      }

      MethodTree enclosingMethod = findEnclosingNode(state.getPath(), MethodTree.class);
      if (enclosingMethod == null) {
        return NO_MATCH;
      }

      Analysis<NullnessValue, ?, ?> analysis = resultCache.get(enclosingMethod);
      if (analysis == null) {
        analysis = dataflow(enclosingMethod, state.getPath(), state.context,
            new NullnessPropagationTransfer()).getAnalysis();
        resultCache.put(enclosingMethod, analysis);
      }

      Name methodName = getSymbol(methodInvocation).getSimpleName();
      List<?> values = getAllValues(methodInvocation.getArguments(), analysis);
      String fixString = String.format("%s(%s)", methodName, Joiner.on(", ").join(values));

      return describeMatch(methodInvocation, replace(methodInvocation, fixString));
    }

    private static List<?> getAllValues(
        List<? extends Tree> args, Analysis<?, ?, ?> analysis) {
      List<Object> values = new ArrayList<>();
      for (Tree arg : args) {
        values.add(analysis.getValue(arg));
      }
      return values;
    }
  }
}
