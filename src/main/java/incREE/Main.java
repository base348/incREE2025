package incREE;

import com.google.gson.Gson;

import incREE.dataset.Input;
import incREE.dataset.Relation;
import incREE.evidence.*;
import incREE.evidence.incEvidence.IncEvidenceSetBuilder;
import incREE.incDC.DynEI;
import incREE.staticDC.DCFinder;
import incREE.staticDC.DCRanker;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class Main {

    private static final int CURRENT_LINES = 150;
    private static final int INC_LINES = 100;

    private static final Gson gson = new Gson();

    private static void saveEvidence(String filename, Map<PredicateBitmap, Integer> evidence) {
        try (FileWriter writer = new FileWriter(filename)) { // "evidence.json"
            gson.toJson(evidence, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<PredicateBitmap, Integer> loadEvidence(String filename) {
        try (FileReader reader = new FileReader("person.json")) {
            Map<PredicateBitmap, Integer> evidence = gson.fromJson(reader, Map.class);
            return evidence;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void test() {
        Input input = new Input("adult.csv");
        Relation r = input.getRelation(CURRENT_LINES);
        EvidenceSetBuilder builder = new EvidenceSetBuilder(r);
        builder.buildEvidenceSet();
        Map<PredicateBitmap, Integer> e = builder.collect();
        System.out.println("Evidence Set build complete.");
//        DCFinder dcFinder = new DCFinder(0.1, r.getTotalTuplePairs(), r.predicateSpace, e1);
//        List<List<Predicate<?>>> cover = dcFinder.findCover();

        IncEvidenceSetBuilder incEvidence = new IncEvidenceSetBuilder(r, INC_LINES);
        Map<PredicateBitmap, Integer> eInc = incEvidence.build();
        System.out.println("Inc Evidence Set build complete.");

        DCFinder dcFinder = new DCFinder(0, r.getTotalTuplePairs(), r.predicateSpace, e);
        List<List<Predicate<?>>> cover = dcFinder.findCover();
        System.out.println("DC Finder complete.");

        DynEI incDC = new DynEI(e, eInc, r.predicateSpace, cover);
        incDC.DynDC();

//        cover.forEach(dc -> Checker.checkDCNaive(r, dc));
//        DCRanker ranker = new DCRanker(cover, er);
//        ranker.getRankedDC();
    }

    private static void buildEvidenceSetAndSave(String filename) {
        Input input = new Input(filename + ".csv");
        Relation r = input.getRelation(CURRENT_LINES);
        EvidenceSetBuilder builder = new EvidenceSetBuilder(r);
        builder.buildEvidenceSet();
        Map<PredicateBitmap, Integer> e = builder.collect();
        saveEvidence("evidence_adult_150.json", e);
    }

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        Input input = new Input("adult.csv");
        Relation r = input.getRelation(CURRENT_LINES);
        EvidenceSetBuilder builder = new EvidenceSetBuilder(r);
        builder.buildEvidenceSet();
        Map<PredicateBitmap, Integer> e = builder.collect();
        saveEvidence("evidence_adult_150.json", e);

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        System.out.println("Evidence Set build complete in " + elapsedTime + " ms.");
    }
}
