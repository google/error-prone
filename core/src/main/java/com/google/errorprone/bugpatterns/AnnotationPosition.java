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
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Streams.stream;
import static com.google.errorprone.BugPattern.LinkType.CUSTOM;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getAnnotationType;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.ErrorProneComment;
import com.google.errorprone.util.ErrorProneToken;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.TypeAnnotations.AnnotationType;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import org.jspecify.annotations.Nullable;

/**
 * Checks annotation positioning, and orphaned Javadocs.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@BugPattern(
    summary = "Annotations should be positioned after Javadocs, but before modifiers.",
    severity = WARNING,
    // TODO(b/218854220): Put a tag back once patcher is fixed.
    linkType = CUSTOM,
    link = "https://google.github.io/styleguide/javaguide.html#s4.8.5-annotations")
public final class AnnotationPosition extends BugChecker
    implements ClassTreeMatcher, MethodTreeMatcher, VariableTreeMatcher {

  private static final ImmutableMap<String, TokenKind> TOKEN_KIND_BY_NAME =
      Arrays.stream(TokenKind.values()).collect(toImmutableMap(tk -> tk.name(), tk -> tk));

  private static final ImmutableSet<TokenKind> MODIFIERS =
      Streams.concat(
              Arrays.stream(Modifier.values())
                  .map(m -> TOKEN_KIND_BY_NAME.get(m.name()))
                  // TODO(b/168625474): sealed doesn't have a token kind in Java 15
                  .filter(Objects::nonNull),
              // Pretend that "<" and ">" are modifiers, so that type arguments wind up grouped with
              // modifiers.
              Stream.of(TokenKind.LT, TokenKind.GT, TokenKind.GTGT))
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
    return handle(tree, tree.getName(), tree.getModifiers(), state);
  }

  private Description handle(Tree tree, Name name, ModifiersTree modifiers, VisitorState state) {
    List<? extends AnnotationTree> annotations = modifiers.getAnnotations();
    if (annotations.isEmpty()) {
      return NO_MATCH;
    }

    int treePos = getStartPosition(tree);
    List<ErrorProneToken> tokens = annotationTokens(tree, state, treePos);
    ErrorProneComment danglingJavadoc = findOrphanedJavadoc(name, tokens);

    ImmutableList<ErrorProneToken> modifierTokens =
        tokens.stream().filter(t -> MODIFIERS.contains(t.kind())).collect(toImmutableList());

    int firstModifierPos =
        modifierTokens.stream().findFirst().map(x -> x.pos()).orElse(Integer.MAX_VALUE);
    int lastModifierPos = Streams.findLast(modifierTokens.stream()).map(x -> x.endPos()).orElse(0);

    Description description =
        checkAnnotations(
            tree, annotations, danglingJavadoc, firstModifierPos, lastModifierPos, state);
    if (!description.equals(NO_MATCH)) {
      return description;
    }
    if (danglingJavadoc == null) {
      return NO_MATCH;
    }
    // If the tree already has Javadoc, don't suggest double-Javadoccing it. It has dangling Javadoc
    // but the suggestion will be rubbish.
    if (JavacTrees.instance(state.context).getDocCommentTree(state.getPath()) != null) {
      return NO_MATCH;
    }
    SuggestedFix.Builder builder = SuggestedFix.builder();
    String javadoc = removeJavadoc(state, danglingJavadoc, builder);

    String message = "Javadocs should appear before any modifiers or annotations.";
    return buildDescription(tree)
        .setMessage(message)
        .addFix(builder.prefixWith(tree, javadoc).build())
        .build();
  }

  /** Tokenizes as little of the {@code tree} as possible to ensure we grab all the annotations. */
  private static List<ErrorProneToken> annotationTokens(
      Tree tree, VisitorState state, int annotationEnd) {
    int endPos;
    if (tree instanceof JCMethodDecl methodTree) {
      if (methodTree.getReturnType() != null) {
        endPos = getStartPosition(methodTree.getReturnType());
      } else if (!methodTree.getParameters().isEmpty()) {
        endPos = getStartPosition(methodTree.getParameters().get(0));
        if (endPos < annotationEnd) {
          endPos = state.getEndPosition(methodTree);
        }
      } else if (methodTree.getBody() != null && !methodTree.getBody().getStatements().isEmpty()) {
        endPos = getStartPosition(methodTree.getBody().getStatements().get(0));
      } else {
        endPos = state.getEndPosition(methodTree);
      }
    } else if (tree instanceof JCVariableDecl variableTree) {
      endPos = getStartPosition(variableTree.getType());
      if (endPos == -1) {
        // handle 'var'
        endPos = state.getEndPosition(variableTree.getModifiers());
      }
    } else if (tree instanceof JCClassDecl classTree) {
      endPos =
          classTree.getMembers().isEmpty()
              ? state.getEndPosition(classTree)
              : classTree.getMembers().get(0).getStartPosition();
    } else {
      throw new AssertionError();
    }
    return state.getOffsetTokens(annotationEnd, endPos);
  }

  /** Checks that annotations are on the right side of the modifiers. */
  private Description checkAnnotations(
      Tree tree,
      List<? extends AnnotationTree> annotations,
      ErrorProneComment danglingJavadoc,
      int firstModifierPos,
      int lastModifierPos,
      VisitorState state) {
    Symbol symbol = getSymbol(tree);
    ImmutableList<AnnotationTree> shouldBeBefore =
        annotations.stream()
            .filter(
                a -> {
                  Position position = annotationPosition(tree, getAnnotationType(a, symbol, state));
                  return position == Position.BEFORE
                      || (position == Position.EITHER && getStartPosition(a) < firstModifierPos);
                })
            .collect(toImmutableList());
    ImmutableList<AnnotationTree> shouldBeAfter =
        annotations.stream()
            .filter(
                a -> {
                  Position position = annotationPosition(tree, getAnnotationType(a, symbol, state));
                  return position == Position.AFTER
                      || (position == Position.EITHER && getStartPosition(a) > firstModifierPos);
                })
            .collect(toImmutableList());

    int lastNonTypeAnnotationOrModifierPosition =
        Streams.concat(
                Stream.of(lastModifierPos),
                shouldBeBefore.stream().map(ASTHelpers::getStartPosition))
            .max(naturalOrder())
            .get();
    int firstTypeAnnotationOrModifierPosition =
        Stream.concat(
                Stream.of(firstModifierPos),
                shouldBeAfter.stream().map(ASTHelpers::getStartPosition))
            .min(naturalOrder())
            .get();
    ImmutableList<AnnotationTree> moveBefore =
        shouldBeBefore.stream()
            .filter(a -> getStartPosition(a) > firstTypeAnnotationOrModifierPosition)
            .collect(toImmutableList());
    ImmutableList<AnnotationTree> moveAfter =
        shouldBeAfter.stream()
            .filter(a -> getStartPosition(a) < lastNonTypeAnnotationOrModifierPosition)
            .collect(toImmutableList());

    if (moveBefore.isEmpty() && moveAfter.isEmpty()) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    for (AnnotationTree annotation : concat(moveBefore, moveAfter)) {
      fix.delete(annotation);
    }
    String javadoc = danglingJavadoc == null ? "" : removeJavadoc(state, danglingJavadoc, fix);
    if (lastModifierPos == 0) {
      fix.replace(
          getStartPosition(tree),
          getStartPosition(tree),
          String.format("%s%s ", javadoc, joinSource(state, concat(moveBefore, moveAfter))));
    } else {
      fix.replace(
              firstModifierPos,
              firstModifierPos,
              String.format("%s%s ", javadoc, joinSource(state, moveBefore)))
          .replace(
              lastModifierPos,
              lastModifierPos,
              String.format(" %s ", joinSource(state, moveAfter)));
    }
    Stream.Builder<String> messages = Stream.builder();
    if (!moveBefore.isEmpty()) {
      ImmutableList<String> names = annotationNames(moveBefore);
      String flattened = String.join(", ", names);
      String isAre =
          names.size() > 1 ? "are not TYPE_USE annotations" : "is not a TYPE_USE annotation";
      messages.add(
          String.format(
              "%s %s, so should appear before any modifiers and after Javadocs.",
              flattened, isAre));
    }
    if (!moveAfter.isEmpty()) {
      ImmutableList<String> names = annotationNames(moveAfter);
      String flattened = String.join(", ", names);
      String isAre = names.size() > 1 ? "are TYPE_USE annotations" : "is a TYPE_USE annotation";
      messages.add(
          String.format(
              "%s %s, so should appear after modifiers and directly before the type.",
              flattened, isAre));
    }
    return buildDescription(tree)
        .setMessage(messages.build().collect(joining(" ")))
        .addFix(fix.build())
        .build();
  }

  private static Position annotationPosition(Tree tree, AnnotationType annotationType) {
    if (tree instanceof ClassTree || annotationType == null) {
      return Position.BEFORE;
    }
    return switch (annotationType) {
      case DECLARATION -> Position.BEFORE;
      case TYPE -> Position.AFTER;
      case NONE, BOTH -> Position.EITHER;
    };
  }

  private static ImmutableList<String> annotationNames(List<AnnotationTree> annotations) {
    return annotations.stream()
        .map(ASTHelpers::getSymbol)
        .filter(Objects::nonNull)
        .map(Symbol::getSimpleName)
        .map(a -> "@" + a)
        .collect(toImmutableList());
  }

  private static String joinSource(VisitorState state, Iterable<AnnotationTree> moveBefore) {
    return stream(moveBefore).map(state::getSourceForNode).collect(joining(" "));
  }

  private static String removeJavadoc(
      VisitorState state, ErrorProneComment danglingJavadoc, SuggestedFix.Builder builder) {
    int javadocStart = danglingJavadoc.getSourcePos(0);
    int javadocEnd = javadocStart + danglingJavadoc.getText().length();
    // Capturing an extra newline helps the formatter.
    if (state.getSourceCode().charAt(javadocEnd) == '\n') {
      javadocEnd++;
    }
    builder.replace(javadocStart, javadocEnd, "");
    return danglingJavadoc.getText();
  }

  private static @Nullable ErrorProneComment findOrphanedJavadoc(
      Name name, List<ErrorProneToken> tokens) {
    for (ErrorProneToken token : tokens) {
      for (ErrorProneComment comment : token.comments()) {
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

  private enum Position {
    BEFORE,
    AFTER,
    EITHER
  }
}
