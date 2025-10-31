/*
 * Copyright 2017 The Error Prone Authors.
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

import static com.google.errorprone.bugpatterns.CloseableDecoratorTypes.CLOSEABLE_DECORATOR_TYPES;
import static com.google.errorprone.fixes.SuggestedFixes.prettyType;
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.symbolHasAnnotation;
import static com.google.errorprone.matchers.Matchers.toType;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.enclosingClass;
import static com.google.errorprone.util.ASTHelpers.findEnclosingNode;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getReturnType;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.hasImplicitType;
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;
import static com.google.errorprone.util.ASTHelpers.isInStaticInitializer;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static com.google.errorprone.util.ASTHelpers.variableIsStaticFinal;

import com.google.auto.value.AutoValue;
import com.google.common.base.CaseFormat;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.ForOverride;
import com.google.errorprone.annotations.MustBeClosed;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.UnusedReturnValueMatcher;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Position;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.lang.model.element.ElementKind;
import org.jspecify.annotations.Nullable;

/**
 * An abstract check for resources that must be closed; used by {@link StreamResourceLeak} and
 * {@link MustBeClosedChecker}.
 */
public abstract class AbstractMustBeClosedChecker extends BugChecker {

  private static final String MUST_BE_CLOSED_ANNOTATION_NAME =
      MustBeClosed.class.getCanonicalName();

  /** Matches trees annotated with {@link MustBeClosed}. */
  protected static final Matcher<Tree> HAS_MUST_BE_CLOSED_ANNOTATION =
      symbolHasAnnotation(MUST_BE_CLOSED_ANNOTATION_NAME);

  private static final Matcher<ExpressionTree> CLOSE_METHOD =
      instanceMethod().onDescendantOf("java.lang.AutoCloseable").named("close");

  private static final Matcher<Tree> MOCKITO_MATCHER =
      toType(
          MethodInvocationTree.class, staticMethod().onClass("org.mockito.Mockito").named("when"));

  static final class NameSuggester {
    private final Multiset<String> assignedNamesInThisMethod = HashMultiset.create();

    /** Returns basename if there are no conflicts, then basename + "2", then basename + "3"... */
    String uniquifyName(String basename) {
      int numPreviousConflicts = assignedNamesInThisMethod.add(basename, 1);
      if (numPreviousConflicts == 0) {
        // First time using this name.
        return basename;
      }
      // If we already have `var foo`, then `var foo2` seems like a good next choice.
      return basename + (numPreviousConflicts + 1);
    }

    /**
     * @param tree must be either MethodInvocationTree or NewClassTree
     */
    String suggestName(ExpressionTree tree) {
      String symbolName =
          switch (tree) {
            case NewClassTree nct -> getSymbol(nct.getIdentifier()).getSimpleName().toString();
            case MethodInvocationTree mit ->
                getReturnType(tree).asElement().getSimpleName().toString();
            default -> throw new AssertionError(tree.getKind());
          };
      return uniquifyName(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, symbolName));
    }
  }

  /**
   * Scans a method body for invocations matching {@code matcher}, emitting them as a single fix.
   */
  protected Description scanEntireMethodFor(
      Matcher<? super ExpressionTree> matcher, MethodTree tree, VisitorState state) {
    BlockTree body = tree.getBody();
    if (body == null) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
    NameSuggester suggester = new NameSuggester();
    Multiset<Tree> closeBraceLocations = HashMultiset.create();
    Tree[] firstIssuedFixLocation = new Tree[1];
    new SuppressibleTreePathScanner<Void, Void>(state) {
      @Override
      public Void visitMethod(MethodTree methodTree, Void aVoid) {
        // Don't descend into sub-methods - we will scanEntireMethod on each later
        return null;
      }

      private void visitNewClassOrMethodInvocation(ExpressionTree tree) {
        VisitorState localState = state.withPath(getCurrentPath());
        if (matcher.matches(tree, localState)) {
          matchNewClassOrMethodInvocation(tree, localState, suggester)
              .ifPresent(
                  change -> {
                    fixBuilder.merge(change.otherFixes());
                    change.closeBraceAfter().ifPresent(closeBraceLocations::add);
                    if (firstIssuedFixLocation[0] == null) {
                      // Attach the finding to the first invocation that broke the rules.
                      firstIssuedFixLocation[0] = tree;
                    }
                  });
        }
      }

      @Override
      public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
        visitNewClassOrMethodInvocation(tree);
        return super.visitMethodInvocation(tree, null);
      }

      @Override
      public Void visitNewClass(NewClassTree tree, Void unused) {
        visitNewClassOrMethodInvocation(tree);
        return super.visitNewClass(tree, null);
      }
    }.scan(new TreePath(state.getPath(), body), null);

    if (firstIssuedFixLocation[0] == null) {
      // No findings fired, even with empty fixes
      return NO_MATCH;
    }
    closeBraceLocations.forEachEntry((t, count) -> fixBuilder.postfixWith(t, "}".repeat(count)));
    return describeMatch(firstIssuedFixLocation[0], fixBuilder.build());
  }

  /**
   * Error Prone's fix application logic doesn't like it when a fix suggests multiple identical
   * insertions at the same position. This Change class breaks up a SuggestedFix into two parts: a
   * position at which to insert a close brace, and a SuggestedFix of other related changes. We use
   * this to aggregate all the suggested changes within a method, so that we can handle adding
   * multiple try blocks with the same scope. Instead of emitting N fixes that each add a new
   * close-brace at the end of that scope, we emit a single fix that adds N close braces.
   */
  @AutoValue
  protected abstract static class Change {
    abstract SuggestedFix otherFixes();

    abstract Optional<Tree> closeBraceAfter();

    @AutoValue.Builder
    abstract static class Builder {
      abstract Builder otherFixes(SuggestedFix value);

      abstract Builder closeBraceAfter(Tree value);

      abstract Change build();

      /**
       * A shortcut for {@code Optional.of(build())}. Many operations taking a Change expect an
       * {@code Optional<Change>}, and if you have a Builder already, you're clearly not planning to
       * return empty(), so this is a convenient way to make the Optional implicit.
       */
      Optional<Change> wrapped() {
        return Optional.of(build());
      }
    }

    static Builder builder(SuggestedFix otherFixes) {
      return new AutoValue_AbstractMustBeClosedChecker_Change.Builder().otherFixes(otherFixes);
    }

    static Optional<Change> of(SuggestedFix fix) {
      return builder(fix).wrapped();
    }
  }

  /**
   * Check that the expression {@code tree} occurs within the resource variable initializer of a
   * try-with-resources statement.
   */
  private Optional<Change> matchNewClassOrMethodInvocation(
      ExpressionTree tree, VisitorState state, NameSuggester suggester) {
    if (isInStaticInitializer(state)) {
      return Optional.empty();
    }
    return checkClosed(tree, state, suggester)
        .filter(
            unusedFix ->
                !(UnusedReturnValueMatcher.expectedExceptionTest(state)
                    || UnusedReturnValueMatcher.mockitoInvocation(tree, state)
                    || MOCKITO_MATCHER.matches(state.getPath().getParentPath().getLeaf(), state)
                    || exemptChange(tree, state)));
  }

  @ForOverride
  protected boolean exemptChange(ExpressionTree tree, VisitorState state) {
    return false;
  }

  private Optional<Change> checkClosed(
      ExpressionTree tree, VisitorState state, NameSuggester suggester) {
    MethodTree callerMethodTree = enclosingMethod(state);
    TreePath path = state.getPath();
    OUTER:
    while (true) {
      TreePath prev = path;
      path = path.getParentPath();
      switch (path.getLeaf()) {
        case ReturnTree returnTree -> {
          if (callerMethodTree == null) {
            // If enclosingMethod returned null, we must be returning from a statement lambda.
            return handleTailPositionInLambda(state);
          }
          // The invocation occurs within a return statement of a method, instead of a lambda
          // expression or anonymous class.
          if (HAS_MUST_BE_CLOSED_ANNOTATION.matches(callerMethodTree, state)) {
            // Ignore invocations of annotated methods and constructors that occur in the return
            // statement of an annotated caller method, since invocations of the caller are
            // enforced.
            return Optional.empty();
          }
          // The caller method is not annotated, so the closing of the returned resource is not
          // enforced. Suggest fixing this by annotating the caller method.
          return Change.of(
              SuggestedFix.builder()
                  .prefixWith(callerMethodTree, "@MustBeClosed\n")
                  .addImport(MustBeClosed.class.getCanonicalName())
                  .build());
        }
        case LambdaExpressionTree lambdaExpressionTree -> {
          // The method invocation is the body of an expression lambda.
          return handleTailPositionInLambda(state);
        }
        case ConditionalExpressionTree conditionalExpressionTree -> {
          if (conditionalExpressionTree.getTrueExpression().equals(prev.getLeaf())
              || conditionalExpressionTree.getFalseExpression().equals(prev.getLeaf())) {
            continue OUTER;
          }
        }
        case MemberSelectTree memberSelectTree -> {
          if (memberSelectTree.getExpression().equals(prev.getLeaf())) {
            Type streamType = state.getTypeFromString(Stream.class.getName());
            Type classType = enclosingClass(getSymbol(memberSelectTree)).asType();
            Type returnType = getReturnType(memberSelectTree);
            if (isSubtype(classType, streamType, state)
                && isSameType(returnType, streamType, state)) {
              // skip enclosing method invocation
              path = path.getParentPath();
              continue OUTER;
            }
          }
        }
        case NewClassTree newClassTree -> {
          if (isClosingDecorator(newClassTree, prev.getLeaf(), state)) {
            if (HAS_MUST_BE_CLOSED_ANNOTATION.matches(newClassTree, state)) {
              // if the decorator is also annotated then it would already be enforced
              return Optional.empty();
            }
            // otherwise, enforce that the decorator must be closed
            continue OUTER;
          }
        }
        case VariableTree variableTree -> {
          var variable = getSymbol(variableTree);
          if (variable.getKind() == ElementKind.RESOURCE_VARIABLE
              || isClosedInFinallyClause(variable, path, state)
              || variableIsStaticFinal(variable)) {
            return Optional.empty();
          }
        }
        case AssignmentTree assignmentTree -> {
          // We shouldn't suggest a try/finally fix when we know the variable is going to be saved
          // for later.
          return findingWithNoFix();
        }
        default -> {}
      }
      // The constructor or method invocation does not occur within the resource variable
      // initializer of a try-with-resources statement.
      return fix(tree, state, suggester);
    }
  }

  protected Optional<Change> fix(ExpressionTree tree, VisitorState state, NameSuggester suggester) {
    return chooseFixType(tree, state, suggester);
  }

  private static Optional<Change> handleTailPositionInLambda(VisitorState state) {
    LambdaExpressionTree lambda = findEnclosingNode(state.getPath(), LambdaExpressionTree.class);
    if (lambda == null) {
      // Apparently we're not inside a lambda?!
      return findingWithNoFix();
    }
    if (hasAnnotation(
        state.getTypes().findDescriptorSymbol(getType(lambda).tsym),
        MUST_BE_CLOSED_ANNOTATION_NAME,
        state)) {
      return Optional.empty();
    }

    return findingWithNoFix();
  }

  private static Optional<Change> findingWithNoFix() {
    return Change.of(SuggestedFix.emptyFix());
  }

  /**
   * Returns the enclosing method of the given visitor state. Returns null if the state is within a
   * lambda expression or anonymous class.
   */
  private static @Nullable MethodTree enclosingMethod(VisitorState state) {
    for (Tree node : state.getPath().getParentPath()) {
      switch (node) {
        case LambdaExpressionTree let -> {
          return null;
        }
        case NewClassTree nct -> {
          return null;
        }
        case MethodTree methodTree -> {
          return methodTree;
        }
        default -> {}
      }
    }
    return null;
  }

  private static boolean isClosedInFinallyClause(VarSymbol var, TreePath path, VisitorState state) {
    if (!isConsideredFinal(var)) {
      return false;
    }
    if (!(path.getParentPath().getLeaf() instanceof BlockTree block)) {
      return false;
    }
    int idx = block.getStatements().indexOf(path.getLeaf());
    if (idx == -1 || idx == block.getStatements().size() - 1) {
      return false;
    }
    StatementTree next = block.getStatements().get(idx + 1);
    if (!(next instanceof TryTree tryTree) || tryTree.getFinallyBlock() == null) {
      return false;
    }
    boolean[] closed = {false};
    tryTree
        .getFinallyBlock()
        .accept(
            new TreeScanner<Void, Void>() {
              @Override
              public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
                if (CLOSE_METHOD.matches(tree, state)
                    && Objects.equals(getSymbol(getReceiver(tree)), var)) {
                  closed[0] = true;
                }
                return null;
              }
            },
            null);
    return closed[0];
  }

  private static Optional<Change> chooseFixType(
      ExpressionTree tree, VisitorState state, NameSuggester suggester) {
    if (state.getPath().getParentPath().getLeaf() instanceof VariableTree parent) {
      return wrapTryFinallyAroundVariableScope(parent, state);
    }
    StatementTree stmt = state.findEnclosing(StatementTree.class);
    if (stmt == null) {
      return Optional.empty();
    }
    if (!(stmt instanceof VariableTree var)) {
      return introduceSingleStatementTry(tree, stmt, state, suggester);
    }
    if (getSymbol(var).getKind() == ElementKind.RESOURCE_VARIABLE) {
      return extractToResourceInCurrentTry(tree, var, state, suggester);
    }
    return splitVariableDeclarationAroundTry(tree, var, state, suggester);
  }

  private static Optional<Change> introduceSingleStatementTry(
      ExpressionTree tree, StatementTree stmt, VisitorState state, NameSuggester suggester) {
    SuggestedFix.Builder fix = SuggestedFix.builder();
    String name = suggester.suggestName(tree);
    if (state.getPath().getParentPath().getLeaf() instanceof ExpressionStatementTree) {
      fix.delete(stmt);
    } else {
      fix.replace(tree, name);
    }
    return Change.builder(
            fix.prefixWith(
                    stmt, String.format("try (var %s = %s) {", name, state.getSourceForNode(tree)))
                .build())
        .closeBraceAfter(stmt)
        .wrapped();
  }

  private static Optional<Change> extractToResourceInCurrentTry(
      ExpressionTree tree,
      StatementTree declaringStatement,
      VisitorState state,
      NameSuggester suggester) {
    Type type = getType(tree);
    if (type == null) {
      return Optional.empty();
    }
    String name = suggester.suggestName(tree);
    SuggestedFix.Builder fix = SuggestedFix.builder();
    return Change.of(
        SuggestedFix.builder()
            .prefixWith(
                declaringStatement,
                String.format(
                    "%s %s = %s;",
                    qualifyType(state, fix, type), name, state.getSourceForNode(tree)))
            .replace(tree, name)
            .build());
  }

  private static Optional<Change> splitVariableDeclarationAroundTry(
      ExpressionTree tree, VariableTree var, VisitorState state, NameSuggester suggester) {
    int initPos = getStartPosition(var.getInitializer());
    Tree type = var.getType();
    String typePrefix;
    int startPos;
    if (!hasImplicitType(var, state)) {
      startPos = state.getEndPosition(type);
      typePrefix = "";
    } else {
      startPos = getStartPosition(var);
      typePrefix = prettyType(getType(type), state);
    }
    String name = suggester.suggestName(tree);
    return Change.builder(
            SuggestedFix.builder()
                .replace(
                    startPos,
                    initPos,
                    String.format(
                        "%s %s;\ntry (var %s = %s) {\n%s =",
                        typePrefix,
                        var.getName(),
                        name,
                        state.getSourceForNode(tree),
                        var.getName()))
                .replace(tree, name)
                .build())
        .closeBraceAfter(var)
        .wrapped();
  }

  private static Optional<Change> wrapTryFinallyAroundVariableScope(
      VariableTree decl, VisitorState state) {
    BlockTree enclosingBlock = state.findEnclosing(BlockTree.class);
    if (enclosingBlock == null) {
      return Optional.empty();
    }
    Tree declTree = decl.getType();
    String declType =
        state.getEndPosition(declTree) == Position.NOPOS ? "var" : state.getSourceForNode(declTree);

    return Change.builder(
            SuggestedFix.builder()
                .delete(decl)
                .prefixWith(
                    decl,
                    String.format(
                        "try (%s %s = %s) {",
                        declType, decl.getName(), state.getSourceForNode(decl.getInitializer())))
                .build())
        .closeBraceAfter(enclosingBlock)
        .wrapped();
  }

  /**
   * Returns true if {@code decorator} is the instantiation of an {@link AutoCloseable} decorator
   * type that decorates the given {@code resource} and always closes the decorated {@code resource}
   * when closed.
   */
  private static boolean isClosingDecorator(
      NewClassTree decorator, Tree resource, VisitorState state) {
    if (decorator.getArguments().isEmpty() || !decorator.getArguments().get(0).equals(resource)) {
      // we assume the decorated resource is always the first argument to the decorator constructor
      return false;
    }
    MethodSymbol constructor = getSymbol(decorator);
    if (!constructor.getThrownTypes().isEmpty()) {
      // resource would not be closed if the decorator constructor throws
      return false;
    }
    Type resourceType = constructor.params().get(0).type;
    Type decoratorType = constructor.owner.type;
    return CLOSEABLE_DECORATOR_TYPES.keySet().stream()
        .filter(key -> isSameType(resourceType, state.getTypeFromString(key), state))
        .limit(1)
        .flatMap(key -> CLOSEABLE_DECORATOR_TYPES.get(key).stream())
        .anyMatch(value -> isSubtype(decoratorType, state.getTypeFromString(value), state));
  }
}
