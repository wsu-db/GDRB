package edu.wsu.eecs.gfc.core;

import java.util.*;

public class TestGFC {

    public static List<String> testingAssertions = new ArrayList<>();

    public static void test(String inputDir,String outputPath,int topK) throws Exception {

        List<Relation<String, String>> relationList = IO.loadRelations(inputDir);
        
        for (Relation<String, String> r : relationList) {
        // System.out.println("FactChecker: OFact_R: "
        //             + FactChecker.predictByHits(patternList, dataTest));

            FactSampler sampler = new FactSampler(inputDir);
            sampler.extract_testing_asserions(inputDir,r);

            System.out.println("\nModel Evaluation: \n"+"\nAccuracy\tPrecision\tRecall\tFMeasure\n"
            + FactChecker.Test_LRModel(topK, r, sampler.getDataTest(), outputPath));
        }
   }

   public static List<String> addTestingData(String sub, String pred, String obj){

    String assertion = sub +"\t" + pred +"\t" + obj +"\t";
    testingAssertions.add(assertion);
    return testingAssertions;
}
}
