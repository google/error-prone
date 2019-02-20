/*
 * Copyright 2015 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.LinkType.CUSTOM;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BlockTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.Pretty;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "MultiVariableDeclaration",
    summary = "Variable declarations should declare only one variable",
    severity = SUGGESTION,
    linkType = CUSTOM,
    tags = StandardTags.STYLE,
    link = "https://google.github.io/styleguide/javaguide.html#s4.8.2.1-variables-per-declaration"
    )
public class MultiVariableDeclaration extends BugChecker
    implements ClassTreeMatcher, BlockTreeMatcher {

  @Override
  public Description matchBlock(BlockTree tree, VisitorState state) {
    return checkDeclarations(tree.getStatements(), state);
  }

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    return checkDeclarations(tree.getMembers(), state);
  }

  private Description checkDeclarations(List<? extends Tree> children, VisitorState state) {
    PeekingIterator<Tree> it = Iterators.<Tree>peekingIterator(children.iterator());
    while (it.hasNext()) {
      if (it.peek().getKind() != Tree.Kind.VARIABLE) {
        it.next();
        continue;
      }
      VariableTree variableTree = (VariableTree) it.next();
      ArrayList<JCVariableDecl> fragments = new ArrayList<>();
      fragments.add((JCVariableDecl) variableTree);
      // Javac handles multi-variable declarations by lowering them in the parser into a series of
      // individual declarations, all of which have the same start position. We search for the first
      // declaration in the group, which is either the first variable declared in this scope or has
      // a distinct end position from the previous declaration.
      while (it.hasNext()
          && it.peek().getKind() == Tree.Kind.VARIABLE
          && ((JCTree) variableTree).getStartPosition()
              == ((JCTree) it.peek()).getStartPosition()) {
        fragments.add((JCVariableDecl) it.next());
      }
      if (fragments.size() == 1) {
        continue;
      }
      Fix fix =
          SuggestedFix.replace(
              fragments.get(0).getStartPosition(),
              state.getEndPosition(Iterables.getLast(fragments)),
              fragments.stream().map(this::pretty).collect(joining("")));
      state.reportMatch(describeMatch(fragments.get(0), fix));
    }
    return NO_MATCH;
  }

  private String pretty(JCVariableDecl variableDecl) {
    StringWriter sw = new StringWriter();
    try {
      new Pretty(sw, true) {
        @Override
        public void visitAnnotation(JCAnnotation anno) {
          if (anno.getArguments().isEmpty()) {
            try {
              print("@");
              printExpr(anno.annotationType);
            } catch (IOException e) {
              throw new UncheckedIOException(e);
            }
          } else {
            super.visitAnnotation(anno);
          }
        }
      }.printStat(variableDecl);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return sw.toString();
  }
}
