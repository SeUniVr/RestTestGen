# Requirements

### Operating system
We suggest to run RestTestGen on Ubuntu. The current version of RestTestGen has been tested on Ubuntu 22.04 LTS.

### Docker
Required if you plan to launch RestTestGen via Docker. Moreover, some external components such as the NLP Rule Extractor are Docker-based, so you need Docker to launch them before starting RestTestGen.

### Java
*Not required if you are using RestTestGen in Docker.*

Make sure to have Java 11 installed in your system. If you are running Ubuntu >= 20.04, you can install Java 11 with the following commands.
```
sudo apt update
sudo apt install openjdk-11-jdk
```

### Dependencies
Dependencies are automatically installed by Gradle during the first run.

### Python
RestTestGen itself does not require Python, but the provided authentication script example does. In order to make such script work, please have Python 3 installed on your machine.
```
sudo apt update
sudo apt install python3
```