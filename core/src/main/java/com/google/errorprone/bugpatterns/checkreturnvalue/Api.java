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
package com.google.errorprone.bugpatterns.checkreturnvalue;

import static com.google.common.base.CharMatcher.whitespace;
import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.Character.isJavaIdentifierStart;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CompileTimeConstant;

/**
 * Represents a Java method or constructor.
 *
 * <p>Provides a method to parse an API from a string format, and emit an API as the same sting.
 */
// TODO(kak): do we want to be able to represent classes in addition to methods/constructors?
// TODO(kak): if not, then consider renaming to `MethodSignature` or something
@AutoValue
public abstract class Api {

  private static final Joiner COMMA_JOINER = Joiner.on(',');

  /** Returns the fully qualified type that contains the given method/constructor. */
  public abstract String className();

  /**
   * Returns the simple name of the method. If the API is a constructor (i.e., {@code
   * isConstructor() == true}), then {@code "<init>"} is returned.
   */
  public abstract String methodName();

  /** Returns the list of fully qualified parameter types for the given method/constructor. */
  public abstract ImmutableList<String> parameterTypes();

  @Override
  public final String toString() {
    return String.format(
        "%s#%s(%s)", className(), methodName(), COMMA_JOINER.join(parameterTypes()));
  }

  /** Returns whether this API represents a constructor or not. */
  boolean isConstructor() {
    return methodName().equals("<init>");
  }

  /**
   * Parses an API string into an {@link Api}, ignoring trailing or inner whitespace between names.
   *
   * <p>Example API strings are:
   *
   * <ul>
   *   <li>a constructor (e.g., {@code java.net.URI#<init>(java.lang.String)})
   *   <li>a static method (e.g., {@code java.net.URI#create(java.lang.String)})
   *   <li>an instance method (e.g., {@code java.util.List#get(int)})
   *   <li>an instance method with types erased (e.g., {@code java.util.List#add(java.lang.Object)})
   * </ul>
   *
   * @throws IllegalArgumentException when {@code api} is not well-formed
   */
  @VisibleForTesting
  public static Api parse(String api) {
    return parse(api, false);
  }

  /**
   * Parses an API string that was already known to not include any leading, trailing, or inner
   * whitespace.
   *
   * <p>If the API string contains whitespace, this method may produce an API object with extraneous
   * spaces attached, or may treat it as malformed (and throw an exception).
   *
   * @throws IllegalArgumentException when {@code api} is not well-formed
   */
  // TODO(glorioso): Refactoring has shown the folly of this method. It's probably not useful, since
  //   if we're _parsing_ into API objects, there's not as much of a performance concern for
  //   touching whitespaces. If we use bare string identifiers instead, whitespaces are problematic,
  //   but this code wouldn't be involved.
  static Api parseFromStringWithoutWhitespace(String api) {
    return parse(api, true);
  }

  static Api internalCreate(String className, String methodName, ImmutableList<String> params) {
    return new AutoValue_Api(className, methodName, params);
  }

  /**
   * Parses an API string into an {@link Api}. Example API strings are:
   *
   * <ul>
   *   <li>a constructor (e.g., {@code java.net.URI#<init>(java.lang.String)})
   *   <li>a static method (e.g., {@code java.net.URI#create(java.lang.String)})
   *   <li>an instance method (e.g., {@code java.util.List#get(int)})
   *   <li>an instance method with types erased (e.g., {@code java.util.List#add(java.lang.Object)})
   * </ul>
   */
  private static Api parse(String api, boolean assumeNoWhitespace) {
    Parser p = new Parser(api, assumeNoWhitespace);

    // Let's parse this in 3 parts:
    //   * Fully-qualified owning name, followed by #
    //   * method name, or "<init>", followed by (
    //   * Any number of parameter types, all but the last followed by a ',', Finishing with )
    //   * and nothing at the end.

    String className = p.owningType();
    String methodName = p.methodName();
    ImmutableList<String> paramList = p.parameters();
    p.ensureNoMoreCharacters();

    return internalCreate(className, methodName, paramList);
  }

  private static final class Parser {
    private final String api;
    private final boolean assumeNoWhitespace;
    private int position = -1;

    Parser(String api, boolean assumeNoWhitespace) {
      this.api = api;
      this.assumeNoWhitespace = assumeNoWhitespace;
    }

    String owningType() {
      StringBuilder buffer = new StringBuilder(api.length());
      token:
      do {
        char next = nextLookingFor('#');
        switch (next) {
          case '#':
            // We've hit the end of the leading type, break out.
            break token;
          case '.':
            // OK, separator
            break;
          default:
            checkArgument(
                isJavaIdentifierPart(next),
                "Unable to parse '%s' because '%s' is not a valid identifier",
                api,
                next);
        }
        buffer.append(next);
      } while (true);
      String type = buffer.toString();

      check(!type.isEmpty(), api, "class name cannot be empty");
      check(
          isJavaIdentifierStart(type.charAt(0)),
          api,
          "the class name must start with a valid character");
      return type;
    }

    String methodName() {
      StringBuilder buffer = new StringBuilder(api.length() - position);
      boolean isConstructor = false;
      boolean finishedConstructor = false;
      // match "<init>", or otherwise a normal method name
      token:
      do {
        char next = nextLookingFor('(');
        switch (next) {
          case '(':
            // We've hit the end of the method name, break out.
            break token;
          case '<':
            // Starting a constructor
            check(!isConstructor, api, "Only one '<' is allowed");
            check(buffer.length() == 0, api, "'<' must come directly after '#'");
            isConstructor = true;
            break;
          case '>':
            check(isConstructor, api, "'<' must come before '>'");
            check(!finishedConstructor, api, "Only one '>' is allowed");
            finishedConstructor = true;
            break;
          default:
            checkArgument(
                isJavaIdentifierPart(next),
                "Unable to parse '%s' because '%s' is not a valid identifier",
                api,
                next);
        }
        buffer.append(next);
      } while (true);

      String methodName = buffer.toString();
      if (isConstructor) {
        check(finishedConstructor, api, "found '<' without closing '>");

        // Must be "<init>" exactly
        checkArgument(
            methodName.equals("<init>"),
            "Unable to parse '%s' because %s is an invalid method name",
            api,
            methodName);
      } else {
        check(!methodName.isEmpty(), api, "method name cannot be empty");
        check(
            isJavaIdentifierStart(methodName.charAt(0)),
            api,
            "the method name must start with a valid character");
      }

      return methodName;
    }

    ImmutableList<String> parameters() {
      // Text until the next ',' or ')' represents the parameter type.
      // If the first token is ')', then we have an empty parameter list.
      StringBuilder buffer = new StringBuilder(api.length() - position);
      ImmutableList.Builder<String> paramBuilder = ImmutableList.builder();
      boolean emptyList = true;
      paramList:
      do {
        char next = nextLookingFor(')');
        switch (next) {
          case ')':
            if (emptyList) {
              return ImmutableList.of();
            }
            // We've hit the end of the whole list, bail out.
            paramBuilder.add(consumeParam(buffer));
            break paramList;
          case ',':
            // We've hit the middle of a parameter, consume it
            paramBuilder.add(consumeParam(buffer));
            break;

          case '[':
          case ']':
          case '.':
            // . characters are separators, [ and ] are array characters, they're checked @ the end
            buffer.append(next);
            break;

          default:
            checkArgument(
                isJavaIdentifierPart(next),
                "Unable to parse '%s' because '%s' is not a valid identifier",
                api,
                next);
            emptyList = false;
            buffer.append(next);
        }
      } while (true);
      return paramBuilder.build();
    }

    private String consumeParam(StringBuilder buffer) {
      String parameter = buffer.toString();
      buffer.setLength(0); // reset the buffer
      check(!parameter.isEmpty(), api, "parameters cannot be empty");

      check(
          isJavaIdentifierStart(parameter.charAt(0)),
          api,
          "parameters must start with a valid character");

      // Array specs must be in balanced pairs at the *end* of the parameter.
      boolean parsingArrayStart = false;
      boolean hasArraySpecifiers = false;
      for (int k = 1; k < parameter.length(); k++) {
        char c = parameter.charAt(k);
        switch (c) {
          case '[':
            check(!parsingArrayStart, api, "multiple consecutive [");
            hasArraySpecifiers = true;
            parsingArrayStart = true;
            break;
          case ']':
            check(parsingArrayStart, api, "unbalanced ] in array type");
            parsingArrayStart = false;
            break;
          default:
            check(
                !hasArraySpecifiers,
                api,
                "types with array specifiers should end in those specifiers");
        }
      }
      check(!parsingArrayStart, api, "[ without closing ] at the end of a parameter type");
      return parameter;
    }

    // skip whitespace characters and give the next non-whitespace character. If we hit the end
    // without a non-whitespace character, throw expecting the delimiter.
    private char nextLookingFor(char delimiter) {
      char next;
      do {
        position++;
        checkArgument(
            position < api.length(),
            "Could not parse '%s' as it must contain an '%s'",
            api,
            delimiter);
        next = api.charAt(position);
      } while (!assumeNoWhitespace && whitespace().matches(next));
      return next;
    }

    void ensureNoMoreCharacters() {
      if (assumeNoWhitespace) {
        return;
      }

      while (++position < api.length()) {
        check(whitespace().matches(api.charAt(position)), api, "it should end in ')'");
      }
    }

    // The @CompileTimeConstant is for performance - reason should be constant and not eagerly
    // constructed.
    private static void check(boolean condition, String api, @CompileTimeConstant String reason) {
      checkArgument(condition, "Unable to parse '%s' because %s", api, reason);
    }
  }
}
