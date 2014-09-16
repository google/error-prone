/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.hasIdentifier;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.matchers.Matchers.nestingKind;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.parentNode;
import static com.google.errorprone.matchers.MultiMatcher.MatchType.ANY;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Types;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;

/**
 * @author alexloh@google.com (Alex Loh)
 */
@BugPattern(name = "ClassCanBeStatic",
    summary = "Inner class is non-static but does not reference enclosing class",
    explanation = "An inner class should be static unless it references members" +
        "of its enclosing class. An inner class that is made non-static unnecessarily" +
        "uses more memory and does not make the intent of the class clear.",
    category = JDK, maturity = EXPERIMENTAL, severity = ERROR)
public class ClassCanBeStatic extends BugChecker implements ClassTreeMatcher {

  /**
   * Matches any class definitions that fit the following:
   * <ol>
   * <li> Is non-static
   * <li> Is an inner class (ie has an enclosing class)
   * <li> Enclosing class is non-nested or static
   * <li> Has no references to variables defined in enclosing class
   * </ol>
   */
  private static Matcher<ClassTree> classTreeMatcher = new Matcher<ClassTree>() {
      @Override
      public boolean matches(ClassTree classTree, VisitorState state) {
        return allOf(
          not(Matchers.<ClassTree>hasModifier(Modifier.STATIC)),
          kindIs(Kind.CLASS),
          nestingKind(NestingKind.MEMBER),
          parentNode(kindIs(Kind.CLASS)),
          anyOf(
              parentNode(nestingKind(NestingKind.TOP_LEVEL)),
              parentNode(Matchers.<ClassTree>hasModifier(Modifier.STATIC))),
          not(hasIdentifier(ANY, referenceEnclosing(classTree, state.getTypes())))
        ).matches(classTree, state);
      }
    };

  private static Matcher<IdentifierTree> referenceEnclosing(ClassTree classTree, Types types) {
    return new ReferenceEnclosing(classTree, types);
  }

  /**
   * Matches an identifier that is declared outside of given classTree
   */
  private static class ReferenceEnclosing implements Matcher<IdentifierTree> {

    private final ClassSymbol currentClass;
    private final Types types;

    public ReferenceEnclosing(ClassTree classTree, Types types) {
      currentClass = (ClassSymbol) ASTHelpers.getSymbol(classTree);
      this.types = types;
    }

    @Override
    public boolean matches(IdentifierTree node, VisitorState state) {
      Symbol sym = ASTHelpers.getSymbol(node);
      return !sym.isLocal() && !sym.isMemberOf(currentClass, types);
    }
  }

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (!classTreeMatcher.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    // figure out where to insert the static modifier
    // if there is other modifier, prepend 'static ' in front of class
    // else insert 'static ' AFTER public/private/protected and BEFORE final
    Fix fix;

    ModifiersTree mods = tree.getModifiers();
    if (mods.getFlags().isEmpty()) {
      fix = SuggestedFix.prefixWith(tree, "static ");
    } else {
      // Note that the use of .toString() here effectively destroys any special
      // formatting, eg if the modifiers previously had multiple spaces or a
      // comment between them, after this fix they will all have exactly one
      // space between each modifier.
      String newmods = mods.toString();
      int ind = newmods.indexOf("final");
      if (ind < 0) {
        // append if 'final' not found
        newmods += "static";
        fix = SuggestedFix.replace(mods, newmods);
      } else {
        // insert at ind, just before 'final'
        newmods = newmods.substring(0, ind) + "static "
                + newmods.substring(ind, newmods.length() - 1);
        fix = SuggestedFix.replace(mods, newmods);
      }
    }

    return describeMatch(tree, fix);
  }
}
