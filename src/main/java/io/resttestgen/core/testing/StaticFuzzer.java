package io.resttestgen.core.testing;

import java.util.List;

public abstract class StaticFuzzer extends Fuzzer {

    public abstract List<TestSequence> generateTestSequences(int numberOfSequences);
}
