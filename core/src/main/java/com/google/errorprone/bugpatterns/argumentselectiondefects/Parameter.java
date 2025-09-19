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

package com.google.errorprone.bugpatterns.argumentselectiondefects;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.errorprone.VisitorState;
import com.google.errorprone.names.NamingConventions;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.comp.Check;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import java.util.List;
import java.util.Optional;

/**
 * Represents either a formal or actual parameter and its position in the argument list.
 *
 * @author andrewrice@google.com (Andrew Rice)
 */
@AutoValue
abstract class Parameter {

  private static final ImmutableSet<String> METHODNAME_PREFIXES_TO_REMOVE =
      ImmutableSet.of("get", "set", "is");

  /** We use this placeholder to indicate a name which is a null literal. */
  @VisibleForTesting static final String NAME_NULL = "*NULL*";

  /** We use this placeholder to indicate a name which we couldn't get a canonical string for. */
  @VisibleForTesting static final String NAME_NOT_PRESENT = "*NOT_PRESENT*";

  abstract String name();

  abstract Type type();

  abstract int index();

  abstract String text();

  abstract Kind kind();

  abstract boolean constant();

  static ImmutableList<Parameter> createListFromVarSymbols(List<VarSymbol> varSymbols) {
    return Streams.mapWithIndex(
            varSymbols.stream(),
            (s, i) ->
                new AutoValue_Parameter(
                    s.getSimpleName().toString(),
                    s.asType(),
                    (int) i,
                    s.getSimpleName().toString(),
                    Kind.IDENTIFIER,
                    false))
        .collect(toImmutableList());
  }

  static ImmutableList<Parameter> createListFromExpressionTrees(List<? extends Tree> trees) {
    return Streams.mapWithIndex(
            trees.stream(),
            (t, i) ->
                new AutoValue_Parameter(
                    getArgumentName(t),
                    Optional.ofNullable(
                            t instanceof ExpressionTree expressionTree
                                ? ASTHelpers.getResultType(expressionTree)
                                : ASTHelpers.getType(t))
                        .orElse(Type.noType),
                    (int) i,
                    t.toString(),
                    t.getKind(),
                    ASTHelpers.constValue(t) != null))
        .collect(toImmutableList());
  }

  static ImmutableList<Parameter> createListFromVariableTrees(List<? extends VariableTree> trees) {
    return createListFromVarSymbols(
        trees.stream().map(ASTHelpers::getSymbol).collect(toImmutableList()));
  }

  /**
   * Return true if this parameter is assignable to the target parameter. This will consider
   * subclassing, autoboxing and null.
   */
  boolean isAssignableTo(Parameter target, VisitorState state) {
    if (state.getTypes().isSameType(type(), Type.noType)
        || state.getTypes().isSameType(target.type(), Type.noType)) {
      return false;
    }
    try {
      return state.getTypes().isAssignable(type(), target.type());
    } catch (CompletionFailure e) {
      // Report completion errors to avoid e.g. https://github.com/bazelbuild/bazel/issues/4105
      Check.instance(state.context)
          .completionError((DiagnosticPosition) state.getPath().getLeaf(), e);
      return false;
    }
  }

  boolean isNullLiteral() {
    return name().equals(NAME_NULL);
  }

  boolean isUnknownName() {
    return name().equals(NAME_NOT_PRESENT);
  }

  private static String getClassName(ClassSymbol s) {
    if (s.isAnonymous()) {
      return s.getSuperclass().tsym.getSimpleName().toString();
    } else {
      return s.getSimpleName().toString();
    }
  }

  /**
   * Extract the name from an argument.
   *
   * <p>
   *
   * <ul>
   *   <li>IdentifierTree - if the identifier is 'this' then use the name of the enclosing class,
   *       otherwise use the name of the identifier
   *   <li>MemberSelectTree - the name of its identifier
   *   <li>NewClassTree - the name of the class being constructed
   *   <li>Null literal - a wildcard name
   *   <li>MethodInvocationTree - use the method name stripping off 'get', 'set', 'is' prefix. If
   *       this results in an empty name then recursively search the receiver
   * </ul>
   *
   * All other trees (including literals other than Null literal) do not have a name and this method
   * will return the marker for an unknown name.
   */
  @VisibleForTesting
  static String getArgumentName(Tree tree) {
    return switch (tree) {
      case VariableTree variableTree -> variableTree.getName().toString();
      case MemberSelectTree memberSelectTree -> memberSelectTree.getIdentifier().toString();
      // null could match anything pretty well
      case LiteralTree literalTree when tree.getKind().equals(Kind.NULL_LITERAL) -> NAME_NULL;
      case IdentifierTree identifierTree -> {
        IdentifierTree idTree = (IdentifierTree) tree;
        if (idTree.getName().contentEquals("this")) {
          // for the 'this' keyword the argument name is the name of the object's class
          Symbol sym = ASTHelpers.getSymbol(idTree);
          yield sym != null ? getClassName(ASTHelpers.enclosingClass(sym)) : NAME_NOT_PRESENT;
        } else {
          // if we have a variable, just extract its name
          yield idTree.getName().toString();
        }
      }
      case MethodInvocationTree methodInvocationTree -> {
        MethodSymbol methodSym = ASTHelpers.getSymbol(methodInvocationTree);
        String name = methodSym.getSimpleName().toString();
        ImmutableList<String> terms = NamingConventions.splitToLowercaseTerms(name);
        String firstTerm = Iterables.getFirst(terms, null);
        if (METHODNAME_PREFIXES_TO_REMOVE.contains(firstTerm)) {
          if (terms.size() == 1) {
            ExpressionTree receiver = ASTHelpers.getReceiver(methodInvocationTree);
            if (receiver == null) {
              yield getClassName(ASTHelpers.enclosingClass(methodSym));
            }
            // recursively try to get a name from the receiver
            yield getArgumentName(receiver);
          } else {
            yield name.substring(firstTerm.length());
          }
        } else {
          yield name;
        }
      }
      case NewClassTree newClassTree -> {
        MethodSymbol constructorSym = ASTHelpers.getSymbol(newClassTree);
        yield constructorSym.owner != null
            ? getClassName((ClassSymbol) constructorSym.owner)
            : NAME_NOT_PRESENT;
      }
      default -> NAME_NOT_PRESENT;
    };
  }
}
