# RestTestGen v2 Beta

A framework for automated black-box testing of RESTful APIs.

> **v2:** this is a re-implementation of the original tool proposed by our research team in 2020. This new version is implemented as a framework that allows researchers and practitioners to implement their own testing strategy on top of our [core](#core-features). Testing strategies presented in our papers are included, and available in the `io.resttestgen.implementation` package.

> **Beta research prototype:** this tool is a research prototype in beta version, so you may experience limitations or unexpected behaviours during testing. Please report bugs using the GitHub issue tracker.

> **Citing our work:** please cite our latest journal paper about RestTestGen: *Automated Black-Box Testing of Nominal and Error Scenarios in RESTful APIs*. BibTeX:
> ```
> @article{Corradini2022,
>     doi = {10.1002/stvr.1808},
>     url = {https://doi.org/10.1002/stvr.1808},
>     year = {2022},
>     month = jan,
>     publisher = {Wiley},
>     author = {Davide Corradini and Amedeo Zampieri and Michele Pasqua and Emanuele Viglianisi and Michael Dallago and Mariano Ceccato},
>     title = {Automated black-box testing of nominal and error scenarios in RESTful APIs},
>     journal = {Software Testing, Verification and Reliability}
> }
> ```

---

## Usage
Launch the `main` method of the class `io.resttestgen.core.cli.App` to start RestTestGen. A complete guide on how to build RestTestGen using Gradle will be provided soon.

## Configuration
RestTestGen comes with a built-in default configuration that works for most testing scenarios. Please look at the class `Configuration` or at the `rtg_config.json` file if you want to customize the behavior of the RestTestGen. More information about customizations will be provided soon.

## <a id="core-features"></a> Features
The core of the RestTestGen framework provides the following features:
- custom parser for OpenAPI specifications v3 in JSON format (we dropped Swagger Codegen that caused instability)
  - please convert Swagger 2.0 files or YAML specifications to JSON v3 specification using [https://editor.swagger.io/](https://editor.swagger.io/)
  - combined parameter schemas (OneOf, AllOf, AnyOf) are supported by the parser, but currently not used in our strategies (wip)
- the Operation Dependency Graph (ODG), that models data dependency among operations
- dictionaries storing values observed at testing time
- smart parameter management using hierarchical data structures
  - these data structures are automatically converted to HTTP requests
- fuzzers to generate parameter values
- parameter mutators
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
  - JUnit + REST Assured test case writer (wip)
- coverage measurements
  - operations coverage (wip)
  - status code coverage (wip)