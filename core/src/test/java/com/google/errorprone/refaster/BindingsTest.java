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

package com.google.errorprone.refaster;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.testing.EqualsTester;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.util.Names;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link Bindings}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class BindingsTest extends AbstractUTreeTest {
  private static class Key extends Bindings.Key<String> {
    Key(String identifier) {
      super(identifier);
    }
  }

  private static class OtherKey extends Bindings.Key<String> {
    OtherKey(String identifier) {
      super(identifier);
    }
  }

  private static class IntKey extends Bindings.Key<Integer> {
    IntKey(String identifier) {
      super(identifier);
    }
  }

  private static final Key FOO = new Key("foo");
  private static final Key BAR = new Key("bar");
  private static final IntKey BAZ = new IntKey("baz");

  @Test
  public void emptyBindings() {
    Bindings bindings = Bindings.create();
    assertThat(bindings.isEmpty()).isTrue();
    assertThat(bindings.containsKey(FOO)).isFalse();
    assertThat(bindings.getBinding(FOO)).isNull();
    assertThat(bindings.asMap()).isEmpty();
    assertThat(bindings.toString()).isEqualTo("{}");
  }

  @Test
  public void putAndGetBindings() {
    Bindings bindings = Bindings.create();
    bindings.putBinding(FOO, "hello");
    bindings.putBinding(BAZ, 42);

    assertThat(bindings.isEmpty()).isFalse();
    assertThat(bindings.asMap()).hasSize(2);
    assertThat(bindings.containsKey(FOO)).isTrue();
    assertThat(bindings.containsKey(BAR)).isFalse();
    assertThat(bindings.containsKey(BAZ)).isTrue();

    assertThat(bindings.getBinding(FOO)).isEqualTo("hello");
    assertThat(bindings.getBinding(BAZ)).isEqualTo(42);
    assertThat(bindings.getBinding(BAR)).isNull();

    assertThat(bindings.asMap()).containsExactly(FOO, "hello", BAZ, 42);
  }

  @Test
  public void nullChecks() {
    Bindings bindings = Bindings.create();
    assertThrows(NullPointerException.class, () -> bindings.containsKey(null));
    assertThrows(NullPointerException.class, () -> bindings.getBinding(null));
    assertThrows(NullPointerException.class, () -> bindings.putBinding(null, "val"));
    assertThrows(NullPointerException.class, () -> bindings.putBinding(FOO, null));
    assertThrows(NullPointerException.class, () -> bindings.clearBinding(null));
    assertThrows(NullPointerException.class, () -> bindings.forEach(null));
    assertThrows(NullPointerException.class, () -> bindings.hasFreeIdentMatching(null));
  }

  @Test
  @SuppressWarnings({
    "unchecked",
    "rawtypes"
  }) // raw Key cast to test runtime type enforcement in putBinding
  public void putRestricts() {
    Bindings bindings = Bindings.create();
    Bindings.Key key = new Key("foo");
    assertThrows(ClassCastException.class, () -> bindings.putBinding(key, 3));
  }

  @Test
  public void clearBinding() {
    Bindings bindings = Bindings.create();
    bindings.putBinding(FOO, "foo1");
    bindings.putBinding(BAR, "bar");
    bindings.putBinding(FOO, "foo2");
    bindings.putBinding(BAZ, 42);

    bindings.clearBinding(FOO);

    assertThat(bindings.asMap()).hasSize(2);
    assertThat(bindings.containsKey(FOO)).isFalse();
    assertThat(bindings.getBinding(FOO)).isNull();
    assertThat(bindings.getBinding(BAR)).isEqualTo("bar");
    assertThat(bindings.getBinding(BAZ)).isEqualTo(42);
    assertThat(bindings.asMap()).containsExactly(BAZ, 42, BAR, "bar");
  }

  @Test
  public void keyClassesDistinct() {
    new EqualsTester()
        .addEqualityGroup(new Key("foo"))
        .addEqualityGroup(new Key("bar"))
        .addEqualityGroup(new OtherKey("foo"))
        .testEquals();
  }

  @Test
  public void shadowingAndRebinding() {
    Bindings bindings = Bindings.create();
    bindings.putBinding(FOO, "first");
    bindings.putBinding(BAR, "other");
    bindings.putBinding(FOO, "second");

    assertThat(bindings.asMap()).hasSize(2);
    assertThat(bindings.getBinding(FOO)).isEqualTo("second");
    assertThat(bindings.getBinding(BAR)).isEqualTo("other");

    // forEach and asMap deduplicate shadowed keys, presenting the most recent value
    List<Object> values = new ArrayList<>();
    bindings.forEach((k, v) -> values.add(v));
    assertThat(values).containsExactly("second", "other");

    assertThat(bindings.asMap()).containsExactly(FOO, "second", BAR, "other");
  }

  @Test
  public void structuralSharingAndScopeIndependence() {
    Bindings parent = Bindings.create();
    parent.putBinding(FOO, "parent-foo");
    parent.putBinding(BAR, "parent-bar");

    Bindings child1 = Bindings.create(parent);
    Bindings child2 = Bindings.create(parent);

    child1.putBinding(FOO, "child1-foo");
    child1.putBinding(BAZ, 100);

    child2.putBinding(BAZ, 200);

    // Parent is unmodified
    assertThat(parent.getBinding(FOO)).isEqualTo("parent-foo");
    assertThat(parent.getBinding(BAR)).isEqualTo("parent-bar");
    assertThat(parent.containsKey(BAZ)).isFalse();

    // Child1 has child1 overrides
    assertThat(child1.getBinding(FOO)).isEqualTo("child1-foo");
    assertThat(child1.getBinding(BAR)).isEqualTo("parent-bar");
    assertThat(child1.getBinding(BAZ)).isEqualTo(100);

    // Child2 has child2 overrides
    assertThat(child2.getBinding(FOO)).isEqualTo("parent-foo");
    assertThat(child2.getBinding(BAR)).isEqualTo("parent-bar");
    assertThat(child2.getBinding(BAZ)).isEqualTo(200);
  }

  @Test
  public void hasBindingForLocalVar() {
    Names names = Names.instance(context);
    VarSymbol sym1 = new VarSymbol(0, names.fromString("myVar"), null, null);
    VarSymbol sym2 = new VarSymbol(0, names.fromString("otherVar"), null, null);
    LocalVarBinding localBinding = LocalVarBinding.create(sym1, null);

    Bindings bindings = Bindings.create();
    assertThat(bindings.hasBindingForLocalVar(sym1)).isFalse();
    assertThat(bindings.hasBindingForLocalVar(null)).isFalse();

    bindings.putBinding(new ULocalVarIdent.Key("myVar"), localBinding);
    assertThat(bindings.hasBindingForLocalVar(sym1)).isTrue();
    assertThat(bindings.hasBindingForLocalVar(sym2)).isFalse();
    assertThat(bindings.hasBindingForLocalVar(null)).isFalse();
  }

  @Test
  public void hasFreeIdentMatching() {
    JCExpression expr1 = parseExpression("\"foo\".length()");
    JCExpression expr2 = parseExpression("x + 1");
    JCExpression expr3 = parseExpression("y * 2");

    Bindings bindings = Bindings.create();
    assertThat(bindings.hasFreeIdentMatching(expr -> expr.toString().contains("length"))).isFalse();

    bindings.putBinding(new UFreeIdent.Key("e3"), expr3);
    bindings.putBinding(new UFreeIdent.Key("e1"), expr1);
    bindings.putBinding(new UFreeIdent.Key("e2"), expr2);
    // head chain: e2 ("x + 1") -> e1 ("\"foo\".length()") -> e3 ("y * 2")

    AtomicInteger matchCount = new AtomicInteger();
    boolean matches =
        bindings.hasFreeIdentMatching(
            expr -> {
              matchCount.incrementAndGet();
              return expr.toString().contains("length");
            });

    assertThat(matches).isTrue();
    // Tested e2 (false), e1 (true), and short-circuited before testing e3
    assertThat(matchCount.get()).isEqualTo(2);

    assertThat(bindings.hasFreeIdentMatching(expr -> expr.toString().contains("nonExistent")))
        .isFalse();
  }
}
