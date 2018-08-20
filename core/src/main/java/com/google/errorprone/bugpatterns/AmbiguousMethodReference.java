/*
 * Copyright 2016 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.StreamSupport.stream;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.Signatures;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "AmbiguousMethodReference",
    category = JDK,
    summary = "Method reference is ambiguous",
    severity = WARNING)
public class AmbiguousMethodReference extends BugChecker implements ClassTreeMatcher {

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    ClassSymbol origin = getSymbol(tree);
    Types types = state.getTypes();
    Iterable<Symbol> members =
        types.membersClosure(getType(tree), /*skipInterface=*/ false).getSymbols();

    // collect declared and inherited methods, grouped by reference descriptor
    Map<String, List<MethodSymbol>> methods =
        stream(members.spliterator(), false)
            .filter(MethodSymbol.class::isInstance)
            .map(MethodSymbol.class::cast)
            .filter(m -> m.isConstructor() || m.owner.equals(origin))
            .collect(
                groupingBy(m -> methodReferenceDescriptor(types, m), toCollection(ArrayList::new)));

    // look for groups of ambiguous method references
    for (Tree member : tree.getMembers()) {
      if (!(member instanceof MethodTree)) {
        continue;
      }
      MethodSymbol msym = getSymbol((MethodTree) member);
      if (isSuppressed(msym)) {
        continue;
      }
      List<MethodSymbol> clash = methods.remove(methodReferenceDescriptor(types, msym));
      if (clash == null) {
        continue;
      }
      // If the clashing group has 1 or 0 non-private methods, method references outside the file
      // are unambiguous.
      int nonPrivateMethodCount = 0;
      for (MethodSymbol method : clash) {
        if (!method.isPrivate()) {
          nonPrivateMethodCount++;
        }
      }
      if (nonPrivateMethodCount < 2) {
        continue;
      }
      clash.remove(msym);
      // ignore overridden inherited methods and hidden interface methods
      clash.removeIf(m -> types.isSubSignature(msym.type, m.type));
      if (clash.isEmpty()) {
        continue;
      }
      String message =
          String.format(
              "This method's reference is ambiguous, its name and functional interface type"
                  + " are the same as: %s",
              clash.stream()
                  .map(m -> Signatures.prettyMethodSignature(origin, m))
                  .collect(joining(", ")));
      state.reportMatch(buildDescription(member).setMessage(message).build());
    }
    return NO_MATCH;
  }

  /** Returns a string descriptor of a method's reference type. */
  private String methodReferenceDescriptor(Types types, MethodSymbol sym) {
    StringBuilder sb = new StringBuilder();
    sb.append(sym.getSimpleName()).append('(');
    if (!sym.isStatic()) {
      sb.append(Signatures.descriptor(sym.owner.type, types));
    }
    sym.params().stream().map(p -> Signatures.descriptor(p.type, types)).forEachOrdered(sb::append);
    sb.append(")");
    return sb.toString();
  }
}
