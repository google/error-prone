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

import static com.google.errorprone.BugPattern.Category.GUAVA;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.methodSelect;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.NewInstanceAnonymousInnerClass;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.Pretty;
import com.sun.tools.javac.util.List;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;

/**
 * Checker for a call of the form:
 * <pre>
 * Ordering.from(new Comparator<T>() { ... })
 * </pre>
 *
 * <p>This can be unwrapped to a new anonymous subclass of Ordering:
 * <pre>
 * new Ordering<T>() { ... }
 * </pre>
 * which is shorter and cleaner (and potentially more efficient).
 *
 * @author sjnickerson@google.com (Simon Nickerson)
 *
 */
@BugPattern(name = "OrderingFrom",
    summary = "Ordering.from(new Comparator<T>() { }) can be refactored to cleaner form",
    explanation =
        "Calls of the form\n" +
        "`Ordering.from(new Comparator<T>() { ... })`\n" +
        "can be unwrapped to a new anonymous subclass of Ordering\n" +
        "`new Ordering<T>() { ... }`\n" +
        "which is shorter and cleaner (and potentially more efficient).",
    category = GUAVA, severity = WARNING, maturity = EXPERIMENTAL)
public class OrderingFrom extends BugChecker implements MethodInvocationTreeMatcher {

  @SuppressWarnings({"unchecked", "varargs"})
  private static final Matcher<MethodInvocationTree> matcher = allOf(
      methodSelect(staticMethod("com.google.common.collect.Ordering", "from")),
      argument(0, new NewInstanceAnonymousInnerClass("java.util.Comparator")));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree methodInvocation, VisitorState state) {
    if(!matcher.matches(methodInvocation, state)) {
      return Description.NO_MATCH;
    }

    // e.g. new Comparator<String>() { ... }
    JCNewClass newComparatorInvocation = (JCNewClass) methodInvocation.getArguments().get(0);

    // e.g. Ordering
    JCIdent orderingIdent = (JCIdent)
        ((JCFieldAccess) ((JCMethodInvocation) methodInvocation).meth).selected;
    // e.g. Ordering<String>
    JCTypeApply newOrderingType = state.getTreeMaker().TypeApply(orderingIdent,
        ((JCTypeApply) newComparatorInvocation.clazz).arguments);

    // Find the class definition and remove the default constructor (it confuses the pretty printer)
    JCClassDecl def = newComparatorInvocation.def;

    // Note that List is not java.util.List, and it's not very nice to deal with.
    ArrayList<JCTree> allDefsExceptConstructor = new ArrayList<>();
    for (JCTree individualDef : def.defs) {
      if (individualDef instanceof JCMethodDecl) {
        JCMethodDecl methodDecl = (JCMethodDecl) individualDef;
        if (!methodDecl.name.toString().equals("<init>")) {
          allDefsExceptConstructor.add(individualDef);
        }
      } else {
        allDefsExceptConstructor.add(individualDef);
      }
    }

    // e.g. new Ordering<String>() { ... }
    JCNewClass newClass = state.getTreeMaker().NewClass(
        newComparatorInvocation.encl, newComparatorInvocation.typeargs, newOrderingType,
        newComparatorInvocation.args,
        state.getTreeMaker().ClassDef(
            def.mods, def.name, def.typarams, def.extending, def.implementing,
            List.from(allDefsExceptConstructor.toArray(
                new JCTree[allDefsExceptConstructor.size()]))));

    StringWriter sw = new StringWriter();
    try {
      Pretty pretty = new Pretty(sw, true);
      pretty.printExpr(newClass);
    } catch (IOException impossible) {
      throw new AssertionError("Impossible IOException");
    }

    String replacement = sw.toString().replace("@Override()", "@Override");

    Fix fix = SuggestedFix.replace(methodInvocation, replacement);

    return describeMatch(methodInvocation, fix);
  }
}
