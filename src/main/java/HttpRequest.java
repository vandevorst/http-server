import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {

    private final RequestType requestType;
    private final String path;
    private final String version;
    private final String body;
    private final Map<String, String> headers;
    private final Map<String, String> params;

    private HttpRequest(RequestType requestType, String path, String version, String body, Map<String, String> headers, Map<String, String> params) {
        this.requestType = requestType;
        this.path = path;
        this.version = version;
        this.body = body;
        this.headers = headers;
        this.params = params;
    }

    public enum RequestType {
        GET,
        POST
    }

    public static HttpRequest parseRequest(BufferedReader reader) throws IOException {
        var startLine = reader.readLine();
        if (startLine == null || startLine.isEmpty()) {
            System.out.printf("No content in request, exiting%n");
            throw new IOException("Invalid request format");
        }

        var startLineComponents = startLine.split(" ");
        if (startLineComponents.length < 3) {
            System.out.println("Start line formatted unexpectedly");
            throw new IOException("Invalid request format");
        }

        var requestTypeStr = startLineComponents[0];

        if (!requestTypeStr.equals("GET") && !requestTypeStr.equals("POST")) {
            System.out.printf("Server does not support %s requests", requestTypeStr);
            throw new IOException("Invalid request format");
        }

        var requestType = RequestType.valueOf(requestTypeStr);
        var version = startLineComponents[2];

        var url = startLineComponents[1].split("\\?");
        var path = url[0];
        var params = url.length > 1 ? parseParams(url[1]) : Map.<String, String>of();

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

        var contentLength = headers.get("Content-Length");
        String body = null;
        if (contentLength != null && !contentLength.isEmpty()) {
            var bodySize = Integer.parseInt(contentLength);
            var bodyArray = new char[bodySize];
            for (int i = 0; i < bodySize; i++) {
                var nextChar = reader.read();
                if (nextChar == -1) {
                    // EOF
                    break;
                }
                bodyArray[i] = (char) nextChar;
            }
            body = String.valueOf(bodyArray);
            System.out.println("Parsed body value: " + body);
        }
        return new HttpRequest(requestType, path, version, body, headers, params);
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public String getPath() {
        return path;
    }

    public String getVersion() {
        return version;
    }

    @Nullable
    public String getBody() {
        return body;
    }

    @Nullable
    public String getHeader(String name) {
        return headers.get(name);
    }

    @Nullable
    public String getParam(String name) {
        return params.get(name);
    }

    private static Map<String, String> parseParams(String params) {
        var paramMap = new HashMap<String, String>();
        for (var param : params.split("&")) {
            var kv = param.split("=");
            if (kv.length != 2) {
                continue;
            }
            paramMap.put(kv[0], kv[1]);
        }
        return paramMap;
    }
}