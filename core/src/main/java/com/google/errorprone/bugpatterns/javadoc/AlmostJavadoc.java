/*
 * Copyright 2019 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.BugPattern.StandardTags.STYLE;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.ErrorProneToken;
import com.google.errorprone.util.ErrorProneTokens;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.tree.JCTree;
import java.util.regex.Pattern;
import javax.lang.model.element.ElementKind;

/**
 * Flags comments which appear to be intended to be Javadoc, but are not started with an extra
 * {@code *}.
 */
@BugPattern(
    name = "AlmostJavadoc",
    summary =
        "This comment contains Javadoc or HTML tags, but isn't started with a double asterisk"
            + " (/**); is it meant to be Javadoc?",
    severity = WARNING,
    tags = STYLE,
    providesFix = REQUIRES_HUMAN_ATTENTION,
    documentSuppression = false)
public final class AlmostJavadoc extends BugChecker implements CompilationUnitTreeMatcher {
  private static final Pattern HAS_TAG =
      Pattern.compile(
          String.format(
              "<\\w+>|@(%s)",
              Streams.concat(
                      JavadocTag.VALID_CLASS_TAGS.stream(),
                      JavadocTag.VALID_METHOD_TAGS.stream(),
                      JavadocTag.VALID_VARIABLE_TAGS.stream())
                  .map(JavadocTag::name)
                  .map(Pattern::quote)
                  .distinct()
                  .collect(joining("|"))));

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    ImmutableMap<Integer, Tree> javadocableTrees = getJavadocableTrees(tree, state);
    for (ErrorProneToken token :
        ErrorProneTokens.getTokens(state.getSourceCode().toString(), state.context)) {
      for (Comment comment : token.comments()) {
        if (!javadocableTrees.containsKey(token.pos())) {
          continue;
        }
        if (!comment.getText().startsWith("/*") || comment.getText().startsWith("/**")) {
          continue;
        }
        if (HAS_TAG.matcher(comment.getText()).find()) {
          int pos = comment.getSourcePos(1);
          SuggestedFix fix = SuggestedFix.replace(pos, pos, "*");
          state.reportMatch(describeMatch(javadocableTrees.get(token.pos()), fix));
        }
      }
    }
    return NO_MATCH;
  }

  private ImmutableMap<Integer, Tree> getJavadocableTrees(
      CompilationUnitTree tree, VisitorState state) {
    ImmutableMap.Builder<Integer, Tree> javadoccablePositions = ImmutableMap.builder();
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitClass(ClassTree classTree, Void unused) {
        if (!shouldMatch()) {
          return null;
        }
        javadoccablePositions.put(startPos(classTree), classTree);
        return super.visitClass(classTree, null);
      }

      @Override
      public Void visitMethod(MethodTree methodTree, Void unused) {
        if (!shouldMatch()) {
          return null;
        }
        if (!ASTHelpers.isGeneratedConstructor(methodTree)) {
          javadoccablePositions.put(startPos(methodTree), methodTree);
        }
        return super.visitMethod(methodTree, null);
      }

      @Override
      public Void visitVariable(VariableTree variableTree, Void unused) {
        if (!shouldMatch()) {
          return null;
        }
        ElementKind kind = getSymbol(variableTree).getKind();
        if (kind == ElementKind.FIELD || kind == ElementKind.ENUM_CONSTANT) {
          javadoccablePositions.put(startPos(variableTree), variableTree);
        }
        return super.visitVariable(variableTree, null);
      }

      private boolean shouldMatch() {
        if (isSuppressed(getCurrentPath().getLeaf())) {
          return false;
        }
        // Check there isn't already a Javadoc for the element under question, otherwise we might
        // suggest double Javadoc.
        return Utils.getDocTreePath(state.withPath(getCurrentPath())) == null;
      }

      private int startPos(Tree tree) {
        return ((JCTree) tree).getStartPosition();
      }
    }.scan(tree, null);
    return javadoccablePositions.build();
  }
}
