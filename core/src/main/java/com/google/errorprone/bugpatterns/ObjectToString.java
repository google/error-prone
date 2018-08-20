/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.predicates.TypePredicate;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.util.Names;

/**
 * Warns against calling toString() on Objects which don't have toString() method overridden and
 * won't produce meaningful output.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    name = "ObjectToString",
    summary =
        "Calling toString on Objects that don't override toString() doesn't"
            + " provide useful information",
    category = JDK,
    severity = WARNING,
    providesFix = ProvidesFix.NO_FIX)
public class ObjectToString extends AbstractToString {

  private static boolean finalNoOverrides(Type type, VisitorState state) {
    if (type == null) {
      return false;
    }
    // We don't flag use of toString() on non-final objects because sub classes might have a
    // meaningful toString() override.
    if (!type.isFinal()) {
      return false;
    }
    Types types = state.getTypes();
    Names names = Names.instance(state.context);
    // find Object.toString
    MethodSymbol toString =
        (MethodSymbol) state.getSymtab().objectType.tsym.members().findFirst(names.toString);
    // We explore the superclasses of the receiver type as well as the interfaces it
    // implements and we collect all overrides of java.lang.Object.toString(). If one of those
    // overrides is present, then we don't flag it.
    return Iterables.isEmpty(
        types
            .membersClosure(type, /* skipInterface= */ false)
            .getSymbolsByName(
                names.toString,
                m ->
                    m != toString
                        && m.overrides(toString, type.tsym, types, /* checkResult= */ false)));
  }

  @Override
  protected TypePredicate typePredicate() {
    return ObjectToString::finalNoOverrides;
  }

  @Override
  protected Optional<String> descriptionMessageForDefaultMatch(Type type, VisitorState state) {
    String format =
        "%1$s is final and does not override Object.toString, so converting it to a string"
            + " will print its identity (e.g. `%2$s@ 4488aabb`) instead of useful information.";
    return Optional.of(
        String.format(
            format,
            SuggestedFixes.prettyType(state, /* fix= */ null, type),
            type.tsym.getSimpleName()));
  }

  @Override
  protected Optional<Fix> implicitToStringFix(ExpressionTree tree, VisitorState state) {
    return Optional.absent();
  }

  @Override
  protected Optional<Fix> toStringFix(Tree parent, ExpressionTree tree, VisitorState state) {
    return Optional.absent();
  }
}
