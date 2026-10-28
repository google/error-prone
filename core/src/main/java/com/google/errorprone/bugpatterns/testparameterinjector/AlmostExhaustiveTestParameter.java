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
import static com.google.errorprone.bugpatterns.testparameterinjector.TestParameterInjectorUtils.getExplicitTestParameterValues;
import static com.google.errorprone.bugpatterns.testparameterinjector.TestParameterInjectorUtils.implicitTestParameterAnnotation;
import static com.google.errorprone.bugpatterns.testparameterinjector.TestParameterInjectorUtils.isEnumType;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.JUnitMatchers.JUNIT4_TEST_ANNOTATION;
import static com.google.errorprone.util.ASTHelpers.enumValues;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import org.jspecify.annotations.Nullable;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Prefer an exhaustive @TestParameter with an assumption to filter out specific enum values,"
            + " so newly added enum values are automatically tested",
    severity = SUGGESTION)
public final class AlmostExhaustiveTestParameter extends BugChecker implements MethodTreeMatcher {

  // TODO(kak): Consider lowering this threshold (e.g., to 3).
  private static final int MIN_EXPLICIT_VALUES = 4;

  private static final Supplier<@Nullable TypeElement> TRUTH_JUNIT =
      VisitorState.memoize(
          state -> state.getElements().getTypeElement("com.google.common.truth.TruthJUnit"));

  // TODO(kak): Consider supporting fields annotated with @TestParameter. However, fixing fields is
  // more complex because it requires inserting the assume() statement(s) into a @Before method.

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    BlockTree body = tree.getBody();
    if (body == null
        || tree.getParameters().isEmpty()
        || !hasAnnotation(tree, JUNIT4_TEST_ANNOTATION, state)) {
      return NO_MATCH;
    }

    SuggestedFix.Builder fix = SuggestedFix.builder();
    List<String> assumptions = new ArrayList<>();
    List<AnnotationTree> matchedAnnotations = new ArrayList<>();
    for (VariableTree param : tree.getParameters()) {
      Type type = getType(param);
      if (!isEnumType(type) || isSuppressed(param, state)) {
        continue;
      }
      for (AnnotationTree annotation : param.getModifiers().getAnnotations()) {
        ImmutableList<String> explicitValueList = getExplicitTestParameterValues(annotation, state);
        if (explicitValueList.size() < MIN_EXPLICIT_VALUES) {
          continue;
        }
        ImmutableSet<String> explicitValues = ImmutableSet.copyOf(explicitValueList);
        if (explicitValues.size() != explicitValueList.size()) {
          continue;
        }
        Set<String> allValues = enumValues(type.tsym);
        if (!allValues.containsAll(explicitValues)) {
          continue;
        }
        Set<String> missing = Sets.difference(allValues, explicitValues);
        if (!shouldMatchMissingValues(missing)) {
          continue;
        }
        matchedAnnotations.add(annotation);
        fix.replace(annotation, implicitTestParameterAnnotation(annotation, state));
        assumptions.add(
            buildAssumeStatement(missing, param.getName().toString(), type, state, fix));
      }
    }
    if (matchedAnnotations.isEmpty()) {
      return NO_MATCH;
    }

    int insertPos = getStartPosition(body) + 1;
    fix.replace(insertPos, insertPos, "\n" + String.join("\n", assumptions));
    return describeMatch(matchedAnnotations.getFirst(), fix.build());
  }

  private static boolean shouldMatchMissingValues(Set<String> missing) {
    return switch (missing.size()) {
      case 1 -> true;
      // Proto3 open enums synthesize an extra UNRECOGNIZED constant alongside the declared values
      // (which typically include a default *_UNSPECIFIED or *_UNKNOWN sentinel at 0), so tests
      // covering all valid declared values commonly omit both that sentinel and UNRECOGNIZED.
      case 2 -> missing.contains("UNRECOGNIZED");
      default -> false;
    };
  }

  private static String buildAssumeStatement(
      Set<String> missing,
      String paramName,
      Type enumType,
      VisitorState state,
      SuggestedFix.Builder fix) {
    String enumName = SuggestedFixes.qualifyType(state, fix, enumType);
    if (TRUTH_JUNIT.get(state) == null) {
      String assumeFalse =
          SuggestedFixes.qualifyStaticImport("org.junit.Assume.assumeFalse", fix, state);
      String condition =
          missing.stream()
              .map(v -> String.format("%s == %s.%s", paramName, enumName, v))
              .collect(joining(" || "));
      return String.format("%s(%s);", assumeFalse, condition);
    }
    String assumeMethod =
        SuggestedFixes.qualifyStaticImport("com.google.common.truth.TruthJUnit.assume", fix, state);
    String subject = String.format("%s().that(%s)", assumeMethod, paramName);
    if (missing.size() == 1) {
      return String.format("%s.isNotEqualTo(%s.%s);", subject, enumName, getOnlyElement(missing));
    }
    return String.format(
        "%s.isNoneOf(%s);",
        subject, missing.stream().map(v -> enumName + "." + v).collect(joining(", ")));
  }
}
