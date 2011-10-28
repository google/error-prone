// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.errorprone.checkers;

import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.expressionMethodSelect;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.matchers.Matchers.methodSelect;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static java.lang.String.format;

import com.google.common.base.Preconditions;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.SimpleTreeVisitor;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Error checker for calls to the Preconditions class in Guava which use 'expensive' methods of
 * producing the error string. In most cases, users are better off using the equivalent methods
 * which defer the computation of the string until the test actually fails.
 * 
 * @author sjnickerson@google.com (Simon Nickerson)
 */
public class PreconditionsExpensiveStringChecker extends ErrorChecker<MethodInvocationTree> {

  private static final Pattern INVALID_FORMAT_CHARACTERS = Pattern.compile("%[^%s]"); 
  
  
  @Override
  public Matcher<MethodInvocationTree> matcher() {
    return allOf(
        anyOf(
            methodSelect(staticMethod("com.google.common.base.Preconditions", "checkNotNull")),
            methodSelect(staticMethod("com.google.common.base.Preconditions", "checkState")),
            methodSelect(staticMethod("com.google.common.base.Preconditions", "checkArgument"))),
        argument(1, allOf(
            kindIs(Kind.METHOD_INVOCATION, ExpressionTree.class),
            expressionMethodSelect(staticMethod("java.lang.String", "format")),
            new StringFormatCallContainsNoSpecialFormattingMatcher()
          ))
    );
  }
  
  @Override
  public AstError produceError(MethodInvocationTree methodInvocationTree, VisitorState state) {
    MemberSelectTree method = (MemberSelectTree) methodInvocationTree.getMethodSelect();
    MethodNameVisitor visitor = new MethodNameVisitor();
    method.accept(visitor, null);
    
    List<? extends ExpressionTree> arguments = methodInvocationTree.getArguments();    
    MethodInvocationTree stringFormat = (MethodInvocationTree) arguments.get(1);
    
    Position position = getPosition(stringFormat);
    
    // TODO(sjnickerson): Figure out how to get a suggested fix. Basically we remove the String.format()
    // wrapper, but I don't know how to express this. This current one is not correct!
    SuggestedFix fix = new SuggestedFix().delete(position);
    
    
    return new AstError(arguments.get(1),
        format("Second argument to Preconditions.%s is a call to String.format() which can be unwrapped",
            visitor.getName()), fix);
  }
  
  private static class MethodNameVisitor extends SimpleTreeVisitor<Void, Void> {
    private String name;
    @Override
    public Void visitMemberSelect(MemberSelectTree member, Void arg1) {
      this.name = member.getIdentifier().toString();
      return null;
    }
    public String getName() {
      return name;
    }
  }

  private static class StringFormatCallContainsNoSpecialFormattingMatcher implements Matcher<ExpressionTree> {
    @Override
    public boolean matches(ExpressionTree t, VisitorState state) {
      Preconditions.checkArgument(
          t instanceof MethodInvocationTree);
      MethodInvocationTree stringFormatInvocation = (MethodInvocationTree) t;
      Preconditions.checkArgument(
          stringFormatInvocation.getArguments().get(0) instanceof LiteralTree);
      LiteralTree firstArg = (LiteralTree) stringFormatInvocation.getArguments().get(0);
      String literal = firstArg.getValue().toString();
      return !INVALID_FORMAT_CHARACTERS.matcher(literal).find();
    }
  }
}
