import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class Service {

    private final String directory;

    public Service(String directory) throws IOException {
        if (!directory.isEmpty() && !directory.equals("/tmp/data/codecrafters.io/http-server-tester/")) {
            throw new IOException("not allowed to use another directory");
        }
        this.directory = directory;
    }

    public HttpServer.Response handleRequest(HttpServer.Request request) {
        var args = request.path().split("/");
        if (args.length == 0) {
            return HttpServer.Response.empty();
        }
        return switch (args[1]) {
            case "echo" -> echo(request);
            case "user-agent" -> agent(request);
            case "files" -> files(request);
            default -> HttpServer.Response.notFound();
        };
    }

    private HttpServer.Response echo(HttpServer.Request request) {
        var args = request.path().split("/");
        var message = args.length > 2 ? args[2] : "";
        return HttpServer.Response.plainText(message);
    }

    private HttpServer.Response agent(HttpServer.Request request) {
        var message = request.headers().get("User-Agent");
        return HttpServer.Response.plainText(message);
    }

    private HttpServer.Response files(HttpServer.Request request) {
        if (directory.isEmpty()) {
            return HttpServer.Response.notFound();
        }
        var args = request.path().split("/");
        var filename = args.length > 2 ? args[2] : "";
        if (filename.isEmpty())  {
            return HttpServer.Response.notFound();
        }
        System.out.println("filename requested: " + filename);
        var fileContents = new StringBuilder();
        try {
            try (var file = Files.newBufferedReader(Path.of(directory + filename))) {
                var nextLine = file.readLine();
                while (nextLine != null) {
                    fileContents.append(nextLine);
                    nextLine = file.readLine();
                }
            }
        } catch (IOException e) {
            // File does not exist
            return HttpServer.Response.notFound();
        }
        var body = fileContents.toString();
        return new HttpServer.Response(200, body, Map.of(
                "Content-Type", "application/octet-stream",
                "Content-Length", String.valueOf(body.length())));
    }
}
