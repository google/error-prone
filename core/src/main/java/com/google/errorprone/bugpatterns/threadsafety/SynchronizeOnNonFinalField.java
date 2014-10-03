/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.threadsafety;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.SynchronizedTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.TreeInfo;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(name = "SynchronizeOnNonFinalField",
    summary = "Synchronizing on non-final fields is not safe: if the field is ever updated,"
    + " different threads may end up locking on different objects.",
    explanation = "Possible fixes:\n"
        + "* If the field is already effectively final, add the missing 'final' modifier.\n"
        + "* If the field needs to be mutable, create a separate lock by adding a private"
        + "  final field and synchronizing on it to guard all accesses.",
    category = JDK, severity = WARNING, maturity = MATURE)
public class SynchronizeOnNonFinalField extends BugChecker
    implements BugChecker.SynchronizedTreeMatcher {

  @Override
  public Description matchSynchronized(SynchronizedTree tree, VisitorState state) {
    Symbol symbol = ASTHelpers.getSymbol(TreeInfo.skipParens((JCExpression) tree.getExpression()));
    if (!(symbol instanceof VarSymbol)) {
      return Description.NO_MATCH;
    }

    // TODO(user): check that the receiver doesn't contain mutable state.
    // Currently 'this.locks[i].mu' is accepted if 'mu' is final but 'locks' is non-final.
    VarSymbol varSymbol = (VarSymbol) symbol;
    if (varSymbol.isLocal()
        || varSymbol.isStatic()
        || (varSymbol.flags() & Flags.FINAL) != 0) {
      return Description.NO_MATCH;
    }

    return describeMatch(tree.getExpression());
  }
}
