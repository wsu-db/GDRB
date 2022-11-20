package edu.wsu.eecs.gfc.exps;
import edu.wsu.eecs.gfc.core.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * The caller to test GFC mining.
 * @author Peng Lin penglin03@gmail.com
 */
public class GFC {

    
    private static final Logger log = LoggerFactory.getLogger(GFC.class);

    public static void listen(int port,String inputDir,String outputDir,double minSupp,double minConf,int maxSize,int topK) throws Exception {

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("Waiting for connection on port ",port);
            Socket client = serverSocket.accept();
            log.info("Accepted connection");

            while(true) {
                try{
                    log.info("Waiting for a request");
                    InputStream input = client.getInputStream();
                    DataOutputStream outputStream  = new DataOutputStream(client.getOutputStream());
                    BufferedReader br = new BufferedReader(new InputStreamReader(input));
                    String request = br.readLine();
                    if(request == ""){
                        log.info("Connection closed");
                        client.close();
                        return;
                    }
                    log.info("Request received");
    
                    JSONObject requestJSON = new JSONObject(request.toString());
    
                    if ((requestJSON.getString("type") == "call") && (requestJSON.getString("content") == "type")) {
    
                        requestJSON.put("type","type_response");
                        requestJSON.put("content","supervised");
                        outputStream.writeUTF(requestJSON.toString());
    
                    }
    
                    if ((requestJSON.getString("type") == "call") && (requestJSON.getString("content") == "training_start")) {
    
                        requestJSON.put("type","ack");
                        requestJSON.put("content","training_start_ack");
                        outputStream.writeUTF(requestJSON.toString());
    
                    }
    
                    if ((requestJSON.getString("type") == "train")) {
    
                        requestJSON.put("type","ack");
                        TrainGFC.addTrainingData(requestJSON.getString("subject"),requestJSON.getString("predicate"),
                                                    requestJSON.getString("object"),requestJSON.getString("score"));
                        requestJSON.put("content","train_ack");
                        outputStream.writeUTF(requestJSON.toString());
    
                    }
    
                    if ((requestJSON.getString("type") == "call") && (requestJSON.getString("content") == "training_complete")) {
    
                        TrainGFC.train(inputDir,outputDir,minSupp,minConf,maxSize,topK);
                        requestJSON.put("type","ack");
                        requestJSON.put("content","training_complete_ack");
                        outputStream.writeUTF(requestJSON.toString());
    
                    }
    
                    if ((requestJSON.getString("type") == "test")) {
    
                        TestGFC.addTestingData(requestJSON.getString("subject"),requestJSON.getString("predicate"),
                                                    requestJSON.getString("object"));
                        TestGFC.test(inputDir,outputDir,topK);
                        requestJSON.put("type","test_result");
                        //TODO: Individual assertion score
                        // requestJSON.put("score",prediction);
                        outputStream.writeUTF(requestJSON.toString());
    
                    }
    
                    log.info("Request answered");
    
                }
                catch(IOException | JSONException e){
                    e.printStackTrace();
                }
            }
            

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) throws Exception {
        String inputDir = args[0];
        String outputDir = args[1];
        new File(outputDir).mkdirs();

        double minSupp = Double.parseDouble(args[2]);
        double minConf = Double.parseDouble(args[3]);
        int maxSize = Integer.parseInt(args[4]);
        int topK = Integer.parseInt(args[5]);

        listen(4011,inputDir,outputDir,minSupp,minConf,maxSize,topK);

    }
}
