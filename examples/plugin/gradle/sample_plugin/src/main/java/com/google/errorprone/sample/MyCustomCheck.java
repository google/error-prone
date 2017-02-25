/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.errorprone.sample;

import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.LinkType.CUSTOM;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.auto.service.AutoService;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import java.io.PrintStream;
import java.util.List;
import java.util.Objects;

/** Matches on string formatting inside print methods. */
@AutoService(BugChecker.class)
@BugPattern(
  name = "MyCustomCheck",
  category = JDK,
  summary = "String formatting inside print method",
  severity = ERROR,
  linkType = CUSTOM,
  link = "example.com/bugpattern/MyCustomCheck"
)
public class MyCustomCheck extends BugChecker implements MethodInvocationTreeMatcher {

  Matcher<ExpressionTree> PRINT_METHOD =
      instanceMethod().onDescendantOf(PrintStream.class.getName()).named("print");

  Matcher<ExpressionTree> STRING_FORMAT =
      staticMethod().onClass(String.class.getName()).named("format");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!PRINT_METHOD.matches(tree, state)) {
      return NO_MATCH;
    }
    Symbol base =
        tree.getMethodSelect()
            .accept(
                new TreeScanner<Symbol, Void>() {
                  @Override
                  public Symbol visitIdentifier(IdentifierTree node, Void unused) {
                    return ASTHelpers.getSymbol(node);
                  }

                  @Override
                  public Symbol visitMemberSelect(MemberSelectTree node, Void unused) {
                    return super.visitMemberSelect(node, null);
                  }
                },
                null);
    if (!Objects.equals(base, state.getSymtab().systemType.tsym)) {
      return NO_MATCH;
    }
    ExpressionTree arg = Iterables.getOnlyElement(tree.getArguments());
    if (!STRING_FORMAT.matches(arg, state)) {
      return NO_MATCH;
    }
    List<? extends ExpressionTree> formatArgs = ((MethodInvocationTree) arg).getArguments();
    return describeMatch(
        tree,
        SuggestedFix.builder()
            .replace(
                ((JCTree) tree).getStartPosition(),
                ((JCTree) formatArgs.get(0)).getStartPosition(),
                "System.err.printf(")
            .replace(
                state.getEndPosition((JCTree) getLast(formatArgs)),
                state.getEndPosition((JCTree) tree),
                ")")
            .build());
  }
}
