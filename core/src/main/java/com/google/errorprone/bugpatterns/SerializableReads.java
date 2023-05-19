/*
 * Copyright 2022 The Error Prone Authors.
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

import com.google.common.collect.ImmutableSet;

/** List of banned methods for {@link BanSerializableRead}. */
public final class SerializableReads {
  private SerializableReads() {}

  public static final ImmutableSet<String> BANNED_OBJECT_INPUT_STREAM_METHODS =
      ImmutableSet.of(
          // Prevent reading objects unsafely into memory.
          "readObject",

          // This is the same, the default value.
          "defaultReadObject",

          // This is for trusted subclasses.
          "readObjectOverride",

          // Ultimately, a lot of the safety worries come from being able to construct arbitrary
          // classes via reading in class descriptors. I don't think anyone will bother calling this
          // directly, but I don't see  any reason not to block it.
          "readClassDescriptor",

          // These are basically the same as above.
          "resolveClass",
          "resolveObject");
}
