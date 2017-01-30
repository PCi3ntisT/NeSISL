package main.java.cz.cvut.ida.nesisl.application;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by EL on 15.8.2016.
 */
public class RealDataExperiments {
    public static void main(String arg[]) {
        String numberOfRepeats = "10";

        Set<String> nominalized = new HashSet<>();
        nominalized.add("breastCancer1");
        nominalized.add("breastCancer2");
        nominalized.add("breastCancer3");
        nominalized.add("diabetes");
        nominalized.add("glass");
        nominalized.add("horse");
        nominalized.add("ionosphere");
        nominalized.add("iris");
        nominalized.add("labor");
        nominalized.add("segmentChallenge");
        nominalized.add("wine");

        String[] domains = new String[]{
       /*         "breastCancer1",
                "breastCancer2",
                "breastCancer3",
                "diabetes",
                "glass",
                "horse",
                "ionosphere",
                "iris",
                "labor",
                "lenses",
                "monks1",
                "monks2",
                "monks3",
                "mushroom",
                "promotor",
                "segmentChallenge",
                "soybean",
                "splice",
          //      "voting",
            //    "wine"/**/

                //"cnf20-7"
                //"lenses",

                //"cnf200-7"

                /*"cnf1-10",
                "cnf2-10",
                "cnf6-10",
                "cnf8-10",
                "cnf15-10",
                "cnf18-10",
                "cnf20-10",
                "cnf32-10",*/
                //"cnf36-10"
                //"promotor",
                "wine"
                //"cnf6-10"
        };
        //zkontrolovat jestli si drzi dataset samplz fakt jako list samplu nebo jako mnozinu - mel by byt list

        Arrays.stream(domains).forEach(domain -> {

            //String experimentFolder = "." + File.separator + "experiments" + File.separator + "realData" + File.separator;
            //String experimentFolder = "." + File.separator + "experiments" + File.separator + "resampled" + File.separator;
            String experimentFolder = "." + File.separator + "experiments" + File.separator + "debugSubsampled" + File.separator;
            String folder = experimentFolder + domain + File.separator;
            String KBANNsetting = "." + File.separator + "experiments" + File.separator + "settings" + File.separator + "KBANN" + File.separator + "-0-0" + File.separator + "kbannSetting.txt";
            String wlsFolder = "-0-0-0-0-0-0-0-0-0";
            String wls = "." + File.separator + "experiments" + File.separator + "settings" + File.separator + "WLS" + File.separator + wlsFolder + File.separator + "wlsSetting.txt";
            String KBANNinput = folder + "theory";
            String data = folder + "data" + ((nominalized.contains(domain)) ? "Nominalized" : ""); //Nominalized";//Nominalized";
            data = folder + "crossvalidationData";
            String backgroundData = folder + "backgroundKnowledgeLearnerData";
            //data = folder + "data";


            System.out.println(folder);
            System.out.println(data);

            System.out.println(data);
            System.out.println(backgroundData);

            String cascorSetting = "." + File.separator + "experiments" + File.separator + "settings" + File.separator + "CasCor" + File.separator + "-0-0-0-0-0-0" + File.separator + "cascorSetting.txt";
            String dncSetting = "." + File.separator + "experiments" + File.separator + "settings" + File.separator + "DNC" + File.separator + "-0-0-0-0-0" + File.separator + "DNCSetting.txt";
            String regentSetting = "." + File.separator + "experiments" + File.separator + "settings" + File.separator + "REGENT" + File.separator + "-0-0-0-0-0-0-0-0-0-0-0-0-0-0-0-0-0-0-0-0-0" + File.separator + "REGENTSetting.txt";

            String tgSetting = "." + File.separator + "experiments" + File.separator + "settings" + File.separator + "TopGen" + File.separator + "-1-0-0-0-0-1-2-0-0-0-1-0" + File.separator + "TopGenSetting.txt";

            System.out.println("pridat majority class do texu");


            try {
                // running check
                //Main.main(new String[]{Main.RULE_EXTRACTION_CHECKER,"KBANN", numberOfRepeats, data, backgroundData, wls, KBANNsetting});

                //Main.main(new String[]{Main.RULE_EXTRACTION_CHECKER,"KBANN", numberOfRepeats, data, data, wls, KBANNsetting});

                //Main.main(new String[]{Main.RULE_EXTRACTION_CHECKER,"KBANN", numberOfRepeats, data, data, wls, KBANNsetting});
                //Main.main(new String[]{"-seed","8","-trimAcc","1.1","PYRAMID", "10", data, data, wls, KBANNsetting});
                //Main.main(new String[]{Main.SET_EXTRACTOR_TOKEN,"jrip","-seed","8","-trimAcc","1.1",Main.CYCLE_TOKEN,"3","KBANN", "10", data, data, wls, KBANNsetting});
                //Main.main(new String[]{"PYRAMID", numberOfRepeats, data, data, wls, "5"});

                //Main.main(new String[]{Main.CYCLE_TOKEN,"10","KBANN", numberOfRepeats, data, backgroundData, wls, KBANNsetting});
                Main.main(new String[]{"KBANN", numberOfRepeats, data, backgroundData, wls, KBANNsetting});


                // old parameters
                //Main.main(new String[]{"backprop", numberOfRepeats, data, wls, KBANNinput, KBANNsetting});
                //Main.main(new String[]{"fullyConnected", numberOfRepeats, data, wls, KBANNinput, KBANNsetting});
                //Main.main(new String[]{"CasCor", numberOfRepeats, data, wls, cascorSetting});

                //Main.main(new String[]{"DNC", numberOfRepeats, data, wls, dncSetting});
                //Main.main(new String[]{"TopGen", numberOfRepeats, data, wls, tgSetting});

                //Main.main(new String[]{"REGENT", numberOfRepeats, data, wls, regentSetting});


            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        });
    }


}
