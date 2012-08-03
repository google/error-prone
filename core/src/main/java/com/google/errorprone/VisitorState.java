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

import com.sun.tools.javac.parser.Scanner;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.parser.Parser;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
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

import javax.tools.JavaFileObject;

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
   * Given the string representation of a type, return the matching Type.
   *
   * TODO(eaftan): support wildcard generics, inner classes,
   * check that the returned types behave as expected
   *
   * @param typeStr The canonical string representation of a type (e.g., "java.lang.Object")
   */
  public Type getType(String typeStr) {
    // way 1

    // Code borrowed from JavacTaskImpl.parseType.
    JavaCompiler compiler = JavaCompiler.instance(context);
    JavaFileObject prev = compiler.log.useSource(null);
    Scanner.Factory scannerFactory = Scanner.Factory.instance(context);
    Parser.Factory parserFactory = Parser.Factory.instance(context);
    Attr attr = Attr.instance(context);
    try {
      Scanner scanner = scannerFactory.newScanner((typeStr+"\u0000").toCharArray(),
          typeStr.length());
      Parser parser = parserFactory.newParser(scanner, false, false);
      JCTree tree = parser.type();



      /* This is a hack.  attr.attribType needs an environment to compile against, and I chose
       * the class that is currently being compiled since (1) it is sure to exist, and (2) it is
       * in the Enter.typeDecls data structure that attr.attribType will use to find the
       * environment.  I could avoid doing this if I duplicate code from Attr, but I think this
       * is a better option.
       */
      TreePath currPath = path;
      while (currPath.getLeaf().getKind() != Kind.CLASS) {
        currPath = currPath.getParentPath();
      }
      JCClassDecl classTree = (JCClassDecl) currPath.getLeaf();
      return attr.attribType(tree, classTree.sym);
    } finally {
      compiler.log.useSource(prev);
    }


    // way 2
//    if (isPrimitiveType(typeStr)) {
//      return getPrimitiveType(typeStr);
//    } else if (isArrayType(typeStr)) {
//      ClassSymbol arraySymbol = getSymtab().arrayClass;
//      Type elemType = getType(typeStr.substring(0, typeStr.length() - 2));
//      if (elemType != null) {
//        return new ArrayType(elemType, arraySymbol);
//      }
//      return null;
//    } else if (isGenericType(typeStr)) {
//      // extract base type
//      Name baseTypeName = getName(typeStr.substring(0, typeStr.indexOf("<")));
//      TypeSymbol baseTypeSym =  getSymtab().classes.get(baseTypeName);
//      if (baseTypeSym != null) {
//        // extract generic types
//        String[] typeArgs = typeStr.substring(typeStr.indexOf("<") + 1, typeStr.length() - 1)
//            .split(",");
//        List<Type> typeArgsList = null;
//        for (int i = 0; i < typeArgs.length; i++) {
//          // TODO(eaftan): anything with getType should be prepared to handle null return value
//          Type t = getType(typeArgs[i].trim());
//          if (typeArgsList == null) {
//            typeArgsList = List.<Type>of(t);
//          } else {
//            typeArgsList.append(t);
//          }
//          return new ClassType(Type.noType, typeArgsList, baseTypeSym);
//        }
//      }
//      return null;
//    } else {    // class type
//      Name typeName = getName(typeStr);
//      ClassSymbol typeSymbol = getSymtab().classes.get(typeName);
//      if (typeSymbol != null) {
//        return typeSymbol.asType();
//      }
//      return null;
//    }
  }

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

  private static boolean isArrayType(String typeStr) {
    return typeStr.endsWith("[]");
  }

  private static boolean isGenericType(String typeStr) {
    return typeStr.contains("<");
  }

  private static boolean isPrimitiveType(String typeStr) {
    return typeStr.equals("byte") || typeStr.equals("short") || typeStr.equals("int") ||
        typeStr.equals("long") || typeStr.equals("float") || typeStr.equals("double") ||
        typeStr.equals("boolean") || typeStr.equals("char");
  }

  private interface NameLookupStrategy {
    Name fromString(Context context, String nameStr);
  }
}
