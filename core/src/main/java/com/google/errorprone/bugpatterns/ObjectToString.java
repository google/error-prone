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
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.predicates.TypePredicate;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import java.util.Set;

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
  providesFix = ProvidesFix.NO_FIX
)
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
    // We explore the superclasses of the receiver type as well as the interfaces it
    // implements and we collect all overrides of java.lang.Object.toString(). If one of those
    // overrides is present, then we don't flag it.
    Types types = state.getTypes();
    Set<MethodSymbol> overridesOfToString =
        ASTHelpers.findMatchingMethods(
            state.getName("toString"),
            methodSymbol -> isToString(methodSymbol, state),
            type,
            types);

    // only has Object.toString()
    if (overridesOfToString.size() == 1) {
      return true;
    }
    return false;
  }

  private static boolean isToString(MethodSymbol methodSymbol, VisitorState state) {
    return !methodSymbol.isStatic()
        && ((methodSymbol.flags() & Flags.SYNTHETIC) == 0)
        && state.getTypes().isSameType(methodSymbol.getReturnType(), state.getSymtab().stringType)
        && methodSymbol.getParameters().isEmpty();
  }

  @Override
  protected TypePredicate typePredicate() {
    return ObjectToString::finalNoOverrides;
  }

  @Override
  protected Optional<String> descriptionMessageForDefaultMatch(Type type) {
    String format =
        "%1$s is final and does not override Object.toString, converting it to a string"
            + " will print its identity (e.g. `%1$s@ 4488aabb`) instead of useful information.";
    return Optional.of(String.format(format, type.toString()));
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
