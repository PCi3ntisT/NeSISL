package main.java.cz.cvut.ida.nesisl.application;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;

/**
 * Created by EL on 16.3.2016.
 */
public class Experiments {

    public static void main(String arg[]) {
        String numberOfRepeats = "1";
        String[] domains = new String[]{
                //"logic" + File.separator + "and2",
                //"logic" + File.separator + "and2Reversed",
                /*"logic" + File.separator + "xor2",
                "logic" + File.separator + "xor2Reversed",
                "logic" + File.separator + "xor4",
                "logic" + File.separator + "xor4Reversed",
                */
                "logic" + File.separator + "allOrNothing",
                /*"logic" + File.separator + "doubleImplication",
                "logic" + File.separator + "or2clauses",
                "logic" + File.separator + "threeIndependentClauses",
                "logic" + File.separator + "twoIndependentClauses",
                */

                //"logic" + File.separator + "threeHierarchyClauses",
                //"logic" + File.separator + "threeHierarchyClausesWithXor",

                //"logic" + File.separator + "",
                //"logic" + File.separator + "and3",
                //"logic" + File.separator + "dnf4",
                //"logic" + File.separator + "xor2",
                //"logic" + File.separator + "xor3"
                //"iris" + File.separator
        };
        String experimentFolder = "." + File.separator + "experiments" + File.separator;

        Arrays.stream(domains).forEach(domain -> {
            String folder = experimentFolder + domain + File.separator;
            String data = folder + "data.txt";
            String wls = folder + "wlsSettings.txt";
            String slfInput = folder + "SLFinput.txt";
            String KBANNinput = folder + "KBANNinput.txt";
            try {
                /*Main.main(new String[]{"KBANN", numberOfRepeats, data, wls, KBANNinput});
                Main.main(new String[]{"CasCor", numberOfRepeats, data, wls});
                Main.main(new String[]{"DNC", numberOfRepeats, data, wls});
                Main.main(new String[]{"SLF", numberOfRepeats, data, wls, slfInput});
                */

                //Main.main(new String[]{"TopGen", numberOfRepeats, data, wls, KBANNinput});

                Main.main(new String[]{"REGENT", numberOfRepeats, data, wls, KBANNinput});
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });
    }
}
