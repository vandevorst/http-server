import javax.annotation.Nullable;
import java.util.Map;

public record HttpResponse(int status, @Nullable String body, Map<String, String> headers) {
    private static final String OK_RESPONSE = "HTTP/1.1 200 OK\r\n";
    private static final String CREATED_RESPONSE = "HTTP/1.1 201 Created\r\n";
    private static final String NOT_FOUND_RESPONSE = "HTTP/1.1 404 Not Found\r\n";
    private static final String SERVER_ERROR = "HTTP/1.1 500 Internal Server Error\r\n";
    public String constructResponse() {
        return switch (status) {
            case 200 -> {
                var sb = new StringBuilder();
                sb.append(OK_RESPONSE);
                for (var header : headers.entrySet()) {
                    sb.append(String.format("%s: %s\r\n", header.getKey(), header.getValue()));
                }
                sb.append("\r\n");
                sb.append(body);
                yield sb.toString();
            }
            case 201 -> CREATED_RESPONSE + "\r\n";
            case 404 -> NOT_FOUND_RESPONSE + "\r\n";
            default -> SERVER_ERROR + "\r\n";
        };
    }
    public static HttpResponse plainText(String body) {
        return new HttpResponse(200, body, Map.of(
                "Content-Type", "text/plain",
                "Content-Length", String.valueOf(body.length())));
    }
    public static HttpResponse empty() { return new HttpResponse(200, null, Map.of()); }
    public static HttpResponse created() { return new HttpResponse(201, null, Map.of()); }
    public static HttpResponse notFound() { return new HttpResponse(404, null, Map.of()); }
    public static HttpResponse serverError() { return new HttpResponse(500, null, Map.of()); }
}