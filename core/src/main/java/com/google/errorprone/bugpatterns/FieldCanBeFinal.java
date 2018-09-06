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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.util.Context;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

/** @author Liam Miller-Cushon (cushon@google.com) */
@BugPattern(
    name = "FieldCanBeFinal",
    category = JDK,
    summary = "This field is only assigned during initialization; consider making it final",
    severity = SUGGESTION,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class FieldCanBeFinal extends BugChecker implements CompilationUnitTreeMatcher {

  /** Annotations that imply a field is non-constant. */
  // TODO(cushon): consider supporting @Var as a meta-annotation
  private static final ImmutableSet<String> IMPLICIT_VAR_ANNOTATIONS =
      ImmutableSet.of(
          "javax.inject.Inject",
          "com.google.inject.Inject",
          "com.google.inject.testing.fieldbinder.Bind",
          "com.googlecode.objectify.v5.annotation.Collection",
          "com.googlecode.objectify.v5.annotation.Id",
          "com.googlecode.objectify.v5.annotation.Index",
          "com.googlecode.objectify.v5.annotation.Parent",
          "com.googlecode.objectify.v5.annotation.Subclass",
          "com.google.errorprone.annotations.Var",
          "com.google.common.annotations.NonFinalForGwt",
          "org.kohsuke.args4j.Argument",
          "org.kohsuke.args4j.Option",
          "org.mockito.Spy",
          "javax.jdo.annotations.Persistent",
          "javax.xml.bind.annotation.XmlAttribute",
          "com.google.gwt.uibinder.client.UiField",
          "com.beust.jcommander.Parameter",
          "javax.persistence.Id");

  /** Annotations that imply all fields in the annotated class are non-constant. */
  private static final ImmutableSet<String> IMPLICIT_VAR_CLASS_ANNOTATIONS =
      ImmutableSet.of(
          "com.googlecode.objectify.v4.annotation.Entity",
          "com.googlecode.objectify.v4.annotation.Embed",
          "com.googlecode.objectify.v5.annotation.Entity",
          "com.googlecode.objectify.v5.annotation.Embed");

  /**
   * Annotations that imply a field is non-constant, and that do not have a canonical
   * implementation. Instead, we match on any annotation with one of the following simple names.
   */
  private static final ImmutableSet<String> IMPLICIT_VAR_ANNOTATION_SIMPLE_NAMES =
      ImmutableSet.of("NonFinalForTesting", "NotFinalForTesting");

  /** Unary operator kinds that implicitly assign to their operand. */
  private static final EnumSet<Kind> UNARY_ASSIGNMENT =
      EnumSet.of(
          Kind.PREFIX_DECREMENT,
          Kind.POSTFIX_DECREMENT,
          Kind.PREFIX_INCREMENT,
          Kind.POSTFIX_INCREMENT);

  /** The initalization context where an assignment occurred. */
  enum InitializationContext {
    /** A class (static) initializer. */
    STATIC,
    /** An instance initializer. */
    INSTANCE,
    /** Neither a static or instance initializer. */
    NONE
  }

  /** A record of all assignments to variables in the current compilation unit. */
  static class VariableAssignmentRecords {

    private final Map<VarSymbol, VariableAssignments> assignments = new LinkedHashMap<>();

    /** Returns all {@link VariableAssignments} in the current compilation unit. */
    public Iterable<VariableAssignments> getAssignments() {
      return assignments.values();
    }

    /** Records an assignment to a variable. */
    public void recordAssignment(Tree tree, InitializationContext init) {
      Symbol sym = ASTHelpers.getSymbol(tree);
      if (sym != null && sym.getKind() == ElementKind.FIELD) {
        recordAssignment((VarSymbol) sym, init);
      }
    }

    /** Records an assignment to a variable. */
    public void recordAssignment(VarSymbol sym, InitializationContext init) {
      getDeclaration(sym).recordAssignment(init);
    }

    private VariableAssignments getDeclaration(VarSymbol sym) {
      VariableAssignments info = assignments.get(sym);
      if (info == null) {
        info = new VariableAssignments(sym);
        assignments.put(sym, info);
      }
      return info;
    }

    /** Records a variable declaration. */
    public void recordDeclaration(VarSymbol sym, VariableTree tree) {
      getDeclaration(sym).recordDeclaration(tree);
    }
  }

  /** A record of all assignments to a specific variable in the current compilation unit. */
  static class VariableAssignments {

    final VarSymbol sym;
    final EnumSet<InitializationContext> writes = EnumSet.noneOf(InitializationContext.class);
    VariableTree declaration;

    VariableAssignments(VarSymbol sym) {
      this.sym = sym;
    }

    /** Records an assignment to the variable. */
    public void recordAssignment(InitializationContext init) {
      writes.add(init);
    }

    /** Records that a variable was declared in this compilation unit. */
    public void recordDeclaration(VariableTree tree) {
      declaration = tree;
    }

    /** Returns true if the variable is effectively final. */
    boolean isEffectivelyFinal() {
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

    VariableTree declaration() {
      return declaration;
    }
  }

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    VariableAssignmentRecords writes = new VariableAssignmentRecords();
    new FinalScanner(writes, state.context).scan(state.getPath(), InitializationContext.NONE);
    outer:
    for (VariableAssignments var : writes.getAssignments()) {
      if (!var.isEffectivelyFinal()) {
        continue;
      }
      if (!var.sym.isPrivate()) {
        continue;
      }
      for (String annotation : IMPLICIT_VAR_ANNOTATIONS) {
        if (ASTHelpers.hasAnnotation(var.sym, annotation, state)) {
          continue outer;
        }
      }
      VariableTree varDecl = var.declaration();
      for (AnnotationTree anno : varDecl.getModifiers().getAnnotations()) {
        if (IMPLICIT_VAR_ANNOTATION_SIMPLE_NAMES.contains(ASTHelpers.getAnnotationName(anno))) {
          return Description.NO_MATCH;
        }
      }
      SuggestedFixes.addModifiers(varDecl, state, Modifier.FINAL)
          .ifPresent(
              f -> {
                if (SuggestedFixes.compilesWithFix(f, state)) {
                  state.reportMatch(describeMatch(varDecl, f));
                }
              });
    }
    return Description.NO_MATCH;
  }

  /** Record assignments to possibly-final variables in a compilation unit. */
  private class FinalScanner extends TreePathScanner<Void, InitializationContext> {

    private final VariableAssignmentRecords writes;
    private final Context context;

    public FinalScanner(VariableAssignmentRecords writes, Context context) {
      this.writes = writes;
      this.context = context;
    }

    @Override
    public Void visitVariable(VariableTree node, InitializationContext init) {
      VarSymbol sym = ASTHelpers.getSymbol(node);
      if (sym.getKind() == ElementKind.FIELD && !isSuppressed(node)) {
        writes.recordDeclaration(sym, node);
      }
      return super.visitVariable(node, InitializationContext.NONE);
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
      if (sym != null && sym.isConstructor()) {
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

    boolean isThisAccess(Tree tree) {
      if (tree.getKind() == Kind.IDENTIFIER) {
        return true;
      }
      if (tree.getKind() != Kind.MEMBER_SELECT) {
        return false;
      }
      ExpressionTree selected = ((MemberSelectTree) tree).getExpression();
      if (!(selected instanceof IdentifierTree)) {
        return false;
      }
      IdentifierTree ident = (IdentifierTree) selected;
      return ident.getName().contentEquals("this");
    }

    @Override
    public Void visitClass(ClassTree node, InitializationContext init) {
      VisitorState state = new VisitorState(context).withPath(getCurrentPath());

      if (isSuppressed(node)) {
        return null;
      }


      for (String annotation : IMPLICIT_VAR_CLASS_ANNOTATIONS) {
        if (ASTHelpers.hasAnnotation(getSymbol(node), annotation, state)) {
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
