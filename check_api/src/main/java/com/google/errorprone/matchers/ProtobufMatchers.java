/*
 * Copyright 2026 The Error Prone Authors.
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

package com.google.errorprone.matchers;

import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.predicates.TypePredicates.allOf;
import static com.google.errorprone.predicates.TypePredicates.isDescendantOf;
import static com.google.errorprone.predicates.TypePredicates.isExactType;
import static javax.lang.model.element.ElementKind.ENUM;

import com.google.errorprone.ClassName;
import com.google.errorprone.predicates.TypePredicate;
import com.google.errorprone.predicates.TypePredicates;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Type;
import org.safere.Pattern;

/** Matchers and utility constants for Protocol Buffers (v2, v3, and Lite). */
public final class ProtobufMatchers {

  public static final String MESSAGE_LITE_CLASS = "com.google.protobuf.MessageLite";
  public static final String MESSAGE_CLASS = "com.google.protobuf.Message";
  public static final String GENERATED_MESSAGE_CLASS = "com.google.protobuf.GeneratedMessage";
  public static final String GENERATED_MESSAGE_LITE_CLASS =
      "com.google.protobuf.GeneratedMessageLite";
  public static final String MUTABLE_MESSAGE_LITE_CLASS = "com.google.protobuf.MutableMessageLite";
  public static final String DYNAMIC_MESSAGE_CLASS = "com.google.protobuf.DynamicMessage";
  public static final String ABSTRACT_MESSAGE_LITE_CLASS =
      "com.google.protobuf.AbstractMessageLite";

  public static final String MESSAGE_LITE_BUILDER_CLASS = "com.google.protobuf.MessageLite.Builder";
  public static final String MESSAGE_BUILDER_CLASS = "com.google.protobuf.Message.Builder";
  public static final String GENERATED_MESSAGE_BUILDER_CLASS =
      "com.google.protobuf.GeneratedMessage.Builder";
  public static final String GENERATED_MESSAGE_LITE_BUILDER_CLASS =
      "com.google.protobuf.GeneratedMessageLite.Builder";
  public static final String MESSAGE_LITE_OR_BUILDER_CLASS =
      "com.google.protobuf.MessageLiteOrBuilder";
  public static final String MESSAGE_OR_BUILDER_CLASS = "com.google.protobuf.MessageOrBuilder";
  public static final String EXTENDABLE_MESSAGE_CLASS =
      "com.google.protobuf.GeneratedMessage.ExtendableMessage";
  public static final String EXTENDABLE_MESSAGE_LITE_CLASS =
      "com.google.protobuf.GeneratedMessageLite.ExtendableMessage";

  public static final String ENUM_LITE_CLASS = "com.google.protobuf.Internal.EnumLite";
  public static final String PROTOCOL_MESSAGE_ENUM_CLASS =
      "com.google.protobuf.ProtocolMessageEnum";
  public static final String INTERNAL_ONE_OF_ENUM_CLASS =
      "com.google.protobuf.AbstractMessageLite.InternalOneOfEnum";

  public static final String BYTE_STRING_CLASS = "com.google.protobuf.ByteString";
  public static final String PROTOCOL_STRING_LIST_CLASS = "com.google.protobuf.ProtocolStringList";
  public static final String UNKNOWN_FIELD_SET_CLASS = "com.google.protobuf.UnknownFieldSet";
  public static final String EXTENSION_LITE_CLASS = "com.google.protobuf.ExtensionLite";
  public static final String EXTENSION_CLASS = "com.google.protobuf.Extension";
  public static final String DESCRIPTOR_CLASS = "com.google.protobuf.Descriptors.Descriptor";
  public static final String FIELD_DESCRIPTOR_CLASS =
      "com.google.protobuf.Descriptors.FieldDescriptor";
  public static final String ENUM_DESCRIPTOR_CLASS =
      "com.google.protobuf.Descriptors.EnumDescriptor";
  public static final String ENUM_VALUE_DESCRIPTOR_CLASS =
      "com.google.protobuf.Descriptors.EnumValueDescriptor";
  public static final String FILE_DESCRIPTOR_CLASS =
      "com.google.protobuf.Descriptors.FileDescriptor";
  public static final String ONEOF_DESCRIPTOR_CLASS =
      "com.google.protobuf.Descriptors.OneofDescriptor";
  public static final String SERVICE_DESCRIPTOR_CLASS =
      "com.google.protobuf.Descriptors.ServiceDescriptor";

  public static final String PROTO_DURATION_CLASS = "com.google.protobuf.Duration";
  public static final String PROTO_TIMESTAMP_CLASS = "com.google.protobuf.Timestamp";
  public static final String PROTO_DURATIONS_UTIL_CLASS = "com.google.protobuf.util.Durations";
  public static final String PROTO_TIMESTAMPS_UTIL_CLASS = "com.google.protobuf.util.Timestamps";
  public static final String PROTO_FIELD_MASK_UTIL_CLASS = "com.google.protobuf.util.FieldMaskUtil";

  public static final ClassName MESSAGE_LITE = ClassName.of(MESSAGE_LITE_CLASS);
  public static final ClassName MESSAGE = ClassName.of(MESSAGE_CLASS);
  public static final ClassName GENERATED_MESSAGE = ClassName.of(GENERATED_MESSAGE_CLASS);
  public static final ClassName GENERATED_MESSAGE_LITE = ClassName.of(GENERATED_MESSAGE_LITE_CLASS);
  public static final ClassName MUTABLE_MESSAGE_LITE = ClassName.of(MUTABLE_MESSAGE_LITE_CLASS);
  public static final ClassName DYNAMIC_MESSAGE = ClassName.of(DYNAMIC_MESSAGE_CLASS);
  public static final ClassName ABSTRACT_MESSAGE_LITE = ClassName.of(ABSTRACT_MESSAGE_LITE_CLASS);

  public static final ClassName MESSAGE_LITE_BUILDER = ClassName.of(MESSAGE_LITE_BUILDER_CLASS);
  public static final ClassName MESSAGE_BUILDER = ClassName.of(MESSAGE_BUILDER_CLASS);
  public static final ClassName GENERATED_MESSAGE_BUILDER =
      ClassName.of(GENERATED_MESSAGE_BUILDER_CLASS);
  public static final ClassName GENERATED_MESSAGE_LITE_BUILDER =
      ClassName.of(GENERATED_MESSAGE_LITE_BUILDER_CLASS);
  public static final ClassName MESSAGE_LITE_OR_BUILDER =
      ClassName.of(MESSAGE_LITE_OR_BUILDER_CLASS);
  public static final ClassName MESSAGE_OR_BUILDER = ClassName.of(MESSAGE_OR_BUILDER_CLASS);
  public static final ClassName EXTENDABLE_MESSAGE = ClassName.of(EXTENDABLE_MESSAGE_CLASS);
  public static final ClassName EXTENDABLE_MESSAGE_LITE =
      ClassName.of(EXTENDABLE_MESSAGE_LITE_CLASS);

  public static final ClassName ENUM_LITE = ClassName.of(ENUM_LITE_CLASS);
  public static final ClassName PROTOCOL_MESSAGE_ENUM = ClassName.of(PROTOCOL_MESSAGE_ENUM_CLASS);
  public static final ClassName INTERNAL_ONE_OF_ENUM = ClassName.of(INTERNAL_ONE_OF_ENUM_CLASS);

  public static final ClassName BYTE_STRING = ClassName.of(BYTE_STRING_CLASS);
  public static final ClassName PROTOCOL_STRING_LIST = ClassName.of(PROTOCOL_STRING_LIST_CLASS);
  public static final ClassName UNKNOWN_FIELD_SET = ClassName.of(UNKNOWN_FIELD_SET_CLASS);
  public static final ClassName EXTENSION_LITE = ClassName.of(EXTENSION_LITE_CLASS);
  public static final ClassName EXTENSION = ClassName.of(EXTENSION_CLASS);
  public static final ClassName DESCRIPTOR = ClassName.of(DESCRIPTOR_CLASS);
  public static final ClassName FIELD_DESCRIPTOR = ClassName.of(FIELD_DESCRIPTOR_CLASS);
  public static final ClassName ENUM_DESCRIPTOR = ClassName.of(ENUM_DESCRIPTOR_CLASS);
  public static final ClassName ENUM_VALUE_DESCRIPTOR = ClassName.of(ENUM_VALUE_DESCRIPTOR_CLASS);
  public static final ClassName FILE_DESCRIPTOR = ClassName.of(FILE_DESCRIPTOR_CLASS);
  public static final ClassName ONEOF_DESCRIPTOR = ClassName.of(ONEOF_DESCRIPTOR_CLASS);
  public static final ClassName SERVICE_DESCRIPTOR = ClassName.of(SERVICE_DESCRIPTOR_CLASS);

  public static final ClassName PROTO_DURATION = ClassName.of(PROTO_DURATION_CLASS);
  public static final ClassName PROTO_TIMESTAMP = ClassName.of(PROTO_TIMESTAMP_CLASS);
  public static final ClassName PROTO_DURATIONS_UTIL = ClassName.of(PROTO_DURATIONS_UTIL_CLASS);
  public static final ClassName PROTO_TIMESTAMPS_UTIL = ClassName.of(PROTO_TIMESTAMPS_UTIL_CLASS);
  public static final ClassName PROTO_FIELD_MASK_UTIL = ClassName.of(PROTO_FIELD_MASK_UTIL_CLASS);

  public static final Supplier<Type> MESSAGE_LITE_TYPE = MESSAGE_LITE.asTypeSupplier();
  public static final Supplier<Type> MESSAGE_TYPE = MESSAGE.asTypeSupplier();
  public static final Supplier<Type> GENERATED_MESSAGE_TYPE = GENERATED_MESSAGE.asTypeSupplier();
  public static final Supplier<Type> GENERATED_MESSAGE_LITE_TYPE =
      GENERATED_MESSAGE_LITE.asTypeSupplier();
  public static final Supplier<Type> MUTABLE_MESSAGE_LITE_TYPE =
      MUTABLE_MESSAGE_LITE.asTypeSupplier();
  public static final Supplier<Type> DYNAMIC_MESSAGE_TYPE = DYNAMIC_MESSAGE.asTypeSupplier();
  public static final Supplier<Type> MESSAGE_LITE_BUILDER_TYPE =
      MESSAGE_LITE_BUILDER.asTypeSupplier();
  public static final Supplier<Type> MESSAGE_BUILDER_TYPE = MESSAGE_BUILDER.asTypeSupplier();
  public static final Supplier<Type> MESSAGE_LITE_OR_BUILDER_TYPE =
      MESSAGE_LITE_OR_BUILDER.asTypeSupplier();
  public static final Supplier<Type> ENUM_LITE_TYPE = ENUM_LITE.asTypeSupplier();
  public static final Supplier<Type> PROTOCOL_MESSAGE_ENUM_TYPE =
      PROTOCOL_MESSAGE_ENUM.asTypeSupplier();
  public static final Supplier<Type> EXTENSION_LITE_TYPE = EXTENSION_LITE.asTypeSupplier();

  /** Matches specifically lite proto messages (excluding full Message and UnknownFieldSet). */
  public static final TypePredicate IS_LITE_PROTO =
      allOf(
          isDescendantOf(MESSAGE_LITE),
          TypePredicates.not(isDescendantOf(MESSAGE)),
          TypePredicates.not(isExactType(UNKNOWN_FIELD_SET)));

  /** Matches specifically lite proto enums (excluding full enum descriptors and oneof enums). */
  public static final TypePredicate IS_LITE_PROTO_ENUM =
      allOf(
          isDescendantOf(ENUM_LITE),
          TypePredicates.not(isDescendantOf(PROTOCOL_MESSAGE_ENUM)),
          TypePredicates.not(isDescendantOf(ENUM_VALUE_DESCRIPTOR)),
          TypePredicates.not(isDescendantOf(INTERNAL_ONE_OF_ENUM)));

  /** Matches a oneof enum (AbstractMessageLite.InternalOneOfEnum). */
  public static final TypePredicate IS_ONEOF_ENUM =
      allOf(
          isDescendantOf(INTERNAL_ONE_OF_ENUM),
          (type, state) -> type != null && type.tsym != null && type.tsym.getKind() == ENUM);

  /** Matches builder {@code build()} or {@code buildPartial()} invocations. */
  public static final Matcher<ExpressionTree> PROTO_BUILD_METHOD =
      instanceMethod().onDescendantOf(MESSAGE_LITE_BUILDER).namedAnyOf("build", "buildPartial");

  /** Matches static {@code newBuilder()} calls on proto messages. */
  public static final Matcher<ExpressionTree> PROTO_NEW_BUILDER_METHOD =
      staticMethod().onDescendantOf(MESSAGE_LITE).named("newBuilder");

  /** Matches {@code toBuilder()} or {@code newBuilderForType()} calls on proto messages. */
  public static final Matcher<ExpressionTree> PROTO_TO_BUILDER_METHOD =
      instanceMethod().onDescendantOf(MESSAGE_LITE).namedAnyOf("toBuilder", "newBuilderForType");

  private static final Pattern GETTER_PATTERN = Pattern.compile("get.+");
  private static final Pattern MUTATOR_PATTERN =
      Pattern.compile("^(set|add|clear|put|merge|remove).*");

  /** Matches immutable proto getters, excluding size/serialization and framework methods. */
  public static final Matcher<ExpressionTree> PROTO_GETTER =
      allOf(
          instanceMethod().onDescendantOf(MESSAGE_LITE_OR_BUILDER).withNameMatching(GETTER_PATTERN),
          not(
              instanceMethod()
                  .anyClass()
                  .namedAnyOf(
                      "getCachedSize",
                      "getSerializedSize",
                      "getDefaultInstanceForType",
                      "getDefaultInstance",
                      "getDescriptorForType",
                      "getParserForType")));

  /**
   * Matches proto builder mutators (e.g. {@code setFoo()}, {@code addFoo()}, {@code clearFoo()},
   * {@code putFoo()}).
   */
  public static final Matcher<ExpressionTree> PROTO_BUILDER_MUTATOR =
      instanceMethod().onDescendantOf(MESSAGE_LITE_BUILDER).withNameMatching(MUTATOR_PATTERN);

  /**
   * Matches static factory methods on {@code com.google.protobuf.util.Timestamps} and {@code
   * com.google.protobuf.util.Durations} that construct time protos from numeric quantities (e.g.
   * {@code fromSeconds()}, {@code fromMillis()}, {@code fromNanos()}).
   */
  public static final Matcher<ExpressionTree> PROTO_TIME_STATIC_FACTORIES =
      anyOf(
          staticMethod()
              .onClass(PROTO_TIMESTAMPS_UTIL)
              .namedAnyOf("fromNanos", "fromMicros", "fromMillis", "fromSeconds"),
          staticMethod()
              .onClass(PROTO_DURATIONS_UTIL)
              .namedAnyOf(
                  "fromNanos",
                  "fromMicros",
                  "fromMillis",
                  "fromSeconds",
                  "fromMinutes",
                  "fromHours",
                  "fromDays"));

  private ProtobufMatchers() {}
}
