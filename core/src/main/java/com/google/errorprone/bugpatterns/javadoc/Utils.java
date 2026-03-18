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

package com.google.errorprone.bugpatterns.javadoc;

import static com.google.errorprone.names.LevenshteinEditDistance.getEditDistance;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.common.collect.ImmutableMap;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.ErrorPronePosition;
import com.google.errorprone.fixes.FixedPosition;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocSourcePositions;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.tree.DCTree.DCDocComment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Position;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.element.ElementKind;
import org.jspecify.annotations.Nullable;

/** Common utility methods for fixing Javadocs. */
final class Utils {
  static Optional<String> getBestMatch(String to, int maxEditDistance, Iterable<String> choices) {
    String bestMatch = null;
    int minDistance = Integer.MAX_VALUE;
    for (String choice : choices) {
      int distance = getEditDistance(to, choice);
      if (distance < minDistance && distance < maxEditDistance) {
        bestMatch = choice;
        minDistance = distance;
      }
    }
    return Optional.ofNullable(bestMatch);
  }

  static DCDocComment getDocComment(VisitorState state, Tree tree) {
    return (DCDocComment)
        ((JCCompilationUnit) state.getPath().getCompilationUnit())
            .docComments.getCommentTree((JCTree) tree);
  }

  static SuggestedFix replace(DocTree docTree, String replacement, VisitorState state) {
    DocSourcePositions positions = JavacTrees.instance(state.context).getSourcePositions();
    CompilationUnitTree compilationUnitTree = state.getPath().getCompilationUnit();
    int startPos = getStartPosition(docTree, state);
    int endPos =
        (int) positions.getEndPosition(compilationUnitTree, getDocCommentTree(state), docTree);
    if (startPos == Position.NOPOS || endPos == Position.NOPOS) {
      return SuggestedFix.emptyFix();
    }
    return SuggestedFix.replace(startPos, endPos, replacement);
  }

  static int getStartPosition(DocTree docTree, VisitorState state) {
    DocSourcePositions positions = JavacTrees.instance(state.context).getSourcePositions();
    CompilationUnitTree compilationUnitTree = state.getPath().getCompilationUnit();
    return (int) positions.getStartPosition(compilationUnitTree, getDocCommentTree(state), docTree);
  }

  static int getEndPosition(DocTree docTree, VisitorState state) {
    DocSourcePositions positions = JavacTrees.instance(state.context).getSourcePositions();
    CompilationUnitTree compilationUnitTree = state.getPath().getCompilationUnit();
    return (int) positions.getEndPosition(compilationUnitTree, getDocCommentTree(state), docTree);
  }

  /**
   * Gets a {@link DiagnosticPosition} for the {@link DocTree} pointed to by {@code path}, attached
   * to the {@link Tree} which it documents.
   */
  static ErrorPronePosition diagnosticPosition(DocTreePath path, VisitorState state) {
    int startPosition = getStartPosition(path.getLeaf(), state);
    Tree tree = path.getTreePath().getLeaf();
    if (startPosition == Position.NOPOS) {
      // javac doesn't seem to store positions for e.g. trivial empty javadoc like `/** */`
      // see: https://github.com/google/error-prone/issues/1981
      startPosition = ASTHelpers.getStartPosition(tree);
    }
    return getDiagnosticPosition(startPosition, tree);
  }

  static ErrorPronePosition getDiagnosticPosition(int startPosition, Tree tree) {
    return new FixedPosition(tree, startPosition);
  }

  static @Nullable DocTreePath getDocTreePath(VisitorState state) {
    DocCommentTree docCommentTree = getDocCommentTree(state);
    if (docCommentTree == null) {
      return null;
    }
    return new DocTreePath(state.getPath(), docCommentTree);
  }

  private static @Nullable DocCommentTree getDocCommentTree(VisitorState state) {
    return JavacTrees.instance(state.context).getDocCommentTree(state.getPath());
  }

  /** Returns a map of positions to trees which can be documented by Javadoc. */
  static ImmutableMap<Integer, TreePath> getJavadoccableTrees(CompilationUnitTree tree) {
    Map<Integer, TreePath> javadoccablePositions = new HashMap<>();
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitPackage(PackageTree packageTree, Void unused) {
        javadoccablePositions.put(ASTHelpers.getStartPosition(packageTree), getCurrentPath());
        return super.visitPackage(packageTree, null);
      }

      @Override
      public Void visitClass(ClassTree classTree, Void unused) {
        if (!(getSymbol(classTree).owner instanceof MethodSymbol)) {
          javadoccablePositions.put(ASTHelpers.getStartPosition(classTree), getCurrentPath());
        }
        return super.visitClass(classTree, null);
      }

      @Override
      public Void visitMethod(MethodTree methodTree, Void unused) {
        if (!ASTHelpers.isGeneratedConstructor(methodTree)) {
          javadoccablePositions.put(ASTHelpers.getStartPosition(methodTree), getCurrentPath());
        }
        return super.visitMethod(methodTree, null);
      }

      @Override
      public Void visitVariable(VariableTree variableTree, Void unused) {
        ElementKind kind = getSymbol(variableTree).getKind();
        if (kind == ElementKind.FIELD) {
          javadoccablePositions.put(ASTHelpers.getStartPosition(variableTree), getCurrentPath());
        }
        // For enum constants, skip past the desugared class declaration.
        if (kind == ElementKind.ENUM_CONSTANT) {
          javadoccablePositions.put(ASTHelpers.getStartPosition(variableTree), getCurrentPath());
          if (variableTree.getInitializer() instanceof NewClassTree newClassTree) {
            ClassTree classBody = newClassTree.getClassBody();
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
        javadoccablePositions.put(ASTHelpers.getStartPosition(moduleTree), getCurrentPath());
        return super.visitModule(moduleTree, null);
      }
    }.scan(tree, null);
    return ImmutableMap.copyOf(javadoccablePositions);
  }

  private Utils() {}
}
