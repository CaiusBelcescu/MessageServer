import com.rabbitmq.client.*;

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

    public String queueName;

    public String desiredTopic;
    private final ScheduledExecutorService consScheduler = Executors.newScheduledThreadPool(1);

    public Receiver(String username, String desiredTopic) throws IOException, TimeoutException {
        ConnectionFactory consConnectionFactory = new ConnectionFactory();
        consConnectionFactory.setHost("localhost");
        queue = username;
        consConnection = consConnectionFactory.newConnection();
        consChannel = consConnection.createChannel();

        consChannel.exchangeDeclare("my-topic-exchange", BuiltinExchangeType.TOPIC,true);
        queueName = consChannel.queueDeclare().getQueue();

        consChannel.queueBind(queueName, "my-topic-exchange", desiredTopic);

        this.desiredTopic = desiredTopic;
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


                consChannel.basicConsume(queueName, true, ((consumerTag, message) -> {
                    System.out.println("From " + desiredTopic +":"+ new String(message.getBody()));
                }), consumerTag -> {
                    System.out.println(consumerTag);
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            while(true){}
        });

        thread.start();
    }
}
