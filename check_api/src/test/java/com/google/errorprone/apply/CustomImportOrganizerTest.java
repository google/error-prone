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
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import com.google.common.collect.ImmutableList;

/** {@link CustomImportOrganizer}Test */
@RunWith(JUnit4.class)
public class CustomImportOrganizerTest {

  private static final ImmutableList<ImportOrganizer.Import> IMPORTS =
      Stream.of(
              "import com.acme",
              "import org.a.example",
              "import org.b.example",
              "import unknown.fred",
              "import java.ping",
              "import org.a.sample",
              "import org.b.sample",
              "import javax.pong",
              "import unknown.barney",
              "import net.wilma",
              "import static com.acme.bla",
              "import static java.ping.bla",
              "import java.util",
              "import static unknown.fred.bla")
          .map(ImportOrganizer.Import::importOf)
          .collect(toImmutableList());

  @Test
  public void staticFirstOtherLast() {
    ImportOrganizer organizer = new CustomImportOrganizer("static=first,order=java:javax:org.a:org.b:com:OTHER");
    ImportOrganizer.OrganizedImports organized = organizer.organizeImports(IMPORTS);
    assertThat(organized.asImportBlock())
        .isEqualTo(
            "import static java.ping.bla;\n"
                + "\n"
                + "import static com.acme.bla;\n"
                + "\n"
                + "import static unknown.fred.bla;\n"
                + "\n"
                + "import java.ping;\n"
                + "import java.util;\n"
                + "\n"
                + "import javax.pong;\n"
                + "\n"
                + "import org.a.example;\n"
                + "import org.a.sample;\n"
                + "\n"
                + "import org.b.example;\n"
                + "import org.b.sample;\n"
                + "\n"
                + "import com.acme;\n"
                + "\n"
                + "import net.wilma;\n"
                + "import unknown.barney;\n"
                + "import unknown.fred;\n"
                );
  }
  
  @Test
  public void staticLastOtherFirst() {
    ImportOrganizer organizer = new CustomImportOrganizer("static=last,order=OTHER:java:javax:org.a:org.b:com");
    ImportOrganizer.OrganizedImports organized = organizer.organizeImports(IMPORTS);
    assertThat(organized.asImportBlock())
        .isEqualTo(
            "import net.wilma;\n"
                + "import unknown.barney;\n"
                + "import unknown.fred;\n"
                + "\n"
                + "import java.ping;\n"
                + "import java.util;\n"
                + "\n"
                + "import javax.pong;\n"
                + "\n"
                + "import org.a.example;\n"
                + "import org.a.sample;\n"
                + "\n"
                + "import org.b.example;\n"
                + "import org.b.sample;\n"
                + "\n"
                + "import com.acme;\n"
                + "\n"
                + "import static unknown.fred.bla;\n"
                + "\n"
                + "import static java.ping.bla;\n"
                + "\n"
                + "import static com.acme.bla;\n"
                );
  }
  
  @Test
  public void staticFirstOtherMiddle() {
    ImportOrganizer organizer = new CustomImportOrganizer("static=first,order=java:javax:OTHER:org.a:org.b:com");
    ImportOrganizer.OrganizedImports organized = organizer.organizeImports(IMPORTS);
    assertThat(organized.asImportBlock())
        .isEqualTo(
            "import static java.ping.bla;\n"
                + "\n"
                + "import static unknown.fred.bla;\n"
                + "\n"
                + "import static com.acme.bla;\n"
                + "\n"
                + "import java.ping;\n"
                + "import java.util;\n"
                + "\n"
                + "import javax.pong;\n"
                + "\n"
                + "import net.wilma;\n"
                + "import unknown.barney;\n"
                + "import unknown.fred;\n"
                + "\n"
                + "import org.a.example;\n"
                + "import org.a.sample;\n"
                + "\n"
                + "import org.b.example;\n"
                + "import org.b.sample;\n"
                + "\n"
                + "import com.acme;\n"
                );
  }
}
