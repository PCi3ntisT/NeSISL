package main.java.cz.cvut.ida.nesisl.api.logic;

import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by EL on 7.3.2016.
 */
public class LiteralFactory {

    private final Map<String,Fact> factMap = new HashMap<>();
    private final Map<Pair<Fact,Boolean>,Literal> literalMap = new HashMap<>();

    public LiteralFactory(){}

    public Literal getLiteral(String factName){
        Boolean positive = true;
        if(Pattern.matches("not\\(.+\\)", factName)){
            positive = false;
            factName = factName.substring("not(".length(),factName.length()-1);
        }
        if(!factMap.containsKey(factName)) {
            factMap.put(factName, new Fact(factName));
        }
        Fact fact = factMap.get(factName);
        Pair<Fact, Boolean> pair = new Pair<>(fact, positive);
        if(!literalMap.containsKey(pair)){
            Literal literal = new Literal(pair);
            literalMap.put(pair,literal);
        }
        return literalMap.get(pair);
    }

}