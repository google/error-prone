/*
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

import com.google.auto.service.AutoService;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Suppliers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;
import com.sun.tools.javac.code.Type;

import java.util.Collections;
import java.util.TreeMap;

import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.constructor;

/**
 * @author mdrob@apache.org (Mike Drob)
 */
@AutoService(BugChecker.class)
@BugPattern(name = "TreeMapNonComparableKeyWithoutComparator",
    category = BugPattern.Category.JDK,
    summary = "Using a TreeMap with keys that are not Comparable and without specifying a Comparator will result in ClassCastExceptions at runtime.",
    severity = BugPattern.SeverityLevel.ERROR,
    linkType = BugPattern.LinkType.NONE)
public class TreeMapNonComparableKeyWithoutComparator extends BugChecker implements BugChecker.NewClassTreeMatcher {
  static final Matcher<ExpressionTree> CONSTRUCTOR = constructor()
      .forClass(TreeMap.class.getName()).withParameters(Collections.emptyList());

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (CONSTRUCTOR.matches(tree, state)) {
      Type keyType = Suppliers.genericTypeOf(Suppliers.identitySupplier(tree), 0).get(state);

      // XXX This is super hacky and probably very fragile
      try {
        Class<?> cls = Class.forName(keyType.toString());
        if (Comparable.class.isAssignableFrom(cls)) return NO_MATCH;
      } catch (Exception e) {

      }

      int end = state.getEndPosition(tree) - 1; // closing parenthesis
      return describeMatch(tree, SuggestedFix.replace(end, end, "Comparator c"));
    }
    return NO_MATCH;
  }
}

