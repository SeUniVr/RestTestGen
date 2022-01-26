package io.resttestgen.core.operationdependencygraph;

public enum DependencyType {
    DATA,           // A data dependency
    CREATE,         // A create CRUD dependency
    RETRIEVE,       // A read CRUD dependency
    UPDATE,         // A update CRUD dependency
    DELETE          // A delete CRUD dependency
    // FIXME: implement new dependencies as discussed
}
