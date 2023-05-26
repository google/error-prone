/*
 * Copyright 2023 The Error Prone Authors.
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

package com.google.errorprone.scanner;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.scanner.ErrorProneInjector.ProvisionException;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class ErrorProneInjectorTest {
  @Test
  public void retrievesPredefinedInstance() {
    var injector = ErrorProneInjector.create().addBinding(Integer.class, 2);

    assertThat(injector.getInstance(Integer.class))
        .isSameInstanceAs(injector.getInstance(Integer.class));
  }

  @Test
  public void noConstructor_injectable() {
    var injector = ErrorProneInjector.create();

    var unused = injector.getInstance(NoConstructor.class);
  }

  @Test
  public void injectConstructor_injectable() {
    var injector = ErrorProneInjector.create();

    var unused = injector.getInstance(InjectConstructor.class);
  }

  @Test
  public void bothConstructors_injectable() {
    var injector = ErrorProneInjector.create().addBinding(Integer.class, 2);

    var obj = injector.getInstance(InjectConstructorAndZeroArgConstructor.class);

    assertThat(obj.x).isEqualTo(2);
  }

  @Test
  public void errorProneFlags_favouredOverZeroArg() {
    var injector =
        ErrorProneInjector.create().addBinding(ErrorProneFlags.class, ErrorProneFlags.empty());

    var obj = injector.getInstance(ErrorProneFlagsAndZeroArgsConstructor.class);

    assertThat(obj.x).isEqualTo(1);
  }

  @Test
  public void pathInError() {
    var injector = ErrorProneInjector.create();

    var e =
        assertThrows(
            ProvisionException.class,
            () -> injector.getInstance(InjectConstructorAndZeroArgConstructor.class));

    assertThat(e).hasMessageThat().contains("Integer <- InjectConstructorAndZeroArgConstructor");
  }

  public static final class NoConstructor {}

  public static final class InjectConstructor {
    @Inject
    InjectConstructor() {}
  }

  public static final class InjectConstructorAndZeroArgConstructor {
    final int x;

    @Inject
    InjectConstructorAndZeroArgConstructor(Integer x) {
      this.x = x;
    }

    InjectConstructorAndZeroArgConstructor() {
      this.x = 0;
    }
  }

  public static final class ErrorProneFlagsAndZeroArgsConstructor {
    final int x;

    ErrorProneFlagsAndZeroArgsConstructor() {
      this.x = 0;
    }

    ErrorProneFlagsAndZeroArgsConstructor(ErrorProneFlags flags) {
      this.x = 1;
    }
  }
}
