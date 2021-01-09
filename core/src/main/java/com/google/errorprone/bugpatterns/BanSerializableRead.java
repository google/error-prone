/*
 * Copyright 2020 The Error Prone Authors.
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

import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.enclosingMethod;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.methodIsNamed;
import static com.google.errorprone.matchers.Matchers.not;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/** A {@link BugChecker} that detects use of the unsafe {@link java.io.Serializable} API. */
@BugPattern(
    name = "BanSerializableRead",
    summary = "Deserializing user input via the `Serializable` API is extremely dangerous",
    severity = SeverityLevel.ERROR)
public final class BanSerializableRead extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> EXEMPT =
      allOf(
          enclosingClass(isSubtypeOf("java.io.Serializable")),
          enclosingMethod(methodIsNamed("readObject")));

  /** Checks for unsafe deserialization calls on an ObjectInputStream in an ExpressionTree. */
  private static final Matcher<ExpressionTree> OBJECT_INPUT_STREAM_DESERIALIZE_MATCHER =
      allOf(
          anyOf(
              // this matches calls to the ObjectInputStream to read some objects
              instanceMethod()
                  .onDescendantOf("java.io.ObjectInputStream")
                  .namedAnyOf(
                      // Prevent reading objects unsafely into memory
                      "readObject",

                      // This is the same, the default value
                      "defaultReadObject",

                      // This is for trusted subclasses
                      "readObjectOverride",

                      // Ultimately, a lot of the safety worries come
                      // from being able to construct arbitrary classes via
                      // reading in class descriptors. I don't think anyone
                      // will bother calling this directly, but I don't see
                      // any reason not to block it.
                      "readClassDescriptor",

                      // These are basically the same as above
                      "resolveClass",
                      "resolveObject"),

              // because in the next part we exempt readObject functions, here we
              // check for calls to those functions
              instanceMethod().onDescendantOf("java.io.Serializable").named("readObject")),

          // Java lets you override or add to the default deserialization behaviour
          // by defining a 'readObject' on your class. In this case, it's super common
          // to see calls to deserialize methods (after all, it's what *would* happen
          // if it *were* deserialized). We specifically want to allow such members to
          // be defined, but never called
          not(EXEMPT));

  /** Checks for unsafe uses of the Java deserialization API. */
  private static final Matcher<ExpressionTree> MATCHER = OBJECT_INPUT_STREAM_DESERIALIZE_MATCHER;

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (state.errorProneOptions().isTestOnlyTarget() || !MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    Description.Builder description = buildDescription(tree);

    return description.build();
  }
}
