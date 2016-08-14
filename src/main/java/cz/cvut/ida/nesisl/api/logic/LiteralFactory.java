package main.java.cz.cvut.ida.nesisl.api.logic;

import main.java.cz.cvut.ida.nesisl.modules.tool.Pair;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by EL on 7.3.2016.
 */
public class LiteralFactory {

    public final String LITERALS_DELIMITER = ",";

    private final Map<String,Fact> factMap = new HashMap<>();
    private final Map<Pair<Fact,Boolean>,Literal> literalMap = new HashMap<>();

    public LiteralFactory(){}

    /**
     * * cannot parse n-true tye rules
     * @param factName
     * @return
     */
    public Literal getLiteral(String factName){
        Boolean positive = true;
        if(Pattern.matches("not\\(.+\\)", factName)){
            positive = false;
            factName = factName.substring("not(".length(),factName.length()-1);
        }
        factName = factName.replaceAll("\\s+","");
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

    /**
     * cannot parse n-true tye rules
     * expect to get only in form of 'a , c, not(d)'; or with dot at the end
     * @param body
     * @return
     */
    public Set<Literal> getLiterals(String body){
        body = body.trim();
        if(body.charAt(body.length()-1) == '.'){
            body = body.substring(0,body.length()-1);
        }
        return Arrays.stream(body.split(",")).map(literal -> getLiteral(literal.trim())).collect(Collectors.toSet());
    }

}