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

import com.google.errorprone.matchers.Description;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

import java.lang.reflect.Method;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class VisitorState {

  private final DescriptionListener descriptionListener;
  private final MatchListener matchListener;
  public final Context context;
  private final TreePath path;

  private VisitorState(Context context, TreePath path,
      DescriptionListener descriptionListener, MatchListener matchListener) {
    this.context = context;
    this.path = path;
    this.descriptionListener = descriptionListener;
    this.matchListener = matchListener;
  }

  public VisitorState(Context context, DescriptionListener listener) {
    this(context, null, listener, new MatchListener() {
      @Override
      public void onMatch(Tree tree) {
      }
    });
  }

  public VisitorState(Context context, MatchListener listener) {
    this(context, null, new DescriptionListener() {
      @Override
      public void onDescribed(Description description) {}
    }, listener);
  }

  public VisitorState withPath(TreePath path) {
    return new VisitorState(context, path, descriptionListener, matchListener);
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

  public JavacElements getElements() {
    return JavacElements.instance(context);
  }

  public Name.Table getNames() {
    return Name.Table.instance(context);
  }

  public DescriptionListener getDescriptionListener() {
    return descriptionListener;
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

  /**
   * Given the string representation of a simple (non-array, non-generic) type, return the
   * matching Type.
   *
   * @param typeStr The canonical string representation of a simple type (e.g., "java.lang.Object")
   * @return The Type that corresponds to the string, or null if it cannot be found
   */
  public Type getTypeFromString(String typeStr) {
    validateTypeStr(typeStr);
    if (isPrimitiveType(typeStr)) {
      return getPrimitiveType(typeStr);
    }
    Name typeName = getName(typeStr);
    ClassSymbol typeSymbol = getSymtab().classes.get(typeName);
    if (typeSymbol != null) {
      return typeSymbol.asType();
    }
    return null;
  }

  /**
   * Build an instance of a Type.
   */
  public Type getType(Type baseType, boolean isArray, List<Type> typeParams) {
    boolean isGeneric = typeParams != null && !typeParams.equals(List.nil());
    if (!isArray && !isGeneric) {
      // Simple type.
      return baseType;
    } else if (isArray && !isGeneric) {
      // Array type, not generic.
      ClassSymbol arraySymbol = getSymtab().arrayClass;
      return new ArrayType(baseType, arraySymbol);
    } else if (!isArray && isGeneric) {
      return new ClassType(Type.noType, typeParams, baseType.tsym);
    } else {
      throw new IllegalArgumentException("Unsupported arguments to getType");
    }
  }

  /**
   * Build an instance of a Type.
   */
  public Type getType(Type baseType, boolean isArray, java.util.List<Type> typeParams) {
    boolean isGeneric = typeParams != null && !typeParams.equals(List.nil());
    if (!isArray && !isGeneric) {
      // Simple type.
      return baseType;
    } else if (isArray && !isGeneric) {
      // Array type, not generic.
      ClassSymbol arraySymbol = getSymtab().arrayClass;
      return new ArrayType(baseType, arraySymbol);
    } else if (!isArray && isGeneric) {
      // Generic type, not array.
      List<Type> typeParamsCopy = List.from(typeParams.toArray(new Type[typeParams.size()]));
      return new ClassType(Type.noType, typeParamsCopy, baseType.tsym);
    } else {
      throw new IllegalArgumentException("Unsupported arguments to getType");
    }
  }

  /**
   * Validates a type string, ensuring it is not generic and not an array type.
   */
  private static void validateTypeStr(String typeStr) {
    if (typeStr.contains("[") || typeStr.contains("]")) {
      throw new IllegalArgumentException("Cannot convert array types, please build them using "
          + "getType()");
    }
    if (typeStr.contains("<") || typeStr.contains(">")) {
      throw new IllegalArgumentException("Cannot covnert generic types, please build them using "
          + "getType()");
    }
  }

  /**
   * Given a string that represents a primitive type (e.g., "int"), return the corresponding Type.
   */
  private Type getPrimitiveType(String typeStr) {
    if (typeStr.equals("byte")) {
      return getSymtab().byteType;
    } else if (typeStr.equals("short")) {
      return getSymtab().shortType;
    } else if (typeStr.equals("int")) {
      return getSymtab().intType;
    } else if (typeStr.equals("long")) {
      return getSymtab().longType;
    } else if (typeStr.equals("float")) {
      return getSymtab().floatType;
    } else if (typeStr.equals("double")) {
      return getSymtab().doubleType;
    } else if (typeStr.equals("boolean")) {
      return getSymtab().booleanType;
    } else if (typeStr.equals("char")) {
      return getSymtab().charType;
    } else {
      throw new IllegalStateException("Type string " + typeStr + " expected to be primitive");
    }
  }

  private static boolean isPrimitiveType(String typeStr) {
    return typeStr.equals("byte") || typeStr.equals("short") || typeStr.equals("int") ||
        typeStr.equals("long") || typeStr.equals("float") || typeStr.equals("double") ||
        typeStr.equals("boolean") || typeStr.equals("char");
  }

  private interface NameLookupStrategy {
    Name fromString(Context context, String nameStr);
  }

  public class TypeBuilder {

  }
}
