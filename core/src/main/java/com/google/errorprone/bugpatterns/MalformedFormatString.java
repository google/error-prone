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
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.isDescendantOfMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.lang.model.type.TypeKind;

import edu.umd.cs.findbugs.formatStringChecker.ExtraFormatArgumentsException;
import edu.umd.cs.findbugs.formatStringChecker.Formatter;
/**
 * @author rburny@google.com (Radoslaw Burny)
 */
@BugPattern(name = "MalformedFormatString",
    summary = "Printf-like format string does not match its arguments",
    explanation = "Format strings for printf family of functions contain format specifiers"
        + " (placeholders) which must match amount and type of arguments that follow them. If there"
        + " are more arguments then specifiers, redundant ones are silently ignored. If there are"
        + " less, or their types don't match, runtime exception is thrown.",
    category = JDK, maturity = EXPERIMENTAL, severity = ERROR)
public class MalformedFormatString extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> isPrintfLike = anyOf(
    isDescendantOfMethod("java.io.PrintStream", "printf(java.lang.String,java.lang.Object...)"),
    isDescendantOfMethod("java.util.Formatter", "format(java.lang.String,java.lang.Object...)"),
    staticMethod("java.lang.String", "format(java.lang.String,java.lang.Object...)"));
  private static final Matcher<ExpressionTree> isPrintfLikeWithLocale = anyOf(
    isDescendantOfMethod("java.io.PrintStream",
        "printf(java.util.Locale,java.lang.String,java.lang.Object...)"),
    isDescendantOfMethod("java.util.Formatter",
        "format(java.util.Locale,java.lang.String,java.lang.Object...)"),
    staticMethod("java.lang.String",
        "format(java.util.Locale,java.lang.String,java.lang.Object...)"));

  private static final Map<TypeKind, String> BOXED_TYPE_NAMES;

  static {
    Map<TypeKind, String> boxedTypeNames = new EnumMap<TypeKind, String>(TypeKind.class);
    boxedTypeNames.put(TypeKind.BYTE, Byte.class.getName());
    boxedTypeNames.put(TypeKind.SHORT, Short.class.getName());
    boxedTypeNames.put(TypeKind.INT, Integer.class.getName());
    boxedTypeNames.put(TypeKind.LONG, Long.class.getName());
    boxedTypeNames.put(TypeKind.FLOAT, Float.class.getName());
    boxedTypeNames.put(TypeKind.DOUBLE, Double.class.getName());
    boxedTypeNames.put(TypeKind.BOOLEAN, Boolean.class.getName());
    boxedTypeNames.put(TypeKind.CHAR, Character.class.getName());
    boxedTypeNames.put(TypeKind.NULL, Object.class.getName());
    BOXED_TYPE_NAMES = Collections.unmodifiableMap(boxedTypeNames);
  }

  // get type name in format accepted by Formatter.check
  private static String getFormatterType(Type type) {
    String boxedTypeName = BOXED_TYPE_NAMES.get(type.getKind());
    String typeName = (boxedTypeName != null ? boxedTypeName : type.toString());
    return ("L" + typeName.replace(".", "/") + ";");
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    int formatIndex;
    if (isPrintfLike.matches(tree, state)) {
      formatIndex = 0;
    } else if (isPrintfLikeWithLocale.matches(tree, state)) {
      formatIndex = 1;
    } else {
      return Description.NO_MATCH;
    }

    List<? extends ExpressionTree> args = tree.getArguments();
    if (args.get(formatIndex).getKind() != Kind.STRING_LITERAL) {
      return Description.NO_MATCH;
    }

    JCLiteral format = (JCLiteral) args.get(formatIndex);
    List<String> argTypes = new ArrayList<String>();
    for (int i = formatIndex + 1; i < args.size(); ++i) {
      Type type = ((JCExpression) args.get(i)).type;
      argTypes.add(getFormatterType(type));
    }

    try {
      Formatter.check((String) format.getValue(), argTypes.toArray(new String[0]));
    } catch (ExtraFormatArgumentsException e) {
      int begin = state.getEndPosition((JCExpression) args.get(formatIndex + e.used));
      int end = state.getEndPosition((JCMethodInvocation) tree);
      if (end < 0) {
        return describeMatch(tree, null);
      }
      Fix fix = new SuggestedFix().replace(begin, end - 1, "");
      return describeMatch(tree, fix);
    } catch (Exception e) {
      // TODO(rburny): provide fixes for other problems
    }
    return Description.NO_MATCH;
  }
}
