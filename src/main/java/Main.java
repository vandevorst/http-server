import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    private static final int CONCURRENCY = 10;
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(CONCURRENCY + 1);


  public static void main(String[] args) throws InterruptedException {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");
     try {
         var httpServer = HttpServer.create(CONCURRENCY, 4221);
         EXECUTOR_SERVICE.submit(httpServer::start);
     } catch (IOException e) {
       System.out.println("IOException: " + e.getMessage());
     }
  }

  private void test(int concurrency) {
      var testClient = HttpClient.newHttpClient();
      var testReqs = List.of(
              "http://localhost:4221/",
              "http://localhost:4221/echo/abc",
              "http://localhost:4221/user-agent",
              "http://localhost:4221/abc"
      );
      for (var i = 0; i < concurrency; i++) {
          EXECUTOR_SERVICE.submit(() -> testClient.send(HttpRequest.newBuilder(URI.create(testReqs.get(concurrency % testReqs.size()))).GET().build(),
                  resp -> HttpResponse.BodyHandlers.ofString().apply(resp)));
      }
  }
}
