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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.hasMethod;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.isStatic;
import static com.google.errorprone.matchers.Matchers.methodHasParameters;
import static com.google.errorprone.matchers.Matchers.methodHasVisibility;
import static com.google.errorprone.matchers.Matchers.methodIsNamed;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.variableType;
import static com.google.errorprone.suppliers.Suppliers.BOOLEAN_TYPE;
import static com.google.errorprone.suppliers.Suppliers.JAVA_LANG_BOOLEAN_TYPE;
import static com.google.errorprone.suppliers.Suppliers.OBJECT_TYPE;
import static com.sun.tools.javac.code.Flags.ENUM;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.MethodVisibility.Visibility;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.Name;

/** Bug checker for equals methods that don't actually override equals. */
@BugPattern(
  name = "NonOverridingEquals",
  summary = "equals method doesn't override Object.equals",
  category = JDK,
  severity = WARNING,
  tags = StandardTags.FRAGILE_CODE,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class NonOverridingEquals extends BugChecker implements MethodTreeMatcher {

  private static final String MESSAGE_BASE = "equals method doesn't override Object.equals";

  /**
   * Matches any method definition that: 1) is named `equals` 2) takes a single argument of a type
   * other than Object 3) returns a boolean or Boolean
   */
  private static final Matcher<MethodTree> MATCHER =
      allOf(
          methodIsNamed("equals"),
          methodHasParameters(variableType(not(isSameType("java.lang.Object")))),
          anyOf(methodReturns(BOOLEAN_TYPE), methodReturns(JAVA_LANG_BOOLEAN_TYPE)));

  /** Matches if the enclosing class overrides Object#equals. */
  private static final Matcher<MethodTree> enclosingClassOverridesEquals =
      enclosingClass(
          hasMethod(
              allOf(
                  methodIsNamed("equals"),
                  methodReturns(BOOLEAN_TYPE),
                  methodHasParameters(variableType(isSameType(OBJECT_TYPE))),
                  not(isStatic()))));

  /**
   * Matches method declarations for which we cannot provide a fix. Our default fix rewrites the
   * equals method to override Object.equals. In these (uncommon) cases, our rewrite algorithm
   * doesn't work:
   *
   * <ul>
   *   <li>the method is static
   *   <li>the method is not public
   *   <li>the method returns a boxed Boolean
   * </ul>
   */
  private static final Matcher<MethodTree> noFixMatcher =
      anyOf(
          isStatic(),
          not(methodHasVisibility(Visibility.PUBLIC)),
          methodReturns(JAVA_LANG_BOOLEAN_TYPE));

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    if (!MATCHER.matches(methodTree, state)) {
      return Description.NO_MATCH;
    }

    // If an overriding equals method has already been defined in the enclosing class, assume
    // this is a type-specific helper method and give advice to either inline it or rename it.
    if (enclosingClassOverridesEquals.matches(methodTree, state)) {
      return buildDescription(methodTree)
          .setMessage(
              MESSAGE_BASE
                  + "; if this is a type-specific helper for a method that does"
                  + " override Object.equals, either inline it into the callers or rename it to"
                  + " avoid ambiguity")
          .build();
    }

    // Don't provide a fix if the method is static, non-public, or returns a boxed Boolean
    if (noFixMatcher.matches(methodTree, state)) {
      return describeMatch(methodTree);
    }

    JCClassDecl cls = (JCClassDecl) state.findEnclosing(ClassTree.class);

    if ((cls.getModifiers().flags & ENUM) != 0) {
      /* If the enclosing class is an enum, then just delete the equals method since enums
       * should always be compared for reference equality. Enum defines a final equals method for
       * just this reason. */
      return buildDescription(methodTree)
          .setMessage(
              MESSAGE_BASE
                  + "; enum instances can safely be compared by reference "
                  + "equality, so please delete this")
          .addFix(SuggestedFix.delete(methodTree))
          .build();
    } else {
      /* Otherwise, change the covariant equals method to override Object.equals. */

      SuggestedFix.Builder fix = SuggestedFix.builder();

      // Add @Override annotation if not present.
      if (ASTHelpers.getAnnotation(methodTree, Override.class) == null) {
        fix.prefixWith(methodTree, "@Override\n");
      }

      // Change method signature, substituting Object for parameter type.
      JCTree parameterType = (JCTree) methodTree.getParameters().get(0).getType();
      Name parameterName = ((JCVariableDecl) methodTree.getParameters().get(0)).getName();
      fix.replace(parameterType, "Object");

      // If there is a method body...
      if (methodTree.getBody() != null) {

        // Add type check at start
        String typeCheckStmt =
            "if (!("
                + parameterName
                + " instanceof "
                + parameterType
                + ")) {\n"
                + "  return false;\n"
                + "}\n";
        fix.prefixWith(methodTree.getBody().getStatements().get(0), typeCheckStmt);

        // Cast all uses of the parameter name using a recursive TreeScanner.
        new CastScanner()
            .scan(
                methodTree.getBody(), new CastState(parameterName, parameterType.toString(), fix));
      }

      return describeMatch(methodTree, fix.build());
    }
  }

  private static class CastState {
    final Name name;
    final String castToType;
    final SuggestedFix.Builder fix;

    public CastState(Name name, String castToType, SuggestedFix.Builder fix) {
      this.name = name;
      this.castToType = castToType;
      this.fix = fix;
    }
  }

  /** A Scanner used to replace all references to a variable with a casted version. */
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
