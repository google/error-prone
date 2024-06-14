/*
 * Copyright 2024 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.IdentifierTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MemberSelectTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ModuleSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "Usage of internal proprietary API which may be removed in a future release",
    severity = WARNING)
public class SunApi extends BugChecker implements MemberSelectTreeMatcher, IdentifierTreeMatcher {
  @Override
  public Description matchIdentifier(IdentifierTree tree, VisitorState state) {
    return match(tree, state);
  }

  @Override
  public Description matchMemberSelect(MemberSelectTree tree, VisitorState state) {
    return match(tree, state);
  }

  private Description match(Tree tree, VisitorState state) {
    Symbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null) {
      return NO_MATCH;
    }
    if (!inJdkUnsupportedModule(sym) && ((sym.flags() & Flags.PROPRIETARY) == 0)) {
      return NO_MATCH;
    }
    if (state.findEnclosing(ImportTree.class) != null) {
      return NO_MATCH;
    }
    return buildDescription(tree)
        .setMessage(
            String.format(
                "%s is an internal JDK API which may be removed in a future release",
                sym.getQualifiedName()))
        .build();
  }

  private static boolean inJdkUnsupportedModule(Symbol sym) {
    PackageSymbol packageSymbol = ASTHelpers.enclosingPackage(sym);
    if (packageSymbol == null) {
      return false;
    }
    ModuleSymbol moduleSymbol = (ModuleSymbol) packageSymbol.getEnclosingElement();
    if (moduleSymbol == null) {
      return false;
    }
    return moduleSymbol.name.contentEquals("jdk.unsupported");
  }
}
