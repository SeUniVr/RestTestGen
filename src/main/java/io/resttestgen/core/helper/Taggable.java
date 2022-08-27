package io.resttestgen.core.helper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Class that enable subclasses to store and manage tags.
 */
public class Taggable {

    public final HashSet<String> tags = new HashSet<>();

    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public boolean addTag(String tag) {
        return tags.add(tag);
    }

    public boolean removeTag(String tag) {
        return tags.remove(tag);
    }

    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }
}
