/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.overloading;

import static java.util.Comparator.comparingInt;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.sun.source.tree.MethodTree;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.lang.model.element.Name;

/**
 * A simple <a href="https://en.wikipedia.org/wiki/Trie">trie</a> implementation.
 *
 * <p>Edges between trie nodes represent method parameters and each path represents some method
 * signature. This trie is an archetype that next method signatures must follow - we extend the trie
 * with new signatures reporting all ordering violations along the way.
 *
 * @author hanuszczak@google.com (Łukasz Hanuszczak)
 */
class ParameterTrie {

  private final Map<Name, ParameterTrie> children;

  public ParameterTrie() {
    this.children = new HashMap<>();
  }

  /**
   * Extends the trie with parameters of {@code methodTree}.
   *
   * <p>If any ordering {@link ParameterOrderingViolation} is discovered during extension procedure,
   * they are reported in the result.
   *
   * <p>The method signature is considered to violate the consistency if there exists a path in the
   * archetype (the trie) that could be used but would require parameter reordering. The extension
   * algorithm simply walks down the trie as long as possible until the signature has parameters
   * matching existing edges in the trie. Once this is no longer possible, new nodes are the trie is
   * extended with remaining parameters.
   */
  public Optional<ParameterOrderingViolation> extendAndComputeViolation(MethodTree methodTree) {
    return new ParameterTrieExtender(methodTree).execute(this);
  }

  /**
   * A convenience class used by {@link ParameterTrie#extendAndComputeViolation(MethodTree)} method.
   *
   * <p>The extension algorithm is inherently stateful. Because threading three state arguments in
   * every recursive function invocation is inconvenient, we use this class to keep track of the
   * state in class members.
   *
   * <p>The violation detection works like this: first a method signature (i.e. all the method
   * parameters) is added to the trie. As long as it is possible the algorithm tries to find a
   * parameter that can be followed using existing edges in the trie. When no such parameter is
   * found, the trie extension procedure begins where remaining parameters are added to the trie in
   * order in which they appear in the initial list. This whole procedure (a path that was followed
   * during the process) determines a "correct", "consistent" ordering of the parameters. If the
   * original input list of parameters has a different order that the one determined by the
   * algorithm a violation is reported.
   */
  private static class ParameterTrieExtender {

    private final MethodTree methodTree;

    private final SortedSet<Parameter> inputParameters;
    private final List<Parameter> outputParameters;

    public ParameterTrieExtender(MethodTree methodTree) {
      this.methodTree = methodTree;

      this.inputParameters = new TreeSet<>(comparingInt(Parameter::position));
      this.outputParameters = new ArrayList<>();
    }

    /**
     * Extends given {@code trie} with initial {@link MethodTree}.
     *
     * <p>If any {@link ParameterOrderingViolation} is found during the extension procedure, it is
     * reported in the result.
     */
    public Optional<ParameterOrderingViolation> execute(ParameterTrie trie) {
      Preconditions.checkArgument(trie != null);

      initialize();
      walk(trie);
      return validate();
    }

    /**
     * Initializes the input and output parameter collections.
     *
     * <p>After initialization, the input parameters should be equal to parameters of the given
     * {@link MethodTree} and output parameters should be empty. After processing this should be
     * reversed: input parameters list should be empty and output parameters should be an ordered
     * version of parameters of the given {@link MethodTree}.
     */
    private void initialize() {
      for (int i = 0; i < getMethodTreeArity(methodTree); i++) {
        inputParameters.add(Parameter.create(methodTree, i));
      }
      outputParameters.clear();
    }

    /**
     * Processes input parameters by walking along the trie as long as there is a path.
     *
     * <p>Once the walk is no longer possible (there is no trie child that matches one of the input
     * parameters) the extender begins expansion procedure (see {@link
     * ParameterTrieExtender#execute(ParameterTrie)}).
     */
    private void walk(ParameterTrie trie) {
      Preconditions.checkArgument(trie != null);

      for (Parameter parameter : inputParameters) {
        if (parameter.tree().isVarArgs() || !trie.children.containsKey(parameter.name())) {
          continue;
        }

        inputParameters.remove(parameter);
        outputParameters.add(parameter);
        walk(trie.children.get(parameter.name()));
        return;
      }

      // Walking not possible anymore, start expansion.
      expand(trie);
    }

    /**
     * Expands the trie with leftover input parameters.
     *
     * <p>It is assumed that given {@code trie} does not have a key corresponding to any input
     * parameter which should happen only if {@link ParameterTrieExtender#walk(ParameterTrie)} has
     * nowhere else to go.
     *
     * <p>Only non-varargs parameters are added to the trie. Any varargs parameters should always be
     * placed at the end of the method signature so it makes no sense to include them in the
     * archetype.
     */
    private void expand(ParameterTrie trie) {
      Preconditions.checkArgument(trie != null);

      if (inputParameters.isEmpty()) {
        return;
      }

      Parameter parameter = inputParameters.first();
      inputParameters.remove(parameter);
      outputParameters.add(parameter);

      ParameterTrie allocatedTrie = new ParameterTrie();
      if (!parameter.tree().isVarArgs()) {
        trie.children.put(parameter.name(), allocatedTrie);
      }
      expand(allocatedTrie);
    }

    /**
     * Reports {@link ParameterOrderingViolation} if output parameters are not ordered as in input
     * file.
     *
     * <p>This method simply goes through the list of output parameters (input parameters ordered by
     * trie traversal algorithm). If a parameter at particular position differs from its original
     * position a violation is reported.
     *
     * <p>If the ordering is consistent then there is no violation and an empty optional is
     * returned.
     */
    private Optional<ParameterOrderingViolation> validate() {
      ImmutableList.Builder<ParameterTree> actual = ImmutableList.builder();
      ImmutableList.Builder<ParameterTree> expected = ImmutableList.builder();

      boolean valid = true;
      for (int i = 0; i < outputParameters.size(); i++) {
        Parameter parameter = outputParameters.get(i);
        if (parameter.position() != i) {
          valid = false;
        }

        actual.add(parameter.tree());
        expected.add(getParameterTree(parameter.position()));
      }

      if (valid) {
        return Optional.empty();
      } else {
        ParameterOrderingViolation violation =
            ParameterOrderingViolation.builder()
                .setMethodTree(methodTree)
                .setActual(actual.build())
                .setExpected(expected.build())
                .build();
        return Optional.of(violation);
      }
    }

    /** Returns a {@link ParameterTree} at {@code position} in the associated method. */
    private ParameterTree getParameterTree(int position) {
      return ParameterTree.create(methodTree.getParameters().get(position));
    }
  }

  /**
   * A class used to represent a {@link ParameterTree} (which is essentially just a {@link
   * com.sun.source.tree.VariableTree} and its original position within the {@link MethodTree}.
   */
  @AutoValue
  abstract static class Parameter {

    public abstract ParameterTree tree();

    public abstract int position();

    public Name name() {
      return tree().getName();
    }

    public static Parameter create(MethodTree methodTree, int position) {
      ParameterTree parameterTree = ParameterTree.create(methodTree.getParameters().get(position));
      return new AutoValue_ParameterTrie_Parameter(parameterTree, position);
    }
  }

  /** Returns arity (number of parameters) of given {@code methodTree}. */
  static int getMethodTreeArity(MethodTree methodTree) {
    return methodTree.getParameters().size();
  }
}
