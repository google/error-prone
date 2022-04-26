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
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.matchers.Matchers.anyMethod;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.constructor;
import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.Character.isJavaIdentifierStart;

import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import java.util.List;

/**
 * Represents a Java method or constructor. Provides a method to parse an API from a string format,
 * and another method to create an ErrorProne {@link Matcher} for the API.
 */
// TODO(kak): do we want to be able to represent classes in addition to methods/constructors?
// TODO(kak): if not, then consider renaming to `MethodSignature` or something
@AutoValue
public abstract class Api {

  // TODO(b/223668437): use this (or something other than the Matcher<> API)
  static Matcher<ExpressionTree> createMatcherFromApis(List<String> apis) {
    return anyOf(apis.stream().map(Api::parse).map(Api::matcher).collect(toImmutableList()));
  }

  static ImmutableSet<Api> createSetFromApis(List<String> apis) {
    return apis.stream().map(Api::parse).collect(toImmutableSet());
  }

  /** Returns the fully qualified type that contains the given method/constructor. */
  abstract String className();

  /**
   * Returns the simple name of the method. If the API is a constructor (i.e., {@code
   * isConstructor() == true}), then {@code "<init>"} is returned.
   */
  abstract String methodName();

  /** Returns the list of fully qualified parameter types for the given method/constructor. */
  abstract ImmutableList<String> parameterTypes();

  @Override
  public final String toString() {
    return String.format(
        "%s#%s(%s)", className(), methodName(), Joiner.on(',').join(parameterTypes()));
  }

  /** Returns whether this API represents a constructor or not. */
  boolean isConstructor() {
    return methodName().equals("<init>");
  }

  private Matcher<ExpressionTree> matcher() {
    return isConstructor()
        ? constructor().forClass(className()).withParameters(parameterTypes())
        : anyMethod()
            .onClass(className())
            .named(methodName())
            // TODO(b/219754967): what about arrays
            .withParameters(parameterTypes());
  }

  private static final Splitter PARAM_SPLITTER = Splitter.on(',');

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
  static Api parse(String apiWithWhitespace) {
    // TODO(kak): consider removing whitespace from the String as we step through the String
    String api = whitespace().removeFrom(apiWithWhitespace);

    boolean isConstructor = false;
    int hashIndex = -1;
    int openParenIndex = -1;
    int closeParenIndex = -1;
    int lessThanIndex = -1;
    int greaterThanIndex = -1;
    for (int i = 0; i < api.length(); i++) {
      char ch = api.charAt(i);
      switch (ch) {
        case '#':
          check(hashIndex == -1, api, "it contains more than one '#'");
          hashIndex = i;
          break;
        case '(':
          check(openParenIndex == -1, api, "it contains more than one '('");
          openParenIndex = i;
          break;
        case ')':
          check(closeParenIndex == -1, api, "it contains more than one ')'");
          closeParenIndex = i;
          break;
        case '<':
          check(lessThanIndex == -1, api, "it contains more than one '<'");
          lessThanIndex = i;
          isConstructor = true;
          break;
        case '>':
          check(greaterThanIndex == -1, api, "it contains more than one '>'");
          greaterThanIndex = i;
          isConstructor = true;
          break;
        case ',': // for separating parameters
        case '.': // for package names and fully qualified parameter names
          break;
        default:
          check(isJavaIdentifierPart(ch), api, "'" + ch + "' is not a valid identifier");
      }
    }

    // make sure we've seen a hash, open paren, and close paren
    check(hashIndex != -1, api, "it must contain a '#'");
    check(openParenIndex != -1, api, "it must contain a '('");
    check(closeParenIndex == api.length() - 1, api, "it must end with ')'");

    // make sure they came in the correct order: <sometext>#<sometext>(<sometext>)
    check(hashIndex < openParenIndex, api, "'#' must come before '('");
    check(openParenIndex < closeParenIndex, api, "'(' must come before ')'");

    if (isConstructor) {
      // make sure that if we've seen a < or >, we also have seen the matching one
      check(lessThanIndex != -1, api, "must contain both '<' and '>'");
      check(greaterThanIndex != -1, api, "must contain both '<' and '>'");

      // make sure the < comes directly after the #
      check(lessThanIndex == hashIndex + 1, api, "'<' must come directly after '#'");

      // make sure that the < comes before the >
      check(lessThanIndex < greaterThanIndex, api, "'<' must come before '>'");

      // make sure that the > comes directly before the (
      check(greaterThanIndex == openParenIndex - 1, api, "'>' must come directly before '('");

      // make sure the only thing between the < and > is exactly "init"
      String constructorName = api.substring(lessThanIndex + 1, greaterThanIndex);
      check(constructorName.equals("init"), api, "invalid method name: " + constructorName);
    }

    String className = api.substring(0, hashIndex);
    String methodName = api.substring(hashIndex + 1, openParenIndex);
    String parameters = api.substring(openParenIndex + 1, closeParenIndex);

    ImmutableList<String> paramList =
        parameters.isEmpty()
            ? ImmutableList.of()
            : PARAM_SPLITTER.splitToStream(parameters).collect(toImmutableList());

    // make sure the class name, method name, and parameter names are not empty
    check(!className.isEmpty(), api, "the class name cannot be empty");
    check(!methodName.isEmpty(), api, "the method name cannot be empty");
    for (String parameter : paramList) {
      check(!parameter.isEmpty(), api, "parameters cannot be empty");

      check(
          isJavaIdentifierStart(parameter.charAt(0)),
          api,
          "parameters must start with a valid character");
    }
    // make sure the class name starts with a valid Java identifier character
    check(
        isJavaIdentifierStart(className.charAt(0)),
        api,
        "the class name must start with a valid character");

    if (!isConstructor) {
      // make sure the method name starts with a valid Java identifier character
      check(
          isJavaIdentifierStart(methodName.charAt(0)),
          api,
          "the method name must start with a valid character");
    }

    return new AutoValue_Api(className, methodName, paramList);
  }

  private static void check(boolean condition, String api, String reason) {
    checkArgument(condition, "Unable to parse '%s' because %s", api, reason);
  }
}
