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

import static com.google.common.collect.Streams.stream;
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.symbolHasAnnotation;
import static com.google.errorprone.matchers.Matchers.toType;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getReturnType;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

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
import com.google.errorprone.util.ASTHelpers;
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
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Position;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

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
      String symbolName;
      switch (tree.getKind()) {
        case NEW_CLASS:
          symbolName = getSymbol(((NewClassTree) tree).getIdentifier()).getSimpleName().toString();
          break;
        case METHOD_INVOCATION:
          symbolName = getReturnType(tree).asElement().getSimpleName().toString();
          break;
        default:
          throw new AssertionError(tree.getKind());
      }
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
    new TreePathScanner<Void, Void>() {
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
        return super.visitMethodInvocation(tree, unused);
      }

      @Override
      public Void visitNewClass(NewClassTree tree, Void unused) {
        visitNewClassOrMethodInvocation(tree);
        return super.visitNewClass(tree, unused);
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
  private final Optional<Change> matchNewClassOrMethodInvocation(
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
      switch (path.getLeaf().getKind()) {
        case RETURN:
          if (callerMethodTree != null) {
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
          // If enclosingMethod returned null, we must be returning from a statement lambda.
          return handleTailPositionInLambda(state);
        case LAMBDA_EXPRESSION:
          // The method invocation is the body of an expression lambda.
          return handleTailPositionInLambda(state);
        case CONDITIONAL_EXPRESSION:
          ConditionalExpressionTree conditionalExpressionTree =
              (ConditionalExpressionTree) path.getLeaf();
          if (conditionalExpressionTree.getTrueExpression().equals(prev.getLeaf())
              || conditionalExpressionTree.getFalseExpression().equals(prev.getLeaf())) {
            continue OUTER;
          }
          break;
        case MEMBER_SELECT:
          MemberSelectTree memberSelectTree = (MemberSelectTree) path.getLeaf();
          if (memberSelectTree.getExpression().equals(prev.getLeaf())) {
            Type type = getType(memberSelectTree);
            Symbol sym = getSymbol(memberSelectTree);
            Type streamType = state.getTypeFromString(Stream.class.getName());
            if (isSubtype(sym.enclClass().asType(), streamType, state)
                && isSameType(type.getReturnType(), streamType, state)) {
              // skip enclosing method invocation
              path = path.getParentPath();
              continue OUTER;
            }
          }
          break;
        case VARIABLE:
          Symbol sym = getSymbol(path.getLeaf());
          if (sym instanceof VarSymbol) {
            VarSymbol var = (VarSymbol) sym;
            if (var.getKind() == ElementKind.RESOURCE_VARIABLE
                || isClosedInFinallyClause(var, path, state)
                || variableInitializationCountsAsClosing(var)) {
              return Optional.empty();
            }
          }
          break;
        case ASSIGNMENT:
          // We shouldn't suggest a try/finally fix when we know the variable is going to be saved
          // for later.
          return findingWithNoFix();
        default:
          break;
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
    LambdaExpressionTree lambda =
        ASTHelpers.findEnclosingNode(state.getPath(), LambdaExpressionTree.class);
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

  private static boolean variableInitializationCountsAsClosing(VarSymbol var) {
    // static final fields don't need to be closed, because they never leave scope
    return (var.isStatic() || var.owner.isEnum()) && var.getModifiers().contains(Modifier.FINAL);
  }

  // We allow calling @MBC methods anywhere inside of a static initializer. This is a compromise:
  // in principle a static final variable might contain a method that creates @MBC objects on
  // demand, and we'd prefer to mark those as illegal. But the false positive rate is high, and it's
  // hard to cover every case. For example, stuff like
  // static final List<Pattern> PATS = STRS.stream().map(s -> Pattern.compile(s)).collect(toList());
  // is fine, but is hard to detect in a general way.
  private static boolean isInStaticInitializer(VisitorState state) {
    return stream(state.getPath())
        .anyMatch(
            tree ->
                (tree instanceof VariableTree
                        && variableInitializationCountsAsClosing((VarSymbol) getSymbol(tree)))
                    || (tree instanceof AssignmentTree
                        && getSymbol(((AssignmentTree) tree).getVariable()) instanceof VarSymbol
                        && variableInitializationCountsAsClosing(
                            (VarSymbol) getSymbol(((AssignmentTree) tree).getVariable()))));
  }

  /**
   * Returns the enclosing method of the given visitor state. Returns null if the state is within a
   * lambda expression or anonymous class.
   */
  @Nullable
  private static MethodTree enclosingMethod(VisitorState state) {
    for (Tree node : state.getPath().getParentPath()) {
      switch (node.getKind()) {
        case LAMBDA_EXPRESSION:
        case NEW_CLASS:
          return null;
        case METHOD:
          return (MethodTree) node;
        default:
          break;
      }
    }
    return null;
  }

  private static boolean isClosedInFinallyClause(VarSymbol var, TreePath path, VisitorState state) {
    if (!isConsideredFinal(var)) {
      return false;
    }
    Tree parent = path.getParentPath().getLeaf();
    if (parent.getKind() != Tree.Kind.BLOCK) {
      return false;
    }
    BlockTree block = (BlockTree) parent;
    int idx = block.getStatements().indexOf(path.getLeaf());
    if (idx == -1 || idx == block.getStatements().size() - 1) {
      return false;
    }
    StatementTree next = block.getStatements().get(idx + 1);
    if (!(next instanceof TryTree)) {
      return false;
    }
    TryTree tryTree = (TryTree) next;
    if (tryTree.getFinallyBlock() == null) {
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
    TreePath path = state.getPath();
    Tree parent = path.getParentPath().getLeaf();
    if (parent instanceof VariableTree) {
      return wrapTryFinallyAroundVariableScope((VariableTree) parent, state);
    }
    StatementTree stmt = state.findEnclosing(StatementTree.class);
    if (stmt == null) {
      return Optional.empty();
    }
    if (!(stmt instanceof VariableTree)) {
      return introduceSingleStatementTry(tree, stmt, state, suggester);
    }
    VarSymbol varSym = getSymbol((VariableTree) stmt);
    if (varSym.getKind() == ElementKind.RESOURCE_VARIABLE) {
      return extractToResourceInCurrentTry(tree, stmt, state, suggester);
    }
    return splitVariableDeclarationAroundTry(tree, (VariableTree) stmt, state, suggester);
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
    int afterTypePos = state.getEndPosition(var.getType());
    String name = suggester.suggestName(tree);
    return Change.builder(
            SuggestedFix.builder()
                .replace(
                    afterTypePos,
                    initPos,
                    String.format(
                        " %s;\ntry (var %s = %s) {\n%s =",
                        var.getName(), name, state.getSourceForNode(tree), var.getName()))
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
}
