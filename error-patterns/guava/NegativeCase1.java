import com.google.errorprone.matchers.PreconditionsCheckNotNullMatcher;

import java.lang.System;

class NegativeCase1 {
  public void go() {
    Preconditions.checkNotNull("this is ok");
  }
  
  private static class Preconditions {
    static void checkNotNull(String string) {
      System.out.println(string);
    }
  }
}