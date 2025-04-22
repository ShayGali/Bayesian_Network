import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a variable and its outcome.
 * For example, if we have a variable A with outcomes {T, F},
 * then we can create a VariableOutcome object like this:
 * VariableOutcome outcome = new VariableOutcome(A, "T");
 */
public class VariableOutcome {
    Variable variable;
    String outcome;

    VariableOutcome(Variable variable, String outcome) {
        if (!variable.getOutcomes().contains(outcome)) {
            throw new IllegalArgumentException("Outcome " + outcome + " is not valid for variable " + variable.getName());
        }

        this.variable = variable;
        this.outcome = outcome;

    }

    /**
     * Get the probability of this variable given the parents.
     * All the relevant parents must be present in the given list.
     *
     * @param given the list of variables that are given.
     * @return the probability of this variable given the parents.
     */
    public double getProbability(List<VariableOutcome> given) {
        // remove all the `given` variables that are not in the `parents`
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < given.size(); i++) {
            VariableOutcome var = given.get(i);
            if (variable.getParents().contains(var.variable)) {
                indices.add(i);
            }
        }

        // if it contains all the parents, then we can calculate the probability directly from the factor
        if (indices.size() != variable.getParents().size()) {
            throw new IllegalArgumentException("Not all parents are present in the given variables");
        }
        // get the factor
        Factor factor = variable.getFactor();
        // get the probability
        List<VariableOutcome> vars = new ArrayList<>();
        for (int index : indices) {
            vars.add(given.get(index));
        }
        vars.add(this);
        // get the probability from the factor
        return factor.getProbability(vars);
    }

    @Override
    public String toString() {
        return "VariableOutcome{" +
                "variable=" + variable +
                ", outcome='" + outcome + '\'' +
                '}';
    }
}