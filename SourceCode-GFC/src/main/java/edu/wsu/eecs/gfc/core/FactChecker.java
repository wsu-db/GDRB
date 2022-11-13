package edu.wsu.eecs.gfc.core;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.Logistic;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

import java.nio.file.Files;
import java.nio.file.Paths;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.util.*;
import java.io.File;
import java.io.IOException;
import com.opencsv.CSVWriter;

/**
 * Doing fact checking here with GFCs
 * <p>
 * @author Peng Lin penglin03@gmail.com
 * @param <VT>
 */
public class FactChecker {

    public static <VT, ET> String predictByHits(List<OGFCRule<VT, ET>> patternList, Map<Boolean, List<Edge<VT, ET>>> dataTest) {
        double tp = 0;
        double fn = 0;
        for (Edge<VT, ET> pos : dataTest.get(true)) {
            boolean hit = false;
            for (OGFCRule<VT, ET> p : patternList) {
                if (p.matchSet().get(p.x()).contains(pos.srcNode())
                        && p.matchSet().get(p.y()).contains(pos.dstNode())) {
                    hit = true;
                    break;
                }
            }
            if (hit) {
                tp++;
            } else {
                fn++;
            }
        }

        double tn = 0;
        double fp = 0;
        for (Edge<VT, ET> neg : dataTest.get(false)) {
            boolean hit = false;
            for (OGFCRule<VT, ET> p : patternList) {
                if (p.matchSet().get(p.x()).contains(neg.srcNode())
                        && p.matchSet().get(p.y()).contains(neg.dstNode())) {
                    hit = true;
                    break;
                }
            }
            if (!hit) {
                tn++;
            } else {
                fp++;
            }
        }

        double accuracy = (tp + tn) / (tp + fn + tn + fp);
        double precision = tp / (tp + fp);
        double recall = tp / (tp + fn);
        double f1 = 2 * precision * recall / (precision + recall);
//        System.out.println(accuracy + "\t" + precision + "\t" + recall + "\t" + f1);
        String outStr = accuracy + "\t" + precision + "\t" + recall + "\t" + f1;
        return outStr;
    }

    public static <VT, ET> String Train_LRModel(List<OGFCRule<VT, ET>> patternList, Relation<VT, ET> r,
                                                              Map<Boolean, List<Edge<VT, ET>>> dataTrain, String outputPath) 
                                                              throws Exception 
    {
        int dim = patternList.size();
        ArrayList<Attribute> fvec = new ArrayList<>(dim + 1);
        for (int i = 0; i < dim; i++) {
            fvec.add(new Attribute("P" + i));
        }
        ArrayList<String> classVals = new ArrayList<>(2);
        classVals.add("TRUE");
        classVals.add("FALSE");
        Attribute attrClass = new Attribute("class", classVals);
        fvec.add(attrClass);

        String rName = r.srcLabel() + "_" + r.edgeLabel() + "_" + r.dstLabel();
        Instances trainSet = new Instances(rName, fvec, 10);
        trainSet.setClassIndex(dim);

        Map<String, List<List<Set<Node<VT>>>>> pMatchSet = new HashMap<String, List<List<Set<Node<VT>>>>>();
        List<Set<Node<VT>>> xSet = new ArrayList<Set<Node<VT>>>();
        List<Set<Node<VT>>> ySet = new ArrayList<Set<Node<VT>>>();
        List<List<Set<Node<VT>>>> pNodes = new ArrayList<List<Set<Node<VT>>>>();

        for (Edge<VT, ET> posTrain : dataTrain.get(true)) {
            Instance iExample = new DenseInstance(dim + 1);
            for (int i = 0; i < patternList.size(); i++) {
                OGFCRule<VT, ET> p = patternList.get(i);
                xSet.add(p.matchSet().get(p.x()));
                ySet.add(p.matchSet().get(p.y()));
                if (p.matchSet().get(p.x()).contains(posTrain.srcNode())
                        && p.matchSet().get(p.y()).contains(posTrain.dstNode())) {
                    iExample.setValue(fvec.get(i), 1);          
                } else {
                    iExample.setValue(fvec.get(i), 0);
                }
            }
            iExample.setValue(fvec.get(dim), "TRUE");
            trainSet.add(iExample);
        }
        pNodes.add(xSet);
        pNodes.add(ySet);
        pMatchSet.put(rName,pNodes);
        Json_Writer(pMatchSet,rName);

        for (Edge<VT, ET> negTrain : dataTrain.get(false)) {
            Instance iExample = new DenseInstance(dim + 1);
            for (int i = 0; i < patternList.size(); i++) {
                OGFCRule<VT, ET> p = patternList.get(i);
                if (p.matchSet().get(p.x()).contains(negTrain.srcNode())
                        && p.matchSet().get(p.y()).contains(negTrain.dstNode())) {
                    iExample.setValue(fvec.get(i), 1);
                } else {
                    iExample.setValue(fvec.get(i), 0);
                }
            }
            iExample.setValue(fvec.get(dim), "FALSE");
            trainSet.add(iExample);
        }
        if (trainSet.size() < 1) {
            return "WARNING: Skip training. Not enough training examples.";
        }

        Classifier model = new Logistic();
        model.buildClassifier(trainSet);
        weka.core.SerializationHelper.write("./Trained_Models/"+rName+"_LR.model", model);

        ArffSaver arffSaver;
        arffSaver = new ArffSaver();
        arffSaver.setInstances(trainSet);
        try {
            arffSaver.setFile(new File(outputPath, rName + "_Train.arff"));
            arffSaver.writeBatch();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String Outstr = "\nModels have been trained with Logistic Regression";
        return Outstr;
    }

    public static <VT> void Json_Writer(Map<String, List<List<Set<Node<VT>>>>> pMatchSet, String rName) throws IOException {
        Gson gson = new Gson();
        Writer writer = Files.newBufferedWriter(Paths.get("./Patterns/"+rName+".json"));
        gson.toJson(pMatchSet, writer);
        writer.close();  
    } 
    
    /**
     * @param <VT>
     * @param <ET>
     * @param patternList
     * @param r
     * @param dataTest
     * @param outputPath
     * @return
     * @throws Exception
     */
    public static <VT, ET> String Test_LRModel(int topK, Relation<VT, ET> r,
                                                              Map<Boolean, List<Edge<VT, ET>>> dataTest, String outputPath) 
                                                              throws Exception 
    {
        int dim = topK;
        ArrayList<Attribute> fvec = new ArrayList<>(dim + 1);
        for (int i = 0; i < dim; i++) {
            fvec.add(new Attribute("P" + i));
        }
        ArrayList<String> classVals = new ArrayList<>(2);
        classVals.add("TRUE");
        classVals.add("FALSE");
        Attribute attrClass = new Attribute("class", classVals);
        fvec.add(attrClass);

        String rName = r.srcLabel() + "_" + r.edgeLabel() + "_" + r.dstLabel();
        Instances testSet = new Instances(rName, fvec, 10);
        testSet.setClassIndex(dim);
        List<Set<Node<VT>>> xSet = new ArrayList<Set<Node<VT>>>();
        List<Set<Node<VT>>> ySet = new ArrayList<Set<Node<VT>>>();
        List<List<Set<Node<VT>>>> pNodes = new ArrayList<List<Set<Node<VT>>>>();
        Map<String, List<List<Set<Node<VT>>>>> pMatchSet = new HashMap<String, List<List<Set<Node<VT>>>>>();
        
        pMatchSet = Json_Reader(rName);
        pNodes = pMatchSet.get(rName);
        xSet = pNodes.get(0);
        ySet = pNodes.get(1);

        List<String> facts = new ArrayList<>();
        for (Edge<VT, ET> posTest : dataTest.get(true)) {
            facts.add(posTest.toString());
            Instance iExample = new DenseInstance(dim + 1);
            for (int i = 0; i < topK; i++) {
                if (xSet.get(i).contains(posTest.srcNode())
                        && ySet.get(i).contains(posTest.dstNode())) {
                    iExample.setValue(fvec.get(i), 1);
                } else {
                    iExample.setValue(fvec.get(i), 0);
                }
            }
            iExample.setValue(fvec.get(dim), "TRUE");
            testSet.add(iExample);
        }
        for (Edge<VT, ET> negTest : dataTest.get(false)) {
            facts.add(negTest.toString());
            Instance iExample = new DenseInstance(dim + 1);
            for (int i = 0; i < topK; i++) {
                if (xSet.get(i).contains(negTest.srcNode())
                        && xSet.get(i).contains(negTest.dstNode())) {
                    iExample.setValue(fvec.get(i), 1);
                } else {
                    iExample.setValue(fvec.get(i), 0);
                }
            }
            iExample.setValue(fvec.get(dim), "FALSE");
            testSet.add(iExample);
        }
        
        if (testSet.size() < 1) {
            return "WARNING: Skip training. Not enough testing examples.";
        }

        // Importing trained model
        Classifier model = null;
        try {
            model = (Classifier) weka.core.SerializationHelper
                    .read("./Trained_Models/"+rName+"_LR.model");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (model == null)
            return null;   

        List<String> result = new ArrayList<>();
        for (int i = 0; i < testSet.numInstances(); i++) {
            double pred = model.classifyInstance(testSet.instance(i));
            // System.out.print("ID: " + Test.instance(i).value(0));
            // System.out.print(", actual: " + Test.classAttribute().value((int) Test.instance(i).classValue()));
            // System.out.println(", predicted: " + Test.classAttribute().value((int) pred));
            result.add(testSet.classAttribute().value((int) pred));
        }

        File file = new File("./"+outputPath+"/Predictions.csv");
        try {
            FileWriter outputfile = new FileWriter(file,true);
            CSVWriter writer = new CSVWriter(outputfile,',',CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);

            String[] header = { "Assertion", "Prediction"};
            writer.writeNext(header);

            for(int i=0; i<facts.size(); i++){
            String[] a = { facts.get(i), result.get(i)};
            writer.writeNext(a);
            }    
            writer.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        Evaluation eval = new Evaluation(testSet);
        eval.crossValidateModel(model, testSet, 10, new Random(1));

        // Evaluation eval = new Evaluation(trainSet);
        // eval.evaluateModel(model, testSet);
        // System.out.println("\nAccuracy: \n"+eval.pctCorrect()+"\n");
        
        String outStr = (eval.pctCorrect() / 100) + "\t" +
                (eval.precision(0)) + "\t" +
                (eval.recall(0)) + "\t" +
                (eval.fMeasure(0));

        ArffSaver arffSaver;
        arffSaver = new ArffSaver();
        arffSaver.setInstances(testSet);
        try {
            arffSaver.setFile(new File(outputPath, rName + "_" + "_Test.arff"));
            arffSaver.writeBatch();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // String outStr = "\nModel has been Tested";
        return outStr;
    }

    public static <VT> Map<String, List<List<Set<Node<VT>>>>> Json_Reader(String rName) throws IOException{
        Gson gson = new Gson();
        Reader reader = Files.newBufferedReader(Paths.get("./Patterns/"+rName+".json"));

        java.lang.reflect.Type mapType = new TypeToken<Map<String, List<List<Set<Node<VT>>>>>>(){}.getType();
        Map<String, List<List<Set<Node<VT>>>>> pMatchSet = gson.fromJson(reader, mapType);
 
        reader.close();

        return pMatchSet;
    }
}