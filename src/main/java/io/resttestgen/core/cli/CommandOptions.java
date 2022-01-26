package io.resttestgen.core.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

@SuppressWarnings("unchecked")

public class CommandOptions {

    protected ArrayList arguments;
    public CommandOptions(String[] args) {
        parse(args);
    }

    public void parse(String[] args) {
        arguments = new ArrayList();
        arguments.addAll(Arrays.asList(args));
    }

    public int size() {
        return arguments.size();
    }

    public boolean hasOption(String option) {
        int index = searchOption(option);
        return index >= 0;
    }

    public int searchOption(String option) {
        String str;
        for (int i = 0; i < arguments.size(); i++) {
            str = (String)arguments.get(i);
            if (str.equalsIgnoreCase(option)) {
                return i;
            }
        }
        return -1;
    }

    public String valueOf(String option) {
        int index = searchOption(option);
        if (index >= 0) {
            return (String) arguments.get(index + 1);
        }
        return null;
    }

    public static boolean checkFileExists(String filePathString) {
        File f = new File(filePathString);
        return f.exists() && !f.isDirectory();
    }
}