/*
 * Copyright 2020 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getThrownExceptions;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isCheckedExceptionType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static com.google.errorprone.util.ASTHelpers.methodCanBeOverridden;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.ErrorProneToken;
import com.google.errorprone.util.ErrorProneTokens;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.ThrowsTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.DocSourcePositions;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import java.util.Objects;
import javax.lang.model.element.Element;

/** Flags checked exceptions which are claimed to be thrown, but are not. */
@BugPattern(
    name = "CheckedExceptionNotThrown",
    summary =
        "This method cannot throw a checked exception that it claims to. This may cause consumers"
            + " of the API to incorrectly attempt to handle, or propagate, this exception.",
    severity = WARNING)
public final class CheckedExceptionNotThrown extends BugChecker implements MethodTreeMatcher {
  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (tree.getThrows().isEmpty()) {
      return NO_MATCH;
    }
    // Don't match test methods: that's rather noisy.
    if (methodCanBeOverridden(getSymbol(tree))
        || state.errorProneOptions().isTestOnlyTarget()
        || tree.getBody() == null) {
      return NO_MATCH;
    }

    ImmutableSet<Type> thrownExceptions =
        getThrownExceptions(tree.getBody(), state).stream()
            .filter(t -> isCheckedExceptionType(t, state))
            .collect(toImmutableSet());

    ImmutableList<ExpressionTree> canActuallyBeThrown =
        tree.getThrows().stream()
            .filter(
                et -> {
                  Type type = getType(et);
                  return !isCheckedExceptionType(type, state)
                      || thrownExceptions.stream().anyMatch(t -> isSubtype(t, type, state));
                })
            .collect(toImmutableList());

    if (tree.getThrows().equals(canActuallyBeThrown)) {
      return NO_MATCH;
    }

    ImmutableSet<Type> thrownTypes =
        canActuallyBeThrown.stream()
            .map(ASTHelpers::getType)
            .filter(Objects::nonNull)
            .collect(toImmutableSet());

    String unthrown =
        tree.getThrows().stream()
            .filter(et -> !canActuallyBeThrown.contains(et))
            .map(state::getSourceForNode)
            .sorted()
            .collect(joining(", ", "(", ")"));
    String description =
        String.format(
            "This method does not throw checked exceptions %s despite claiming to. This may cause"
                + " consumers of the API to incorrectly attempt to handle, or propagate, this"
                + " exception.",
            unthrown);

    SuggestedFix throwsFix =
        canActuallyBeThrown.isEmpty()
            ? deleteEntireThrowsClause(tree, state)
            : SuggestedFix.replace(
                getStartPosition(tree.getThrows().get(0)),
                state.getEndPosition(getLast(tree.getThrows())),
                canActuallyBeThrown.stream().map(state::getSourceForNode).collect(joining(", ")));
    SuggestedFix fix =
        SuggestedFix.builder().merge(fixJavadoc(thrownTypes, state)).merge(throwsFix).build();
    return buildDescription(tree.getThrows().get(0)).setMessage(description).addFix(fix).build();
  }

  private SuggestedFix fixJavadoc(ImmutableSet<Type> actuallyThrownTypes, VisitorState state) {
    DocCommentTree docCommentTree =
        JavacTrees.instance(state.context).getDocCommentTree(state.getPath());
    if (docCommentTree == null) {
      return SuggestedFix.emptyFix();
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    DocTreePath docTreePath = new DocTreePath(state.getPath(), docCommentTree);
    new DocTreePathScanner<Void, Void>() {
      @Override
      public Void visitThrows(ThrowsTree throwsTree, Void unused) {
        ReferenceTree exName = throwsTree.getExceptionName();
        Element element =
            JavacTrees.instance(state.context)
                .getElement(new DocTreePath(getCurrentPath(), exName));
        if (element != null) {
          Type type = (Type) element.asType();
          if (!actuallyThrownTypes.contains(type)) {
            DocSourcePositions positions = JavacTrees.instance(state.context).getSourcePositions();
            CompilationUnitTree compilationUnitTree = state.getPath().getCompilationUnit();
            fix.replace(
                (int) positions.getStartPosition(compilationUnitTree, docCommentTree, throwsTree),
                (int) positions.getEndPosition(compilationUnitTree, docCommentTree, throwsTree),
                "");
          }
        }
        return super.visitThrows(throwsTree, null);
      }
    }.scan(docTreePath, null);
    return fix.build();
  }

  private static SuggestedFix deleteEntireThrowsClause(MethodTree tree, VisitorState state) {
    int endPos = state.getEndPosition(getLast(tree.getThrows()));
    int methodStartPos = getStartPosition(tree);

    int startPos =
        ErrorProneTokens.getTokens(
                state.getSourceCode().subSequence(methodStartPos, endPos).toString(),
                methodStartPos,
                state.context)
            .stream()
            .filter(token -> token.kind().equals(TokenKind.THROWS))
            .findFirst()
            .map(ErrorProneToken::pos)
            .get();
    return SuggestedFix.replace(startPos, endPos, "");
  }
}
