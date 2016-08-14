package main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann;

import main.java.cz.cvut.ida.nesisl.api.logic.Fact;
import main.java.cz.cvut.ida.nesisl.api.logic.Literal;
import main.java.cz.cvut.ida.nesisl.api.logic.LiteralFactory;
import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by EL on 7.3.2016.
 */
public class RuleCreationWrapper {
    private Set<Fact> facts = new HashSet<>();
    private Set<Fact> inputFacts = new HashSet<>();
    private Set<Fact> intermediateFacts = new HashSet<>();
    private Set<Fact> conclusionFacts = new HashSet<>();
    private Map<Fact, Set<Pair<String, Boolean>>> rules = new HashMap<>();
    private LiteralFactory factory = new LiteralFactory();

    public RuleCreationWrapper() {
    }

    public Set<Fact> getFacts() {
        return facts;
    }

    public void setFacts(Set<Fact> facts) {
        this.facts = facts;
    }

    public Set<Fact> getInputFacts() {
        return inputFacts;
    }

    public void setInputFacts(Set<Fact> inputFacts) {
        this.inputFacts = inputFacts;
    }

    public Set<Fact> getIntermediateFacts() {
        return intermediateFacts;
    }

    public void setIntermediateFacts(Set<Fact> intermediateFacts) {
        this.intermediateFacts = intermediateFacts;
    }

    public Set<Fact> getConclusionFacts() {
        return conclusionFacts;
    }

    public void setConclusionFacts(Set<Fact> conclusionFacts) {
        this.conclusionFacts = conclusionFacts;
    }

    public Map<Fact, Set<Pair<String, Boolean>>> getRules() {
        return rules;
    }

    public void setRules(Map<Fact, Set<Pair<String, Boolean>>> rules) {
        this.rules = rules;
    }

    public LiteralFactory getFactory() {
        return factory;
    }

    public void setFactory(LiteralFactory factory) {
        this.factory = factory;
    }
}