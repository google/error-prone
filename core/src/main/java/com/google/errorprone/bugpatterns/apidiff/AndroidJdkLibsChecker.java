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
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.common.io.Resources;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.protobuf.ExtensionRegistry;
import com.sun.source.tree.ExpressionTree;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Checks for uses of classes, fields, or methods that are not compatible with legacy Android
 * devices. As of Android N, that includes all of JDK8 (which is only supported on Nougat) except
 * type and repeated annotations, which are compiled in a backwards compatible way.
 */
@BugPattern(
    name = "AndroidJdkLibsChecker",
    altNames = "AndroidApiChecker",
    summary = "Use of class, field, or method that is not compatible with legacy Android devices",
    severity = ERROR)
// TODO(b/32513850): Allow Android N+ APIs, e.g., by computing API diff using android.jar
public class AndroidJdkLibsChecker extends ApiDiffChecker {

  private static ApiDiff loadApiDiff(boolean allowJava8) {
    try {
      byte[] diffData =
          Resources.toByteArray(
              Resources.getResource(
                  AndroidJdkLibsChecker.class,
                  allowJava8 ? "android_java8.binarypb" : "android.binarypb"));
      ApiDiffProto.Diff diff =
          ApiDiffProto.Diff.newBuilder()
              .mergeFrom(diffData, ExtensionRegistry.getEmptyRegistry())
              .build();
      return ApiDiff.fromProto(diff);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private final boolean allowJava8;

  public AndroidJdkLibsChecker(ErrorProneFlags flags) {
    this(flags.getBoolean("Android:Java8Libs").orElse(false));
  }

  public AndroidJdkLibsChecker() {
    this(false);
  }

  private AndroidJdkLibsChecker(boolean allowJava8) {
    super(loadApiDiff(allowJava8));
    this.allowJava8 = allowJava8;
  }

  private static final Matcher<ExpressionTree> FOREACH_ON_COLLECTION =
      instanceMethod()
          .onDescendantOf("java.util.Collection")
          .named("forEach")
          .withParameters("java.util.function.Consumer");

  @Override
  protected Description check(ExpressionTree tree, VisitorState state) {
    Description description = super.check(tree, state);
    if (description.equals(NO_MATCH)) {
      return NO_MATCH;
    }
    if (allowJava8 && FOREACH_ON_COLLECTION.matches(tree, state)) {
      return NO_MATCH;
    }
    return description;
  }
}
