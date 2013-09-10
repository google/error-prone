package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneScanner;
import com.google.errorprone.Scanner;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Suppressable;
import com.sun.source.tree.*;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A base class for implementing bug checkers. The {@code BugChecker} supplies a Scanner
 * implementation for this checker, making it easy to use a single checker.
 * Subclasses should also implement one or more of the {@code *Checker} interfaces in this class
 * to declare which tree node types to match against.
 *
 * @author Colin Decker
 * @author Eddie Aftandilian (eaftan@google.com)
 */
public abstract class BugChecker implements Suppressable, Serializable {

  protected final String canonicalName;
  /**
   * A collection of IDs for this check, to be checked for in @SuppressWarnings annotations.
   */
  protected final Set<String> allNames;
  protected final BugPattern pattern;

  public BugChecker() {
    pattern = this.getClass().getAnnotation(BugPattern.class);
    if (pattern == null) {
      throw new IllegalStateException("Class " + this.getClass().getCanonicalName()
          + " not annotated with @BugPattern");
    }
    canonicalName = pattern.name();
    allNames = new HashSet<String>();
    allNames.add(canonicalName);
    allNames.addAll(Arrays.asList(pattern.altNames()));
  }

  /**
   * Helper to create a Description for the common case where the diagnostic message is not
   * parameterized.
   */
  protected Description describeMatch(Tree node, SuggestedFix fix) {
    return new Description(node, getDiagnosticMessage(), fix, pattern.severity());
  }

  /**
   * Generate the compiler diagnostic message based on information in the @BugPattern annotation.
   *
   * <p>If the formatSummary element of the annotation has been set, then use format string
   * substitution to generate the message.  Otherwise, just use the summary element directly.
   *
   * @param args Arguments referenced by the format specifiers in the annotation's formatSummary
   *     element
   * @return The compiler diagnostic message.
   */
  protected String getDiagnosticMessage(Object... args) {
    String summary;
    if (!pattern.formatSummary().isEmpty()) {
      if (args.length == 0) {
        throw new IllegalStateException("Compiler error message expects a format string, but "
            + "no arguments were provided");
      }
      summary = String.format(pattern.formatSummary(), args);
    } else {
      summary = pattern.summary();
    }
    return "[" + pattern.name() + "] " + summary + getLink();
  }

  /**
   * Construct the link text to include in the compiler error message.
   */
  private String getLink() {
    switch (pattern.linkType()) {
      case WIKI:
        return "\n  (see http://code.google.com/p/error-prone/wiki/" + pattern.name() + ")";
      case CUSTOM:
        // annotation.link() must be provided.
        if (pattern.link().isEmpty()) {
          throw new IllegalStateException("If linkType element of @BugPattern is CUSTOM, "
              + "a link element must also be provided.");
        }
        return  "\n  (see " + pattern.link() + ")";
      case NONE:
        return "";
      default:
        throw new IllegalStateException("Unexpected value for linkType element of @BugPattern: "
            + pattern.linkType());
    }
  }

  public String getCanonicalName() {
    return canonicalName;
  }

  @Override
  public Set<String> getAllNames() {
    return allNames;
  }

  public final Scanner createScanner() {
    return ErrorProneScanner.forMatcher(this.getClass());
  }

  public static interface AnnotationTreeMatcher extends Suppressable {
    Description matchAnnotation(AnnotationTree tree, VisitorState state);
  }

  public static interface ArrayAccessTreeMatcher extends Suppressable {
    Description matchArrayAccess(ArrayAccessTree tree, VisitorState state);
  }

  public static interface ArrayTypeTreeMatcher extends Suppressable {
    Description matchArrayType(ArrayTypeTree tree, VisitorState state);
  }

  public static interface AssertTreeMatcher extends Suppressable {
    Description matchAssert(AssertTree tree, VisitorState state);
  }

  public static interface AssignmentTreeMatcher extends Suppressable {
    Description matchAssignment(AssignmentTree tree, VisitorState state);
  }

  public static interface BinaryTreeMatcher extends Suppressable {
    Description matchBinary(BinaryTree tree, VisitorState state);
  }

  public static interface BlockTreeMatcher extends Suppressable {
    Description matchBlock(BlockTree tree, VisitorState state);
  }

  public static interface BreakTreeMatcher extends Suppressable {
    Description matchBreak(BreakTree tree, VisitorState state);
  }

  public static interface CaseTreeMatcher extends Suppressable {
    Description matchCase(CaseTree tree, VisitorState state);
  }

  public static interface CatchTreeMatcher extends Suppressable {
    Description matchCatch(CatchTree tree, VisitorState state);
  }

  public static interface ClassTreeMatcher extends Suppressable {
    Description matchClass(ClassTree tree, VisitorState state);
  }

  public static interface CompilationUnitTreeMatcher extends Suppressable {
    Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state);
  }

  public static interface CompoundAssignmentTreeMatcher extends Suppressable {
    Description matchCompoundAssignment(CompoundAssignmentTree tree, VisitorState state);
  }

  public static interface ConditionalExpressionTreeMatcher
      extends Suppressable {
    Description matchConditionalExpression(ConditionalExpressionTree tree, VisitorState state);
  }

  public static interface ContinueTreeMatcher extends Suppressable {
    Description matchContinue(ContinueTree tree, VisitorState state);
  }

  public static interface DoWhileLoopTreeMatcher extends Suppressable {
    Description matchDoWhileLoop(DoWhileLoopTree tree, VisitorState state);
  }

  public static interface EmptyStatementTreeMatcher extends Suppressable {
    Description matchEmptyStatement(EmptyStatementTree tree, VisitorState state);
  }

  public static interface EnhancedForLoopTreeMatcher extends Suppressable {
    Description matchEnhancedForLoop(EnhancedForLoopTree tree, VisitorState state);
  }

  // Intentionally skip ErroneousTreeMatcher -- we don't analyze malformed expressions.

  public static interface ExpressionStatementTreeMatcher extends Suppressable {
    Description matchExpressionStatement(ExpressionStatementTree tree, VisitorState state);
  }

  public static interface ForLoopTreeMatcher extends Suppressable {
    Description matchForLoop(ForLoopTree tree, VisitorState state);
  }

  public static interface IdentifierTreeMatcher extends Suppressable {
    Description matchIdentifier(IdentifierTree tree, VisitorState state);
  }

  public static interface IfTreeMatcher extends Suppressable {
    Description matchIf(IfTree tree, VisitorState state);
  }

  public static interface ImportTreeMatcher extends Suppressable {
    Description matchImport(ImportTree tree, VisitorState state);
  }

  public static interface InstanceOfTreeMatcher extends Suppressable {
    Description matchInstanceOf(InstanceOfTree tree, VisitorState state);
  }

  public static interface LabeledStatementTreeMatcher extends Suppressable {
    Description matchLabeledStatement(LabeledStatementTree tree, VisitorState state);
  }

  public static interface LiteralTreeMatcher extends Suppressable {
    Description matchLiteral(LiteralTree tree, VisitorState state);
  }

  public static interface MemberSelectTreeMatcher extends Suppressable {
    Description matchMemberSelect(MemberSelectTree tree, VisitorState state);
  }

  public static interface MethodTreeMatcher extends Suppressable {
    Description matchMethod(MethodTree tree, VisitorState state);
  }

  public static interface MethodInvocationTreeMatcher extends Suppressable {
    Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state);
  }

  public static interface ModifiersTreeMatcher extends Suppressable {
    Description matchModifiers(ModifiersTree tree, VisitorState state);
  }

  public static interface NewArrayTreeMatcher extends Suppressable {
    Description matchNewArray(NewArrayTree tree, VisitorState state);
  }

  public static interface NewClassTreeMatcher extends Suppressable {
    Description matchNewClass(NewClassTree tree, VisitorState state);
  }

  // Intentionally skip OtherTreeMatcher. It seems to be used only for let expressions, which are
  // generated by javac to implement autoboxing. We are only interested in source-level constructs.

  public static interface ParameterizedTypeTreeMatcher extends Suppressable {
    Description matchParameterizedType(ParameterizedTypeTree tree, VisitorState state);
  }

  public static interface ParenthesizedTreeMatcher extends Suppressable {
    Description matchParenthesized(ParenthesizedTree tree, VisitorState state);
  }

  public static interface PrimitiveTypeTreeMatcher extends Suppressable {
    Description matchPrimitiveType(PrimitiveTypeTree tree, VisitorState state);
  }

  public static interface ReturnTreeMatcher extends Suppressable {
    Description matchReturn(ReturnTree tree, VisitorState state);
  }

  public static interface SwitchTreeMatcher extends Suppressable {
    Description matchSwitch(SwitchTree tree, VisitorState state);
  }

  public static interface SynchronizedTreeMatcher extends Suppressable {
    Description matchSynchronized(SynchronizedTree tree, VisitorState state);
  }

  public static interface ThrowTreeMatcher extends Suppressable {
    Description matchThrow(ThrowTree tree, VisitorState state);
  }

  public static interface TryTreeMatcher extends Suppressable {
    Description matchTry(TryTree tree, VisitorState state);
  }

  public static interface TypeCastTreeMatcher extends Suppressable {
    Description matchTypeCast(TypeCastTree tree, VisitorState state);
  }

  public static interface TypeParameterTreeMatcher extends Suppressable {
    Description matchTypeParameter(TypeParameterTree tree, VisitorState state);
  }

  public static interface UnaryTreeMatcher extends Suppressable {
    Description matchUnary(UnaryTree tree, VisitorState state);
  }

  // Intentionally skip UnionTypeTreeMatcher -- this is not available in Java 6.

  public static interface VariableTreeMatcher extends Suppressable {
    Description matchVariable(VariableTree tree, VisitorState state);
  }

  public static interface WhileLoopTreeMatcher extends Suppressable {
    Description matchWhileLoop(WhileLoopTree tree, VisitorState state);
  }

  public static interface WildcardTreeMatcher extends Suppressable {
    Description matchWildcard(WildcardTree tree, VisitorState state);
  }
}