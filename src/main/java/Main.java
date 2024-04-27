import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Main {
  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");

     try (var serverSocket = new ServerSocket(4221)){
         serverSocket.setReuseAddress(true);
         var clientSocket = serverSocket.accept(); // Wait for connection from client.

//         var inStream = clientSocket.getInputStream();
//         var in = inStream.readAllBytes();

         var outStream = clientSocket.getOutputStream();
         outStream.write(constructOkResponse().getBytes(StandardCharsets.UTF_8));

//         inStream.close();
         outStream.close();

         System.out.println("accepted new connection");
     } catch (IOException e) {
       System.out.println("IOException: " + e.getMessage());
     }
  }

  private static String constructOkResponse() {
      return "HTTP/1.1 200 OK\r\n\r\n";
  }
}
