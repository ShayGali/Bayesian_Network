import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Ex1 {

    private static double[] parseProbabilities(String probabilities) {
        String[] probStrings = probabilities.split(" ");
        double[] probArray = new double[probStrings.length];
        for (int i = 0; i < probStrings.length; i++) {
            probArray[i] = Double.parseDouble(probStrings[i]);
        }
        return probArray;
    }

    public static BayesNet getBayesNet(String fileName) throws IOException, SAXException, ParserConfigurationException {

        BayesNet bayesNet = new BayesNet();

        File xmlFile = new File(fileName);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);

        doc.getDocumentElement().normalize();

        // get variable nodes and the outcomes
        NodeList variableNodes = doc.getElementsByTagName("VARIABLE");
        for (int i = 0; i < variableNodes.getLength(); i++) {
            Element variableElement = (Element) variableNodes.item(i);
            String variableName = variableElement.getElementsByTagName("NAME").item(0).getTextContent();
            List<String> outcomes = new ArrayList<>();
            NodeList outcomeNodes = variableElement.getElementsByTagName("OUTCOME");
            for (int j = 0; j < outcomeNodes.getLength(); j++) {
                String outcome = outcomeNodes.item(j).getTextContent();
                outcomes.add(outcome);
            }

            bayesNet.addVariable(variableName, outcomes);
        }

        // get the dependencies
        NodeList definitionNodes = doc.getElementsByTagName("DEFINITION");

        for (int i = 0; i < definitionNodes.getLength(); i++) {
            Element definitionElement = (Element) definitionNodes.item(i);
            String forVariableName = definitionElement.getElementsByTagName("FOR").item(0).getTextContent();

            // get the parents
            List<String> parents = new ArrayList<>();
            NodeList parentNodes = definitionElement.getElementsByTagName("GIVEN");
            for (int j = 0; j < parentNodes.getLength(); j++) {
                String parentName = parentNodes.item(j).getTextContent();
                parents.add(parentName);
            }

            // get the probabilities
            String probabilities = definitionElement.getElementsByTagName("TABLE").item(0).getTextContent();

            // add the dependency to the BayesNet
            bayesNet.addDependency(forVariableName, parents, parseProbabilities(probabilities));
        }

        return bayesNet;
    }

    public static void main(String[] args) {
        BayesNet bayesNet;
        // open the input.txt file
        try {
            BufferedReader reader = new BufferedReader(new FileReader("input.txt"));
            String fileName = reader.readLine();
            bayesNet = getBayesNet(fileName);
            reader.close();
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }


//        bayesNet.printVariables();
//        Variable A = bayesNet.getVariable("J");
//        Factor factorA = A.getFactor();
//        System.out.println(A.getParents().stream().map(Variable::getName).collect(Collectors.toList()));
//        System.out.println(factorA.toString());

//        String query = "P(B=F,E=T,A=T,M=T,J=F)";
        String query = "P(B=T|J=T,M=T),1";
//
        double res = bayesNet.answerQuery(query);
//         print the result with 5 decimal places rounded (if the remainder is 0.5 or more, round up, otherwise round down)
        System.out.printf("%s = %.5f\n", query, res);

    }

}
