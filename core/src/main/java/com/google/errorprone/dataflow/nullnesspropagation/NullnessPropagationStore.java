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

import com.google.common.collect.ImmutableMap;

import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;

import java.util.HashMap;
import java.util.Map;

import javax.lang.model.element.Element;

/**
 * Immutable map from each local variable to its {@link NullnessValue}. Note that, while the
 * interface is written in terms of <b>nodes</b>, the stored data is indexed by variable
 * <b>declaration</b>, so values persist across nodes.
 *
 * <p>To derive a new instance, {@linkplain #toBuilder() create a builder} from an old instance. To
 * start from scratch, call {@link #empty()}.
 *
 * @author deminguyen@google.com (Demi Nguyen)
 */
public final class NullnessPropagationStore implements Store<NullnessPropagationStore> {
  private static final NullnessPropagationStore EMPTY =
      new NullnessPropagationStore(ImmutableMap.<Element, NullnessValue>of());

  public static NullnessPropagationStore empty() {
    return EMPTY;
  }

  /*
   * TODO(cpovirk): Return to LocalVariableNode keys if LocalVariableNode.equals is fixed to use the
   * variable's declaring element instead of its name.
   */
  private final ImmutableMap<Element, NullnessValue> contents;

  private NullnessPropagationStore(Map<Element, NullnessValue> contents) {
    this.contents = ImmutableMap.copyOf(contents);
  }

  public NullnessValue getInformation(LocalVariableNode node) {
    return contents.get(node.getElement());
  }

  public Builder toBuilder() {
    return new Builder(this);
  }

  /**
   * Builder for {@link NullnessPropagationStore} instances. To obtain an instance, obtain a
   * {@link NullnessPropagationStore} (such as {@link NullnessPropagationStore#empty()}), and call
   * {@link NullnessPropagationStore#toBuilder() toBuilder()} on it.
   */
  public static final class Builder {
    private final Map<Element, NullnessValue> contents;

    Builder(NullnessPropagationStore prototype) {
      contents = new HashMap<>(prototype.contents);
    }

    public void setInformation(LocalVariableNode node, NullnessValue value) {
      contents.put(node.getElement(), checkNotNull(value));
    }

    public NullnessPropagationStore build() {
      return new NullnessPropagationStore(contents);
    }
  }

  @Override
  public NullnessPropagationStore copy() {
    // No need to copy because it's immutable.
    return this;
  }

  @Override
  public NullnessPropagationStore leastUpperBound(NullnessPropagationStore other) {
    Builder result = EMPTY.toBuilder();
    for (Element var : intersection(contents.keySet(), other.contents.keySet())) {
      result.contents.put(var, contents.get(var).leastUpperBound(other.contents.get(var)));
    }
    return result.build();
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
