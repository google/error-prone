/*
 * Copyright 2014 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.refaster;

import static com.google.common.base.Preconditions.checkState;

import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.errorprone.refaster.PlaceholderUnificationVisitor.State;
import com.google.errorprone.refaster.UPlaceholderExpression.PlaceholderParamIdent;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.RuntimeVersion;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCArrayAccess;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCAssignOp;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCCatch;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCConditional;
import com.sun.tools.javac.tree.JCTree.JCDoWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCEnhancedForLoop;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCIf;
import com.sun.tools.javac.tree.JCTree.JCInstanceOf;
import com.sun.tools.javac.tree.JCTree.JCLabeledStatement;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCMemberReference;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCNewArray;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCParens;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCSwitch;
import com.sun.tools.javac.tree.JCTree.JCSynchronized;
import com.sun.tools.javac.tree.JCTree.JCThrow;
import com.sun.tools.javac.tree.JCTree.JCTry;
import com.sun.tools.javac.tree.JCTree.JCTypeCast;
import com.sun.tools.javac.tree.JCTree.JCUnary;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.JCWhileLoop;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import javax.annotation.Nullable;

/**
 * Given a tree as input, returns all the ways this placeholder invocation could be matched with
 * that tree, represented as States, containing the {@code Unifier}, the list of all parameters of
 * the placeholder method that were unified with subtrees of the given tree, and a {@code
 * JCExpression} representing the implementation of the placeholder method, with references to the
 * placeholder parameters replaced with a corresponding {@code PlaceholderParamIdent}.
 */
@AutoValue
abstract class PlaceholderUnificationVisitor
    extends SimpleTreeVisitor<Choice<? extends State<? extends JCTree>>, State<?>> {

  /**
   * Represents the state of a placeholder unification in progress, including the current unifier
   * state, the parameters of the placeholder method that have been bound, and a result used to
   * store additional state.
   */
  @AutoValue
  abstract static class State<R> {
    static <R> State<R> create(
        List<UVariableDecl> seenParameters, Unifier unifier, @Nullable R result) {
      return new AutoValue_PlaceholderUnificationVisitor_State<>(seenParameters, unifier, result);
    }

    public abstract List<UVariableDecl> seenParameters();

    public abstract Unifier unifier();

    @Nullable
    public abstract R result();

    public <R2> State<R2> withResult(R2 result) {
      return create(seenParameters(), unifier(), result);
    }

    public State<R> fork() {
      return create(seenParameters(), unifier().fork(), result());
    }
  }

  public static PlaceholderUnificationVisitor create(
      TreeMaker maker, Map<UVariableDecl, UExpression> arguments) {
    return new AutoValue_PlaceholderUnificationVisitor(maker, ImmutableMap.copyOf(arguments));
  }

  abstract TreeMaker maker();

  abstract ImmutableMap<UVariableDecl, UExpression> arguments();

  /**
   * Returns all the ways this tree might be unified with the arguments to this placeholder
   * invocation. That is, if the placeholder invocation looks like {@code placeholder(arg1, arg2,
   * ...)}, then the {@code Choice} will contain any ways this tree can be unified with {@code
   * arg1}, {@code arg2}, or the other arguments.
   */
  Choice<State<PlaceholderParamIdent>> tryBindArguments(
      final ExpressionTree node, final State<?> state) {
    return Choice.from(arguments().entrySet())
        .thenChoose(
            (final Map.Entry<UVariableDecl, UExpression> entry) ->
                unifyParam(entry.getKey(), entry.getValue(), node, state.fork()));
  }

  private Choice<State<PlaceholderParamIdent>> unifyParam(
      final UVariableDecl placeholderParam,
      UExpression placeholderArg,
      ExpressionTree toUnify,
      final State<?> state) {
    return placeholderArg
        .unify(toUnify, state.unifier())
        .transform(
            (Unifier unifier) ->
                State.create(
                    state.seenParameters().prepend(placeholderParam),
                    unifier,
                    new PlaceholderParamIdent(placeholderParam, unifier.getContext())));
  }

  public Choice<? extends State<? extends JCTree>> unify(@Nullable Tree node, State<?> state) {
    if (node instanceof ExpressionTree) {
      return unifyExpression((ExpressionTree) node, state);
    } else if (node == null) {
      return Choice.of(state.<JCTree>withResult(null));
    } else {
      return node.accept(this, state);
    }
  }

  public Choice<State<List<JCTree>>> unify(
      @Nullable Iterable<? extends Tree> nodes, State<?> state) {
    if (nodes == null) {
      return Choice.of(state.<List<JCTree>>withResult(null));
    }
    Choice<State<List<JCTree>>> choice = Choice.of(state.withResult(List.<JCTree>nil()));
    for (final Tree node : nodes) {
      choice =
          choice.thenChoose(
              (State<List<JCTree>> s) ->
                  unify(node, s)
                      .transform(
                          treeState ->
                              treeState.withResult(s.result().prepend(treeState.result()))));
    }
    return choice.transform(s -> s.withResult(s.result().reverse()));
  }

  static boolean equivalentExprs(Unifier unifier, JCExpression expr1, JCExpression expr2) {
    return expr1.type != null
        && expr2.type != null
        && Types.instance(unifier.getContext()).isSameType(expr2.type, expr1.type)
        && expr2.toString().equals(expr1.toString());
  }

  /**
   * Verifies that the given tree does not directly conflict with an already-bound {@code
   * UFreeIdent} or {@code ULocalVarIdent}.
   */
  static final TreeVisitor<Boolean, Unifier> FORBIDDEN_REFERENCE_VISITOR =
      new SimpleTreeVisitor<Boolean, Unifier>() {
        @Override
        protected Boolean defaultAction(Tree node, Unifier unifier) {
          if (!(node instanceof JCExpression)) {
            return false;
          }
          JCExpression expr = (JCExpression) node;
          for (UFreeIdent.Key key :
              Iterables.filter(unifier.getBindings().keySet(), UFreeIdent.Key.class)) {
            JCExpression keyBinding = unifier.getBinding(key);
            if (equivalentExprs(unifier, expr, keyBinding)) {
              return true;
            }
          }
          return false;
        }

        @Override
        public Boolean visitIdentifier(IdentifierTree node, Unifier unifier) {
          for (LocalVarBinding localBinding :
              Iterables.filter(unifier.getBindings().values(), LocalVarBinding.class)) {
            if (localBinding.getSymbol().equals(ASTHelpers.getSymbol(node))) {
              return true;
            }
          }
          return defaultAction(node, unifier);
        }
      };

  /**
   * Returns all the ways this placeholder invocation might unify with the specified tree: either by
   * unifying the entire tree with an argument to the placeholder invocation, or by recursing on the
   * subtrees.
   */
  @SuppressWarnings("unchecked")
  public Choice<? extends State<? extends JCExpression>> unifyExpression(
      @Nullable ExpressionTree node, State<?> state) {
    if (node == null) {
      return Choice.of(state.<JCExpression>withResult(null));
    }
    Choice<? extends State<? extends JCExpression>> tryBindArguments =
        tryBindArguments(node, state);
    if (!node.accept(FORBIDDEN_REFERENCE_VISITOR, state.unifier())) {
      return tryBindArguments.or((Choice) node.accept(this, state));
    } else {
      return tryBindArguments;
    }
  }

  /**
   * Returns all the ways this placeholder invocation might unify with the specified list of trees.
   */
  public Choice<State<List<JCExpression>>> unifyExpressions(
      @Nullable Iterable<? extends ExpressionTree> nodes, State<?> state) {
    return unify(nodes, state)
        .transform(s -> s.withResult(List.convert(JCExpression.class, s.result())));
  }

  @SuppressWarnings("unchecked")
  public Choice<? extends State<? extends JCStatement>> unifyStatement(
      @Nullable StatementTree node, State<?> state) {
    return (Choice<? extends State<? extends JCStatement>>) unify(node, state);
  }

  public Choice<State<List<JCStatement>>> unifyStatements(
      @Nullable Iterable<? extends StatementTree> nodes, State<?> state) {
    return chooseSubtrees(
        state, s -> unify(nodes, s), stmts -> List.convert(JCStatement.class, stmts));
  }

  @Override
  protected Choice<State<JCTree>> defaultAction(Tree node, State<?> state) {
    return Choice.of(state.withResult((JCTree) node));
  }

  /**
   * This method, and its overloads, take
   *
   * <ol>
   *   <li>an initial state
   *   <li>functions that, given one state, return a branch choosing a subtree
   *   <li>a function that takes pieces of a tree type and recomposes them
   * </ol>
   */
  private static <T, R> Choice<State<R>> chooseSubtrees(
      State<?> state,
      Function<State<?>, Choice<? extends State<? extends T>>> choice1,
      Function<T, R> finalizer) {
    return choice1.apply(state).transform(s -> s.withResult(finalizer.apply(s.result())));
  }

  private static <T1, T2, R> Choice<State<R>> chooseSubtrees(
      State<?> state,
      Function<State<?>, Choice<? extends State<? extends T1>>> choice1,
      Function<State<?>, Choice<? extends State<? extends T2>>> choice2,
      BiFunction<T1, T2, R> finalizer) {
    return choice1
        .apply(state)
        .thenChoose(
            s1 ->
                choice2
                    .apply(s1)
                    .transform(s2 -> s2.withResult(finalizer.apply(s1.result(), s2.result()))));
  }

  @FunctionalInterface
  private interface TriFunction<T1, T2, T3, R> {
    R apply(T1 t1, T2 t2, T3 t3);
  }

  private static <T1, T2, T3, R> Choice<State<R>> chooseSubtrees(
      State<?> state,
      Function<State<?>, Choice<? extends State<? extends T1>>> choice1,
      Function<State<?>, Choice<? extends State<? extends T2>>> choice2,
      Function<State<?>, Choice<? extends State<? extends T3>>> choice3,
      TriFunction<T1, T2, T3, R> finalizer) {
    return choice1
        .apply(state)
        .thenChoose(
            s1 ->
                choice2
                    .apply(s1)
                    .thenChoose(
                        s2 ->
                            choice3
                                .apply(s2)
                                .transform(
                                    s3 ->
                                        s3.withResult(
                                            finalizer.apply(
                                                s1.result(), s2.result(), s3.result())))));
  }

  @FunctionalInterface
  private interface QuadFunction<T1, T2, T3, T4, R> {
    R apply(T1 t1, T2 t2, T3 t3, T4 t4);
  }

  private static <T1, T2, T3, T4, R> Choice<State<R>> chooseSubtrees(
      State<?> state,
      Function<State<?>, Choice<? extends State<? extends T1>>> choice1,
      Function<State<?>, Choice<? extends State<? extends T2>>> choice2,
      Function<State<?>, Choice<? extends State<? extends T3>>> choice3,
      Function<State<?>, Choice<? extends State<? extends T4>>> choice4,
      QuadFunction<T1, T2, T3, T4, R> finalizer) {
    return choice1
        .apply(state)
        .thenChoose(
            s1 ->
                choice2
                    .apply(s1)
                    .thenChoose(
                        s2 ->
                            choice3
                                .apply(s2)
                                .thenChoose(
                                    s3 ->
                                        choice4
                                            .apply(s3)
                                            .transform(
                                                s4 ->
                                                    s4.withResult(
                                                        finalizer.apply(
                                                            s1.result(),
                                                            s2.result(),
                                                            s3.result(),
                                                            s4.result()))))));
  }

  @Override
  public Choice<State<JCArrayAccess>> visitArrayAccess(final ArrayAccessTree node, State<?> state) {
    return chooseSubtrees(
        state,
        s -> unifyExpression(node.getExpression(), s),
        s -> unifyExpression(node.getIndex(), s),
        maker()::Indexed);
  }

  @Override
  public Choice<State<JCBinary>> visitBinary(final BinaryTree node, State<?> state) {
    final Tag tag = ((JCBinary) node).getTag();
    return chooseSubtrees(
        state,
        s -> unifyExpression(node.getLeftOperand(), s),
        s -> unifyExpression(node.getRightOperand(), s),
        (l, r) -> maker().Binary(tag, l, r));
  }

  @Override
  public Choice<State<JCMethodInvocation>> visitMethodInvocation(
      final MethodInvocationTree node, State<?> state) {
    return chooseSubtrees(
        state,
        s -> unifyExpression(node.getMethodSelect(), s),
        s -> unifyExpressions(node.getArguments(), s),
        (select, args) -> maker().Apply(null, select, args));
  }

  @Override
  public Choice<State<JCFieldAccess>> visitMemberSelect(
      final MemberSelectTree node, State<?> state) {
    return chooseSubtrees(
        state,
        s -> unifyExpression(node.getExpression(), s),
        expr -> maker().Select(expr, (Name) node.getIdentifier()));
  }

  @Override
  public Choice<State<JCParens>> visitParenthesized(ParenthesizedTree node, State<?> state) {
    return chooseSubtrees(state, s -> unifyExpression(node.getExpression(), s), maker()::Parens);
  }

  private static final Set<Tag> MUTATING_UNARY_TAGS =
      Collections.unmodifiableSet(EnumSet.of(Tag.PREINC, Tag.PREDEC, Tag.POSTINC, Tag.POSTDEC));

  @Override
  public Choice<State<JCUnary>> visitUnary(UnaryTree node, State<?> state) {
    final Tag tag = ((JCUnary) node).getTag();
    return chooseSubtrees(
            state, s -> unifyExpression(node.getExpression(), s), expr -> maker().Unary(tag, expr))
        .condition(
            s ->
                !MUTATING_UNARY_TAGS.contains(tag)
                    || !(s.result().getExpression() instanceof PlaceholderParamIdent));
  }

  @Override
  public Choice<State<JCTypeCast>> visitTypeCast(final TypeCastTree node, State<?> state) {
    return chooseSubtrees(
        state,
        s -> unifyExpression(node.getExpression(), s),
        expr -> maker().TypeCast((JCTree) node.getType(), expr));
  }

  @Override
  public Choice<State<JCInstanceOf>> visitInstanceOf(final InstanceOfTree node, State<?> state) {
    return chooseSubtrees(
        state,
        s -> unifyExpression(node.getExpression(), s),
        expr -> maker().TypeTest(expr, (JCTree) node.getType()));
  }

  @Override
  public Choice<State<JCNewClass>> visitNewClass(final NewClassTree node, State<?> state) {
    if (node.getEnclosingExpression() != null
        || (node.getTypeArguments() != null && !node.getTypeArguments().isEmpty())) {
      return Choice.none();
    }

    return chooseSubtrees(
        state,
        s -> unifyExpression(node.getIdentifier(), s),
        s -> unifyExpressions(node.getArguments(), s),
        (ident, args) -> maker().NewClass(null, null, ident, args, fixupAnonClassBody(node.getClassBody())));
  }

  private JCClassDecl fixupAnonClassBody(ClassTree classBody) {
    if (classBody == null) {
      return null;
    }

    JCClassDecl classDecl = (JCClassDecl) classBody;

    List<JCTree> classMembers = classDecl.defs.stream()
        .filter(existingMember -> !(existingMember instanceof MethodTree && ASTHelpers.isGeneratedConstructor((MethodTree) existingMember)))
        .collect(List.collector());

    return maker().ClassDef(
        classDecl.mods,
        classDecl.name,
        classDecl.typarams,
        classDecl.extending,
        classDecl.implementing,
        classMembers);
  }

  @Override
  public Choice<State<JCNewArray>> visitNewArray(final NewArrayTree node, State<?> state) {
    return chooseSubtrees(
        state,
        s -> unifyExpressions(node.getDimensions(), s),
        s -> unifyExpressions(node.getInitializers(), s),
        (dims, inits) -> maker().NewArray((JCExpression) node.getType(), dims, inits));
  }

  @Override
  public Choice<State<JCConditional>> visitConditionalExpression(
      final ConditionalExpressionTree node, State<?> state) {
    return chooseSubtrees(
        state,
        s -> unifyExpression(node.getCondition(), s),
        s -> unifyExpression(node.getTrueExpression(), s),
        s -> unifyExpression(node.getFalseExpression(), s),
        maker()::Conditional);
  }

  @Override
  public Choice<State<JCAssign>> visitAssignment(final AssignmentTree node, State<?> state) {
    return chooseSubtrees(
            state,
            s -> unifyExpression(node.getVariable(), s),
            s -> unifyExpression(node.getExpression(), s),
            maker()::Assign)
        .condition(s -> !(s.result().getVariable() instanceof PlaceholderParamIdent));
  }

  @Override
  public Choice<State<JCAssignOp>> visitCompoundAssignment(
      final CompoundAssignmentTree node, State<?> state) {
    return chooseSubtrees(
            state,
            s -> unifyExpression(node.getVariable(), s),
            s -> unifyExpression(node.getExpression(), s),
            (variable, expr) -> maker().Assignop(((JCAssignOp) node).getTag(), variable, expr))
        .condition(assignOp -> !(assignOp.result().getVariable() instanceof PlaceholderParamIdent));
  }

  @Override
  public Choice<State<JCExpressionStatement>> visitExpressionStatement(
      ExpressionStatementTree node, State<?> state) {
    return chooseSubtrees(state, s -> unifyExpression(node.getExpression(), s), maker()::Exec);
  }

  @Override
  public Choice<State<JCBlock>> visitBlock(BlockTree node, State<?> state) {
    return chooseSubtrees(
        state, s -> unifyStatements(node.getStatements(), s), stmts -> maker().Block(0, stmts));
  }

  @Override
  public Choice<State<JCThrow>> visitThrow(ThrowTree node, State<?> state) {
    return chooseSubtrees(state, s -> unifyExpression(node.getExpression(), s), maker()::Throw);
  }

  @Override
  public Choice<State<JCEnhancedForLoop>> visitEnhancedForLoop(
      final EnhancedForLoopTree node, State<?> state) {
    return chooseSubtrees(
        state,
        s -> unifyExpression(node.getExpression(), s),
        s -> unifyStatement(node.getStatement(), s),
        (expr, stmt) -> maker().ForeachLoop((JCVariableDecl) node.getVariable(), expr, stmt));
  }

  @Override
  public Choice<State<JCIf>> visitIf(final IfTree node, State<?> state) {
    return chooseSubtrees(
        state,
        s -> unifyExpression(node.getCondition(), s),
        s -> unifyStatement(node.getThenStatement(), s),
        s -> unifyStatement(node.getElseStatement(), s),
        maker()::If);
  }

  @Override
  public Choice<State<JCDoWhileLoop>> visitDoWhileLoop(final DoWhileLoopTree node, State<?> state) {
    return chooseSubtrees(
        state,
        s -> unifyStatement(node.getStatement(), s),
        s -> unifyExpression(node.getCondition(), s),
        maker()::DoLoop);
  }

  @Override
  public Choice<State<JCForLoop>> visitForLoop(final ForLoopTree node, State<?> state) {
    return chooseSubtrees(
        state,
        s -> unifyStatements(node.getInitializer(), s),
        s -> unifyExpression(node.getCondition(), s),
        s -> unifyStatements(node.getUpdate(), s),
        s -> unifyStatement(node.getStatement(), s),
        (inits, cond, update, stmt) ->
            maker().ForLoop(inits, cond, List.convert(JCExpressionStatement.class, update), stmt));
  }

  @Override
  public Choice<State<JCLabeledStatement>> visitLabeledStatement(
      final LabeledStatementTree node, State<?> state) {
    return chooseSubtrees(
        state,
        s -> unifyStatement(node.getStatement(), s),
        stmt -> maker().Labelled((Name) node.getLabel(), stmt));
  }

  @Override
  public Choice<State<JCVariableDecl>> visitVariable(final VariableTree node, State<?> state) {
    return chooseSubtrees(
        state,
        s -> unifyExpression(node.getInitializer(), s),
        init ->
            maker()
                .VarDef(
                    (JCModifiers) node.getModifiers(),
                    (Name) node.getName(),
                    (JCExpression) node.getType(),
                    init));
  }

  @Override
  public Choice<State<JCWhileLoop>> visitWhileLoop(final WhileLoopTree node, State<?> state) {
    return chooseSubtrees(
        state,
        s -> unifyExpression(node.getCondition(), s),
        s -> unifyStatement(node.getStatement(), s),
        maker()::WhileLoop);
  }

  @Override
  public Choice<State<JCSynchronized>> visitSynchronized(
      final SynchronizedTree node, State<?> state) {
    return chooseSubtrees(
        state,
        s -> unifyExpression(node.getExpression(), s),
        s -> unifyStatement(node.getBlock(), s),
        (expr, block) -> maker().Synchronized(expr, (JCBlock) block));
  }

  @Override
  public Choice<State<JCReturn>> visitReturn(ReturnTree node, State<?> state) {
    return chooseSubtrees(state, s -> unifyExpression(node.getExpression(), s), maker()::Return);
  }

  @Override
  public Choice<State<JCTry>> visitTry(final TryTree node, State<?> state) {
    return chooseSubtrees(
        state,
        s -> unify(node.getResources(), s),
        s -> unifyStatement(node.getBlock(), s),
        s -> unify(node.getCatches(), s),
        s -> unifyStatement(node.getFinallyBlock(), s),
        (resources, block, catches, finallyBlock) ->
            maker()
                .Try(
                    resources,
                    (JCBlock) block,
                    List.convert(JCCatch.class, catches),
                    (JCBlock) finallyBlock));
  }

  @Override
  public Choice<State<JCCatch>> visitCatch(final CatchTree node, State<?> state) {
    return chooseSubtrees(
        state,
        s -> unifyStatement(node.getBlock(), s),
        block -> maker().Catch((JCVariableDecl) node.getParameter(), (JCBlock) block));
  }

  @Override
  public Choice<State<JCSwitch>> visitSwitch(final SwitchTree node, State<?> state) {
    return chooseSubtrees(
        state,
        s -> unifyExpression(node.getExpression(), s),
        s -> unify(node.getCases(), s),
        (expr, cases) -> maker().Switch(expr, List.convert(JCCase.class, cases)));
  }

  @Override
  public Choice<State<JCCase>> visitCase(final CaseTree node, State<?> state) {
    return chooseSubtrees(
        state, s -> unifyStatements(node.getStatements(), s), stmts -> makeCase(node, stmts));
  }

  private JCCase makeCase(CaseTree node, List<JCStatement> stmts) {
    try {
      if (RuntimeVersion.isAtLeast12()) {
        Enum<?> caseKind = (Enum) CaseTree.class.getMethod("getCaseKind").invoke(node);
        checkState(
            caseKind.name().contentEquals("STATEMENT"),
            "expression switches are not supported yet");
        return (JCCase)
            TreeMaker.class
                .getMethod(
                    "Case",
                    Class.forName("com.sun.source.tree.CaseTree.CaseKind"),
                    List.class,
                    List.class,
                    JCTree.class)
                .invoke(
                    maker(),
                    caseKind,
                    List.of((JCExpression) node.getExpression()),
                    stmts,
                    /* body= */ null);
      } else {
        return (JCCase)
            TreeMaker.class
                .getMethod("Case", JCExpression.class, List.class)
                .invoke(maker(), node.getExpression(), stmts);
      }
    } catch (ReflectiveOperationException e) {
      throw new LinkageError(e.getMessage(), e);
    }
  }

  @Override
  public Choice<State<JCLambda>> visitLambdaExpression(
      final LambdaExpressionTree node, State<?> state) {
    return chooseSubtrees(
        state,
        s -> unify(node.getBody(), s),
        body ->
            maker()
                .Lambda(
                    List.convert(
                        JCVariableDecl.class, (List<? extends VariableTree>) node.getParameters()),
                    body));
  }

  @Override
  public Choice<State<JCMemberReference>> visitMemberReference(
      final MemberReferenceTree node, State<?> state) {
    return chooseSubtrees(
        state,
        s -> unifyExpression(node.getQualifierExpression(), s),
        expr ->
            maker()
                .Reference(
                    node.getMode(),
                    (Name) node.getName(),
                    expr,
                    List.convert(
                        JCExpression.class,
                        (List<? extends ExpressionTree>) node.getTypeArguments())));
  }
}
