package incREE;

import com.google.gson.stream.JsonWriter;
import incREE.dataset.Input;
import incREE.dataset.Relation;
import incREE.evidence.*;
import incREE.evidence.incEvidence.IncEvidenceSetBuilder;
import incREE.staticDC.CoverFinder;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class Main {

    private static final int CURRENT_LINES = 10000;
    private static final int INC_LINES = 9800;

    private static void merge(Map<PredicateBitmap, Integer> map1, Map<PredicateBitmap, Integer> map2) {
        map2.forEach((key, value) -> map1.merge(key, value, Integer::sum));
    }

    private static void mergeAndSave() {
        Map<PredicateBitmap, Integer> evidence;
        Map<PredicateBitmap, Integer> incEvidence;
        try {
            evidence = Evidence.toMap(FileManager.loadEvidence(CURRENT_LINES));
        } catch (IOException e) {
            System.out.println("Building the required evidence map instead.");
            evidence = buildEvidenceSet();
        }
        incEvidence = buildIncEvidenceSet();
        merge(evidence, incEvidence);
        FileManager.saveEvidence(CURRENT_LINES + INC_LINES, evidence);
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

//        CoverFinder dcFinder = new CoverFinder(0, r.getTotalTuplePairs(), e);
//        List<List<Predicate<?>>> cover = dcFinder.findCover();
//        System.out.println("DC Finder complete.");

//        DynEI incDC = new DynEI(e, eInc, r.predicateSpace, cover);
//        incDC.DynDC();

//        cover.forEach(dc -> Checker.checkDCNaive(r, dc));
//        DCRanker ranker = new DCRanker(cover, er);
//        ranker.getRankedDC();
    }

    private static void buildEvidenceSetAndSave() {
        Map<PredicateBitmap, Integer> e = buildEvidenceSet();
        FileManager.saveEvidence(CURRENT_LINES, e);
    }

    private static Map<PredicateBitmap, Integer> buildEvidenceSet() {
        Input input = new Input(FileManager.relationFileName());
        Relation r = input.getRelation(CURRENT_LINES);
        EvidenceSetBuilder builder = new EvidenceSetBuilder(r);
        builder.buildEvidenceSetNaive();
        return builder.collect();
    }

    private static Map<PredicateBitmap, Integer> buildIncEvidenceSet() {
        Input input = new Input(FileManager.relationFileName());
        Relation r = input.getRelation(CURRENT_LINES, INC_LINES);
        IncEvidenceSetBuilder builder = new IncEvidenceSetBuilder(r, INC_LINES);
        return builder.build();
    }

    private static void saveColumnPairs() {
        Input input = new Input(FileManager.relationFileName());
        Relation r = input.getRelation(CURRENT_LINES + INC_LINES);
        List<DataPredicateGroup> predicateGroups = r.predicateGroups;

        FileManager.saveColumnPairs(predicateGroups);
    }

    private static void findCover() throws IOException {
        List<Evidence> evidence = FileManager.loadEvidence(CURRENT_LINES);
        List<AbstractPredicateGroup> predicateGroups = FileManager.loadAbstractPredicateGroups();
        CoverFinder coverFinder = new CoverFinder(0, Evidence.size(evidence), evidence, predicateGroups);
        CoverFinder.Result r = coverFinder.findCover();
        System.out.println("Cover find complete.");
//        FileManager.saveCover(CURRENT_LINES, r);
//        FileManager.saveTerminal(CURRENT_LINES, r);
//        FileManager.writeExpression(CURRENT_LINES, r);
        FileManager.trailSave(CURRENT_LINES, r);
    }

    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();

//        saveColumnPairs();
//        buildEvidenceSetAndSave();
//        mergeAndSave();
        findCover();
//        FileManager.trailLoad(CURRENT_LINES);

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        System.out.println("Complete in " + elapsedTime + " ms.");
    }
}
