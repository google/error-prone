/*
 * Copyright 2018 The Error Prone Authors.
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

package com.google.errorprone.apply;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link IdeaImportOrganizer}Test */
@RunWith(JUnit4.class)
public class IdeaImportOrganizerTest {

  private static final ImmutableList<ImportOrganizer.Import> IMPORTS =
      Stream.of(
              "import com.android.blah",
              "import android.foo",
              "import java.ping",
              "import javax.pong",
              "import unknown.fred",
              "import unknown.barney",
              "import net.wilma",
              "import static com.android.blah.blah",
              "import static android.foo.bar",
              "import static java.ping.pong",
              "import static javax.pong.ping",
              "import static unknown.fred.flintstone",
              "import static net.wilma.flintstone")
          .map(ImportOrganizer.Import::importOf)
          .collect(toImmutableList());

  @Test
  public void testStaticLastOrdering() {
    IdeaImportOrganizer organizer = new IdeaImportOrganizer();
    ImportOrganizer.OrganizedImports organized = organizer.organizeImports(IMPORTS);
    assertThat(organized.asImportBlock())
        .isEqualTo(
            "import android.foo;\n"
                + "import com.android.blah;\n"
                + "import net.wilma;\n"
                + "import unknown.barney;\n"
                + "import unknown.fred;\n"
                + "\n"
                + "import javax.pong;\n"
                + "import java.ping;\n"
                + "\n"
                + "import static android.foo.bar;\n"
                + "import static com.android.blah.blah;\n"
                + "import static java.ping.pong;\n"
                + "import static javax.pong.ping;\n"
                + "import static net.wilma.flintstone;\n"
                + "import static unknown.fred.flintstone;\n");
  }
}
