/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.argumentselectiondefects;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.List;

/**
 * Holds information about the method invocation (or new class construction) that we are processing.
 *
 * @author andrewrice@google.com (Andrew Rice)
 */
@AutoValue
abstract class InvocationInfo {

  abstract Tree tree();

  abstract MethodSymbol symbol();

  abstract ImmutableList<? extends ExpressionTree> actualParameters();

  abstract ImmutableList<VarSymbol> formalParameters();

  abstract VisitorState state();

  static InvocationInfo createFromMethodInvocation(
      MethodInvocationTree tree, MethodSymbol symbol, VisitorState state) {
    return new AutoValue_InvocationInfo(
        tree,
        symbol,
        ImmutableList.copyOf(tree.getArguments()),
        getFormalParametersWithoutVarArgs(symbol),
        state);
  }

  static InvocationInfo createFromNewClass(
      NewClassTree tree, MethodSymbol symbol, VisitorState state) {
    return new AutoValue_InvocationInfo(
        tree,
        symbol,
        ImmutableList.copyOf(tree.getArguments()),
        getFormalParametersWithoutVarArgs(symbol),
        state);
  }

  private static ImmutableList<VarSymbol> getFormalParametersWithoutVarArgs(
      MethodSymbol invokedMethodSymbol) {
    List<VarSymbol> formalParameters = invokedMethodSymbol.getParameters();

    /* javac can get argument names from debugging symbols if they are not available from
    other sources. When it does this for an inner class sometimes it returns the implicit this
    pointer for the outer class as the first name (but not the first type). If we see this, then
    just abort */
    if (!formalParameters.isEmpty()
        && formalParameters.get(0).getSimpleName().toString().matches("this\\$[0-9]+")) {
      return ImmutableList.of();
    }

    /* If we have a varargs method then just ignore the final parameter and trailing actual
    parameters */
    int size =
        invokedMethodSymbol.isVarArgs() ? formalParameters.size() - 1 : formalParameters.size();

    return ImmutableList.copyOf(formalParameters.subList(0, size));
  }
}
