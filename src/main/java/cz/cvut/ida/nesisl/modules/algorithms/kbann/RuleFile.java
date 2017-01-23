package main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann;

import main.java.cz.cvut.ida.nesisl.api.data.Dataset;
import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.api.logic.Literal;
import main.java.cz.cvut.ida.nesisl.modules.dataset.DatasetImpl;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by EL on 1.3.2016.
 */
public class RuleFile {

    public static final String INPUT_FACTS_HEADER = "INPUT FACTS";
    public static final String OUTPUT_FACTS_HEADER = "OUTPUT FACTS";
    public static final String RULES = "RULES";

    public static final String N_TRUE = "n-true";
    public static final String UNCHANGABLE_RULE = "::-";
    public static final String CHANGABLE_RULE = ":-";
    public static final String RULE_ENDING_TOKEN = "."; // this should be used in the parser but is not
    public static final String NOT_TOKEN = "not"; // this should be used in the parser but is not
    public static final String NOT_TOKEN_OPENING_BRACKET = "(";
    public static final String NOT_TOKEN_CLOSING_BRACKET = ")";
    public static final String ANTECEDENTS_DELIMITER = ",";

    public Rules preprocessRules() {
        if (!this.isAcyclic()) {
            throw new IllegalStateException("Cannot make KBANN network from cyclic theory.");
        }
        return Rules.create(this);
    }

    private enum ReadingState {
        INPUT_FACTS_HEADER, OUTPUT_FACTS_HEADER, RULES
    }

    private final Set<Fact> facts;
    private final Set<Fact> inputFacts;
    private final Set<Fact> intermediateFacts;
    private final Set<Fact> conclusionFacts;
    private final Map<Fact, Set<Pair<String, Boolean>>> rules;

    private RuleFile(Set<Fact> facts, Set<Fact> inputFacts, Set<Fact> intermediateFacts, Set<Fact> conclusionFacts, Map<Fact, Set<Pair<String, Boolean>>> rules) {
        this.facts = facts;
        this.inputFacts = inputFacts;
        this.intermediateFacts = intermediateFacts;
        this.conclusionFacts = conclusionFacts;
        this.rules = rules;
    }

    public Set<Fact> getFacts() {
        return Collections.unmodifiableSet(facts);
    }

    public Set<Fact> getInputFacts() {
        return Collections.unmodifiableSet(inputFacts);
    }

    public Set<Fact> getIntermediateFacts() {
        return Collections.unmodifiableSet(intermediateFacts);
    }

    public Set<Fact> getConclusionFacts() {
        return Collections.unmodifiableSet(conclusionFacts);
    }

    public Map<Fact, Set<Pair<String, Boolean>>> getRules() {
        return Collections.unmodifiableMap(rules);
    }

    public void addFact(Fact fact) {
        facts.add(fact);
    }

    public static RuleFile create(File file, Dataset dataset) {
        RuleCreationWrapper wrapper = new RuleCreationWrapper();
        wrapper.getInputFacts().addAll(dataset.getInputFactOrder());
        wrapper.getConclusionFacts().addAll(dataset.getOutputFactOrder());
       try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            ReadingState state = null;
            boolean change = false;
            while ((line = br.readLine()) != null) {
                if (line.trim().length() < 1 || (
                        line.trim().charAt(0) == DatasetImpl.COMMENTED_LINE_START)) {
                    continue;
                }
                wrapper = readRule(line, wrapper);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new RuleFile(wrapper.getFacts(), wrapper.getInputFacts(), wrapper.getIntermediateFacts(), wrapper.getConclusionFacts(), wrapper.getRules());
    }


    // creator for (old) formating of artificial dataset
    /*
    private static RuleCreationWrapper addImplication(String line, RuleCreationWrapper wrapper) {
        Fact head = retrieveHeadFromRule(line, wrapper);
        Set<Literal> body = retrieveBodyRule(line, wrapper);
        boolean isModifiable = retrieveModifiablitiy(line);

        if (!wrapper.getRules().containsKey(head)) {
            wrapper.getRules().put(head, new HashSet<>());
        }
        wrapper.getRules().get(head).add(new Pair<>(body, isModifiable));
        return wrapper;
    }

    public static RuleFile create(File file) {
        RuleCreationWrapper wrapper = new RuleCreationWrapper();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            ReadingState state = null;
            boolean change = false;
            while ((line = br.readLine()) != null) {
                switch (line) {
                    case INPUT_FACTS_HEADER:
                        state = ReadingState.INPUT_FACTS_HEADER;
                        change = true;
                        break;
                    case OUTPUT_FACTS_HEADER:
                        state = ReadingState.OUTPUT_FACTS_HEADER;
                        change = true;
                        break;
                    case RULES:
                        state = ReadingState.RULES;
                        change = true;
                        break;
                    default:
                        change = false;
                        break;
                }

                if (change) {
                    change = false;
                    continue;
                }
                wrapper = processLineAccordingToState(state, line, wrapper);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new RuleFile(wrapper.getFacts(), wrapper.getInputFacts(), wrapper.getIntermediateFacts(), wrapper.getConclusionFacts(), wrapper.getRules());
    }

    private static RuleCreationWrapper processLineAccordingToState(ReadingState state, String line, RuleCreationWrapper wrapper) {
        switch (state) {
            case INPUT_FACTS_HEADER:
                wrapper = addInputFact(line, wrapper);
                break;
            case OUTPUT_FACTS_HEADER:
                wrapper = addOutputFact(line, wrapper);
                break;
            case RULES:
                wrapper = addImplication(line, wrapper);
                break;
            default:
                throw new IllegalStateException("Proccesing line within uknown status.");
        }
        return wrapper;
    }
*/

    private static RuleCreationWrapper addInputFact(String line, RuleCreationWrapper wrapper) {
        Literal literal = wrapper.getFactory().getLiteral(line.trim());
        wrapper.getFacts().add(literal.getFact());
        wrapper.getInputFacts().add(literal.getFact());
        return wrapper;
    }

    private static RuleCreationWrapper readRule(String line, RuleCreationWrapper wrapper) {
        Fact head = retrieveHeadFromRule(line, wrapper);
        String body = retrieveBodyRule(line, wrapper);
        boolean isModifiable = retrieveModifiablitiy(line);

        if (!wrapper.getRules().containsKey(head)) {
            wrapper.getRules().put(head, new HashSet<>());
        }

        if (body.trim().equals(".")) {
            // awful hack because of malformed (zero rule) theories.
            return wrapper;
        }
        wrapper.getRules().get(head).add(new Pair<>(body, isModifiable));
        return wrapper;
    }

    private static boolean retrieveModifiablitiy(String line) {
        return !line.contains(UNCHANGABLE_RULE);
    }

    private static String retrieveBodyRule(String line, RuleCreationWrapper wrapper) {
        String[] splitted = line.split(UNCHANGABLE_RULE);
        if (splitted.length != 2) {
            splitted = line.split(CHANGABLE_RULE);
        }
        if (splitted.length != 2) {
            throw new IllegalStateException("This line violates rule notation.");
        }
        return splitted[1];
    }

/* (old) artificial domain format
  private static Set<Literal> retrieveBodyRule(String line, RuleCreationWrapper wrapper) {
        String[] splitted = line.split(":-");
        if (splitted.length != 2) {
            throw new IllegalStateException("This line violates rule notation.");
        }

        String body = "";
        if (splitted[1].charAt(splitted[1].length() - 1) == '.') {
            body = splitted[1].substring(0, splitted[1].length() - 1);
        } else {
            splitted = splitted[1].split("\\.");
            System.out.println(splitted.length);
            if (splitted.length != 2) {
                throw new IllegalStateException("This line violates rule notation. '" + line + "'");
            }
            body = splitted[0];
        }

        Set<Literal> bodySet = new HashSet<>();
        String[] facts = body.split(",");

        for (int idx = 0; idx < facts.length; idx++) {
            Literal literal = wrapper.getFactory().getLiteral(facts[idx].trim());
            Fact fact = literal.getFact();
            wrapper = addIntermediateIfNeeded(fact, wrapper);
            bodySet.add(literal);
        }

        if (bodySet.isEmpty()) {
            throw new UnsupportedOperationException("Rules with empty body not implemented.");
        }
        return bodySet;
    }*/

    private static RuleCreationWrapper addIntermediateIfNeeded(Fact fact, RuleCreationWrapper wrapper) {
        if (!wrapper.getFacts().contains(fact)) {
            wrapper.getFacts().add(fact);
            wrapper.getIntermediateFacts().add(fact);
        }
        return wrapper;
    }

    private static Fact retrieveHeadFromRule(String line, RuleCreationWrapper wrapper) {
        String[] splitted = line.split(UNCHANGABLE_RULE);
        if (splitted.length != 2) {
            splitted = line.split(CHANGABLE_RULE);
        }
        if (splitted.length != 2) {
            throw new IllegalStateException("This line violates rule notation.");
        }
        Literal literal = wrapper.getFactory().getLiteral(splitted[0].trim());


        //System.out.println(line);
        //System.out.println(literal);
        if (literal.getFact().getFact().length() < DatasetImpl.CLASS_TOKEN.length()
                || !literal.getFact().getFact().substring(0, DatasetImpl.CLASS_TOKEN.length()).equals(DatasetImpl.CLASS_TOKEN.length())) {
            addIntermediateIfNeeded(literal.getFact(), wrapper);
        }

        if (!literal.isPositive()) {
            throw new UnsupportedOperationException("Head negation not implemented so far.");
        }
        return literal.getFact();
    }

    private static RuleCreationWrapper addOutputFact(String line, RuleCreationWrapper wrapper) {
        Literal literal = wrapper.getFactory().getLiteral(line.trim());
        wrapper.getFacts().add(literal.getFact());
        wrapper.getConclusionFacts().add(literal.getFact());
        return wrapper;
    }

    public boolean isAcyclic() {
        Stack<Fact> stack = new Stack<>();
        Set<Fact> origins = new HashSet<>();
        origins.addAll(conclusionFacts);
        origins.addAll(intermediateFacts);

        Set<Fact> cache = new HashSet<>();

        for (Fact origin : origins) {
            if (!rules.containsKey(origin) || cache.contains(origin)) {
                cache.add(origin);
                continue;
            }
            if (isCyclic(origin, stack, cache)) {
                return false;
            }
        }
        return true;
    }

    private boolean isCyclic(Fact origin, Stack<Fact> stack, Set<Fact> cache) {
        if (!rules.containsKey(origin) || cache.contains(origin)) {
            cache.add(origin);
            return false;
        }

        if (stack.contains(origin)) {
            return true;
        }
        stack.push(origin);

        for (Pair<String, Boolean> pair : rules.get(origin)) {
            for (String antecedent : retrieveLiteralOnly(pair.getLeft())) {
                if (isCyclic(new Fact(antecedent), stack, cache)) {
                    return true;
                }
            }
        }

        stack.pop();
        cache.add(origin);
        return false;
    }

    // awfull
    // can process only one flat rules (not capable of (n-true 5 (3,4,5,6,7, not(8), n-of 2 (..,..)))
    private List<String> retrieveLiteralOnly(String ruleBody) {
        List<String> result = new ArrayList<>();
        String trimmed = ruleBody.trim();

        String[] splitted;
        if (trimmed.contains(N_TRUE + " ")) {
            int start = trimmed.indexOf(N_TRUE);
            String tripped = trimmed.substring(start);
            start = tripped.indexOf("(");
            int end = trimmed.indexOf(")");
            splitted = tripped.substring(start + 1, end).split(ANTECEDENTS_DELIMITER);
        } else {
            splitted = trimmed.split(ANTECEDENTS_DELIMITER);
        }

        Arrays.stream(splitted).forEach(token -> result.add(retrieveProposition(token.trim())));

        return result;
    }

    private String retrieveProposition(String token) {
        if (Pattern.matches(NOT_TOKEN + "\\(.+\\)", token)) {
            token = token.substring((NOT_TOKEN + "(").length(), token.length() - 1);
        }
        return token.replaceAll("\\s+", "");
    }

}




