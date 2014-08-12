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

import com.google.errorprone.dataflow.nullnesspropagation.NullnessValue.Type;

import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A mapping of Nodes to Values
 *
 * @author deminguyen@google.com (Demi Nguyen)
 */
public class NullnessPropagationStore implements Store<NullnessPropagationStore> {

  private final Map<Node, NullnessValue> contents;

  public NullnessPropagationStore() {
    contents = new HashMap<Node, NullnessValue>();
  }

  protected NullnessPropagationStore(Map<Node, NullnessValue> contents) {
    this.contents = contents;
  }

  public NullnessValue getInformation(Node n) {
    if (contents.containsKey(n)) {
      return contents.get(n);
    }
    return new NullnessValue(Type.NULLABLE);
  }

  public void mergeInformation(Node node, NullnessValue val) {
    NullnessValue newValue;
    if (contents.containsKey(node)) {
      newValue = val.leastUpperBound(contents.get(node));
    } else {
      newValue = val;
    }
    contents.put(node, newValue);
  }

  public void setInformation(Node n, NullnessValue val) {
    contents.put(n, val);
  }

  @Override
  public NullnessPropagationStore copy() {
    return new NullnessPropagationStore(new HashMap<Node, NullnessValue>(contents));
  }

  @Override
  public NullnessPropagationStore leastUpperBound(NullnessPropagationStore other) {
    Map<Node, NullnessValue> newContents = new HashMap<Node, NullnessValue>();
    for (Entry<Node, NullnessValue> e : other.contents.entrySet()) {
      Node n = e.getKey();
      NullnessValue otherVal = e.getValue();
      if (contents.containsKey(n)) {
        newContents.put(n, otherVal.leastUpperBound(contents.get(n)));
      } else {
        newContents.put(n, otherVal);
      }
    }
    for (Entry<Node, NullnessValue> e : contents.entrySet()) {
      Node n = e.getKey();
      NullnessValue thisVal = e.getValue();
      if (!other.contents.containsKey(n)) {
        newContents.put(n, thisVal);
      }
    }
    return new NullnessPropagationStore(newContents);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (!(o instanceof NullnessPropagationStore)) {
      return false;
    }
    NullnessPropagationStore other = (NullnessPropagationStore) o;
    return this.contents.equals(other.contents);
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

}

