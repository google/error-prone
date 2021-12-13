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
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.common.base.CaseFormat;
import com.google.common.base.Strings;
import com.google.common.base.Verify;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Streams;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.MustBeClosed;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
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
import java.util.Collection;
import java.util.Map;
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

  /** Matches trees annotated with {@link MustBeClosed}. */
  protected static final Matcher<Tree> HAS_MUST_BE_CLOSED_ANNOTATION =
      symbolHasAnnotation(MustBeClosed.class.getCanonicalName());

  private static final Matcher<ExpressionTree> CLOSE_METHOD =
      instanceMethod().onDescendantOf("java.lang.AutoCloseable").named("close");

  private static final Matcher<Tree> MOCKITO_MATCHER =
      toType(
          MethodInvocationTree.class, staticMethod().onClass("org.mockito.Mockito").named("when"));

  /** Scans a method body for invocations matching {@code m}, and emitting them as a single fix. */
  protected Description scanEntireMethodFor(
      Matcher<? super MethodInvocationTree> m, MethodTree tree, VisitorState state) {
    FixAggregator aggregator = findingPerMethod();
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitMethod(MethodTree methodTree, Void aVoid) {
        // Don't descend into sub-methods - we will scanEntireMethod on each later
        return null;
      }

      @Override
      public Void visitMethodInvocation(MethodInvocationTree methodInvocationTree, Void aVoid) {
        VisitorState localState = state.withPath(getCurrentPath());
        if (m.matches(methodInvocationTree, localState)) {
          Description description =
              matchNewClassOrMethodInvocation(methodInvocationTree, localState, aggregator);
          // This shouldn't return fixes - aggregator is per-method, so it should just save
          // up some potential fixes for us to combine later.
          Verify.verify(description.fixes.isEmpty());
        }
        return super.visitMethodInvocation(methodInvocationTree, aVoid);
      }
    }.scan(tree.getBody(), null);
    return aggregator.flush().map(fix -> describeMatch(tree, fix)).orElse(NO_MATCH);
  }

  /**
   * Check that the expression {@code tree} occurs within the resource variable initializer of a
   * try-with-resources statement.
   */
  protected Description matchNewClassOrMethodInvocation(
      ExpressionTree tree, VisitorState state, FixAggregator aggregator) {
    if (isInStaticInitializer(state)) {
      return NO_MATCH;
    }
    Description description = checkClosed(tree, state, aggregator);
    if (description == NO_MATCH) {
      return NO_MATCH;
    }
    if (AbstractReturnValueIgnored.expectedExceptionTest(tree, state)
        || AbstractReturnValueIgnored.mockitoInvocation(tree, state)
        || MOCKITO_MATCHER.matches(state.getPath().getParentPath().getLeaf(), state)) {
      return NO_MATCH;
    }
    return description;
  }

  private Description checkClosed(
      ExpressionTree tree, VisitorState state, FixAggregator aggregator) {
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
              return NO_MATCH;
            }
            // The caller method is not annotated, so the closing of the returned resource is not
            // enforced. Suggest fixing this by annotating the caller method.
            return describeMatch(
                tree,
                SuggestedFix.builder()
                    .prefixWith(callerMethodTree, "@MustBeClosed\n")
                    .addImport(MustBeClosed.class.getCanonicalName())
                    .build());
          }
          // In a lambda that returns a MBC variable, there's no place to suggest annotating the
          // method, and suggesting a try/finally is inane. Instead, just issue a fixless finding.
          return emptyFix(tree);
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
              return NO_MATCH;
            }
          }
          break;
        case ASSIGNMENT:
          // We shouldn't suggest a try/finally fix when we know the variable is going to be saved
          // for later.
          return emptyFix(tree);
        default:
          break;
      }
      // The constructor or method invocation does not occur within the resource variable
      // initializer of a try-with-resources statement.
      Description.Builder description = buildDescription(tree);
      addFix(description, tree, state, aggregator);
      return description.build();
    }
  }

  private Description emptyFix(Tree tree) {
    return describeMatch(tree);
  }

  private static boolean variableInitializationCountsAsClosing(VarSymbol var) {
    // static final fields don't need to be closed, because they never leave scope
    return var.isStatic() && var.getModifiers().contains(Modifier.FINAL);
  }

  // We allow calling @MBC methods anywhere inside of a static initializer. This is a compromise:
  // in principle a static final variable might contain a method that creates @MBC objects on
  // demand, and we'd prefer to mark those as illegal. But the false positive rate is high, and it's
  // hard to cover every case. For example, stuff like
  // static final List<Pattern> PATS = STRS.stream().map(s -> Pattern.compile(s)).collect(toList());
  // is fine, but is hard to detect in a general way.
  private static boolean isInStaticInitializer(VisitorState state) {
    return Streams.stream(state.getPath())
        .anyMatch(
            tree ->
                tree instanceof VariableTree
                    && variableInitializationCountsAsClosing((VarSymbol) getSymbol(tree)));
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

  private Optional<TryBlock> chooseFixType(ExpressionTree tree, VisitorState state) {
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
      return introduceSingleStatementTry(tree, stmt, state);
    }
    VarSymbol varSym = getSymbol((VariableTree) stmt);
    if (varSym.getKind() == ElementKind.RESOURCE_VARIABLE) {
      return extractToResourceInCurrentTry(tree, stmt, state);
    }
    return splitVariableDeclarationAroundTry(tree, (VariableTree) stmt, state);
  }

  protected void addFix(
      Description.Builder description,
      ExpressionTree tree,
      VisitorState state,
      FixAggregator aggregator) {
    chooseFixType(tree, state).flatMap(aggregator::report).ifPresent(description::addFix);
  }

  private Optional<TryBlock> introduceSingleStatementTry(
      ExpressionTree tree, StatementTree stmt, VisitorState state) {
    Type type = getType(tree);
    if (type == null) {
      return Optional.empty();
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    String name = suggestName(tree);
    if (state.getPath().getParentPath().getLeaf() instanceof ExpressionStatementTree) {
      fix.delete(stmt);
    } else {
      fix.replace(tree, name);
    }
    return Optional.of(
        new TryBlock(
            stmt,
            fix.prefixWith(
                stmt,
                String.format(
                    "try (%s %s = %s) {",
                    qualifyType(state, fix, type), name, state.getSourceForNode(tree)))));
  }

  private Optional<TryBlock> extractToResourceInCurrentTry(
      ExpressionTree tree, StatementTree declaringStatement, VisitorState state) {
    Type type = getType(tree);
    if (type == null) {
      return Optional.empty();
    }
    String name = suggestName(tree);
    SuggestedFix.Builder fix = SuggestedFix.builder();
    return Optional.of(
        new TryBlock(
            fix.prefixWith(
                    declaringStatement,
                    String.format(
                        "%s %s = %s;",
                        qualifyType(state, fix, type), name, state.getSourceForNode(tree)))
                .replace(tree, name)));
  }

  private Optional<TryBlock> splitVariableDeclarationAroundTry(
      ExpressionTree tree, VariableTree var, VisitorState state) {
    Type type = getType(tree);
    if (type == null) {
      return Optional.empty();
    }
    int initPos = getStartPosition(var.getInitializer());
    int afterTypePos = state.getEndPosition(var.getType());
    String name = suggestName(tree);
    SuggestedFix.Builder fix = SuggestedFix.builder();
    return Optional.of(
        new TryBlock(
            var,
            fix.replace(
                    afterTypePos,
                    initPos,
                    String.format(
                        " %s;\ntry (%s %s = %s) {\n%s =",
                        var.getName(),
                        qualifyType(state, fix, type),
                        name,
                        state.getSourceForNode(tree),
                        var.getName()))
                .replace(tree, name)));
  }

  private Optional<TryBlock> wrapTryFinallyAroundVariableScope(
      VariableTree decl, VisitorState state) {
    BlockTree enclosingBlock = state.findEnclosing(BlockTree.class);
    if (enclosingBlock == null) {
      return Optional.empty();
    }
    return Optional.of(
        new TryBlock(
            enclosingBlock,
            SuggestedFix.builder()
                .prefixWith(
                    decl,
                    String.format(
                        "try (%s %s = %s) {",
                        state.getSourceForNode(decl.getType()),
                        decl.getName().toString(),
                        state.getSourceForNode(decl.getInitializer())))
                .delete(decl)));
  }

  // Will be either MethodInvocationTree or NewClassTree
  private String suggestName(ExpressionTree tree) {
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
    return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, symbolName);
  }

  /**
   * Allows us to aggregate multiple changes into a single SuggestedFix. The fix-applying machinery
   * assumes that inserting the same text at the same location multiple times is a mistake, so if we
   * create several try blocks that end at the same line, the insertions of } will conflict. This
   * class separates out the close-brace location from the other fixes, so that the close-braces can
   * all be inserted at once atomically.
   */
  private static class TryBlock {
    final Optional<Tree> closeBraceAfter;
    final SuggestedFix.Builder otherChanges;

    /** For changes that don't need to insert a close brace. */
    TryBlock(SuggestedFix.Builder changes) {
      this.closeBraceAfter = Optional.empty();
      this.otherChanges = changes;
    }

    TryBlock(Tree closeBraceAfter, SuggestedFix.Builder otherChanges) {
      this.closeBraceAfter = Optional.of(closeBraceAfter);
      this.otherChanges = otherChanges;
    }
  }

  /** A strategy for handling and potentially combining multiple fixes. */
  protected interface FixAggregator {
    /**
     * Attempt to report a fix. A non-empty result should be reported as if by {@link
     * VisitorState#reportMatch(Description)}. An empty result implies that the fix is being saved
     * up to be later emitted by {@link #flush()}.
     */
    Optional<SuggestedFix> report(TryBlock fix);

    /**
     * Returns a single fix containing all the changes saved up by earlier calls to {@link
     * #report(TryBlock)}
     */
    Optional<SuggestedFix> flush();
  }

  private static final class FindingPerMethod implements FixAggregator {
    private FindingPerMethod() {}

    private final Multimap<Optional<Tree>, TryBlock> reports = ArrayListMultimap.create();

    @Override
    public Optional<SuggestedFix> report(TryBlock fix) {
      // Overlapping close brace is the only thing we need to coalesce by
      reports.put(fix.closeBraceAfter, fix);
      return Optional.empty();
    }

    @Override
    public Optional<SuggestedFix> flush() {
      if (reports.isEmpty()) {
        return Optional.empty();
      }
      SuggestedFix.Builder fix = SuggestedFix.builder();
      for (Map.Entry<Optional<Tree>, Collection<TryBlock>> e : reports.asMap().entrySet()) {
        Optional<Tree> block = e.getKey();
        Collection<TryBlock> changes = e.getValue();

        block.ifPresent(b -> fix.postfixWith(b, Strings.repeat("}", changes.size())));
        for (TryBlock change : changes) {
          fix.merge(change.otherChanges);
        }
      }

      reports.clear();
      return Optional.of(fix.build());
    }
  }

  /** A FixAggregator that saves up all its findings from within a single method to emit at once. */
  protected FixAggregator findingPerMethod() {
    return new FindingPerMethod();
  }

  private static final class FindingPerSite implements FixAggregator {
    private FindingPerSite() {}

    private static final FindingPerSite INSTANCE = new FindingPerSite();

    @Override
    public Optional<SuggestedFix> report(TryBlock t) {
      return Optional.of(
          t.closeBraceAfter
              .map(where -> t.otherChanges.postfixWith(where, "}"))
              .orElse(t.otherChanges)
              .build());
    }

    @Override
    public Optional<SuggestedFix> flush() {
      return Optional.empty();
    }
  }

  /** A FixAggregator that emits a separate fix for each method usage. */
  protected FixAggregator findingPerSite() {
    return FindingPerSite.INSTANCE;
  }
}
