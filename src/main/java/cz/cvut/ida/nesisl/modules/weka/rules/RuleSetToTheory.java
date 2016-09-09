package main.java.cz.cvut.ida.nesisl.modules.weka.rules;

import main.java.cz.cvut.ida.nesisl.modules.algorithms.kbann.RuleFile;
import main.java.cz.cvut.ida.nesisl.modules.dataset.DatasetImpl;

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

        RuleSet ruleSet = RuleSet.create(input);

        System.out.println("\n" + ruleSet + "\n");

        System.out.println(ruleSet.getTheory());

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

        if (1 == ruleSet.getNumberOfRules() && 1 == ruleSet.getRules().get(0).getNumberOfImplications()) {
            // just a special case
        } else {
            ruleSet.getRules().stream()
                    .forEach(targetImplicatorSet -> {
                        String output = targetImplicatorSet.getHead();
                        List<Implication> impliedBy = targetImplicatorSet.getImplications();

                        if (ruleSet.isBinaryClassClassification() && before.size() >= 1) {
                            // do nothing, since the first class is already translated and the second is not to be translated at all
                        } else {
                            if (impliedBy.isEmpty()) {
                                // ending case
                                if (ruleSet.isBinaryClassClassification()) {
                                    theory.append(createRule(output, new ArrayList<>(), true) + "\n");
                                } else {
                                    theory.append(createRule(generateTargetClass(output), createNotAntecedents(before), true) + "\n");
                                }
                            } else if (1 == impliedBy.size()) {
                                if (ruleSet.isBinaryClassClassification()) {
                                    theory.append(createRule(DatasetImpl.CLASS_TOKEN,
                                            appendLists(createNotAntecedents(before),
                                                    convertImplication(impliedBy.iterator().next())),
                                            true) + "\n");
                                    before.add(DatasetImpl.CLASS_TOKEN);
                                } else {
                                    String supportingConcept = "forClass" + output;
                                    theory.append(createRule(supportingConcept,
                                            appendLists(createNotAntecedents(before),
                                                    convertImplication(impliedBy.iterator().next())),
                                            true) + "\n");
                                    List<String> targetAntecedents = new ArrayList<>();
                                    targetAntecedents.add(supportingConcept);
                                    theory.append(createRule(generateTargetClass(output), targetAntecedents, true) + "\n");
                                    before.add(supportingConcept);
                                }
                            } else if (impliedBy.size() > 1) {
                                if (ruleSet.isBinaryClassClassification()) {
                                    impliedBy.stream()
                                            .forEach(rule ->
                                                            theory.append(createRule(DatasetImpl.CLASS_TOKEN,
                                                                    appendLists(createNotAntecedents(before),
                                                                            convertImplication(rule)),
                                                                    true) + "\n")
                                            );
                                    before.add(DatasetImpl.CLASS_TOKEN);
                                } else {
                                    String supportingConcept = "forClass" + output;
                                    impliedBy.stream()
                                            .forEach(rule ->
                                                            theory.append(createRule(supportingConcept,
                                                                    appendLists(createNotAntecedents(before),
                                                                            convertImplication(rule)),
                                                                    true) + "\n")
                                            );
                                    List<String> targetAntecedents = new ArrayList<>();
                                    targetAntecedents.add(supportingConcept);
                                    theory.append(createRule(generateTargetClass(output), targetAntecedents, true) + "\n");
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

    private static List<String> convertImplication(Implication implication) {
        return implication.getBody()
                .stream()
                .map(antecedent ->
                                antecedent.getAttribute() + DatasetImpl.ATTRIBUTE_VALUE_DELIMITER + antecedent.getValue()
                )
                .collect(Collectors.toList());
    }

    private static String generateTargetClass(String target) {
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
