
import com.rabbitmq.client.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

public class Server {
    private static ServerSocket serverSocket = null;
    private static Socket clientSocket = null;
    private static final ArrayList<ClientHandler> CLIENT_HANDLER_LIST = new ArrayList<ClientHandler>();
    private static Map<String, Integer> wordList = new ConcurrentHashMap<>();
    private static Map<String, Integer> wordListTopic = new ConcurrentHashMap<>();

    public static void refreshWords(String message) throws IOException {
        String[] words = message.split(" ");
        for(String word:words){
            if(!wordList.containsKey(word)){
                wordList.put(word, 1);
            }else{
                wordList.put(word,wordList.get(word) + 1);
            }
        }
        checkWordFrequencyDirectMessages();
    }

    public static void topicWords(String message) throws IOException {
        String[] words = message.split(" ");
        for(String word:words) {
            if (!wordListTopic.containsKey(word)) {
                wordListTopic.put(word, 1);
            } else {
                wordListTopic.put(word, wordListTopic.get(word) + 1);
            }
        }
        checkWordFrequencyTopics();
    }


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
        System.out.println("What is trending right now in direct messages: " +"#"+ key);
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
        System.out.println("What is trending right now in topics: "+ "#"+key);
    }
    public static void StartServer() throws IOException, TimeoutException {
        Server.task();
        int port = 8080;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println(e);
        }
        while (true) {
            try {

                clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, CLIENT_HANDLER_LIST);
                clientHandler.start();
                CLIENT_HANDLER_LIST.add(clientHandler);
                System.out.println("Client added");
            } catch (IOException | TimeoutException e) {
                System.out.println(e);
            }
        }
    }
}
