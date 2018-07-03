package com.google.errorprone.bugpatterns.inject.dagger.testdata;

import dagger.Module;
import dagger.Provides;

import javax.inject.Inject;

import java.util.function.DoubleToIntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

public class UnusedInjectUseCases {

  @Inject String injectedString;

 // BUG: Diagnostic contains: Unused field with @Inject annotation. 
  @Inject String unusedString;

  // BUG: Diagnostic contains: Unused field with @Inject annotation.
  @Inject int unusedInt;

  @SuppressWarnings("UnusedInject")
  @Inject int suppressedUnusedInt;

  @Inject String usedString1;
  @Inject String usedString2;
  @Inject String usedString3;
  @Inject String usedString4;
  @Inject String usedString5;
  @Inject String usedString6;

  @Inject int usedInt1;
  @Inject int usedInt2;
  @Inject int usedInt3;
  @Inject int usedInt4;
  @Inject int usedInt5;

  @Inject boolean usedBool1;
  @Inject boolean usedBool2;
  @Inject boolean usedBool3;

  @Inject String [] usedStringArr;

  @Inject String usedStringLambda;

  @Inject Integer usedInteger;

  @Inject Test usedTest;
  class Test {
    public String s;
  }

  @Inject int usedIntInSwitch;

  @Inject int usedIntInLambda;

  String do_not_report_any_error;

  public String bar() {

    ToIntFunction intFunction = injectedString -> usedIntInLambda;

    System.out.println(injectedString);
    if (usedString1.equals("abc")) {
      return usedString2;
    }

    String x = usedString3;
    x = (String) usedString4;
    x = new String(usedString5);

    if(usedBool1) {
      x = usedBool2 ? "a" : "b";
    }

    if(!usedBool3) {
      // do nothing
    }

    if (usedInt1 > 5) { }
    int y = usedInt2 < 10 ? usedInt3 : usedInt4;

    for(String s : usedStringArr) {
       y++;
    }

    Supplier<String> z = usedStringLambda::toString;
    x = z.get();

    for (int i = 0; i < usedInt5; i++) { }

    Integer a = new Integer(5);
    if(a.equals(this.usedInteger)) {
      // do nothing
    }

    x = usedTest.s.toString();

    switch(usedIntInSwitch) {
      case 1: x = "a"; break;
      case 2: x = "b"; break;
      default: break;
    }

    return usedString6.toString();
  }

}
