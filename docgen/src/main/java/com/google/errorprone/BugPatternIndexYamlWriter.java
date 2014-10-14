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
import com.google.errorprone.BugPattern.Instance;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class BugPatternIndexYamlWriter {

  void dump(Collection<Instance> patterns, Writer w)
      throws IOException {
    Map<String, List<Map<String, String>>> data = new TreeMap<>(Ordering.natural().reverse());

   ListMultimap<String, BugPattern.Instance> index = index(patterns, new Function<Instance, String>() {
      @Override
      public String apply(Instance input) {
        return input.maturity.description + " : " + input.severity;
      }});

    for (Entry<String, Collection<Instance>> entry : index.asMap().entrySet()) {
      data.put(entry.getKey(), FluentIterable
          .from(entry.getValue())
          .transform(new Function<Instance, Map<String, String>>() {
            @Override
            public Map<String, String> apply(Instance input) {
              return ImmutableMap.of("name", input.name, "summary", input.summary);
            }
          })
          .toSortedList(new Ordering<Map<String, String>>() {
            @Override
            public int compare(Map<String, String> left, Map<String, String> right) {
              return left.get("name").compareTo(right.get("name"));
            }
          }));
    }
    new Yaml().dump(data, w);
  }
}
