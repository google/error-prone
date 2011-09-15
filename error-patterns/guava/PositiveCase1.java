import com.google.common.base.Preconditions;

public class PositiveCase1 {
  public void error() {
    Preconditions.checkNotNull("string literal");
  }
}