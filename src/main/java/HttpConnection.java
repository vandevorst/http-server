import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class HttpConnection {

    private static final String OK_RESPONSE = "HTTP/1.1 200 OK\r\n";
    private static final String NOT_FOUND_RESPONSE = "HTTP/1.1 404 Not Found\r\n";

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
        var outputMessage = parsePath(path);

        socket.getOutputStream().write(outputMessage.getBytes(StandardCharsets.UTF_8));
    }

    private String parsePath(String path) {
        var args = path.split("/");
        var outputMessage = new StringBuilder();
        if (args.length == 0) {
            outputMessage.append(OK_RESPONSE);
            outputMessage.append("\r\n");
        } else if (args[1].equals("echo")) {
            outputMessage.append(OK_RESPONSE);
            outputMessage.append("Content-Type: text/plain\r\n");
            var message = args.length > 2 ? args[2] : "";
            outputMessage.append(String.format("Content-Length: %d\r\n", message.length()));
            outputMessage.append("\r\n");
            outputMessage.append(message);
        } else {
            outputMessage.append(NOT_FOUND_RESPONSE);
            outputMessage.append("\r\n");
        }
        return outputMessage.toString();
    }

}
