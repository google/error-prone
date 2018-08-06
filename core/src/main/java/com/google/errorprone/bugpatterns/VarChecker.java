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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.Var;
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
import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import javax.lang.model.element.Modifier;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "Var",
    summary = "Non-constant variable missing @Var annotation",
    category = JDK,
    severity = WARNING,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class VarChecker extends BugChecker implements VariableTreeMatcher {

  private static final String UNNECESSARY_FINAL = "Unnecessary 'final' modifier.";

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    Symbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null) {
      return Description.NO_MATCH;
    }
    if (ASTHelpers.hasAnnotation(sym, Var.class, state)) {
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
    switch (sym.getKind()) {
      case PARAMETER:
      case LOCAL_VARIABLE:
      case EXCEPTION_PARAMETER:
      case RESOURCE_VARIABLE:
        return handleLocalOrParam(tree, state, sym);
      default:
        return Description.NO_MATCH;
    }
  }

  boolean forLoopVariable(VariableTree tree, TreePath path) {
    Tree parent = path.getParentPath().getLeaf();
    if (!(parent instanceof ForLoopTree)) {
      return false;
    }
    ForLoopTree forLoop = (ForLoopTree) parent;
    return forLoop.getInitializer().contains(tree);
  }

  private Description handleLocalOrParam(VariableTree tree, VisitorState state, Symbol sym) {
    if (sym.getModifiers().contains(Modifier.FINAL)) {
      if (Source.instance(state.context).compareTo(Source.lookup("1.8")) >= 0) {
        // In Java 8, the final modifier is never necessary on locals/parameters because
        // effectively final variables can be used anywhere a final variable is required.
        Optional<SuggestedFix> fix = SuggestedFixes.removeModifiers(tree, state, Modifier.FINAL);
        // The fix may not be present for TWR variables that were not explicitly final
        if (fix.isPresent()) {
          return buildDescription(tree).setMessage(UNNECESSARY_FINAL).addFix(fix.get()).build();
        }
      }
      return Description.NO_MATCH;
    }
    if (!Collections.disjoint(
        sym.owner.getModifiers(), EnumSet.of(Modifier.ABSTRACT, Modifier.NATIVE))) {
      // flow information isn't collected for body-less methods
      return Description.NO_MATCH;
    }
    if ((sym.flags() & (Flags.EFFECTIVELY_FINAL | Flags.FINAL)) != 0) {
      return Description.NO_MATCH;
    }
    return describeMatch(tree, addVarAnnotation(tree));
  }

  private static Fix addVarAnnotation(VariableTree tree) {
    return SuggestedFix.builder().prefixWith(tree, "@Var ").addImport(Var.class.getName()).build();
  }
}
