package main.java.cz.cvut.ida.nesisl.modules.weka.rules;

import main.java.cz.cvut.ida.nesisl.modules.neural.algorithms.kbann.RuleFile;
import main.java.cz.cvut.ida.nesisl.modules.dataset.DatasetImpl;
import main.java.cz.cvut.ida.nesisl.modules.dataset.attributes.BinaryAttribute;
import main.java.cz.cvut.ida.nesisl.modules.dataset.attributes.ClassAttribute;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by EL on 8.9.2016.
 */
public class RuleSetToTheory {


    public static void main(String[] args) {
        System.out.println("test prevodu z rule setu do teorie");
        String input = "JRIP rules:\n" +
                "===========\n" +
                "\n" +
                "(3 = y) and (10 = n) => 16=republican (138.0/3.0)\n" +
                "(3 = y) and (15 = y) => 16=republican (19.0/3.0)\n" +
                "(3 = y) and (2 = n) => 16=republican (16.0/4.0)\n" +
                " => 16=democrat (262.0/5.0)\n" +
                "\n" +
                "Number of Rules : 4";

        /*input = "(3 = y) and (10 = n) => 16=republican (138.0/3.0)\n" +
                "(3 = y) and (15 = y) => 16=republican (19.0/3.0)\n" +
                "(3 = y) and (2 = n) => 16=republican (16.0/4.0)\n" +
                "(5 = n) => 16=democrat (262.0/5.0)\n" +
                "(5 = n) => 16=sanders (262.0/5.0)\n" +
                "(5 = y) and (6 = n) => 16=algore (262.0/5.0)\n" +
                " => 16=mrMackie (262.0/5.0)\n"
                ;*/

        /*RuleSet ruleSet = RuleSet.createTest(input);

        System.out.println("\n" + ruleSet + "\n");

        System.out.println(ruleSet.getTheory());*/

    }

    private final String theory;

    public RuleSetToTheory(String theory) {
        this.theory = theory;
    }

    public String getTheory() {
        return theory;
    }

    public static RuleSetToTheory create(RuleSet ruleSet) {
        String theory = translateRuleSetToTheory(ruleSet);
        return new RuleSetToTheory(theory);
    }

    // not much readable... the reason lies in the 2-class case represented by a single output and multiple outputs
    // is designed to translate, without cycles, only rule sets, where each class is implied by an arbitrary number of implications, but !, which follows immediately after each other
    private static String translateRuleSetToTheory(RuleSet ruleSet) {
        StringBuilder theory = new StringBuilder();
        Set<String> before = new HashSet<>();

        if (ruleSet.isBinaryClassClassification()
                && ruleSet.getRules().get(0).getNumberOfImplications() < 2) {
            Rule firstRule = ruleSet.getRules().get(0);

            if (0 == firstRule.getNumberOfImplications()) {
                theory.append(createRule(generateTargetClass(firstRule.getHead(), ruleSet.getClassAttribute()),
                        new ArrayList<>(),
                        true) + "\n");
            } else {
                Implication implication = ruleSet.getRules().get(0).getImplications().get(0);
                theory.append(createRule(generateTargetClass(firstRule.getHead(), ruleSet.getClassAttribute()),
                        convertImplication(implication,ruleSet),
                        true) + "\n");

            }
            if (!firstRule.getHead().equals(ruleSet.getClassAttribute().getPositiveClass())) {
                ArrayList<String> negatedAnotherClass = new ArrayList<>();
                negatedAnotherClass.add(RuleFile.NOT_TOKEN + RuleFile.NOT_TOKEN_OPENING_BRACKET + generateTargetClass(firstRule.getHead(), ruleSet.getClassAttribute()) + RuleFile.NOT_TOKEN_CLOSING_BRACKET);
                theory.append(createRule(generateTargetClass(ruleSet.getClassAttribute().getPositiveClass(), ruleSet.getClassAttribute()),
                        negatedAnotherClass,
                        true) + "\n");
            }

        /*}else if (1 == ruleSet.getNumberOfRules() && 0 == ruleSet.getRules().get(0).getNumberOfImplications()) {
            // just a special case
            theory.append(createRule(DatasetImpl.CLASS_TOKEN,
                    new ArrayList<>(),
                    true) + "\n");
            before.add(DatasetImpl.CLASS_TOKEN);
        } else if (1 == ruleSet.getNumberOfRules() && 1 == ruleSet.getRules().get(0).getNumberOfImplications()) {
            // just a special case
            Implication implication = ruleSet.getRules().get(0).getImplications().get(0);
            theory.append(createRule(DatasetImpl.CLASS_TOKEN,
                    convertImplication(implication),
                    true) + "\n");
            before.add(DatasetImpl.CLASS_TOKEN);*/
        } else {
            Rule last = ruleSet.getRules().get(ruleSet.getRules().size() - 1);
            ruleSet.getRules().stream()
                    .forEach(targetImplicatorSet -> {
                        String output = targetImplicatorSet.getHead();
                        List<Implication> impliedBy = targetImplicatorSet.getImplications();

                        if (ruleSet.isBinaryClassClassification() && before.size() >= 1) {
                            // do nothing, since the first class is already translated and the second is not to be translated at all
                        } else {
                            if (impliedBy.isEmpty()) {
                                // not really sound since when there are two last rules with zero antecedents, then it's not an ending case
                                // e.g. rule set: [] => class1; [] => class2
                                if (last != impliedBy) {
                                    throw new IllegalStateException("The translation algorithm does not implement transfering a theory that contains more than one last rule with zero antecedents.");
                                }

                                // ending case
                                if (ruleSet.isBinaryClassClassification()) {
                                    theory.append(createRule(generateTargetClass(output, ruleSet.getClassAttribute()), new ArrayList<>(), true) + "\n");
                                    if (!output.equals(ruleSet.getClassAttribute().getPositiveClass())) {
                                        ArrayList<String> negatedAnotherClass = new ArrayList<>();
                                        negatedAnotherClass.add(RuleFile.NOT_TOKEN + RuleFile.NOT_TOKEN_OPENING_BRACKET + generateTargetClass(output, ruleSet.getClassAttribute()) + RuleFile.NOT_TOKEN_CLOSING_BRACKET);
                                        theory.append(createRule(generateTargetClass(ruleSet.getClassAttribute().getPositiveClass(), ruleSet.getClassAttribute()),
                                                negatedAnotherClass,
                                                true) + "\n");
                                    }
                                } else {
                                    theory.append(createRule(generateTargetClass(output, ruleSet.getClassAttribute()), createNotAntecedents(before), true) + "\n");
                                }


                            } else if (1 == impliedBy.size()) {
                                if (ruleSet.isBinaryClassClassification()) {
                                    String outputHead = generateTargetClass(targetImplicatorSet.getHead(), ruleSet.getClassAttribute());
                                    theory.append(createRule(outputHead,
                                            appendLists(createNotAntecedents(before),
                                                    convertImplication(impliedBy.iterator().next(),ruleSet)),
                                            true) + "\n");
                                    before.add(outputHead);

                                    if (!output.equals(ruleSet.getClassAttribute().getPositiveClass())) {
                                        ArrayList<String> negatedAnotherClass = new ArrayList<>();
                                        negatedAnotherClass.add(RuleFile.NOT_TOKEN + RuleFile.NOT_TOKEN_OPENING_BRACKET + generateTargetClass(output, ruleSet.getClassAttribute()) + RuleFile.NOT_TOKEN_CLOSING_BRACKET);
                                        theory.append(createRule(generateTargetClass(ruleSet.getClassAttribute().getPositiveClass(), ruleSet.getClassAttribute()),
                                                negatedAnotherClass,
                                                true) + "\n");
                                    }

                                } else {
                                    String supportingConcept = "forClass" + output;
                                    theory.append(createRule(supportingConcept,
                                            appendLists(createNotAntecedents(before),
                                                    convertImplication(impliedBy.iterator().next(),ruleSet)),
                                            true) + "\n");
                                    List<String> targetAntecedents = new ArrayList<>();
                                    targetAntecedents.add(supportingConcept);
                                    theory.append(createRule(generateTargetClass(output, ruleSet.getClassAttribute()), targetAntecedents, true) + "\n");
                                    before.add(supportingConcept);
                                }
                            } else if (impliedBy.size() > 1) {
                                if (ruleSet.isBinaryClassClassification()) {
                                    String outputHead = generateTargetClass(targetImplicatorSet.getHead(), ruleSet.getClassAttribute());
                                    impliedBy.stream()
                                            .forEach(rule ->
                                                            theory.append(createRule(outputHead,
                                                                    appendLists(createNotAntecedents(before),
                                                                            convertImplication(rule,ruleSet)),
                                                                    true) + "\n")
                                            );
                                    before.add(outputHead);
                                    if (!output.equals(ruleSet.getClassAttribute().getPositiveClass())) {
                                        ArrayList<String> negatedAnotherClass = new ArrayList<>();
                                        negatedAnotherClass.add(RuleFile.NOT_TOKEN + RuleFile.NOT_TOKEN_OPENING_BRACKET + generateTargetClass(output, ruleSet.getClassAttribute()) + RuleFile.NOT_TOKEN_CLOSING_BRACKET);
                                        theory.append(createRule(generateTargetClass(ruleSet.getClassAttribute().getPositiveClass(), ruleSet.getClassAttribute()),
                                                negatedAnotherClass,
                                                true) + "\n");
                                    }

                                } else {
                                    String supportingConcept = "forClass" + output;
                                    impliedBy.stream()
                                            .forEach(rule ->
                                                            theory.append(createRule(supportingConcept,
                                                                    appendLists(createNotAntecedents(before),
                                                                            convertImplication(rule,ruleSet)),
                                                                    true) + "\n")
                                            );
                                    List<String> targetAntecedents = new ArrayList<>();
                                    targetAntecedents.add(supportingConcept);
                                    theory.append(createRule(generateTargetClass(output, ruleSet.getClassAttribute()), targetAntecedents, true) + "\n");
                                    before.add(supportingConcept);
                                }
                            }
                        }
                    });
        }
        return theory.toString();
    }

    private static List<String> appendLists(List<String> first, List<String> second) {
        List<String> result = new ArrayList<>(first);
        result.addAll(second);
        return result;
    }

    private static List<String> convertImplication(Implication implication,RuleSet ruleSet) {
        return implication.getBody()
                .stream()
                .map(antecedent -> {
                            if (ruleSet.isBinary(antecedent)) {
                                if (BinaryAttribute.isValueTrue(antecedent.getValue())) {
                                    return antecedent.getAttribute();
                                } else {
                                    return RuleFile.NOT_TOKEN + RuleFile.NOT_TOKEN_OPENING_BRACKET + antecedent.getAttribute() + RuleFile.NOT_TOKEN_CLOSING_BRACKET;
                                }
                            } else {
                                return antecedent.getAttribute() + DatasetImpl.ATTRIBUTE_VALUE_DELIMITER + antecedent.getValue();
                            }
                        }
                )
                .collect(Collectors.toList());
    }

    /**
     * really not a nice hack :( hardcoded, you cannot use "hopefullyNonClassAttribute" as a name for intermediate conclusion
     *
     * @param target
     * @param classAttribute
     * @return
     */
    private static String generateTargetClass(String target, ClassAttribute classAttribute) {
        //throw new IllegalStateException("velka chyba, pri vice labelech to generuje vsechny trida jako hopefullyNonclassAttribute");
        if (classAttribute.isBinary()) {
            //System.out.println(target + "\t" + classAttribute.getPositiveClass());

            if (classAttribute.getPositiveClass().equals(target)) {
                return DatasetImpl.CLASS_TOKEN + DatasetImpl.ATTRIBUTE_VALUE_DELIMITER + target;
            } else {
                return "hopefullyNonClassAttribute" + DatasetImpl.ATTRIBUTE_VALUE_DELIMITER + target;
            }
        }
        return DatasetImpl.CLASS_TOKEN + DatasetImpl.ATTRIBUTE_VALUE_DELIMITER + target;
    }

    private static List<String> createNotAntecedents(Set<String> antecedents) {
        return antecedents.stream()
                .map(antecedent -> RuleFile.NOT_TOKEN + "(" + antecedent + ")")
                .collect(Collectors.toList());
    }

    private static String createRule(String target, List<String> antecedents, boolean isModifiable) {
        StringBuilder sb = new StringBuilder();
        sb.append(target + " ");
        if (isModifiable) {
            sb.append(RuleFile.CHANGABLE_RULE + " ");
        } else {
            sb.append(RuleFile.UNCHANGABLE_RULE + " ");
        }
        antecedents.forEach(antecedent -> sb.append(antecedent + RuleFile.ANTECEDENTS_DELIMITER));
        return sb.toString().substring(0, sb.length() - 1) + RuleFile.RULE_ENDING_TOKEN;
    }
}
