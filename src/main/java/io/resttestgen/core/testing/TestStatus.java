package io.resttestgen.core.testing;

public enum TestStatus {

    CREATED, // Test interaction created, but not yet executed
    EXECUTED, // Test interaction executed
    ERROR // Test interaction execution returned an error
}
