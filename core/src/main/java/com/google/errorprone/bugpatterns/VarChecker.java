/*
 * Copyright 2015 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.util.ASTHelpers.getAnnotationWithSimpleName;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;
import static com.google.errorprone.util.AnnotationNames.VAR_ANNOTATION;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import javax.lang.model.element.Modifier;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "Var",
    summary = "Non-constant variable missing @Var annotation",
    severity = WARNING)
public class VarChecker extends BugChecker implements VariableTreeMatcher {

  private static final String UNNECESSARY_FINAL = "Unnecessary 'final' modifier.";

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    Symbol sym = ASTHelpers.getSymbol(tree);
    if (hasAnnotation(sym, VAR_ANNOTATION, state)) {
      if ((sym.flags() & Flags.EFFECTIVELY_FINAL) != 0) {
        return buildDescription(tree)
            .setMessage("@Var variable is never modified")
            .addFix(
                SuggestedFix.delete(
                    getAnnotationWithSimpleName(tree.getModifiers().getAnnotations(), "Var")))
            .build();
      }
      return Description.NO_MATCH;
    }
    if (!ASTHelpers.getGeneratedBy(state).isEmpty()) {
      return Description.NO_MATCH;
    }
    if (TreeInfo.isReceiverParam((JCTree) tree)) {
      return Description.NO_MATCH;
    }
    if (forLoopVariable(tree, state.getPath())) {
      // for loop indices are implicitly @Var
      // TODO(cushon): consider requiring @Var if the index is modified in the body of the loop
      return Description.NO_MATCH;
    }
    return switch (sym.getKind()) {
      case PARAMETER, LOCAL_VARIABLE, EXCEPTION_PARAMETER, RESOURCE_VARIABLE, BINDING_VARIABLE ->
          handleLocalOrParam(tree, state, sym);
      default -> Description.NO_MATCH;
    };
  }

  boolean forLoopVariable(VariableTree tree, TreePath path) {
    Tree parent = path.getParentPath().getLeaf();
    if (!(parent instanceof ForLoopTree forLoop)) {
      return false;
    }
    return forLoop.getInitializer().contains(tree);
  }

  private Description handleLocalOrParam(VariableTree tree, VisitorState state, Symbol sym) {
    if (sym.getModifiers().contains(Modifier.FINAL)) {
      // The final modifier is never necessary on locals/parameters because the compiler recognizes
      // them as "effectively final" where relevant.
      Optional<SuggestedFix> fix = SuggestedFixes.removeModifiers(tree, state, Modifier.FINAL);
      // The fix may not be present for TWR variables that were not explicitly final
      if (fix.isPresent()) {
        return buildDescription(tree).setMessage(UNNECESSARY_FINAL).addFix(fix.get()).build();
      }
      return Description.NO_MATCH;
    }
    if (!Collections.disjoint(
        sym.owner.getModifiers(), EnumSet.of(Modifier.ABSTRACT, Modifier.NATIVE))) {
      // flow information isn't collected for body-less methods
      return Description.NO_MATCH;
    }
    if (isConsideredFinal(sym)) {
      return Description.NO_MATCH;
    }
    return describeMatch(tree, addVarAnnotation(tree));
  }

  private static Fix addVarAnnotation(VariableTree tree) {
    return SuggestedFix.builder().prefixWith(tree, "@Var ").addImport(VAR_ANNOTATION).build();
  }
}
