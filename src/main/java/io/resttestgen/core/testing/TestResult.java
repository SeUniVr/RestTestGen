package io.resttestgen.core.testing;

public class TestResult {

    private enum TestResultEnum { PENDING, PASS, FAIL, ERROR, UNKNOWN }
    private transient final String defaultMessage = "No further information available.";

    private TestResultEnum result = TestResultEnum.PENDING;
    private String message = "Pending evaluation.";


    public void setPass() {
        this.result = TestResultEnum.PASS;
        this.message = defaultMessage;
    }

    public void setPass(String message) {
        this.result = TestResultEnum.PASS;
        this.message = message;
    }

    public void setFail() {
        this.result = TestResultEnum.FAIL;
        this.message = defaultMessage;
    }

    public void setFail(String message) {
        this.result = TestResultEnum.FAIL;
        this.message = message;
    }

    public void setError() {
        this.result = TestResultEnum.ERROR;
        this.message = defaultMessage;
    }

    public void setError(String message) {
        this.result = TestResultEnum.ERROR;
        this.message = message;
    }

    public void setUnknown() {
        this.result = TestResultEnum.UNKNOWN;
        this.message = defaultMessage;
    }

    public void setUnknown(String message) {
        this.result = TestResultEnum.UNKNOWN;
        this.message = message;
    }

    public boolean isPending() {
        return result == TestResultEnum.PENDING;
    }

    public boolean isPass() {
        return result == TestResultEnum.PASS;
    }

    public boolean isFail() {
        return result == TestResultEnum.FAIL;
    }

    public boolean isError() {
        return result == TestResultEnum.ERROR;
    }

    public boolean isUnknown() {
        return result == TestResultEnum.UNKNOWN;
    }
}
