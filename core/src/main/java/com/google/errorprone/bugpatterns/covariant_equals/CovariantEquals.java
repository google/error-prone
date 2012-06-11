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

package com.google.errorprone.bugpatterns.covariant_equals;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.MethodVisibility.Visibility;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.EnclosingClass.findEnclosingClass;
import static com.google.errorprone.matchers.Matchers.*;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
@BugPattern(name = "CovariantEquals",
    summary = "equals() method doesn't override Object.equals()",
    explanation = "To be used by many libraries, an `equals` method must override `Object.equals`," +
        "which has a single parameter of type `java.lang.Object`. " +
        "Defining a method which looks like `equals` but doesn't have the same signature is dangerous, " +
        "since comparisons will have different results depending on which `equals` is called.",
    category = JDK, maturity = EXPERIMENTAL, severity = ERROR)
public class CovariantEquals extends DescribingMatcher<MethodTree> {
  
  /**
   * Matches any method definitions that fit the following:
   * 1) Defined method is named "equals."
   * 2) Defined method returns a boolean.
   * 3) Defined method takes a single parameter of the same type as the enclosing class.
   * 4) The enclosing class does not have a method defined that really overrides Object.equals().
   */
  @Override
  @SuppressWarnings("unchecked")    // matchers + varargs cause this
  public boolean matches(MethodTree methodTree, VisitorState state) { 
    return allOf(
        methodHasVisibility(Visibility.PUBLIC),
        methodIsNamed("equals"),
        methodReturns(state.getSymtab().booleanType),
        methodHasParameters(variableType(isSameType(findEnclosingClass(state)))),
        enclosingClass(not(hasMethod(allOf(MethodTree.class,
            methodIsNamed("equals"),
            methodReturns(state.getSymtab().booleanType),
            methodHasParameters(variableType(isSameType(state.getSymtab().objectType)))))))
    ).matches(methodTree, state);
  }

  /**
   * Generates a new method that overrides Object.equals. 
   */
  @Override
  public Description describe(MethodTree methodTree, VisitorState state) {
    /* Transformation:
     * 1) Change method signature, substituting "Object" for the parameter type.
     * 2) Insert at the start of the method body:
     *    if (!(<parameter name> instanceof <parameter type>)) {
     *      return false;
     *    }
     * 3) For each usage of the parameter in the method, cast it to the
     *    parameter type.
     */
    JCTree parameterType = (JCTree) methodTree.getParameters().get(0).getType();
    Name parameterName = ((JCVariableDecl) methodTree.getParameters().get(0)).getName();
    
    SuggestedFix changeMethodDecl = new SuggestedFix()
        .replace(parameterType, "Object");
    //SuggestedFix addType
    return new Description(methodTree, diagnosticMessage, changeMethodDecl);
  }
  
  public static class Scanner extends com.google.errorprone.Scanner {
    private CovariantEquals matcher = new CovariantEquals();

    @Override
    public Void visitMethod(MethodTree node, VisitorState visitorState) {
      evaluateMatch(node, visitorState, matcher);
      return super.visitMethod(node, visitorState);
    }
  }
}
