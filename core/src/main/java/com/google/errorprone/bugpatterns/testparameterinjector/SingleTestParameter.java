/*
 * Copyright 2026 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.testparameterinjector;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.bugpatterns.testparameterinjector.TestParameterInjectorUtils.TEST_PARAMETER;
import static com.google.errorprone.bugpatterns.testparameterinjector.TestParameterInjectorUtils.extractTestParametersAnnotations;
import static com.google.errorprone.bugpatterns.testparameterinjector.TestParameterInjectorUtils.isExhaustiveValues;
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.testparameterinjector.TestParameterInjectorUtils.TestParametersAnnotations;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "@TestParameter on the parameter is simpler than @TestParameters if there's only one test"
            + " parameter.",
    severity = SUGGESTION)
public final class SingleTestParameter extends BugChecker implements MethodTreeMatcher {

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    if (methodTree.getParameters().size() != 1) {
      return NO_MATCH;
    }
    VariableTree parameter = getOnlyElement(methodTree.getParameters());
    if (hasAnnotation(getSymbol(parameter), TEST_PARAMETER, state)) {
      return NO_MATCH;
    }
    Type paramType = getType(parameter);
    if (paramType == null || !paramType.getTypeArguments().isEmpty()) {
      return NO_MATCH;
    }

    TestParametersAnnotations annotations =
        extractTestParametersAnnotations(methodTree, state).orElse(null);
    if (annotations == null || annotations.values().isEmpty()) {
      return NO_MATCH;
    }

    ImmutableSet<String> parameterNames = ImmutableSet.of(parameter.getName().toString());
    List<String> values = new ArrayList<>(annotations.values().size());
    for (String yaml : annotations.values()) {
      Object parsed = parseYamlStringToObject(yaml);
      if (!(parsed instanceof Map<?, ?> map) || !map.keySet().equals(parameterNames)) {
        return NO_MATCH;
      }
      Object value = getOnlyElement(map.values());
      if (value instanceof Map<?, ?>
          || value instanceof Iterable<?>
          || Objects.equals(value, "null")) {
        return NO_MATCH;
      }
      values.add(stringify(value));
    }

    SuggestedFix.Builder fix = SuggestedFix.builder();
    for (AnnotationTree anno : annotations.annotationsToRemove()) {
      fix.delete(anno);
    }

    String testParameterName = qualifyType(state, fix, TEST_PARAMETER);
    String testParameterAnnotation =
        isExhaustiveValues(paramType, values, state)
            ? String.format("@%s ", testParameterName)
            : String.format(
                "@%s({%s}) ",
                testParameterName,
                values.stream().map(state::getConstantExpression).collect(joining(", ")));

    return describeMatch(methodTree, fix.prefixWith(parameter, testParameterAnnotation).build());
  }

  private static @Nullable Object parseYamlStringToObject(String yamlString) {
    try {
      return newYaml().load(yamlString);
    } catch (YAMLException e) {
      return null;
    }
  }

  private static String stringify(@Nullable Object obj) {
    return (obj instanceof String s) ? s : newYaml().dump(obj).stripTrailing();
  }

  // SnakeYAML's `Yaml` is not thread-safe, so create a fresh instance per operation.
  private static Yaml newYaml() {
    LoaderOptions loaderOptions = new LoaderOptions();
    loaderOptions.setAllowDuplicateKeys(false);
    return new Yaml(new SafeConstructor(loaderOptions));
  }
}
