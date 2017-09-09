/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.LiteralTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import java.util.regex.Pattern;

/**
 * Matcher for a <code>long</code> literal with a lower-case ell for a suffix (e.g. <code>234l
 * </code>) rather than the more readable upper-case ell (e.g. <code>234L</code>).
 *
 * @author Simon Nickerson (sjnickerson@google.com)
 */
@BugPattern(
  name = "LongLiteralLowerCaseSuffix",
  summary = "Prefer 'L' to 'l' for the suffix to long literals",
  explanation =
      "A long literal can have a suffix of 'L' or 'l', but the former is less "
          + "likely to be confused with a '1' in most fonts.",
  category = JDK,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class LongLiteralLowerCaseSuffix extends BugChecker implements LiteralTreeMatcher {

  private static final Matcher<LiteralTree> matcher =
      new Matcher<LiteralTree>() {
        @Override
        public boolean matches(LiteralTree literalTree, VisitorState state) {
          if (literalTree.getKind() == Kind.LONG_LITERAL) {
            // The javac AST doesn't seem to record whether the suffix is present, or whether it's
            // an 'l' or 'L'. We have to look at the original source
            String longLiteral = getLongLiteral(literalTree, state);
            return longLiteral != null && longLiteral.endsWith("l");
          } else {
            return false;
          }
        }
      };

  // Doesn't need to be strict, just shouldn't read past the end
  // of the literal.
  private static final Pattern LONG_LITERAL_PATTERN =
      Pattern.compile("-? *(0[bBxX]?)?[0-9a-fA-F_]+[lL]?");

  /**
   * Extracts the long literal corresponding to a given {@link LiteralTree} node from the source
   * code as a string. Returns null if the source code is not available.
   */
  private static String getLongLiteral(LiteralTree literalTree, VisitorState state) {
    JCLiteral longLiteral = (JCLiteral) literalTree;
    CharSequence sourceFile = state.getSourceCode();
    if (sourceFile == null) {
      return null;
    }
    int start = longLiteral.getStartPosition();
    java.util.regex.Matcher matcher =
        LONG_LITERAL_PATTERN.matcher(sourceFile.subSequence(start, sourceFile.length()));
    if (matcher.lookingAt()) {
      return matcher.group();
    }
    return null;
  }

  @Override
  public Description matchLiteral(LiteralTree literalTree, VisitorState state) {
    if (!matcher.matches(literalTree, state)) {
      return Description.NO_MATCH;
    }
    StringBuilder longLiteral = new StringBuilder(getLongLiteral(literalTree, state));
    longLiteral.setCharAt(longLiteral.length() - 1, 'L');
    Fix fix = SuggestedFix.replace(literalTree, longLiteral.toString());
    return describeMatch(literalTree, fix);
  }
}
