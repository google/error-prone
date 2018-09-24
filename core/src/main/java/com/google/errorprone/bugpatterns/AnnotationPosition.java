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

package com.google.errorprone.bugpatterns;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.BugPattern.LinkType.CUSTOM;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.BugPattern.StandardTags.STYLE;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.ErrorProneToken;
import com.google.errorprone.util.ErrorProneTokens;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.TypeAnnotations.AnnotationType;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;

/**
 * Checks annotation positioning, and orphaned Javadocs.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@BugPattern(
    name = "AnnotationPosition",
    summary = "Annotations should be positioned after Javadocs, but before modifiers..",
    severity = WARNING,
    tags = STYLE,
    linkType = CUSTOM,
    link = "https://google.github.io/styleguide/javaguide.html#s4.8.5-annotations",
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public final class AnnotationPosition extends BugChecker
    implements ClassTreeMatcher, MethodTreeMatcher, VariableTreeMatcher {

  private static final ImmutableMap<String, TokenKind> TOKEN_KIND_BY_NAME =
      Arrays.stream(TokenKind.values()).collect(toImmutableMap(tk -> tk.name(), tk -> tk));

  private static final ImmutableSet<TokenKind> MODIFIERS =
      Arrays.stream(Modifier.values())
          .map(m -> TOKEN_KIND_BY_NAME.get(m.name()))
          .collect(toImmutableSet());

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    return handle(tree, tree.getSimpleName(), tree.getModifiers(), state);
  }

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    return handle(tree, tree.getName(), tree.getModifiers(), state);
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    Symbol symbol = getSymbol(tree);
    if (symbol.getKind() != ElementKind.FIELD) {
      return NO_MATCH;
    }
    return handle(tree, tree.getName(), tree.getModifiers(), state);
  }

  private Description handle(Tree tree, Name name, ModifiersTree modifiers, VisitorState state) {
    List<? extends AnnotationTree> annotations = modifiers.getAnnotations();
    if (annotations.isEmpty()) {
      return NO_MATCH;
    }

    int treePos = ((JCTree) tree).getStartPosition();
    List<ErrorProneToken> tokens = annotationTokens(tree, state, treePos);
    Comment danglingJavadoc = findOrphanedJavadoc(name, tokens);

    ImmutableList<ErrorProneToken> modifierTokens =
        tokens.stream().filter(t -> MODIFIERS.contains(t.kind())).collect(toImmutableList());
    if (!modifierTokens.isEmpty()) {
      int firstModifierPos = treePos + modifierTokens.get(0).pos();
      int lastModifierPos = treePos + getLast(modifierTokens).endPos();

      Description description =
          checkAnnotations(
              tree,
              treePos,
              annotations,
              danglingJavadoc,
              firstModifierPos,
              lastModifierPos,
              state);
      if (!description.equals(NO_MATCH)) {
        return description;
      }
    }
    if (danglingJavadoc != null) {
      SuggestedFix.Builder builder = SuggestedFix.builder();
      String javadoc = removeJavadoc(state, treePos, danglingJavadoc, builder);

      String message = "Javadocs should appear before any modifiers or annotations.";
      return buildDescription(tree)
          .setMessage(message)
          .addFix(builder.prefixWith(tree, javadoc).build())
          .build();
    }
    return NO_MATCH;
  }

  /** Tokenizes as little of the {@code tree} as possible to ensure we grab all the annotations. */
  private static ImmutableList<ErrorProneToken> annotationTokens(
      Tree tree, VisitorState state, int annotationEnd) {
    int endPos;
    if (tree instanceof JCMethodDecl) {
      JCMethodDecl methodTree = (JCMethodDecl) tree;
      endPos =
          methodTree.getBody() == null
              ? state.getEndPosition(methodTree)
              : methodTree.getBody().getStartPosition();
    } else if (tree instanceof JCVariableDecl) {
      endPos = ((JCVariableDecl) tree).getType().getStartPosition();
    } else if (tree instanceof JCClassDecl) {
      JCClassDecl classTree = (JCClassDecl) tree;
      endPos =
          classTree.getMembers().isEmpty()
              ? state.getEndPosition(classTree)
              : classTree.getMembers().get(0).getStartPosition();
    } else {
      throw new AssertionError();
    }
    return ErrorProneTokens.getTokens(
        state.getSourceCode().subSequence(annotationEnd, endPos).toString(), state.context);
  }

  /** Checks that annotations are on the right side of the modifiers. */
  private Description checkAnnotations(
      Tree tree,
      int treePos,
      List<? extends AnnotationTree> annotations,
      Comment danglingJavadoc,
      int firstModifierPos,
      int lastModifierPos,
      VisitorState state) {
    SuggestedFix.Builder builder = SuggestedFix.builder();
    List<AnnotationTree> moveBefore = new ArrayList<>();
    List<AnnotationTree> moveAfter = new ArrayList<>();

    boolean annotationProblem = false;
    for (AnnotationTree annotation : annotations) {
      int annotationPos = ((JCTree) annotation).getStartPosition();
      if (annotationPos <= firstModifierPos) {
        continue;
      }
      AnnotationType annotationType =
          ASTHelpers.getAnnotationType(annotation, getSymbol(tree), state);
      if (annotationPos >= lastModifierPos) {
        if (tree instanceof ClassTree || annotationType == AnnotationType.DECLARATION) {
          annotationProblem = true;
          moveBefore.add(annotation);
        }
      } else {
        annotationProblem = true;
        if (tree instanceof ClassTree
            || annotationType == AnnotationType.DECLARATION
            || annotationType == null) {
          moveBefore.add(annotation);
        } else {
          moveAfter.add(annotation);
        }
      }
    }
    if (annotationProblem) {
      for (AnnotationTree annotation : moveBefore) {
        builder.delete(annotation);
      }
      for (AnnotationTree annotation : moveAfter) {
        builder.delete(annotation);
      }
      String javadoc =
          danglingJavadoc == null ? "" : removeJavadoc(state, treePos, danglingJavadoc, builder);
      builder
          .replace(
              firstModifierPos,
              firstModifierPos,
              String.format("%s%s ", javadoc, joinSource(state, moveBefore)))
          .replace(
              lastModifierPos, lastModifierPos, String.format("%s ", joinSource(state, moveAfter)));
      ImmutableList<String> names =
          annotations.stream()
              .map(ASTHelpers::getSymbol)
              .filter(Objects::nonNull)
              .map(Symbol::getSimpleName)
              .map(a -> "@" + a)
              .collect(toImmutableList());
      String flattened = names.stream().collect(joining(", "));
      String isAre = names.size() > 1 ? "are not type annotations" : "is not a type annotation";
      String message =
          String.format(
              "%s %s, so should appear before any modifiers and after Javadocs.", flattened, isAre);
      return buildDescription(tree).setMessage(message).addFix(builder.build()).build();
    }
    return NO_MATCH;
  }

  private static String joinSource(VisitorState state, List<AnnotationTree> moveBefore) {
    return moveBefore.stream().map(state::getSourceForNode).collect(joining(" "));
  }

  private static String removeJavadoc(
      VisitorState state, int startPos, Comment danglingJavadoc, SuggestedFix.Builder builder) {
    int javadocStart = startPos + danglingJavadoc.getSourcePos(0);
    int javadocEnd = javadocStart + danglingJavadoc.getText().length();
    // Capturing an extra newline helps the formatter.
    if (state.getSourceCode().charAt(javadocEnd) == '\n') {
      javadocEnd++;
    }
    builder.replace(javadocStart, javadocEnd, "");
    return danglingJavadoc.getText();
  }

  @Nullable
  private static Comment findOrphanedJavadoc(Name name, List<ErrorProneToken> tokens) {
    for (ErrorProneToken token : tokens) {
      for (Comment comment : token.comments()) {
        if (comment.getText().startsWith("/**")) {
          return comment;
        }
      }
      if (token.kind() == TokenKind.IDENTIFIER && token.name().equals(name)) {
        return null;
      }
    }
    return null;
  }
}
