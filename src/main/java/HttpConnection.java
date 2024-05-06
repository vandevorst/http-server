import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class HttpConnection {

    private record Request(String requestType, String path, String version, Map<String, String> headers) {}

    private static final String OK_RESPONSE = "HTTP/1.1 200 OK\r\n";
    private static final String NOT_FOUND_RESPONSE = "HTTP/1.1 404 Not Found\r\n";

    private final Socket socket;

    public HttpConnection(Socket socket) {
        this.socket = socket;
    }

    public void handleRequest() throws IOException {
        var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        var req = readRequest(reader);
        var resp = buildResponse(req);
        socket.getOutputStream().write(resp.getBytes(StandardCharsets.UTF_8));
    }


    private static Request readRequest(BufferedReader reader) throws IOException {
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

    private String buildResponse(Request request) {
        var args = request.path.split("/");
        if (args.length == 0) {
            return String.format("%s\r\n", OK_RESPONSE);
        }

        return switch (args[1]) {
            case "echo" -> echo(request);
            case "user-agent" -> agent(request);
            default -> String.format("%s\r\n", NOT_FOUND_RESPONSE);
        };
    }

    private String echo(Request request) {
        var args = request.path.split("/");
        var message = args.length > 2 ? args[2] : "";
        return formatOkResponse(message);
    }

    private String agent(Request request) {
        var message = request.headers.get("User-Agent");
        return formatOkResponse(message);
    }

    private String formatOkResponse(String body) {
        return OK_RESPONSE +
                "Content-Type: text/plain\r\n" +
                String.format("Content-Length: %d\r\n", body.length()) +
                "\r\n" +
                body;
    }
}
