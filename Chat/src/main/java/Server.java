
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
    private static Map<String, Integer> wordList = new ConcurrentHashMap<>();
    private static Map<String, Integer> wordListTopic = new ConcurrentHashMap<>();

    public void run(){
        final Thread thread = new Thread(() -> {
            try {
                  checkWordFrequencyDirectMessages();
                  checkWordFrequencyTopics();
            } catch (IOException e) {}
        });
        serverScheduler.scheduleAtFixedRate(thread, 0, 10000, TimeUnit.MILLISECONDS);
    }

    public static void checkWordFrequencyDirectMessages() throws IOException{
        List<String> words = new ArrayList<>(wordList.keySet());
        Integer max = 0;
        String key="";
        for(Map.Entry<String, Integer> word: wordList.entrySet())
        {
            if(max < word.getValue()){
                key = word.getKey();
                max= word.getValue();
            }
        }
        System.out.println("The most used word is:" + key);
    }


    public static void checkWordFrequencyTopics() throws IOException{
        List<String> words = new ArrayList<>(wordListTopic.keySet());
        Integer max = 0;
        String key="";
        for(Map.Entry<String, Integer> word: wordListTopic.entrySet())
        {
            if(max < word.getValue()){
                key = word.getKey();
                max= word.getValue();
            }
        }
        System.out.println("The most used word in a topic is:" + key);
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
    public static boolean isUserConnected(String user){
        List<String> clients = new ArrayList(clientList.keySet());

        if(clients.contains(user)) {
            return true;
        }

        return false;
    }

    public static void refreshWords(String message){
        String[] words = message.split(" ");

        for(String word:words){
            if(!wordList.containsKey(word)){
                wordList.put(word, 1);
            }else{
                wordList.put(word,wordList.get(word) + 1);
            }
        }
    }

    public static void topicWords(String message){
        String[] words = message.split(" ");

        for(String word:words){
            if(!wordListTopic.containsKey(word)){
                wordListTopic.put(word, 1);
            }else{
                wordListTopic.put(word,wordListTopic.get(word) + 1);
            }
        }
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

        serverChannel.basicQos(1);

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
                    if(isUserConnected(request[1]))
                        response = "true";
                    else response = "false";
                    break;
                case "RefreshWords":
                    refreshWords(request[1]);
                    break;
                case "TopicWords":
                    topicWords(request[1]);
                default:
            }

            serverChannel.basicPublish("", delivery.getProperties().getReplyTo(), replyProprieties, response.getBytes(StandardCharsets.UTF_8));
            serverChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        };

        serverChannel.basicConsume("server",  false, serverCallback, (CONSUMER -> {}));


    }
}