import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;
import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;

/**
 * Positive examples for invalid null comparison of a proto message field.
 */
public class InvalidProtoFieldNullComparisonPositiveCases {
  public static void main(String[] args) {
    TestProtoMessage message = TestProtoMessage.newBuilder().build();
    //BUG: Suggestion includes "message.hasMessage()"
    if (message.getMessage() != null) {
      System.out.println("always true");
    //BUG: Suggestion includes "!message.hasMessage()"
    } else if (message.getMessage() == null) {
      System.out.println("impossible");
    //BUG: Suggestion includes "message.hasMessage()"
    } else if (null != message.getMessage()) {
      System.out.println("always true");
    //BUG: Suggestion includes "message.getMessage().hasField()"
    } else if (message.getMessage().getField() != null) {
      System.out.println("always true");
    //BUG: Suggestion includes "!message.getMultiFieldList().isEmpty()"
    } else if (message.getMultiFieldList() != null) {
      System.out.println("always true");
    //BUG: Suggestion includes "message.getMultiFieldList().isEmpty()"
    } else if (null == message.getMultiFieldList()) {
      System.out.println("impossible");
    }
  }
}
