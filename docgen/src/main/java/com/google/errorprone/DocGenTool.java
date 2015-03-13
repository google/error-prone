/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import static com.google.common.io.Files.readLines;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.io.Files;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * Utility main which consumes the same tab-delimited text file and generates GitHub pages for
 * the BugPatterns.
 */
public class DocGenTool {

  static class Options {
    @Parameter(names = {"-bug_patterns"}, description = "Path to bugPatterns.txt",
        required = true)
    private String bugPatterns;

    @Parameter(names = {"-docs_repository"}, description = "Path to docs repository",
        required = true)
    private String docsRepository;

    @Parameter(names = {"-examples"}, description = "Path to examples", required = true)
    private String examples;

    @Parameter(names = {"-generate_frontmatter"}, description = "Generate yaml front-matter",
        arity = 1)
    private boolean generateFrontMatter = true;

    @Parameter(names = {"-use_pygments_highlighting"},
        description = "Use pygments for highlighting", arity = 1)
    private boolean usePygments = true;
  }

  public static void main(String[] args) throws IOException {
    Options options = new Options();
    new JCommander(options, args);

    final File bugPatterns = new File(options.bugPatterns);
    if (!bugPatterns.exists()) {
      System.err.println("Cannot find bugPatterns file: " + options.bugPatterns);
      System.exit(1);
    }
    final File wikiDir = new File(options.docsRepository);
    wikiDir.mkdir();
    final File exampleDirBase = new File(options.examples);
    if (!exampleDirBase.exists()) {
      System.err.println("Cannot find example directory: " + options.examples);
      System.exit(1);
    }

    File bugpatternDir = new File(wikiDir, "bugpattern");
    if (!bugpatternDir.exists()) {
      bugpatternDir.mkdirs();
    }
    new File(wikiDir, "_data").mkdirs();
    BugPatternFileGenerator generator = new BugPatternFileGenerator(
        bugpatternDir, exampleDirBase, options.generateFrontMatter, options.usePygments);
    try (Writer w =
        Files.newWriter(new File(wikiDir, "_data/bugpatterns.yaml"), StandardCharsets.UTF_8)) {
      new BugPatternIndexYamlWriter().dump(readLines(bugPatterns, UTF_8, generator), w);
    }
  }
}
