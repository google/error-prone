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

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

import java.util.Map;
import java.util.Set;

/** A JDK6 Compatible {@link ErrorProneEndPosMap} */
public class EndPosMap6 implements ErrorProneEndPosMap {

  private Map<JCTree, Integer> map;

  EndPosMap6(Map<JCTree, Integer> map) {
    this.map = map;
  }

  /**
   * The JDK6 implementation of endPosMap returns null if there's no mapping for the given key.
   */
  @Override
  public Integer getEndPosition(DiagnosticPosition pos) {
    return pos.getEndPosition(map);
  }

  @Override
  public Set<Map.Entry<JCTree, Integer>> entrySet() {
    return map.entrySet();
  }

  public static int getEndPos(DiagnosticPosition pos, Map<JCTree, Integer> map) {
    return pos.getEndPosition(map);
  }
}
