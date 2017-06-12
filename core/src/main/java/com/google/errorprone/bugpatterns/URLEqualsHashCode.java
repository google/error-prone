/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.matchers.Matchers.anyOf;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import java.util.List;

/**
 * Points out on creation of Set and HashMap of type java.net.URL.
 *
 * <p>equals() and hashCode() of java.net.URL class make blocking internet connections.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
  name = "URLEqualsHashCode",
  summary =
      "Creation of a Set/HashSet/HashMap of java.net.URL."
          + " equals() and hashCode() of java.net.URL class make blocking internet connections.",
  category = JDK,
  severity = WARNING,
  tags = StandardTags.FRAGILE_CODE
)
public class URLEqualsHashCode extends BugChecker implements NewClassTreeMatcher {

  private static final String URL_CLASS = "java.net.URL";

  private static final Matcher<Tree> TYPE_MATCHER =
      anyOf(
          new URLTypeArgumentMatcher("java.util.Set", 0),
          new URLTypeArgumentMatcher("java.util.Map", 0));

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (TYPE_MATCHER.matches(tree, state)) {
      return describeMatch(tree);
    }
    return Description.NO_MATCH;
  }

  private static class URLTypeArgumentMatcher implements Matcher<Tree> {
    private final String clazz;
    private final int typeArgumentIndex;

    URLTypeArgumentMatcher(String clazz, int index) {
      this.clazz = clazz;
      this.typeArgumentIndex = index;
    }

    @Override
    public boolean matches(Tree tree, VisitorState state) {
      Symbol sym = state.getSymbolFromString(clazz);
      if (sym == null) {
        return false;
      }
      Type type = ASTHelpers.getType(tree);
      if (!ASTHelpers.isSubtype(type, sym.type, state)) {
        return false;
      }
      Types types = state.getTypes();
      Type superType = types.asSuper(type, sym);
      if (superType == null) {
        return false;
      }
      List<Type> typeArguments = superType.getTypeArguments();
      if (typeArguments.isEmpty()) {
        return false;
      }
      return ASTHelpers.isSameType(
          typeArguments.get(typeArgumentIndex), state.getTypeFromString(URL_CLASS), state);
    }
  }
}
