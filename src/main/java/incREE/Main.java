package incREE;

import incREE.dataset.Input;
import incREE.dataset.Relation;
import incREE.evidence.*;
import incREE.evidence.incEvidence.IncEvidenceSetBuilder;
import incREE.staticDC.CoverFinder;

import java.io.IOException;
import java.util.*;

public class Main {

    private static final int CURRENT_LINES = 30;
    private static final int INC_LINES = 170;

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

//    private static void findCover() throws IOException {
//        Map<PredicateBitmap, Integer> evidence = FileManager.loadEvidence(CURRENT_LINES);
//        Input input = new Input(FileManager.relationFileName());
//        Relation r = input.getRelation(1, 0);
//        CoverFinder coverFinder = new CoverFinder(0, r.getTotalTuplePairs(), evidence, r.predicateGroups);
//        List<PredicateBitmap> cover = coverFinder.findCover();
//        System.out.println("Cover find complete.");
//        FileManager.saveCover(CURRENT_LINES, cover);
//    }

    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();

//        saveColumnPairs();
//        buildEvidenceSetAndSave();
        mergeAndSave();
//        findCover();
//        writeExpression();

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        System.out.println("Complete in " + elapsedTime + " ms.");
    }
}
