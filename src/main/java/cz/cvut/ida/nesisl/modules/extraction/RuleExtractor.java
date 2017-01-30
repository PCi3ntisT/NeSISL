package main.java.cz.cvut.ida.nesisl.modules.extraction;

/**
 * Created by EL on 26.1.2017.
 */
public enum RuleExtractor {
    TREPAN, JRIP, NONE;

    public static RuleExtractor create(String string) {
        if("jrip".equals(string.toLowerCase())){
            return JRIP;
        }
        // else if "trepan".equals(string.toLowerCases())
        return TREPAN;
    }
}
