/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.tree.JCTree;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import javax.lang.model.type.TypeKind;

@BugPattern(name = "ArraysAsListPrimitiveArray",
    summary =
        "Arrays.asList does not autobox primitive arrays, as one might expect.",
    explanation =
        "Arrays.asList does not autobox primitive arrays, as one might expect. " +
        "If you intended to autobox the primitive array, use an asList method " +
        "from Guava that does autobox.  If you intended to create a singleton " +
        "list containing the primitive array, use Collections.singletonList to " +
        "make your intent clearer.",
    category = JDK, severity = ERROR, maturity = EXPERIMENTAL)
public class ArraysAsListPrimitiveArray extends BugChecker implements MethodInvocationTreeMatcher {
  private static final Matcher<MethodInvocationTree> ARRAYS_AS_LIST_SINGLE_ARRAY = Matchers.allOf(
      Matchers.methodSelect(Matchers.staticMethod("java.util.Arrays", "asList")),
      Matchers.argumentCount(1),
      Matchers.argument(1, Matchers.<ExpressionTree>isArrayType()));

  private static final Map<TypeKind, String> GUAVA_UTILS;

  static {
    Map<TypeKind, String> guavaUtils = new EnumMap<>(TypeKind.class);
    guavaUtils.put(TypeKind.BOOLEAN, "Booleans");
    guavaUtils.put(TypeKind.BYTE, "Bytes");
    guavaUtils.put(TypeKind.SHORT, "Shorts");
    guavaUtils.put(TypeKind.INT, "Ints");
    guavaUtils.put(TypeKind.LONG, "Longs");
    guavaUtils.put(TypeKind.CHAR, "Chars");
    guavaUtils.put(TypeKind.FLOAT, "Floats");
    guavaUtils.put(TypeKind.DOUBLE, "Doubles");
    GUAVA_UTILS = Collections.unmodifiableMap(guavaUtils);
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (ARRAYS_AS_LIST_SINGLE_ARRAY.matches(tree, state)) {
      JCTree array = (JCTree) tree.getArguments().get(0);
      Type componentType = ((ArrayType) array.type).getComponentType();
      String guavaUtils = GUAVA_UTILS.get(componentType.getKind());
      if (guavaUtils != null) {
        Fix fix = SuggestedFix.builder()
            .addImport("com.google.common.primitives." + guavaUtils)
            .replace(tree.getMethodSelect(), guavaUtils + ".asList")
            .build();
        return describeMatch(tree, fix);
      }
    }

    return Description.NO_MATCH;
  }
}

