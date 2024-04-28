import java.io.*;
import java.net.ServerSocket;

public class Main {

  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");

     try (var serverSocket = new ServerSocket(4221)){
         serverSocket.setReuseAddress(true);
         try (var clientSocket = serverSocket.accept()) { // Wait for connection from client.
             System.out.println("accepted new connection");
             var connection = new HttpConnection(clientSocket);
             connection.handleRequest();
         }
     } catch (IOException e) {
       System.out.println("IOException: " + e.getMessage());
     }
  }
}
