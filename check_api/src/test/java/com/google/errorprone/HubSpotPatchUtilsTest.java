/*
 * Copyright 2020 The Error Prone Authors.
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

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class HubSpotPatchUtilsTest {

  @Test
  public void itIncrementsFileNames() throws Exception {
    Path path = Files.createTempDirectory("patch-utils-test");

    Path res1 = HubSpotPatchUtils.resolvePatchFile(path);
    assertThat(res1.toString()).endsWith("error-prone.patch");

    Files.createFile(path.resolve("error-prone.patch"));

    Path res2 = HubSpotPatchUtils.resolvePatchFile(path);
    assertThat(res2.toString()).endsWith("error-prone-1.patch");

    Files.createFile(path.resolve("error-prone-1.patch"));

    Path res3 = HubSpotPatchUtils.resolvePatchFile(path);
    assertThat(res3.toString()).endsWith("error-prone-2.patch");
  }
}
