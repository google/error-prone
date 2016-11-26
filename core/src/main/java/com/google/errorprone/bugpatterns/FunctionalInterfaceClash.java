/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.Signatures;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.util.List;
import java.util.ArrayList;
import java.util.Collection;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "FunctionalInterfaceClash",
  summary = "Overloads will be ambiguous when passing lambda arguments",
  category = JDK,
  severity = WARNING
)
public class FunctionalInterfaceClash extends BugChecker implements ClassTreeMatcher {
  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    ClassSymbol origin = getSymbol(tree);
    Types types = state.getTypes();
    // collect declared and inherited methods whose signature contains a functional interface
    Multimap<String, MethodSymbol> methods = HashMultimap.create();
    for (Symbol sym : types.membersClosure(getType(tree), /*skipInterface=*/ false).getSymbols()) {
      if (!(sym instanceof MethodSymbol)) {
        continue;
      }
      MethodSymbol msym = (MethodSymbol) sym;
      if (msym.getParameters().stream().noneMatch(p -> maybeFunctionalInterface(p.type, types))) {
        continue;
      }
      if (msym.isConstructor() && !msym.owner.equals(origin)) {
        continue;
      }
      methods.put(functionalInterfaceSignature(state, msym), msym);
    }
    // check if any declared members clash with another declared or inherited member
    // (don't report clashes between inherited members)
    for (Tree member : tree.getMembers()) {
      if (!(member instanceof MethodTree)) {
        continue;
      }
      MethodSymbol msym = getSymbol((MethodTree) member);
      if (msym.getParameters().stream().noneMatch(p -> maybeFunctionalInterface(p.type, types))) {
        continue;
      }
      Collection<MethodSymbol> clash =
          new ArrayList<>(methods.removeAll(functionalInterfaceSignature(state, msym)));
      clash.remove(msym);
      // ignore inherited methods that are overridden in the original class
      clash.removeIf(m -> msym.overrides(m, origin, types, false));
      if (!clash.isEmpty()) {
        String message =
            "When passing lambda arguments to this function, callers will need a cast to"
                + " disambiguate with: "
                + clash
                    .stream()
                    .map(m -> Signatures.prettyMethodSignature(origin, m))
                    .collect(joining("\n    "));
        state.reportMatch(buildDescription(member).setMessage(message).build());
      }
    }
    return NO_MATCH;
  }

  /**
   * A string representation of a method descriptor, where all parameters whose type is a functional
   * interface are "erased" to the interface's function type. For example, `foo(Supplier<String>)`
   * is represented as `foo(()->Ljava/lang/String;)`.
   */
  private static String functionalInterfaceSignature(VisitorState state, MethodSymbol msym) {
    return String.format(
        "%s(%s)",
        msym.getSimpleName(),
        msym.getParameters()
            .stream()
            .map(p -> functionalInterfaceSignature(state, p.type))
            .collect(joining(",")));
  }

  private static String functionalInterfaceSignature(VisitorState state, Type type) {
    Types types = state.getTypes();
    if (!maybeFunctionalInterface(type, types)) {
      return Signatures.descriptor(type, types);
    }
    Type descriptorType = types.findDescriptorType(type);
    List<Type> fiparams = descriptorType.getParameterTypes();
    // Implicitly typed block-statement-bodied lambdas are potentially compatible with
    // void-returning and value-returning functional interface types, so we don't consider return
    // types in general. The except is nullary functional interfaces, since the lambda parameters
    // will never be implicitly typed.
    String result =
        fiparams.isEmpty() ? Signatures.descriptor(descriptorType.getReturnType(), types) : "_";
    return String.format(
        "(%s)->%s",
        fiparams.stream().map(t -> Signatures.descriptor(t, types)).collect(joining(",")), result);
  }

  private static boolean maybeFunctionalInterface(Type type, Types types) {
    try {
      return types.isFunctionalInterface(type);
    } catch (CompletionFailure e) {
      return false;
    }
  }
}
