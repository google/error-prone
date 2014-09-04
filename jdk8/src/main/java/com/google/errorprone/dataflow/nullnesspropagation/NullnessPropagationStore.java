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

package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.union;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessValue.NULLABLE;

import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A mapping from {@link Node} to {@link NullnessValue}.
 *
 * @author deminguyen@google.com (Demi Nguyen)
 */
public final class NullnessPropagationStore implements Store<NullnessPropagationStore> {
  private final Map<Node, NullnessValue> contents = new HashMap<>();

  public NullnessValue getInformation(Node n) {
    checkNotNull(n);
    return orNullable(contents.get(n));
  }

  public void setInformation(Node n, NullnessValue val) {
    contents.put(checkNotNull(n), checkNotNull(val));
  }

  @Override
  public NullnessPropagationStore copy() {
    NullnessPropagationStore copy = new NullnessPropagationStore();
    copy.contents.putAll(contents);
    return copy;
  }

  @Override
  public NullnessPropagationStore leastUpperBound(NullnessPropagationStore other) {
    NullnessPropagationStore result = new NullnessPropagationStore();
    for (Node n : union(contents.keySet(), other.contents.keySet())) {
      result.contents.put(n, getInformation(n).leastUpperBound(other.getInformation(n)));
    }
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof NullnessPropagationStore)) {
      return false;
    }
    NullnessPropagationStore other = (NullnessPropagationStore) o;
    return contents.equals(other.contents);
  }

  @Override
  public int hashCode() {
    return contents.hashCode();
  }

  @Override
  public String toString() {
    // only output local variable information
    Map<Node, NullnessValue> smallerContents = new HashMap<Node, NullnessValue>();
    for (Entry<Node, NullnessValue> e : contents.entrySet()) {
      if (e.getKey() instanceof LocalVariableNode) {
        smallerContents.put(e.getKey(), e.getValue());
      }
    }
    return smallerContents.toString();
  }

  @Override
  public boolean canAlias(FlowExpressions.Receiver a, FlowExpressions.Receiver b) {
    return true;
  }

  @Override
  public boolean hasDOToutput() {
    return false;
  }

  @Override
  public String toDOToutput() {
    throw new UnsupportedOperationException("DOT output not supported");
  }

  private static NullnessValue orNullable(NullnessValue nullnessValue) {
    return (nullnessValue != null) ? nullnessValue : NULLABLE;
  }
}
