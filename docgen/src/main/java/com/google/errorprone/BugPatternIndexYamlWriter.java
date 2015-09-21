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

import static com.google.common.collect.Multimaps.index;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Ordering;

import org.yaml.snakeyaml.Yaml;

import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class BugPatternIndexYamlWriter {

  void dump(Collection<BugPatternInstance> patterns, Writer w) {
    Map<String, List<Map<String, String>>> data = new TreeMap<>(Ordering.natural().reverse());

    ListMultimap<String, BugPatternInstance> index =
        index(
            patterns,
            new Function<BugPatternInstance, String>() {
              @Override
              public String apply(BugPatternInstance input) {
                return input.maturity.description + " : " + input.severity;
              }
            });

    for (Entry<String, Collection<BugPatternInstance>> entry : index.asMap().entrySet()) {
      data.put(
          entry.getKey(),
          FluentIterable.from(entry.getValue())
              .transform(
                  new Function<BugPatternInstance, Map<String, String>>() {
                    @Override
                    public Map<String, String> apply(BugPatternInstance input) {
                      return ImmutableMap.of("name", input.name, "summary", input.summary);
                    }
                  })
              .toSortedList(
                  new Ordering<Map<String, String>>() {
                    @Override
                    public int compare(Map<String, String> left, Map<String, String> right) {
                      return left.get("name").compareTo(right.get("name"));
                    }
                  }));
    }
    new Yaml().dump(data, w);
  }
}
