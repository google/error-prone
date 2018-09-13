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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.errorprone.bugpatterns.apidiff.ApiDiffProto.Diff;
import java.util.Set;

/** The difference between two APIs. */
@AutoValue
public abstract class ApiDiff {

  /** A per class unique identifier for a field or method. */
  @AutoValue
  public abstract static class ClassMemberKey {

    /** The simple name of the member. */
    public abstract String identifier();

    /** The JVMS 4.3 member descriptor. */
    public abstract String descriptor();

    public static ClassMemberKey create(String identifier, String descriptor) {
      return new AutoValue_ApiDiff_ClassMemberKey(identifier, descriptor);
    }

    @Override
    public final String toString() {
      return String.format("%s:%s", identifier(), descriptor());
    }
  }

  /** Binary names of classes only present in the new API. */
  public abstract ImmutableSet<String> unsupportedClasses();

  /** Members only present in the new API, grouped by binary name of their declaring class. */
  public abstract ImmutableSetMultimap<String, ClassMemberKey> unsupportedMembersByClass();

  /** Returns true if the class with the given binary name is unsupported. */
  boolean isClassUnsupported(String className) {
    return unsupportedClasses().contains(className);
  }

  /** Returns true if the member with the given declaring class is unsupported. */
  boolean isMemberUnsupported(String className, ClassMemberKey memberKey) {
    return unsupportedMembersByClass().containsEntry(className, memberKey)
        || unsupportedMembersByClass()
            .containsEntry(className, ClassMemberKey.create(memberKey.identifier(), ""));
  }

  public static ApiDiff fromMembers(
      Set<String> unsupportedClasses, Multimap<String, ClassMemberKey> unsupportedMembersByClass) {
    return new AutoValue_ApiDiff(
        ImmutableSet.copyOf(unsupportedClasses),
        ImmutableSetMultimap.copyOf(unsupportedMembersByClass));
  }

  /** Converts a {@link Diff} to a {@link ApiDiff}. */
  public static ApiDiff fromProto(Diff diff) {
    ImmutableSet.Builder<String> unsupportedClasses = ImmutableSet.builder();
    ImmutableSetMultimap.Builder<String, ClassMemberKey> unsupportedMembersByClass =
        ImmutableSetMultimap.builder();
    for (ApiDiffProto.ClassDiff c : diff.getClassDiffList()) {
      switch (c.getDiffCase()) {
        case EVERYTHING_DIFF:
          unsupportedClasses.add(c.getEverythingDiff().getClassName());
          break;
        case MEMBER_DIFF:
          ApiDiffProto.MemberDiff memberDiff = c.getMemberDiff();
          for (ApiDiffProto.ClassMember member : memberDiff.getMemberList()) {
            unsupportedMembersByClass.put(
                memberDiff.getClassName(),
                ClassMemberKey.create(member.getIdentifier(), member.getMemberDescriptor()));
          }
          break;
        default:
          throw new AssertionError(c.getDiffCase());
      }
    }
    return new AutoValue_ApiDiff(unsupportedClasses.build(), unsupportedMembersByClass.build());
  }

  /** Converts a {@link ApiDiff} to a {@link ApiDiffProto.Diff}. */
  public Diff toProto() {
    ApiDiffProto.Diff.Builder builder = ApiDiffProto.Diff.newBuilder();
    for (String className : unsupportedClasses()) {
      builder.addClassDiff(
          ApiDiffProto.ClassDiff.newBuilder()
              .setEverythingDiff(ApiDiffProto.EverythingDiff.newBuilder().setClassName(className)));
    }
    for (String className : unsupportedMembersByClass().keySet()) {
      ApiDiffProto.MemberDiff.Builder memberDiff =
          ApiDiffProto.MemberDiff.newBuilder().setClassName(className);
      for (ClassMemberKey member : unsupportedMembersByClass().get(className)) {
        memberDiff.addMember(
            ApiDiffProto.ClassMember.newBuilder()
                .setIdentifier(member.identifier())
                .setMemberDescriptor(member.descriptor()));
      }
      builder.addClassDiff(ApiDiffProto.ClassDiff.newBuilder().setMemberDiff(memberDiff));
    }
    return builder.build();
  }
}
