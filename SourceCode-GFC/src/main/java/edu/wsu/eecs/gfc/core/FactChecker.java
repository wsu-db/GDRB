package edu.wsu.eecs.gfc.core;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.classifiers.functions.Logistic;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVSaver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Doing fact checking here with GFCs
 * <p>
 * @author Peng Lin penglin03@gmail.com
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

    public static <VT, ET> String predictByLogisticRegression(List<OGFCRule<VT, ET>> patternList, Relation<VT, ET> r,
                                                              Map<Boolean, List<Edge<VT, ET>>> dataTrain,
                                                              Map<Boolean, List<Edge<VT, ET>>> dataTest,
                                                              String outputPath, String tag) throws Exception {
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
        Instances testSet = new Instances(rName, fvec, 10);
        trainSet.setClassIndex(dim);
        testSet.setClassIndex(dim);

        for (Edge<VT, ET> posTrain : dataTrain.get(true)) {
            Instance iExample = new DenseInstance(dim + 1);
            for (int i = 0; i < patternList.size(); i++) {
                OGFCRule<VT, ET> p = patternList.get(i);
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
        for (Edge<VT, ET> posTest : dataTest.get(true)) {
            Instance iExample = new DenseInstance(dim + 1);
            for (int i = 0; i < patternList.size(); i++) {
                OGFCRule<VT, ET> p = patternList.get(i);
                if (p.matchSet().get(p.x()).contains(posTest.srcNode())
                        && p.matchSet().get(p.y()).contains(posTest.dstNode())) {
                    iExample.setValue(fvec.get(i), 1);
                } else {
                    iExample.setValue(fvec.get(i), 0);
                }
            }
            iExample.setValue(fvec.get(dim), "TRUE");
            testSet.add(iExample);
        }
        for (Edge<VT, ET> negTest : dataTest.get(false)) {
            Instance iExample = new DenseInstance(dim + 1);
            for (int i = 0; i < patternList.size(); i++) {
                OGFCRule<VT, ET> p = patternList.get(i);
                if (p.matchSet().get(p.x()).contains(negTest.srcNode())
                        && p.matchSet().get(p.y()).contains(negTest.dstNode())) {
                    iExample.setValue(fvec.get(i), 1);
                } else {
                    iExample.setValue(fvec.get(i), 0);
                }
            }
            iExample.setValue(fvec.get(dim), "FALSE");
            testSet.add(iExample);
        }

        if (trainSet.size() < 1) {
            return "WARNING: Skip training. Not enough training examples.";
        }
        if (testSet.size() < 1) {
            return "WARNING: Skip training. Not enough testing examples.";
        }

        Classifier model = new Logistic();
        model.buildClassifier(trainSet);
        Evaluation eval = new Evaluation(trainSet);
        eval.evaluateModel(model, testSet);


        String outStr = (eval.pctCorrect() / 100) + "\t" +
                (eval.precision(0)) + "\t" +
                (eval.recall(0)) + "\t" +
                (eval.fMeasure(0));
//        System.out.println((eval.pctCorrect() / 100) + "\t" +
//                (eval.precision(0)) + "\t" +
//                (eval.recall(0)) + "\t" +
//                (eval.fMeasure(0))
//        );

        ArffSaver arffSaver;
        arffSaver = new ArffSaver();
        arffSaver.setInstances(trainSet);
        try {
            arffSaver.setFile(new File(outputPath, rName + "_" + tag + "_Train.arff"));
            arffSaver.writeBatch();
        } catch (IOException e) {
            e.printStackTrace();
        }

        arffSaver = new ArffSaver();
        arffSaver.setInstances(testSet);
        try {
            arffSaver.setFile(new File(outputPath, rName + "_" + tag + "_Test.arff"));
            arffSaver.writeBatch();
        } catch (IOException e) {
            e.printStackTrace();
        }


        ThresholdCurve tc = new ThresholdCurve();
        int classIndex = 0;
        Instances result = tc.getCurve(eval.predictions(), classIndex);

        CSVSaver csvSaver = new CSVSaver();
        csvSaver.setInstances(result);
        try {
            csvSaver.setFile(new File(outputPath, rName + "_" + tag + "_RocTT.csv"));
            csvSaver.writeBatch();
        } catch (IOException e) {
            e.printStackTrace();
        }

        eval.crossValidateModel(model, trainSet, 10, new Random(1));


        tc = new ThresholdCurve();
        result = tc.getCurve(eval.predictions(), 0);

        csvSaver = new CSVSaver();
        csvSaver.setInstances(result);
        try {
            csvSaver.setFile(new File(outputPath, rName + "_" + tag + "_RocCV.csv"));
            csvSaver.writeBatch();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        System.out.println(eval.toClassDetailsString());
//        System.out.println(eval.toMatrixString());
        return outStr;
    }
}