package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugPattern;
import com.google.errorprone.Scanner;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Description;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.BreakTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ContinueTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ErroneousTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.tree.WildcardTree;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A base class for implementing bug checkers.  You will need to override {@code match} methods
 * that either return {@code null} (meaning no match) or a {@link Description} (indicating a match,
 * an error message, and a suggested fix). The {@code BugChecker} can supply a Scanner
 * implementation for this checker, making it easy to use a single checker.
 *
 * @author Colin Decker
 * @author Eddie Aftandilian (eaftan@google.com)
 */
public abstract class BugChecker {

  protected final String canonicalName;
  /**
   * A collection of IDs for this check, to be checked for in @SuppressWarnings annotations.
   */
  protected final Set<String> allNames;
  protected final BugPattern annotation;

  public BugChecker() {
    annotation = this.getClass().getAnnotation(BugPattern.class);
    if (annotation == null) {
      throw new IllegalStateException("Class " + this.getClass().getCanonicalName()
          + " not annotated with @BugPattern");
    }
    canonicalName = annotation.name();
    allNames = new HashSet<String>();
    allNames.add(canonicalName);
    allNames.addAll(Arrays.asList(annotation.altNames()));
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
    if (!annotation.formatSummary().isEmpty()) {
      if (args.length == 0) {
        throw new IllegalStateException("Compiler error message expects a format string, but "
            + "no arguments were provided");
      }
      summary = String.format(annotation.formatSummary(), args);
    } else {
      summary = annotation.summary();
    }
    return "[" + annotation.name() + "] " + summary + getLink();
  }

  /**
   * Construct the link text to include in the compiler error message.
   */
  private String getLink() {
    switch (annotation.linkType()) {
      case WIKI:
        return "\n  (see http://code.google.com/p/error-prone/wiki/" + annotation.name() + ")";
      case CUSTOM:
        // annotation.link() must be provided.
        if (annotation.link().isEmpty()) {
          throw new IllegalStateException("If linkType element of @BugPattern is CUSTOM, "
              + "a link element must also be provided.");
        }
        return  "\n  (see " + annotation.link() + ")";
      case NONE:
        return "";
      default:
        throw new IllegalStateException("Unexpected value for linkType element of @BugPattern: "
            + annotation.linkType());
    }
  }

  public String getCanonicalName() {
    return canonicalName;
  }

  public Set<String> getAllNames() {
    return allNames;
  }

  // TODO(eaftan): This isn't going to work reflectively.
  public final Scanner createScanner() {
    return new BugCheckingScanner();
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchAnnotation(AnnotationTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchArrayAccess(ArrayAccessTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchArrayType(ArrayTypeTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchAssert(AssertTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchAssignment(AssignmentTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchBlock(BlockTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchBreak(BreakTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchCase(CaseTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchCatch(CatchTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchClass(ClassTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchCompoundAssignment(CompoundAssignmentTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchConditionalExpression(ConditionalExpressionTree tree,
      VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchContinue(ContinueTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchDoWhileLoop(DoWhileLoopTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchEmptyStatement(EmptyStatementTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchEnhancedForLoop(EnhancedForLoopTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchErroneous(ErroneousTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchExpressionStatement(ExpressionStatementTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchForLoop(ForLoopTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchIdentifier(IdentifierTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchIf(IfTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchImport(ImportTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchInstanceOf(InstanceOfTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchLabeledStatement(LabeledStatementTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchLiteral(LiteralTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchMemberSelect(MemberSelectTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchMethod(MethodTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchModifiers(ModifiersTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchNewArray(NewArrayTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchOther(Tree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchParameterizedType(ParameterizedTypeTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchParenthesized(ParenthesizedTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchPrimitiveType(PrimitiveTypeTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchReturn(ReturnTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchSwitch(SwitchTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchSynchronized(SynchronizedTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchThrow(ThrowTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchTry(TryTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchTypeCast(TypeCastTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchTypeParameter(TypeParameterTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchUnary(UnaryTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchVariable(VariableTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchWhileLoop(WhileLoopTree tree, VisitorState state) {
    return null;
  }

  /**
   * Returns a {@code Description} if the code for the given tree should match or
   * {@code null} to indicate no change.
   */
  public Description matchWildcard(WildcardTree tree, VisitorState state) {
    return null;
  }

  /**
   * {@code Scanner} that uses the enclosing {@code BugChecker2}.
   */
  private final class BugCheckingScanner extends Scanner {

    /**
     * If {@code description} is null, does nothing. Otherwise, reports the tree as a match and
     * reports the given description.
     */
    private void report(Tree tree, VisitorState state, Description description) {
      if (description != null) {
        state.getMatchListener().onMatch(tree);
        state.getDescriptionListener().onDescribed(description);
      }
    }

    @Override
    public Void visitAnnotation(AnnotationTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchAnnotation(tree, stateWithPath));
      }
      return super.visitAnnotation(tree, state);
    }

    @Override
    public Void visitArrayAccess(ArrayAccessTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchArrayAccess(tree, stateWithPath));
      }
      return super.visitArrayAccess(tree, state);
    }

    @Override
    public Void visitArrayType(ArrayTypeTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchArrayType(tree, stateWithPath));
      }
      return super.visitArrayType(tree, state);
    }

    @Override
    public Void visitAssert(AssertTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchAssert(tree, stateWithPath));
      }
      return super.visitAssert(tree, state);
    }

    @Override
    public Void visitAssignment(AssignmentTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchAssignment(tree, stateWithPath));
      }
      return super.visitAssignment(tree, state);
    }

    @Override
    public Void visitBinary(BinaryTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchBinary(tree, stateWithPath));
      }
      return super.visitBinary(tree, state);
    }

    @Override
    public Void visitBlock(BlockTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchBlock(tree, stateWithPath));
      }
      return super.visitBlock(tree, state);
    }

    @Override
    public Void visitBreak(BreakTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchBreak(tree, stateWithPath));
      }
      return super.visitBreak(tree, state);
    }

    @Override
    public Void visitCase(CaseTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchCase(tree, stateWithPath));
      }
      return super.visitCase(tree, state);
    }

    @Override
    public Void visitCatch(CatchTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchCatch(tree, stateWithPath));
      }
      return super.visitCatch(tree, state);
    }

    @Override
    public Void visitClass(ClassTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchClass(tree, stateWithPath));
      }
      return super.visitClass(tree, state);
    }

    @Override
    public Void visitCompilationUnit(CompilationUnitTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchCompilationUnit(tree, stateWithPath));
      }
      return super.visitCompilationUnit(tree, state);
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchCompoundAssignment(tree, stateWithPath));
      }
      return super.visitCompoundAssignment(tree, state);
    }

    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchConditionalExpression(tree, stateWithPath));
      }
      return super.visitConditionalExpression(tree, state);
    }

    @Override
    public Void visitContinue(ContinueTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchContinue(tree, stateWithPath));
      }
      return super.visitContinue(tree, state);
    }

    @Override
    public Void visitDoWhileLoop(DoWhileLoopTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchDoWhileLoop(tree, stateWithPath));
      }
      return super.visitDoWhileLoop(tree, state);
    }

    @Override
    public Void visitEmptyStatement(EmptyStatementTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchEmptyStatement(tree, stateWithPath));
      }
      return super.visitEmptyStatement(tree, state);
    }

    @Override
    public Void visitEnhancedForLoop(EnhancedForLoopTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchEnhancedForLoop(tree, stateWithPath));
      }
      return super.visitEnhancedForLoop(tree, state);
    }

    @Override
    public Void visitErroneous(ErroneousTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchErroneous(tree, stateWithPath));
      }
      return super.visitErroneous(tree, state);
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatementTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchExpressionStatement(tree, stateWithPath));
      }
      return super.visitExpressionStatement(tree, state);
    }

    @Override
    public Void visitForLoop(ForLoopTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchForLoop(tree, stateWithPath));
      }
      return super.visitForLoop(tree, state);
    }

    @Override
    public Void visitIdentifier(IdentifierTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchIdentifier(tree, stateWithPath));
      }
      return super.visitIdentifier(tree, state);
    }

    @Override
    public Void visitIf(IfTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchIf(tree, stateWithPath));
      }
      return super.visitIf(tree, state);
    }

    @Override
    public Void visitImport(ImportTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchImport(tree, stateWithPath));
      }
      return super.visitImport(tree, state);
    }

    @Override
    public Void visitInstanceOf(InstanceOfTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchInstanceOf(tree, stateWithPath));
      }
      return super.visitInstanceOf(tree, state);
    }

    @Override
    public Void visitLabeledStatement(LabeledStatementTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchLabeledStatement(tree, stateWithPath));
      }
      return super.visitLabeledStatement(tree, state);
    }

    @Override
    public Void visitLiteral(LiteralTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchLiteral(tree, stateWithPath));
      }
      return super.visitLiteral(tree, state);
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchMemberSelect(tree, stateWithPath));
      }
      return super.visitMemberSelect(tree, state);
    }

    @Override
    public Void visitMethod(MethodTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchMethod(tree, stateWithPath));
      }
      return super.visitMethod(tree, state);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchMethodInvocation(tree, stateWithPath));
      }
      return super.visitMethodInvocation(tree, state);
    }

    @Override
    public Void visitModifiers(ModifiersTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchModifiers(tree, stateWithPath));
      }
      return super.visitModifiers(tree, state);
    }

    @Override
    public Void visitNewArray(NewArrayTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchNewArray(tree, stateWithPath));
      }
      return super.visitNewArray(tree, state);
    }

    @Override
    public Void visitNewClass(NewClassTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchNewClass(tree, stateWithPath));
      }
      return super.visitNewClass(tree, state);
    }

    @Override
    public Void visitOther(Tree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchOther(tree, stateWithPath));
      }
      return super.visitOther(tree, state);
    }

    @Override
    public Void visitParameterizedType(ParameterizedTypeTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchParameterizedType(tree, stateWithPath));
      }
      return super.visitParameterizedType(tree, state);
    }

    @Override
    public Void visitParenthesized(ParenthesizedTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchParenthesized(tree, stateWithPath));
      }
      return super.visitParenthesized(tree, state);
    }

    @Override
    public Void visitPrimitiveType(PrimitiveTypeTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchPrimitiveType(tree, stateWithPath));
      }
      return super.visitPrimitiveType(tree, state);
    }

    @Override
    public Void visitReturn(ReturnTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchReturn(tree, stateWithPath));
      }
      return super.visitReturn(tree, state);
    }

    @Override
    public Void visitSwitch(SwitchTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchSwitch(tree, stateWithPath));
      }
      return super.visitSwitch(tree, state);
    }

    @Override
    public Void visitSynchronized(SynchronizedTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchSynchronized(tree, stateWithPath));
      }
      return super.visitSynchronized(tree, state);
    }

    @Override
    public Void visitThrow(ThrowTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchThrow(tree, stateWithPath));
      }
      return super.visitThrow(tree, state);
    }

    @Override
    public Void visitTry(TryTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchTry(tree, stateWithPath));
      }
      return super.visitTry(tree, state);
    }

    @Override
    public Void visitTypeCast(TypeCastTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchTypeCast(tree, stateWithPath));
      }
      return super.visitTypeCast(tree, state);
    }

    @Override
    public Void visitTypeParameter(TypeParameterTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchTypeParameter(tree, stateWithPath));
      }
      return super.visitTypeParameter(tree, state);
    }

    @Override
    public Void visitUnary(UnaryTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchUnary(tree, stateWithPath));
      }
      return super.visitUnary(tree, state);
    }

    @Override
    public Void visitVariable(VariableTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchVariable(tree, stateWithPath));
      }
      return super.visitVariable(tree, state);
    }

    @Override
    public Void visitWhileLoop(WhileLoopTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchWhileLoop(tree, stateWithPath));
      }
      return super.visitWhileLoop(tree, state);
    }

    @Override
    public Void visitWildcard(WildcardTree tree, VisitorState state) {
      VisitorState stateWithPath = state.withPath(getCurrentPath());
      if (!isSuppressed(getAllNames())) {
        report(tree, stateWithPath, matchWildcard(tree, stateWithPath));
      }
      return super.visitWildcard(tree, state);
    }
  }
}