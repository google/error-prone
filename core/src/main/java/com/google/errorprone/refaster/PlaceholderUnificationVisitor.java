/*
 * Copyright 2014 Google Inc. All rights reserved.
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

import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.errorprone.refaster.PlaceholderUnificationVisitor.State;
import com.google.errorprone.refaster.UPlaceholderExpression.PlaceholderParamIdent;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
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
import javax.annotation.Nullable;

/**
 * Given a tree as input, returns all the ways this placeholder invocation could be matched with
 * that tree, represented as States, containing the {@code Unifier}, the list of all parameters of
 * the placeholder method that were unified with subtrees of the given tree, and a
 * {@code JCExpression} representing the implementation of the placeholder method, with references
 * to the placeholder parameters replaced with a corresponding {@code PlaceholderParamIdent}.
 */
@AutoValue
abstract class PlaceholderUnificationVisitor
    extends SimpleTreeVisitor<Choice<? extends State<? extends JCTree>>,
                              State<?>> {
  
  /**
   * Represents the state of a placeholder unification in progress, including the
   * current unifier state, the parameters of the placeholder method that have been bound,
   * and a result used to store additional state.
   */
  @AutoValue
  abstract static class State<R> {
    static <R> State<R> create(
        List<UVariableDecl> seenParameters, Unifier unifier, @Nullable R result) {
      return new AutoValue_PlaceholderUnificationVisitor_State<R>(
          seenParameters, unifier, result);
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
      TreeMaker maker, 
      Map<UVariableDecl, UExpression> arguments) {
    return new AutoValue_PlaceholderUnificationVisitor(maker, ImmutableMap.copyOf(arguments));
  }
  
  abstract TreeMaker maker();
  abstract ImmutableMap<UVariableDecl, UExpression> arguments();

  /**
   * Returns all the ways this tree might be unified with the arguments to this placeholder
   * invocation. That is, if the placeholder invocation looks like
   * {@code placeholder(arg1, arg2, ...)}, then the {@code Choice} will contain any ways this
   * tree can be unified with {@code arg1}, {@code arg2}, or the other arguments.
   */
  Choice<State<PlaceholderParamIdent>> tryBindArguments(
      final ExpressionTree node, final State<?> state) {
    return Choice.from(arguments().entrySet())
        .thenChoose(new Function<Map.Entry<UVariableDecl, UExpression>,
                                 Choice<State<PlaceholderParamIdent>>>() {
          @Override
          public Choice<State<PlaceholderParamIdent>> apply(
              final Map.Entry<UVariableDecl, UExpression> entry) {
            return unifyParam(entry.getKey(), entry.getValue(), node, state.fork());
          }
        });
  }
  
  private Choice<State<PlaceholderParamIdent>> unifyParam(
      final UVariableDecl placeholderParam,
      UExpression placeholderArg, 
      ExpressionTree toUnify, 
      final State<?> state) {
    return placeholderArg.unify(toUnify, state.unifier()).transform(
        new Function<Unifier, State<PlaceholderParamIdent>>() {
          @Override
          public State<PlaceholderParamIdent> apply(Unifier unifier) {
            return State.create(state.seenParameters().prepend(placeholderParam), unifier,
                new PlaceholderParamIdent(placeholderParam, unifier.getContext()));
          }
        });
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
    Choice<State<List<JCTree>>> choice =
        Choice.of(state.withResult(List.<JCTree>nil()));
    for (final Tree node : nodes) {
      choice = choice.thenChoose(new Function<State<List<JCTree>>,
          Choice<State<List<JCTree>>>>() {
        @Override
        public Choice<State<List<JCTree>>> apply(
            final State<List<JCTree>> state) {
          return unify(node, state).transform(new Function<
              State<? extends JCTree>,
              State<List<JCTree>>>() {

            @Override
            public State<List<JCTree>> apply(
                State<? extends JCTree> treeState) {
              return treeState.withResult(state.result().prepend(treeState.result()));
            }
          });
        }
      });
    }
    return choice.transform(new Function<State<List<JCTree>>, State<List<JCTree>>>() {
      @Override
      public State<List<JCTree>> apply(State<List<JCTree>> state) {
        return state.withResult(state.result().reverse());
      }
    });
  }

  static boolean equivalentExprs(Unifier unifier, JCExpression expr1, JCExpression expr2) {
    return Types.instance(unifier.getContext()).isSameType(expr2.type, expr1.type)
        && expr2.toString().equals(expr1.toString());
  }

  /**
   * Verifies that the given tree does not directly conflict with an already-bound
   * {@code UFreeIdent} or {@code ULocalVarIdent}.
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
   * Returns all the ways this placeholder invocation might unify with the specified tree: either
   * by unifying the entire tree with an argument to the placeholder invocation, or by recursing
   * on the subtrees.
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
   * Returns all the ways this placeholder invocation might unify with the specified list of
   * trees.
   */
  public Choice<State<List<JCExpression>>> unifyExpressions(
      @Nullable Iterable<? extends ExpressionTree> nodes, State<?> state) {
    return unify(nodes, state).transform(
        new Function<State<List<JCTree>>, State<List<JCExpression>>>() {
          @Override
          public State<List<JCExpression>> apply(State<List<JCTree>> state) {
            return state.withResult(List.convert(JCExpression.class, state.result()));
          }
        });
  }
  
  @SuppressWarnings("unchecked")
  public Choice<? extends State<? extends JCStatement>> unifyStatement(
      @Nullable StatementTree node, State<?> state) {
    return (Choice<? extends State<? extends JCStatement>>) unify(node, state);
  }
  
  public Choice<State<List<JCStatement>>> unifyStatements(
      @Nullable Iterable<? extends StatementTree> nodes, State<?> state) {
    return unify(nodes, state).transform(
        new Function<State<List<JCTree>>, State<List<JCStatement>>>() {
          @Override
          public State<List<JCStatement>> apply(State<List<JCTree>> state) {
            return state.withResult(List.convert(JCStatement.class, state.result()));
          }
        });
  }

  @Override
  protected Choice<State<JCTree>> defaultAction(
      Tree node, State<?> state) {
    return Choice.of(state.withResult((JCTree) node));
  }

  /*
   * All the visit methods look more or less like this one: we recursively visit the first 
   * subnode of the provided tree, and for each unification state of that subnode, we 
   * continue recursively through the next subnode, and when we have recursed through all 
   * subnodes, we rebuild the same type of tree node with each subtree replaced by its copied or 
   * replaced version.
   * 
   * This looks much less ugly with lambdas and streams, but we can't use those yet.
   */

  @Override
  public Choice<State<JCArrayAccess>> visitArrayAccess(
      final ArrayAccessTree node, State<?> state) {
    return unifyExpression(node.getExpression(), state)
        .thenChoose(new Function<State<? extends JCExpression>,
                                 Choice<State<JCArrayAccess>>>() {
          @Override
          public Choice<State<JCArrayAccess>> apply(
              final State<? extends JCExpression> expressionState) {
            return unifyExpression(node.getIndex(), expressionState)
                .transform(new Function<State<? extends JCExpression>,
                                        State<JCArrayAccess>>() {
                  @Override
                  public State<JCArrayAccess> apply(
                      State<? extends JCExpression> indexState) {
                    return indexState.withResult(
                        maker().Indexed(expressionState.result(), indexState.result()));
                  }
                });
          }
        });
  }

  @Override
  public Choice<State<JCBinary>> visitBinary(
      final BinaryTree node, State<?> state) {
    final Tag tag = ((JCBinary) node).getTag();
    return unifyExpression(node.getLeftOperand(), state)
        .thenChoose(new Function<State<? extends JCExpression>,
                                 Choice<State<JCBinary>>>() {
          @Override
          public Choice<State<JCBinary>> apply(
              final State<? extends JCExpression> leftState) {
            return unifyExpression(node.getRightOperand(), leftState)
                .transform(new Function<State<? extends JCExpression>,
                                        State<JCBinary>>() {
                  @Override
                  public State<JCBinary> apply(
                      State<? extends JCExpression> rightState) {
                    return rightState.withResult(
                        maker().Binary(tag, leftState.result(), rightState.result()));
                  }
                });
          }
        });
  }

  @Override
  public Choice<State<JCMethodInvocation>> visitMethodInvocation(
      final MethodInvocationTree node, State<?> state) {
    return unifyExpression(node.getMethodSelect(), state)
        .thenChoose(new Function<State<? extends JCExpression>,
                                 Choice<State<JCMethodInvocation>>>() {
          @Override
          public Choice<State<JCMethodInvocation>> apply(
              final State<? extends JCExpression> selectState) {
            return unifyExpressions(node.getArguments(), selectState)
                .transform(new Function<State<List<JCExpression>>,
                                        State<JCMethodInvocation>>() {
                  @Override
                  public State<JCMethodInvocation> apply(
                      State<List<JCExpression>> argsState) {
                    return argsState.withResult(
                        maker().Apply(null, selectState.result(), argsState.result()));
                  }
                });
          }
        });
  }

  @Override
  public Choice<State<JCFieldAccess>> visitMemberSelect(
      final MemberSelectTree node, State<?> state) {
    return unifyExpression(node.getExpression(), state)
        .transform(new Function<State<? extends JCExpression>,
                                State<JCFieldAccess>>() {
          @Override
          public State<JCFieldAccess> apply(
              State<? extends JCExpression> exprState) {
            return exprState.withResult(
                maker().Select(exprState.result(), (Name) node.getIdentifier()));
          }
        });
  }

  @Override
  public Choice<State<JCParens>> visitParenthesized(
      ParenthesizedTree node, State<?> state) {
    return unifyExpression(node.getExpression(), state)
        .transform(new Function<State<? extends JCExpression>,
                                State<JCParens>>() {
          @Override
          public State<JCParens> apply(
              State<? extends JCExpression> expressionState) {
            return expressionState.withResult(maker().Parens(expressionState.result()));
          }
        });
  }

  private static final Set<Tag> MUTATING_UNARY_TAGS = 
      Collections.unmodifiableSet(EnumSet.of(Tag.PREINC, Tag.PREDEC, Tag.POSTINC, Tag.POSTDEC));
  
  @Override
  public Choice<State<JCUnary>> visitUnary(
      UnaryTree node, State<?> state) {
    final Tag tag = ((JCUnary) node).getTag();
    return unifyExpression(node.getExpression(), state)
        .thenOption(new Function<State<? extends JCExpression>,
                                Optional<State<JCUnary>>>() {
          @Override
          public Optional<State<JCUnary>> apply(
              State<? extends JCExpression> expressionState) {
            if (MUTATING_UNARY_TAGS.contains(tag) 
                && expressionState.result() instanceof PlaceholderParamIdent) {
              return Optional.absent();
            }
            return Optional.of(
                expressionState.withResult(maker().Unary(tag, expressionState.result())));
          }
        });
  }

  @Override
  public Choice<State<JCTypeCast>> visitTypeCast(
      final TypeCastTree node, State<?> state) {
    return unifyExpression(node.getExpression(), state)
        .transform(new Function<State<? extends JCExpression>,
                                State<JCTypeCast>>() {
          @Override
          public State<JCTypeCast> apply(
              State<? extends JCExpression> expressionState) {
            return expressionState.withResult(
                maker().TypeCast((JCTree) node.getType(), expressionState.result()));
          }
        });
  }

  @Override
  public Choice<State<JCInstanceOf>> visitInstanceOf(
      final InstanceOfTree node, State<?> state) {
    return unifyExpression(node.getExpression(), state)
        .transform(new Function<State<? extends JCExpression>,
                                State<JCInstanceOf>>() {
          @Override
          public State<JCInstanceOf> apply(
              State<? extends JCExpression> expressionState) {
            return expressionState.withResult(
                maker().TypeTest(expressionState.result(), (JCTree) node.getType()));
          }
        });
  }

  @Override
  public Choice<State<JCNewClass>> visitNewClass(
      final NewClassTree node, State<?> state) {
    if (node.getEnclosingExpression() != null
        || (node.getTypeArguments() != null && !node.getTypeArguments().isEmpty())
        || node.getClassBody() != null) {
      return Choice.none();
    }
    return unifyExpression(node.getIdentifier(), state)
        .thenChoose(new Function<State<? extends JCExpression>,
                                 Choice<State<JCNewClass>>>() {
          @Override
          public Choice<State<JCNewClass>> apply(
              final State<? extends JCExpression> identifierState) {
            return unifyExpressions(node.getArguments(), identifierState)
                .transform(new Function<State<List<JCExpression>>,
                                        State<JCNewClass>>() {
                  @Override
                  public State<JCNewClass> apply(
                      State<List<JCExpression>> argsState) {
                    return argsState.withResult(maker().NewClass(
                        null, null, identifierState.result(), argsState.result(), null));
                  }
                });
          }
        });
  }

  @Override
  public Choice<State<JCNewArray>> visitNewArray(
      final NewArrayTree node, State<?> state) {
    return unifyExpressions(node.getDimensions(), state)
        .thenChoose(new Function<State<List<JCExpression>>,
                                 Choice<State<JCNewArray>>>() {
          @Override
          public Choice<State<JCNewArray>> apply(
              final State<List<JCExpression>> dimsState) {
            return unifyExpressions(node.getInitializers(), dimsState)
                .transform(new Function<State<List<JCExpression>>,
                                        State<JCNewArray>>() {
                  @Override
                  public State<JCNewArray> apply(
                      State<List<JCExpression>> initsState) {
                    return initsState.withResult(maker().NewArray(
                        (JCExpression) node.getType(), dimsState.result(), initsState.result()));
                  }
                });
          }
        });
  }

  @Override
  public Choice<State<JCConditional>> visitConditionalExpression(
      final ConditionalExpressionTree node, State<?> state) {
    return unifyExpression(node.getCondition(), state)
        .thenChoose(new Function<State<? extends JCExpression>,
                                 Choice<State<JCConditional>>>() {

          @Override
          public Choice<State<JCConditional>> apply(
              final State<? extends JCExpression> condState) {
            return unifyExpression(node.getTrueExpression(), condState)
                .thenChoose(new Function<State<? extends JCExpression>,
                                         Choice<State<JCConditional>>>() {
                  @Override
                  public Choice<State<JCConditional>> apply(
                      final State<? extends JCExpression> trueState) {
                    return unifyExpression(node.getFalseExpression(), trueState)
                        .transform(
                            new Function<State<? extends JCExpression>,
                                         State<JCConditional>>() {
                              @Override
                              public State<JCConditional> apply(
                                  State<? extends JCExpression>
                                  falseState) {
                                return falseState.withResult(maker().Conditional(
                                    condState.result(), trueState.result(), falseState.result()));
                              }
                            });
                  }
                });
          }
        });
  }

  @Override
  public Choice<State<JCAssign>> visitAssignment(
      final AssignmentTree node, State<?> state) {
    return unifyExpression(node.getVariable(), state)
        .thenChoose(new Function<State<? extends JCExpression>,
                                 Choice<State<JCAssign>>>() {

          @Override
          public Choice<State<JCAssign>> apply(
              final State<? extends JCExpression> varState) {
            if (varState.result() instanceof PlaceholderParamIdent) {
              // forbid assignment to placeholder variables
              return Choice.none();
            }
            return unifyExpression(node.getExpression(), varState)
                .transform(new Function<State<? extends JCExpression>,
                                        State<JCAssign>>() {
                  @Override
                  public State<JCAssign> apply(
                      State<? extends JCExpression> exprState) {
                    return exprState.withResult(
                        maker().Assign(varState.result(), exprState.result()));
                  }
                });
          }
        });
  }

  @Override
  public Choice<State<JCAssignOp>> visitCompoundAssignment(
      final CompoundAssignmentTree node, State<?> state) {
    return unifyExpression(node.getVariable(), state).thenChoose(
        new Function<State<? extends JCExpression>, Choice<State<JCAssignOp>>>() {
          @Override
          public Choice<State<JCAssignOp>> apply(final State<? extends JCExpression> varState) {
            if (varState.result() instanceof PlaceholderParamIdent) {
              // forbid assignment to placeholder variables
              return Choice.none();
            }
            return unifyExpression(node.getExpression(), varState).transform(
                new Function<State<? extends JCExpression>, State<JCAssignOp>>() {
                  @Override
                  public State<JCAssignOp> apply(State<? extends JCExpression> exprState) {
                    return exprState.withResult(maker().Assignop(((JCAssignOp) node).getTag(),
                        varState.result(), exprState.result()));
                  }
                });
          }
        });
  }

  @Override
  public Choice<State<JCExpressionStatement>> visitExpressionStatement(
      ExpressionStatementTree node, State<?> state) {
    return unifyExpression(node.getExpression(), state).transform(new Function<
        State<? extends JCExpression>,
        State<JCExpressionStatement>>() {
      @Override
      public State<JCExpressionStatement> apply(
          State<? extends JCExpression> exprState) {
        return exprState.withResult(maker().Exec(exprState.result()));
      }
    });
  }

  @Override
  public Choice<State<JCBlock>> visitBlock(BlockTree node,
      State<?> state) {
    return unifyStatements(node.getStatements(), state).transform(new Function<
        State<List<JCStatement>>, State<JCBlock>>() {

      @Override
      public State<JCBlock> apply(
          State<List<JCStatement>> state) {
        return state.withResult(maker().Block(0, state.result()));
      }
    });
  }

  @Override
  public Choice<State<JCThrow>> visitThrow(ThrowTree node,
      State<?> state) {
    return unifyExpression(node.getExpression(), state).transform(new Function<
        State<? extends JCExpression>,
        State<JCThrow>>() {

      @Override
      public State<JCThrow> apply(
          State<? extends JCExpression> exprState) {
        return exprState.withResult(maker().Throw(exprState.result()));
      }
    });
  }

  @Override
  public Choice<State<JCEnhancedForLoop>> visitEnhancedForLoop(
      final EnhancedForLoopTree node, State<?> state) {
    return unifyExpression(node.getExpression(), state).thenChoose(new Function<
        State<? extends JCExpression>,
        Choice<State<JCEnhancedForLoop>>>() {

      @Override
      public Choice<State<JCEnhancedForLoop>> apply(
          final State<? extends JCExpression> exprState) {
        return unifyStatement(node.getStatement(), exprState).transform(new Function<
            State<? extends JCStatement>,
            State<JCEnhancedForLoop>>() {

          @Override
          public State<JCEnhancedForLoop> apply(
              State<? extends JCStatement> stmtState) {
            return stmtState.withResult(
                maker().ForeachLoop(
                    (JCVariableDecl) node.getVariable(), 
                    exprState.result(), 
                    stmtState.result()));
          }
        });
      }
    });
  }

  @Override
  public Choice<State<JCIf>> visitIf(final IfTree node, State<?> state) {
    return unifyExpression(node.getCondition(), state).thenChoose(
        new Function<State<? extends JCExpression>, Choice<State<JCIf>>>() {
          @Override
          public Choice<State<JCIf>> apply(final State<? extends JCExpression> condState) {
            return unifyStatement(node.getThenStatement(), condState).thenChoose(
                new Function<State<? extends JCStatement>, Choice<State<JCIf>>>() {
                  @Override
                  public Choice<State<JCIf>> apply(final State<? extends JCStatement> thenState) {
                    return unifyStatement(node.getElseStatement(), thenState).transform(
                        new Function<State<? extends JCStatement>, State<JCIf>>() {
                          @Override
                          public State<JCIf> apply(State<? extends JCStatement> elseState) {
                            return elseState.withResult(maker().If(condState.result(),
                                thenState.result(), elseState.result()));
                          }
                        });
                  }
                });
          }
        });
  }

  @Override
  public Choice<State<JCDoWhileLoop>> visitDoWhileLoop(final DoWhileLoopTree node,
      State<?> state) {
    return unifyStatement(node.getStatement(), state).thenChoose(
        new Function<State<? extends JCStatement>, Choice<State<JCDoWhileLoop>>>() {

          @Override
          public Choice<State<JCDoWhileLoop>> apply(
              final State<? extends JCStatement> stmtState) {
            return unifyExpression(node.getCondition(), stmtState).transform(
                new Function<State<? extends JCExpression>, State<JCDoWhileLoop>>() {

                  @Override
                  public State<JCDoWhileLoop> apply(State<? extends JCExpression> condState) {
                    return condState.withResult(
                        maker().DoLoop(stmtState.result(), condState.result()));
                  }
                });
          }
        });
  }

  @Override
  public Choice<State<JCForLoop>> visitForLoop(final ForLoopTree node, State<?> state) {
    return unifyStatements(node.getInitializer(), state).thenChoose(
        new Function<State<List<JCStatement>>, Choice<State<JCForLoop>>>() {

          @Override
          public Choice<State<JCForLoop>> apply(final State<List<JCStatement>> initsState) {
            return unifyExpression(node.getCondition(), initsState).thenChoose(
                new Function<State<? extends JCExpression>, Choice<State<JCForLoop>>>() {

                  @Override
                  public Choice<State<JCForLoop>> apply(
                      final State<? extends JCExpression> condState) {
                    return unifyStatements(node.getUpdate(), condState).thenChoose(
                        new Function<State<List<JCStatement>>, Choice<State<JCForLoop>>>() {

                          @Override
                          public Choice<State<JCForLoop>> apply(
                              final State<List<JCStatement>> updateState) {
                            return unifyStatement(node.getStatement(), updateState).transform(
                                new Function<State<? extends JCStatement>, State<JCForLoop>>() {

                                  @Override
                                  public State<JCForLoop> apply(
                                      State<? extends JCStatement> stmtState) {
                                    return stmtState.withResult(maker().ForLoop(initsState.result(),
                                        condState.result(), List.convert(
                                            JCExpressionStatement.class, updateState.result()),
                                        stmtState.result()));
                                  }
                                });
                          }
                        });
                  }
                });
          }
        });
  }

  @Override
  public Choice<State<JCLabeledStatement>> visitLabeledStatement(
      final LabeledStatementTree node, State<?> state) {
    return unifyStatement(node.getStatement(), state).transform(
        new Function<State<? extends JCStatement>, State<JCLabeledStatement>>() {
          @Override
          public State<JCLabeledStatement> apply(State<? extends JCStatement> stmtState) {
            return stmtState.withResult(
                maker().Labelled((Name) node.getLabel(), stmtState.result()));
          }
        });
  }

  @Override
  public Choice<State<JCVariableDecl>> visitVariable(final VariableTree node, State<?> state) {
    return unifyExpression(node.getInitializer(), state).transform(
        new Function<State<? extends JCExpression>, State<JCVariableDecl>>() {

          @Override
          public State<JCVariableDecl> apply(State<? extends JCExpression> initState) {
            return initState.withResult(maker().VarDef((JCModifiers) node.getModifiers(),
                (Name) node.getName(), (JCExpression) node.getType(), initState.result()));
          }
        });
  }

  @Override
  public Choice<State<JCWhileLoop>> visitWhileLoop(final WhileLoopTree node, State<?> state) {
    return unifyExpression(node.getCondition(), state).thenChoose(
        new Function<State<? extends JCExpression>, Choice<State<JCWhileLoop>>>() {
          @Override
          public Choice<State<JCWhileLoop>> apply(final State<? extends JCExpression> condState) {
            return unifyStatement(node.getStatement(), condState).transform(
                new Function<State<? extends JCStatement>, State<JCWhileLoop>>() {
                  @Override
                  public State<JCWhileLoop> apply(State<? extends JCStatement> stmtState) {
                    return stmtState.withResult(
                        maker().WhileLoop(condState.result(), stmtState.result()));
                  }
                });
          }
        });
  }

  @Override
  public Choice<State<JCSynchronized>> visitSynchronized(final SynchronizedTree node,
      State<?> state) {
    return unifyExpression(node.getExpression(), state).thenChoose(
        new Function<State<? extends JCExpression>, Choice<State<JCSynchronized>>>() {

          @Override
          public Choice<State<JCSynchronized>> apply(
              final State<? extends JCExpression> exprState) {
            return unifyStatement(node.getBlock(), exprState).transform(
                new Function<State<? extends JCStatement>, State<JCSynchronized>>() {

                  @Override
                  public State<JCSynchronized> apply(State<? extends JCStatement> blockState) {
                    return blockState.withResult(
                        maker().Synchronized(exprState.result(), (JCBlock) blockState.result()));
                  }
                });
          }
        });
  }

  @Override
  public Choice<State<JCReturn>> visitReturn(ReturnTree node, State<?> state) {
    return unifyExpression(node.getExpression(), state).transform(
        new Function<State<? extends JCExpression>, State<JCReturn>>() {
          @Override
          public State<JCReturn> apply(State<? extends JCExpression> exprState) {
            return exprState.withResult(maker().Return(exprState.result()));
          }
        });
  }

  @Override
  public Choice<State<JCTry>> visitTry(final TryTree node, State<?> state) {
    return unify(node.getResources(), state).thenChoose(
        new Function<State<List<JCTree>>, Choice<State<JCTry>>>() {
          @Override
          public Choice<State<JCTry>> apply(final State<List<JCTree>> resourcesState) {
            return unifyStatement(node.getBlock(), resourcesState).thenChoose(
                new Function<State<? extends JCStatement>, Choice<State<JCTry>>>() {
                  @Override
                  public Choice<State<JCTry>> apply(
                      final State<? extends JCStatement> blockState) {
                    return unify(node.getCatches(), blockState).thenChoose(
                        new Function<State<List<JCTree>>, Choice<State<JCTry>>>() {
                          @Override
                          public Choice<State<JCTry>> apply(
                              final State<List<JCTree>> catchesState) {
                            return unifyStatement(node.getFinallyBlock(), catchesState).transform(
                                new Function<State<? extends JCStatement>, State<JCTry>>() {
                                  @Override
                                  public State<JCTry> apply(
                                      State<? extends JCStatement> finallyState) {
                                    return finallyState.withResult(maker().Try(
                                        resourcesState.result(), (JCBlock) blockState.result(),
                                        List.convert(JCCatch.class, catchesState.result()),
                                        (JCBlock) finallyState.result()));
                                  }
                                });
                          }
                        });
                  }
                });
          }
        });
  }

  @Override
  public Choice<State<JCCatch>> visitCatch(final CatchTree node, State<?> state) {
    return unifyStatement(node.getBlock(), state).transform(
        new Function<State<? extends JCStatement>, State<JCCatch>>() {
          @Override
          public State<JCCatch> apply(State<? extends JCStatement> blockState) {
            return blockState.withResult(maker().Catch(
                (JCVariableDecl) node.getParameter(),
                (JCBlock) blockState.result()));
          }
        });
  }

  @Override
  public Choice<State<JCSwitch>> visitSwitch(final SwitchTree node, State<?> state) {
    return unifyExpression(node.getExpression(), state).thenChoose(
        new Function<State<? extends JCExpression>, Choice<State<JCSwitch>>>() {
          @Override
          public Choice<State<JCSwitch>> apply(final State<? extends JCExpression> exprState) {
            return unify(node.getCases(), exprState).transform(
                new Function<State<List<JCTree>>, State<JCSwitch>>() {
                  @Override
                  public State<JCSwitch> apply(State<List<JCTree>> casesState) {
                    return casesState.withResult(maker().Switch(exprState.result(),
                        List.convert(JCCase.class, casesState.result())));
                  }
                });
          }
        });
  }

  @Override
  public Choice<State<JCCase>> visitCase(final CaseTree node, State<?> state) {
    return unifyStatements(node.getStatements(), state).transform(
        new Function<State<List<JCStatement>>, State<JCCase>>() {
          @Override
          public State<JCCase> apply(State<List<JCStatement>> stmtsState) {
            return stmtsState.withResult(
                maker().Case((JCExpression) node.getExpression(), stmtsState.result()));
          }
        });
  }

  @Override
  public Choice<State<JCLambda>> visitLambdaExpression(
      final LambdaExpressionTree node, State<?> state) {
    return unify(node.getBody(), state).transform(
        new Function<State<? extends JCTree>, State<JCLambda>>() {
          @Override
          public State<JCLambda> apply(State<? extends JCTree> bodyState) {
            return bodyState.withResult(maker().Lambda(
                List.convert(JCVariableDecl.class, 
                    (List<? extends VariableTree>) node.getParameters()),
                bodyState.result()));
          }
        });
  }

  @Override
  public Choice<State<JCMemberReference>> visitMemberReference(final MemberReferenceTree node,
      State<?> state) {
    return unifyExpression(node.getQualifierExpression(), state).transform(
        new Function<State<? extends JCExpression>, State<JCMemberReference>>() {
          @Override
          public State<JCMemberReference> apply(State<? extends JCExpression> exprState) {
            return exprState.withResult(maker().Reference(node.getMode(), (Name) node.getName(),
                exprState.result(), 
                List.convert(JCExpression.class,
                    (List<? extends ExpressionTree>) node.getTypeArguments())));
          }
        });
  }
}