/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import com.google.errorprone.VisitorState;
import com.sun.source.tree.AnnotationTree;

/**
 * Matches an annotation that does not have a particular argument, possibly because the default
 * value is being used.
 *
 * @author mwacker@google.com (Mike Wacker)
 */
public class AnnotationDoesNotHaveArgument implements Matcher<AnnotationTree> {

  private final String name;

  /**
   * Creates a new matcher.
   *
   * @param name the name of the argument to search for
   */
  public AnnotationDoesNotHaveArgument(String name) {
    this.name = name;
  }

  @Override
  public boolean matches(AnnotationTree annotationTree, VisitorState state) {
    return AnnotationMatcherUtils.getArgument(annotationTree, name) == null;
  }
}
