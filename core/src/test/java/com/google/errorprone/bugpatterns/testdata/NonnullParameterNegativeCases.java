import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.ParametersAreNullableByDefault;
import javax.annotation.meta.TypeQualifierNickname;

public class NonnullParameterNegativeCases {
  void foo(Object param) {}

  void foo2(@Nullable Object param) {}

  @ParametersAreNonnullByDefault
  static class Bar {
    static void foo3(@Nullable Object param) {}
    @ParametersAreNullableByDefault
    static void foo4(Object param) {}
  }

  static void foo5(Object... varargs) {}

  @ParametersAreNonnullByDefault
  static void foo6(Void param) {}

  public void invoke() {
    foo(null);
    foo2(null);
    Bar.foo3(null);
    Bar.foo4(null);

    foo5((Object[]) null);
    foo5((Object) null);
    foo5(null, null);
    foo5();

    foo6(null);
  }

  @ParametersAreNonnullByDefault
  @Nonnull
  static void foo7() {}
  
  @Retention(RetentionPolicy.RUNTIME)
  @TypeQualifierNickname
  @Nonnull
  @interface OtherNonnull {}
  
  static void foo8(@Nonnull @OtherNonnull Object param) {}
}
