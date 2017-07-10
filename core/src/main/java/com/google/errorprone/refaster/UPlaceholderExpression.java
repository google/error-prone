/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.refaster;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.auto.value.AutoValue;
import com.google.common.base.Functions;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
import java.util.Map;

/**
 * {@code UTree} representation of an invocation of a placeholder method.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
public abstract class UPlaceholderExpression extends UExpression {

  static UPlaceholderExpression create(
      PlaceholderMethod placeholder, Iterable<? extends UExpression> arguments) {
    ImmutableList<UVariableDecl> placeholderParams = placeholder.parameters().asList();
    ImmutableList<UExpression> argumentsList = ImmutableList.copyOf(arguments);
    ImmutableMap.Builder<UVariableDecl, UExpression> builder = ImmutableMap.builder();
    for (int i = 0; i < placeholderParams.size(); i++) {
      builder.put(placeholderParams.get(i), argumentsList.get(i));
    }
    return new AutoValue_UPlaceholderExpression(placeholder, builder.build());
  }

  abstract PlaceholderMethod placeholder();

  abstract ImmutableMap<UVariableDecl, UExpression> arguments();

  public static final class PlaceholderParamIdent extends JCIdent {
    final UVariableDecl param;

    PlaceholderParamIdent(UVariableDecl param, Context context) {
      super(Names.instance(context).fromString(param.getName().contents()), null);
      this.param = checkNotNull(param);
    }
  }

  static class UncheckedCouldNotResolveImportException extends RuntimeException {
    UncheckedCouldNotResolveImportException(CouldNotResolveImportException e) {
      super(e);
    }

    @Override
    public synchronized CouldNotResolveImportException getCause() {
      return (CouldNotResolveImportException) super.getCause();
    }
  }

  static TreeCopier<Inliner> copier(
      final Map<UVariableDecl, UExpression> arguments, Inliner inliner) {
    return new TreeCopier<Inliner>(inliner.maker()) {
      @Override
      public <T extends JCTree> T copy(T tree, Inliner inliner) {
        if (tree == null) {
          return null;
        }
        T result = super.copy(tree, inliner);
        if (result.toString().equals(tree.toString())) {
          return tree;
        } else {
          return result;
        }
      }

      @Override
      public JCTree visitIdentifier(IdentifierTree node, Inliner inliner) {
        if (node instanceof PlaceholderParamIdent) {
          try {
            return arguments.get(((PlaceholderParamIdent) node).param).inline(inliner);
          } catch (CouldNotResolveImportException e) {
            throw new UncheckedCouldNotResolveImportException(e);
          }
        } else {
          return super.visitIdentifier(node, inliner);
        }
      }
    };
  }

  @Override
  public JCExpression inline(Inliner inliner) throws CouldNotResolveImportException {
    /*
     * Copy the original source bound to the placeholder, except anywhere we matched a placeholder
     * parameter, replace that with the corresponding expression in this invocation.
     */
    try {
      return copier(arguments(), inliner)
          .copy(inliner.getBinding(placeholder().exprKey()), inliner);
    } catch (UncheckedCouldNotResolveImportException e) {
      throw e.getCause();
    }
  }

  @Override
  public Kind getKind() {
    return Kind.OTHER;
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitOther(this, data);
  }

  public boolean reverify(Unifier unifier) {
    return MoreObjects.firstNonNull(
        new PlaceholderVerificationVisitor(
                Collections2.transform(
                    placeholder().requiredParameters(), Functions.forMap(arguments())),
                arguments().values())
            .scan(unifier.getBinding(placeholder().exprKey()), unifier),
        true);
  }

  @Override
  protected Choice<Unifier> defaultAction(Tree node, Unifier unifier) {
    // for now we only match JCExpressions
    if (placeholder().returnType().equals(UPrimitiveType.VOID) || !(node instanceof JCExpression)) {
      return Choice.none();
    }
    final JCExpression expr = (JCExpression) node;

    PlaceholderVerificationVisitor verification =
        new PlaceholderVerificationVisitor(
            Collections2.transform(
                placeholder().requiredParameters(), Functions.forMap(arguments())),
            arguments().values());
    if (!verification.scan(node, unifier) || !verification.allRequiredMatched()) {
      return Choice.none();
    }

    /*
     * We copy the tree with a TreeCopier, replacing matches for the parameters with
     * PlaceholderParamIdents, and updating unifierHolder as we unify things, including forbidding
     * references to local variables, etc.
     */
    Choice<? extends PlaceholderUnificationVisitor.State<? extends JCExpression>> states =
        PlaceholderUnificationVisitor.create(TreeMaker.instance(unifier.getContext()), arguments())
            .unifyExpression(
                expr,
                PlaceholderUnificationVisitor.State.create(
                    List.<UVariableDecl>nil(), unifier, null));
    return states.thenOption(
        (PlaceholderUnificationVisitor.State<? extends JCExpression> state) -> {
          if (ImmutableSet.copyOf(state.seenParameters())
              .containsAll(placeholder().requiredParameters())) {
            Unifier resultUnifier = state.unifier();
            JCExpression prevBinding = resultUnifier.getBinding(placeholder().exprKey());
            if (prevBinding != null) {
              return prevBinding.toString().equals(state.result().toString())
                  ? Optional.of(resultUnifier)
                  : Optional.<Unifier>absent();
            }
            JCExpression result = state.result();
            if (!placeholder()
                .matcher()
                .matches(result, UMatches.makeVisitorState(expr, resultUnifier))) {
              return Optional.absent();
            }
            result.type = expr.type;
            resultUnifier.putBinding(placeholder().exprKey(), result);
            return Optional.of(resultUnifier);
          } else {
            return Optional.absent();
          }
        });
  }
}
