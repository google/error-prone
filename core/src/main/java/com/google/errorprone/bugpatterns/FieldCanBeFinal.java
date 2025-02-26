/*
 * Copyright 2018 The Error Prone Authors.
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
package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.util.ASTHelpers.canBeRemoved;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.shouldKeep;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/**
 * @author Liam Miller-Cushon (cushon@google.com)
 */
@BugPattern(
    summary = "This field is only assigned during initialization; consider making it final",
    severity = SUGGESTION)
public class FieldCanBeFinal extends BugChecker implements CompilationUnitTreeMatcher {

  /** Annotations that imply a field is non-constant. */
  // TODO(cushon): consider supporting @Var as a meta-annotation
  private static final ImmutableSet<String> IMPLICIT_VAR_ANNOTATIONS =
      ImmutableSet.of(
          // keep-sorted start
          "com.beust.jcommander.Parameter",
          "com.google.common.annotations.NonFinalForGwt",
          "com.google.errorprone.annotations.Var",
          "com.google.gwt.uibinder.client.UiField",
          "com.google.inject.Inject",
          "com.google.inject.testing.fieldbinder.Bind",
          "com.google.testing.junit.testparameterinjector.TestParameter",
          "jakarta.inject.Inject",
          "jakarta.jdo.annotations.Persistent",
          "jakarta.persistence.Id",
          "jakarta.xml.bind.annotation.XmlAttribute",
          "javax.inject.Inject",
          "javax.jdo.annotations.Persistent",
          "javax.persistence.Id",
          "javax.xml.bind.annotation.XmlAttribute",
          "org.kohsuke.args4j.Argument",
          "org.kohsuke.args4j.Option",
          "org.mockito.Spy",
          "picocli.CommandLine.Option"
          // keep-sorted end
          );

  private static final String OBJECTIFY_PREFIX = "com.googlecode.objectify.";

  /**
   * Annotations that imply a field is non-constant, and that do not have a canonical
   * implementation. Instead, we match on any annotation with one of the following simple names.
   */
  private static final ImmutableSet<String> IMPLICIT_VAR_ANNOTATION_SIMPLE_NAMES =
      ImmutableSet.of("NonFinalForTesting", "NotFinalForTesting");

  /** Unary operator kinds that implicitly assign to their operand. */
  private static final ImmutableSet<Kind> UNARY_ASSIGNMENT =
      Sets.immutableEnumSet(
          Kind.PREFIX_DECREMENT,
          Kind.POSTFIX_DECREMENT,
          Kind.PREFIX_INCREMENT,
          Kind.POSTFIX_INCREMENT);

  /** The initialization context where an assignment occurred. */
  private enum InitializationContext {
    /** A class (static) initializer. */
    STATIC,
    /** An instance initializer. */
    INSTANCE,
    /** Neither a static or instance initializer. */
    NONE
  }

  /** A record of all assignments to variables in the current compilation unit. */
  private static class VariableAssignmentRecords {

    private final Map<VarSymbol, VariableAssignments> assignments = new LinkedHashMap<>();

    /** Returns all {@link VariableAssignments} in the current compilation unit. */
    private Collection<VariableAssignments> getAssignments() {
      return assignments.values();
    }

    /** Records an assignment to a variable. */
    private void recordAssignment(Tree tree, InitializationContext init) {
      Symbol sym = ASTHelpers.getSymbol(tree);
      if (sym != null && sym.getKind() == ElementKind.FIELD) {
        recordAssignment((VarSymbol) sym, init);
      }
    }

    /** Records an assignment to a variable. */
    private void recordAssignment(VarSymbol sym, InitializationContext init) {
      getDeclaration(sym).recordAssignment(init);
    }

    private VariableAssignments getDeclaration(VarSymbol sym) {
      return assignments.computeIfAbsent(sym, VariableAssignments::new);
    }

    /** Records a variable declaration. */
    private void recordDeclaration(VarSymbol sym, VariableTree tree) {
      getDeclaration(sym).recordDeclaration(tree);
    }
  }

  /** A record of all assignments to a specific variable in the current compilation unit. */
  private static class VariableAssignments {

    private final VarSymbol sym;
    private final EnumSet<InitializationContext> writes =
        EnumSet.noneOf(InitializationContext.class);
    private VariableTree declaration;

    VariableAssignments(VarSymbol sym) {
      this.sym = sym;
    }

    /** Records an assignment to the variable. */
    private void recordAssignment(InitializationContext init) {
      writes.add(init);
    }

    /** Records that a variable was declared in this compilation unit. */
    private void recordDeclaration(VariableTree tree) {
      declaration = tree;
    }

    /** Returns true if the variable is effectively final. */
    private boolean isEffectivelyFinal() {
      if (declaration == null) {
        return false;
      }
      if (sym.getModifiers().contains(Modifier.FINAL)) {
        // actually final != effectively final
        return false;
      }
      if (writes.contains(InitializationContext.NONE)) {
        return false;
      }
      // The unsound heuristic for effectively final fields is that they are initialized at least
      // once in an initializer with the right static-ness. Multiple initializations are allowed
      // because we don't consider control flow, and zero initializations are allowed to handle
      // class and instance initializers, and delegating constructors that don't initialize the
      // field directly.
      InitializationContext wanted;
      InitializationContext other;
      if (sym.isStatic()) {
        wanted = InitializationContext.STATIC;
        other = InitializationContext.INSTANCE;
      } else {
        wanted = InitializationContext.INSTANCE;
        other = InitializationContext.STATIC;
      }
      if (writes.contains(other)) {
        return false;
      }
      return writes.contains(wanted) || (sym.flags() & Flags.HASINIT) == Flags.HASINIT;
    }

    private VariableTree declaration() {
      return declaration;
    }
  }

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    VariableAssignmentRecords writes = new VariableAssignmentRecords();
    new FinalScanner(writes, state).scan(state.getPath(), InitializationContext.NONE);
    for (VariableAssignments var : writes.getAssignments()) {
      if (!var.isEffectivelyFinal()) {
        continue;
      }
      if (!canBeRemoved(var.sym)) {
        continue;
      }
      if (shouldKeep(var.declaration)) {
        continue;
      }
      if (IMPLICIT_VAR_ANNOTATIONS.stream().anyMatch(a -> hasAnnotation(var.sym, a, state))) {
        continue;
      }
      for (Attribute.Compound anno : var.sym.getAnnotationMirrors()) {
        TypeElement annoElement = (TypeElement) anno.getAnnotationType().asElement();
        if (IMPLICIT_VAR_ANNOTATION_SIMPLE_NAMES.contains(annoElement.getSimpleName().toString())) {
          return Description.NO_MATCH;
        }
        if (annoElement.getQualifiedName().toString().startsWith(OBJECTIFY_PREFIX)) {
          return Description.NO_MATCH;
        }
      }
      VariableTree varDecl = var.declaration();
      SuggestedFixes.addModifiers(varDecl, state, Modifier.FINAL)
          .filter(f -> SuggestedFixes.compilesWithFix(f, state))
          .ifPresent(f -> state.reportMatch(describeMatch(varDecl, f)));
    }
    return Description.NO_MATCH;
  }

  /** Record assignments to possibly-final variables in a compilation unit. */
  private class FinalScanner extends TreePathScanner<Void, InitializationContext> {

    private final VariableAssignmentRecords writes;
    private final VisitorState compilationState;

    private FinalScanner(VariableAssignmentRecords writes, VisitorState compilationState) {
      this.writes = writes;
      this.compilationState = compilationState;
    }

    @Override
    public Void visitVariable(VariableTree node, InitializationContext init) {
      VarSymbol sym = ASTHelpers.getSymbol(node);
      if (sym.getKind() == ElementKind.FIELD && !isSuppressed(node, compilationState)) {
        writes.recordDeclaration(sym, node);
      }
      return super.visitVariable(node, InitializationContext.NONE);
    }

    @Override
    public Void visitLambdaExpression(
        LambdaExpressionTree lambdaExpressionTree, InitializationContext init) {
      // reset the initialization context when entering lambda
      return super.visitLambdaExpression(lambdaExpressionTree, InitializationContext.NONE);
    }

    @Override
    public Void visitBlock(BlockTree node, InitializationContext init) {
      if (getCurrentPath().getParentPath().getLeaf().getKind() == Kind.CLASS) {
        init = node.isStatic() ? InitializationContext.STATIC : InitializationContext.INSTANCE;
      }
      return super.visitBlock(node, init);
    }

    @Override
    public Void visitMethod(MethodTree node, InitializationContext init) {
      MethodSymbol sym = ASTHelpers.getSymbol(node);
      if (sym.isConstructor()) {
        init = InitializationContext.INSTANCE;
      }
      return super.visitMethod(node, init);
    }

    @Override
    public Void visitAssignment(AssignmentTree node, InitializationContext init) {
      if (init == InitializationContext.INSTANCE && !isThisAccess(node.getVariable())) {
        // don't record assignments in initializers that aren't to members of the object
        // being initialized
        init = InitializationContext.NONE;
      }
      writes.recordAssignment(node.getVariable(), init);
      return super.visitAssignment(node, init);
    }

    private boolean isThisAccess(Tree tree) {
      if (tree.getKind() == Kind.IDENTIFIER) {
        return true;
      }
      if (tree.getKind() != Kind.MEMBER_SELECT) {
        return false;
      }
      ExpressionTree selected = ((MemberSelectTree) tree).getExpression();
      return selected instanceof IdentifierTree ident && ident.getName().contentEquals("this");
    }

    @Override
    public Void visitClass(ClassTree node, InitializationContext init) {
      VisitorState state = compilationState.withPath(getCurrentPath());

      if (isSuppressed(node, state)) {
        return null;
      }

      for (Attribute.Compound anno : getSymbol(node).getAnnotationMirrors()) {
        TypeElement annoElement = (TypeElement) anno.getAnnotationType().asElement();
        if (annoElement.getQualifiedName().toString().startsWith(OBJECTIFY_PREFIX)) {
          return null;
        }
      }

      // reset the initialization context when entering a new declaration
      return super.visitClass(node, InitializationContext.NONE);
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, InitializationContext init) {
      init = InitializationContext.NONE;
      writes.recordAssignment(node.getVariable(), init);
      return super.visitCompoundAssignment(node, init);
    }

    @Override
    public Void visitUnary(UnaryTree node, InitializationContext init) {
      if (UNARY_ASSIGNMENT.contains(node.getKind())) {
        init = InitializationContext.NONE;
        writes.recordAssignment(node.getExpression(), init);
      }
      return super.visitUnary(node, init);
    }
  }
}
