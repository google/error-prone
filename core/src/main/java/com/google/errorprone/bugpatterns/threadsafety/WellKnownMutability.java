/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.primitives.Primitives;
import com.google.errorprone.VisitorState;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;

import java.lang.reflect.TypeVariable;
import java.util.HashSet;
import java.util.Set;

/** A collection of types with with known mutability. */
final class WellKnownMutability {


  /** Types that are known to be immutable. */
  static final ImmutableMap<String, ImmutableAnnotationInfo> KNOWN_IMMUTABLE =
      getBootstrapClasses();

  static class Builder {
    ImmutableMap.Builder<String, ImmutableAnnotationInfo> mapBuilder = ImmutableMap.builder();

    public Builder addAll(Set<Class<?>> clazzs) {
      for (Class<?> clazz : clazzs) {
        add(clazz);
      }
      return this;
    }

    public Builder add(Class<?> clazz, String... containerOf) {
      ImmutableSet<String> containerTyParams = ImmutableSet.copyOf(containerOf);
      HashSet<String> actualTyParams = new HashSet<>();
      for (TypeVariable<?> x : clazz.getTypeParameters()) {
        actualTyParams.add(x.getName());
      }
      SetView<String> difference = Sets.difference(containerTyParams, actualTyParams);
      if (!difference.isEmpty()) {
        throw new AssertionError(
            String.format(
                "Container type parameter(s) %s did not exist, expected %s",
                difference,
                actualTyParams));
      }
      mapBuilder.put(
          clazz.getName(),
          ImmutableAnnotationInfo.create(clazz.getName(), ImmutableList.copyOf(containerOf)));
      return this;
    }

    public Builder add(String className, String... containerOf) {
      mapBuilder.put(
          className, ImmutableAnnotationInfo.create(className, ImmutableList.copyOf(containerOf)));
      return this;
    }

    public ImmutableMap<String, ImmutableAnnotationInfo> build() {
      return mapBuilder.build();
    }
  }

  // TODO(cushon): share this list with other code analyzing types for immutability
  // TODO(cushon): generate this at build-time to get type-safety without added compile-time deps
  private static ImmutableMap<String, ImmutableAnnotationInfo> getBootstrapClasses() {
    return new Builder()
        .addAll(Primitives.allPrimitiveTypes())
        .addAll(Primitives.allWrapperTypes())
        .add(java.lang.Class.class)
        .add(java.lang.String.class)
        .add(java.net.InetAddress.class)
        .add(java.util.Locale.class)
        .add("org.joda.time.DateTime")
        .add("org.joda.time.DateTimeZone")
        .add("org.joda.time.Duration")
        .add("org.joda.time.Instant")
        .add("org.joda.time.LocalDate")
        .add("org.joda.time.LocalDateTime")
        .add(com.google.common.primitives.UnsignedLong.class)
        .add(com.google.common.base.Converter.class)
        .add("com.google.protobuf.ByteString")
        .add("com.google.protobuf.Descriptors$Descriptor")
        .add("com.google.protobuf.Descriptors$EnumDescriptor")
        .add("com.google.protobuf.Descriptors$FieldDescriptor")
        .add("com.google.protobuf.Descriptors$FileDescriptor")
        .add("com.google.protobuf.Descriptors$ServiceDescriptor")
        .add("com.google.protobuf.ExtensionRegistry$ExtensionInfo")
        .add("com.google.common.net.MediaType")
        .add("com.google.common.net.Uri")
        .add("com.google.protobuf.Extension")
        .add(com.google.common.collect.ImmutableCollection.class, "E")
        .add(com.google.common.collect.ImmutableSet.class, "E")
        .add(com.google.common.collect.ImmutableList.class, "E")
        .add(com.google.common.collect.ImmutableMultiset.class, "E")
        .add(com.google.common.collect.ImmutableMap.class, "K", "V")
        .add(com.google.common.collect.ImmutableMultimap.class, "K", "V")
        .add(com.google.common.collect.ImmutableRangeMap.class, "K", "V")
        .add(com.google.common.collect.ImmutableTable.class, "R", "C", "V")
        .add(com.google.common.base.Optional.class, "T")
        .add(com.google.common.collect.Range.class, "C")
        .build();
  }

  /** Types that are known to be mutable. */
  static final ImmutableSet<String> KNOWN_UNSAFE = getKnownUnsafeClasses();

  private static ImmutableSet<String> getKnownUnsafeClasses() {
    ImmutableSet.Builder<String> result = ImmutableSet.<String>builder();
    for (Class<?> clazz :
        ImmutableSet.<Class<?>>of(
            java.lang.Iterable.class,
            java.lang.Object.class,
            java.util.ArrayList.class,
            java.util.Collection.class,
            java.util.List.class,
            java.util.Map.class,
            java.util.Set.class,
            java.util.EnumSet.class,
            java.util.EnumMap.class)) {
      result.add(clazz.getName());
    }
    return result.build();
  }

  /** @return true if the type is an immutable proto. */
  public static boolean isImmutableProto(VisitorState state, Type type) {
    return isProto2MessageClass(state, type) && !isProto2MutableMessageClass(state, type);
  }

  /**
   * Compile-time equivalent of
   * {@code com.google.io.protocol.ProtocolSupport#isProto2MessageClass}.
   */
  private static boolean isProto2MessageClass(VisitorState state, Type type) {
    // ProtocolSupport matches Message (not MessageLite) for legacy reasons
    Type messageType = state.getTypeFromString("com.google.protobuf.MessageLite");
    Type protocolMessageType = state.getTypeFromString("com.google.io.protocol.ProtocolMessage");
    Types types = state.getTypes();
    return types.isAssignable(type, messageType) && !types.isAssignable(type, protocolMessageType);
  }

  /**
   * Compile-time equivalent of
   * {@code com.google.io.protocol.ProtocolSupport#isProto2MutableMessageClass}.
   */
  private static boolean isProto2MutableMessageClass(VisitorState state, Type type) {
    // ProtocolSupport matches MutableMessage (not MutableMessageLite) for legacy reasons
    Type mutableMessageType = state.getTypeFromString("com.google.protobuf.MutableMessageLite");
    Type protocolMessageType = state.getTypeFromString("com.google.io.protocol.ProtocolMessage");
    Types types = state.getTypes();
    return types.isAssignable(type, mutableMessageType)
        && !types.isAssignable(type, protocolMessageType);
  }
}
