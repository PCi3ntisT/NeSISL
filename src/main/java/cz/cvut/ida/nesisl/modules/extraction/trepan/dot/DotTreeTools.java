package main.java.cz.cvut.ida.nesisl.modules.extraction.trepan.dot;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by EL on 1.11.2016.
 */
public class DotTreeTools {

    public static final String IS_TRUE_WITH_SPACES = "= true"; // as it is in the output in case of only one antecedent in non terminal node
    public static final String IS_TRUE = "=true";
    public static final String IS_FALSE = "=false";
    public static final CharSequence DECISION_MARKER = "shape=box";
    public static final String EXPRESSION_LABEL_DELIMITER = ",";
    private static final String OF_DELIMITER = "of";

    private static final DotTreeTools base = new DotTreeTools();

    public static DotTreeTools getDefault() {
        return base;
    }

    public boolean isTerminal(DotNode node, DotTree tree) {
        return !tree.getEdges().containsKey(node);
    }

    public long getN(String label) {
        if (!label.contains(OF_DELIMITER)) {
            return 1;
        }
        int startIdx = label.indexOf("{");
        int endIndex = label.indexOf("}");
        return label.substring(startIdx, endIndex).split(EXPRESSION_LABEL_DELIMITER).length;
    }

    public long getM(String label) {
        if (!label.contains(OF_DELIMITER)) {
            return 1;
        }
        int startIdx = 0;
        int endIndex = label.indexOf(OF_DELIMITER);
        return Integer.valueOf(label.substring(startIdx, endIndex).trim());
    }

    public List<String> retrieveAntecedents(String label) {
        if (!label.contains(OF_DELIMITER)) {
            List<String> result = new ArrayList<>();
            result.add(label);
            return result;
        }

        int startIdx = label.indexOf("{") + 1;
        int endIndex = label.indexOf("}");

        String antecedents = label.substring(startIdx, endIndex);
        if (!antecedents.contains(EXPRESSION_LABEL_DELIMITER)) {
            List<String> result = new ArrayList<>();
            result.add(antecedents);
            return result;
        }
        return Arrays.stream(antecedents.split(EXPRESSION_LABEL_DELIMITER))
                .map(antecedent -> antecedent.trim())
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
