package io.resttestgen.core.openapi;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OpenAPI {

    private final ArrayList<URL> servers;
    private final Set<Operation> operations;

    public OpenAPI () {
        this.servers = new ArrayList<>();
        this.operations = new HashSet<>();
    }

    public void addServer(URL serverURL) {
        this.servers.add(serverURL);
    }

    public List<URL> getServers() {
        return servers;
    }

    public URL getDefaultServer() {
        return servers.get(0);
    }

    public void addOperation(Operation operation) {
        this.operations.add(operation);
    }

    public Set<Operation> getOperations() {
        return this.operations;
    }
}