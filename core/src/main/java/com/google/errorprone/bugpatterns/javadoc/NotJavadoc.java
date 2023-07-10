/*
 * Copyright 2023 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.javadoc;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.bugpatterns.javadoc.Utils.getDiagnosticPosition;
import static com.google.errorprone.fixes.SuggestedFix.replace;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ErrorProneTokens.getTokens;
import static com.sun.tools.javac.parser.Tokens.Comment.CommentStyle.JAVADOC;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.ErrorProneToken;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.parser.Tokens.Comment;
import java.util.HashMap;
import java.util.Map;
import javax.lang.model.element.ElementKind;

/** A BugPattern; see the summary. */
@BugPattern(
    summary = "Avoid using `/**` for comments which aren't actually Javadoc.",
    severity = WARNING,
    documentSuppression = false)
public final class NotJavadoc extends BugChecker implements CompilationUnitTreeMatcher {
  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    ImmutableMap<Integer, Tree> javadocableTrees = getJavadoccableTrees(tree);
    ImmutableRangeSet<Integer> suppressedRegions = suppressedRegions(state);
    for (ErrorProneToken token : getTokens(state.getSourceCode().toString(), state.context)) {
      for (Comment comment : token.comments()) {
        if (!comment.getStyle().equals(JAVADOC) || comment.getText().equals("/**/")) {
          continue;
        }
        if (javadocableTrees.containsKey(token.pos())) {
          continue;
        }

        if (suppressedRegions.intersects(
            Range.closed(
                comment.getSourcePos(0), comment.getSourcePos(comment.getText().length() - 1)))) {
          continue;
        }

        int endPos = 2;
        while (comment.getText().charAt(endPos) == '*') {
          endPos++;
        }
        state.reportMatch(
            describeMatch(
                getDiagnosticPosition(comment.getSourcePos(0), tree),
                replace(comment.getSourcePos(1), comment.getSourcePos(endPos - 1), "")));
      }
    }
    return NO_MATCH;
  }

  private ImmutableMap<Integer, Tree> getJavadoccableTrees(CompilationUnitTree tree) {
    Map<Integer, Tree> javadoccablePositions = new HashMap<>();
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitPackage(PackageTree packageTree, Void unused) {
        javadoccablePositions.put(getStartPosition(packageTree), packageTree);
        return super.visitPackage(packageTree, null);
      }

      @Override
      public Void visitClass(ClassTree classTree, Void unused) {
        if (!(getSymbol(classTree).owner instanceof MethodSymbol)) {
          javadoccablePositions.put(getStartPosition(classTree), classTree);
        }
        return super.visitClass(classTree, null);
      }

      @Override
      public Void visitMethod(MethodTree methodTree, Void unused) {
        if (!ASTHelpers.isGeneratedConstructor(methodTree)) {
          javadoccablePositions.put(getStartPosition(methodTree), methodTree);
        }
        return super.visitMethod(methodTree, null);
      }

      @Override
      public Void visitVariable(VariableTree variableTree, Void unused) {
        ElementKind kind = getSymbol(variableTree).getKind();
        if (kind == ElementKind.FIELD) {
          javadoccablePositions.put(getStartPosition(variableTree), variableTree);
        }
        // For enum constants, skip past the desugared class declaration.
        if (kind == ElementKind.ENUM_CONSTANT) {
          javadoccablePositions.put(getStartPosition(variableTree), variableTree);
          if (variableTree.getInitializer() instanceof NewClassTree) {
            ClassTree classBody = ((NewClassTree) variableTree.getInitializer()).getClassBody();
            if (classBody != null) {
              scan(classBody.getMembers(), null);
            }
            return null;
          }
        }
        return super.visitVariable(variableTree, null);
      }

      @Override
      public Void visitModule(ModuleTree moduleTree, Void unused) {
        javadoccablePositions.put(getStartPosition(moduleTree), moduleTree);
        return super.visitModule(moduleTree, null);
      }
    }.scan(tree, null);
    return ImmutableMap.copyOf(javadoccablePositions);
  }
}
