/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

package com.google.errorprone;

import com.google.errorprone.matchers.ErrorProducingMatcher.AstError;
import com.sun.source.tree.*;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Pair;
import com.google.errorprone.matchers.ErrorProducingMatcher;
import com.google.errorprone.matchers.PreconditionsCheckNotNullMatcher;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import static java.util.Arrays.asList;

/**
 * Visitor, following the visitor pattern, which may visit each node in the parsed AST.
 * @author Alex Eagle (alexeagle@google.com)
 */
class ASTVisitor implements TreeVisitor<Void, VisitorState> {

  //TODO: proper logging
  private static final Boolean DEBUG = false;
  private static final String MESSAGE_BUNDLE_KEY = "error.prone";

  private final Element element;
  private final JavacElements elementUtils;
  private final ProcessingEnvironment processingEnv;
  private final Context context;

  private final Iterable<? extends ErrorProducingMatcher<MethodInvocationTree>>
      methodInvocationMatchers = asList(new PreconditionsCheckNotNullMatcher());

  public ASTVisitor(Element element, ProcessingEnvironment processingEnv) {
    this.element = element;
    this.context = ((JavacProcessingEnvironment)processingEnv).getContext();
    this.elementUtils = ((JavacProcessingEnvironment)processingEnv).getElementUtils();
    this.processingEnv = processingEnv;
  }

  private void trace(Tree tree) {
    if (DEBUG) {
      String firstLine = tree.toString();
      int newline = firstLine.indexOf('\n');
      if (newline == 0) {
        newline = firstLine.indexOf('\n', 1);
      }
      if (newline >= 0) {
        firstLine = firstLine.substring(0, newline);
      }
      Log.instance(context).noticeWriter.println("Visiting " + tree.getClass().getSimpleName()
          + ": " + firstLine);
    }
  }

  private void emitError(AstError error, VisitorState state) {
    Log log = Log.instance(context);
    Pair<JCTree, JCCompilationUnit> treeAndTopLevel =
        elementUtils.getTreeAndTopLevel(element, null, null);
    JavaFileObject originalSource = null;
    if (treeAndTopLevel != null) {
      originalSource = log.useSource(treeAndTopLevel.snd.getSourceFile());
    }
    try {
      // Workaround. The first API increments the error count and causes the build to fail.
      // The second API gets the correct line and column number.
      // TODO: figure out how to get both features with one error
      processingEnv.getMessager().printMessage(Kind.ERROR, "");
      log.error((DiagnosticPosition) error.match, MESSAGE_BUNDLE_KEY, error.message);
    } finally {
      if (originalSource != null) {
        log.useSource(originalSource);
      }
    }
  }

  @Override
  public Void visitAnnotation(AnnotationTree annotationTree, VisitorState state) {
    trace(annotationTree);
    return null;
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree methodInvocationTree, VisitorState state) {
    trace(methodInvocationTree);
    for (ErrorProducingMatcher<MethodInvocationTree> matcher : methodInvocationMatchers) {
      AstError error = matcher.matchWithError(methodInvocationTree, state);
      if (error != null) {
        emitError(error, state);
      }
    }
    return null;
  }

  @Override
  public Void visitAssert(AssertTree assertTree, VisitorState state) {
    trace(assertTree);
    return null;
  }

  @Override
  public Void visitAssignment(AssignmentTree assignmentTree, VisitorState state) {
    trace(assignmentTree);
    return null;
  }

  @Override
  public Void visitCompoundAssignment(CompoundAssignmentTree compoundAssignmentTree,
      VisitorState state) {
    trace(compoundAssignmentTree);
    return null;
  }

  @Override
  public Void visitBinary(BinaryTree binaryTree, VisitorState state) {
    trace(binaryTree);
    return null;
  }

  @Override
  public Void visitBlock(BlockTree blockTree, VisitorState state) {
    trace(blockTree);
    for (StatementTree statementTree : blockTree.getStatements()) {
      statementTree.accept(this, state);
    }
    return null;
  }

  @Override
  public Void visitBreak(BreakTree breakTree, VisitorState state) {
    trace(breakTree);
    return null;
  }

  @Override
  public Void visitCase(CaseTree caseTree, VisitorState state) {
    trace(caseTree);
    return null;
  }

  @Override
  public Void visitCatch(CatchTree catchTree, VisitorState state) {
    trace(catchTree);
    return null;
  }

  @Override
  public Void visitClass(ClassTree classTree, VisitorState state) {
    trace(classTree);
    for (Tree tree : classTree.getMembers()) {
      tree.accept(this, state);
    }
    return null;
  }

  @Override
  public Void visitConditionalExpression(ConditionalExpressionTree conditionalExpressionTree,
      VisitorState state) {
    trace(conditionalExpressionTree);
    return null;
  }

  @Override
  public Void visitContinue(ContinueTree continueTree, VisitorState state) {
    trace(continueTree);
    return null;
  }

  @Override
  public Void visitDoWhileLoop(DoWhileLoopTree doWhileLoopTree, VisitorState state) {
    trace(doWhileLoopTree);
    return null;
  }

  @Override
  public Void visitErroneous(ErroneousTree erroneousTree, VisitorState state) {
    trace(erroneousTree);
    return null;
  }

  @Override
  public Void visitExpressionStatement(ExpressionStatementTree expressionStatementTree,
      VisitorState state) {
    trace(expressionStatementTree);
    expressionStatementTree.getExpression().accept(this, state);
    return null;
  }

  @Override
  public Void visitEnhancedForLoop(EnhancedForLoopTree enhancedForLoopTree, VisitorState state) {
    trace(enhancedForLoopTree);
    return null;
  }

  @Override
  public Void visitForLoop(ForLoopTree forLoopTree, VisitorState state) {
    trace(forLoopTree);
    return null;
  }

  @Override
  public Void visitIdentifier(IdentifierTree identifierTree, VisitorState state) {
    trace(identifierTree);
    return null;
  }

  @Override
  public Void visitIf(IfTree ifTree, VisitorState state) {
    trace(ifTree);
    return null;
  }

  @Override
  public Void visitImport(ImportTree importTree, VisitorState state) {
    state.imports.add(importTree);
    trace(importTree);
    return null;
  }

  @Override
  public Void visitArrayAccess(ArrayAccessTree arrayAccessTree, VisitorState state) {
    trace(arrayAccessTree);
    return null;
  }

  @Override
  public Void visitLabeledStatement(LabeledStatementTree labeledStatementTree, VisitorState state) {
    trace(labeledStatementTree);
    return null;
  }

  @Override
  public Void visitLiteral(LiteralTree literalTree, VisitorState state) {
    trace(literalTree);
    return null;
  }

  @Override
  public Void visitMethod(MethodTree methodTree, VisitorState state) {
    trace(methodTree);

    if (methodTree.getBody() != null) {
      methodTree.getBody().accept(this, state);
    }
    return null;
  }

  @Override
  public Void visitModifiers(ModifiersTree modifiersTree, VisitorState state) {
    trace(modifiersTree);
    return null;
  }

  @Override
  public Void visitNewArray(NewArrayTree newArrayTree, VisitorState state) {
    trace(newArrayTree);
    return null;
  }

  @Override
  public Void visitNewClass(NewClassTree newClassTree, VisitorState state) {
    trace(newClassTree);
    if (newClassTree.getClassBody() != null) {
      newClassTree.getClassBody().accept(this, state);
    }
    return null;
  }

  @Override
  public Void visitParenthesized(ParenthesizedTree parenthesizedTree, VisitorState state) {
    trace(parenthesizedTree);
    return null;
  }

  @Override
  public Void visitReturn(ReturnTree returnTree, VisitorState state) {
    trace(returnTree);
    return null;
  }

  @Override
  public Void visitMemberSelect(MemberSelectTree memberSelectTree, VisitorState state) {
    trace(memberSelectTree);
    return null;
  }

  @Override
  public Void visitEmptyStatement(EmptyStatementTree emptyStatementTree, VisitorState state) {
    trace(emptyStatementTree);
    return null;
  }

  @Override
  public Void visitSwitch(SwitchTree switchTree, VisitorState state) {
    trace(switchTree);
    return null;
  }

  @Override
  public Void visitSynchronized(SynchronizedTree synchronizedTree, VisitorState state) {
    trace(synchronizedTree);
    return null;
  }

  @Override
  public Void visitThrow(ThrowTree throwTree, VisitorState state) {
    trace(throwTree);
    return null;
  }

  @Override
  public Void visitCompilationUnit(CompilationUnitTree compilationUnitTree, VisitorState state) {
    trace(compilationUnitTree);
    return null;
  }

  @Override
  public Void visitTry(TryTree tryTree, VisitorState state) {
    trace(tryTree);
    return null;
  }

  @Override
  public Void visitParameterizedType(ParameterizedTypeTree parameterizedTypeTree,
      VisitorState state) {
    trace(parameterizedTypeTree);
    return null;
  }

  @Override
  public Void visitArrayType(ArrayTypeTree arrayTypeTree, VisitorState state) {
    trace(arrayTypeTree);
    return null;
  }

  @Override
  public Void visitTypeCast(TypeCastTree typeCastTree, VisitorState state) {
    trace(typeCastTree);
    return null;
  }

  @Override
  public Void visitPrimitiveType(PrimitiveTypeTree primitiveTypeTree, VisitorState state) {
    trace(primitiveTypeTree);
    return null;
  }

  @Override
  public Void visitTypeParameter(TypeParameterTree typeParameterTree, VisitorState state) {
    trace(typeParameterTree);
    return null;
  }

  @Override
  public Void visitInstanceOf(InstanceOfTree instanceOfTree, VisitorState state) {
    trace(instanceOfTree);
    return null;
  }

  @Override
  public Void visitUnary(UnaryTree unaryTree, VisitorState state) {
    trace(unaryTree);
    return null;
  }

  @Override
  public Void visitVariable(VariableTree variableTree, VisitorState state) {
    trace(variableTree);
    if (variableTree.getInitializer() != null) {
      variableTree.getInitializer().accept(this, state);
    }
    return null;
  }

  @Override
  public Void visitWhileLoop(WhileLoopTree whileLoopTree, VisitorState state) {
    trace(whileLoopTree);
    return null;
  }

  @Override
  public Void visitWildcard(WildcardTree wildcardTree, VisitorState state) {
    trace(wildcardTree);
    return null;
  }

  @Override
  public Void visitOther(Tree tree, VisitorState state) {
    trace(tree);
    return null;
  }
}
