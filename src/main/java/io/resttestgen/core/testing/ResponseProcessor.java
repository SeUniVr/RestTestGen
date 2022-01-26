package io.resttestgen.core.testing;

import okhttp3.Response;

public interface ResponseProcessor {

    public void process(Response response, String responseBody);
}
