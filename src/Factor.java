import java.util.*;

/**
 * Represents a factor in a Bayesian network.
 * A factor is defined over a set of variables and provides the probability
 * for each possible assignment of outcomes to these variables.
 */
public class Factor {
    private final List<Variable> variables;
    private final double[] probabilities;
    private final Map<List<String>, Double> factorTable;

    /**
     * Constructs a Factor for the given variables and their probabilities.
     *
     * @param variables     The list of variables (parents + child, order matters).
     * @param probabilities The probability values for each combination of outcomes.
     */
    public Factor(List<Variable> variables, double[] probabilities) {
        this.variables = new ArrayList<>(variables);
        this.probabilities = Arrays.copyOf(probabilities, probabilities.length);
        this.factorTable = new LinkedHashMap<>(); // change this to `LinkedHashMap` to keep insertion order
        buildFactorTable();
    }

    /**
     * Returns the probability for the given assignment of variable outcomes.
     *
     * @param vars The list of VariableOutcome representing the assignment.
     * @return The probability for the assignment.
     * @throws IllegalArgumentException if the assignment is incomplete or not found.
     */
    public double getProbability(List<VariableOutcome> vars) {
        // map to hold the variable and its corresponding outcome
        Map<Variable, String> outcomeMap = new HashMap<>();
        for (VariableOutcome vo : vars) {
            outcomeMap.put(vo.variable, vo.outcome);
        }

        // create the `key` for the factor table (the outcomes of the variables)
        List<String> key = new ArrayList<>();
        for (Variable currentVar : variables) {
            String outcome = outcomeMap.get(currentVar);
            if (outcome == null) {
                throw new IllegalArgumentException("Variable not found in the factor: " + currentVar.getName());
            }
            key.add(outcome);
        }

        if (factorTable.containsKey(key)) {
            return factorTable.get(key);
        } else {
            throw new IllegalArgumentException("Combination of outcomes not found in the factor table: " + key);
        }
    }

    /**
     * Builds the factor table mapping each combination of outcomes to its probability.
     */
    private void buildFactorTable() {
        int numVariables = variables.size();
        if (numVariables == 0) return;

        int totalCombinations = 1;
        for (Variable var : variables) {
            totalCombinations *= var.getOutcomes().size();
        }


        // go through all combinations of outcomes
        for (int i = 0; i < totalCombinations; i++) {
            // Create a list to hold the current combination of outcomes
            List<String> currentCombination = new ArrayList<>(Collections.nCopies(numVariables, null));
            int tempIndex = i;

            // Iterate through each variable in reverse order to fill the combination
            for (int j = numVariables - 1; j >= 0; j--) {
                Variable currentVar = variables.get(j);
                // Get the index of the current variable's outcome based on the tempIndex
                List<String> outcomes = currentVar.getOutcomes();
                int numOutcomes = outcomes.size();
                int outcomeIndex = tempIndex % numOutcomes;
                // Set the outcome for the current variable
                currentCombination.set(j, outcomes.get(outcomeIndex));

                // Update the temporary index for the next variable (which cycles slower)
                tempIndex /= numOutcomes;
            }

            factorTable.put(Collections.unmodifiableList(currentCombination), probabilities[i]);
        }
    }

    /**
     * Returns a string representation of the factor's CPT.
     *
     * @return The CPT as a formatted string.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Factor CPT:\n");

        // Header
        for (Variable variable : variables) {
            sb.append(String.format("%-10s | ", variable.getName()));
        }
        sb.append("Probability\n");

        // Separator line
        for (int i = 0; i < variables.size(); i++) {
            sb.append("-----------|-");
        }
        sb.append("-----------\n");


        for (Map.Entry<List<String>, Double> entry : factorTable.entrySet()) {
            List<String> combination = entry.getKey();
            for (String outcome : combination) {
                sb.append(String.format("%-10s | ", outcome));
            }
            sb.append(String.format("%.3f%n", entry.getValue()));
        }
        return sb.toString();
    }
}
