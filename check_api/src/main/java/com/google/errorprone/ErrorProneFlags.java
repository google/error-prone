/*
 * Copyright 2017 The Error Prone Authors.
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

package com.google.errorprone;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represents an immutable map of Error Prone flags to their set values.
 *
 * <p>All get* methods return an {@code Optional<*>} containing the value for the given key, or
 * empty if the flag is unset.
 *
 * <p>To access ErrorProneFlags from a BugChecker class, add a constructor to the class that takes
 * one parameter of type ErrorProneFlags.
 *
 * <p>See <a href="https://errorprone.info/docs/flags">documentation</a> for full syntax description.
 */
public final class ErrorProneFlags implements Serializable {
  public static final String PREFIX = "-XepOpt:";

  private final ImmutableMap<String, String> flagsMap;

  public static ErrorProneFlags empty() {
    return new ErrorProneFlags(ImmutableMap.of());
  }

  public static ErrorProneFlags fromMap(Map<String, String> flagsMap) {
    return new ErrorProneFlags(ImmutableMap.copyOf(flagsMap));
  }

  private ErrorProneFlags(ImmutableMap<String, String> flagsMap) {
    this.flagsMap = flagsMap;
  }

  public ImmutableMap<String, String> getFlagsMap() {
    return ImmutableMap.copyOf(flagsMap);
  }

  /**
   * Gets flag value for the given key as a String, wrapped in an {@link Optional}, which is empty
   * if the flag is unset.
   */
  public Optional<String> get(String key) {
    return Optional.ofNullable(flagsMap.get(key));
  }

  /**
   * Gets the flag value for the given key as a Boolean, wrapped in an {@link Optional}, which is
   * empty if the flag is unset.
   *
   * <p>The value within the {@link Optional} will be {@code true} if the flag's value is "true",
   * {@code false} for "false", both case insensitive. If the value is neither "true" nor "false",
   * throws an {@link IllegalArgumentException}.
   *
   * <p>Note that any flag set without a value, e.g. {@code -XepOpt:FlagValue}, will be "true".
   */
  public Optional<Boolean> getBoolean(String key) {
    return this.get(key).map(ErrorProneFlags::parseBoolean);
  }

  /**
   * Gets the flag value for an enum of the given type, wrapped in an {@link Optional}, which is
   * empty if the flag is unset.
   */
  public <T extends Enum<T>> Optional<T> getEnum(String key, Class<T> clazz) {
    return this.get(key).map(value -> asEnumValue(key, value, clazz));
  }

  /**
   * Gets the flag value for a comma-separated set of enums of the given type, wrapped in an {@link
   * Optional}, which is empty if the flag is unset. If the flag is explicitly set to empty, an
   * empty set will be returned.
   */
  public <T extends Enum<T>> Optional<ImmutableSet<T>> getEnumSet(String key, Class<T> clazz) {
    return this.get(key)
        .map(
            value ->
                Streams.stream(Splitter.on(',').omitEmptyStrings().split(value))
                    .map(v -> asEnumValue(key, v, clazz))
                    .collect(toImmutableSet()));
  }

  private static <T extends Enum<T>> T asEnumValue(String key, String value, Class<T> clazz) {
    try {
      return Enum.valueOf(clazz, value);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          String.format(
              "Error Prone flag %s=%s could not be parsed as an enum constant of %s",
              key, value, clazz),
          e);
    }
  }

  private static boolean parseBoolean(String value) {
    if ("true".equalsIgnoreCase(value)) {
      return true;
    }
    if ("false".equalsIgnoreCase(value)) {
      return false;
    }
    throw new IllegalArgumentException(
        String.format("Error Prone flag value %s could not be parsed as a boolean.", value));
  }

  /**
   * Gets the flag value for the given key as an Integer, wrapped in an {@link Optional}, which is
   * empty if the flag is unset.
   *
   * <p>If the flag's value cannot be interpreted as an Integer, throws a {@link
   * NumberFormatException} (note: float values will *not* be interpreted as integers and will throw
   * an exception!)
   */
  public Optional<Integer> getInteger(String key) {
    return this.get(key).map(Integer::valueOf);
  }

  /**
   * Gets the flag value for the given key as a comma-separated {@link List} of Strings, wrapped in
   * an {@link Optional}, which is empty if the flag is unset.
   *
   * <p>(note: empty strings included, e.g. {@code "-XepOpt:List=,1,,2," => ["","1","","2",""]})
   */
  public Optional<List<String>> getList(String key) {
    return this.get(key).map(v -> ImmutableList.copyOf(Splitter.on(',').split(v)));
  }

  /**
   * Gets the flag value for the given key as a comma-separated {@link Set} of Strings, wrapped in
   * an {@link Optional}, which is empty if the flag is unset.
   *
   * <p>(note: empty strings included, e.g. {@code "-XepOpt:Set=,1,,1,2," => ["","1","2"]})
   */
  public Optional<Set<String>> getSet(String key) {
    return this.get(key).map(v -> ImmutableSet.copyOf(Splitter.on(',').split(v)));
  }

  /** Whether this Flags object is empty, i.e. no flags have been set. */
  public boolean isEmpty() {
    return this.flagsMap.isEmpty();
  }

  /**
   * Returns a new ErrorProneFlags object with the values of two ErrorProneFlags objects added
   * together. For flags that appear in both instances, the values in {@code other} override {@code
   * this}.
   */
  @CheckReturnValue
  public ErrorProneFlags plus(ErrorProneFlags other) {
    Map<String, String> combinedMaps = new HashMap<>(this.getFlagsMap());
    combinedMaps.putAll(other.getFlagsMap());
    return ErrorProneFlags.fromMap(combinedMaps);
  }


  /** Builder for Error Prone command-line flags object. Parses flags from strings. */
  public static class Builder {

    private final HashMap<String, String> flagsMap = new HashMap<>();

    private Builder() {}

    /**
     * Given a String custom flag in the format {@code "-XepOpt:FlagName=Value"}, places the flag in
     * this builder's dictionary, e.g. {@code flagsMap["FlagName"] = "Value"}
     */
    @CanIgnoreReturnValue
    public Builder parseFlag(String flag) {
      checkArgument(flag.startsWith(PREFIX));

      // Strip prefix
      String remaining = flag.substring(PREFIX.length());

      // Get key and value by splitting on first equals sign.
      String[] parts = remaining.split("=", 2);
      String key = parts[0];
      String value = parts.length < 2 ? "true" : parts[1];

      this.putFlag(key, value);
      return this;
    }

    /** Puts a key-value pair directly in this builder's dictionary. Mostly exists for testing. */
    @CanIgnoreReturnValue
    public Builder putFlag(String key, String value) {
      flagsMap.put(key, value);
      return this;
    }

    public ErrorProneFlags build() {
      return fromMap(flagsMap);
    }
  }

  public static Builder builder() {
    return new Builder();
  }
}
