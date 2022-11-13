package edu.wsu.eecs.gfc.exps;
import edu.wsu.eecs.gfc.core.*;

import java.io.File;

/**
 * The caller to test GFC mining.
 * @author Peng Lin penglin03@gmail.com
 */
public class GFC {

    public static void main(String[] args) throws Exception {
        String inputDir = args[0];
        String outputDir = args[1];
        new File(outputDir).mkdirs();

        double minSupp = Double.parseDouble(args[2]);
        double minConf = Double.parseDouble(args[3]);
        int maxSize = Integer.parseInt(args[4]);
        int topK = Integer.parseInt(args[5]);

        TrainGFC.train(inputDir,outputDir,minSupp,minConf,maxSize,topK);

        TestGFC.test(inputDir,outputDir,topK);
    }
}
