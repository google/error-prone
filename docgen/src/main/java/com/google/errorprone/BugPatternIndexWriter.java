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

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Ordering;
import com.google.errorprone.DocGenTool.Target;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class BugPatternIndexWriter {

  void dump(Collection<BugPatternInstance> patterns, Writer w, Target target) throws IOException {

    Map<String, List<Map<String, String>>> data = new TreeMap<>(Ordering.natural().reverse());

    ListMultimap<String, BugPatternInstance> index =
        index(
            patterns,
            new Function<BugPatternInstance, String>() {
              @Override
              public String apply(BugPatternInstance input) {
                return (input.maturity.description + " : " + input.severity).replace("_", "\\_");
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

    Map<String, Object> templateData = new HashMap<>();

    List<Map<String, Object>> entryData = new ArrayList<>();
    for (Entry<String, List<Map<String, String>>> entry : data.entrySet()) {
      entryData.add(ImmutableMap.of("category", entry.getKey(), "checks", entry.getValue()));
    }
    templateData.put("bugpatterns", entryData);

    if (target == Target.EXTERNAL) {
      Map<String, String> frontmatterData =
          ImmutableMap.<String, String>builder()
              .put("title", "Bug Patterns")
              .put("layout", "master")
              .build();
      DumperOptions options = new DumperOptions();
      options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
      Yaml yaml = new Yaml(options);
      Writer yamlWriter = new StringWriter();
      yamlWriter.write("---\n");
      yaml.dump(frontmatterData, yamlWriter);
      yamlWriter.write("---\n");
      templateData.put("frontmatter", yamlWriter.toString());

      MustacheFactory mf = new DefaultMustacheFactory();
      Mustache mustache =
          mf.compile("com/google/errorprone/resources/bugpatterns_external.mustache");
      mustache.execute(w, templateData);
    } else {
      MustacheFactory mf = new DefaultMustacheFactory();
      Mustache mustache =
          mf.compile("com/google/errorprone/resources/bugpatterns_internal.mustache");
      mustache.execute(w, templateData);
    }
  }
}
