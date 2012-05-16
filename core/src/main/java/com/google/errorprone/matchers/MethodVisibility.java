// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.errorprone.matchers;

import com.google.errorprone.VisitorState;

import com.sun.source.tree.MethodTree;

import java.util.Set;

import javax.lang.model.element.Modifier;

/**
 * A matcher for method visibility (public, private, protected, or default).
 * 
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class MethodVisibility implements Matcher<MethodTree> {

  private final Visibility visibility;
  
  public MethodVisibility(Visibility visibility) {
    this.visibility = visibility;
  }

  @Override
  public boolean matches(MethodTree t, VisitorState state) {
    Set<Modifier> modifiers = t.getModifiers().getFlags();
    if (visibility == Visibility.DEFAULT) {
      if (modifiers.contains(Visibility.PUBLIC.toModifier()) ||
          modifiers.contains(Visibility.PROTECTED.toModifier()) ||
          modifiers.contains(Visibility.PRIVATE.toModifier())) {
        return false;
      } else {
        return true;
      }
    } else {
      if (modifiers.contains(visibility.toModifier())) {
        return true;
      } else {
        return false;
      }
    }
  }
  
  public static enum Visibility {
    PUBLIC    (Modifier.PUBLIC),
    PROTECTED (Modifier.PROTECTED),
    DEFAULT   (null),
    PRIVATE   (Modifier.PRIVATE);
    
    private Modifier correspondongModifier;
    Visibility(Modifier correspondingModifier) {
      this.correspondongModifier = correspondingModifier;
    }
    
    public Modifier toModifier() {
      return correspondongModifier;
    }
  }
}
