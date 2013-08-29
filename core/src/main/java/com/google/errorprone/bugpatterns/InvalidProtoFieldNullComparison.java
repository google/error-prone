package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.BugPattern.MaturityLevel;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;

import java.util.EnumSet;
import java.util.Set;

@BugPattern(name = "InvalidProtoFieldNullComparison", category = Category.ONE_OFF,
    severity = SeverityLevel.ERROR, maturity = MaturityLevel.EXPERIMENTAL,
    summary = "Protocol Buffers fields cannot be null",
    explanation = "This checker looks for invalid comparisons of Proto fields with null." +
    		" These comparisons are always true or false since Proto field accessors never " +
    		"return null.")
public class InvalidProtoFieldNullComparison extends DescribingMatcher<BinaryTree> {

  private static final String PROTO_SUPER_CLASS = "com.google.protobuf.GeneratedMessage";

  private static final Matcher<ExpressionTree> protoMessageReceiverMatcher =
      Matchers.instanceMethod(Matchers.isSubtypeOf(PROTO_SUPER_CLASS), "*");

  private static final String LIST_INTERFACE = "java.util.List";

  private static final Matcher<Tree> returnsListMatcher =
      Matchers.isCastableTo(LIST_INTERFACE);

  private static final Set<Kind> COMPARISON_OPERATORS =
      EnumSet.of(Kind.EQUAL_TO, Kind.NOT_EQUAL_TO);

  @Override
  public boolean matches(BinaryTree tree, VisitorState state) {
    if (!COMPARISON_OPERATORS.contains(tree.getKind())) {
      return false;
    }
    ExpressionTree leftOperand = tree.getLeftOperand();
    ExpressionTree rightOperand = tree.getRightOperand();
    return (isNull(rightOperand) && isProtoMessageGetInvocation(leftOperand, state))
        || (isNull(leftOperand) && isProtoMessageGetInvocation(rightOperand, state));
  }

  private boolean isNull(ExpressionTree tree) {
    return tree.getKind() == Kind.NULL_LITERAL;
  }

  private boolean isProtoMessageGetInvocation(ExpressionTree tree, VisitorState state) {
    return (isGetMethodInvocation(tree, state) || isGetListMethodInvocation(tree, state))
        && isProtoMessage(tree, state);
  }

  private boolean isFieldGetMethod(String methodName) {
    return methodName.startsWith("get");
  }

  private String getMethodName(ExpressionTree tree) {
    MethodInvocationTree method = (MethodInvocationTree) tree;
    ExpressionTree expressionTree = method.getMethodSelect();
    JCFieldAccess access = (JCFieldAccess) expressionTree;
    return access.sym.getQualifiedName().toString();
  }

  private boolean isGetListMethodInvocation(ExpressionTree tree, VisitorState state) {
    if (tree.getKind() == Tree.Kind.METHOD_INVOCATION) {
      MethodInvocationTree method = (MethodInvocationTree) tree;
      if (!method.getArguments().isEmpty()) {
        return false;
      }
      if (!returnsListMatcher.matches(method, state)) {
        return false;
      }
      ExpressionTree expressionTree = method.getMethodSelect();
      if (expressionTree instanceof JCFieldAccess) {
        JCFieldAccess access = (JCFieldAccess) expressionTree;
        String methodName = access.sym.getQualifiedName().toString();
        return isFieldGetMethod(methodName);
      }
      return true;
    }
    return false;
  }

  private boolean isGetMethodInvocation(ExpressionTree tree, VisitorState state) {
    if (tree.getKind() == Tree.Kind.METHOD_INVOCATION) {
      MethodInvocationTree method = (MethodInvocationTree) tree;
      if (!method.getArguments().isEmpty()) {
        return false;
      }
      if (returnsListMatcher.matches(method, state)) {
        return false;
      }
      ExpressionTree expressionTree = method.getMethodSelect();
      if (expressionTree instanceof JCFieldAccess) {
        JCFieldAccess access = (JCFieldAccess) expressionTree;
        String methodName = access.sym.getQualifiedName().toString();
        return isFieldGetMethod(methodName);
      }
      return true;
    }
    return false;
  }

  private boolean isList(Type type, VisitorState state) {
    Type listInterface = state.getTypeFromString(LIST_INTERFACE);
    if (listInterface == null) {
      return false;
    }
    try {
      if (state.getTypes().isSubtype(type, state.getTypes().erasure(listInterface))) {
        return true;
      }
    } catch (CompletionFailure e) {
    }
    return false;
  }

  private boolean isProtoMessage(ExpressionTree tree, VisitorState state) {
    return protoMessageReceiverMatcher.matches(((MethodInvocationTree) tree).getMethodSelect(), state);
  }

  private static String replaceLast(String text, String pattern, String replacement) {
    StringBuilder builder = new StringBuilder(text);
    int lastIndexOf = builder.lastIndexOf(pattern);
    return builder.replace(lastIndexOf, lastIndexOf + pattern.length(), replacement).toString();
  }

  /**
   * Creates replacements for the following comparisons:
   * <pre>
   * proto.getField() == null --> !proto.hasField()
   * proto.getField() != null --> proto.hasField()
   * proto.getList() == null  --> proto.getList().isEmpty()
   * proto.getList() != null  --> !proto.getList().isEmpty()
   * <pre>
   * Also creates replacements for the Yoda style version of them.
   */
  private String createReplacement(BinaryTree tree, VisitorState state) {
    ExpressionTree leftOperand = tree.getLeftOperand();
    ExpressionTree rightOperand = tree.getRightOperand();
    ExpressionTree methodInvocation;
    if (isNull(leftOperand)) {
      methodInvocation = rightOperand;
    } else {
      methodInvocation = leftOperand;
    }
    if (isGetMethodInvocation(methodInvocation, state)) {
      String methodName = getMethodName(methodInvocation);
      String hasMethod = methodName.replaceFirst("get", "has");
      String replacement = replaceLast(methodInvocation.toString(), methodName, hasMethod);
      replacement = tree.getKind() == Kind.EQUAL_TO ? "!" + replacement : replacement;
      return replacement;
    } else {
      String replacement = methodInvocation.toString() + ".isEmpty()";
      replacement = tree.getKind() == Kind.EQUAL_TO ? replacement : "!" + replacement;
      return replacement;
    }
  }

  @Override
  public Description describe(BinaryTree t, VisitorState state) {
    return new Description(t, "Proto fields are never null",
        new SuggestedFix().replace(t, createReplacement(t, state)));
  }

  public static class Scanner extends com.google.errorprone.Scanner {
    private InvalidProtoFieldNullComparison matcher = new InvalidProtoFieldNullComparison();

    @Override
    public Void visitBinary(BinaryTree tree, VisitorState visitorState) {
      evaluateMatch(tree, visitorState, matcher);
      return super.visitBinary(tree, visitorState);
    }
  }
}
