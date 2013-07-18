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

import com.sun.source.tree.ClassTree;

import java.util.Set;

import javax.lang.model.element.Modifier;

/**
 * A matcher for class modifiers (public, private, protected, abstract, final, etc.).
 * 
 * TODO: This matcher is almost identical to MethodModifier. Unfortunately there is 
 * no 'HasModifier' interface that unites the two classes at the type level cleanly.
 *
 * @author alexloh@google.com (Alex Loh)
 */
public class ClassModifier implements Matcher<ClassTree> {

  private final Modifier modifier;

  public ClassModifier(Modifier modifier) {
    this.modifier = modifier;
  }

  @Override
  public boolean matches(ClassTree t, VisitorState state) {
    Set<Modifier> modifiers = t.getModifiers().getFlags();
    return modifiers.contains(modifier);
  }
}
