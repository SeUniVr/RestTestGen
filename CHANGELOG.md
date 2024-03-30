# RestTestGen Changelog

### v24.03
- Upgraded Gradle to v8.3 (both Gradle Wrapper and Docker).
- Support for patterns (regular expressions) to generate compliant values.
- New implementation of mutators (parameter and operation mutators).
- Response processors are now called interaction processors as they can also process request data.
- Improved log messages.

### v23.09
- Fixed export of OpenAPI specifications, which caused some specifications to be invalid.
- Added support for export of combined schemas (e.g., `allOf`, `oneOf`, etc.) in exported specifications.
- Parameter value providers can be configured with a so-called "source class", allowing to specify the source of parameter values (this feature is working but not 100% precise in the current version):
  - `SELF`: values are sourced from the particular instance of the parameter.
  - `SAME_NAME`: values are sourced from all the parameters with the same name in the API. For instance, example values are source from all the parameters with the same name, potentially increasing the amount of valid values for the parameter.
  - `SAME_NORMALIZED_NAME`: values are sourced from all the parameters with the same normalized name (according to RestTestGen's normalization algorithm) in the API.
- New method to set values to parameter with providers, that stores the provider with which the value was gathered.
- Added new narrow random parameter value provider which chooses random values from narrower boundaries. E.g., not MIN_INT~MAX_INT, but 0~120.
- Parameter value providers are now instantiated through a cached factory which caches previous instances for optimization purposes.
- Fixed `TestRunner` that did not execute test interactions if 429 was not among the invalid status codes.
- All responses containing JSON bodies are processed, independently of the declared response content-type. Previously, only responses with `application/json` as content type were processed, causing the loss of processing of potentially useful if the API responded with an inappropriate content-type (very common behavior, also in mainstream API).
- Improved log messages.

### v23.07
- New way to configure and launch the tool, including individual configuration for each API.
    - YAML OpenAPI specifications now natively supported (automatic conversion to JSON on first launch).
    - Added support for reset script, allowing to reset the state of API under test.
- Improved authentication support: multiple users; token is now refreshed when expired.
- Added Dockerfile to quickly build and run RestTestGen in a Docker container.
- Renamed classes and variables to match new implementation.
- Improved messages in logs.
- Updated Gradle to version 7.6.2.

### v23.05
- Included new strategy (`NlpStrategy`) to enhance specifications with NLP techniques (constraints and example values are automatically extracted from text descriptions and added to the specification). To use this strategy make sure you have the Rule Extractor service running at `http://localhost:4000`. You can download the Rule Extractor from [https://github.com/codingsoo/nlp2rest/](https://github.com/codingsoo/nlp2rest/). Deployment with Docker container is suggested.
- Major refactoring of `Operation` and `Parameter` classes (including sub-classes). Introduced safe setters for hierarchical parameters, renamed several classes.
- Specification export now supports multiple examples and inter-parameter dependencies.

### v23.04
- Added support to REST path, to quickly query parameters in REST requests and responses.
- Added method `size()` to `ParameterArray` to get the size of the array.

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