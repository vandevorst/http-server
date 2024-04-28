import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Main {

    private static final int MAX_LINES = 10;

    private static class HttpRequest {
        String method;
        String path;
        String version;
        Map<String, String> headers;
    }

  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");

     try (var serverSocket = new ServerSocket(4221)){
         serverSocket.setReuseAddress(true);
         var clientSocket = serverSocket.accept(); // Wait for connection from client.
         System.out.println("accepted new connection");
         var connection = new HttpConnection(clientSocket);
         connection.handleRequest();
     } catch (IOException e) {
       System.out.println("IOException: " + e.getMessage());
     }
  }
}
