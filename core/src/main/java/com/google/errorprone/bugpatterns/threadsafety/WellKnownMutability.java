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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.primitives.Primitives;
import com.google.errorprone.VisitorState;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.suppliers.Suppliers;
import com.sun.tools.javac.code.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashSet;
import java.util.Set;

/** A collection of types with with known mutability. */
final class WellKnownMutability {


  /** Types that are known to be immutable. */
  static final ImmutableMap<String, ImmutableAnnotationInfo> KNOWN_IMMUTABLE =
      getBootstrapClasses();

  static class Builder {
    final ImmutableMap.Builder<String, ImmutableAnnotationInfo> mapBuilder = ImmutableMap.builder();

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
                "For %s, please update the type parameter(s) from %s to %s",
                clazz, difference, actualTyParams));
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

  // TODO(b/35724557): share this list with other code analyzing types for immutability
  // TODO(cushon): generate this at build-time to get type-safety without added compile-time deps
  private static ImmutableMap<String, ImmutableAnnotationInfo> getBootstrapClasses() {
    return new Builder()
        .addAll(Primitives.allPrimitiveTypes())
        .addAll(Primitives.allWrapperTypes())
        .add("com.google.protobuf.ByteString")
        .add("com.google.protobuf.Descriptors$Descriptor")
        .add("com.google.protobuf.Descriptors$EnumDescriptor")
        .add("com.google.protobuf.Descriptors$EnumValueDescriptor")
        .add("com.google.protobuf.Descriptors$FieldDescriptor")
        .add("com.google.protobuf.Descriptors$FileDescriptor")
        .add("com.google.protobuf.Descriptors$ServiceDescriptor")
        .add("com.google.protobuf.Extension")
        .add("com.google.protobuf.ExtensionRegistry$ExtensionInfo")
        .add("com.google.re2j.Pattern")
        .add(com.google.common.base.CharMatcher.class)
        .add(com.google.common.base.Converter.class)
        .add(com.google.common.base.Joiner.class)
        .add(com.google.common.base.Optional.class, "T")
        .add(com.google.common.base.Splitter.class)
        .add(com.google.common.collect.ImmutableBiMap.class, "K", "V")
        .add(com.google.common.collect.ImmutableCollection.class, "E")
        .add(com.google.common.collect.ImmutableList.class, "E")
        .add(com.google.common.collect.ImmutableListMultimap.class, "K", "V")
        .add(com.google.common.collect.ImmutableMap.class, "K", "V")
        .add(com.google.common.collect.ImmutableMultimap.class, "K", "V")
        .add(com.google.common.collect.ImmutableMultiset.class, "E")
        .add(com.google.common.collect.ImmutableRangeMap.class, "K", "V")
        .add(com.google.common.collect.ImmutableRangeSet.class, "C")
        .add(com.google.common.collect.ImmutableSet.class, "E")
        .add(com.google.common.collect.ImmutableSetMultimap.class, "K", "V")
        .add(com.google.common.collect.ImmutableSortedMap.class, "K", "V")
        .add(com.google.common.collect.ImmutableSortedMultiset.class, "E")
        .add(com.google.common.collect.ImmutableSortedSet.class, "E")
        .add(com.google.common.collect.ImmutableTable.class, "R", "C", "V")
        .add(com.google.common.collect.Range.class, "C")
        .add(com.google.common.graph.ImmutableGraph.class, "N")
        .add(com.google.common.graph.ImmutableNetwork.class, "N", "E")
        .add(com.google.common.graph.ImmutableValueGraph.class, "N", "V")
        .add(com.google.common.hash.HashCode.class)
        .add(com.google.common.io.BaseEncoding.class)
        .add(com.google.common.net.MediaType.class)
        .add(com.google.common.primitives.UnsignedLong.class)
        .add(java.lang.Class.class)
        .add(java.lang.String.class)
        .add(java.lang.annotation.Annotation.class)
        .add(java.math.BigDecimal.class)
        .add(java.math.BigInteger.class)
        .add(java.net.InetAddress.class)
        .add(java.net.URI.class)
        .add(java.util.Locale.class)
        .add(java.util.regex.Pattern.class)
        .add("android.net.Uri")
        .add("java.util.Optional", "T")
        .add("java.time.Duration")
        .add("java.time.Instant")
        .add("java.time.LocalDate")
        .add("java.time.LocalDateTime")
        .add("java.time.LocalTime")
        .add("java.time.MonthDay")
        .add("java.time.OffsetDateTime")
        .add("java.time.OffsetTime")
        .add("java.time.Period")
        .add("java.time.Year")
        .add("java.time.YearMonth")
        .add("java.time.ZoneId")
        .add("java.time.ZoneOffset")
        .add("java.time.ZonedDateTime")
        .add("java.time.chrono.AbstractChronology")
        .add("java.time.chrono.ChronoLocalDate")
        .add("java.time.chrono.ChronoLocalDateTime", "D")
        .add("java.time.chrono.ChronoPeriod")
        .add("java.time.chrono.ChronoZonedDateTime", "D")
        .add("java.time.chrono.Chronology")
        .add("java.time.chrono.Era")
        .add("java.time.chrono.HijrahChronology")
        .add("java.time.chrono.HijrahDate")
        .add("java.time.chrono.IsoChronology")
        .add("java.time.chrono.JapaneseChronology")
        .add("java.time.chrono.JapaneseDate")
        .add("java.time.chrono.JapaneseEra")
        .add("java.time.chrono.MinguoChronology")
        .add("java.time.chrono.MinguoDate")
        .add("java.time.chrono.ThaiBuddhistChronology")
        .add("java.time.chrono.ThaiBuddhistDate")
        .add("java.time.format.DateTimeFormatter")
        .add("java.time.format.DecimalStyle")
        .add("java.time.temporal.TemporalField")
        .add("java.time.temporal.TemporalUnit")
        .add("java.time.temporal.ValueRange")
        .add("java.time.temporal.WeekFields")
        .add("java.time.zone.ZoneOffsetTransition")
        .add("java.time.zone.ZoneOffsetTransitionRule")
        .add("java.time.zone.ZoneRules")
        .add("java.time.zone.ZoneRulesProvider")
        .add("org.joda.time.DateTime")
        .add("org.joda.time.DateTimeZone")
        .add("org.joda.time.Duration")
        .add("org.joda.time.Instant")
        .add("org.joda.time.LocalDate")
        .add("org.joda.time.LocalDateTime")
        .add("org.joda.time.Period")
        .add("org.joda.time.format.DateTimeFormatter")
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

  // ProtocolSupport matches Message (not MessageLite) for legacy reasons
  private static final Supplier<Type> MESSAGE_TYPE =
      Suppliers.typeFromString("com.google.protobuf.MessageLite");

  private static final Supplier<Type> MUTABLE_MESSAGE_TYPE =
      Suppliers.typeFromString("com.google.protobuf.MutableMessageLite");

  private static final Supplier<Type> PROTOCOL_MESSAGE_TYPE =
      Suppliers.typeFromString("com.google.io.protocol.ProtocolMessage");

  private static boolean isAssignableTo(Type type, Supplier<Type> supplier, VisitorState state) {
    Type to = supplier.get(state);
    if (to == null) {
      // the type couldn't be loaded
      return false;
    }
    return state.getTypes().isAssignable(type, to);
  }

  /**
   * Compile-time equivalent of
   * {@code com.google.io.protocol.ProtocolSupport#isProto2MessageClass}.
   */
  static boolean isProto2MessageClass(VisitorState state, Type type) {
    checkNotNull(type);
    return isAssignableTo(type, MESSAGE_TYPE, state)
        && !isAssignableTo(type, PROTOCOL_MESSAGE_TYPE, state);
  }

  /**
   * Compile-time equivalent of
   * {@code com.google.io.protocol.ProtocolSupport#isProto2MutableMessageClass}.
   */
  static boolean isProto2MutableMessageClass(VisitorState state, Type type) {
    checkNotNull(type);
    return isAssignableTo(type, MUTABLE_MESSAGE_TYPE, state)
        && !isAssignableTo(type, PROTOCOL_MESSAGE_TYPE, state);
  }

  /** Returns true if the type is an annotation. */
  static boolean isAnnotation(VisitorState state, Type type) {
    return isAssignableTo(type, Suppliers.ANNOTATION_TYPE, state);
  }
}
