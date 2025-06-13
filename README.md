# Bayesian Network

This repository contains an academic project implementing Bayesian network inference in Java. The goal was to create a small inference engine capable of reading a network description in **BIFXML** format, processing probabilistic queries and producing the resulting probabilities.

## Features

- Load Bayesian networks from XML files. Example networks are provided (`alarm_net.xml`, `big_net.xml`, `family.xml`).
- Support for calculating joint probabilities as well as conditional probabilities using three elimination methods.
- Basic counters to track the number of sum and product operations during inference.
- Includes a `TestFactor` class with unit tests for the `Factor` operations (requires JUnit 5).

## Project Structure

```
├── src/                 # Java source files
│   ├── BayesNet.java    # Core inference engine
│   ├── Counter.java     # Operation counters
│   ├── Ex1.java         # Runner that reads queries from `input.txt`
│   ├── Factor.java      # Factor representation and operations
│   ├── InteractionGraph.java
│   ├── Variable.java
│   ├── VariableOutcome.java
│   └── TestFactor.java  # JUnit tests for Factor (optional)
├── alarm_net.xml        # Example network
├── big_net.xml          # Example network
├── family.xml           # Example network
├── input.txt            # Example input file with queries
└── output.txt           # Example output produced by Ex1
```

## Running

1. Ensure you have a Java compiler (tested with `javac 1.8`) available in your `PATH`.
2. Compile the project:

```bash
javac src/*.java
```

3. Place the network file you want to use and the queries in `input.txt`. The first line of `input.txt` should be the path to the XML file followed by one query per line. Queries have the form `P(X=Y|Z=W),<method>` where `<method>` is `1`, `2` or `3` for the different elimination approaches.
4. Run the program:

```bash
java -cp src Ex1
```

The results are written to `output.txt` in the format `<probability>,<sumCount>,<productCount>` where the counts reflect how many summation and multiplication operations were performed.

### Running the Tests

The `TestFactor` class provides JUnit tests for the `Factor` class. To execute them you need JUnit 5 on your classpath. For example:

```bash
# assuming junit-platform-console-standalone.jar is downloaded
javac -cp junit-platform-console-standalone.jar src/*.java
java -jar junit-platform-console-standalone.jar -cp src --scan-classpath
```

## License

This project was created as part of a university assignment and is provided for educational purposes.
