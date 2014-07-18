/*
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.GUICE;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.isType;
import static com.sun.source.tree.Tree.Kind.VARIABLE;
import static javax.lang.model.element.Modifier.FINAL;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.MultiMatcher;
import com.google.errorprone.matchers.MultiMatcher.MatchType;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
@BugPattern(name = "GuiceInjectOnFinalField", summary =
    "Although Guice allows injecting final fields, doing so is not "
    + "recommended because the injected value may not be visible to other threads.",
    explanation = "See https://code.google.com/p/google-guice/wiki/InjectionPoints",
    category = GUICE, severity = WARNING, maturity = EXPERIMENTAL)
@SuppressWarnings("serial")
public class GuiceInjectOnFinalField extends BugChecker implements VariableTreeMatcher {

  private static final String GUICE_INJECT_ANNOTATION = "com.google.inject.Inject";

  private static final MultiMatcher<Tree, AnnotationTree> ANNOTATED_WITH_GUICE_INJECT_MATCHER =
      annotations(MatchType.ANY, isType(GUICE_INJECT_ANNOTATION));

  private static final Matcher<Tree> FINAL_FIELD_MATCHER = new Matcher<Tree>() {
    @Override
    public boolean matches(Tree t, VisitorState state) {
      return isField(t, state) && ((VariableTree) t).getModifiers().getFlags().contains(FINAL);
    }
  };

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    if (ANNOTATED_WITH_GUICE_INJECT_MATCHER.matches(tree, state)
            && FINAL_FIELD_MATCHER.matches(tree, state)) {
      JCModifiers modifiers = ((JCVariableDecl) tree).getModifiers();
      long replacementFlags = modifiers.flags ^ Flags.FINAL;
      JCModifiers replacementModifiers = TreeMaker.instance(state.context)
          .Modifiers(replacementFlags, modifiers.annotations);
      /*
       * replace new lines with strings, trim whitespace and remove empty parens to make the
       * suggested fixes look sane
       */
      String replacementModifiersString =
          replacementModifiers.toString().replace('\n', ' ').replace("()", "").trim();
      return describeMatch(modifiers,
          SuggestedFix.replace(modifiers, replacementModifiersString));
    }
    return Description.NO_MATCH;
  }

  private static boolean isField(Tree tree, VisitorState state) {
    return tree.getKind().equals(VARIABLE)
        && ASTHelpers.findEnclosingNode(state.getPath(), ClassTree.class)
            .getMembers().contains(tree);
  }
}
