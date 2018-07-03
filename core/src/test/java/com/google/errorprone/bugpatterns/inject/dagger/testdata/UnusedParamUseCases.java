package com.google.errorprone.bugpatterns.inject.dagger.testdata;

import dagger.Module;
import dagger.Provides;


import java.util.function.DoubleToIntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

public class UnusedParamUseCases {

  // no warning
  private void annotationLessMethod(String unused) {
  }

  // BUG: Diagnostic contains: Unused parameter in method with @Provides annotation.
  @Provides static Integer providesMethodWithUnusedParam(String unused) {
     return new Integer(0);
  }

  @SuppressWarnings("UnusedParam")
  @Provides static Integer providesMethodWithUnusedParamSuppressed(String unusedSuppressed) {
    return new Integer(0);
  }

  @Provides static void providesMethodWithUsedParam1(String used) {
    String x = used;
  }

  @Provides public String bar(
      String usedString1,
      String usedString2,
      String usedString3,
      String usedString4,
      String usedString5,
      String usedString6,
      int usedInt1,
      int usedInt2,
      int usedInt3,
      int usedInt4,
      int usedInt5,
      boolean usedBool1,
      boolean usedBool2,
      boolean usedBool3,
      String [] usedStringArr,
      String usedStringLambda,
      Integer usedInteger,
      UnusedParamUseCases.Test usedTest,
      UnusedParamUseCases.Test usedTestId,
      int usedIntInSwitch,
      int usedIntInLambda) {
    ToIntFunction intFunction = injectedString -> usedIntInLambda;

    if (usedString1.equals("abc")) {
      return usedString2;
    }

    String x = usedString3;
    x = (String) usedString4;
    x = new String(usedString5);

    if (usedBool1) {
      x = usedBool2 ? "a" : "b";
    }

    if (!usedBool3) {
      // do nothing
    }

    if (usedInt1 > 5) {
    }
    int y = usedInt2 < 10 ? usedInt3 : usedInt4;

    for (String s : usedStringArr) {
      y++;
    }

    Supplier<String> z = usedStringLambda::toString;
    x = z.get();

    for (int i = 0; i < usedInt5; i++) {
    }

    Integer a = new Integer(5);
    if (a.equals(usedInteger)) {
      // do nothing
    }

    x = usedTest.s.toString();

    switch (usedIntInSwitch) {
      case 1:
        x = "a";
        break;
      case 2:
        x = "b";
        break;
      default:
        break;
    }
    Object o = usedTestId.new SubTest();

    return usedString6.toString();
  }

  class Test {
    public String s;
    class SubTest {
      public Integer i;
    }
  }
}
