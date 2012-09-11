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
import static com.google.errorprone.BugPattern.MaturityLevel.ON_BY_DEFAULT;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.tree.JCTree.JCLiteral;

import java.io.IOException;

/**
 * Matcher for a <code>long</code> literal with a lower-case ell for a suffix (e.g.
 * <code>234l</code>) rather than the more readable upper-case ell (e.g. <code>234L</code>).
 * 
 * @author Simon Nickerson (sjnickerson@google.com)
 */
@BugPattern(name = "LongLiteralLowerCaseSuffix",
    summary = "Prefer 'L' to 'l' for the suffix to long literals",
    explanation = "A long literal can have a suffix of 'L' or 'l', but the former is less " +
    "likely to be confused with a '1' in most fonts.",
    category = JDK, severity = ERROR, maturity = ON_BY_DEFAULT)
public class LongLiteralLowerCaseSuffix extends DescribingMatcher<LiteralTree> {

  @SuppressWarnings({"unchecked", "varargs"})
  private static final Matcher<LiteralTree> matcher = new Matcher<LiteralTree>() {
    @Override
    public boolean matches(LiteralTree literalTree, VisitorState state) {
      if (literalTree.getKind() == Kind.LONG_LITERAL) {
        // Ugh. The javac AST doesn't seem to record whether the suffix is present, or whether it's
        // an 'l' or 'L'. We have to look at the original source code. 
        JCLiteral longLiteral = (JCLiteral) literalTree;
        try {
          CharSequence sourceFile = state.getPath().getCompilationUnit().getSourceFile()
              .getCharContent(false);
          if (sourceFile == null) {
            return false;
          }
          // Loop through characters of source code from the start position until we hit
          // a non-digit, non-underscore character. If it's 'l', it's the case we're after.
          for (int pos = longLiteral.getStartPosition(); pos < sourceFile.length(); pos++) {
            char literalChar = sourceFile.charAt(pos);
            if (Character.isDigit(literalChar)
                || literalChar == '_' /* Java 7 allows '_' within literals */) {
              continue;
            }
            return literalChar == 'l';
          }
        } catch (IOException e) {
          // IOException while loading content - don't match.
          return false;
        }
      }
      return false;
    }
  };

  @Override
  public boolean matches(LiteralTree methodInvocationTree, VisitorState state) {
    return matcher.matches(methodInvocationTree, state);
  }
  
  @Override
  public Description describe(LiteralTree t, VisitorState state) {
    SuggestedFix fix = new SuggestedFix().replace(t, t.toString().toUpperCase());
    return new Description(t, diagnosticMessage, fix);
  }
  
  public static class Scanner extends com.google.errorprone.Scanner {
    private final DescribingMatcher<LiteralTree> matcher = new LongLiteralLowerCaseSuffix();
    
    @Override
    public Void visitLiteral(LiteralTree node, VisitorState visitorState) {
      evaluateMatch(node, visitorState, matcher);
      return super.visitLiteral(node, visitorState);
    }
  }
}
