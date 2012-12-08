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
 * For feature request: http://code.google.com/p/error-prone/issues/detail?id=38
 * @author alexeagle@google.com (Alex Eagle)
 */
@BugPattern(name = "RuntimeAccessToNonRetainedAnnotation",
        category = JDK, maturity = PROPOSED, severity = ERROR,
        summary = "The Class.getAnnotation method may be called with an annotation type that is not retained at " +
                "runtime, and thus it will always return null.",
        explanation = "Annotation types may be annotated with `@Retention(RUNTIME)` so that they're included in the " +
                "class file and retained by the VM at run time, so they may be read reflectively. If an annotation " +
                "isn't marked this way, then it cannot be used reflectively.")
public class RuntimeAccessToNonRetainedAnnotation extends DescribingMatcher<Tree>{

    @Override
    public Description describe(Tree tree, VisitorState state) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean matches(Tree tree, VisitorState state) {
        throw new UnsupportedOperationException("not implemented");
    }
}
