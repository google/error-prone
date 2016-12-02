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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import javax.lang.model.element.ElementKind;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "FilesLinesLeak",
  category = JDK,
  summary = "The stream returned by Files.lines should be closed using try-with-resources",
  severity = ERROR
)
public class FilesLinesLeak extends BugChecker implements MethodInvocationTreeMatcher {

  public static final Matcher<ExpressionTree> MATCHER =
      MethodMatchers.staticMethod().onClass("java.nio.file.Files").named("lines");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    if (inTWR(state)) {
      return NO_MATCH;
    }
    Description.Builder description = buildDescription(tree);
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (parent instanceof MemberSelectTree) {
      MemberSelectTree select = (MemberSelectTree) parent;
      StatementTree statement = state.findEnclosing(StatementTree.class);
      SuggestedFix.Builder fix = SuggestedFix.builder();
      if (statement instanceof VariableTree) {
        VariableTree var = (VariableTree) statement;
        int pos = ((JCTree) var).getStartPosition();
        int initPos = ((JCTree) var.getInitializer()).getStartPosition();
        int eqPos = pos + state.getSourceForNode(var).substring(0, initPos - pos).lastIndexOf('=');
        fix.replace(
            eqPos,
            initPos,
            String.format(
                ";\ntry (Stream<String> stream = %s) {\n%s =",
                state.getSourceForNode(tree), var.getName().toString()));
      } else {
        fix.prefixWith(
            statement,
            String.format("try (Stream<String> stream = %s) {\n", state.getSourceForNode(tree)));
        fix.replace(select.getExpression(), "stream");
      }
      fix.replace(tree, "stream");
      fix.postfixWith(statement, "}");
      fix.addImport("java.util.stream.Stream");
      description.addFix(fix.build());
    }
    return description.build();
  }

  private boolean inTWR(VisitorState state) {
    TreePath path = state.getPath().getParentPath();
    while (path.getLeaf().getKind() == Tree.Kind.CONDITIONAL_EXPRESSION) {
      path = path.getParentPath();
    }
    Symbol sym = ASTHelpers.getSymbol(path.getLeaf());
    return sym != null && sym.getKind() == ElementKind.RESOURCE_VARIABLE;
  }
}
