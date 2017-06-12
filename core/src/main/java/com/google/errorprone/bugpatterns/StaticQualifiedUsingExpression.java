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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.matchers.Matchers.selectedIsInstance;
import static com.google.errorprone.matchers.Matchers.staticFieldAccess;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MemberSelectTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

/** @author eaftan@google.com (Eddie Aftandilian) */
@BugPattern(
  name = "StaticQualifiedUsingExpression",
  summary = "A static variable or method should be qualified with a class name, not expression",
  category = JDK,
  severity = WARNING,
  altNames = {"static", "static-access", "StaticAccessedFromInstance"},
  generateExamplesFromTestCases = false,
  tags = StandardTags.STYLE
)
public class StaticQualifiedUsingExpression extends BugChecker implements MemberSelectTreeMatcher {

  private static final String MESSAGE_TEMPLATE =
      "Static %s %s should not be accessed from an " + "object instance; instead use %s";

  private static final Matcher<ExpressionTree> staticAccessedFromInstanceMatcher =
      allOf(
          anyOf(staticMethod(), staticFieldAccess()),
          kindIs(Kind.MEMBER_SELECT),
          selectedIsInstance());

  @Override
  public Description matchMemberSelect(MemberSelectTree tree, VisitorState state) {
    if (!staticAccessedFromInstanceMatcher.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    // Is the static member being accessed a method or a variable?
    Symbol staticMemberSym = ASTHelpers.getSymbol(tree);
    if (staticMemberSym == null) {
      return Description.NO_MATCH;
    }
    boolean isMethod = staticMemberSym instanceof MethodSymbol;

    // Is the static member defined in this class?
    ClassSymbol ownerSym = staticMemberSym.owner.enclClass();
    ClassSymbol whereAccessedSym =
        ASTHelpers.getSymbol(
            ASTHelpers.findEnclosingNode(state.getPath().getParentPath(), ClassTree.class));
    SuggestedFix.Builder fix = SuggestedFix.builder();
    boolean staticMemberDefinedHere = whereAccessedSym.equals(ownerSym);
    String replacement;
    if (staticMemberDefinedHere && isMethod) {
      replacement = staticMemberSym.getSimpleName().toString();
    } else {
      replacement = qualifyType(state, fix, staticMemberSym);
    }
    fix.replace(tree, replacement);

    // Compute strings to interpolate into diagnostic message.
    String memberName = staticMemberSym.getSimpleName().toString();
    String methodOrVariable = isMethod ? "method" : "variable";

    String customDiagnosticMessage =
        String.format(MESSAGE_TEMPLATE, methodOrVariable, memberName, replacement);
    return buildDescription(tree).setMessage(customDiagnosticMessage).addFix(fix.build()).build();
  }
}
