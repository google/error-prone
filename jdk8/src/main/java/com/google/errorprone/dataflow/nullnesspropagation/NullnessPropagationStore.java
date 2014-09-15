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
import static com.google.common.collect.Sets.intersection;

import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;

import java.util.HashMap;
import java.util.Map;

import javax.lang.model.element.Element;

/**
 * Tracks the {@link NullnessValue} of each local variable. Note that, while the interface is
 * written in terms of <b>nodes</b>, the stored data is indexed by variable <b>declaration</b>, so
 * values persist across nodes.
 *
 * @author deminguyen@google.com (Demi Nguyen)
 */
public final class NullnessPropagationStore implements Store<NullnessPropagationStore> {
  /*
   * TODO(cpovirk): Return to LocalVariableNode keys if LocalVariableNode.equals is fixed to use the
   * variable's declaring element instead of its name.
   */
  private final Map<Element, NullnessValue> contents = new HashMap<>();

  public NullnessValue getInformation(LocalVariableNode node) {
    return contents.get(node.getElement());
  }

  public void setInformation(LocalVariableNode node, NullnessValue value) {
    contents.put(node.getElement(), checkNotNull(value));
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
    for (Element var : intersection(contents.keySet(), other.contents.keySet())) {
      result.contents.put(var, contents.get(var).leastUpperBound(other.contents.get(var)));
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
    return contents.toString();
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
