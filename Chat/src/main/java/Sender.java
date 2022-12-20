
import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Sender {

    private Connection senderConnection;
    private Channel senderChannel;

    public Sender() throws IOException, TimeoutException {

        ConnectionFactory producerFactory = new ConnectionFactory();
        producerFactory.setHost("localhost");
        senderConnection = producerFactory.newConnection();
        senderChannel = senderConnection.createChannel();
        senderChannel.exchangeDeclare("my-topic-exchange", BuiltinExchangeType.TOPIC,true);
    }


    public String sendMessage(String message, String user, String sender,String topicType){
        switch(user)
                {
                    case "topic":
                        return postTopic(senderChannel, message,topicType);
                    case "server":
                        return requestToServer(senderChannel, message, user);
                    default:
                        if(verifyUserConnected(senderChannel, "CONNECTED?" +
                            "->" + user)) return postMessage(senderChannel, message, user, sender);
                        return "not found";
                }
    }

    public String postMessage(Channel channel, String message, String queue, String sender){
            try
            {
                createQueue(channel, queue);
                String senderAndMessage = sender + "->" + message;
                channel.basicPublish("", queue, false, null, senderAndMessage.getBytes());
                return "";
            }
            catch (IOException e){
                e.printStackTrace();
            }
            return "";
    }


    private void createQueue(Channel channel, String queueName) throws IOException{
            channel.queueDeclare(queueName, false, false, false, null);
    }

    private boolean verifyUserConnected(Channel channel, String user)
        {
            try
            {
                final String corrId = java.util.UUID.randomUUID().toString();
                String replyQueueName = channel.queueDeclare().getQueue();

                AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder().correlationId(corrId).replyTo(replyQueueName).build();
                channel.basicPublish("", "server", properties, user.getBytes());

                final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);

                String ctag = channel.basicConsume(replyQueueName, true, (consumeTag, delivery) -> {
                    if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                        response.offer(new String(delivery.getBody(), StandardCharsets.UTF_8));
                    }
                }, consumeTag -> {
                });

                String result = response.take();
                channel.basicCancel(ctag);

                if(result.equals("true")) return true;
                return false;

            }
            catch (IOException | InterruptedException e){
                e.printStackTrace();
            }
            return false;
        }


    private String requestToServer(Channel channel, String message, String queue)
    {
        try
        {
            final String corrId = java.util.UUID.randomUUID().toString();
            String replyQueue = channel.queueDeclare().getQueue();

            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder().correlationId(corrId).replyTo(replyQueue).build();

            channel.basicPublish("", queue, properties, message.getBytes());

            final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);

            String ctag = channel.basicConsume(replyQueue, true, (consumeTag, delivery) -> {
                if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                    response.offer(new String(delivery.getBody(), StandardCharsets.UTF_8));
                }
            }, consumeTag -> {
            });

            String result = response.take();
            channel.basicCancel(ctag);
            return result;
        }
        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
        return "";
    }

    public String postTopic(Channel channel, String message, String topicType)
    {
        try
        {
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .expiration("10000")
                    .build();
            channel.basicPublish("my-topic-exchange", topicType, null, message.getBytes());
            return "";
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        return "";
    }
}
