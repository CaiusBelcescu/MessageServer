import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;


public class Main {

    public static void main(String[] args) throws IOException, TimeoutException {

        System.out.println("For starting the server type 1,and for creating a client type 2 ");
        Scanner scanner = new Scanner(System.in);
        String aux=scanner.nextLine();
        if(aux.equals("1")){ //this part handles the creation of the server
            Server server =  new Server();
            server.StartServer();
        }else if(aux.equals("2")){ // this part handles the creation of the client
            System.out.println("For sending a private message to another user type for:UserName");
            System.out.println("For sending a message into a topic write /createTopic->NameOfTheTopic-TimeForTheMessage->Message");
            System.out.println("For displaying the available topic write /displayTopics");
            System.out.println("For displaying the messages from a topic write /displayTopicMessages->NameOfTopic");
            ClientConnection client = new ClientConnection();
            client.connectClient();
        }else{
            System.out.println("Please try again. Something went wrong");
        }
    }
}