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

package com.google.errorprone.util;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.code.Types.DefaultTypeVisitor;
import com.sun.tools.javac.code.Types.SignatureGenerator;
import com.sun.tools.javac.util.Name;
import java.util.Arrays;

/** Signature generation. */
public class Signatures {

  /** Returns the binary names of the class. */
  public static String classDescriptor(Type type, Types types) {
    SigGen sig = new SigGen(types);
    sig.assembleClassSig(types.erasure(type));
    return sig.toString();
  }

  /** Returns a JVMS 4.3.3 method descriptor. */
  public static String descriptor(Type type, Types types) {
    SigGen sig = new SigGen(types);
    sig.assembleSig(types.erasure(type));
    return sig.toString();
  }

  private static class SigGen extends SignatureGenerator {

    private final com.sun.tools.javac.util.ByteBuffer buffer =
        new com.sun.tools.javac.util.ByteBuffer();

    protected SigGen(Types types) {
      super(types);
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

    @Override
    public String toString() {
      // We could use buffer.toName(Names), but we want a string anyways and this
      // avoids plumbing a Context or instances of Names through.
      // Names always uses UTF-8 internally.
      return new String(Arrays.copyOf(buffer.elems, buffer.length), UTF_8);
    }
  }

  /**
   * Pretty-prints a method signature for use in diagnostics.
   *
   * <p>Uses simple names for declared types, and omitting formal type parameters and the return
   * type since they do not affect overload resolution.
   */
  public static String prettyMethodSignature(ClassSymbol origin, MethodSymbol m) {
    StringBuilder sb = new StringBuilder();
    if (!m.owner.equals(origin)) {
      sb.append(m.owner.getSimpleName()).append('.');
    }
    sb.append(m.isConstructor() ? origin.getSimpleName() : m.getSimpleName()).append('(');
    sb.append(
        m.getParameters().stream()
            .map(v -> v.type.accept(PRETTY_TYPE_VISITOR, null))
            .collect(joining(", ")));
    sb.append(')');
    return sb.toString();
  }

  /** Pretty-prints a Type for use in diagnostics, using simple names for class types */
  public static String prettyType(Type type) {
    return type.accept(PRETTY_TYPE_VISITOR, null);
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
          StringBuilder sb = new StringBuilder();
          sb.append(t.tsym.getSimpleName());
          if (t.getTypeArguments().nonEmpty()) {
            sb.append('<');
            sb.append(
                t.getTypeArguments().stream()
                    .map(a -> a.accept(this, null))
                    .collect(joining(", ")));
            sb.append(">");
          }
          return sb.toString();
        }

        @Override
        public String visitCapturedType(Type.CapturedType t, Void s) {
          return t.wildcard.accept(this, null);
        }

        @Override
        public String visitArrayType(Type.ArrayType t, Void aVoid) {
          return t.elemtype.accept(this, null) + "[]";
        }

        @Override
        public String visitType(Type t, Void s) {
          return t.toString();
        }
      };
}
