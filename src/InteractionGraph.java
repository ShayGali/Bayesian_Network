import java.util.*;

public class InteractionGraph {
    private final Set<Variable> variables;
    private final Map<Variable, Set<Variable>> variableToNeighborsMap;

    public InteractionGraph(List<Factor> factors) {
        this.variables = new HashSet<>();
        this.variableToNeighborsMap = new HashMap<>();

        for (Factor factor : factors) {
            List<Variable> factorVariables = factor.getVariables();
            for (Variable variable : factorVariables) {
                if (!variableToNeighborsMap.containsKey(variable)) {
                    variableToNeighborsMap.put(variable, new HashSet<>());
                    variables.add(variable);
                }
                variableToNeighborsMap.get(variable).addAll(factorVariables);
                variableToNeighborsMap.get(variable).remove(variable);
            }
        }
    }

    // Minimum Degree heuristic
    public List<Variable> minDegreeOrder(Set<Variable> variablesToEliminate) {
        // Work on local copies to avoid mutating the graph
        Set<Variable> vars = new HashSet<>(variables);
        Map<Variable, Set<Variable>> neighborsMap = new HashMap<>();
        for (Map.Entry<Variable, Set<Variable>> entry : variableToNeighborsMap.entrySet()) {
            neighborsMap.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }

        List<Variable> order = new ArrayList<>();

        while (!vars.isEmpty()) {
            Variable minVariable = null;
            int minDegree = Integer.MAX_VALUE;

            for (Variable variable : vars) {
                int degree = neighborsMap.get(variable).size();
                if (degree < minDegree) {
                    minDegree = degree;
                    minVariable = variable;
                }
            }

            if (minVariable == null) break;
            order.add(minVariable);

            // Add fill-in edges between all neighbors of minVariable
            Set<Variable> neighbors = neighborsMap.get(minVariable);
            for (Variable v1 : neighbors) {
                for (Variable v2 : neighbors) {
                    if (!v1.equals(v2)) {
                        neighborsMap.get(v1).add(v2);
                        neighborsMap.get(v2).add(v1);
                    }
                }
            }
            // Remove minVariable from neighbors' sets
            for (Variable neighbor : neighbors) {
                neighborsMap.get(neighbor).remove(minVariable);
            }
            // Remove minVariable from graph
            neighborsMap.remove(minVariable);
            vars.remove(minVariable);
        }

        // Only return variables that are in variablesToEliminate
        List<Variable> eliminationOrder = new ArrayList<>();
        for (Variable variable : order) {
            if (variablesToEliminate.contains(variable)) {
                eliminationOrder.add(variable);
            }
        }
        return eliminationOrder;
    }

    // Minimum Fill heuristic
    public List<Variable> minFillOrder(Set<Variable> variablesToEliminate) {
        // Work on local copies to avoid mutating the graph
        Set<Variable> vars = new HashSet<>(variables);
        Map<Variable, Set<Variable>> neighborsMap = new HashMap<>();
        for (Map.Entry<Variable, Set<Variable>> entry : variableToNeighborsMap.entrySet()) {
            neighborsMap.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }

        List<Variable> order = new ArrayList<>();

        while (!vars.isEmpty()) {
            Variable minVariable = null;
            int minFill = Integer.MAX_VALUE;

            for (Variable variable : vars) {
                Set<Variable> neighbors = neighborsMap.get(variable);
                int fill = 0;
                // Count number of missing edges between neighbors
                List<Variable> neighborList = new ArrayList<>(neighbors);
                for (int i = 0; i < neighborList.size(); i++) {
                    for (int j = i + 1; j < neighborList.size(); j++) {
                        Variable v1 = neighborList.get(i);
                        Variable v2 = neighborList.get(j);
                        if (!neighborsMap.get(v1).contains(v2)) {
                            fill++;
                        }
                    }
                }
                if (fill < minFill) {
                    minFill = fill;
                    minVariable = variable;
                }
            }

            if (minVariable == null) break;
            order.add(minVariable);

            // Add fill-in edges between all neighbors of minVariable
            Set<Variable> neighbors = neighborsMap.get(minVariable);
            for (Variable v1 : neighbors) {
                for (Variable v2 : neighbors) {
                    if (!v1.equals(v2)) {
                        neighborsMap.get(v1).add(v2);
                        neighborsMap.get(v2).add(v1);
                    }
                }
            }
            // Remove minVariable from neighbors' sets
            for (Variable neighbor : neighbors) {
                neighborsMap.get(neighbor).remove(minVariable);
            }
            // Remove minVariable from graph
            neighborsMap.remove(minVariable);
            vars.remove(minVariable);
        }

        // Only return variables that are in variablesToEliminate
        List<Variable> eliminationOrder = new ArrayList<>();
        for (Variable variable : order) {
            if (variablesToEliminate.contains(variable)) {
                eliminationOrder.add(variable);
            }
        }
        return eliminationOrder;
    }
}
