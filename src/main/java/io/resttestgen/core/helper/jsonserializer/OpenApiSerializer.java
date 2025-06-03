package io.resttestgen.core.helper.jsonserializer;

import com.google.gson.*;
import io.resttestgen.core.openapi.OpenApi;
import io.resttestgen.core.openapi.Operation;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Used by Gson to write out the Java representation of an OpenAPI, as JSON OpenAPI specification.
 */
public class OpenApiSerializer implements JsonSerializer<OpenApi> {

    @Override
    public JsonElement serialize(OpenApi src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Operation.class, new OperationSerializer())
                .setPrettyPrinting()
                .create();
        result.add("openapi", gson.toJsonTree("3.0.1"));
        result.add("info", gson.toJsonTree(new Info(src)));
        result.add("servers", gson.toJsonTree(getServers(src)));
        result.add("paths", gson.toJsonTree(getPaths(src)));
        return result;
    }

    private List<Server> getServers(OpenApi openAPI) {
        List<Server> servers = new LinkedList<>();

        for (URL url : openAPI.getServers()) {
            servers.add(new Server(url.toString()));
        }

        return servers;
    }

    private Map<String, Path> getPaths(OpenApi openAPI) {
        Map<String, Path> pathMap = new HashMap<>();

        // Initialize map
        for (Operation operation : openAPI.getOperations()) {
            pathMap.computeIfAbsent(operation.getEndpoint(), k -> new Path(operation.getEndpoint()));
            Path path = pathMap.get(operation.getEndpoint());
            switch (operation.getMethod()) {
                case GET:
                    path.setGet(operation);
                    break;
                case POST:
                    path.setPost(operation);
                    break;
                case PUT:
                    path.setPut(operation);
                    break;
                case PATCH:
                    path.setPatch(operation);
                    break;
                case DELETE:
                    path.setDelete(operation);
            }
        }

        return pathMap;
    }

    private static class Info {

        private final String title;
        private final String summary;
        private final String description;
        private final String termsOfService;
        private final Contact contact;
        private final License license;
        private final String version;

        public Info(OpenApi openAPI) {
            this.title = openAPI.getTitle().isEmpty() ? null : openAPI.getTitle();
            this.summary = openAPI.getSummary().isEmpty() ? null : openAPI.getSummary();
            this.description = openAPI.getDescription().isEmpty() ? null : openAPI.getDescription();
            this.termsOfService = openAPI.getTermsOfService().isEmpty() ? null : openAPI.getTermsOfService();
            if (!openAPI.getContactName().isEmpty() || !openAPI.getContactUrl().isEmpty() || !openAPI.getContactEmail().isEmpty()) {
                this.contact = new Contact(openAPI);
            } else {
                this.contact = null;
            }
            if (!openAPI.getLicenseName().isEmpty() || !openAPI.getLicenseUrl().isEmpty()) {
                this.license = new License(openAPI);
            } else {
                this.license = null;
            }
            this.version = openAPI.getVersion().isEmpty() ? null : openAPI.getVersion();
        }
    }

    private static class Contact {

        private String name;
        private String url;
        private String email;

        public Contact(OpenApi openAPI) {
            this.name = openAPI.getContactName().isEmpty() ? null : openAPI.getContactName();
            this.url = openAPI.getContactUrl().isEmpty() ? null : openAPI.getContactUrl();
            this.email = openAPI.getContactEmail().isEmpty() ? null : openAPI.getContactEmail();
        }
    }

    private static class License {

        private String name;
        private String url;

        public License(OpenApi openAPI) {
            this.name = openAPI.getLicenseName().isEmpty() ? null : openAPI.getLicenseName();
            this.url = openAPI.getLicenseUrl().isEmpty() ? null : openAPI.getLicenseUrl();
        }
    }

    private static class Server {

        private final String url;

        public Server(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    }

    private static class Path {

        private transient final String path;

        private Operation get;
        private Operation post;
        private Operation put;
        private Operation patch;
        private Operation delete;

        public Path(String path) {
            this.path = path;
            get = null;
            post = null;
            put = null;
            patch = null;
            delete = null;
        }

        public String getPath() {
            return path;
        }

        public Operation getGet() {
            return get;
        }

        public void setGet(Operation get) {
            this.get = get;
        }

        public Operation getPost() {
            return post;
        }

        public void setPost(Operation post) {
            this.post = post;
        }

        public Operation getPut() {
            return put;
        }

        public void setPut(Operation put) {
            this.put = put;
        }

        public Operation getPatch() {
            return patch;
        }

        public void setPatch(Operation patch) {
            this.patch = patch;
        }

        public Operation getDelete() {
            return delete;
        }

        public void setDelete(Operation delete) {
            this.delete = delete;
        }
    }
}
