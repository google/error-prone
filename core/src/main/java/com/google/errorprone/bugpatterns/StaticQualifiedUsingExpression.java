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
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MemberSelectTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.StatementTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.Objects;

/** @author eaftan@google.com (Eddie Aftandilian) */
@BugPattern(
    name = "StaticQualifiedUsingExpression",
    summary = "A static variable or method should be qualified with a class name, not expression",
    severity = ERROR,
    altNames = {"static", "static-access", "StaticAccessedFromInstance"},
    tags = StandardTags.FRAGILE_CODE)
public class StaticQualifiedUsingExpression extends BugChecker implements MemberSelectTreeMatcher {

  @Override
  public Description matchMemberSelect(MemberSelectTree tree, VisitorState state) {
    Symbol sym = getSymbol(tree);
    if (sym == null) {
      return NO_MATCH;
    }
    switch (sym.getKind()) {
      case FIELD:
        if (sym.getSimpleName().contentEquals("class")
            || sym.getSimpleName().contentEquals("super")) {
          return NO_MATCH;
        }
        // fall through
      case ENUM_CONSTANT:
      case METHOD:
        if (!sym.isStatic()) {
          return NO_MATCH;
        }
        break; // continue below
      default:
        return NO_MATCH;
    }
    ClassSymbol owner = sym.owner.enclClass();
    ExpressionTree expression = tree.getExpression();
    switch (expression.getKind()) {
      case MEMBER_SELECT:
      case IDENTIFIER:
        // References to static variables should be qualified by the type name of the owning type,
        // or a sub-type. e.g.: if CONST is declared in Foo, and SubFoo extends Foo,
        // allow `Foo.CONST` and `SubFoo.CONST` (but not, say, `new Foo().CONST`.
        Symbol base = getSymbol(expression);
        if (base instanceof ClassSymbol && base.isSubClass(owner, state.getTypes())) {
          return NO_MATCH;
        }
        break;
      default: // continue below
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    String replacement;
    boolean isMethod = sym instanceof MethodSymbol;
    if (isMethod && Objects.equals(getSymbol(state.findEnclosing(ClassTree.class)), owner)) {
      replacement = sym.getSimpleName().toString();
    } else {
      replacement = qualifyType(state, fix, sym);
    }
    fix.replace(tree, replacement);

    // Spill possibly side-effectful qualifier expressions to the top level.
    // This doesn't preserve order of operations for non-trivial expressions, but we don't have
    // letexprs and hopefully it'll call attention to the fact that just deleting the qualifier
    // might not always be the right fix.
    if (expression instanceof MethodInvocationTree || expression instanceof NewClassTree) {
      StatementTree statement = state.findEnclosing(StatementTree.class);
      if (statement != null) {
        fix.prefixWith(statement, state.getSourceForNode(expression) + ";");
      }
    }

    return buildDescription(tree)
        .setMessage(
            String.format(
                "Static %s %s should not be accessed from an object instance; instead use %s",
                isMethod ? "method" : "variable", sym.getSimpleName(), replacement))
        .addFix(fix.build())
        .build();
  }
}
