import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class HttpServer implements Closeable {

    private final ExecutorService executor;
    private final ServerSocket serverSocket;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final RequestHandler requestHandler;

    public static HttpServer create(int concurrency, int port, RequestHandler requestHandler) throws IOException {
        var serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        var executor = Executors.newFixedThreadPool(concurrency);
        return new HttpServer(executor, serverSocket, requestHandler);
    }

    private HttpServer(ExecutorService executor, ServerSocket serverSocket, RequestHandler requestHandler) {
        this.executor = executor;
        this.serverSocket = serverSocket;
        this.requestHandler = requestHandler;
    }

    public void start() {
        while (!shutdown.get()) {
            try {
                var clientSocket = serverSocket.accept(); // Wait for connection from client.
                executor.submit(() -> handleConnection(clientSocket));
            } catch (IOException e) {
                System.out.println("Error accepting new connection: " + e);
            }
        }
    }

    // Handles a request and then closes the socket
    private void handleConnection(Socket socket) throws CompletionException {
        System.out.println("handling request on " + Thread.currentThread().getName());
        HttpRequest req;
        try {
            req = readRequest(socket);
        } catch (IOException e) {
            throw new CompletionException(e);
        }
        System.out.println("read req: " + req);
        var res = requestHandler.handle(req);
        System.out.println("got res: " + res);

        writeResponse(res, socket);
        System.out.println("Written response, closing socket");

        try {
            socket.close();
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }

    private HttpRequest readRequest(Socket socket) throws IOException {
        try {
            var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            return HttpRequest.parseRequest(reader);
        } catch (IOException e) {
            System.out.println("Error reading from input stream: " + Arrays.toString(e.getStackTrace()));
            throw e;
        }
    }

    private void writeResponse(HttpResponse response, Socket socket) {
        try {
            var resp = response.constructResponse();
            socket.getOutputStream().write(resp.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.out.println("Error writing response: " + e);
        }
    }

    @Override
    public void close() throws IOException {
        System.out.println("Shutting down");
        shutdown.set(true);
        executor.shutdown();
        serverSocket.close();
    }
}
