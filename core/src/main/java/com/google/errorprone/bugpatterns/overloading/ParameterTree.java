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

package com.google.errorprone.bugpatterns.overloading;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Flags;
import java.util.Set;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;

/**
 * A simpler version of standard {@link VariableTree} used to distinguish between real variable
 * declarations and method parameters.
 *
 * <p>{@link VariableTree} is in a sense "too rich" to represent method parameters. For example, it
 * is allowed to have an initializer or arbitrary modifiers. This class is a much simpler version of
 * variable tree used to distinguish actual real {@link VariableTree} (variable declarations) from
 * simple method parameters. During construction it is validated that the underlying {@link
 * VariableTree} can really be used as method parameter.
 *
 * <p>It also provides slightly more sensible (for our purposes) {@link Object#toString()}
 * implementation.
 *
 * @author hanuszczak@google.com (Łukasz Hanuszczak)
 */
@AutoValue
abstract class ParameterTree {

  public abstract Name getName();

  public abstract Tree getType();

  public abstract boolean isVarArgs();

  public static ParameterTree create(VariableTree variableTree) {
    Preconditions.checkArgument(isValidParameterTree(variableTree));

    Name name = variableTree.getName();
    Tree type = variableTree.getType();
    boolean isVarargs = isVariableTreeVarArgs(variableTree);
    return new AutoValue_ParameterTree(name, type, isVarargs);
  }

  private static boolean isValidParameterTree(VariableTree variableTree) {
    // A valid parameter has no initializer.
    if (variableTree.getInitializer() != null) {
      return false;
    }

    // A valid parameter either has no modifiers or has only `final` keyword.
    Set<Modifier> flags = variableTree.getModifiers().getFlags();
    return flags.isEmpty() || (flags.size() == 1 && flags.contains(Modifier.FINAL));
  }

  @Override
  public final String toString() {
    String type = getType().toString();
    String name = getName().toString();

    // Hacky fix to improve pretty-printing of varargs (otherwise they are printed as Type[]).
    if (isVarArgs()) {
      type = type.substring(0, type.length() - 2) + "...";
    }

    return type + " " + name;
  }

  // TODO(hanuszczak): This should probably be moved to ASTHelpers.
  private static boolean isVariableTreeVarArgs(VariableTree variableTree) {
    return (ASTHelpers.getSymbol(variableTree).flags() & Flags.VARARGS) != 0;
  }
}
