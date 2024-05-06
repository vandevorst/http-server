import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class CCRequestHandler implements RequestHandler {
    private final String directory;

    public CCRequestHandler(String directory) throws IOException {
        if (!directory.isEmpty() && !directory.equals("/tmp/data/codecrafters.io/http-server-tester/")) {
            throw new IOException("not allowed to use another directory");
        }
        this.directory = directory;
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        var args = request.getPath().split("/");
        if (args.length == 0) {
            return HttpResponse.empty();
        }
        return switch (args[1]) {
            case "echo" -> echo(request);
            case "user-agent" -> agent(request);
            case "files" -> {
                if (request.getRequestType() == HttpRequest.RequestType.GET) {
                    yield getFile(request);
                } else {
                    yield saveFile(request);
                }
            }
            default -> HttpResponse.notFound();
        };
    }

    private HttpResponse echo(HttpRequest request) {
        var args = request.getPath().split("/");
        var message = args.length > 2 ? args[2] : "";
        return HttpResponse.plainText(message);
    }

    private HttpResponse agent(HttpRequest request) {
        var message = request.getHeader("User-Agent");
        return HttpResponse.plainText(message);
    }

    private HttpResponse getFile(HttpRequest request) {
        if (directory.isEmpty()) {
            return HttpResponse.notFound();
        }
        var args = request.getPath().split("/");
        var filename = args.length > 2 ? args[2] : "";
        if (filename.isEmpty())  {
            return HttpResponse.notFound();
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
            return HttpResponse.notFound();
        }
        var body = fileContents.toString();
        return new HttpResponse(200, body, Map.of(
                "Content-Type", "application/octet-stream",
                "Content-Length", String.valueOf(body.length())));
    }

    private HttpResponse saveFile(HttpRequest request) {
        if (directory.isEmpty()) {
            return HttpResponse.notFound();
        }
        var args = request.getPath().split("/");
        var filename = args.length > 2 ? args[2] : "";
        if (filename.isEmpty())  {
            return HttpResponse.notFound();
        }
        if (request.getBody() == null || request.getBody().isEmpty()) {
            return HttpResponse.notFound();
        }
        try {
            var filePath = Path.of(directory + filename);
            Files.deleteIfExists(filePath);
            Files.createFile(filePath);
            try (var writer = Files.newBufferedWriter(filePath)) {
                writer.write(request.getBody());
                writer.flush();
            }
        } catch (IOException e) {
            // File does not exist
            return HttpResponse.notFound();
        }
        return HttpResponse.created();
    }
}
