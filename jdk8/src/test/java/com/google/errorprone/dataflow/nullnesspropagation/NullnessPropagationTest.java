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
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.dataflow.DataFlow;
import com.google.errorprone.dataflow.DataFlow.Result;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;

import org.checkerframework.dataflow.analysis.Analysis;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

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
      summary = "Dataflow analysis of method definitions for testing nullness propagation",
      explanation =
          "This test checker runs dataflow analysis over method definitions to test nullness"
          + "propagation and generate a control flow graph.",
      category = JDK, severity = ERROR, maturity = EXPERIMENTAL)
  public class NullnessPropagationChecker extends BugChecker
      implements MethodInvocationTreeMatcher {

    private final Matcher<ExpressionTree> matcher =
        staticMethod(
            NullnessPropagationTest.class.getName(),
            "triggerNullnessChecker");
    
    /**
     * Compute a dataflow result once per method declaration and store it in the map
     */
    private final Map<MethodTree,
        Result<NullnessValue, NullnessPropagationStore, NullnessPropagationTransfer>> results =
        new HashMap<>();
    
    /**
     * Uses this test class' static method {@code triggerNullnessChecker} to match and check the
     * abstract value of a local variable
     */
    @Override
    public Description matchMethodInvocation(
        MethodInvocationTree methodInvocation, final VisitorState state) {
      if (!matcher.matches(methodInvocation, state)) {
        return Description.NO_MATCH;
      }
            
      MethodTree enclosingMethod = ASTHelpers.findEnclosingNode(state.getPath(), MethodTree.class);
      Analysis<NullnessValue, NullnessPropagationStore, NullnessPropagationTransfer> analysis;
      
      if (results.containsKey(enclosingMethod)) {
        analysis = results.get(enclosingMethod).getAnalysis();
      } else {
        NullnessPropagationTransfer transfer = new NullnessPropagationTransfer();
        Result<NullnessValue, NullnessPropagationStore, NullnessPropagationTransfer> result =
            DataFlow.dataflow(enclosingMethod, state.getPath(), state.context, transfer);
        results.put(enclosingMethod, result);
        analysis = result.getAnalysis();
      }
            
      // Examine values for nodes of interest: argument(s) to the method invocation
      List<? extends ExpressionTree> args = methodInvocation.getArguments();
      if (methodInvocation.getMethodSelect() instanceof MemberSelectTree) {
        MemberSelectTree methodName = (MemberSelectTree) methodInvocation.getMethodSelect();
        String fixString = methodName.getIdentifier().toString() + "(";
        for (int i = 0; i < args.size(); ++i) {
          fixString += analysis.getValue(args.get(i)).toString();
          if (i + 1 < args.size()) {
            fixString += ", ";
          }
        }
        fixString += ")";
        
        Fix fix = SuggestedFix.replace(methodInvocation, fixString);
        return describeMatch(methodInvocation, fix);
      }
      
      return Description.NO_MATCH;
    }
  }
}