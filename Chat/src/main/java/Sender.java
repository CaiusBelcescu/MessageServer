
import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Sender {

    private Connection prodConnection;
    private Channel prodChannel;

    public Sender() throws IOException, TimeoutException {

        ConnectionFactory producerFactory = new ConnectionFactory();
        producerFactory.setHost("localhost");

        prodConnection = producerFactory.newConnection();
        prodChannel = prodConnection.createChannel();

        prodChannel.exchangeDeclare("my-topic-exchange", BuiltinExchangeType.TOPIC,true);

    }


    public String sendMessage(String message, String user, String sender,String topicType){

        switch(user)
                {
                    case "topic":
                        return postTopic(prodChannel, message,topicType);
                    case "server":
                        return requestToServer(prodChannel, message, user);
                    default:
                        if(verifyUserConnected(prodChannel, "CONNECTED?" +
                            "->" + user)) return postMessage(prodChannel, message, user, sender);
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

                String ctag = channel.basicConsume(replyQueueName, true, (cTag, delivery) -> {
                    if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                        response.offer(new String(delivery.getBody(), StandardCharsets.UTF_8));
                    }
                }, cTag -> {
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

                    String ctag = channel.basicConsume(replyQueue, true, (cTag, delivery) -> {
                        if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                            response.offer(new String(delivery.getBody(), StandardCharsets.UTF_8));
                        }
                    }, cTag -> {
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



    public void closeConnection() throws IOException {
        prodConnection.close();
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

    public static void subscribeMessage(Channel channel, String message1, String topicType) throws IOException, TimeoutException {


    }
}
