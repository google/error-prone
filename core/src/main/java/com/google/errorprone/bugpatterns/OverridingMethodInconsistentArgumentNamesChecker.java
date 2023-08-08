/*
 * Copyright 2023 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableBiMap.toImmutableBiMap;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.util.ASTHelpers.findSuperMethods;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;

import com.google.common.collect.ImmutableBiMap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.Optional;
import javax.lang.model.element.Name;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "Arguments of overriding method are inconsistent with overridden method.",
    severity = WARNING)
public class OverridingMethodInconsistentArgumentNamesChecker extends BugChecker
    implements MethodTreeMatcher {

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    MethodSymbol methodSymbol = getSymbol(methodTree);
    Optional<MethodSymbol> superMethod =
        findSuperMethods(methodSymbol, state.getTypes()).stream().findFirst();
    if (superMethod.isEmpty() || methodSymbol.isVarArgs() || superMethod.get().isVarArgs()) {
      return Description.NO_MATCH;
    }

    ImmutableBiMap<Name, Integer> params = getParams(methodSymbol);
    ImmutableBiMap<Name, Integer> superParams = getParams(superMethod.get());

    for (Name param : params.keySet()) {
      int position = params.get(param);
      if (!superParams.containsKey(param) || position == superParams.get(param)) {
        continue;
      }
      Name samePositionSuperParam = superParams.inverse().get(position);
      if (params.containsKey(samePositionSuperParam)) {
        return buildDescription(methodTree).setMessage(getDescription(superMethod.get())).build();
      }
    }

    return Description.NO_MATCH;
  }

  private static ImmutableBiMap<Name, Integer> getParams(MethodSymbol methodSymbol) {
    return range(0, methodSymbol.getParameters().size())
        .boxed()
        .collect(toImmutableBiMap(i -> methodSymbol.getParameters().get(i).name, i -> i));
  }

  /**
   * Provides a violation description with suggested parameter ordering.
   *
   * <p>We're not returning a suggested fix as re-ordering is not safe and might break the code.
   */
  public String getDescription(MethodSymbol methodSymbol) {
    return "The parameters of this method are inconsistent with the overridden method."
        + " A consistent order would be: "
        + getSuggestedSignature(methodSymbol);
  }

  private static String getSuggestedSignature(MethodSymbol methodSymbol) {
    return String.format(
        "%s(%s)", methodSymbol.getSimpleName(), getSuggestedParameters(methodSymbol));
  }

  private static String getSuggestedParameters(MethodSymbol methodSymbol) {
    return methodSymbol.getParameters().stream().map(p -> p.name).collect(joining(", "));
  }
}
