package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.ON_BY_DEFAULT;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.hasElementWithValue;
import static com.google.errorprone.matchers.Matchers.isType;
import static com.google.errorprone.matchers.Matchers.stringLiteral;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.AnnotationTree;

import java.util.List;

/**
 * Find uses of SuppressWarnings with "deprecated".
 * 
 * @author sjnickerson@google.com (Simon Nickerson)
 */
@BugPattern(name = "SuppressWarningsDeprecated",
  summary = "Suppressing \"deprecated\" is probably a typo",
  explanation =
    "To suppress warnings to deprecated methods, you should add the annotation\n" +
    "{{{@SuppressWarnings(\"deprecation\")}}}\n" +
    "and not\n" +
    "{{{@SuppressWarnings(\"deprecated\")}}}",
  category = JDK, severity = ERROR, maturity = ON_BY_DEFAULT)
public class SuppressWarningsDeprecated extends AbstractSuppressWarningsMatcher {

  @SuppressWarnings({"varargs", "unchecked"})
  private static final Matcher<AnnotationTree> matcher = allOf(
      isType("java.lang.SuppressWarnings"),
      hasElementWithValue("value", stringLiteral("deprecated")));

  @Override
  protected Matcher<AnnotationTree> getMatcher() {
    return matcher;
  }
  
  @Override
  protected void processSuppressWarningsValues(List<String> values) {
    for (int i = 0; i < values.size(); i++) {
      if (values.get(i).equals("deprecated")) {
        values.set(i, "deprecation");
      }
    }
  }
  
  public static class Scanner extends com.google.errorprone.Scanner {
    private final DescribingMatcher<AnnotationTree> matcher = new SuppressWarningsDeprecated();

    @Override
    public Void visitAnnotation(AnnotationTree node, VisitorState visitorState) {
      evaluateMatch(node, visitorState, matcher);
      return super.visitAnnotation(node, visitorState);
    }
  }
}
