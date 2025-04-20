import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Variable {
    private final String name;
    private List<String> outcomes;
    private List<Variable> parents;
    private Set<Variable> children;

    private double[] probabilities;

    private Factor factor;

    public Variable(String name, List<String> outcomes) {
        this.name = name;
        this.outcomes = outcomes;
        this.parents = new ArrayList<>();
        this.children = new HashSet<>();
        this.probabilities = null;
        factor = null;
    }

    public String getName() {
        return name;
    }

    public void setProbabilities(double[] probabilities) {
        this.probabilities = probabilities;
    }

    public void setParents(List<Variable> parents) { // NEW
        this.parents = parents;
    }

    public void addChildren(Variable child) {
        this.children.add(child);
    }

    public List<String> getOutcomes() {
        return outcomes;
    }

    public List<Variable> getParents() {
        return parents;
    }

    public Factor getFactor() {
        if (factor == null) {
            // create a new factor with copy of the variables and this element, and the probabilities
            List<Variable> copyParents = new ArrayList<>(parents);
            copyParents.add(this);
            factor = new Factor(copyParents, probabilities);
        }
        return factor;
    }

    @Override
    public String toString() {
        return "Variable{" +
                "name='" + name + '\'' +
                '}';
    }
}