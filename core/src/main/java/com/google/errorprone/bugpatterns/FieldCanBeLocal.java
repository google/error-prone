/*
 * Copyright 2019 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.util.ASTHelpers.getAnnotation;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.tree.JCTree;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.ElementKind;

/** Flags fields which can be replaced with local variables. */
@BugPattern(
    name = "FieldCanBeLocal",
    altNames = {"unused", "Unused"},
    summary = "This field can be replaced with a local variable in the methods that use it.",
    severity = SUGGESTION,
    documentSuppression = false)
public final class FieldCanBeLocal extends BugChecker implements CompilationUnitTreeMatcher {
  private static final ImmutableSet<ElementType> VALID_ON_LOCAL_VARIABLES =
      Sets.immutableEnumSet(ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE);

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    Map<VarSymbol, TreePath> potentialFields = new HashMap<>();
    Multimap<VarSymbol, TreePath> unconditionalAssignments = HashMultimap.create();
    Multimap<VarSymbol, Tree> uses = HashMultimap.create();

    new SuppressibleTreePathScanner<Void, Void>() {
      @Override
      public Void visitVariable(VariableTree variableTree, Void unused) {
        VarSymbol symbol = getSymbol(variableTree);
        if (symbol != null
            && symbol.getKind() == ElementKind.FIELD
            && symbol.isPrivate()
            && canBeLocal(variableTree)
        ) {
          potentialFields.put(symbol, getCurrentPath());
        }
        return null;
      }

      private boolean canBeLocal(VariableTree variableTree) {
        if (variableTree.getModifiers() == null) {
          return true;
        }
        return variableTree.getModifiers().getAnnotations().stream()
            .allMatch(this::canBeUsedOnLocalVariable);
      }

      private boolean canBeUsedOnLocalVariable(AnnotationTree annotationTree) {
        // TODO(b/137842683): Should this (and all other places using getAnnotation with Target) be
        // replaced with annotation mirror traversals?
        // This is safe given we know that Target does not have Class fields.
        Target target = getAnnotation(annotationTree, Target.class);
        if (target == null) {
          return true;
        }
        return !Sets.intersection(VALID_ON_LOCAL_VARIABLES, ImmutableSet.copyOf(target.value()))
            .isEmpty();
      }

    }.scan(state.getPath(), null);

    new TreePathScanner<Void, Void>() {
      boolean inMethod = false;

      @Override
      public Void visitClass(ClassTree classTree, Void unused) {
        if (isSuppressed(classTree)) {
          return null;
        }
        inMethod = false;
        return super.visitClass(classTree, unused);
      }

      @Override
      public Void visitMethod(MethodTree methodTree, Void unused) {
        if (methodTree.getBody() == null) {
          return null;
        }
        handleMethodLike(new TreePath(getCurrentPath(), methodTree.getBody()));

        inMethod = true;
        super.visitMethod(methodTree, null);
        inMethod = false;
        return null;
      }

      @Override
      public Void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree, Void unused) {
        if (lambdaExpressionTree.getBody() == null) {
          return null;
        }
        handleMethodLike(new TreePath(getCurrentPath(), lambdaExpressionTree.getBody()));
        inMethod = true;
        super.visitLambdaExpression(lambdaExpressionTree, null);
        inMethod = false;
        return null;
      }

      private void handleMethodLike(TreePath treePath) {
        int depth = Iterables.size(getCurrentPath());
        new TreePathScanner<Void, Void>() {
          Set<VarSymbol> unconditionallyAssigned = new HashSet<>();

          @Override
          public Void visitAssignment(AssignmentTree assignmentTree, Void unused) {
            scan(assignmentTree.getExpression(), null);
            Symbol symbol = getSymbol(assignmentTree.getVariable());
            if (!(symbol instanceof VarSymbol)) {
              return scan(assignmentTree.getVariable(), null);
            }
            VarSymbol varSymbol = (VarSymbol) symbol;
            if (!potentialFields.containsKey(varSymbol)) {
              return scan(assignmentTree.getVariable(), null);
            }
            // An unconditional assignment in a MethodTree is three levels deeper than the
            // MethodTree itself.
            if (Iterables.size(getCurrentPath()) == depth + 3) {
              unconditionallyAssigned.add(varSymbol);
              unconditionalAssignments.put(varSymbol, getCurrentPath());
            }
            return scan(assignmentTree.getVariable(), null);
          }

          @Override
          public Void visitIdentifier(IdentifierTree identifierTree, Void unused) {
            handleIdentifier(identifierTree);
            return super.visitIdentifier(identifierTree, null);
          }

          @Override
          public Void visitMemberSelect(MemberSelectTree memberSelectTree, Void unused) {
            handleIdentifier(memberSelectTree);
            return super.visitMemberSelect(memberSelectTree, null);
          }

          private void handleIdentifier(Tree tree) {
            Symbol symbol = getSymbol(tree);
            if (!(symbol instanceof VarSymbol)) {
              return;
            }
            VarSymbol varSymbol = (VarSymbol) symbol;
            uses.put(varSymbol, tree);
            if (!unconditionallyAssigned.contains(varSymbol)) {
              potentialFields.remove(varSymbol);
            }
          }

          @Override
          public Void visitMethod(MethodTree methodTree, Void unused) {
            return null;
          }

          @Override
          public Void visitLambdaExpression(
              LambdaExpressionTree lambdaExpressionTree, Void unused) {
            return null;
          }
        }.scan(treePath, null);
      }

      @Override
      public Void visitIdentifier(IdentifierTree identifierTree, Void unused) {
        if (!inMethod) {
          potentialFields.remove(getSymbol(identifierTree));
        }
        return null;
      }

      @Override
      public Void visitMemberSelect(MemberSelectTree memberSelectTree, Void unused) {
        if (!inMethod) {
          potentialFields.remove(getSymbol(memberSelectTree));
        }
        return super.visitMemberSelect(memberSelectTree, null);
      }
    }.scan(state.getPath(), null);

    for (Map.Entry<VarSymbol, TreePath> entry : potentialFields.entrySet()) {
      VarSymbol varSymbol = entry.getKey();
      TreePath declarationSite = entry.getValue();

      Collection<TreePath> assignmentLocations = unconditionalAssignments.get(varSymbol);
      if (assignmentLocations.isEmpty()) {
        continue;
      }
      SuggestedFix.Builder fix = SuggestedFix.builder();
      VariableTree variableTree = (VariableTree) declarationSite.getLeaf();
      String type = state.getSourceForNode(variableTree.getType());
      String annotations = getAnnotationSource(state, variableTree);
      fix.delete(declarationSite.getLeaf());
      Set<Tree> deletedTrees = new HashSet<>();
      for (TreePath assignmentSite : assignmentLocations) {
        AssignmentTree assignmentTree = (AssignmentTree) assignmentSite.getLeaf();
        Symbol rhsSymbol = getSymbol(assignmentTree.getExpression());

        // If the RHS of the assignment is a variable with the same name as the field, just remove
        // the assignment.
        String assigneeName = getSymbol(assignmentTree.getVariable()).getSimpleName().toString();
        if (rhsSymbol != null
            && assignmentTree.getExpression() instanceof IdentifierTree
            && rhsSymbol.getSimpleName().contentEquals(assigneeName)) {
          deletedTrees.add(assignmentTree.getVariable());
          fix.delete(assignmentSite.getParentPath().getLeaf());
        } else {
          fix.prefixWith(assignmentSite.getLeaf(), annotations + " " + type + " ");
        }
      }
      // Strip "this." off any uses of the field.
      for (Tree usage : uses.get(varSymbol)) {
        if (deletedTrees.contains(usage)
            || usage.getKind() == Kind.IDENTIFIER
            || usage.getKind() != Kind.MEMBER_SELECT) {
          continue;
        }
        ExpressionTree selected = ((MemberSelectTree) usage).getExpression();
        if (!(selected instanceof IdentifierTree)) {
          continue;
        }
        IdentifierTree ident = (IdentifierTree) selected;
        if (ident.getName().contentEquals("this")) {
          fix.replace(((JCTree) ident).getStartPosition(), state.getEndPosition(ident) + 1, "");
        }
      }
      state.reportMatch(describeMatch(declarationSite.getLeaf(), fix.build()));
    }
    return Description.NO_MATCH;
  }

  private static String getAnnotationSource(VisitorState state, VariableTree variableTree) {
    List<? extends AnnotationTree> annotations = variableTree.getModifiers().getAnnotations();
    if (annotations == null || annotations.isEmpty()) {
      return "";
    }
    return state
        .getSourceCode()
        .subSequence(
            ((JCTree) annotations.get(0)).getStartPosition(),
            state.getEndPosition(getLast(annotations)))
        .toString();
  }
}
