/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.util.ASTHelpers.constValue;

import com.google.common.base.CharMatcher;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.LiteralTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.LiteralTree;
import com.sun.tools.javac.code.Type;
import java.math.BigDecimal;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "FloatingPointLiteralPrecision",
  category = JDK,
  summary = "Floating point literal loses precision",
  severity = WARNING,
  tags = StandardTags.STYLE,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class FloatingPointLiteralPrecision extends BugChecker implements LiteralTreeMatcher {
  @Override
  public Description matchLiteral(LiteralTree tree, VisitorState state) {
    Type type = ASTHelpers.getType(tree);
    if (type == null) {
      return NO_MATCH;
    }
    String suffix;
    BigDecimal value;
    switch (type.getKind()) {
      case DOUBLE:
        value = new BigDecimal(Double.toString(constValue(tree, Double.class)));
        suffix = "";
        break;
      case FLOAT:
        value = new BigDecimal(Float.toString(constValue(tree, Float.class)));
        suffix = "f";
        break;
      default:
        return NO_MATCH;
    }
    String source = state.getSourceForNode(tree);
    switch (source.charAt(source.length() - 1)) {
      case 'f':
      case 'F':
      case 'd':
      case 'D':
        source = source.substring(0, source.length() - 1);
        break;
      default: // fall out
    }
    source = CharMatcher.is('_').removeFrom(source);
    BigDecimal exact;
    try {
      exact = new BigDecimal(source);
    } catch (NumberFormatException e) {
      // BigDecimal doesn't support e.g. hex floats
      return NO_MATCH;
    }
    // Compare the actual and exact value, ignoring scale.
    // BigDecimal#equals returns false for e.g. `1.0` and `1.00`.
    if (exact.compareTo(value) == 0) {
      return NO_MATCH;
    }
    return describeMatch(tree, SuggestedFix.replace(tree, value + suffix));
  }
}
