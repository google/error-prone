package preconditionschecknotnull;

import com.google.common.base.Preconditions;

public class PositiveCase1 {
  public void error() {
    Preconditions.checkNotNull("string literal");
    String thing = null;
    Preconditions.checkNotNull("thing is null", thing);
    Preconditions.checkNotNull("a string literal " + "that's got two parts", thing);
  }
}