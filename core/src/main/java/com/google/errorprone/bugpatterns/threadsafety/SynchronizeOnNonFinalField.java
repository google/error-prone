/*
 * Copyright 2014 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.stripParentheses;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.SynchronizedTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.stream.Stream;
import javax.lang.model.element.Name;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "SynchronizeOnNonFinalField",
    summary =
        "Synchronizing on non-final fields is not safe: if the field is ever updated,"
            + " different threads may end up locking on different objects.",
    severity = WARNING,
    tags = StandardTags.FRAGILE_CODE)
public class SynchronizeOnNonFinalField extends BugChecker
    implements BugChecker.SynchronizedTreeMatcher {

  @Override
  public Description matchSynchronized(SynchronizedTree tree, VisitorState state) {
    Symbol symbol = ASTHelpers.getSymbol(stripParentheses(tree.getExpression()));
    if (!(symbol instanceof VarSymbol)) {
      return NO_MATCH;
    }

    // TODO(cushon): check that the receiver doesn't contain mutable state.
    // Currently 'this.locks[i].mu' is accepted if 'mu' is final but 'locks' is non-final.
    VarSymbol varSymbol = (VarSymbol) symbol;
    if (ASTHelpers.isLocal(varSymbol)
        || varSymbol.isStatic()
        || (varSymbol.flags() & Flags.FINAL) != 0) {
      return NO_MATCH;
    }
    if (ASTHelpers.hasAnnotation(varSymbol, LazyInit.class, state)) {
      return NO_MATCH;
    }

    Name ownerName = varSymbol.owner.enclClass().getQualifiedName();
    if (Stream.of("java.io.Writer", "java.io.Reader").anyMatch(ownerName::contentEquals)) {
      // These classes contain a non-final 'lock' variable available to subclasses, and we can't
      // make these locks final.
      return NO_MATCH;
    }

    return describeMatch(tree.getExpression());
  }
}
