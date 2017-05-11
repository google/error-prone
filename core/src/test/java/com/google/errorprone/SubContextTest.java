/*
 * Copyright 2013 Google Inc. All rights reserved.
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

import static org.junit.Assert.assertEquals;

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
  private static final Key<String> KEY1 = new Key<String>();
  private static final Key<String> KEY2 = new Key<String>();

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

    assertEquals("key1", overlay.get(KEY1));
    assertEquals(Enum1.VALUE1, overlay.get(Enum1.class));
    assertEquals("key2", overlay.get(KEY2));
    assertEquals(Enum2.VALUE, overlay.get(Enum2.class));

    assertEquals(null, base.get(KEY2));
    assertEquals(null, base.get(Enum2.class));
  }

  @Test
  public void testOverride() {
    Context base = new Context();
    base.put(KEY1, "key1");
    base.put(Enum1.class, Enum1.VALUE1);
    Context overlay = new SubContext(base);
    overlay.put(KEY1, "key2");
    overlay.put(Enum1.class, Enum1.VALUE2);

    assertEquals("key2", overlay.get(KEY1));
    assertEquals(Enum1.VALUE2, overlay.get(Enum1.class));
    assertEquals("key1", base.get(KEY1));
    assertEquals(Enum1.VALUE1, base.get(Enum1.class));
  }
}
