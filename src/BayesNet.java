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
        double res = 1.0;
        for (int i = 0; i < variableList.size(); i++) {
            VariableOutcome variableOutcome = variableList.get(i);
            res *= variableOutcome.getProbability(variableList);
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
        // remove the `P`, `parenthesis` and `method`
        query = query.substring(2, query.length() - 3);
        // split the query by `|` to get the `query` and `evidence`
        String[] parts = query.split("\\|");
        String queryPart = parts[0].trim();
        String evidencePart = parts[1].trim();

        // split the query by `,` to get the variables and their outcomes
        String[] queryVariables = queryPart.split(",");
        String[] evidenceVariables = evidencePart.split(",");

        // create a list of variables and their outcomes
        Set<String> queryVariablesSet = new HashSet<>();
        List<VariableOutcome> queryVariablesOutcomes = parseVariableOutcomes(queryVariables);
        List<VariableOutcome> evidenceVariablesOutcomes = parseVariableOutcomes(evidenceVariables);

        Set<String> queryVariablesNames = queryVariablesOutcomes.stream().map(variableOutcome -> variableOutcome.variable.getName()).collect(Collectors.toSet());
        Set<String> evidenceVariablesNames = evidenceVariablesOutcomes.stream().map(variableOutcome -> variableOutcome.variable.getName()).collect(Collectors.toSet());

        queryVariablesSet.addAll(queryVariablesNames);
        queryVariablesSet.addAll(evidenceVariablesNames);

        // find all hidden variables
        Set<String> hiddenVariablesSet = new HashSet<>(this.variables.keySet());
        hiddenVariablesSet.removeAll(queryVariablesSet);


        // calc the joint probability of the evidence and the query
        // we use the law of total probability - so we calculate the joint probability of the evidence and the query and all combinations of the hidden variables
        // go through all combinations of the hidden variables and combine them with the evidence and the query (the outcomes of them are not known)

        double numerator = 0.0;
        double denominator = 0.0;
        List<List<VariableOutcome>> hiddenCombos = getAllVariableOutcomes(hiddenVariablesSet);
        List<List<VariableOutcome>> queryCombos = getAllVariableOutcomes(queryVariablesSet);
        // remove all query options that we calculated
        for (List<VariableOutcome> queryCombo : queryCombos) {
            for (List<VariableOutcome> hiddenCombo : hiddenCombos) {
                List<VariableOutcome> combo = new ArrayList<>(hiddenCombo);
                // add the evidence to the combo
                combo.addAll(evidenceVariablesOutcomes);
                // add the query to the combo
                combo.addAll(queryCombo);
                // calculate the joint probability of the evidence and the query
                double prob = calculateJointProbabilityFromVarOutcomeList(combo);
                // add the joint probability to the total
                denominator += prob;

                // check if the combination contains the query original outcomes - add the joint probability to the numerator
                boolean containsQuery = true;
                for (VariableOutcome variableOutcome : combo) {
                    if (queryVariablesNames.contains(variableOutcome.variable.getName())) {
                        // get the outcome from the original query
                        for (VariableOutcome queryVariableOutcome : queryVariablesOutcomes) {
                            if (variableOutcome.variable.getName().equals(queryVariableOutcome.variable.getName())) {
                                containsQuery = variableOutcome.outcome.equals(queryVariableOutcome.outcome);
                                break;
                            }
                        }
                    }
                }
                if (containsQuery) {
                    numerator += prob;
                }
            }
        }

        return numerator / denominator;
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
