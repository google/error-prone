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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.classHasModifier;
import static com.google.errorprone.matchers.Matchers.hasIdentifier;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.parentNode;
import static com.google.errorprone.matchers.MultiMatcher.MatchType.ANY;
import static com.sun.source.tree.Tree.Kind.CLASS;
import static javax.lang.model.element.Modifier.STATIC;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Types;

/**
 * @author alexloh@google.com (Alex Loh)
 */
@BugPattern(name = "NonStaticInnerClass",
    summary = "Inner class is non-static but does not reference enclosing class",
    explanation = "An inner class should be static unless it references members" +
        "of its enclosing class. An inner class that is made non-static unnecessarily" +
        "uses more memory and does not make the intent of the class clear.",
    category = JDK, maturity = EXPERIMENTAL, severity = WARNING)
public class NonStaticInnerClass extends DescribingMatcher<ClassTree> {

  /**
   * Matches any class definitions that fit the following:
   * 1) Is non-static
   * 2) Is an inner class (ie has an enclosing class)
   * 3) Enclosing class is non-nested or static
   * 4) Has no references to variables defined in enclosing class
   */
  @Override
  public boolean matches(ClassTree classTree, VisitorState state) {
    return allOf(
      not(classHasModifier(STATIC)),
      parentNode(kindIs(CLASS)),
      anyOf(not(parentNode(parentNode(kindIs(CLASS)))), parentNode(classHasModifier(STATIC))),
      kindIs(CLASS),
      not(hasIdentifier(ANY, referenceEnclosing(classTree, state.getTypes())))
    ).matches(classTree, state);
  }

  @Override
  public Description describe(ClassTree classTree, VisitorState state) {
    // figure out where to insert the static modifier
    // if there is other modifer, prepend 'static ' in front of class
    // else insert 'static ' AFTER public/private/protected and BEFORE final
    SuggestedFix fix = new SuggestedFix();

    ModifiersTree mods = classTree.getModifiers();
    if (mods.getFlags().isEmpty()) {
      fix.prefixWith(classTree, "static ");
    } else {
      String newmods = mods.toString();
      int ind = newmods.indexOf("final");
      if (ind < 0) {
        // append if 'final' not found
        newmods += "static";
        fix.replace(mods, newmods);
      } else {
        // insert at ind, just before 'final'
        newmods = newmods.substring(0, ind) + "static " 
                + newmods.substring(ind, newmods.length() - 1);
        fix.replace(mods, newmods);
      }
      fix.replace(mods, newmods);
    }

    return new Description(classTree, getDiagnosticMessage(), fix);
  }
  
  private Matcher<IdentifierTree> referenceEnclosing(ClassTree classTree, Types types) {
    return new ReferenceEnclosing(classTree, types);
  }

  /**
   * Matches an identifier that is declared outside of given classTree
   */
  private class ReferenceEnclosing implements Matcher<IdentifierTree> {

    private final ClassSymbol currentClass;
    private final Types types;

    public ReferenceEnclosing(ClassTree classTree, Types types) {
      currentClass = (ClassSymbol) ASTHelpers.getSymbol(classTree);
      this.types = types;
    }

    @Override
    public boolean matches(IdentifierTree node, VisitorState state) {

      Symbol sym = ASTHelpers.getSymbol(node);
      ClassSymbol nodeOwner = sym.enclClass();
      return !sym.isLocal() && !sym.isMemberOf(currentClass, types);
    }
  }

  /**
   * Scanner for NonStaticInnerClass
   * 
   * @author alexloh@google.com (Alex Loh)
   */
  public static class Scanner extends com.google.errorprone.Scanner {
    private final DescribingMatcher<ClassTree> matcher = new NonStaticInnerClass();

    @Override
    public Void visitClass(ClassTree node, VisitorState visitorState) {
      evaluateMatch(node, visitorState, matcher);
      return super.visitClass(node, visitorState);
    }
  }
}
