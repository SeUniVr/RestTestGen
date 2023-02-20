# RestTestGen Changelog

### v23.02
- Values in "requires" inter-parameter dependencies are used as example values for parameters.
- Added several utility methods to ParameterElements and subclasses.
- `CRUDSemantics` is now `OperationSemantics`, supporting multiple semantics other than CRUD (e.g., login, logout).
- Added method to `ExtendedRandom` to generate short lengths for strings and arrays.
- Fixed bug in application of several inter-parameter dependency.
- Fixed a bug that caused mandatory array parameters to be removed in some cases.
- Fixed exceptions caused by response processors trying to process non-executed test interactions.
- Fixed exception caused by `nextLength(minLength, maxLength)` method of `ExtendedRandom`, when `minLength` and `maxLength` had the same value.
- Fixed bug in status code coverage computation.

### v23.01
- Added mass assignment vulnerability detection security testing strategy (beta). (Add `"strategyName": "MassAssignmentSecurityTestingStrategy"` to the configuration file to use this strategy).

### v22.12
- Self-signed certificates are trusted by internal HTTPS client.
- Added feature to export the parsed, and possibly modified, internal representation of the OpenAPI specification.
- Fixed parsing of `exclusiveMinimum` and `exclusiveMaximum` properties of OpenAPI parameters.
- Fixed `ClassCastException` in method `replace()` of `ParameterElement`.

### v22.11
- Added support for `*/*` request and response bodies, that are rendered and parsed as JSON.
- General improvements to parameters management: e.g., support for location string (lat/long), better management of arrays, better casting of numeric strings to number parameters, etc.
- Simpler implementation of error fuzzer (older implementation still available in other class).
- Fixed problem with encoding of values of path parameters.

### v22.10
- Added support for basic inter-parameter dependencies (as defined by Martin-Lopez et al.) such as requires, or, onlyOne, allOrNone, zeroOrOne.
- Added support for `application/x-www-form-urlencoded` request bodies.
- Fixed mutation operators of error fuzzer
- Fixed computation of coverage
- Fixed REST-assured writer