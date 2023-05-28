# RestTestGen v2

A framework for automated black-box testing of RESTful APIs.

> **v2:** this is a re-implementation of the original tool proposed by our research team in 2020. This new version is implemented as a framework that allows researchers and practitioners to implement their own testing strategy on top of our core. Testing strategies presented in our papers are included, and available in the `io.resttestgen.implementation` package.

> **Citing our work:** bibtex entries for our papers are available in the [CITE.md](CITE.md) file.

The core of the RestTestGen framework provides the following features:
- custom parser for OpenAPI specifications v3 in JSON format (we dropped Swagger Codegen that caused instability)
  - please convert Swagger 2.0 files or YAML specifications to JSON v3 specification using [https://editor.swagger.io/](https://editor.swagger.io/)
  - combined parameter schemas (OneOf, AllOf, AnyOf) are supported by the parser, but currently not used in our strategies (wip)
- the Operation Dependency Graph (ODG), that models data dependencies among operations
- dictionaries storing values observed at testing time
- smart parameter management using hierarchical data structures
  - these data structures are automatically converted to HTTP requests
- fuzzers to generate parameter values
- mutation operators
  - data type mutation operator
  - missing required mutation operator
  - constraint violation mutation operator
- response processors to elaborate responses
- operations sorters to provide customized operations testing ordering
  - random sorter
  - ODG-based dynamic sorter
- oracles to evaluate test executions
  - nominal status code oracle
  - error status code oracle
  - schema validation oracle (wip)
- writers to output test cases and test results to file
  - JSON report writer
  - JUnit + OkHttp test case writer (wip)
  - JUnit + REST Assured test case writer
- coverage measurements
  - path coverage
  - operations coverage
  - status code coverage
  - parameter coverage
  - parameter value coverage

---

## Changelog

### v23.05
- Included new strategy (`NlpStrategy`) to enhance specifications with NLP techniques (constraints and example values are automatically extracted from text descriptions and added to the specification). To use this strategy make sure you have the Rule Extractor service running at `http://localhost:4000`. You can download the Rule Extractor from [https://github.com/codingsoo/nlp2rest/](https://github.com/codingsoo/nlp2rest/). Deployment with Docker container is suggested.
- Major refactoring of `Operation` and `Parameter` classes (including sub-classes). Introduced safe setters for hierarchical parameters, renamed several classes.
- Specification export now supports multiple examples and inter-parameter dependencies.

For the changelog of past versions see the [CHANGELOG.md](CHANGELOG.md) file.


## Requirements
- Java 11
- Ubuntu, or other Linux distributions (RestTestGen was not tested on Windows)

For further information see the [REQUIREMENTS.md](REQUIREMENTS.md) file.

## Configuration
RestTestGen comes with a built-in default configuration suitable for most REST APIs, defined in the `io.resttestgen.core.Configuration` class. The default configuration can be overridden by means of the `rtg_config.json` file. The file must be located in the current working directory.

Configurable preferences:
- `logVerbosity`: the verbosity of the log. Can be `DEBUG`, `INFO`, `WARN`, or `ERROR` (default: `INFO`)
- `testingSessionName`: the name of the testing session printed in the report files (default: `test-session-CURRENT_TIME`)
- `outputPath`: the path in which output data is written (default: `output/`)
- `specificationFileName`: the file name of the input OpenAPI specification (default: `openapi.json`)
- `authCommand`: command to be run to gather information about authentication. See [Authentication](#auth) (default: none)
- `strategyName`: the name of the testing strategy class to be run. `NominalAndErrorStrategy` or your own strategy class (default: `NominalAndErrorStrategy`)
- `qualifiableNames`: list of parameter names to be qualified. E.g., `id` -> `petId`, `name` -> `petName` (default: `['id', 'name']`)
- `odgFileName`: output file name for the operation dependency graph (default: `odg.dot`)

We suggest to override only the strictly necessary preferences, typically the file name of the OpenAPI specification, and the authentication command.

Specific preferences to the nominal and error testing strategy are at the moment only customizable in the source code.

Example of a configuration file (`rtg_config.json`):
```
{
  "specificationFileName": "path/to/my/specification.json",
  "authCommand": "python auth.py",
  "strategyName": "NominalAndErrorStrategy",
  "testingSessionName": "customNameForTestingSession",
  "outputPath": "output/"
}
```

### <a id="auth"></a>Authentication
> (skip if your REST API does not require authentication)

RestRestGen supports authenticated REST APIs. To gather authentication information, RestTestGen runs the authentication command (if provided in the configuration), whose output is expected to be a JSON array. Objects in the array are authentication parameters: they have a name, a value, a location (header, query, etc.), and a timeout (the authentication command will be re-executed, e.g., to gather a new token, after the timeout). The current version of RestTestGen only supports 1 parameter (the first one in the array). Other parameters are currently ignored.


Example of the output of the authentication command:
```
[
  {
    "name": "Authorization",
    "value": "Bearer [TOKEN]",
    "in": "header",
    "timeout": 600
  }
]
```
This output is generated by an example python script `auth.py`, which authenticates a user with the REST API through HTTP interactions. In this example the authentication command for RestTestGen is `python path/to/auth.py`.

## Building and running RestTestGen
To build and run RestTestGen with Gradle use the command: `./gradlew run`

Alternatively, you can open the RestTestGen Gradle project with IntelliJ IDEA. To run RestTestGen, click the play icon alongside the `main` method of the class `io.resttestgen.core.cli.App`.

> Warning: make sure that your gradlew file is executable. Moreover, some test cases only work with the default OpenAPI specification, so in the case the execution fails due to failed test cases, we recommend to skip tests adding `-x test` to Gradle commands.

## Currently available testing strategies

- `NominalAndErrorStrategy`: performs nominal and error testing of the selected REST API.
- `MassAssignmentSecurityTestingStrategy`: tests the selected REST API for mass assignment vulnerabilities.
- `NlpStrategy`: enhances the OpenAPI specification of the selected REST API by extracting constraints and example values from textual descriptions in natural language. Requires Rule Extraction service to be running at [http://localhost:4000/](). Download and deploy the Rule Extractor service from the [NLP2REST repository](https://github.com/codingsoo/nlp2rest/).

## Contributing

Contributions by researchers, practitioners, and developers are welcome.

To extend the RestTestGen framework and its core components, please contribute in the `io.resttestgen.core` package. If you are developing a novel testing strategy, please contribute in the `io.resttestgen.implementaion` package.
 
Please submit your contribution by means of a pull request. Thank you!