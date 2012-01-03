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

import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;

/**
 * @author sjnickerson@google.com (Simon Nickerson)
 *
 */
public class ExpressionMethodSelect implements Matcher<ExpressionTree> {

  private final Matcher<ExpressionTree> methodSelectMatcher;
  
  public ExpressionMethodSelect(Matcher<ExpressionTree> methodSelectMatcher) {
    this.methodSelectMatcher = methodSelectMatcher;
  }
  
  @Override
  public boolean matches(ExpressionTree t, VisitorState state) {
    if (t.getKind() != Kind.METHOD_INVOCATION) {
      return false;  
    }
    
    MethodInvocationTree methodInvocation = (MethodInvocationTree) t;
    return methodSelectMatcher.matches(methodInvocation.getMethodSelect(), state);
  }

}
