import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.ParametersAreNullableByDefault;
import javax.annotation.meta.TypeQualifierNickname;

public class NonnullParameterPositiveCases {
  void foo(@Nonnull Object param) {}

  @ParametersAreNonnullByDefault
  void foo2(Object param) {}

  @ParametersAreNonnullByDefault
  static class Bar {
    static void foo3(Object param) {}
    @ParametersAreNullableByDefault
    static void foo4(@Nonnull Object param) {}
  }

  static void foo5(@Nonnull Object... varargs) {}

  public void invoke() {
    // BUG: Diagnostic contains: null value
    // 0
    foo(null);
    // BUG: Diagnostic contains: null value
    foo2(null);
    // BUG: Diagnostic contains: null value
    Bar.foo3(null);
    // BUG: Diagnostic contains: null value
    Bar.foo4(null);

    // BUG: Diagnostic contains: null value
    foo5((Object[]) null);
    // BUG: Diagnostic contains: null value
    foo5((Object) null);
    // BUG: Diagnostic contains: null value
    // 0, 1
    foo5(null, null);
    // BUG: Diagnostic contains: null value
    // 4
    foo5(new Object(), new Object(), new Object(), new Object(), null);
  }

  // BUG: Diagnostic contains: More than one
  // Nonnull
  // Nullable
  // parameter
  static void foo6(@Nonnull @Nullable Object param) {}

  @ParametersAreNonnullByDefault
  @ParametersAreNullableByDefault
  // BUG: Diagnostic contains: More than one
  // ParametersAreNonnullByDefault
  // ParametersAreNullableByDefault
  // method
  static void foo7(Object param) {}

  // BUG: Diagnostic contains: cannot be applied to Void
  static void foo8(@Nonnull Void param) {}

  @ParametersAreNonnullByDefault
  @ParametersAreNullableByDefault
  // BUG: Diagnostic contains: More than one
  // ParametersAreNonnullByDefault
  // ParametersAreNullableByDefault
  // class
  static class Bar2 {}

  @Retention(RetentionPolicy.RUNTIME)
  @TypeQualifierNickname
  @Nonnull
  @interface OtherNonnull {}

  // BUG: Diagnostic contains: OtherNonnull
  // cannot be applied to Void
  static void foo9(@OtherNonnull Void param) {}
  
  @Nullable
  @OtherNonnull
  // BUG: Diagnostic contains: More than one
  // Nonnull
  // OtherNonnull
  static void foo10(Object param) {}
  
  @Retention(RetentionPolicy.RUNTIME)
  @TypeQualifierNickname
  @OtherNonnull
  @interface SuperIndirectNonnull {}
  
  // BUG: Diagnostic contains: SuperIndirectNonnull
  // cannot be applied to Void
  static void foo11(@SuperIndirectNonnull Void param) {}
}
