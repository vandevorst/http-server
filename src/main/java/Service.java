import java.util.Arrays;
import java.util.Map;

public class Service {

    public HttpServer.Response handleRequest(HttpServer.Request request) {
//        try {
//            // simulate some processing
//            Thread.sleep(500);
//        } catch (InterruptedException e) {
//            // ignore
//        }
        var args = request.path().split("/");
        if (args.length == 0) {
            return HttpServer.Response.empty();
        }
        return switch (args[1]) {
            case "echo" -> echo(request);
            case "user-agent" -> agent(request);
            default -> HttpServer.Response.notFound();
        };
    }

    private HttpServer.Response echo(HttpServer.Request request) {
        var args = request.path().split("/");
        var message = args.length > 2 ? args[2] : "";
        return new HttpServer.Response(200, message, Map.of(
                "Content-Type", "text/plain",
                "Content-Length", String.valueOf(message.length())));
    }

    private HttpServer.Response agent(HttpServer.Request request) {
        var message = request.headers().get("User-Agent");
        return new HttpServer.Response(200, message, Map.of(
                "Content-Type", "text/plain",
                "Content-Length", String.valueOf(message.length())));
    }
}
