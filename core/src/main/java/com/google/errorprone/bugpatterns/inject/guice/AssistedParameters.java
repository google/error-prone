/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.inject.guice;

import static com.google.errorprone.BugPattern.Category.GUICE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.InjectMatchers.ASSISTED_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.ASSISTED_INJECT_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.hasInjectAnnotation;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.methodHasParameters;
import static com.google.errorprone.matchers.Matchers.methodIsConstructor;

import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.ChildMultiMatcher.MatchType;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.MultiMatcher;
import com.google.errorprone.matchers.MultiMatcher.MultiMatchResult;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Attribute.Compound;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.lang.model.element.TypeElement;

/** @author sgoldfeder@google.com (Steven Goldfeder) */
@BugPattern(
  name = "GuiceAssistedParameters",
  summary =
      "A constructor cannot have two @Assisted parameters of the same type unless they are "
          + "disambiguated with named @Assisted annotations.",
  explanation =
      "See https://google.github.io/guice/api-docs/latest/javadoc/com/google/inject/assistedinject/FactoryModuleBuilder.html",
  category = GUICE,
  severity = ERROR
)
public class AssistedParameters extends BugChecker implements MethodTreeMatcher {

  private static final Matcher<MethodTree> IS_CONSTRUCTOR_WITH_INJECT_OR_ASSISTED =
      allOf(
          methodIsConstructor(),
          anyOf(hasInjectAnnotation(), hasAnnotation(ASSISTED_INJECT_ANNOTATION)));

  private static final MultiMatcher<MethodTree, VariableTree> ASSISTED_PARAMETER_MATCHER =
      methodHasParameters(MatchType.AT_LEAST_ONE, Matchers.hasAnnotation(ASSISTED_ANNOTATION));

  private static final Function<VariableTree, String> VALUE_FROM_ASSISTED_ANNOTATION =
      new Function<VariableTree, String>() {
        @Override
        public String apply(VariableTree variableTree) {
          for (Compound c : ASTHelpers.getSymbol(variableTree).getAnnotationMirrors()) {
            if (((TypeElement) c.getAnnotationType().asElement())
                .getQualifiedName()
                .contentEquals(ASSISTED_ANNOTATION)) {
              // Assisted only has 'value', and value can only contain 1 element.
              Collection<Attribute> valueEntries = c.getElementValues().values();
              if (!valueEntries.isEmpty()) {
                return Iterables.getOnlyElement(valueEntries).getValue().toString();
              }
            }
          }
          return "";
        }
      };

  @Override
  public final Description matchMethod(MethodTree constructor, final VisitorState state) {
    if (!IS_CONSTRUCTOR_WITH_INJECT_OR_ASSISTED.matches(constructor, state)) {
      return Description.NO_MATCH;
    }

    // Gather @Assisted parameters, partition by type
    MultiMatchResult<VariableTree> assistedParameters =
        ASSISTED_PARAMETER_MATCHER.multiMatchResult(constructor, state);
    if (!assistedParameters.matches()) {
      return Description.NO_MATCH;
    }

    Multimap<Type, VariableTree> parametersByType =
        partitionParametersByType(assistedParameters.matchingNodes(), state);

    // If there's more than one parameter with the same type, they could conflict unless their
    // @Assisted values are different.
    List<ConflictResult> conflicts = new ArrayList<>();
    for (Map.Entry<Type, Collection<VariableTree>> typeAndParameters :
        parametersByType.asMap().entrySet()) {
      Collection<VariableTree> parametersForThisType = typeAndParameters.getValue();
      if (parametersForThisType.size() < 2) {
        continue;
      }

      // Gather the @Assisted value from each parameter. If any value is repeated amongst the
      // parameters in this type, it's a compile error.
      ImmutableListMultimap<String, VariableTree> keyForAssistedVariable =
          Multimaps.index(parametersForThisType, VALUE_FROM_ASSISTED_ANNOTATION);

      for (Entry<String, List<VariableTree>> assistedValueToParameters :
          Multimaps.asMap(keyForAssistedVariable).entrySet()) {
        if (assistedValueToParameters.getValue().size() > 1) {
          conflicts.add(
              ConflictResult.create(
                  typeAndParameters.getKey(),
                  assistedValueToParameters.getKey(),
                  assistedValueToParameters.getValue()));
        }
      }
    }

    if (conflicts.isEmpty()) {
      return Description.NO_MATCH;
    }

    return buildDescription(constructor).setMessage(buildErrorMessage(conflicts)).build();
  }

  private String buildErrorMessage(List<ConflictResult> conflicts) {
    StringBuilder sb =
        new StringBuilder(
            " Assisted parameters of the same type need to have distinct values for the @Assisted"
                + " annotation. There are conflicts between the annotations on this constructor:");

    for (ConflictResult conflict : conflicts) {
      sb.append("\n").append(conflict.type());

      if (!conflict.value().isEmpty()) {
        sb.append(", @Assisted(\"").append(conflict.value()).append("\")");
      }
      sb.append(": ");

      List<String> simpleParameterNames =
          Lists.transform(
              conflict.parameters(),
              new Function<VariableTree, String>() {
                @Override
                public String apply(VariableTree variableTree) {
                  return variableTree.getName().toString();
                }
              });
      Joiner.on(", ").appendTo(sb, simpleParameterNames);
    }

    return sb.toString();
  }

  @AutoValue
  abstract static class ConflictResult {
    abstract Type type();

    abstract String value();

    abstract List<VariableTree> parameters();

    static ConflictResult create(Type t, String v, List<VariableTree> p) {
      return new AutoValue_AssistedParameters_ConflictResult(t, v, p);
    }
  }

  // Since Type doesn't have strong equality semantics, we have to use Types.isSameType to
  // determine which parameters are conflicting with each other.
  private Multimap<Type, VariableTree> partitionParametersByType(
      List<VariableTree> parameters, VisitorState state) {

    Types types = state.getTypes();
    Multimap<Type, VariableTree> multimap = LinkedListMultimap.create();

    variables:
    for (VariableTree node : parameters) {
      // Normalize Integer => int
      Type type = types.unboxedTypeOrType(ASTHelpers.getType(node));
      for (Type existingType : multimap.keySet()) {
        if (types.isSameType(existingType, type)) {
          multimap.put(existingType, node);
          continue variables;
        }
      }

      // A new type for the map.
      multimap.put(type, node);
    }

    return multimap;
  }
}
