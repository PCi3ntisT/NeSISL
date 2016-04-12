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
                //"logic" + File.separator + "allOrNothing",
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

                "artificialLogic" + File.separator + "1007",

                /*"artificialLogic" + File.separator + "467",
                "artificialLogic" + File.separator + "830",
                "artificialLogic" + File.separator + "853",
                "artificialLogic" + File.separator + "2277",*/
        };
        String experimentFolder = "." + File.separator + "experiments" + File.separator;

        Arrays.stream(domains).forEach(domain -> {
            String folder = experimentFolder + domain + File.separator;
            String data = folder + "data.txt";
            //String wls = folder + "wlsSettings.txt";
            String wlsFolder = "-0-0-0-0-0-0-0-0";
            String wls = "." + File.separator + "experiments" + File.separator + "settings" + File.separator + "WLS" + File.separator + wlsFolder + File.separator + "wlsSetting.txt";
            //String slfInput = folder + "SLFinput.txt";
            //String KBANNinput = folder + "KBANNinput.txt";
            String expSettings = "." + File.separator + "experiments" + File.separator + "settings" + File.separator;
            String KBANNinput = folder + "KBANNinput.txt";
            String slfInput = folder + File.separator + "SLFinput.txt";
            String KBANNsetting = expSettings + "KBANN" + File.separator + "-1" + File.separator + "kbannSetting.txt";
            String cascorSetting = expSettings + "CasCor" + File.separator + "-0-0-0-0-0-0" + File.separator + "cascorSetting.txt";
            String dncSetting = expSettings + "DNC" + File.separator + "-0-0-0-0-0" + File.separator + "DNCSetting.txt";
            String tgSetting = expSettings + "TopGen" + File.separator + "-1-1-2-2-2-2-3-2" + File.separator + "TopGenSetting.txt";
            String regentSetting = expSettings + "REGENT" + File.separator + "-0-0-0-0-0-0-0-0-0-0-0-0-0-0-0-0-0" + File.separator + "REGENTSetting.txt";

            try {
                //Main.main(new String[]{"CasCor", numberOfRepeats, data, wls, cascorSetting});
                /*Main.main(new String[]{"KBANN", numberOfRepeats, data, wls, KBANNinput, KBANNsetting});

                Main.main(new String[]{"DNC", numberOfRepeats, data, wls, dncSetting});
                Main.main(new String[]{"SLF", numberOfRepeats, data, wls, slfInput});
                Main.main(new String[]{"TopGen", numberOfRepeats, data, wls, KBANNinput, tgSetting});
                */
                Main.main(new String[]{"REGENT", numberOfRepeats, data, wls, KBANNinput, regentSetting});
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });
    }
}
