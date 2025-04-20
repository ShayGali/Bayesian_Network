import java.util.*;

public class Factor {
    private final List<Variable> variables;
    private final double[] probabilities;

    private final Map<List<String>, Double> factorTable;

    public Factor(List<Variable> variables, double[] probabilities) {
        this.variables = new ArrayList<>(variables);
        this.probabilities = Arrays.copyOf(probabilities, probabilities.length);
        this.factorTable = new LinkedHashMap<>(); // change this to `LinkedHashMap` to keep insertion order
        buildFactorTable();
    }

    public double getProbability(List<VariableOutcome> vars) {
        // built the key for the map
        List<String> key = new ArrayList<>();
        for (Variable currentVar : variables) {
            String outcome = null;
            for (VariableOutcome var : vars) {
                if (var.variable.equals(currentVar)) {
                    outcome = var.outcome;
                    break;
                }
            }
            if (outcome == null) {
                throw new IllegalArgumentException("Variable not found in the factor.");
            }
            key.add(outcome);
        }

        // Check if the key exists in the factor table
        if (factorTable.containsKey(key)) {
            return factorTable.get(key);
        } else {
            throw new IllegalArgumentException("Combination of outcomes not found in the factor table.");
        }
    }

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
