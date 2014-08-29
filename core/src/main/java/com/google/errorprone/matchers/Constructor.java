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

import com.sun.source.tree.NewClassTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCNewClass;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches a call to a constructor.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class Constructor implements Matcher<NewClassTree>{

  private final String className;
  private final List<String> parameterTypes;

  public Constructor(String className, List<String> parameterTypes) {
    this.className = className;
    this.parameterTypes = parameterTypes;
  }

  @Override
  public boolean matches(NewClassTree newClassTree, VisitorState state) {
    /* TODO(user): Don't catch NullPointerException.  Need to do this right now
     * for internal use, but remember to remove later. */
    try {
      JCNewClass newClass = (JCNewClass) newClassTree;
      String thisClassName = newClass.constructor.getEnclosingElement().toString();
      com.sun.tools.javac.util.List<Type> thisParameterTypes =
          newClass.constructor.type.getParameterTypes();
      List<String> thisParameterTypesAsStrings = new ArrayList<>(thisParameterTypes.length());
      for (Type t : thisParameterTypes) {
        thisParameterTypesAsStrings.add(t.toString());
      }

      return thisClassName.equals(className) && thisParameterTypesAsStrings.equals(parameterTypes);
    } catch (NullPointerException e) {
      return false;
    }
  }
}
