import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class HttpServer implements Closeable {

    public record Request(String requestType, String path, String version, Map<String, String> headers) {}
    public record Response(int status, @Nullable String body, Map<String, String> headers) {
        public static Response plainText(String body) {
            return new Response(200, body, Map.of(
                    "Content-Type", "text/plain",
                    "Content-Length", String.valueOf(body.length())));
        }
        public static Response empty() { return new Response(200, null, Map.of()); }
        public static Response notFound() { return new Response(404, null, Map.of()); }
        public static Response serverError() { return new Response(500, null, Map.of()); }
    }

    private static final String OK_RESPONSE = "HTTP/1.1 200 OK\r\n";
    private static final String NOT_FOUND_RESPONSE = "HTTP/1.1 404 Not Found\r\n";
    private static final String SERVER_ERROR = "HTTP/1.1 500 Internal Server Error\r\n";

    private final ExecutorService executor;
    private final ServerSocket serverSocket;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final Service service;

    public static HttpServer create(int concurrency, int port, Service service) throws IOException {
        var serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        var executor = Executors.newFixedThreadPool(concurrency);
        return new HttpServer(executor, serverSocket, service);
    }

    private HttpServer(ExecutorService executor, ServerSocket serverSocket, Service service) {
        this.executor = executor;
        this.serverSocket = serverSocket;
        this.service = service;
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
        Request req = null;
        try {
            req = readRequest(socket);
        } catch (IOException e) {
            throw new CompletionException(e);
        }
        System.out.println("read req: " + req);
        var res = service.handleRequest(req);
        System.out.println("got res: " + res);

        writeResponse(res, socket);
        System.out.println("Written response, closing socket");

        try {
            socket.close();
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }

    private Request readRequest(Socket socket) throws IOException {
        try {
            var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            return parseRequest(reader);
        } catch (IOException e) {
            System.out.println("Error reading from input stream: " + Arrays.toString(e.getStackTrace()));
            throw e;
        }
    }

    private void writeResponse(Response response, Socket socket) {
        try {
            var resp = constructResponse(response);
            socket.getOutputStream().write(resp.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.out.println("Error writing response: " + e);
        }
    }

    private static String constructResponse(Response response) {
        return switch (response.status) {
            case 200 -> {
                var sb = new StringBuilder();
                sb.append(OK_RESPONSE);
                for (var header : response.headers.entrySet()) {
                    sb.append(String.format("%s: %s\r\n", header.getKey(), header.getValue()));
                }
                sb.append("\r\n");
                sb.append(response.body);
                yield sb.toString();
            }
            case 404 -> NOT_FOUND_RESPONSE + "\r\n";
            default -> SERVER_ERROR + "\r\n";
        };
    }

    private static Request parseRequest(BufferedReader reader) throws IOException {
        var startLine = reader.readLine();
        if (startLine == null || startLine.isEmpty()) {
            System.out.printf("No content in request, exiting%n");
            throw new IOException("Invalid request format");
        }

        if (!startLine.startsWith("GET")) {
            System.out.println("Expected GET request, got another form of request");
            throw new IOException("Invalid request format");
        }

        var startLineComponents = startLine.split(" ");
        if (startLineComponents.length < 3) {
            System.out.println("Start line formatted unexpectedly");
            throw new IOException("Invalid request format");
        }

        var requestType = startLineComponents[0];
        var path = startLineComponents[1];
        var version = startLineComponents[2];

        var headers = new HashMap<String, String>();
        var nextLine = reader.readLine();
        while (nextLine != null && !nextLine.isEmpty() && !nextLine.isBlank()) {
            System.out.println("Reading line " + nextLine);
            var header = nextLine.split(": ");
            if (header.length != 2) {
                System.out.printf("Header %s formatted incorrectly, skipping%n", nextLine);
                continue;
            }
            headers.put(header[0], header[1]);
            nextLine = reader.readLine();
        }

        //parse body

        return new Request(requestType, path, version, headers);
    }

    @Override
    public void close() throws IOException {
        System.out.println("Shutting down");
        shutdown.set(true);
        executor.shutdown();
        serverSocket.close();
    }
}
