package io.resttestgen.core.dictionary;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DictionaryTest {

    private static Dictionary dictionary;

    @BeforeAll
    public static void setUp() {
        dictionary = new Dictionary();
    }

    @Test
    public void addAndGetEntryTest() /*throws InvalidOpenAPIException*/ {
        /*DictionaryEntry dictionaryEntry = new DictionaryEntry(new ParameterName("myParameter"),
                new Operation("/test", "GET", null), 999);
        dictionary.addEntry(dictionaryEntry);

        assertEquals(dictionaryEntry, dictionary.getEntryList(dictionaryEntry.getNormalizedParameterName()).get(0));*/
    }
}
