/*
 * Copyright 2015 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.scanner;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.CodeTransformer;
import com.google.errorprone.DescriptionListener;
import com.google.errorprone.ErrorProneOptions;
import com.google.errorprone.VisitorState;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.util.Context;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Adapter from an {@link ErrorProneScanner} to a {@link CodeTransformer}.
 */
@AutoValue
public abstract class ErrorProneScannerTransformer implements CodeTransformer {
  static final Context.Key<Map<String, SeverityLevel>> SEVERITY_MAP_KEY = new Context.Key<>();

  public static ErrorProneScannerTransformer create(ErrorProneScanner scanner) {
    return new AutoValue_ErrorProneScannerTransformer(scanner);
  }

  abstract ErrorProneScanner scanner();

  @Override
  public void apply(TreePath tree, Context context, DescriptionListener listener) {
    scanner().scan(tree, createVisitorState(context, listener).withPath(tree));
  }

  @Override
  public ImmutableClassToInstanceMap<Annotation> annotations() {
    return ImmutableClassToInstanceMap.<Annotation>builder().build();
  }

  /**
   * Create a VisitorState object from a compilation unit.
   */
  private VisitorState createVisitorState(Context context, DescriptionListener listener) {
    return new VisitorState(
        context, listener, context.get(SEVERITY_MAP_KEY), context.get(ErrorProneOptions.class));
  }
}
