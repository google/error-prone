/*
 * Copyright 2015 The Error Prone Authors.
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
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.bugpatterns.ImmutableCollections;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.suppliers.Suppliers;
import com.sun.tools.javac.code.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A collection of types with known mutability. */
@Immutable
public final class WellKnownMutability implements ThreadSafety.KnownTypes {

  /** Types that are known to be immutable. */
  private final ImmutableMap<String, AnnotationInfo> knownImmutableClasses;

  /** Types that are known to be mutable. */
  private final ImmutableSet<String> knownUnsafeClasses;

  private WellKnownMutability(List<String> knownImmutable, List<String> knownUnsafe) {
    knownImmutableClasses = buildImmutableClasses(knownImmutable);
    knownUnsafeClasses = buildUnsafeClasses(knownUnsafe);
  }

  public static WellKnownMutability fromFlags(ErrorProneFlags flags) {
    List<String> immutable = flags.getList("Immutable:KnownImmutable").orElse(ImmutableList.of());
    List<String> unsafe = flags.getList("Immutable:KnownUnsafe").orElse(ImmutableList.of());
    return new WellKnownMutability(immutable, unsafe);
  }

  public Map<String, AnnotationInfo> getKnownImmutableClasses() {
    return knownImmutableClasses;
  }

  @Override
  public Map<String, AnnotationInfo> getKnownSafeClasses() {
    return getKnownImmutableClasses();
  }

  @Override
  public Set<String> getKnownUnsafeClasses() {
    return knownUnsafeClasses;
  }

  static class Builder {
    final ImmutableMap.Builder<String, AnnotationInfo> mapBuilder = ImmutableMap.builder();

    public Builder addClasses(Set<Class<?>> clazzs) {
      for (Class<?> clazz : clazzs) {
        add(clazz);
      }
      return this;
    }

    public Builder addStrings(List<String> classNames) {
      for (String className : classNames) {
        add(className);
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
          AnnotationInfo.create(clazz.getName(), ImmutableList.copyOf(containerOf)));
      return this;
    }

    public Builder add(String className, String... containerOf) {
      mapBuilder.put(
          className, AnnotationInfo.create(className, ImmutableList.copyOf(containerOf)));
      return this;
    }

    public ImmutableMap<String, AnnotationInfo> build() {
      return mapBuilder.build();
    }
  }

  // TODO(b/35724557): share this list with other code analyzing types for immutability
  // TODO(cushon): generate this at build-time to get type-safety without added compile-time deps
  private static ImmutableMap<String, AnnotationInfo> buildImmutableClasses(
      List<String> extraKnownImmutables) {
    return new Builder()
        .addStrings(extraKnownImmutables)
        .addClasses(Primitives.allPrimitiveTypes())
        .addClasses(Primitives.allWrapperTypes())
        .add("com.github.zafarkhaja.semver.Version")
        .add("com.google.protobuf.ByteString")
        .add("com.google.protobuf.Descriptors$Descriptor")
        .add("com.google.protobuf.Descriptors$EnumDescriptor")
        .add("com.google.protobuf.Descriptors$EnumValueDescriptor")
        .add("com.google.protobuf.Descriptors$FieldDescriptor")
        .add("com.google.protobuf.Descriptors$FileDescriptor")
        .add("com.google.protobuf.Descriptors$OneofDescriptor")
        .add("com.google.protobuf.Descriptors$ServiceDescriptor")
        .add("com.google.protobuf.Extension")
        .add("com.google.protobuf.ExtensionRegistry$ExtensionInfo")
        // NOTE: GeneratedMessage is included here for external tests that use this class for
        // checking whether classes are immutable. Within error prone, protos are verified
        // using isProto2MessageClass from this class.
        .add("com.google.protobuf.GeneratedMessage")
        .add("com.google.re2j.Pattern")
        .add("com.google.inject.TypeLiteral")
        .add("com.google.inject.Key")
        .add(com.google.common.base.CharMatcher.class)
        .add(com.google.common.base.Converter.class)
        .add(com.google.common.base.Joiner.class)
        .add(com.google.common.base.Optional.class, "T")
        .add(com.google.common.base.Splitter.class)
        .add(com.google.common.collect.ContiguousSet.class, "C")
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
        .add("com.google.common.hash.AbstractHashFunction") // package-private
        .add(com.google.common.hash.HashCode.class)
        .add(com.google.common.io.BaseEncoding.class)
        .add(com.google.common.net.MediaType.class)
        .add(com.google.common.primitives.UnsignedInteger.class)
        .add(com.google.common.primitives.UnsignedLong.class)
        .add("com.ibm.icu.number.LocalizedNumberFormatter")
        .add("com.ibm.icu.number.LocalizedNumberRangeFormatter")
        .add("com.ibm.icu.number.UnlocalizedNumberFormatter")
        .add("com.ibm.icu.number.UnlocalizedNumberRangeFormatter")
        .add("com.ibm.icu.util.Currency")
        .add("com.ibm.icu.util.ULocale")
        .add(java.lang.Class.class)
        .add(java.lang.String.class)
        .add(java.lang.annotation.Annotation.class)
        .add(java.math.BigDecimal.class)
        .add(java.math.BigInteger.class)
        .add(java.net.InetAddress.class)
        .add(java.net.URI.class)
        .add(java.nio.ByteOrder.class)
        .add(java.nio.charset.Charset.class)
        .add(java.nio.file.Path.class)
        .add(java.nio.file.WatchEvent.class)
        .add(java.nio.file.attribute.AclEntry.class)
        .add(java.nio.file.attribute.FileTime.class)
        .add(java.util.UUID.class)
        .add(java.util.Locale.class)
        .add(java.util.regex.Pattern.class)
        .add("android.net.Uri")
        .add("java.util.AbstractMap$SimpleImmutableEntry", "K", "V")
        .add("java.util.Optional", "T")
        .add("java.util.OptionalDouble")
        .add("java.util.OptionalInt")
        .add("java.util.OptionalLong")
        .add("java.time.Clock")
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
        .add("org.threeten.bp.Duration")
        .add("org.threeten.bp.Instant")
        .add("org.threeten.bp.LocalDate")
        .add("org.threeten.bp.LocalDateTime")
        .add("org.threeten.bp.LocalTime")
        .add("org.threeten.bp.MonthDay")
        .add("org.threeten.bp.OffsetDateTime")
        .add("org.threeten.bp.OffsetTime")
        .add("org.threeten.bp.Period")
        .add("org.threeten.bp.Year")
        .add("org.threeten.bp.YearMonth")
        .add("org.threeten.bp.ZoneId")
        .add("org.threeten.bp.ZoneOffset")
        .add("org.threeten.bp.ZonedDateTime")
        .add("org.threeten.bp.chrono.AbstractChronology")
        .add("org.threeten.bp.chrono.ChronoLocalDate")
        .add("org.threeten.bp.chrono.ChronoLocalDateTime", "D")
        .add("org.threeten.bp.chrono.ChronoPeriod")
        .add("org.threeten.bp.chrono.ChronoZonedDateTime", "D")
        .add("org.threeten.bp.chrono.Chronology")
        .add("org.threeten.bp.chrono.Era")
        .add("org.threeten.bp.chrono.HijrahChronology")
        .add("org.threeten.bp.chrono.HijrahDate")
        .add("org.threeten.bp.chrono.IsoChronology")
        .add("org.threeten.bp.chrono.JapaneseChronology")
        .add("org.threeten.bp.chrono.JapaneseDate")
        .add("org.threeten.bp.chrono.JapaneseEra")
        .add("org.threeten.bp.chrono.MinguoChronology")
        .add("org.threeten.bp.chrono.MinguoDate")
        .add("org.threeten.bp.chrono.ThaiBuddhistChronology")
        .add("org.threeten.bp.chrono.ThaiBuddhistDate")
        .add("org.threeten.bp.format.DateTimeFormatter")
        .add("org.threeten.bp.format.DecimalStyle")
        .add("org.threeten.bp.temporal.TemporalField")
        .add("org.threeten.bp.temporal.TemporalUnit")
        .add("org.threeten.bp.temporal.ValueRange")
        .add("org.threeten.bp.temporal.WeekFields")
        .add("org.threeten.bp.zone.ZoneOffsetTransition")
        .add("org.threeten.bp.zone.ZoneOffsetTransitionRule")
        .add("org.threeten.bp.zone.ZoneRules")
        .add("org.threeten.bp.zone.ZoneRulesProvider")
        .add("org.threeten.extra.Days")
        .add("org.threeten.extra.Hours")
        .add("org.threeten.extra.Interval")
        .add("org.threeten.extra.Minutes")
        .add("org.threeten.extra.Months")
        .add("org.threeten.extra.Seconds")
        .add("org.threeten.extra.Weeks")
        .add("org.threeten.extra.Years")
        .add("org.joda.time.DateTime")
        .add("org.joda.time.DateTimeZone")
        .add("org.joda.time.Days")
        .add("org.joda.time.Duration")
        .add("org.joda.time.Instant")
        .add("org.joda.time.Interval")
        .add("org.joda.time.LocalDate")
        .add("org.joda.time.LocalDateTime")
        .add("org.joda.time.Period")
        .add("org.joda.time.format.DateTimeFormatter")
        .add("org.openqa.selenium.Dimension")
        .add("org.openqa.selenium.DeviceRotation")
        .add("org.openqa.selenium.ImmutableCapabilities")
        .build();
  }

  private static ImmutableSet<String> buildUnsafeClasses(List<String> knownUnsafes) {
    return ImmutableSet.<String>builder()
        .addAll(knownUnsafes)
        .addAll(ImmutableCollections.MUTABLE_TO_IMMUTABLE_CLASS_NAME_MAP.keySet())
        .add("com.google.protobuf.util.FieldMaskUtil.MergeOptions")
        .add(java.util.BitSet.class.getName())
        .add(java.util.Calendar.class.getName())
        .add(java.lang.Iterable.class.getName())
        .add(java.lang.Object.class.getName())
        .add("java.text.DateFormat")
        .add(java.util.ArrayList.class.getName())
        .add(java.util.Collection.class.getName())
        .add(java.util.EnumMap.class.getName())
        .add(java.util.EnumSet.class.getName())
        .add(java.util.List.class.getName())
        .add(java.util.Map.class.getName())
        .add(java.util.HashMap.class.getName())
        .add(java.util.HashSet.class.getName())
        .add(java.util.NavigableMap.class.getName())
        .add(java.util.NavigableSet.class.getName())
        .add(java.util.TreeMap.class.getName())
        .add(java.util.TreeSet.class.getName())
        .add(java.util.Vector.class.getName())
        .add(java.util.Set.class.getName())
        .build();
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
    to = state.getTypes().erasure(to);
    return state.getTypes().isAssignable(type, to);
  }

  /**
   * Compile-time equivalent of {@code com.google.io.protocol.ProtocolSupport#isProto2MessageClass}.
   */
  public static boolean isProto2MessageClass(VisitorState state, Type type) {
    checkNotNull(type);
    return isAssignableTo(type, MESSAGE_TYPE, state)
        && !isAssignableTo(type, PROTOCOL_MESSAGE_TYPE, state);
  }

  /**
   * Compile-time equivalent of {@code
   * com.google.io.protocol.ProtocolSupport#isProto2MutableMessageClass}.
   */
  public static boolean isProto2MutableMessageClass(VisitorState state, Type type) {
    checkNotNull(type);
    return isAssignableTo(type, MUTABLE_MESSAGE_TYPE, state)
        && !isAssignableTo(type, PROTOCOL_MESSAGE_TYPE, state);
  }

  /** Returns true if the type is an annotation. */
  public static boolean isAnnotation(VisitorState state, Type type) {
    return isAssignableTo(type, Suppliers.ANNOTATION_TYPE, state);
  }
}
