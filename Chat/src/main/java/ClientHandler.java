import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;


public class ClientHandler extends Thread {

    private String clientName = null;
    private BufferedReader inputStream = null;
    private PrintStream outputStream = null;

    private Socket clientSocket = null;
    private final ArrayList<ClientHandler> clientHandlerList;
    private BlockingQueue<PrivateMessage> privateMessage = new ArrayBlockingQueue<PrivateMessage>(5);
    private static List<TopicMessage> publicMessage = new ArrayList<TopicMessage>();
    private Connection senderConnection;
    private Channel senderChannel;

    private static Map<String, Long> clientList = new ConcurrentHashMap<>();


    public ClientHandler(Socket clientSocket, ArrayList<ClientHandler> clientHandlerList) throws IOException, TimeoutException {
        this.clientSocket = clientSocket;
        this.clientHandlerList = clientHandlerList;
        ConnectionFactory producerFactory = new ConnectionFactory();
        producerFactory.setHost("localhost");
        senderConnection = producerFactory.newConnection();
        senderChannel = senderConnection.createChannel();
        senderChannel.exchangeDeclare("my-topic-exchange", BuiltinExchangeType.TOPIC,true);

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
        return"";
    }


    public synchronized void sendMessage(String sender, String receiver,String message){
        PrivateMessage privateMessage = new PrivateMessage(sender, receiver, message);
        this.privateMessage.offer(privateMessage);
    }
    private void createTopic(String header, String message) {
        TopicMessage topicMessage = new TopicMessage(header, message);
        publicMessage.add(topicMessage);
    }
    public void getMessage() {
        PrivateMessage privateMessage = null;
        try {
            privateMessage = this.privateMessage.poll(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(privateMessage != null){
            for (ClientHandler clientHandler : clientHandlerList) {
                if (clientHandler != this && clientHandler.clientName != null && clientHandler.clientName.equals(privateMessage.getReceiver())) {
                    clientHandler.outputStream.println("<" + privateMessage.getSender() + "> " + privateMessage.getMessage());
                    break;
                }
            }
        }
    }
    public void run() {
        ArrayList<ClientHandler> clientHandlerThreads = this.clientHandlerList;
        try {
            inputStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            outputStream = new PrintStream(clientSocket.getOutputStream());
            String name = getClientName();
            clientList.put(name,1L);
            synchronized (this) {
                for (ClientHandler clientHandler : clientHandlerThreads) {
                    if (clientHandler == this) {
                        clientName = "for:" + name;
                        break;
                    }
                }
            }
            while (true) {
                String line = inputStream.readLine();
                publicMessage.removeIf(message -> message.timeoutExpired(System.currentTimeMillis()));

                if (line.startsWith("/exit")) {
                    break;
                } else if (line.startsWith("for:")) {
                    sendPrivateMessage(line);
                } else if(line.startsWith("/createTopic")) {
                    createTopic(line);
                } else if(line.startsWith("/displayTopics")) {
                    displayAvailableTopics();
                }else if(line.startsWith("/displayTopicMessages")) {
                    displayTopicMessage(line);
                }
            }
            inputStream.close();
            outputStream.close();
            clientSocket.close();
        } catch (IOException e) {
        }
    }

    private String getClientName() throws IOException {
        String name;
        while (true) {
            outputStream.println("Name: ");
            name = inputStream.readLine().trim();
            if (name.indexOf("for:") == -1) {
                break;
            } else {
                outputStream.println("The name should not contain for:");
            }
        }
        return name;
    }

    private void sendPrivateMessage(String line) {
        String[] words = line.split("\s", 2);
        if (words.length > 1 && words[1] != null) {
            words[1] = words[1].trim();
            if (!words[1].isEmpty()) {
                if(checkUserConnectivity(words[0])){
                    String[] auxname=this.clientName.split(":",2);
                    sendMessage(auxname[1],words[0],words[1]);
                    getMessage();
                    requestToServer(senderChannel, "RefreshWords->"+words[1],"server");
                }
                else{
                    outputStream.println("The user is offline!!!");
                }
            }
        }
    }
    private synchronized void displayTopicMessage(String[] topic) {
        String topicType = topic[1];
        for(Iterator<TopicMessage> itr = publicMessage.iterator() ; itr.hasNext() ;) {
            TopicMessage msg = itr.next();
            if (topicType.equals(msg.getTopicType())) {
                this.outputStream.println(msg.getMessage());
            }
        }
    }

    private void createTopic(String line) {
        String[] topic = line.split("->", 3);
        synchronized (this) {
            createTopic(topic[1],topic[2]);
            requestToServer(senderChannel, "TopicWords->"+topic[2],"server");
        }
    }

    public boolean checkUserConnectivity(String name){
        boolean isUser = false;
        List<String> clients = new ArrayList(clientList.keySet());
        if(clients.contains(name.replace("for:",""))) {
            isUser = true;
        }
        return isUser;
    }

    private void displayTopicMessage(String line) {
        String[] topic = line.split("->", 2);
        synchronized (this) {
            if (topic.length == 2) {
                displayTopicMessage(topic);
            } else {
                this.outputStream.println("Please specify from which topic you want to read the messages by writing:/displayTopicMessages->NameOfTopic");
                displayAvailableTopics();
            }
        }
    }
    private void displayAvailableTopics() {
        for(TopicMessage msg : publicMessage) {
            this.outputStream.println(msg.getTopicType());
        }
    }
}
