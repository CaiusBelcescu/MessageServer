
import Models.ModelTimestamp;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;


public class Server extends Thread{
    private static Map<String, Long> clientList = new ConcurrentHashMap<>();
    private static ModelTimestamp serverTimestamp = new ModelTimestamp();
    private final ScheduledExecutorService serverScheduler = Executors.newScheduledThreadPool(1);

    public void run(){
        final Thread thread = new Thread(() -> {
            try {
                verifyUsersConnectivity();
            } catch (IOException e) {}
        });
        serverScheduler.scheduleAtFixedRate(thread, 0, 5000, TimeUnit.MILLISECONDS);
    }

    //checks the timestamp of all users
    public void verifyUsersConnectivity() throws IOException{
//        List<String> clients = new ArrayList(clientList.keySet());
//
//        for(String client: clients){
//            Iterator<Map.Entry<String, Long>> iterator = clientList.entrySet().iterator();
//            while (iterator.hasNext()){
//                Map.Entry<String, Long> entry = iterator.next();
//                if(client.equals(entry.getKey())) {
//                    long idleTime = (-1 * (entry.getValue() - serverTimestamp.getTime()));
//                    if(idleTime > 5100){
//                        System.out.println("Client " + client + " idle for: " + idleTime/1000 + " seconds");
//                        clientList.remove(client);
//                        iterator.remove();
//                    }
//                    else {
//                        System.out.println("Client " + client + " idle for: " + idleTime/1000 + " seconds");
//                    }
//                }
//            }
//        }
    }

    //add user to clientList
    public static void addUser(String username) {
        List<String> clients = new ArrayList(clientList.keySet());
        if(!clients.contains(username))
        {
            clientList.put(username, serverTimestamp.getTime());
            System.out.println("Added new user! (" + username + ")");
        }
        else System.out.println("User " + username + " exists!" );

    }

    //returns true if user is connected
    public static boolean userConnected(String user){
        List<String> clients = new ArrayList(clientList.keySet());
        if(clients.contains(user)) {
            return true;
        }
        return false;
    }

    //returns true if user is still in the list and sets his timestamp
    public static boolean setUserTimestamp(String user){
        Iterator<Map.Entry<String, Long>> iterator = clientList.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String, Long> entry = iterator.next();
            if(user.equals(entry.getKey())){
                entry.setValue(Long.parseLong(""+serverTimestamp.getTime()));
                return true;
            }
        }
        return false;
    }

    //creates server queue
    public static void task() throws IOException, TimeoutException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");

        Connection serverConnection = connectionFactory.newConnection();
        Channel serverChannel = serverConnection.createChannel();
        serverChannel.queueDeclare("server", false, false, false, null);
        serverChannel.queuePurge("server"); //clear all messages from queue
        serverChannel.basicQos(1);//receives only 1 unacknowledged message at once

        DeliverCallback serverCallback = (consumer, delivery) -> {
            AMQP.BasicProperties replyProprieties = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(delivery.getProperties().getCorrelationId())
                    .build();

            String response = "";
            String[] request = new String(delivery.getBody(), StandardCharsets.UTF_8).split("->");
            switch(request[0]){
                case "addUser":
                    addUser(request[1]);
                    break;
                case "PING":
                    setUserTimestamp(request[1]);
                    break;
                case "CONNECTED?":
                    if(userConnected(request[1]))
                        response = "true";
                    else response = "false";
                    break;
                default:
            }
            serverChannel.basicPublish("", delivery.getProperties().getReplyTo(), replyProprieties, response.getBytes(StandardCharsets.UTF_8));
            serverChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        };
        serverChannel.basicConsume("server",  false, serverCallback, (CONSUMER -> {}));


    }
}