/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.constValue;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.AnnotationMatcherUtils;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.method.MethodMatchers.MethodNameMatcher;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.List;
import java.util.regex.Pattern;

/** Check for the a11y antipattern of setting CSS outline attributes to none or 0. */
@BugPattern(
    name = "OutlineNone",
    summary =
        "Setting CSS outline style to none or 0 (while not otherwise providing visual focus "
            + "indicators) is inaccessible for users navigating a web page without a mouse.",
    severity = WARNING,
    providesFix = REQUIRES_HUMAN_ATTENTION)
public class OutlineNone extends BugChecker
    implements MethodInvocationTreeMatcher, AnnotationTreeMatcher {

  // TODO(b/119196262): Expand to more methods of setting CSS properties.
  private static final Matcher<AnnotationTree> TEMPLATE_ANNOTATION =
      Matchers.isType("com.google.gwt.safehtml.client.SafeHtmlTemplates.Template");
  private static final MethodNameMatcher GWT_SET_PROPERTY =
      Matchers.instanceMethod()
          .onDescendantOf("com.google.gwt.dom.client.Style")
          .withNameMatching(Pattern.compile("setProperty(Px)?"));
  private static final Pattern OUTLINE_NONE_REGEX =
      Pattern.compile("outline\\s*:\\s*(none|0px)\\s*;?");
  private static final ImmutableSet<String> NONE_STRINGS = ImmutableSet.of("none", "0px");

  /**
   * Matches on {@code @Template} annotations whose value contains "outline:none" or equivalent
   * outline style.
   */
  @Override
  public Description matchAnnotation(AnnotationTree tree, VisitorState state) {
    if (!TEMPLATE_ANNOTATION.matches(tree, state)) {
      return NO_MATCH;
    }
    ExpressionTree arg = AnnotationMatcherUtils.getArgument(tree, "value");
    String template = constValue(arg, String.class);
    if (template == null) {
      return NO_MATCH;
    }
    java.util.regex.Matcher matcher = OUTLINE_NONE_REGEX.matcher(template);
    if (!matcher.find()) {
      return NO_MATCH;
    }
    return describeMatch(tree);
  }

  /** Matches on {@code setProperty("outline", "none")} and equivalent method calls. */
  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    List<? extends ExpressionTree> args = tree.getArguments();
    /*
     * Matches the following methods from com.google.gwt.dom.client.Style:
     *   setProperty(String name, String value)
     *   setProperty(String name, double value, Unit unit)
     *   setPropertyPx(String name, int value)
     * The first argument is always `name`; we only care about these when `name` is "outline".
     * The second argument is always `value`, but of variable type. We care about the strings
     *   "none" and "0px", and any numeric `0`.
     * We don't care about the `unit` parameter, as zero of anything is bad.
     */
    if (GWT_SET_PROPERTY.matches(tree, state)
        && args.size() >= 2
        && "outline".equals(constValue(args.get(0), String.class))
        && constantNoneOrZero(args.get(1))) {
      return describeMatch(tree);
    }
    return NO_MATCH;
  }

  /**
   * Matches if the expression is a numeric 0, or either of the strings "none" or "0px". These are
   * the values which will cause the outline to be invisible and therefore impede accessibility.
   */
  private static boolean constantNoneOrZero(ExpressionTree arg) {
    Object value = constValue(arg);
    if (value instanceof String && NONE_STRINGS.contains(value)) {
      return true;
    }
    return value instanceof Number && ((Number) value).doubleValue() == 0.0;
  }
}
