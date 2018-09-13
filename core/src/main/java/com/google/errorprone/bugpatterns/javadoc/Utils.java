package com.google.errorprone.bugpatterns.javadoc;

import static com.google.errorprone.names.LevenshteinEditDistance.getEditDistance;

import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.DocSourcePositions;
import com.sun.source.util.DocTreePath;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.tree.DCTree.DCDocComment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import java.util.Optional;
import javax.annotation.Nullable;

/** Common utility methods for fixing Javadocs. */
final class Utils {

  /** Maximum edit distance for fixing parameter names and tags. */
  private static final int EDIT_LIMIT = 5;

  static Optional<String> getBestMatch(String to, Iterable<String> choices) {
    String bestMatch = null;
    int minDistance = Integer.MAX_VALUE;
    for (String choice : choices) {
      int distance = getEditDistance(to, choice);
      if (distance < minDistance && distance < EDIT_LIMIT) {
        bestMatch = choice;
        minDistance = distance;
      }
    }
    return Optional.ofNullable(bestMatch);
  }

  static DCDocComment getDocComment(VisitorState state, Tree tree) {
    return ((JCCompilationUnit) state.getPath().getCompilationUnit())
        .docComments.getCommentTree((JCTree) tree);
  }

  static SuggestedFix replace(DocTree docTree, String replacement, VisitorState state) {
    DocSourcePositions positions = JavacTrees.instance(state.context).getSourcePositions();
    CompilationUnitTree compilationUnitTree = state.getPath().getCompilationUnit();
    int startPos = getStartPosition(docTree, state);
    int endPos =
        (int) positions.getEndPosition(compilationUnitTree, getDocCommentTree(state), docTree);
    return SuggestedFix.replace(startPos, endPos, replacement);
  }

  static int getStartPosition(DocTree docTree, VisitorState state) {
    DocSourcePositions positions = JavacTrees.instance(state.context).getSourcePositions();
    CompilationUnitTree compilationUnitTree = state.getPath().getCompilationUnit();
    return (int) positions.getStartPosition(compilationUnitTree, getDocCommentTree(state), docTree);
  }

  @Nullable
  static DocTreePath getDocTreePath(VisitorState state) {
    DocCommentTree docCommentTree = getDocCommentTree(state);
    if (docCommentTree == null) {
      return null;
    }
    return new DocTreePath(state.getPath(), docCommentTree);
  }

  @Nullable
  private static DocCommentTree getDocCommentTree(VisitorState state) {
    return JavacTrees.instance(state.context).getDocCommentTree(state.getPath());
  }

  private Utils() {}
}
