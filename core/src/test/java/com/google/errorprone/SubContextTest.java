/*
 * Copyright 2013 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone;

import static com.google.common.truth.Truth.assertThat;

import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Context.Key;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link SubContext}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class SubContextTest {
  private static final Key<String> KEY1 = new Key<>();
  private static final Key<String> KEY2 = new Key<>();

  enum Enum1 {
    VALUE1,
    VALUE2;
  }

  enum Enum2 {
    VALUE;
  }

  @Test
  public void testOverlay() {
    Context base = new Context();
    base.put(KEY1, "key1");
    base.put(Enum1.class, Enum1.VALUE1);
    Context overlay = new SubContext(base);
    overlay.put(KEY2, "key2");
    overlay.put(Enum2.class, Enum2.VALUE);

    assertThat(overlay.get(KEY1)).isEqualTo("key1");
    assertThat(overlay.get(Enum1.class)).isEqualTo(Enum1.VALUE1);
    assertThat(overlay.get(KEY2)).isEqualTo("key2");
    assertThat(overlay.get(Enum2.class)).isEqualTo(Enum2.VALUE);

    assertThat(base.get(KEY2)).isNull();
    assertThat(base.get(Enum2.class)).isNull();
  }

  @Test
  public void testOverride() {
    Context base = new Context();
    base.put(KEY1, "key1");
    base.put(Enum1.class, Enum1.VALUE1);
    Context overlay = new SubContext(base);
    overlay.put(KEY1, "key2");
    overlay.put(Enum1.class, Enum1.VALUE2);

    assertThat(overlay.get(KEY1)).isEqualTo("key2");
    assertThat(overlay.get(Enum1.class)).isEqualTo(Enum1.VALUE2);
    assertThat(base.get(KEY1)).isEqualTo("key1");
    assertThat(base.get(Enum1.class)).isEqualTo(Enum1.VALUE1);
  }
}
