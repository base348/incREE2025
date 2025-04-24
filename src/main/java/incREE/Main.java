package incREE;

import incREE.dataset.Input;
import incREE.dataset.Relation;
import incREE.evidence.*;
import incREE.evidence.incEvidence.IncEvidenceSetBuilder;
import incREE.cover.StaticCoverFinder;

import java.io.IOException;
import java.util.*;

public class Main {

    private static final int CURRENT_LINES = 30000;
    private static final int INC_LINES = 30000;

    private static void merge(Map<PredicateBitmap, Integer> map1, Map<PredicateBitmap, Integer> map2) {
        map2.forEach((key, value) -> map1.merge(key, value, Integer::sum));
    }

    private static void subtract(Map<PredicateBitmap, Integer> map1, Map<PredicateBitmap, Integer> map2) {
        map2.forEach((key, value) -> {
            map1.merge(key, -value, Integer::sum); // 减去对应的值
            map1.remove(key, 0); // 如果减完后值为0，则移除该键值对
        });
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

    private static void buildEvidenceSetAndSave() {
        Map<PredicateBitmap, Integer> e = buildEvidenceSet();
        FileManager.saveEvidence(CURRENT_LINES, e);
    }

    private static Map<PredicateBitmap, Integer> buildEvidenceSet() {
        Input input = new Input(FileManager.relationFileName()).read(CURRENT_LINES);
        Relation r = input.getRelation();
        EvidenceSetBuilder builder = new EvidenceSetBuilder(r);
        builder.buildEvidenceSetNaive();
        return builder.collect();
    }

    private static Map<PredicateBitmap, Integer> buildIncEvidenceSet() {
        Input input = new Input(FileManager.relationFileName()).read(CURRENT_LINES).readInc(INC_LINES);
        Relation r = input.getRelation();
        IncEvidenceSetBuilder builder = new IncEvidenceSetBuilder(r, INC_LINES);
        return builder.build();
    }

    private static void saveColumnPairs() {
        Relation r = new Input(FileManager.relationFileName()).read().getRelation();
        List<DataPredicateGroup> predicateGroups = r.predicateGroups;
        FileManager.saveColumnPairs(predicateGroups);
    }

    private static void findCover(double errorRate, int dcLength) throws IOException {
        List<Evidence> evidence = FileManager.loadEvidence(CURRENT_LINES);
        List<AbstractPredicateGroup> predicateGroups = FileManager.loadAbstractPredicateGroups();
        int errorThreshold = (int)(errorRate * Evidence.size(evidence));
        dcLength = Math.min(dcLength, predicateGroups.size());
        StaticCoverFinder coverFinder = new StaticCoverFinder(errorThreshold, evidence, predicateGroups, dcLength);
        StaticCoverFinder.Result r = coverFinder.findCover();
        System.out.println("Cover find complete.");
        FileManager.saveCover(CURRENT_LINES, r.covers, dcLength, errorThreshold);
        FileManager.saveTerminal(CURRENT_LINES, r.terminals, dcLength, errorThreshold);
//        FileManager.writeExpression(CURRENT_LINES, r.covers);
    }

    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();

//        saveColumnPairs();
//        buildEvidenceSetAndSave();
//        mergeAndSave();
        findCover(0, 6);

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        System.out.println("Complete in " + elapsedTime + " ms.");
    }
}
