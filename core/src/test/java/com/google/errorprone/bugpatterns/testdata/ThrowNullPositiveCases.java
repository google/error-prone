public class ThrowNullPositiveCases {
  public void foo() {
    // BUG: Diagnostic contains: throw new NullPointerException();
    throw null;
  }
}
