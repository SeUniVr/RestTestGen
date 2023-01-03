# RestTestGen Changelog

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