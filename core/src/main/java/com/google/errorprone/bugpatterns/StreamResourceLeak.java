/*
 * Copyright 2016 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree;
import java.util.List;
import java.util.Objects;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "StreamResourceLeak",
    altNames = "FilesLinesLeak",
    category = JDK,
    summary =
        "Streams that encapsulate a closeable resource should be closed using"
            + " try-with-resources",
    severity = WARNING,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class StreamResourceLeak extends AbstractMustBeClosedChecker
    implements MethodInvocationTreeMatcher {

  public static final Matcher<ExpressionTree> MATCHER =
      MethodMatchers.staticMethod()
          .onClass("java.nio.file.Files")
          .namedAnyOf("lines", "newDirectoryStream", "list", "walk", "find");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    return matchNewClassOrMethodInvocation(tree, state);
  }

  @Override
  protected void addFix(Description.Builder description, ExpressionTree tree, VisitorState state) {
    TreePath parentPath = state.getPath().getParentPath();
    Tree parent = parentPath.getLeaf();
    SuggestedFix.Builder fix = SuggestedFix.builder();
    String streamType = SuggestedFixes.prettyType(state, fix, ASTHelpers.getReturnType(tree));
    if (parent instanceof MemberSelectTree) {
      StatementTree statement = state.findEnclosing(StatementTree.class);
      if (statement instanceof VariableTree) {
        // Variables need to be declared outside the try-with-resources:
        // e.g. `int count = Files.lines(p).count();`
        // -> `int count; try (Stream<String> stream = Files.lines(p)) { count = stream.count(); }`
        VariableTree var = (VariableTree) statement;
        int pos = ((JCTree) var).getStartPosition();
        int initPos = ((JCTree) var.getInitializer()).getStartPosition();
        int eqPos = pos + state.getSourceForNode(var).substring(0, initPos - pos).lastIndexOf('=');
        fix.replace(
            eqPos,
            initPos,
            String.format(
                ";\ntry (%s stream = %s) {\n%s =",
                streamType, state.getSourceForNode(tree), var.getName().toString()));
      } else {
        // the non-variable case, e.g. `return Files.lines(p).count()`
        // -> try (Stream<Stream> stream = Files.lines(p)) { return stream.count(); }`
        fix.prefixWith(
            statement,
            String.format("try (%s stream = %s) {\n", streamType, state.getSourceForNode(tree)));
      }
      fix.replace(tree, "stream");
      fix.postfixWith(statement, "}");
      description.addFix(fix.build());
    } else if (parent instanceof VariableTree) {
      // If the stream is assigned to a variable, wrap the variable in a try-with-resources
      // that includes all statements in the same block that reference the variable.
      Tree grandParent = parentPath.getParentPath().getLeaf();
      if (!(grandParent instanceof BlockTree)) {
        return;
      }
      List<? extends StatementTree> statements = ((BlockTree) grandParent).getStatements();
      int idx = statements.indexOf(parent);
      int lastUse = idx;
      for (int i = idx + 1; i < statements.size(); i++) {
        boolean[] found = {false};
        statements
            .get(i)
            .accept(
                new TreeScanner<Void, Void>() {
                  @Override
                  public Void visitIdentifier(IdentifierTree tree, Void unused) {
                    if (Objects.equals(ASTHelpers.getSymbol(tree), ASTHelpers.getSymbol(parent))) {
                      found[0] = true;
                    }
                    return null;
                  }
                },
                null);
        if (found[0]) {
          lastUse = i;
        }
      }
      fix.prefixWith(parent, "try (");
      fix.replace(
          state.getEndPosition(((VariableTree) parent).getInitializer()),
          state.getEndPosition(parent),
          ") {");
      fix.postfixWith(statements.get(lastUse), "}");
      description.addFix(fix.build());
    } else if (parent instanceof EnhancedForLoopTree) {
      // If the stream is used in a loop (e.g. directory streams), wrap the loop in
      // try-with-resources.
      fix.prefixWith(
          parent,
          String.format("try (%s stream = %s) {\n", streamType, state.getSourceForNode(tree)));
      fix.replace(tree, "stream");
      fix.postfixWith(parent, "}");
      description.addFix(fix.build());
    } else if (parent instanceof MethodInvocationTree) {
      // If the stream is used in a method that is called in an expression statement, wrap it in
      // try-with-resources.
      Tree grandParent = parentPath.getParentPath().getLeaf();
      if (!(grandParent instanceof ExpressionStatementTree)) {
        return;
      }
      fix.prefixWith(
          parent,
          String.format("try (%s stream = %s) {\n", streamType, state.getSourceForNode(tree)));
      fix.replace(tree, "stream");
      fix.postfixWith(grandParent, "}");
      description.addFix(fix.build());
    }
  }
}
