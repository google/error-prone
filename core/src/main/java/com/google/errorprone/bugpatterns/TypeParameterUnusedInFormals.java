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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.TypeVar;
import com.sun.tools.javac.code.Types;
import java.util.HashSet;
import java.util.Set;

@BugPattern(
  name = "TypeParameterUnusedInFormals",
  summary =
      "Declaring a type parameter that is only used in the return type is a misuse of"
          + " generics: operations on the type parameter are unchecked, it hides unsafe casts at"
          + " invocations of the method, and it interacts badly with method overload resolution.",
  category = JDK,
  severity = WARNING,
  tags = StandardTags.FRAGILE_CODE
)
public class TypeParameterUnusedInFormals extends BugChecker implements MethodTreeMatcher {

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    MethodSymbol methodSymbol = ASTHelpers.getSymbol(tree);
    if (methodSymbol == null) {
      return Description.NO_MATCH;
    }

    // Only match methods where the return type is just a type parameter.
    // e.g. the following is OK: <T> List<T> newArrayList();
    TypeVar retType;
    switch (methodSymbol.getReturnType().getKind()) {
      case TYPEVAR:
        retType = (TypeVar) methodSymbol.getReturnType();
        break;
      default:
        return Description.NO_MATCH;
    }

    if (!methodSymbol.equals(retType.tsym.owner)) {
      return Description.NO_MATCH;
    }

    // Ignore f-bounds.
    // e.g.: <T extends Enum<T>> T unsafeEnumDeserializer();
    if (retType.bound != null && TypeParameterFinder.visit(retType.bound).contains(retType.tsym)) {
      return Description.NO_MATCH;
    }

    // Ignore cases where the type parameter is used in the declaration of a formal parameter.
    // e.g.: <T> T noop(T t);
    for (VarSymbol formalParam : methodSymbol.getParameters()) {
      if (TypeParameterFinder.visit(formalParam.type).contains(retType.tsym)) {
        return Description.NO_MATCH;
      }
    }

    return describeMatch(tree);
  }

  /**
   * A visitor that records the set of {@link com.sun.tools.javac.code.Type.TypeVar}s referenced by
   * the current type.
   */
  private static class TypeParameterFinder extends Types.DefaultTypeVisitor<Void, Void> {

    static Set<Symbol.TypeSymbol> visit(Type type) {
      TypeParameterFinder visitor = new TypeParameterFinder();
      type.accept(visitor, null);
      return visitor.seen;
    }

    private final Set<Symbol.TypeSymbol> seen = new HashSet<>();

    @Override
    public Void visitClassType(Type.ClassType type, Void unused) {
      if (type instanceof Type.IntersectionClassType) {
        // TypeVisitor doesn't support intersection types natively
        visitIntersectionClassType((Type.IntersectionClassType) type);
      } else {
        for (Type t : type.getTypeArguments()) {
          t.accept(this, null);
        }
      }
      return null;
    }

    public void visitIntersectionClassType(Type.IntersectionClassType type) {
      for (Type component : type.getComponents()) {
        component.accept(this, null);
      }
    }

    @Override
    public Void visitWildcardType(Type.WildcardType type, Void unused) {
      if (type.getSuperBound() != null) {
        type.getSuperBound().accept(this, null);
      }
      if (type.getExtendsBound() != null) {
        type.getExtendsBound().accept(this, null);
      }
      return null;
    }

    @Override
    public Void visitArrayType(Type.ArrayType type, Void unused) {
      type.elemtype.accept(this, null);
      return null;
    }

    @Override
    public Void visitTypeVar(Type.TypeVar type, Void unused) {
      // only visit f-bounds once:
      if (!seen.add(type.tsym)) {
        return null;
      }
      if (type.bound != null) {
        type.bound.accept(this, null);
      }
      return null;
    }

    @Override
    public Void visitType(Type type, Void unused) {
      return null;
    }
  }
}
