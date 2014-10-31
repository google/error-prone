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
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.matchers.Matchers.selectedIsInstance;
import static com.google.errorprone.matchers.Matchers.staticFieldAccess;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
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

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@BugPattern(name = "StaticAccessedFromInstance",
    summary = "A static variable or method should not be accessed from an object instance",
    explanation = "A static variable or method should never be accessed from an instance.  This "
        + "hides the fact that the variable or method is static and does not depend on the value "
        + "of the object instance on which this variable or method is being invoked.",
    category = JDK, severity = WARNING, maturity = MATURE, altNames = "static")
public class StaticAccessedFromInstance extends BugChecker implements MemberSelectTreeMatcher {

  private static final String MESSAGE_TEMPLATE = "Static %s %s should not be accessed from an "
      + "object instance; instead use %s";

  private static final Matcher<ExpressionTree> staticAccessedFromInstanceMatcher = allOf(
      anyOf(
          staticMethod("*", "*"),
          staticFieldAccess()),
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
    Symbol ownerSym = staticMemberSym.owner;
    Symbol whereAccessedSym = ASTHelpers.getSymbol(
        ASTHelpers.findEnclosingNode(state.getPath().getParentPath(), ClassTree.class));
    if (!(ownerSym instanceof ClassSymbol && whereAccessedSym instanceof ClassSymbol)) {
      return Description.NO_MATCH;
    }
    boolean staticMemberDefinedHere = whereAccessedSym.equals(ownerSym);

    SuggestedFix.Builder fix = SuggestedFix.builder();
    String replacement;
    if (staticMemberDefinedHere && isMethod) {
      // If the static member is defined in the enclosing class and the member is a method, then
      // just use the bare method name. Don't do this for fields, because they may share a simple
      // name with a local in the same scope.
      // TODO(user): If we had access to name resolution info, we could do this in all applicable
      // cases.  Investigate Scope.Entry for this.
      replacement = tree.getIdentifier().toString();
    } else {
      // Replace the operand of the field access expression with the simple name of the class.
      replacement = ownerSym.getSimpleName() + "." + tree.getIdentifier();

      // Don't import implicitly imported packages (java.lang.* and current package).
      // TODO(user): move this logic into addImport?
      Symbol packageSym = ownerSym.packge();
      if (!packageSym.toString().equals("java.lang")
          && !packageSym.equals(whereAccessedSym.packge())) {
        fix.addImport(ownerSym.toString());
      }
    }
    fix.replace(tree, replacement);

    // Compute strings to interpolate into diagnostic message.
    String memberName = staticMemberSym.getSimpleName().toString();
    String methodOrVariable = isMethod ? "method" : "variable";

    String customDiagnosticMessage = String.format(MESSAGE_TEMPLATE,
        methodOrVariable, memberName, replacement);
    return buildDescription(tree)
        .setMessage(customDiagnosticMessage)
        .addFix(fix.build())
        .build();
  }
}
