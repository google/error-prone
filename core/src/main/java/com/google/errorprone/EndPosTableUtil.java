/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.AbstractLog;
import com.sun.tools.javac.util.DiagnosticSource;
import com.sun.tools.javac.util.IntHashTable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.tools.JavaFileObject;

/**
 * Work around protected access restrictions on the EndPosTable implementations.
 */
class EndPosTableUtil extends JavacParser {

 protected EndPosTableUtil() {
   // Don't instantiate this.
   super(null, null, false, false, false);
   throw new IllegalStateException();
 }

 static boolean isEmpty(EndPosTable table) {
   return table instanceof EmptyEndPosTable;
 }

 /**
  * Use reflection to bypass access restrictions on SimpleEndPosTable.endPosMap.
  * This is pretty terrible, but we need the map's entrySet to construct WrappedTreeMaps.
  * TODO(user): investigate alternatives. Could we get we avoid the need to know the map's
  * contents by building the WrappedTreeMap lazily?
  */
 private static final Field END_POS_MAP_FIELD;
 static {
   try {
     END_POS_MAP_FIELD = JavacParser.SimpleEndPosTable.class.getDeclaredField("endPosMap");
     END_POS_MAP_FIELD.setAccessible(true);
   } catch (Exception e) {
     throw new LinkageError(e.getMessage());
   }
 }
 @SuppressWarnings("unchecked")  // Unsafe reflection.
 public static IntHashTable getMap(EndPosTable table) {
   try {
     return (IntHashTable) END_POS_MAP_FIELD.get(table);
   } catch (Exception e) {
     throw new LinkageError(e.getMessage());
   }
 }

 private static final Field getFieldOrDie(Class<?> clazz, String fieldName) {
   try {
     Field field = clazz.getDeclaredField(fieldName);
     field.setAccessible(true);
     return field;
   } catch (ReflectiveOperationException e) {
     throw new LinkageError(e.getMessage());
   }
 }

 private static final Field OBJS_FIELD = getFieldOrDie(IntHashTable.class, "objs");
 private static final Field INTS_FIELD = getFieldOrDie(IntHashTable.class, "ints");
 private static final Object DELETED;
 static {
   try {
     DELETED = getFieldOrDie(IntHashTable.class, "DELETED").get(IntHashTable.class);
   } catch (ReflectiveOperationException e) {
     throw new LinkageError(e.getMessage());
   }
 }
 @SuppressWarnings("unchecked")  // Unsafe reflection.
 static Set<Entry<JCTree, Integer>> getEntries(EndPosTable table) {
   if (isEmpty(table)) {
     return Collections.emptySet();
   }
   IntHashTable rawMap = getMap(table);
   Set<Map.Entry<JCTree, Integer>> entries = new HashSet<>();
   Object[] objs;
   int[] ints;
   try {
     objs = (Object[]) OBJS_FIELD.get(rawMap);
     ints = (int[]) INTS_FIELD.get(rawMap);
   } catch (ReflectiveOperationException e) {
     throw new LinkageError(e.getMessage());
   }
   for (int i = 0; i < objs.length; i++) {
     if (objs[i] != null && objs[i] != DELETED) {
       entries.add(new SimpleImmutableEntry<JCTree, Integer>((JCTree) objs[i], ints[i]));
     }
   }
   return entries;
 }

 private static final Method ABSTRACT_LOG__GET_SOURCE;
 private static final Field DIAGNOSTIC_SOURCE__END_POS_TABLE;
 static {
   try {
     ABSTRACT_LOG__GET_SOURCE =
         AbstractLog.class.getDeclaredMethod("getSource", JavaFileObject.class);
     ABSTRACT_LOG__GET_SOURCE.setAccessible(true);

     DIAGNOSTIC_SOURCE__END_POS_TABLE =
         DiagnosticSource.class.getDeclaredField("endPosTable");
     DIAGNOSTIC_SOURCE__END_POS_TABLE.setAccessible(true);
   } catch (Exception e) {
     throw new LinkageError(e.getMessage());
   }
 }

 static void resetEndPosMap(JavaCompiler compiler, JavaFileObject sourceFile) {
   try {
     DiagnosticSource diagnosticSource = (DiagnosticSource)
         ABSTRACT_LOG__GET_SOURCE.invoke(compiler.log, sourceFile);
     DIAGNOSTIC_SOURCE__END_POS_TABLE.set(diagnosticSource, null);
   } catch (Exception e) {
     throw new LinkageError(e.getMessage());
   }
 }
}
