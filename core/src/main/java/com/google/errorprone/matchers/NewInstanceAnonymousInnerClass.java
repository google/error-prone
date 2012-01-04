/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCNewClass;

/**
 * Matcher for a new instance of an anonymous inner class.
 * 
 * @author sjnickerson@google.com (Simon Nickerson)
 */
public class NewInstanceAnonymousInnerClass implements Matcher<ExpressionTree> {
  
  /**
   * Fully-qualified name of the class or interface we are subclassing for our
   * anonymous inner class, e.g. a new Comparator
   */
  private final String superClass;
  
  public NewInstanceAnonymousInnerClass(String superClass) {
    this.superClass = superClass;
  }
  
  @Override
  public boolean matches(ExpressionTree t, VisitorState state) {
    if (t instanceof JCNewClass) {
      JCNewClass invocation = (JCNewClass)t;
      Type type = invocation.getIdentifier().type;
      if (type.tsym.getQualifiedName().toString().equals(superClass)) {
        return true;
      }
    }
    return false;
  }
}
