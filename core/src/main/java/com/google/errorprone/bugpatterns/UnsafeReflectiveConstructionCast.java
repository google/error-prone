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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.TypeCastTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import java.lang.reflect.Constructor;

/**
 * Checks unsafe instance creation via reflection.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    name = "UnsafeReflectiveConstructionCast",
    summary =
        "Prefer `asSubclass` instead of casting the result of `newInstance`,"
            + " to detect classes of incorrect type before invoking their constructors."
            + "This way, if the class is of the incorrect type,"
            + "it will throw an exception before invoking its constructor.",
    category = JDK,
    severity = WARNING,
    tags = StandardTags.FRAGILE_CODE,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class UnsafeReflectiveConstructionCast extends BugChecker implements TypeCastTreeMatcher {

  private static final Matcher<ExpressionTree> CLASS_FOR_NAME =
      staticMethod().onClass(Class.class.getName()).named("forName");
  private static final Matcher<ExpressionTree> CLASS_GET_DECLARED_CTOR =
      instanceMethod().onExactClass(Class.class.getName()).named("getDeclaredConstructor");
  private static final Matcher<ExpressionTree> CTOR_NEW_INSTANCE =
      instanceMethod().onExactClass(Constructor.class.getName()).named("newInstance");

  @Override
  public Description matchTypeCast(TypeCastTree typeCastTree, VisitorState state) {
    // typeCastTree = (Foo) Class.forName(someString).getDeclaredConstructor(...).newInstance(args);
    ExpressionTree newInstanceTree = ASTHelpers.stripParentheses(typeCastTree.getExpression());
    // tree = Class.forName(someString).getDeclaredConstructor(...).newInstance(args);
    if (!CTOR_NEW_INSTANCE.matches(newInstanceTree, state)) {
      return Description.NO_MATCH;
    }
    ExpressionTree treeReceiver = ASTHelpers.getReceiver(newInstanceTree);
    // treeReceiver = Class.forName(someString).getDeclaredConstructor(...)
    if (!CLASS_GET_DECLARED_CTOR.matches(treeReceiver, state)) {
      return Description.NO_MATCH;
    }
    ExpressionTree classForName = ASTHelpers.getReceiver(treeReceiver);
    // classForName = Class.forName(someString)
    if (!CLASS_FOR_NAME.matches(classForName, state)) {
      return Description.NO_MATCH;
    }
    Symbol typeSym = ASTHelpers.getSymbol(typeCastTree.getType());
    if (typeSym == null) {
      return Description.NO_MATCH;
    }
    Types types = state.getTypes();
    Type typeCastTreeType = ASTHelpers.getType(typeCastTree.getType());
    Type erasedType = types.erasure(typeCastTreeType);
    if (ASTHelpers.isSameType(erasedType, state.getSymtab().objectType, state)) {
      // unbounded type parameter
      return Description.NO_MATCH;
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    String typeSource = SuggestedFixes.qualifyType(state, fix, erasedType);
    // what we want :
    // Class.forName(someString).asSubclass(Foo.class).getDeclaredConstructor().newInstance();
    // add .asSubclass(Foo.class)
    fix.postfixWith(classForName, ".asSubclass(" + typeSource + ".class)");
    if (types.isSameType(typeCastTreeType, erasedType)) {
      // remove the type
      fix.replace(
          ((JCTree) typeCastTree).getStartPosition(),
          ((JCTree) typeCastTree.getExpression()).getStartPosition(),
          "");
    }
    return describeMatch(classForName, fix.build());
  }
}
