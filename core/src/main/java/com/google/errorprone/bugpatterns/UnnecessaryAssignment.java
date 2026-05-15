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

package com.google.errorprone.bugpatterns;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.findLast;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.AT_LEAST_ONE;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.JUnitMatchers.isJUnit4TestRunnerOfType;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.hasArgumentWithValue;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.symbolHasAnnotation;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static com.google.errorprone.util.ErrorProneTokens.getTokens;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.AssignmentTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.InjectMatchers;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.MultiMatcher;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.ErrorProneToken;
import com.google.errorprone.util.MoreAnnotations;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.element.Modifier;

/**
 * Discourage manual initialization or assignment to fields annotated with framework annotations.
 */
// TODO: b/447474042 - All diagnostics should customize the message to mention the specific
// annotation
@BugPattern(
    summary =
        "Fields annotated with @Inject/@Mock/@TestParameter should not be manually assigned to, as"
            + " they should be initialized by a framework. Remove the assignment if a framework is"
            + " being used, or the annotation if one isn't.",
    severity = WARNING)
public final class UnnecessaryAssignment extends BugChecker
    implements AssignmentTreeMatcher, VariableTreeMatcher {

  private static final ImmutableSet<String> FRAMEWORK_ANNOTATIONS =
      ImmutableSet.<String>builder()
          .addAll(InjectMatchers.INJECT_ANNOTATIONS)
          .add("com.google.testing.junit.testparameterinjector.TestParameter")
          .build();

  private static final String MOCK_ANNOTATION = "org.mockito.Mock";
  private static final Matcher<Tree> HAS_MOCK_ANNOTATION = symbolHasAnnotation(MOCK_ANNOTATION);

  private static final Matcher<Tree> HAS_NON_MOCK_FRAMEWORK_ANNOTATION =
      allOf(
          anyOf(
              FRAMEWORK_ANNOTATIONS.stream()
                  .map(Matchers::symbolHasAnnotation)
                  .collect(toImmutableList())),
          not(UnnecessaryAssignment::isOptionalInject));

  /** Returns true if the tree is annotated with @com.google.inject.Inject(optional = true). */
  private static boolean isOptionalInject(Tree tree, VisitorState state) {
    var symbol = getSymbol(tree);
    var compound = symbol.attribute(GUICE_INJECT.get(state));
    if (compound == null) {
      return false;
    }
    return MoreAnnotations.getValue(compound, "optional")
        .map(a -> Objects.equals(a.getValue(), true))
        .orElse(false);
  }

  private static final Supplier<Symbol> GUICE_INJECT =
      VisitorState.memoize(
          state -> state.getSymbolFromString(InjectMatchers.GUICE_INJECT_ANNOTATION));

  private static final Matcher<ExpressionTree> MOCK_FACTORY =
      staticMethod().onClass("org.mockito.Mockito").named("mock");

  private static final Matcher<ExpressionTree> INITIALIZES_MOCKS =
      anyOf(staticMethod().onClass("org.mockito.MockitoAnnotations").named("initMocks"));

  private static final MultiMatcher<ClassTree, AnnotationTree> MOCKITO_RUNNER =
      annotations(
          AT_LEAST_ONE,
          hasArgumentWithValue(
              "value",
              isJUnit4TestRunnerOfType(ImmutableList.of("org.mockito.junit.MockitoJUnitRunner"))));

  private static boolean hasAnnotation(Tree tree, String annotation, VisitorState state) {
    Symbol sym = getSymbol(tree);
    return sym != null && ASTHelpers.hasAnnotation(sym, annotation, state);
  }

  private static Optional<String> hasAnnotation(
      Tree tree, Collection<String> annotations, VisitorState state) {
    return annotations.stream().filter(a -> hasAnnotation(tree, a, state)).findFirst();
  }

  @Override
  public Description matchAssignment(AssignmentTree tree, VisitorState state) {
    Tree variable = tree.getVariable();
    Optional<String> annotation = Optional.empty();
    if (hasAnnotation(variable, MOCK_ANNOTATION, state)) {
      annotation = Optional.of(MOCK_ANNOTATION);
    } else {
      annotation =
          hasAnnotation(variable, FRAMEWORK_ANNOTATIONS, state)
              .filter(a -> !isOptionalInject(variable, state));
    }

    if (annotation.isEmpty()) {
      return NO_MATCH;
    }

    SuggestedFix fix =
        state.getPath().getParentPath().getLeaf() instanceof ExpressionStatementTree est
            ? SuggestedFix.delete(est)
            : SuggestedFix.emptyFix();

    return buildDescription(tree).addFix(fix).setMessage(buildMessage(annotation.get())).build();
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    boolean hasMockAnnotation = HAS_MOCK_ANNOTATION.matches(tree, state);
    boolean hasInjectyAnnotation = HAS_NON_MOCK_FRAMEWORK_ANNOTATION.matches(tree, state);
    if (hasMockAnnotation && hasInjectyAnnotation) {
      return buildDescription(tree)
          .setMessage(
              "Fields shouldn't be annotated with both @Mock and another @Inject-like annotation,"
                  + " because both Mockito and the injector will assign to the field, and one of"
                  + " the values will overwrite the other")
          .build();
    }
    if (tree.getInitializer() == null) {
      return NO_MATCH;
    }
    if (hasMockAnnotation) {
      return buildDescription(tree)
          .addFix(createMockFix(tree, state))
          .setMessage(buildMessage(MOCK_ANNOTATION))
          .build();
    }
    if (hasInjectyAnnotation) {
      Description.Builder description = buildDescription(tree);
      if (!tree.getModifiers().getFlags().contains(Modifier.FINAL)) {
        String source =
            state
                .getSourceCode()
                .subSequence(getStartPosition(tree), getStartPosition(tree.getInitializer()))
                .toString();
        ImmutableList<ErrorProneToken> tokens =
            getTokens(source, getStartPosition(tree), state.context);
        int equalsPos =
            findLast(tokens.stream().filter(t -> t.kind().equals(TokenKind.EQ))).get().pos();
        description.addFix(
            SuggestedFix.builder()
                .setShortDescription("Remove the variable's initializer")
                .replace(equalsPos, state.getEndPosition(tree.getInitializer()), "")
                .build());
      }
      AnnotationTree annotationToRemove =
          tree.getModifiers().getAnnotations().stream()
              .filter(
                  anno ->
                      FRAMEWORK_ANNOTATIONS.stream()
                          .anyMatch(
                              fanno ->
                                  isSubtype(getType(anno), state.getTypeFromString(fanno), state)))
              .findFirst()
              .get();
      return description
          .setMessage(
              String.format(
                  "Fields annotated with @%s should not be manually assigned to, as they should be"
                      + " initialized by a framework. Remove the assignment if a framework is"
                      + " being used, or the annotation if one isn't.",
                  getType(annotationToRemove).tsym.getSimpleName()))
          .addFix(
              SuggestedFix.builder()
                  .setShortDescription("Remove the annotation")
                  .delete(annotationToRemove)
                  .build())
          .build();
    }
    return NO_MATCH;
  }

  private static SuggestedFix createMockFix(VariableTree tree, VisitorState state) {
    if (MOCK_FACTORY.matches(tree.getInitializer(), state)
        && !classContainsInitializer(state.findEnclosing(ClassTree.class), state)) {
      AnnotationTree anno =
          ASTHelpers.getAnnotationWithSimpleName(tree.getModifiers().getAnnotations(), "Mock");
      return SuggestedFix.delete(anno);
    }
    int startPos = getStartPosition(tree);
    ImmutableList<ErrorProneToken> tokens =
        state.getOffsetTokens(startPos, getStartPosition(tree.getInitializer()));
    for (ErrorProneToken token : Lists.reverse(tokens)) {
      if (token.kind() == TokenKind.EQ) {
        return SuggestedFix.replace(token.pos(), state.getEndPosition(tree.getInitializer()), "");
      }
    }
    return SuggestedFix.emptyFix();
  }

  private static boolean classContainsInitializer(ClassTree classTree, VisitorState state) {
    AtomicBoolean initialized = new AtomicBoolean(false);
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitClass(ClassTree classTree, Void unused) {
        if (MOCKITO_RUNNER.matches(classTree, state)) {
          initialized.set(true);
          return null;
        }
        return super.visitClass(classTree, null);
      }

      @Override
      public Void visitMethodInvocation(MethodInvocationTree methodInvocationTree, Void unused) {
        if (INITIALIZES_MOCKS.matches(methodInvocationTree, state)) {
          initialized.set(true);
          return null;
        }
        return super.visitMethodInvocation(methodInvocationTree, null);
      }

      @Override
      public Void visitNewClass(NewClassTree newClassTree, Void unused) {
        if (INITIALIZES_MOCKS.matches(newClassTree, state)) {
          initialized.set(true);
          return null;
        }
        return super.visitNewClass(newClassTree, null);
      }
    }.scan(classTree, null);
    return initialized.get();
  }

  private static String buildMessage(String annotationUsed) {
    String simpleName = annotationUsed.substring(annotationUsed.lastIndexOf('.') + 1);
    return String.format(
        "Fields annotated with @%s should not be manually assigned to, as they should be"
            + " initialized by a framework. Remove the assignment if a framework is being"
            + " used, or the annotation if one isn't.",
        simpleName);
  }
}
