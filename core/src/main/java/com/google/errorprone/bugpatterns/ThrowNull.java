package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.matchers.Matchers.isNull;
import static com.google.errorprone.matchers.Matchers.throwStatement;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.LinkType;
import com.google.errorprone.BugPattern.MaturityLevel;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ThrowTreeMatcher;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnalysis;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.StatementTree;
import com.sun.source.tree.ThrowTree;

@BugPattern(name = "ThrowNull", summary = "throw null results in a NullPointerException.",
    explanation = "throw null is allowed by the vanilla compiler, but since throw checks"
        + " that its argument is nonnull, this actually ends up throwing a NullPointerException."
        + " Nevertheless, there's never a valid reason to do this.", linkType = LinkType.CUSTOM,
    link = "http://stackoverflow.com/q/17576922/869736", category = JDK,
    maturity = MaturityLevel.EXPERIMENTAL, severity = SeverityLevel.ERROR)
public class ThrowNull extends BugChecker implements ThrowTreeMatcher {
  private static final NullnessAnalysis NULLNESS_ANALYSIS = new NullnessAnalysis();
  private static final Matcher<StatementTree> THROW_NULL_MATCHER = throwStatement(isNull(NULLNESS_ANALYSIS));

  @Override
  public Description matchThrow(ThrowTree tree, VisitorState state) {
    if (THROW_NULL_MATCHER.matches(tree, state)) {
      return buildDescription(tree).addFix(
          SuggestedFix.replace(tree.getExpression(), "new NullPointerException()")).build();
    } else {
      return Description.NO_MATCH;
    }
  }
}
