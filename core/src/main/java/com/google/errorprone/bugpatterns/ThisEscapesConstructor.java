package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Matchers.methodIsConstructor;
import static com.google.errorprone.matchers.Matchers.hasArguments;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.StatementTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

import java.util.List;

/**
 * Bug checker to detect usage of {@code return null;}.
 */
@BugPattern(name = "ThisEscapesConstructor", summary = "Do not let 'this' escape the constructor.", severity = SUGGESTION)
public class ThisEscapesConstructor extends BugChecker implements MethodTreeMatcher {
    // your code here
    private static final Matcher<MethodTree> IS_CONSTRUCTOR = methodIsConstructor();
    // private static final Matcher<MethodTree> THIS_IN_EXPRESSION = IdentifierTreeMatcher;

    @Override
    public Description matchMethod(MethodTree constructor, VisitorState state) {
        if (!IS_CONSTRUCTOR.matches(constructor, state)) {
            return NO_MATCH;
        }
        if (constructor.getBody() == null) {
            return NO_MATCH;
        }

        BlockTree body = constructor.getBody();
        new TreeScanner<Void, Void>() {
                @Override
                public Void visitAssignment(AssignmentTree assignment, Void unused) {
                    //look at right hand side only
                    Description description = scanExpression(body, assignment.getExpression(), state);
                    return super.visitAssignment(assignment, unused);
                }
                
                @Override
                public Void visitMethodInvocation(MethodInvocationTree invocation, Void unused) {
                    Description description = scanMethodInvocation(body, invocation, state);
                    return super.visitMethodInvocation(invocation, unused);
                }
            }.scan(body, null);

        return NO_MATCH;
    }

    Description scanMethodInvocation(BlockTree block, MethodInvocationTree invocation, VisitorState state) {
        // TODO: add looking at if any arguments equal "this"
        List<? extends ExpressionTree> args = invocation.getArguments();
        Description d;
        for (ExpressionTree arg : args) {
            scanExpression(block, arg, state);
        }
        return NO_MATCH;
    }

    Description scanExpression(BlockTree block, ExpressionTree rhs, VisitorState state) {
        if (!(rhs instanceof IdentifierTree)) {
          return NO_MATCH;
        }
        IdentifierTree identifierTree = ((IdentifierTree) rhs);
        if (identifierTree.getName().contentEquals("this")) {
            state.reportMatch(describeMatch(identifierTree));
        }
        return NO_MATCH;
        
    }
}