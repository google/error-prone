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

package com.google.errorprone;

import com.google.errorprone.refactors.RefactoringMatcher.Refactor;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;

import java.lang.reflect.Method;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class VisitorState {

  private final RefactorListener refactorListener;
  private final MatchListener matchListener;
  public final Context context;
  private final TreePath path;

  private VisitorState(Context context, TreePath path,
      RefactorListener refactorListener, MatchListener matchListener) {
    this.context = context;
    this.path = path;
    this.refactorListener = refactorListener;
    this.matchListener = matchListener;
  }

  public VisitorState(Context context, RefactorListener listener) {
    this(context, null, listener, new MatchListener() {
      @Override
      public void onMatch(Tree tree) {
      }
    });
  }

  public VisitorState(Context context, MatchListener listener) {
    this(context, null, new RefactorListener() {
      @Override
      public void onRefactor(Refactor refactor) {}
    }, listener);
  }

  public VisitorState withPath(TreePath path) {
    return new VisitorState(context, path, refactorListener, matchListener);
  }

  public TreePath getPath() {
    return path;
  }

  public TreeMaker getTreeMaker() {
    return TreeMaker.instance(context);
  }

  public Types getTypes() {
    return Types.instance(context);
  }

  public Symtab getSymtab() {
    return Symtab.instance(context);
  }

  public RefactorListener getRefactorListener() {
    return refactorListener;
  }

  public MatchListener getMatchListener() {
    return matchListener;
  }

  // Cache the name lookup strategy since it requires expensive reflection, and is used a lot
  private static final NameLookupStrategy NAME_LOOKUP_STRATEGY = createNameLookup();
  private static NameLookupStrategy createNameLookup() {
    ClassLoader classLoader = VisitorState.class.getClassLoader();
    // OpenJDK 7
    try {
      Class<?> namesClass = classLoader.loadClass("com.sun.tools.javac.util.Names");
      final Method instanceMethod = namesClass.getDeclaredMethod("instance", Context.class);
      final Method fromStringMethod = namesClass.getDeclaredMethod("fromString", String.class);
      return new NameLookupStrategy() {
        @Override public Name fromString(Context context, String nameStr) {
          try {
            Object names = instanceMethod.invoke(null, context);
            return (Name) fromStringMethod.invoke(names, nameStr);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
      };
    } catch (ClassNotFoundException e) {
      // OpenJDK 6
      try {
        Class<?> nameTableClass = classLoader.loadClass("com.sun.tools.javac.util.Name$Table");
        final Method instanceMethod = nameTableClass.getMethod("instance", Context.class);
        final Method fromStringMethod = Name.class.getMethod("fromString", nameTableClass, String.class);
        return new NameLookupStrategy() {
          @Override public Name fromString(Context context, String nameStr) {
            try {
              Object nameTable = instanceMethod.invoke(null, context);
              return (Name) fromStringMethod.invoke(null, nameTable, nameStr);
            } catch (Exception e1) {
              throw new RuntimeException(e1);
            }
          }
        };
      } catch (Exception e1) {
        throw new RuntimeException("Unexpected error loading com.sun.tools.javac.util.Names", e1);
      }
    } catch (Exception e) {
      throw new RuntimeException("Unexpected error loading com.sun.tools.javac.util.Names", e);
    }
  }

  public Name getName(String nameStr) {
    return NAME_LOOKUP_STRATEGY.fromString(context, nameStr);
  }

  private interface NameLookupStrategy {
    Name fromString(Context context, String nameStr);
  }
}
