import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Variable {
    private final String name;
    private final List<String> outcomes;
    private List<Variable> parents;

    private Factor factor;

    public Variable(String name, List<String> outcomes) {
        this.name = name;
        this.outcomes = new ArrayList<>(outcomes);
        this.parents = new ArrayList<>();
        factor = null;
    }

    public String getName() {
        return name;
    }

    public void setCpt(double[] probabilities) {
        if(parents == null){
            throw new IllegalArgumentException("Parents are not set for variable " + name);
        }
        List<Variable> copyParents = new ArrayList<>(parents);
        copyParents.add(this);
        factor = new Factor(copyParents, probabilities);
    }

    public void setParents(List<Variable> parents) { // NEW
        this.parents = parents;
    }


    public List<String> getOutcomes() {
        return outcomes;
    }

    public List<Variable> getParents() {
        return parents;
    }

    public Factor getFactor() {
        return factor;
    }

    /**
     * check if this variable is a descendant of the given variable
     *
     * @param variable the variable that is can be a parent
     * @return true if this variable is a descendant of the given variable (the given variable is an ancestor of this variable)
     */
    public boolean isDescendantOf(Variable variable) {
        if (variable == null) return false;
        if (this == variable) return true;
        for (Variable parent : parents) {
            if (parent.isDescendantOf(variable)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Variable variable = (Variable) o;
        return Objects.equals(name, variable.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Variable{" +
                "name='" + name + '\'' +
                '}';
    }
}