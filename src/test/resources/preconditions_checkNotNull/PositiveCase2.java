package preconditions_checkNotNull;

/**
 * Test case for fully qualified method call.
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class PositiveCase2 {
  public void error() {
    com.google.common.base.Preconditions.checkNotNull("string literal");
  }
}