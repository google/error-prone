/*
 * Copyright 2022 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.checkreturnvalue;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for the {@link CanIgnoreReturnValueSuggester}. */
@RunWith(JUnit4.class)
public class CanIgnoreReturnValueSuggesterTest {

  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(CanIgnoreReturnValueSuggester.class, getClass());

  @Test
  public void simpleCase() {
    helper
        .addInputLines(
            "Client.java",
            """
            package com.google.frobber;

            public final class Client {
              private String name;

              public Client setName(String name) {
                this.name = name;
                return this;
              }
            }
            """)
        .addOutputLines(
            "Client.java",
            """
            package com.google.frobber;

            import com.google.errorprone.annotations.CanIgnoreReturnValue;

            public final class Client {
              private String name;

              @CanIgnoreReturnValue
              public Client setName(String name) {
                this.name = name;
                return this;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void b362106953_returnThis() {
    helper
        .addInputLines(
            "Client.java",
            """
            package com.google.frobber;

            public final class Client {
              public Client setName(String name) {
                return this;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void b362106953_returnParam() {
    helper
        .addInputLines(
            "Client.java",
            """
            package com.google.frobber;

            public final class Client {
              public String setName(String name) {
                return name;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void parenthesizedCastThis() {
    helper
        .addInputLines(
            "Client.java",
            """
            package com.google.frobber;

            public final class Client {
              private String name;

              public Client setName(String name) {
                this.name = name;
                return ((Client) (this));
              }
            }
            """)
        .addOutputLines(
            "Client.java",
            """
            package com.google.frobber;

            import com.google.errorprone.annotations.CanIgnoreReturnValue;

            public final class Client {
              private String name;

              @CanIgnoreReturnValue
              public Client setName(String name) {
                this.name = name;
                return ((Client) (this));
              }
            }
            """)
        .doTest();
  }

  @Test
  public void returnsInputParam() {
    helper
        .addInputLines(
            "Client.java",
            """
            package com.google.frobber;

            public final class Client {
              public String method(String name) {
                System.out.println(name);
                return name;
              }
            }
            """)
        .addOutputLines(
            "Client.java",
            """
            package com.google.frobber;

            import com.google.errorprone.annotations.CanIgnoreReturnValue;

            public final class Client {
              @CanIgnoreReturnValue
              public String method(String name) {
                System.out.println(name);
                return name;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void returnsInputParam_intParam() {
    helper
        .addInputLines(
            "Client.java",
            """
            package com.google.frobber;

            public final class Client {
              public int method(int value) {
                value = 42;
                return value;
              }
            }
            """)
        // make sure we don't fire if the value has been re-assigned!
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void returnsInputParam_withParens() {
    helper
        .addInputLines(
            "Client.java",
            """
            package com.google.frobber;

            public final class Client {
              public String method(String name) {
                return (name);
              }
            }
            """)
        .addOutputLines(
            "Client.java",
            """
            package com.google.frobber;

            import com.google.errorprone.annotations.CanIgnoreReturnValue;

            public final class Client {
              @CanIgnoreReturnValue
              public String method(String name) {
                return (name);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void returnInputParams_multipleParams() {
    helper
        .addInputLines(
            "ReturnInputParam.java",
            """
            package com.google.frobber;

            public final class ReturnInputParam {
              public static StringBuilder append(StringBuilder input, String name) {
                input.append(name);
                return input;
              }
            }
            """)
        .addOutputLines(
            "ReturnInputParam.java",
            """
            package com.google.frobber;

            import com.google.errorprone.annotations.CanIgnoreReturnValue;

            public final class ReturnInputParam {
              @CanIgnoreReturnValue
              public static StringBuilder append(StringBuilder input, String name) {
                input.append(name);
                return input;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void returnInputParams_moreComplex() {
    helper
        .addInputLines(
            "ReturnInputParam.java",
            """
            package com.google.frobber;

            public final class ReturnInputParam {
              public static StringBuilder append(StringBuilder input, String name) {
                input.append("name = ").append(name);
                return input;
              }
            }
            """)
        .addOutputLines(
            "ReturnInputParam.java",
            """
            package com.google.frobber;

            import com.google.errorprone.annotations.CanIgnoreReturnValue;

            public final class ReturnInputParam {
              @CanIgnoreReturnValue
              public static StringBuilder append(StringBuilder input, String name) {
                input.append("name = ").append(name);
                return input;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void returnsInputParamWithMultipleReturns() {
    helper
        .addInputLines(
            "Client.java",
            """
            package com.google.frobber;

            public final class Client {
              public String method(String a, String b) {
                if (System.currentTimeMillis() > 0) {
                  return a;
                }
                return b;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void returnsInputParamWithMultipleReturns_oneReturnIsConstant() {
    helper
        .addInputLines(
            "Client.java",
            """
            package com.google.frobber;

            public final class Client {
              public String method(String a, String b) {
                if (System.currentTimeMillis() > 0) {
                  return a;
                }
                return "hi";
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void returnsInputParamWithTernary() {
    helper
        .addInputLines(
            "Client.java",
            """
            package com.google.frobber;

            public final class Client {
              public String method(String a, String b) {
                return (System.currentTimeMillis() > 0) ? a : b;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void builder_abstractClass() {
    helper
        .addInputLines(
            "Builder.java",
            """
            package com.google.frobber;

            public abstract class Builder {
              public abstract Builder setName(String name);

              public abstract Builder enableDeathStar();

              public abstract Builder clone();

              public abstract Builder copy();

              public abstract Builder getCopy();

              public abstract Builder newBuilder();
            }
            """)
        .addOutputLines(
            "Builder.java",
            """
            package com.google.frobber;

            import com.google.errorprone.annotations.CanIgnoreReturnValue;

            public abstract class Builder {
              @CanIgnoreReturnValue
              public abstract Builder setName(String name);

              @CanIgnoreReturnValue
              public abstract Builder enableDeathStar();

              public abstract Builder clone();

              public abstract Builder copy();

              public abstract Builder getCopy();

              public abstract Builder newBuilder();
            }
            """)
        .doTest();
  }

  @Test
  public void builder_interface() {
    helper
        .addInputLines(
            "Builder.java",
            """
            package com.google.frobber;

            public interface Builder {
              Builder setName(String name);

              Builder enableDeathStar();

              Builder copy();

              Builder clone();

              Builder newBuilder();
            }
            """)
        .addOutputLines(
            "Builder.java",
            """
            package com.google.frobber;

            import com.google.errorprone.annotations.CanIgnoreReturnValue;

            public interface Builder {
              @CanIgnoreReturnValue
              Builder setName(String name);

              @CanIgnoreReturnValue
              Builder enableDeathStar();

              Builder copy();

              Builder clone();

              Builder newBuilder();
            }
            """)
        .doTest();
  }

  @Test
  public void autoValueBuilder() {
    helper
        .addInputLines(
            "Animal.java",
            """
            package com.google.frobber;

            import com.google.auto.value.AutoValue;

            @AutoValue
            abstract class Animal {
              abstract String name();

              abstract int numberOfLegs();

              static Builder builder() {
                return null;
              }

              @AutoValue.Builder
              abstract static class Builder {
                abstract Builder setName(String value);

                abstract Builder setNumberOfLegs(int value);

                abstract Animal build();
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void returnSelf_b234875737() {
    helper
        .addInputLines(
            "Client.java",
            """
            package com.google.frobber;

            public final class Client {
              private String name;

              public Client setName(String name) {
                this.name = name;
                return self();
              }

              private Client self() {
                return this;
              }
            }
            """)
        .addOutputLines(
            "Client.java",
            """
            package com.google.frobber;

            import com.google.errorprone.annotations.CanIgnoreReturnValue;

            public final class Client {
              private String name;

              @CanIgnoreReturnValue
              public Client setName(String name) {
                this.name = name;
                return self();
              }

              private Client self() {
                return this;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void returnGetThis() {
    helper
        .addInputLines(
            "Client.java",
            """
            package com.google.frobber;

            public final class Client {
              private String name;

              public Client setName(String name) {
                this.name = name;
                return getThis();
              }

              private Client getThis() {
                return this;
              }
            }
            """)
        .addOutputLines(
            "Client.java",
            """
            package com.google.frobber;

            import com.google.errorprone.annotations.CanIgnoreReturnValue;

            public final class Client {
              private String name;

              @CanIgnoreReturnValue
              public Client setName(String name) {
                this.name = name;
                return getThis();
              }

              private Client getThis() {
                return this;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void simpleCaseAlreadyAnnotatedWithCirv() {
    helper
        .addInputLines(
            "Client.java",
            """
            package com.google.frobber;

            import com.google.errorprone.annotations.CanIgnoreReturnValue;

            public final class Client {
              private String name;

              @CanIgnoreReturnValue
              public Client setName(String name) {
                this.name = name;
                return this;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void alreadyAnnotatedWithProtobufCirv_b356526159() {
    helper
        .addInputLines(
            "Client.java",
            """
            package com.google.protobuf;

            public final class Client {
              private String name;

              @CanIgnoreReturnValue
              public Client setName(String name) {
                this.name = name;
                return this;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void alreadyAnnotatedWithArbitraryCirv_b356526159() {
    helper
        .addInputLines(
            "Client.java",
            """
            package com.google.frobber;

            public final class Client {
              @interface CanIgnoreReturnValue {}

              private String name;

              @CanIgnoreReturnValue
              public Client setName(String name) {
                this.name = name;
                return this;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void simpleCaseAlreadyAnnotatedWithCrv() {
    helper
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.CheckReturnValue;",
            "public final class Client {",
            "  private String name;",
            "  @CheckReturnValue", // this is "wrong" -- the checker could fix it though!
            "  public Client setName(String name) {",
            "    this.name = name;",
            "    return this;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void simpleCaseWithNestedLambda() {
    helper
        .addInputLines(
            "Client.java",
            """
            package com.google.frobber;

            import java.util.function.Function;

            public final class Client {
              private String name;

              public Client setName(String name) {
                new Function<String, String>() {
                  @Override
                  public String apply(String in) {
                    return "kurt";
                  }
                };
                return this;
              }
            }
            """)
        .addOutputLines(
            "Client.java",
            """
            package com.google.frobber;

            import com.google.errorprone.annotations.CanIgnoreReturnValue;
            import java.util.function.Function;

            public final class Client {
              private String name;

              @CanIgnoreReturnValue
              public Client setName(String name) {
                new Function<String, String>() {
                  @Override
                  public String apply(String in) {
                    return "kurt";
                  }
                };
                return this;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void anotherMethodDoesntReturnThis() {
    helper
        .addInputLines(
            "Client.java",
            """
            package com.google.frobber;

            public final class Client {
              private String name;

              public Client setName(String name) {
                this.name = name;
                return this;
              }

              public Client getValue2() {
                return new Client();
              }
            }
            """)
        .addOutputLines(
            "Client.java",
            """
            package com.google.frobber;

            import com.google.errorprone.annotations.CanIgnoreReturnValue;

            public final class Client {
              private String name;

              @CanIgnoreReturnValue
              public Client setName(String name) {
                this.name = name;
                return this;
              }

              public Client getValue2() {
                return new Client();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nestedCase() {
    helper
        .addInputLines(
            "Client.java",
            """
            package com.google.frobber;

            public final class Client {
              private String name;

              public Client setName(String name) {
                this.name = name;
                if (true) {
                  return new Client();
                }
                return this;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void nestedCaseBothReturningThis() {
    helper
        .addInputLines(
            "Client.java",
            """
            package com.google.frobber;

            public final class Client {
              private String name;

              public Client setName(String name) {
                this.name = name;
                if (true) {
                  return this;
                }
                return this;
              }
            }
            """)
        .addOutputLines(
            "Client.java",
            """
            package com.google.frobber;

            import com.google.errorprone.annotations.CanIgnoreReturnValue;

            public final class Client {
              private String name;

              @CanIgnoreReturnValue
              public Client setName(String name) {
                this.name = name;
                if (true) {
                  return this;
                }
                return this;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void capitalVoidReturnType() {
    helper
        .addInputLines(
            "Client.java",
            """
            package com.google.frobber;

            public final class Client {
              public Void getValue() {
                return null;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void lowerVoidReturnType() {
    helper
        .addInputLines(
            "Client.java",
            """
            package com.google.frobber;

            public final class Client {
              public Void getValue() {
                return null;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void constructor() {
    helper
        .addInputLines(
            "Client.java",
            """
            package com.google.frobber;

            public final class Client {
              public Client() {}
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void refasterAfterTemplate() {
    helper
        .addInputLines(
            "A.java",
            """
            import com.google.errorprone.refaster.annotation.AfterTemplate;

            class A {
              static final class MethodLacksBeforeTemplateAnnotation {
                @AfterTemplate
                String after(String str) {
                  return str;
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void sometimesThrows() {
    helper
        .addInputLines(
            "Client.java",
            """
            package com.google.frobber;

            public final class Client {
              private String name;

              public Client setName(String name) {
                this.name = name;
                if (true) throw new UnsupportedOperationException();
                return this;
              }
            }
            """)
        .addOutputLines(
            "Client.java",
            """
            package com.google.frobber;

            import com.google.errorprone.annotations.CanIgnoreReturnValue;

            public final class Client {
              private String name;

              @CanIgnoreReturnValue
              public Client setName(String name) {
                this.name = name;
                if (true) throw new UnsupportedOperationException();
                return this;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void alwaysThrows() {
    helper
        .addInputLines(
            "Client.java",
            """
            package com.google.frobber;

            public final class Client {
              private String name;

              public Client setName(String name) {
                throw new UnsupportedOperationException();
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void simpleCaseWithSimpleNameConflict() {
    helper
        .addInputLines(
            "Client.java",
            """
            package com.google.frobber;

            public final class Client {
              private String name;

              public @interface CanIgnoreReturnValue {}

              public Client setName(String name) {
                this.name = name;
                return this;
              }
            }
            """)
        .addOutputLines(
            "Client.java",
            """
            package com.google.frobber;

            public final class Client {
              private String name;

              public @interface CanIgnoreReturnValue {}

              @com.google.errorprone.annotations.CanIgnoreReturnValue
              public Client setName(String name) {
                this.name = name;
                return this;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void onlyReturnsThis_b236423646() {
    helper
        .addInputLines(
            "Client.java",
            """
            package com.google.frobber;

            public final class Client {
              public Client getFoo() {
                return this;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void onlyReturnsSelf_b236423646() {
    helper
        .addInputLines(
            "Client.java",
            """
            package com.google.frobber;

            public final class Client {
              public Client getFoo() {
                return self();
              }

              public Client self() {
                return this;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void delegateToCirvMethod() {
    helper
        .addInputLines(
            "Client.java",
            """
            package com.google.frobber;

            import com.google.errorprone.annotations.CanIgnoreReturnValue;
            import java.util.Arrays;
            import java.util.List;

            public final class Client {
              public Client setFoo(String... args) {
                return setFoo(Arrays.asList(args));
              }

              public Client setFoos(String... args) {
                return this.setFoo(Arrays.asList(args));
              }

              @CanIgnoreReturnValue
              public Client setFoo(List<String> args) {
                return this;
              }
            }
            """)
        .addOutputLines(
            "Client.java",
            """
            package com.google.frobber;

            import com.google.errorprone.annotations.CanIgnoreReturnValue;
            import java.util.Arrays;
            import java.util.List;

            public final class Client {
              @CanIgnoreReturnValue
              public Client setFoo(String... args) {
                return setFoo(Arrays.asList(args));
              }

              @CanIgnoreReturnValue
              public Client setFoos(String... args) {
                return this.setFoo(Arrays.asList(args));
              }

              @CanIgnoreReturnValue
              public Client setFoo(List<String> args) {
                return this;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void converter_b240039465() {
    helper
        .addInputLines(
            "Parent.java",
            """
            package com.google.frobber;

            import com.google.errorprone.annotations.CanIgnoreReturnValue;

            abstract class Parent<X> {
              @CanIgnoreReturnValue
              X doFrom(String in) {
                return from(in);
              }

              abstract X from(String value);
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "public final class Client extends Parent<Integer> {",
            // While doFrom(String) is @CIRV, since it returns Integer, and not Client, we don't add
            // @CIRV here.
            "  public Integer badMethod(String value) {",
            "    return doFrom(value);",
            "  }",
            "  @Override",
            "  public Integer from(String value) {",
            "    return Integer.parseInt(value);",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void immutableBuilder_b265049495() {
    helper
        .addInputLines(
            "Animal.java",
            """
            package com.google.frobber;

            import com.google.auto.value.AutoValue;

            @AutoValue
            abstract class AnimalBuilder {
              abstract String name();

              public AnimalBuilder withName(String name) {
                return builder().setName(name).build();
              }

              static Builder builder() {
                return null;
              }

              @AutoValue.Builder
              abstract static class Builder {
                abstract Builder setName(String value);

                abstract AnimalBuilder build();
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void providesMethod_b267362954() {
    helper
        .addInputLines(
            "Example.java",
            """
            package com.google.frobber;

            public final class Example {
              static CharSequence provideName(String name) {
                return name;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void exemptedByCustomAnnotation() {
    helper
        .addInputLines(
            "Foo.java",
            """
            package example;

            @interface Foo {}
            """)
        .expectUnchanged()
        .addInputLines(
            "ExemptedByCustomAnnotation.java",
            """
            package example;

            public final class ExemptedByCustomAnnotation {
              private String name;

              @Foo
              public ExemptedByCustomAnnotation setName(String name) {
                this.name = name;
                return this;
              }
            }
            """)
        .expectUnchanged()
        .setArgs("-XepOpt:CanIgnoreReturnValue:ExemptingMethodAnnotations=example.Foo")
        .doTest();
  }

  @Test
  public void daggerComponentBuilder_b318407972() {
    helper
        .addInputLines(
            "Builder.java",
            """
            package com.google.frobber;

            import dagger.Component;

            @Component.Builder
            interface Builder {
              Builder setName(String name);

              String build();
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void daggerSubcomponentBuilder_b318407972() {
    helper
        .addInputLines(
            "Builder.java",
            """
            package com.google.frobber;

            import dagger.Subcomponent;

            @Subcomponent.Builder
            interface Builder {
              Builder setName(String name);

              String build();
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void anonymous() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.util.function.Function;

            public final class Test {
              public void setName() {
                var o =
                    new Function<Function, Function>() {
                      @Override
                      public Function apply(Function in) {
                        return in;
                      }
                    };
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void returningANewInstance() {
    helper
        .addInputLines(
            "MyBuilder.java",
            """
            public final class MyBuilder {
              MyBuilder() {}

              MyBuilder(String name) {}

              MyBuilder(MyBuilder builder) {}

              public MyBuilder passingParam(String name) {
                return new MyBuilder(name);
              }

              public MyBuilder passingThis() {
                return new MyBuilder(this);
              }

              public MyBuilder notPassing() {
                return new MyBuilder();
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void providesWithoutChangingType_b267463718() {
    helper
        .addInputLines(
            "ExampleModule.java",
            "package com.google.frobber;",
            "public final class ExampleModule {",
            "  @com.google.inject.Provides",
            "  boolean provideEnableNewRiskAssignment(boolean enableNewRiskAssignment) {",
            "    return enableNewRiskAssignment;",
            "  }",
            "}")
        // we don't fire because @Provides is @Keep
        .expectUnchanged()
        .doTest();
  }
}
