import com.google.errorprone.bugpatterns.Test.TestFieldProtoMessage;
import com.google.errorprone.bugpatterns.Test.TestProtoMessage;

/**
 * Negative examples for invalid null comparison of a proto message field.
 */
public class InvalidProtoFieldNullComparisonNegativeCases {
  public static void main(String[] args) {
    TestProtoMessage message = TestProtoMessage.newBuilder().build();
    Object object = new Object();
    if (message.getMessage() != object) {
    } else if (object != message.getMessage()) {
    } else if (message.getMessage().getField() != object) {
    } else if (message.getMultiFieldList() != object) {
    } else if (object == message.getMultiFieldList()) {
    }
  }
}
