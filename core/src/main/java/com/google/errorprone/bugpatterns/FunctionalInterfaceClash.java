/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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
import static java.util.stream.Collectors.joining;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.code.Types.DefaultTypeVisitor;
import com.sun.tools.javac.code.Types.SignatureGenerator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import java.util.ArrayList;
import java.util.Collection;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "FunctionalInterfaceClash",
  summary = "Overloads will be ambiguous when passing lambda arguments",
  category = JDK,
  severity = WARNING
)
public class FunctionalInterfaceClash extends BugChecker implements ClassTreeMatcher {
  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    ClassSymbol origin = getSymbol(tree);
    Types types = state.getTypes();
    // collect declared and inherited methods whose signature contains a functional interface
    Multimap<String, MethodSymbol> methods = HashMultimap.create();
    for (Symbol sym : types.membersClosure(getType(tree), /*skipInterface=*/ false).getSymbols()) {
      if (!(sym instanceof MethodSymbol)) {
        continue;
      }
      MethodSymbol msym = (MethodSymbol) sym;
      if (msym.getParameters().stream().map(p -> p.type).noneMatch(types::isFunctionalInterface)) {
        continue;
      }
      if (msym.isConstructor() && !msym.owner.equals(origin)) {
        continue;
      }
      methods.put(functionalInterfaceSignature(state, msym), msym);
    }
    // check if any declared members clash with another declared or inherited member
    // (don't report clashes between inherited members)
    for (Tree member : tree.getMembers()) {
      if (!(member instanceof MethodTree)) {
        continue;
      }
      MethodSymbol msym = getSymbol((MethodTree) member);
      if (msym.getParameters().stream().map(p -> p.type).noneMatch(types::isFunctionalInterface)) {
        continue;
      }
      Collection<MethodSymbol> clash =
          new ArrayList<>(methods.removeAll(functionalInterfaceSignature(state, msym)));
      clash.remove(msym);
      // ignore inherited methods that are overridden in the original class
      clash.removeIf(m -> msym.overrides(m, origin, types, false));
      if (!clash.isEmpty()) {
        String message =
            String.format(
                "When passing lambda arguments to this function, callers will need a cast to"
                    + " disambiguate with: %s",
                clash.stream().map(m -> pretty(origin, m)).collect(joining("\n    ")));
        state.reportMatch(buildDescription(member).setMessage(message).build());
      }
    }
    return NO_MATCH;
  }

  /**
   * A string representation of a method descriptor, where all parameters whose type is a functional
   * interface are "erased" to the interface's function type. For example, `foo(Supplier<String>)`
   * is represented as `foo(()->Ljava/lang/String;)`.
   */
  private String functionalInterfaceSignature(VisitorState state, MethodSymbol msym) {
    return String.format(
        "%s(%s)",
        msym.getSimpleName(),
        msym.getParameters()
            .stream()
            .map(p -> functionalInterfaceSignature(state, p.type))
            .collect(joining(",")));
  }

  private String functionalInterfaceSignature(VisitorState state, Type type) {
    Types types = state.getTypes();
    if (!types.isFunctionalInterface(type)) {
      return SigGen.descriptor(type, state);
    }
    Type descriptorType = types.findDescriptorType(type);
    List<Type> fiparams = descriptorType.getParameterTypes();
    // Implicitly typed block-statement-bodied lambdas are potentially compatible with
    // void-returning and value-returning functional interface types, so we don't consider return
    // types in general. The except is nullary functional interfaces, since the lambda parameters
    // will never be implicitly typed.
    String result =
        fiparams.isEmpty() ? SigGen.descriptor(descriptorType.getReturnType(), state) : "_";
    return String.format(
        "(%s)->%s",
        fiparams.stream().map(t -> SigGen.descriptor(t, state)).collect(joining(",")), result);
  }

  /** Generate binary descriptors. */
  // TODO(cushon): find a better home for this, share it with the api checker
  static class SigGen extends SignatureGenerator {

    static String classDescriptor(Type type, VisitorState state) {
      SigGen sig = new SigGen(state);
      sig.assembleClassSig(state.getTypes().erasure(type));
      return sig.toName().toString();
    }

    static String descriptor(Type type, VisitorState state) {
      SigGen sig = new SigGen(state);
      sig.assembleSig(state.getTypes().erasure(type));
      return sig.toName().toString();
    }

    private final com.sun.tools.javac.util.ByteBuffer buffer =
        new com.sun.tools.javac.util.ByteBuffer();
    private final VisitorState state;

    protected SigGen(VisitorState state) {
      super(state.getTypes());
      this.state = state;
    }

    @Override
    protected void append(char ch) {
      buffer.appendByte(ch);
    }

    @Override
    protected void append(byte[] ba) {
      buffer.appendBytes(ba);
    }

    @Override
    protected void append(Name name) {
      buffer.appendName(name);
    }

    Name toName() {
      return buffer.toName(Names.instance(state.context));
    }
  }

  /**
   * Pretty-prints a method signature using simple names for declared types, and omitting formal
   * type parameters and the return type since they do not affect overload resolution.
   */
  private String pretty(ClassSymbol origin, MethodSymbol m) {
    StringBuilder sb = new StringBuilder();
    if (!m.owner.equals(origin)) {
      sb.append(m.owner.getSimpleName()).append('.');
    }
    sb.append(m.isConstructor() ? origin.getSimpleName() : m.getSimpleName()).append('(');
    sb.append(
        m.getParameters()
            .stream()
            .map(v -> v.type.accept(PRETTY_TYPE_VISITOR, null))
            .collect(joining(", ")));
    sb.append(')');
    return sb.toString();
  }

  private static final Type.Visitor<String, Void> PRETTY_TYPE_VISITOR =
      new DefaultTypeVisitor<String, Void>() {
        @Override
        public String visitWildcardType(Type.WildcardType t, Void aVoid) {
          StringBuilder sb = new StringBuilder();
          sb.append(t.kind);
          if (t.kind != BoundKind.UNBOUND) {
            sb.append(t.type.accept(this, null));
          }
          return sb.toString();
        }

        @Override
        public String visitClassType(Type.ClassType t, Void s) {
          StringBuilder buf = new StringBuilder();
          buf.append(t.tsym.getSimpleName());
          if (t.getTypeArguments().nonEmpty()) {
            buf.append('<');
            boolean first = true;
            for (Type ta : t.getTypeArguments()) {
              if (!first) {
                buf.append(", ");
              }
              first = false;
              buf.append(ta.accept(this, null));
            }
            buf.append(">");
          }
          return buf.toString();
        }

        @Override
        public String visitCapturedType(Type.CapturedType t, Void s) {
          return t.wildcard.accept(this, null);
        }

        @Override
        public String visitType(Type t, Void s) {
          return t.toString();
        }
      };
}
