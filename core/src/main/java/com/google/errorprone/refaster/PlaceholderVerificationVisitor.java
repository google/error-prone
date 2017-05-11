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

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Cheap visitor to verify that a tree or set of trees could conceivably match a placeholder, by
 * potentially matching at least all of the required expressions, and not matching forbidden
 * expressions, i.e. other variables known to Refaster.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
final class PlaceholderVerificationVisitor extends TreeScanner<Boolean, Unifier> {
  private final Collection<UExpression> unmatched;
  private final ImmutableCollection<UExpression> allowed;

  /**
   * @param required UExpressions that must potentially match at least one expression in the tested
   *     code.
   * @param allowed UExpressions that are explicitly allowed, and excepted from the ban on
   *     references to known variables
   */
  PlaceholderVerificationVisitor(
      Collection<? extends UExpression> required, Collection<? extends UExpression> allowed) {
    this.unmatched = new LinkedList<>(required);
    this.allowed = ImmutableList.copyOf(allowed);
    assert this.allowed.containsAll(unmatched);
  }

  public boolean allRequiredMatched() {
    return unmatched.isEmpty();
  }

  private boolean couldUnify(UExpression expr, Tree tree, Unifier unifier) {
    return expr.unify(tree, unifier.fork()).first().isPresent();
  }

  @Override
  public Boolean scan(Tree node, Unifier unifier) {
    Iterator<UExpression> iterator = unmatched.iterator();
    while (iterator.hasNext()) {
      if (couldUnify(iterator.next(), node, unifier)) {
        iterator.remove();
        return true;
      }
    }
    for (UExpression expr : allowed) {
      if (couldUnify(expr, node, unifier)) {
        return true;
      }
    }
    if (node instanceof JCExpression) {
      JCExpression expr = (JCExpression) node;
      for (UFreeIdent.Key key :
          Iterables.filter(unifier.getBindings().keySet(), UFreeIdent.Key.class)) {
        JCExpression keyBinding = unifier.getBinding(key);
        if (PlaceholderUnificationVisitor.equivalentExprs(unifier, expr, keyBinding)) {
          return false;
        }
      }
    }
    return firstNonNull(super.scan(node, unifier), true);
  }

  @Override
  public Boolean visitIdentifier(IdentifierTree node, Unifier unifier) {
    for (LocalVarBinding localBinding :
        Iterables.filter(unifier.getBindings().values(), LocalVarBinding.class)) {
      if (localBinding.getSymbol().equals(ASTHelpers.getSymbol(node))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Boolean reduce(Boolean r1, Boolean r2) {
    return firstNonNull(r1, true) && firstNonNull(r2, true);
  }
}
