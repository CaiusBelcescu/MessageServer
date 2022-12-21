import java.io.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;

public class Client {
    private Sender sender;
    private Receiver receiver;
    private String user;

    public Client(String user, String desiredTopic) throws IOException, TimeoutException {
        this.user = user;
        this.sender = new Sender();
        this.receiver = new Receiver(user,desiredTopic);
        String result = sendMessage("addUser" + "->" + user, "server","");
        if (result.equals("false"))
            System.out.println("Exists User");
        receiver.consumeMessage();
        receiver.subscribeTopic();
    }

    public String getUser() {
        return user;
    }

    public String sendMessage(String message, String user,String topicType) throws IOException {

        try {
            String result = sender.sendMessage(message, user, getUser(),topicType);

            if (result.equals("not found")) {
                System.out.println("Offline User");
                return result;
            }

        } catch (Exception e) {
            String exceptionMessage = e.getMessage();
            System.out.println(exceptionMessage);
        } finally {
            return "";
        }

    }
}
