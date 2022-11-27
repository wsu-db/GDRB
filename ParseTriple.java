import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class ParseTriple {

    static HashMap<String, String> str_nodes = new HashMap();


    record PreProcessedTriple(String subject, String object, String predicate, int truthValue){ }

    public static String getPredicate(String predicate) {
        if (predicate.equals("<starring>"))
            return "<actedIn>";
        else if ((predicate.equals("<director>"))) {
            return "<directed>";
        } else if (predicate.equals("<producer>") | predicate.equals("<productionCompany>") | predicate.equals("<author>")) {
            return "<created>";
        } else if (predicate.equals("<award>")) {
            return "<hasWonPrize>";
        } else if (predicate.equals("<education>")) {
            return "<graduatedFrom>";
        } else if (predicate.equals("<birthPlace>")) {
            return "<wasBornIn>";
        } else if (predicate.equals("<deathPlace>")) {
            return "<diedIn>";
        } else if (predicate.equals("<team>")) {
            return "<isAffiliatedTo>";
        } else if (predicate.equals("<spouse>")){
            return "<isMarriedTo>";
        } else
            return predicate;
    }

    public static String parseTriples(String subject, String object, String predicate, int truthValue) {
        String parsedTriple = "";
        parsedTriple = subject + "," + getSubjectRelation(subject, predicate) + "," + getPredicate(predicate)
        + "," + object + "," + getObjectRelation(object, predicate);
        return parsedTriple;
    }

    public static PreProcessedTriple preprocessTriple(String subject, String object, String predicate, int truth_value) {
        String[] subjectTokens = subject.split("/");
        subject = String.format("<%s>",subjectTokens[subjectTokens.length - 1]);

        String[] objectTokens = object.split("/");
        object = String.format("<%s>",objectTokens[objectTokens.length - 1]);

        String[] predicateTokens = predicate.split("/");
        predicate = String.format("<%s>",predicateTokens[predicateTokens.length - 1]);


        return new PreProcessedTriple(subject, object, predicate, truth_value);
    }

    public static void readStrNodeFile() {
        if (str_nodes.isEmpty()) {
            String path = "SourceCode-GFC/sample_data/gfc_str_nodes.tsv";
            HashMap<String, String> hashMap = new HashMap<>();
            String line = "";
            try {
                BufferedReader br = new BufferedReader(new FileReader(path));
                while ((line = br.readLine()) != null) {
                    String[] strNode = line.split("\t");
                    hashMap.put(strNode[0], strNode[1]);
                }
                System.out.println(hashMap.size());
                str_nodes = hashMap;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static String getSubjectObjectOntology(String predicate, String returnType) {
        String subject = "";
        String object = "";
        if (predicate.equals("<director>")) {
            subject = "<wordnet_movie_106613686>";
            object = "<wordnet_director_110014939>";
        } else if (predicate.equals("<starring>")) {
            subject = "<wordnet_movie_106613686>";
            object = "<wordnet_actor_109765278>";
        } else if (predicate.equals("<producer>")) {
            subject = "<wikicategory_American_film_producers>";
            object = "<wordnet_company_108058098>";
        } else if (predicate.equals("<productionCompany>")) {
            subject = "<wordnet_movie_106613686>";
            object = "<wordnet_company_108058098>";
        } else if (predicate.equals("<award>")) {
            subject = "<wordnet_person_100007846>";
            object = "<wordnet_award_106696483>";
        } else if (predicate.equals("<education>")) {
            subject = "<wordnet_person_100007846>";
            object = "<wordnet_university_108286569>";
        } else if (predicate.equals("<birthPlace>")) {
            subject = "<wordnet_person_100007846>";
            object = "<wordnet_city_108524735>";
        } else if (predicate.equals("<deathPlace>")) {
            subject = "<wordnet_person_100007846>";
            object = "<wordnet_city_108524735>";
        } else if (predicate.equals("<team>")) {
            subject = "<wordnet_player_110439851>";
            object = "<wordnet_team_108208560>";
        } else if(predicate.equals("<spouse>")){
            subject = "<wordnet_person_100007846>";
            object = "<wordnet_person_100007846>";
        }
        if (returnType.equals("subject")) {
            return subject;
        } else
            return object;
    }

    public static String getSubjectRelation(String subject, String predicate) {
        if (str_nodes.containsKey(subject)) {
            return str_nodes.get(subject);
        } else {
            return getSubjectObjectOntology(predicate, "subject");
        }
    }

    public static String getObjectRelation(String object, String predicate){
        if(str_nodes.containsKey(object)){
            return str_nodes.get(object);
        }
        else{
            return getSubjectObjectOntology(predicate,"object");
        }
    }

    public static void main(String[] args) {
        readStrNodeFile();
        PreProcessedTriple preprocessedTriple = preprocessTriple("http://dbpedia.org/resource/Kinsey_(film)","http://dbpedia.org/resource/Tim_Curry","http://dbpedia.org/ontology/starring",1);
        System.out.println(parseTriples(preprocessedTriple.subject,preprocessedTriple.object,preprocessedTriple.predicate,preprocessedTriple.truthValue));
    }
}
