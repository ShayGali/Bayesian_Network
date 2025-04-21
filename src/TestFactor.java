import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFactor {

    public static <T> List<T> ListOf(T... elements) {
        List<T> list = new ArrayList<>();
        for (T element : elements) {
            list.add(element);
        }
        return list;
    }

    @Test
    void eliminateTest() {
        List<String> outcomes = ListOf("T", "F");
        Variable E = new Variable("E", outcomes);
        Variable B = new Variable("B", outcomes);
        Variable A = new Variable("A", outcomes);

        // Test the eliminate method
        List<Variable> variables = ListOf(E, B, A);

        Map<List<String>, Double> factorTable = new LinkedHashMap<>();
        factorTable.put(ListOf("T", "T", "T"), 0.5985);
        factorTable.put(ListOf("T", "F", "T"), 0.1827);
        factorTable.put(ListOf("F", "T", "T"), 0.5922);
        factorTable.put(ListOf("F", "F", "T"), 0.00063);
        factorTable.put(ListOf("T", "T", "F"), 0.000025);
        factorTable.put(ListOf("T", "F", "F"), 0.000355);
        factorTable.put(ListOf("F", "T", "F"), 0.00003);
        factorTable.put(ListOf("F", "F", "F"), 0.0004995);

        Factor factor = new Factor(variables, factorTable);
        Factor eliminatedFactor = factor.eliminate(A);
        assertEquals(0.5985250, eliminatedFactor.getProbability(ListOf(new VariableOutcome(E, "T"), new VariableOutcome(B, "T"))), 0.0001);
        assertEquals(0.1830550, eliminatedFactor.getProbability(ListOf(new VariableOutcome(E, "T"), new VariableOutcome(B, "F"))), 0.0001);
        assertEquals(0.5922300, eliminatedFactor.getProbability(ListOf(new VariableOutcome(E, "F"), new VariableOutcome(B, "T"))), 0.0001);
        assertEquals(0.0011295, eliminatedFactor.getProbability(ListOf(new VariableOutcome(E, "F"), new VariableOutcome(B, "F"))), 0.0001);
    }

    @Test
    void testJoin() {
        // create variables
        List<String> outcomes = ListOf("T", "F");
        Variable A = new Variable("A", outcomes);
        Variable B = new Variable("B", outcomes);
        Variable E = new Variable("E", outcomes);

        // create f1
        Map<List<String>, Double> f1Table = new LinkedHashMap<>();
        f1Table.put(ListOf("T"), 0.63);
        f1Table.put(ListOf("F"), 0.0005);

        // create f2
        Map<List<String>, Double> f2Table = new LinkedHashMap<>();
        f2Table.put(ListOf("T", "T", "T"), 0.95);
        f2Table.put(ListOf("T", "F", "T"), 0.29);
        f2Table.put(ListOf("F", "T", "T"), 0.94);
        f2Table.put(ListOf("F", "F", "T"), 0.001);
        f2Table.put(ListOf("T", "T", "F"), 0.05);
        f2Table.put(ListOf("T", "F", "F"), 0.71);
        f2Table.put(ListOf("F", "T", "F"), 0.06);
        f2Table.put(ListOf("F", "F", "F"), 0.999);

        // create factors
        Factor f1 = new Factor(ListOf(A), f1Table);
        Factor f2 = new Factor(ListOf(E, B, A), f2Table);

        Factor newFactor = Factor.join(f1, f2);

        // create expected result (the order is E, B, A)
        List<Variable> expectedVariables = ListOf(E, B, A);
        Map<List<String>, Double> expectedFactorTable = new LinkedHashMap<>();
        expectedFactorTable.put(ListOf("T", "T", "T"), 0.5985);
        expectedFactorTable.put(ListOf("T", "F", "T"), 0.1827);
        expectedFactorTable.put(ListOf("F", "T", "T"), 0.5922);
        expectedFactorTable.put(ListOf("F", "F", "T"), 0.00063);
        expectedFactorTable.put(ListOf("T", "T", "F"), 0.000025);
        expectedFactorTable.put(ListOf("T", "F", "F"), 0.000355);
        expectedFactorTable.put(ListOf("F", "T", "F"), 0.00003);
        expectedFactorTable.put(ListOf("F", "F", "F"), 0.0004995);

        for (Map.Entry<List<String>, Double> entry : expectedFactorTable.entrySet()) {
            List<String> key = entry.getKey();
            Double value = entry.getValue();
            // create a new list of VariableOutcome
            List<VariableOutcome> variableOutcomes = new ArrayList<>();
            for (int i = 0; i < key.size(); i++) {
                variableOutcomes.add(new VariableOutcome(expectedVariables.get(i), key.get(i)));
            }

            // check if the new factor has the same probability
            assertEquals(value, newFactor.getProbability(variableOutcomes), 0.0001);
        }
    }

    @Test
    void testSetEvidence1(){
        List<String> outcomes = ListOf("T", "F");
        Variable E = new Variable("E", outcomes);
        Variable B = new Variable("B", outcomes);
        Variable A = new Variable("A", outcomes);

        List<Variable> variables = ListOf(E, B, A);

        Map<List<String>, Double> factorTable = new LinkedHashMap<>();
        factorTable.put(ListOf("T", "T", "T"), 0.5985);
        factorTable.put(ListOf("T", "F", "T"), 0.1827);
        factorTable.put(ListOf("F", "T", "T"), 0.5922);
        factorTable.put(ListOf("F", "F", "T"), 0.00063);
        factorTable.put(ListOf("T", "T", "F"), 0.000025);
        factorTable.put(ListOf("T", "F", "F"), 0.000355);
        factorTable.put(ListOf("F", "T", "F"), 0.00003);
        factorTable.put(ListOf("F", "F", "F"), 0.0004995);

        Factor factor = new Factor(variables, factorTable);
        List<VariableOutcome> evidences = ListOf(new VariableOutcome(E, "T"));
        Factor factorWithEvidence = factor.setEvidences(evidences);


        assertEquals(0.5985, factorWithEvidence.getProbability(ListOf(new VariableOutcome(B, "T"), new VariableOutcome(A, "T"))), 0.0001);
        assertEquals(0.1827, factorWithEvidence.getProbability(ListOf(new VariableOutcome(B, "F"), new VariableOutcome(A, "T"))), 0.0001);
        assertEquals(0.000025, factorWithEvidence.getProbability(ListOf(new VariableOutcome(B, "T"), new VariableOutcome(A, "F"))), 0.0001);
        assertEquals(0.000355, factorWithEvidence.getProbability(ListOf(new VariableOutcome(B, "F"), new VariableOutcome(A, "F"))), 0.0001);

    }

    @Test
    void testSetEvidence2() {
        List<String> outcomes = ListOf("T", "F");
        Variable A = new Variable("A", outcomes);
        Variable B = new Variable("B", outcomes);
        Variable C = new Variable("C", outcomes);
        Variable D = new Variable("D", outcomes);

        List<Variable> variables = ListOf(A, B, C, D);
        Map<List<String>, Double> factorTable = new LinkedHashMap<>();
        factorTable.put(ListOf("T", "T", "T", "T"), 0.1);
        factorTable.put(ListOf("T", "T", "T", "F"), 0.2);
        factorTable.put(ListOf("T", "T", "F", "T"), 0.3);
        factorTable.put(ListOf("T", "T", "F", "F"), 0.4);
        factorTable.put(ListOf("T", "F", "T", "T"), 0.5);
        factorTable.put(ListOf("T", "F", "T", "F"), 0.6);
        factorTable.put(ListOf("T", "F", "F", "T"), 0.7);
        factorTable.put(ListOf("T", "F", "F", "F"), 0.8);
        factorTable.put(ListOf("F", "T", "T", "T"), 0.9);
        factorTable.put(ListOf("F", "T", "T", "F"), 0.01);
        factorTable.put(ListOf("F", "T", "F", "T"), 0.02);
        factorTable.put(ListOf("F", "T", "F", "F"), 0.03);
        factorTable.put(ListOf("F", "F", "T", "T"), 0.04);
        factorTable.put(ListOf("F", "F", "T", "F"), 0.05);
        factorTable.put(ListOf("F", "F", "F", "T"), 0.06);
        factorTable.put(ListOf("F", "F", "F", "F"), 0.07);
        Factor factor = new Factor(variables, factorTable);
        Factor newFactor =  factor.setEvidences(ListOf(new VariableOutcome(A, "T"), new VariableOutcome(B, "F")));
        assertEquals(0.5, newFactor.getProbability(ListOf(new VariableOutcome(C, "T"), new VariableOutcome(D, "T"))), 0.0001);
        assertEquals(0.6, newFactor.getProbability(ListOf(new VariableOutcome(C, "T"), new VariableOutcome(D, "F"))), 0.0001);
        assertEquals(0.7, newFactor.getProbability(ListOf(new VariableOutcome(C, "F"), new VariableOutcome(D, "T"))), 0.0001);
        assertEquals(0.8, newFactor.getProbability(ListOf(new VariableOutcome(C, "F"), new VariableOutcome(D, "F"))), 0.0001);
    }

}
