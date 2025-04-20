import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BayesNet {
    // init variable to outcome class

    HashMap<String, Variable> variables;

    public BayesNet() {
        this.variables = new HashMap<>();
    }

    public double answerQuery(String query) {
        // if the query parenthesis, its joint probability query
        if (query.endsWith(")")) {
            return calculateJointProbabilityFromQuery(query);
        } else {// it for the algorithm
            return calculateProbForComplexQuery(query);
        }
    }

    private double calculateJointProbabilityFromQuery(String query) {
        // remove the P, and the parenthesis
        query = query.substring(2, query.length() - 1);

        // split the query by the comma
        String[] variables = query.split(",");

        // create a list of variables and their outcomes
        List<VariableOutcome> variableList = parseVariableOutcomes(variables);

        return calculateJointProbabilityFromVarOutcomeList(variableList);
    }

    private static double calculateJointProbabilityFromVarOutcomeList(List<VariableOutcome> variableList) {
        // calculate the joint probability - each variable is independent of the net given the parents
        Counter counter = Counter.instance;
        double res = variableList.get(0).getProbability(variableList); // get the probability of the first variable
        for (int i = 1; i < variableList.size(); i++) {
            VariableOutcome variableOutcome = variableList.get(i);
            res *= variableOutcome.getProbability(variableList);
            counter.incrementProductCounter();
        }

        return res;
    }


    private double calculateProbForComplexQuery(String query) {
        char method = query.charAt(query.length() - 1); // get the method

        switch (method) {
            case '1':
                return calculateProbForComplexQueryMethod1(query);
            case '2':
                return calculateProbForComplexQueryMethod2(query);
            case '3':
                return calculateProbForComplexQueryMethod3(query);
            default:
                throw new IllegalArgumentException("Invalid method: " + method);
        }

    }

    private double calculateProbForComplexQueryMethod1(String query) {
        // parse the query and evidence
        QueryParts qp = parseQueryAndEvidence(query);

        // get the query and evidence variables
        Set<String> allObservedVars = new HashSet<>();
        allObservedVars.addAll(qp.queryVarNames);
        allObservedVars.addAll(qp.evidenceVarNames);

        Set<String> hiddenVars = new HashSet<>(this.variables.keySet());
        hiddenVars.removeAll(allObservedVars);

        // get all combinations of the hidden variables and the query variables
        List<List<VariableOutcome>> hiddenCombos = getAllVariableOutcomes(hiddenVars);
        List<List<VariableOutcome>> queryCombos = getAllVariableOutcomes(qp.queryVarNames);

        double numerator = 0.0; // the probability of the query & evidence
        double denominator = 0.0; // the probability of the (query & evidence) + (!query & evidence)

        // go through all combinations of the hidden variables and the query variables (the evidence is fixed)
        // and calculate the joint probability for each combination
        Counter counter = Counter.instance;
        for (List<VariableOutcome> queryCombo : queryCombos) {
            for (List<VariableOutcome> hiddenCombo : hiddenCombos) {
                List<VariableOutcome> fullAssignment = new ArrayList<>();
                fullAssignment.addAll(hiddenCombo);
                fullAssignment.addAll(qp.evidenceOutcomes);
                fullAssignment.addAll(queryCombo);

                double prob = calculateJointProbabilityFromVarOutcomeList(fullAssignment);

                // if the query matches the evidence, add to the numerator
                if (matchesQuery(queryCombo, qp.queryOutcomes)) {
                    if (numerator > 0) {
                        counter.incrementSumCounter();
                    }
                    numerator += prob;
                } else { // we will add the numerator to the denominator in the end
                    if (denominator > 0) {
                        counter.incrementSumCounter();
                    }
                    denominator += prob;
                }

            }
        }
        denominator += numerator;
        counter.incrementSumCounter();

        // return the normalized probability
        return numerator / denominator;
    }

    // Helper class to hold parsed query/evidence parts

    /**
     * Helper class to hold parsed query and evidence parts.
     * It contains the query outcomes, evidence outcomes, and their respective variable names.
     * This is used to simplify the parsing and matching process.
     */
    private static class QueryParts {
        List<VariableOutcome> queryOutcomes;
        List<VariableOutcome> evidenceOutcomes;
        Set<String> queryVarNames;
        Set<String> evidenceVarNames;

        QueryParts(List<VariableOutcome> q, List<VariableOutcome> e) {
            this.queryOutcomes = q;
            this.evidenceOutcomes = e;
            this.queryVarNames = new HashSet<>();
            this.evidenceVarNames = new HashSet<>();
            for (VariableOutcome vo : q) this.queryVarNames.add(vo.variable.getName());
            for (VariableOutcome vo : e) this.evidenceVarNames.add(vo.variable.getName());
        }
    }

    /**
     * Parses the query and evidence from the given string.
     * The string format is assumed to be "P(X=outcome1, Y=outcome2 | Z=outcome3, W=outcome4)"
     * where X, Y are query variables and Z, W are evidence variables.
     *
     * @param query the query string
     * @return a QueryParts object containing the parsed query and evidence outcomes
     */
    private QueryParts parseQueryAndEvidence(String query) {
        String stripped = query.substring(2, query.length() - 3);
        String[] parts = stripped.split("\\|");
        String queryPart = parts[0].trim();
        String evidencePart = parts[1].trim();

        List<VariableOutcome> queryOutcomes = parseVariableOutcomes(queryPart.split(","));
        List<VariableOutcome> evidenceOutcomes = parseVariableOutcomes(evidencePart.split(","));
        return new QueryParts(queryOutcomes, evidenceOutcomes);
    }

    /**
     * Checks if the given assignment of variable outcomes matches the original query outcomes.
     * This is used to ensure that the correct outcomes are being considered in the calculation.
     *
     * @param assignment    the assignment of variable outcomes
     * @param originalQuery the original query outcomes
     * @return true if the assignment matches the original query, false otherwise
     */
    private boolean matchesQuery(List<VariableOutcome> assignment, List<VariableOutcome> originalQuery) {
        // Build a map from variable name to outcome for the assignment
        Map<String, String> assignmentMap = new HashMap<>();
        for (VariableOutcome vo : assignment) {
            assignmentMap.put(vo.variable.getName(), vo.outcome);
        }
        // Check that all variables in the original query match the assignment
        for (VariableOutcome orig : originalQuery) {
            if (!assignmentMap.containsKey(orig.variable.getName()) ||
                    !assignmentMap.get(orig.variable.getName()).equals(orig.outcome)) {
                return false;
            }
        }
        return true;
    }

    private double calculateProbForComplexQueryMethod2(String query) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private double calculateProbForComplexQueryMethod3(String query) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void addVariable(String name, List<String> outcomes) {
        Variable variable = new Variable(name, outcomes);
        this.variables.put(name, variable);
    }

    /**
     * Adds a dependency to the BayesNet. <b> Make sure to add the variable first, the function asserts that the variable and all parent variables are present in the BayesNet. </b>
     *
     * @param variableName  the name of the variable
     * @param parents       the names of the parent variables
     * @param probabilities the probabilities associated with the variable
     * @throws AssertionError if the variable or any parent variable is not found
     */
    public void addDependency(String variableName, List<String> parents, double[] probabilities) {
        Variable variable = this.variables.get(variableName);
        assert variable != null : "Variable not found: " + variableName;
        List<Variable> parentVariables = new ArrayList<>();
        for (String parentName : parents) {
            Variable parentVariable = this.variables.get(parentName);
            assert parentVariable != null : "Parent variable not found: " + parentName;
            parentVariables.add(parentVariable);
            parentVariable.addChildren(variable);
        }

        variable.setParents(parentVariables);

        variable.setProbabilities(probabilities);

    }

    public void printVariables() {
        for (Variable variable : this.variables.values()) {
            System.out.println(variable.getName() + ": " + variable.getOutcomes() + " " + variable.getParents().stream().map(Variable::getName).collect(Collectors.toList()));
        }
    }

    /**
     * Parses the variable outcomes from the string array.
     *
     * @param variableOutcomesAsString the string array containing the variable outcomes. for example: ["X=T", "Y=F"]
     * @return a list of VariableOutcome objects
     */
    private List<VariableOutcome> parseVariableOutcomes(String[] variableOutcomesAsString) {
        List<VariableOutcome> variableOutcomes = new ArrayList<>();
        for (String variable : variableOutcomesAsString) {
            // split the variable by the `=`
            String[] parts1 = variable.split("=");
            String name = parts1[0].trim();
            String outcome = parts1[1].trim();
            Variable var = this.variables.get(name);
            assert var != null : "Variable not found: " + name;
            variableOutcomes.add(new VariableOutcome(var, outcome));
        }
        return variableOutcomes;
    }

    /**
     * Get all combinations of the variable outcomes for the given variable names.
     *
     * @param variableNames the names of the variables
     * @return a list of all combinations of the variable outcomes
     */
    private List<List<VariableOutcome>> getAllVariableOutcomes(Set<String> variableNames) {
        Stream<List<VariableOutcome>> prod = Stream.of(Collections.emptyList());
        for (String varName : variableNames) {
            Variable var = this.variables.get(varName);
            prod = prod.flatMap(prefix ->
                    var.getOutcomes().stream()
                            .map(elem -> {
                                List<VariableOutcome> copy = new ArrayList<>(prefix);
                                copy.add(new VariableOutcome(var, elem));
                                return copy;
                            })
            );
        }
        return prod.collect(Collectors.toList());
    }


}
