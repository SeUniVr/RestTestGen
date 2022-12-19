package io.resttestgen.core.openapi;

import com.google.gson.GsonBuilder;
import io.resttestgen.core.Configuration;
import io.resttestgen.core.Environment;
import io.resttestgen.core.helper.jsonserializer.OpenApiSerializer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OpenAPI {

    private String title;
    private String summary;
    private String description;
    private String termsOfService;
    private String contactName;
    private String contactUrl;
    private String contactEmail;
    private String licenseName;
    private String licenseUrl;
    private String version;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTermsOfService() {
        return termsOfService;
    }

    public void setTermsOfService(String termsOfService) {
        this.termsOfService = termsOfService;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactUrl() {
        return contactUrl;
    }

    public void setContactUrl(String contactUrl) {
        this.contactUrl = contactUrl;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getLicenseName() {
        return licenseName;
    }

    public void setLicenseName(String licenseName) {
        this.licenseName = licenseName;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Export the specification to file.
     * @param filename the file name for the specification.
     * @throws IOException in case file IO fails.
     */
    public void exportAsJsonOpenApiSpecification(String filename) throws IOException {

        Configuration configuration = Environment.getInstance().getConfiguration();

        File file = new File(configuration.getOutputPath() + configuration.getTestingSessionName() + "/");

        file.mkdirs();

        FileWriter writer = new FileWriter(configuration.getOutputPath() + configuration.getTestingSessionName() + "/" + (filename.endsWith(".json") ? filename : filename + ".json"));

        // Write to file with Gson, using custom serializer
        new GsonBuilder()
                .registerTypeAdapter(OpenAPI.class, new OpenApiSerializer())
                .setPrettyPrinting()
                .create()
                .toJson(this, writer);

        // Close the writer
        writer.close();
    }
}