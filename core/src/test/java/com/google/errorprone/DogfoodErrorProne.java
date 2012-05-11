/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import java.io.File;
import java.io.FileFilter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Compile error-prone using error-prone. Useful for rough benchmarking.
 * @author alexeagle@google.com (Alex Eagle)
 */
public class DogfoodErrorProne {

  public static void main(String[] args) throws Exception {
    new DogfoodErrorProne().compile();
  }

  private void compile() throws URISyntaxException {
    long start = System.currentTimeMillis();
    new ErrorProneCompiler.Builder().build().compile(findSources());
    System.out.printf("Finished compiling in %d millis\n", System.currentTimeMillis() - start);
  }

  private String[] findSources() throws URISyntaxException {
   String propertiesFile = new File(getClass().getResource("errors.properties").toURI())
       .getAbsolutePath();
    File baseDir = new File(propertiesFile
        .substring(0, propertiesFile.indexOf("core") + "core".length()), "src/main/java");
    List<String> sources = new ArrayList<String>();
    addSourcesUnder(baseDir, sources);
    return sources.toArray(new String[sources.size()]);
  }

  private void addSourcesUnder(File baseDir, List<String> sources) {
    FileFilter javaSources = new FileFilter() {
      @Override
      public boolean accept(File file) {
        return file.isDirectory() || file.getName().endsWith(".java");
      }
    };
    for (File file : baseDir.listFiles(javaSources)) {
      if (file.isDirectory()) {
        addSourcesUnder(file, sources);
      } else {
        sources.add(file.getAbsolutePath());
      }
    }
  }

}
