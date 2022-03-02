package io.resttestgen.core;

import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.ParameterLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class AuthenticationInfo {

    private static final Logger logger = LogManager.getLogger(AuthenticationInfo.class);

    private String description;
    private ParameterName name;
    private String value;
    private ParameterLocation in;
    private Long timeout;

    public static AuthenticationInfo parse(Map<String, Object> x) {

        // Check that the parsed JSON map contains all and only the required fields
        if (x.size() < 4 || x.size() > 5 || !x.containsKey("name") || !x.containsKey("value") ||
                !x.containsKey("in") || !x.containsKey("timeout")) {

            logger.error("Authorization script must return a json containing all and only the fields" +
                    "'name', 'value', 'in', 'timeout'. Instead, its result was:\n");
        }

        AuthenticationInfo auth = new AuthenticationInfo();
        auth.setName(new ParameterName((String) x.get("name")));
        auth.setValue((String) x.get("value"));
        auth.setIn(ParameterLocation.getLocationFromString((String) x.get("in")));
        auth.setTimeout(((Double)x.get("timeout")).longValue());
        if (x.containsKey("description")) {
            auth.setDescription((String) x.get("description"));
        }

        return auth;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ParameterName getName() {
        return name;
    }

    public void setName(ParameterName name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public ParameterLocation getIn() {
        return in;
    }

    public void setIn(ParameterLocation in) {
        this.in = in;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

}
