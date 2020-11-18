/*
 * Copyright 2018 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.apidiff;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.bugpatterns.apidiff.ApiDiff.ClassMemberKey;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Checks for uses of classes, fields, or methods that are not compatible with legacy Android
 * devices. As of Android N, that includes all of JDK8 (which is only supported on Nougat) except
 * type and repeated annotations, which are compiled in a backwards compatible way.
 */
@BugPattern(
    name = "AndroidJdkLibsChecker",
    altNames = {"Java7ApiChecker", "AndroidApiChecker"},
    summary = "Use of class, field, or method that is not compatible with legacy Android devices",
    severity = ERROR)
// TODO(b/32513850): Allow Android N+ APIs, e.g., by computing API diff using android.jar
public class AndroidJdkLibsChecker extends ApiDiffChecker {

  public AndroidJdkLibsChecker(ErrorProneFlags flags) {
    this(flags.getBoolean("Android:Java8Libs").orElse(false));
  }

  public AndroidJdkLibsChecker() {
    this(false);
  }

  private AndroidJdkLibsChecker(boolean allowJava8) {
    super(deriveApiDiff(allowJava8));
  }

  private static ApiDiff deriveApiDiff(boolean allowJava8) {
    ClassSupportInfo support = new ClassSupportInfo(allowJava8);
    ImmutableSet<String> unsupportedClasses =
        ImmutableSet.<String>builder()
            .addAll(
                Java7ApiChecker.API_DIFF.unsupportedClasses().stream()
                    .filter(cls -> !support.allowedPackages.contains(packageName(cls)))
                    .filter(cls -> !support.allowedClasses.contains(cls))
                    .collect(Collectors.toSet()))
            .addAll(support.bannedClasses)
            .build();

    ImmutableMultimap<String, ClassMemberKey> unsupportedMembers =
        ImmutableSetMultimap.<String, ClassMemberKey>builder()
            .putAll(
                Java7ApiChecker.API_DIFF.unsupportedMembersByClass().entries().stream()
                    .filter(e -> !support.allowedPackages.contains(packageName(e.getKey())))
                    .filter(e -> !support.allowedClasses.contains(e.getKey()))
                    .filter(e -> support.bannedMembers.isEmpty() || !support.memberIsAllowed(e))
                    .collect(Collectors.toSet()))
            .putAll(support.bannedMembers)
            .build();

    return ApiDiff.fromMembers(unsupportedClasses, unsupportedMembers);
  }

  private static String packageName(String className) {
    return className.substring(0, className.lastIndexOf('/') + 1);
  }

  private static class ClassSupportInfo {

    private final ImmutableSet<String> allowedPackages;
    private final ImmutableSet<String> allowedClasses;
    private final ImmutableSet<String> bannedClasses;
    private final ImmutableSetMultimap<String, ClassMemberKey> allowedMembers;
    private final ImmutableSetMultimap<String, ClassMemberKey> bannedMembers;

    ClassSupportInfo(boolean allowJava8) {
      allowedPackages = allowJava8 ? DESUGAR_ALLOWED_PACKAGES : ImmutableSet.of();
      allowedClasses = allowJava8 ? DESUGAR_ALLOWED_CLASSES : BASE_ALLOWED_CLASSES;
      bannedClasses = BASE_BANNED_CLASSES;
      allowedMembers = allowJava8 ? DESUGAR_ALLOWED_MEMBERS : ImmutableSetMultimap.of();
      bannedMembers = allowJava8 ? DESUGAR_BANNED_MEMBERS : ImmutableSetMultimap.of();
    }

    private boolean memberIsAllowed(Map.Entry<String, ClassMemberKey> member) {
      return allowedMembers.containsEntry(member.getKey(), member.getValue())
          || allowedMembers.get(member.getKey()).stream()
              .anyMatch(
                  memberKey ->
                      memberKey.descriptor().isEmpty()
                          && memberKey.identifier().equals(member.getValue().identifier()));
    }

    // Desugar support info from
    // https://github.com/bazelbuild/bazel/blob/master/tools/android/desugar.sh
    private static final ImmutableSet<String> DESUGAR_ALLOWED_PACKAGES =
        ImmutableSet.of(
            "java/time/", //
            "java/time/chrono/",
            "java/time/temporal/",
            "java/time/zone/",
            "java/util/stream/",
            "java/util/function/");
    private static final ImmutableSet<String> BASE_ALLOWED_CLASSES =
        ImmutableSet.of(
            "java/lang/annotation/Repeatable", //
            "java/lang/annotation/ElementType");
    private static final ImmutableSet<String> DESUGAR_ALLOWED_CLASSES =
        ImmutableSet.<String>builder()
            .addAll(BASE_ALLOWED_CLASSES)
            .add("java/io/UncheckedIOException")
            .add("java/util/ArrayList")
            .add("java/util/Collection")
            .add("java/util/Comparator")
            .add("java/util/DoubleSummaryStatistics")
            .add("java/util/IntSummaryStatistics")
            .add("java/util/Iterator")
            .add("java/util/List")
            .add("java/util/LongSummaryStatistics")
            .add("java/util/Map")
            .add("java/util/HashMap")
            .add("java/util/Map\\$$Entry")
            .add("java/util/Objects")
            .add("java/util/Optional")
            .add("java/util/OptionalDouble")
            .add("java/util/OptionalInt")
            .add("java/util/OptionalLong")
            .add("java/util/PrimitiveIterator")
            .add("java/util/Set")
            .add("java/util/Spliterator")
            .add("java/util/Spliterator$OfDouble")
            .add("java/util/Spliterator$OfInt")
            .add("java/util/Spliterator$OfLong")
            .add("java/util/Spliterator$OfPrimitive")
            .add("java/util/Spliterators")
            .add("java/util/Spliterators$AbstractDoubleSpliterator")
            .add("java/util/Spliterators$AbstractIntSpliterator")
            .add("java/util/Spliterators$AbstractLongSpliterator")
            .add("java/util/Spliterators$AbstractSpliterator")
            .add("java/util/StringJoiner")
            .add("java/util/concurrent/ConcurrentHashMap")
            .add("java/util/concurrent/ConcurrentMap")
            .add("java/util/concurrent/atomic/AtomicInteger")
            .add("java/util/concurrent/atomic/AtomicLong")
            .add("java/util/concurrent/atomic/AtomicReference")
            .build();
    private static final ImmutableSet<String> BASE_BANNED_CLASSES =
        // see b/72354470, https://github.com/typetools/checker-framework/issues/1781
        ImmutableSet.of("javax/lang/model/type/TypeKind");
    // Descriptor is empty string to match on any member with same simple name.
    private static final ImmutableSetMultimap<String, ClassMemberKey> DESUGAR_ALLOWED_MEMBERS =
        ImmutableSetMultimap.<String, ClassMemberKey>builder()
            .put("java/util/Arrays", ClassMemberKey.create("stream", ""))
            .put("java/util/Date", ClassMemberKey.create("from", ""))
            .put("java/util/GregorianCalendar", ClassMemberKey.create("from", ""))
            .put("java/util/TimeZone", ClassMemberKey.create("getTimeZone", ""))
            .put("java/lang/Integer", ClassMemberKey.create("sum", ""))
            .put("java/lang/Long", ClassMemberKey.create("sum", ""))
            .put("java/lang/Double", ClassMemberKey.create("sum", ""))
            .put("java/lang/Integer", ClassMemberKey.create("min", ""))
            .put("java/lang/Long", ClassMemberKey.create("min", ""))
            .put("java/lang/Double", ClassMemberKey.create("min", ""))
            .put("java/lang/Integer", ClassMemberKey.create("max", ""))
            .put("java/lang/Long", ClassMemberKey.create("max", ""))
            .put("java/lang/Double", ClassMemberKey.create("max", ""))
            .put("java/lang/Math", ClassMemberKey.create("toIntExact", ""))
            .build();
    private static final ImmutableSetMultimap<String, ClassMemberKey> DESUGAR_BANNED_MEMBERS =
        ImmutableSetMultimap.<String, ClassMemberKey>builder()
            .put("java/util/Collection", ClassMemberKey.create("parallelStream", ""))
            .put("java/util/stream/BaseStream", ClassMemberKey.create("parallel", ""))
            .build();
  }
}
