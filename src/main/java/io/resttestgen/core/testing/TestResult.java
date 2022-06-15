package io.resttestgen.core.testing;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public class TestResult {

    private enum TestResultEnum { PENDING, PASS, FAIL, ERROR, UNKNOWN }
    private transient final String defaultMessage = "No further information available.";

    private TestResultEnum result = TestResultEnum.PENDING;
    private String message = "Pending evaluation.";


    public TestResult setPass() {
        this.result = TestResultEnum.PASS;
        this.message = defaultMessage;
        return this;
    }

    public TestResult setPass(String message) {
        this.result = TestResultEnum.PASS;
        this.message = message;
        return this;
    }

    public TestResult setFail() {
        this.result = TestResultEnum.FAIL;
        this.message = defaultMessage;
        return this;
    }

    public TestResult setFail(String message) {
        this.result = TestResultEnum.FAIL;
        this.message = message;
        return this;
    }

    public TestResult setError() {
        this.result = TestResultEnum.ERROR;
        this.message = defaultMessage;
        return this;
    }

    public TestResult setError(String message) {
        this.result = TestResultEnum.ERROR;
        this.message = message;
        return this;
    }

    public TestResult setUnknown() {
        this.result = TestResultEnum.UNKNOWN;
        this.message = defaultMessage;
        return this;
    }

    public TestResult setUnknown(String message) {
        this.result = TestResultEnum.UNKNOWN;
        this.message = message;
        return this;
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
