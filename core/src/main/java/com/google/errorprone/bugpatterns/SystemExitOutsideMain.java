/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.enclosingMethod;
import static com.google.errorprone.matchers.Matchers.hasModifier;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.methodHasArity;
import static com.google.errorprone.matchers.Matchers.methodHasParameters;
import static com.google.errorprone.matchers.Matchers.methodHasVisibility;
import static com.google.errorprone.matchers.Matchers.methodIsNamed;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.matchers.MethodVisibility.Visibility.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers.MethodNameMatcher;
import com.google.errorprone.suppliers.Suppliers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import java.util.Optional;

/**
 * Check for calls to {@code System.exit()} outside of a main method.
 *
 * @author seibelsabrina@google.com (Sabrina Seibel)
 */
@BugPattern(
    name = "SystemExitOutsideMain",
    summary = "Code that contains System.exit() is untestable.",
    severity = WARNING,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class SystemExitOutsideMain extends BugChecker implements MethodInvocationTreeMatcher {
  private static final MethodNameMatcher CALLS_TO_SYSTEM_EXIT =
      staticMethod().onClass("java.lang.System").named("exit");

  private static final Matcher<MethodTree> MAIN_METHOD =
      allOf(
          methodHasArity(1),
          methodHasVisibility(PUBLIC),
          hasModifier(STATIC),
          methodReturns(Suppliers.VOID_TYPE),
          methodIsNamed("main"),
          methodHasParameters(isSameType(Suppliers.arrayOf(Suppliers.STRING_TYPE))));

  private static final Matcher<ExpressionTree> CALLS_TO_SYSTEM_EXIT_OUTSIDE_MAIN =
      allOf(CALLS_TO_SYSTEM_EXIT, not(enclosingMethod(MAIN_METHOD)));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (CALLS_TO_SYSTEM_EXIT_OUTSIDE_MAIN.matches(tree, state)) {
      Optional<? extends Tree> mainMethodInThisClass =
          ASTHelpers.findEnclosingNode(state.getPath(), ClassTree.class).getMembers().stream()
              .filter(t -> t instanceof MethodTree)
              .filter(t -> MAIN_METHOD.matches((MethodTree) t, state))
              .findAny();
      return mainMethodInThisClass.isPresent() ? Description.NO_MATCH : describeMatch(tree);
    }
    return Description.NO_MATCH;
  }
}
