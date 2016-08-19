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
        String numberOfRepeats = "2";

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
          */      "monks1",
            /*    "monks2",
                "monks3",
                "mushroom",
                "promotor",
                "segmentChallenge",
                "soybean",
                "splice",
          //      "voting",
            //    "wine"/**/
        };

        Arrays.stream(domains).forEach(domain -> {

            String experimentFolder = "." + File.separator + "experiments" + File.separator + "realData" + File.separator;
            String folder = experimentFolder + domain + File.separator;
            String KBANNsetting = "." + File.separator + "experiments" + File.separator + "settings" + File.separator + "KBANN" + File.separator + "-0-0" + File.separator + "kbannSetting.txt";
            String wlsFolder = "-0-0-0-0-0-0-0-0";
            String wls = "." + File.separator + "experiments" + File.separator + "settings" + File.separator + "WLS" + File.separator + wlsFolder + File.separator + "wlsSetting.txt";
            String KBANNinput = folder + "theory";
            String data = folder + "data" + ((nominalized.contains(domain)) ? "Nominalized": ""); //Nominalized";//Nominalized";
            data = folder + "data";

            System.out.println(folder);
            System.out.println(data);

            String cascorSetting = "." + File.separator + "experiments" + File.separator  + "settings" + File.separator + "CasCor" + File.separator + "-0-0-0-0-0-0" + File.separator + "cascorSetting.txt";
            String dncSetting = "." + File.separator + "experiments" + File.separator + "settings" +File.separator + "DNC" + File.separator + "-0-0-0-0-0" + File.separator + "DNCSetting.txt";
            String regentSetting = "." + File.separator + "experiments" + File.separator + "settings" +File.separator + "REGENT" + File.separator + "-0-0-0-0-0-0-0-0-0-0-0-0-0-0-0-0-0-0-0" + File.separator + "REGENTSetting.txt";

            try {
                //Main.main(new String[]{"KBANN", numberOfRepeats, data, wls, KBANNinput, KBANNsetting});
                //Main.main(new String[]{"backprop", numberOfRepeats, data, wls, KBANNinput, KBANNsetting});
                //Main.main(new String[]{"CasCor", numberOfRepeats, data, wls, cascorSetting});

                //Main.main(new String[]{"DNC", numberOfRepeats, data, wls, dncSetting});
                //Main.main(new String[]{"DNC", numberOfRepeats, data, wls, dncSetting});

                Main.main(new String[]{"REGENT", numberOfRepeats, data, wls, KBANNinput, regentSetting});


            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });
    }


}
