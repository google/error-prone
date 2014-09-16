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
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.hasMethod;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.methodHasParameters;
import static com.google.errorprone.matchers.Matchers.methodHasVisibility;
import static com.google.errorprone.matchers.Matchers.methodIsNamed;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.variableType;
import static com.google.errorprone.suppliers.Suppliers.BOOLEAN_TYPE;
import static com.google.errorprone.suppliers.Suppliers.ENCLOSING_CLASS;
import static com.google.errorprone.suppliers.Suppliers.OBJECT_TYPE;
import static com.sun.tools.javac.code.Flags.ENUM;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.MethodVisibility.Visibility;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.Name;

import java.util.List;

/**
 * @author alexeagle@google.com (Alex Eagle)
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@BugPattern(name = "CovariantEquals",
    summary = "equals() method doesn't override Object.equals()",
    explanation = "To be used by many libraries, an `equals` method must override `Object.equals`," +
        "which has a single parameter of type `java.lang.Object`. " +
        "Defining a method which looks like `equals` but doesn't have the same signature is dangerous, " +
        "since comparisons will have different results depending on which `equals` is called.",
    category = JDK, maturity = EXPERIMENTAL, severity = ERROR)
public class CovariantEquals extends BugChecker implements MethodTreeMatcher {

  public static final Matcher<MethodTree> MATCHER = allOf(
      methodHasVisibility(Visibility.PUBLIC),
      methodIsNamed("equals"),
      methodReturns(BOOLEAN_TYPE),
      methodHasParameters(variableType(isSameType(ENCLOSING_CLASS))),
      enclosingClass(not(hasMethod(Matchers.<MethodTree>allOf(
          methodIsNamed("equals"),
          methodReturns(BOOLEAN_TYPE),
          methodHasParameters(variableType(isSameType(OBJECT_TYPE)))))))
  );

  /**
   * Matches any method definitions that fit the following:
   * 1) Defined method is named "equals."
   * 2) Defined method returns a boolean.
   * 3) Defined method takes a single parameter of the same type as the enclosing class.
   * 4) The enclosing class does not have a method defined that really overrides Object.equals().
   */
  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    if (!MATCHER.matches(methodTree, state)) {
      return Description.NO_MATCH;
    }

    SuggestedFix.Builder fix = SuggestedFix.builder();
    JCClassDecl cls = (JCClassDecl) state.findEnclosing(ClassTree.class);

    if ((cls.getModifiers().flags & ENUM) != 0) {
      /* If the enclosing class is an enum, then just delete the equals method since enums
       * should always be compared for reference equality. Enum defines a final equals method for
       * just this reason. */
      fix.delete(methodTree);
    } else {
      /* Otherwise, change the covariant equals method to override Object.equals. */
      JCTree parameterType = (JCTree) methodTree.getParameters().get(0).getType();
      Name parameterName = ((JCVariableDecl) methodTree.getParameters().get(0)).getName();

      // Add @Override annotation if not present.
      boolean hasOverrideAnnotation = false;
      List<JCAnnotation> annotations = ((JCMethodDecl) methodTree).getModifiers().getAnnotations();
      for (JCAnnotation annotation : annotations) {
        if (annotation.annotationType.type.tsym == state.getSymtab().overrideType.tsym) {
          hasOverrideAnnotation = true;
        }
      }
      if (!hasOverrideAnnotation) {
        fix.prefixWith(methodTree, "@Override\n");
      }

      // Change method signature, substituting Object for parameter type.
      fix.replace(parameterType, "Object");

      // If there is a method body...
      if (methodTree.getBody() != null) {

        // Add type check at start
        String typeCheckStmt = "if (!(" + parameterName + " instanceof " + parameterType + ")) {\n"
            + "  return false;\n"
            + "}\n";
        fix.prefixWith(methodTree.getBody().getStatements().get(0), typeCheckStmt);

        // Cast all uses of the parameter name using a recursive TreeScanner.
        new CastScanner().scan(methodTree.getBody(), new CastState(parameterName,
            parameterType.toString(), fix));
      }
    }

    return describeMatch(methodTree, fix.build());
  }

  private static class CastState {
    Name name;
    String castToType;
    SuggestedFix.Builder fix;

    public CastState(Name name, String castToType, SuggestedFix.Builder fix) {
      this.name = name;
      this.castToType = castToType;
      this.fix = fix;
    }
  }

  /**
   * A Scanner used to replace all references to a variable with
   * a casted version.
   */
  private static class CastScanner extends TreeScanner<Void, CastState> {
    @Override
    public Void visitIdentifier(IdentifierTree node, CastState state) {
      if (state.name.equals(node.getName())) {
        state.fix.replace(node, "((" + state.castToType + ") " + state.name + ")");
      }

      return super.visitIdentifier(node, state);
    }
  }
}
