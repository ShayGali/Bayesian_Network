import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BayesNet {
    HashMap<String, Variable> variables;

    public BayesNet() {
        this.variables = new HashMap<>();
    }

    /**
     * Adds a variable to the BayesNet. <b> Make sure to add the variable first, the function asserts that the variable and all parent variables are present in the BayesNet. </b>
     *
     * @param name     the name of the variable
     * @param outcomes the possible outcomes of the variable
     */
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
        }
        variable.setParents(parentVariables);
        variable.setCpt(probabilities);
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
        double res = variableList.get(0).getProbability(variableList); // get the probability of the first variable
        for (int i = 1; i < variableList.size(); i++) {
            res *= variableList.get(i).getProbability(variableList);
        }

        // we make `variableList.size() - 1` multiplications
        Counter.instance.incrementProductCounter(variableList.size() - 1);

        return res;
    }


    private double calculateProbForComplexQuery(String query) {
        char method = query.charAt(query.length() - 1); // get the method
        QueryParts qp = parseQueryAndEvidence(query, new HashSet<>(this.variables.values()));

        // check if we can get the probability only using the CPT of the query variable
        if (qp.queryOutcomes.size() == 1){
            // check if all the evidence variables are parents of the query variable
            Variable queryVar = qp.queryOutcomes.get(0).variable;
            boolean allEvidenceAreParents = true;
            for (VariableOutcome evidence : qp.evidenceOutcomes) {
                if (!queryVar.getParents().contains(evidence.variable)) {
                    allEvidenceAreParents = false;
                    break;
                }
            }
            if (allEvidenceAreParents) {
                // get the probability of the query variable given the evidence
                return qp.queryOutcomes.get(0).getProbability(qp.evidenceOutcomes);
            }
        }

        switch (method) {
            case '1':
                return calculateProbForComplexQueryMethod1(qp);
            case '2':
                return calculateProbForComplexQueryMethod2(qp);
            case '3':
                return calculateProbForComplexQueryMethod3(qp);
            default:
                throw new IllegalArgumentException("Invalid method: " + method);
        }
    }

    private double calculateProbForComplexQueryMethod1(QueryParts qp) {
        // get all combinations of the hidden variables and the query variables
        List<List<VariableOutcome>> hiddenCombos = getAllVariableOutcomes(qp.hiddenVar);
        List<List<VariableOutcome>> queryCombos = getAllVariableOutcomes(qp.queryVar);

        double numerator = 0.0; // the probability of the query & evidence
        double denominator = 0.0; // the probability of the (query & evidence) + (!query & evidence)

        // go through all combinations of the hidden variables and the query variables (the evidence is fixed)
        // and calculate the joint probability for each combination
        for (List<VariableOutcome> queryCombo : queryCombos) {
            for (List<VariableOutcome> hiddenCombo : hiddenCombos) {
                // get the full assignment of the variables
                List<VariableOutcome> fullAssignment = new ArrayList<>();
                fullAssignment.addAll(hiddenCombo);
                fullAssignment.addAll(qp.evidenceOutcomes);
                fullAssignment.addAll(queryCombo);
                // calculate the joint probability for the full assignment
                double prob = calculateJointProbabilityFromVarOutcomeList(fullAssignment);

                // if the query matches the evidence, add to the numerator
                if (matchesQuery(queryCombo, qp.queryOutcomes)) {
                    if (numerator > 0) {
                        Counter.instance.incrementSumCounter();
                    }
                    numerator += prob;
                } else { // we will add the numerator to the denominator in the end
                    if (denominator > 0) {
                        Counter.instance.incrementSumCounter();
                    }
                    denominator += prob;
                }

            }
        }

        denominator += numerator;
        Counter.instance.incrementSumCounter();

        // return the normalized probability
        return numerator / denominator;
    }

    private double calculateProbForComplexQueryMethod2(QueryParts qp) {
        // sort the `hiddenVars` by the variable name
        List<Variable> sortedHiddenVars = qp.hiddenVar.stream()
                .sorted(Comparator.comparing(Variable::getName))
                .collect(Collectors.toList());

        // start the variable elimination
        List<Variable> relevantHiddenVars = filterRelevantHiddenVars(qp.queryOutcomes, qp.evidenceOutcomes, sortedHiddenVars);
        List<Factor> factors = collectInitialFactors(relevantHiddenVars, qp.queryOutcomes, qp.evidenceOutcomes);

        factors = setEvidenceOnFactors(factors, qp.evidenceOutcomes);

        factors = eliminateHiddenVariables(factors, relevantHiddenVars);

        Factor finalFactor = Factor.join(factors);
        finalFactor = finalFactor.normalize();
        return finalFactor.getProbability(qp.queryOutcomes);
    }

    private double calculateProbForComplexQueryMethod3(QueryParts qp) {
        List<Variable> relevantHiddenVars = filterRelevantHiddenVars(qp.queryOutcomes, qp.evidenceOutcomes, new ArrayList<>(qp.hiddenVar));
        List<Factor> factors = collectInitialFactors(relevantHiddenVars, qp.queryOutcomes, qp.evidenceOutcomes);

        factors = setEvidenceOnFactors(factors, qp.evidenceOutcomes);

        // get the order of the hidden variables
        InteractionGraph ig = new InteractionGraph(factors);
        List<Variable> orderedHiddenVars = ig.minFillOrder(new HashSet<>(relevantHiddenVars));

        factors = eliminateHiddenVariables(factors, orderedHiddenVars);

        Factor finalFactor = Factor.join(factors);
        finalFactor = finalFactor.normalize();
        return finalFactor.getProbability(qp.queryOutcomes);
    }

    private double variableElimination(List<VariableOutcome> queryOutcomes, List<VariableOutcome> evidenceOutcomes, List<Variable> orderedHiddenVars) {
        List<Variable> relevantHiddenVars = filterRelevantHiddenVars(queryOutcomes, evidenceOutcomes, orderedHiddenVars);
        List<Factor> factors = collectInitialFactors(relevantHiddenVars, queryOutcomes, evidenceOutcomes);

        factors = setEvidenceOnFactors(factors, evidenceOutcomes);

        factors = eliminateHiddenVariables(factors, relevantHiddenVars);

        Factor finalFactor = Factor.join(factors);
        finalFactor = finalFactor.normalize();
        return finalFactor.getProbability(queryOutcomes);
    }


    /**
     * Helper method to filter the relevant hidden variables based on the query and evidence outcomes.
     *
     * @param queryOutcomes     the query variable outcomes
     * @param evidenceOutcomes  the evidence variable outcomes
     * @param orderedHiddenVars the ordered list of hidden variables
     * @return a list of relevant hidden variables (the order is preserved)
     */
    private List<Variable> filterRelevantHiddenVars(List<VariableOutcome> queryOutcomes, List<VariableOutcome> evidenceOutcomes, List<Variable> orderedHiddenVars) {
        boolean[] isHiddenRelevant = new boolean[orderedHiddenVars.size()];
        for (VariableOutcome vo : queryOutcomes) {
            for (int i = 0; i < orderedHiddenVars.size(); i++) {
                if (!isHiddenRelevant[i] && vo.variable.isDescendantOf(orderedHiddenVars.get(i))) {
                    isHiddenRelevant[i] = true;
                }
            }
        }
        for (VariableOutcome vo : evidenceOutcomes) {
            for (int i = 0; i < orderedHiddenVars.size(); i++) {
                if (!isHiddenRelevant[i] && vo.variable.isDescendantOf(orderedHiddenVars.get(i))) {
                    isHiddenRelevant[i] = true;
                }
            }
        }

        List<Variable> filtered = new ArrayList<>();
        for (int i = 0; i < orderedHiddenVars.size(); i++) {
            if (isHiddenRelevant[i]) {
                filtered.add(orderedHiddenVars.get(i));
            }
        }
        return filtered;
    }


    /**
     * Helper method to collect the factors for given variables.
     *
     * @param relevantHiddenVars the list of relevant hidden variables
     * @param queryOutcomes      the query variable outcomes
     * @param evidenceOutcomes   the evidence variable outcomes
     * @return a list of factors for the relevant hidden variables, query outcomes, and evidence outcomes
     */
    private List<Factor> collectInitialFactors(List<Variable> relevantHiddenVars, List<VariableOutcome> queryOutcomes, List<VariableOutcome> evidenceOutcomes) {
        List<Factor> factors = new ArrayList<>();
        for (Variable v : relevantHiddenVars) {
            factors.add(v.getFactor());
        }
        for (VariableOutcome vo : evidenceOutcomes) {
            factors.add(vo.variable.getFactor());
        }
        for (VariableOutcome vo : queryOutcomes) {
            factors.add(vo.variable.getFactor());
        }
        return factors;
    }

    /**
     * Hlper method to set evidence on the factors.
     *
     * @param factors          the list of factors
     * @param evidenceOutcomes the evidence outcomes
     * @return a list of updated factors with evidence set
     */
    private List<Factor> setEvidenceOnFactors(List<Factor> factors, List<VariableOutcome> evidenceOutcomes) {
        List<Factor> updatedFactors = new ArrayList<>();
        for (Factor factor : factors) {
            Factor updatedFactor = factor.setEvidences(evidenceOutcomes);
            if (updatedFactor.getSize() > 1) { // only keep factors with more than one row
                updatedFactors.add(updatedFactor);
            }
        }
        return updatedFactors;
    }


    /**
     * Eliminates hidden variables from the list of factors.
     *
     * @param factors    the list of factors
     * @param hiddenVars the list of hidden variables to eliminate
     * @return the updated list of factors after eliminating the hidden variables
     */
    private List<Factor> eliminateHiddenVariables(List<Factor> factors, List<Variable> hiddenVars) {
        for (Variable hiddenVariable : hiddenVars) {
            List<Factor> factorsWithHiddenVar = new ArrayList<>();
            List<Factor> factorsWithoutHiddenVar = new ArrayList<>();
            for (Factor factor : factors) {
                if (factor.getVariables().contains(hiddenVariable)) {
                    factorsWithHiddenVar.add(factor);
                } else {
                    factorsWithoutHiddenVar.add(factor);
                }
            }
            Factor joinedFactor = Factor.join(factorsWithHiddenVar);
            Factor joinedFactorEliminated = joinedFactor.eliminate(hiddenVariable);
            if (joinedFactorEliminated.getSize() > 1) { // only keep factors with more than one row
                factorsWithoutHiddenVar.add(joinedFactorEliminated);
            }
            factors = factorsWithoutHiddenVar;
        }
        return factors;
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


    /**
     * Helper method to get all combinations of the variable outcomes for the given variable names.
     *
     * @param variableNames the names of the variables
     * @return a list of all combinations of the variable outcomes
     */
    private List<List<VariableOutcome>> getAllVariableOutcomes(Set<Variable> variableNames) {
        Stream<List<VariableOutcome>> prod = Stream.of(Collections.emptyList());
        for (Variable var : variableNames) {
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

    /**
     * Helper class to hold parsed query and evidence parts.
     * It contains the query outcomes, evidence outcomes, and their respective variable names.
     * This is used to simplify the parsing and matching process.
     */
    private static class QueryParts {
        List<VariableOutcome> queryOutcomes;
        List<VariableOutcome> evidenceOutcomes;
        Set<Variable> queryVar;
        Set<Variable> hiddenVar;

        QueryParts(List<VariableOutcome> q, List<VariableOutcome> e, Set<Variable> variables) {
            this.queryOutcomes = q;
            this.evidenceOutcomes = e;
            this.queryVar = new HashSet<>();
            this.hiddenVar = new HashSet<>(variables);
            for (VariableOutcome vo : q) {
                this.queryVar.add(vo.variable);
                this.hiddenVar.remove(vo.variable);
            }
            for (VariableOutcome vo : e) {
                this.hiddenVar.remove(vo.variable);
            }

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
     * Parses the query and evidence from the given string.
     * The query is expected to be in the format "P(X=F,Y=T|Z=T,W=F),n". Where n in 1,2,3 (the method), and the outcomes need to be part of the variable possible outcomes.
     *
     * @param query     the query string
     * @param variables the set of variables in the BayesNet
     * @return a QueryParts object containing the query and evidence outcomes
     */
    private QueryParts parseQueryAndEvidence(String query, Set<Variable> variables) {
        String stripped = query.substring(2, query.length() - 3);
        String[] parts = stripped.split("\\|");
        String queryPart = parts[0].trim();
        List<VariableOutcome> evidenceOutcomes;
        List<VariableOutcome> queryOutcomes = parseVariableOutcomes(queryPart.split(","));
        if (parts.length == 1) {
            evidenceOutcomes = new ArrayList<>();
        } else {
            String evidencePart = parts[1].trim();
            evidenceOutcomes = parseVariableOutcomes(evidencePart.split(","));
        }

        return new QueryParts(queryOutcomes, evidenceOutcomes, variables);
    }
}
