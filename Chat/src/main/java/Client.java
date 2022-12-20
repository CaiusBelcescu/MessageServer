import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Client {
    private Sender producer;
    private Receiver consumer;
    private String user;
    private final ScheduledExecutorService clientScheduler = Executors.newScheduledThreadPool(1);

    public Client(String user) throws IOException, TimeoutException {
        this.user = user;
        this.producer = new Sender();
        this.consumer = new Receiver(user);
        String result = sendMessage("addUser" + "->" + user, "server","");
        if (result.equals("false"))
            System.out.println("Exists User");

        ping(); //server knows the status
        consumer.consumeMessage();
        consumer.subscribeTopic();
    }


    public String getUser() {
        return user;
    }

    public void exit() throws IOException {
        this.producer.closeConnection();
        this.consumer.closeConsConnection();
    }

    public String sendMessage(String message, String user,String topicType) throws IOException, TimeoutException {

        try {
            String result = producer.sendMessage(message, user, getUser(),topicType);

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

    public void ping() {

        final Runnable runnable = () -> {
            try {
                sendMessage("PING" + "->" + user, "server","");
            } catch (IOException | TimeoutException e) {
                e.printStackTrace();
            }
        };

        //sends ping
        clientScheduler.scheduleAtFixedRate(runnable, 0, 5000, TimeUnit.MILLISECONDS);
    }
}
