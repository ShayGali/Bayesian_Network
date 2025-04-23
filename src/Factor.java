import java.util.*;

/**
 * Represents a factor in a Bayesian network.
 * A factor is defined over a set of variables and provides the probability
 * for each possible assignment of outcomes to these variables.
 */
public class Factor {
    private final List<Variable> variables;
    private final Map<List<String>, Double> factorTable;

    /**
     * Constructs a Factor for the given variables and their probabilities.
     *
     * @param variables     The list of variables (parents + child, order matters).
     * @param probabilities The probability values for each combination of outcomes.
     */
    public Factor(List<Variable> variables, double[] probabilities) {
        this.variables = new ArrayList<>(variables);
        this.factorTable = new LinkedHashMap<>();
        buildFactorTable(Arrays.copyOf(probabilities, probabilities.length));
    }

    /**
     * Constructs a Factor for the given variables and their CPT table.
     *
     * @param variables   The list of variables (parents + child, order matters).
     * @param factorTable The factor table mapping each combination of outcomes to its probability.
     */
    public Factor(List<Variable> variables, Map<List<String>, Double> factorTable) {
        this.variables = new ArrayList<>(variables);
        this.factorTable = new LinkedHashMap<>(factorTable);

    }

    /**
     * Builds the factor table mapping each combination of outcomes to its probability.
     *
     * @param probabilities The probability values for each combination of outcomes.
     */
    private void buildFactorTable(double[] probabilities) {
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

            factorTable.put(currentCombination, probabilities[i]);
        }
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
     * Eliminates a variable from the factor by summing out its outcomes.
     *
     * @param variable The variable to be eliminated.
     * @return A new factor with the variable eliminated. (the current factor is not modified)
     */
    public Factor eliminate(Variable variable) {
        Counter counter = Counter.instance;
        // check if the variable is in the factor
        int varIndex = variables.indexOf(variable);
        if (varIndex == -1) {
            throw new IllegalArgumentException("Variable " + variable.getName() + " not found in the factor.");
        }

        // go through all combinations of outcomes, and sum out the variable
        Map<List<String>, Double> newFactorTable = new LinkedHashMap<>();
        for (Map.Entry<List<String>, Double> entry : factorTable.entrySet()) {
            List<String> combination = entry.getKey();
            double probability = entry.getValue();

            // create a new key without the variable
            List<String> newKey = new ArrayList<>(combination);
            newKey.remove(varIndex);

            // sum out the variable
            if (newFactorTable.containsKey(newKey)) {
                newFactorTable.put(newKey, newFactorTable.get(newKey) + probability);
                counter.incrementSumCounter();
            } else {
                newFactorTable.put(newKey, probability);
            }
        }

        // create a new factor with the new factor table
        List<Variable> newVariables = new ArrayList<>(variables);
        newVariables.remove(varIndex);

        return new Factor(newVariables, newFactorTable);
    }


    /**
     * Sets the evidence for the factor. Will remove each row that does not match the evidence, and remove the corresponding variable from the factor.
     *
     * @param evidences The evidence to set for the factor.
     * @return A new factor without the evidence variables and with the remaining probabilities.
     */
    public Factor setEvidences(List<VariableOutcome> evidences) {
        // indices of evidence variables in the factor
        List<Integer> evidenceIndices = new ArrayList<>();
        for (VariableOutcome vo : evidences) {
            int idx = variables.indexOf(vo.variable);
            evidenceIndices.add(idx);
        }

        if (evidenceIndices.isEmpty()) {
            return this; // no evidence, return the original factor
        }

        // sort the indices in descending order (to remove them from the end)
        List<Integer> sortedIndices = new ArrayList<>(evidenceIndices);
        sortedIndices.removeIf(idx -> idx == -1); // remove invalid indices
        sortedIndices.sort(Collections.reverseOrder());

        // list of variables in the factor
        List<Variable> newVariables = new ArrayList<>(variables);

        // create a new factor table for the new factor
        Map<List<String>, Double> newFactorTable = new LinkedHashMap<>();
        for (Map.Entry<List<String>, Double> entry : factorTable.entrySet()) {
            List<String> combination = entry.getKey();
            double probability = entry.getValue();

            // check if the combination matches the evidence
            boolean matches = true;
            for (int i = 0; i < evidences.size(); i++) {
                int idx = evidenceIndices.get(i);
                if (idx != -1 && !combination.get(idx).equals(evidences.get(i).outcome)) {
                    matches = false;
                    break;
                }
            }

            if (matches) {
                // create a new key without the evidence variables
                List<String> newKey = new ArrayList<>(combination);
                for (int idx : sortedIndices) {
                    newKey.remove(idx);
                }
                newFactorTable.put(newKey, probability);
            }
        }

        // create a new factor with the new factor table
        for (int idx : sortedIndices) {
            newVariables.remove(idx); // remove the evidence variables from the factor
        }
        return new Factor(newVariables, newFactorTable);
    }

    /**
     * Joins two factors into one. The resulting factor will have all variables from both factors.
     * The probabilities will be calculated based on the joint distribution.
     *
     * @return The resulting factor with all variables and their probabilities.
     */
    public static Factor join(Factor f1, Factor f2) {
        Counter counter = Counter.instance;

        // get all variables from both factors
        Set<Variable> allVariables = new LinkedHashSet<>(f1.variables);
        allVariables.addAll(f2.variables);

        // covert the set to a list
        List<Variable> allVariablesList = new ArrayList<>(allVariables);

        int totalCombinations = 1;
        for (Variable var : allVariables) {
            totalCombinations *= var.getOutcomes().size();
        }

        // create a new factor table for the joint factor
        Map<List<String>, Double> newFactorTable = new LinkedHashMap<>();
        // go through all combinations of outcomes
        for (int i = 0; i < totalCombinations; i++) {
            // Create a list to hold the current combination of outcomes
            List<String> currentCombination = new ArrayList<>(Collections.nCopies(allVariables.size(), null));
            int tempIndex = i;

            // Iterate through each variable in reverse order to fill the combination
            for (int j = allVariables.size() - 1; j >= 0; j--) {
                Variable currentVar = allVariablesList.get(j);
                // Get the index of the current variable's outcome based on the tempIndex
                List<String> outcomes = currentVar.getOutcomes();
                int numOutcomes = outcomes.size();
                int outcomeIndex = tempIndex % numOutcomes;
                // Set the outcome for the current variable
                currentCombination.set(j, outcomes.get(outcomeIndex));

                // Update the temporary index for the next variable (which cycles slower)
                tempIndex /= numOutcomes;
            }
            // get the probabilities from f1
            List<VariableOutcome> f1Vars = new ArrayList<>();
            for (int j = 0; j < currentCombination.size(); j++) {
                Variable currentVar = allVariablesList.get(j);
                String outcome = currentCombination.get(j);
                if (f1.variables.contains(currentVar)) {
                    f1Vars.add(new VariableOutcome(currentVar, outcome));
                }
            }
            double f1Prob = f1.getProbability(f1Vars);

            // get the probabilities from f2
            List<VariableOutcome> f2Vars = new ArrayList<>();
            for (int j = 0; j < currentCombination.size(); j++) {
                Variable currentVar = allVariablesList.get(j);
                String outcome = currentCombination.get(j);
                if (f2.variables.contains(currentVar)) {
                    f2Vars.add(new VariableOutcome(currentVar, outcome));
                }
            }
            double f2Prob = f2.getProbability(f2Vars);

            // multiply the probabilities
            double jointProb = f1Prob * f2Prob;
            // put the joint probability in the factor table
            newFactorTable.put(currentCombination, jointProb);
        }
        counter.incrementProductCounter(totalCombinations);
        return new Factor(allVariablesList, newFactorTable);
    }

    /**
     * Joins a list of factors into one. The resulting factor will have all variables from all factors.
     *
     * @param factors The list of factors to join.
     * @return The resulting factor with all variables and their probabilities.
     */
    public static Factor join(List<Factor> factors) {
        if (factors.isEmpty()) {
            throw new IllegalArgumentException("List of factors is empty.");
        }

        //sort the factors by size if the size is the same, sort by the sum of the ASCII value of the variable names
        factors.sort((f1, f2) -> {
            if (f1.getSize() != f2.getSize()) {
                return Integer.compare(f1.getSize(), f2.getSize());
            }
            int sum1 = f1.getVariables().stream().mapToInt(v -> v.getName().chars().sum()).sum();
            int sum2 = f2.getVariables().stream().mapToInt(v -> v.getName().chars().sum()).sum();
            return Integer.compare(sum1, sum2);
        });

        Factor result = factors.get(0);
        for (int i = 1; i < factors.size(); i++) {
            result = join(result, factors.get(i));
        }
        return result;
    }

    /**
     * Normalizes the factor by dividing each probability by the sum of all probabilities.
     *
     * @return A new factor with normalized probabilities.
     */
    public Factor normalize() {
        Counter counter = Counter.instance;
        // sum up all probabilities
        double sum = 0.0;
        for (double prob : factorTable.values()) {
            sum += prob;
        }
        counter.incrementSumCounter(factorTable.size() - 1);


        // create a new factor table for the normalized factor
        Map<List<String>, Double> normalizedFactorTable = new LinkedHashMap<>();
        for (Map.Entry<List<String>, Double> entry : factorTable.entrySet()) {
            List<String> combination = entry.getKey();
            double probability = entry.getValue() / sum; // normalize the probability
            normalizedFactorTable.put(combination, probability);
        }

        // create a new factor with the normalized factor table
        return new Factor(variables, normalizedFactorTable);
    }

    public int getSize() {
        return factorTable.size();
    }

    public List<Variable> getVariables() {
        return variables;
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
            sb.append(String.format("%.7f%n", entry.getValue()));
        }
        return sb.toString();
    }

}
