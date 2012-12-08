package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.Tree;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.PROPOSED;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

/**
 * For feature request: http://code.google.com/p/error-prone/issues/detail?id=35
 * @author alexeagle@google.com (Alex Eagle)
 */
@BugPattern(name = "IncompatibleEqualsAndHashcode",
        category = JDK, maturity = PROPOSED, severity = ERROR,
        summary = "Objects which are equals() should have the same hashcode()",
        // TODO: distill  cpovirk's summary
        explanation = "See http://code.google.com/p/error-prone/issues/detail?id=35")
public class IncompatibleEqualsAndHashcode extends DescribingMatcher<Tree> {
    @Override
    public Description describe(Tree tree, VisitorState state) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean matches(Tree tree, VisitorState state) {
        throw new UnsupportedOperationException("not implemented");
    }
}
