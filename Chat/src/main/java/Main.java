import org.apache.log4j.BasicConfigurator;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;


public class Main {

    public static void main(String[] args) throws IOException, TimeoutException {

        //BasicConfigurator.configure();
        System.out.println("For starting the server type 1,and for creating a client type 2 ");

        Scanner scanner = new Scanner(System.in);
        String aux=scanner.nextLine();
        if(aux.equals("1")){ //this part handles the creation of the server
            Server server = new Server();
            server.start();
            server.task();
        }else if(aux.equals("2")){ // this part handles the creation of the client
            try {
                System.out.println("Please type your username : ");
                Client newClient = null;
                newClient = new Client(scanner.nextLine());
                System.out.println("What do you want to do?");
                System.out.println("For sending a message to a user simply type |the desired username destination|->|Message|");
                System.out.println("For sending a topic message simply type |topic|->|Message|");
                System.out.println("For exit type exit."); //TO DO
                String input=scanner.nextLine();
                while(!input.isEmpty()){
                    switch(input){
                        case "exit":
                            //newClient.exit();
                            System.exit(0);
                            break;
                        default:
                            if(input.contains("->")) {
                                String[] request = input.split("->");
                                newClient.sendMessage(request[1],request[0]);
                            }
                    }
                    input=scanner.nextLine();
                }
            } catch (Exception e) {
                System.out.println("Error when trying to create the client");
                e.printStackTrace();
            }
        }else{
            System.out.println("Please try again. Something went wrong");
        }
    }
}