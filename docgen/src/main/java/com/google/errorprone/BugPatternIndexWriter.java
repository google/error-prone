/*
 * Copyright 2014 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.primitives.Booleans.trueFirst;
import static java.util.Comparator.comparing;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.DocGenTool.Target;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class BugPatternIndexWriter {

  private record IndexEntry(boolean onByDefault, SeverityLevel severity) {
    static IndexEntry create(boolean onByDefault, SeverityLevel severity) {
      return new IndexEntry(onByDefault, severity);
    }

    String asCategoryHeader() {
      return (onByDefault() ? "On by default" : "Experimental") + " : " + severity();
    }
  }

  private record MiniDescription(String name, String summary) {
    static MiniDescription create(BugPatternInstance bugPattern) {
      return new MiniDescription(bugPattern.name, bugPattern.summary);
    }
  }

  void dump(
      Collection<BugPatternInstance> patterns, Writer w, Target target, Set<String> enabledChecks)
      throws IOException {
    // (Default, Severity) -> [Pattern...]
    SortedSetMultimap<IndexEntry, MiniDescription> sorted =
        TreeMultimap.create(
            comparing(IndexEntry::onByDefault, trueFirst()).thenComparing(IndexEntry::severity),
            Comparator.comparing(MiniDescription::name));
    for (BugPatternInstance pattern : patterns) {
      sorted.put(
          IndexEntry.create(enabledChecks.contains(pattern.name), pattern.severity),
          MiniDescription.create(pattern));
    }

    Map<String, Object> templateData = new HashMap<>();

    ImmutableList<Map<String, Object>> bugpatternData =
        Multimaps.asMap(sorted).entrySet().stream()
            .map(
                e ->
                    ImmutableMap.of(
                        "category", e.getKey().asCategoryHeader(), "checks", e.getValue()))
            .collect(toImmutableList());

    templateData.put("bugpatterns", bugpatternData);

    if (target == Target.EXTERNAL) {
      ImmutableMap<String, String> frontmatterData =
          ImmutableMap.<String, String>builder()
              .put("title", "Bug Patterns")
              .put("layout", "bugpatterns")
              .buildOrThrow();
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
