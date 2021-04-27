import com.rabbitmq.rabbittesttool.clients.MessagePayload;
import com.rabbitmq.rabbittesttool.model.ReceivedMessage;

public class TestMessageUtils {
    public static MessagePayload generateMessagePayload(int stream, long sequenceNumber) {
        return new MessagePayload(stream, sequenceNumber, System.nanoTime());
    }

    public static ReceivedMessage generateReceivedMessage(int stream, long sequenceNumber) {
        return generateReceivedMessage(stream, sequenceNumber, false);
    }

    public static ReceivedMessage generateReceivedMessage(MessagePayload mp) {
        return generateReceivedMessage(mp, false);
    }

    public static ReceivedMessage generateReceivedMessage(int stream, long sequenceNumber, boolean redelivered) {
        return generateReceivedMessage(stream, sequenceNumber, redelivered, "c1");
    }

    public static ReceivedMessage generateReceivedMessage(int stream, long sequenceNumber, boolean redelivered, String consumerId) {
        return new ReceivedMessage("c1",
                "vh1",
                "q1",
                new MessagePayload(stream, sequenceNumber, System.nanoTime()),
                redelivered,
                1,
                System.currentTimeMillis()+1);
    }

    public static ReceivedMessage generateReceivedMessage(MessagePayload mp, boolean redelivered) {
        return new ReceivedMessage("c1",
                "vh1",
                "q1",
                mp,
                redelivered,
                1,
                System.currentTimeMillis()+1);
    }
}
