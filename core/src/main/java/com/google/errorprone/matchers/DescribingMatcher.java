/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.Tree;

/**
 * In addition to matching an AST node, also gives a message and/or suggested fix.
 * @author alexeagle@google.com (Alex Eagle)
 */
public abstract class DescribingMatcher<T extends Tree> implements Matcher<T> {
    
  protected final String name;
  protected final String[] altNames;
  protected final String diagnosticMessage;
  
  public DescribingMatcher() {
    BugPattern annotation = this.getClass().getAnnotation(BugPattern.class);
    if (annotation == null) {
      throw new IllegalStateException("Class " + this.getClass().getCanonicalName()
          + " not annotated with @BugPattern");
    }
    name = annotation.name();
    altNames = annotation.altNames();
    diagnosticMessage = "[" + annotation.name() + "] " + annotation.summary()
        + "\n  (see http://code.google.com/p/error-prone/wiki/" + annotation.name() + ")";
  }
  
  public String getName() {
    return name;
  }
  
  public String[] getAltNames() {
    return altNames;
  }

  /**
   * Additional description of a matched AST node, useful in reporting the error or performing an
   * automated refactoring.
   * @param t an AST node which matched this matcher
   * @param state the shared state
   * @return the description
   */
  public abstract Description describe(T t, VisitorState state);

}
