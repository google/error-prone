/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import com.google.common.base.Optional;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** The Error Prone version. */
public final class ErrorProneVersion {

  private static final String PROPERTIES_RESOURCE =
      "/META-INF/maven/com.google.errorprone/error_prone_core/pom.properties";

  /**
   * Loads the Error Prone version.
   *
   * <p>This depends on the Maven build, and will always return {@code Optional.absent()} with other
   * build systems.
   */
  public static Optional<String> loadVersionFromPom() {
    try (InputStream stream = ErrorProneVersion.class.getResourceAsStream(PROPERTIES_RESOURCE)) {
      if (stream == null) {
        return Optional.absent();
      }
      Properties mavenProperties = new Properties();
      mavenProperties.load(stream);
      return Optional.of(mavenProperties.getProperty("version"));
    } catch (IOException expected) {
      return Optional.absent();
    }
  }
}
