import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class Receiver {

    private String queue;
    private Channel consChannel;
    private Connection consConnection;
    private final ScheduledExecutorService consScheduler = Executors.newScheduledThreadPool(1);

    public Receiver(String username) throws IOException, TimeoutException {
        ConnectionFactory consConnectionFactory = new ConnectionFactory();
        consConnectionFactory.setHost("localhost");
        queue = username;
        consConnection = consConnectionFactory.newConnection();
        consChannel = consConnection.createChannel();
    }

    public void closeConsConnection() throws IOException {
        consConnection.close();
    }

    public void consumeMessage()
    {
        final Runnable runnable = () -> {
            try {
                consChannel.queueDeclare(queue, false, false, false, null);

                consChannel.basicConsume(queue, true, (consLbl, message) -> {
                    String receivedMessage = new String(message.getBody(), StandardCharsets.UTF_8);
                    System.out.println("Received: "+receivedMessage);
                }, consLbl -> {});
            } catch (IOException e) {
                e.printStackTrace();
            }

        };
        consScheduler.scheduleAtFixedRate(runnable, 0, 500, TimeUnit.MILLISECONDS);
    }

    public void subscribeTopic()
    {
        Thread thread = new Thread(() -> {
            try {
                consChannel.exchangeDeclare("topicQueue", "fanout");

                String queue = consChannel.queueDeclare().getQueue();

                consChannel.queueBind(queue, "topicQueue", "");

                DeliverCallback deliverCallback = (consLbl, deliveryMessage) -> {
                    String receivedMessage = new String(deliveryMessage.getBody(), "UTF-8");
                    System.out.println("Topic received: " + receivedMessage);
                };

                consChannel.basicConsume(queue, true, deliverCallback, consLbl -> { });
            } catch (IOException e) {
                e.printStackTrace();
            }

            while(true){}
        });

        thread.start();
    }
}
