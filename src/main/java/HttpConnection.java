import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class HttpConnection {

    private static final String OK_RESPONSE = "HTTP/1.1 200 OK\r\n\r\n";
    private static final String NOT_FOUND_RESPONSE = "HTTP/1.1 404 Not Found\r\n\r\n";

    private final Socket socket;

    public HttpConnection(Socket socket) {
        this.socket = socket;
    }

    public void handleRequest() throws IOException {
        var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
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

        var path = startLineComponents[1];
        String outputMessage;
        if (path.equals("/")) {
            outputMessage = OK_RESPONSE;
        } else {
            outputMessage = NOT_FOUND_RESPONSE;
        }
        socket.getOutputStream().write(outputMessage.getBytes(StandardCharsets.UTF_8));
    }

}
