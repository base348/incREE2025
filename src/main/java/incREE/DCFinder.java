package incREE;

import incREE.cover.Cover;
import incREE.cover.DynEI;
import incREE.cover.StaticCoverFinder;
import incREE.dataset.Input;
import incREE.dataset.Relation;
import incREE.evidence.*;
import incREE.evidence.incEvidence.IncEvidenceSetBuilder;
import incREE.helpers.Timer;

import java.io.IOException;
import java.util.*;

public class DCFinder {

    private static void buildEvidenceSetAndSave(int lines) {
        Map<PredicateBitmap, Integer> e = buildEvidenceSet(lines);
        FileManager.saveEvidence(lines, e);
    }

    private static void mergeAndSave(int cur, int inc) {
        Map<PredicateBitmap, Integer> evidence;
        Map<PredicateBitmap, Integer> incEvidence;
        evidence = Evidence.toMap(FileManager.loadEvidence(cur));
        incEvidence = buildIncEvidenceSet(cur, inc);
        Evidence.merge(evidence, incEvidence);
        FileManager.saveEvidence(cur + inc, evidence);
    }

    private static Map<PredicateBitmap, Integer> buildIncEvidenceSet(int cur, int inc) {
        Input input = new Input(FileManager.relationFileName()).read(cur).readInc(inc);
        Relation r = input.getRelation();
        IncEvidenceSetBuilder builder = new IncEvidenceSetBuilder(r, inc);
        return builder.build();
    }

    private static Map<PredicateBitmap, Integer> buildEvidenceSet(int lines) {
        return buildIncEvidenceSet(0, lines);
    }

    private static void saveColumnPairs() {
        Relation r = new Input(FileManager.relationFileName()).read().getRelation();
        List<DataPredicateGroup> predicateGroups = r.predicateGroups;
        FileManager.saveColumnPairs(predicateGroups);
    }

    private static void findCover(int tupleLines, int errorThreshold, int dcLength) {
        System.out.println("Finding cover...");
        List<Evidence> evidence = FileManager.loadEvidence(tupleLines);
        List<AbstractPredicateGroup> predicateGroups = FileManager.loadAbstractPredicateGroups();
//        int errorThreshold = 100;
        dcLength = Math.min(dcLength, predicateGroups.size());
        StaticCoverFinder coverFinder = new StaticCoverFinder(errorThreshold,
                evidence, predicateGroups, dcLength, false);
        StaticCoverFinder.Result r = coverFinder.findCover();
        System.out.println("Cover find complete.");
        System.out.println(r.covers.size() + " covers found.");
        FileManager.saveCover(tupleLines, r.covers, dcLength, errorThreshold);

        List<Evidence> uncovered = Evidence.fromMap(r.uncovered);
//        for (Evidence e : evidence) {
//            if (!isAllCovered(e, r.covers)) {
//                uncovered.add(e);
//            }
//        }

        FileManager.saveUncovered(tupleLines, uncovered, dcLength, errorThreshold);
    }

    private static boolean in(Cover cover, List<Cover> covers) {
        for (Cover c : covers) {
            if (cover.containing.equals(c.containing)) {
                return true;
            }
        }
        return false;
    }

    private static int numChildren(Cover cover, List<Cover> covers) {
        int numChildren = 0;
        for (Cover c : covers) {
            if (cover.containing.isSubsetOf(c.containing)) {
                numChildren ++;
            }
        }
        return numChildren;
    }

    private static String getCoverRepresentation(Cover cover, List<AbstractPredicate> allPredicates) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = cover.containing.nextSetBit(0); i >= 0; i = cover.containing.nextSetBit(i + 1)) {
            sb.append(allPredicates.get(i).getExpression());
            sb.append(" or ");
        }
        sb.delete(sb.length() - 4, sb.length()).append(")");
        return sb.toString();
    }

    private static void findCoverInc(int current, int inc, int errorThreshold, int dcLength) {
        System.out.println("Finding cover...");
        List<Cover> cover = FileManager.loadCover(current, dcLength, errorThreshold);
        List<Evidence> evidence = FileManager.loadEvidence(current);
        List<Evidence> evidenceAll = FileManager.loadEvidence(current + inc);
        List<Evidence> evidenceUncovered;
        evidenceUncovered = FileManager.loadUncovered(current, dcLength, errorThreshold);
        List<AbstractPredicateGroup> predicateGroups = FileManager.loadAbstractPredicateGroups();

        DynEI finder = new DynEI(evidenceAll, evidence, evidenceUncovered, cover, predicateGroups, errorThreshold, dcLength);
        StaticCoverFinder.Result r = finder.run();
        FileManager.saveCover(current + inc, r.covers, dcLength, errorThreshold);

//        List<Evidence> uncovered = new ArrayList<>();
//        for (Evidence e : evidenceAll) {
//            if (DynEI.notAllCovered(e, cover)) {
//                uncovered.add(e);
//            }
//        }

        FileManager.saveUncovered(current + inc, Evidence.fromMap(r.uncovered), dcLength, errorThreshold);
    }

    private static void analyzeCover() throws IOException {
        List<Cover> cover = FileManager.loadCover(3002, 6, 5);
        List<Cover> coverInc = FileManager.loadCover(3000, 6, 5);
//        cover.sort();
        int i = 0;
        for (Cover c : cover) {
            i ++;
            Iterator<Cover> it = coverInc.iterator();
            while (it.hasNext()) {
                Cover ci = it.next();
                if (c.containing.equals(ci.containing)) {
                    c.uncovered -= ci.uncovered;
                    it.remove();
                    break;
                }
            }
        }
        cover.sort(Comparator.comparingInt(c -> c.uncovered));
        System.out.println(cover.size());
    }

    private static void saveUncoveredEvidence(int current, int errorThreshold, int dcLength) throws IOException {
        List<Cover> cover = FileManager.loadCover(current, dcLength, errorThreshold);
        List<Evidence> evidence = FileManager.loadEvidence(current);

        List<Evidence> uncovered = new ArrayList<>();
        for (Evidence e : evidence) {
            if (DynEI.notAllCovered(e, cover)) {
                uncovered.add(e);
            }
        }

        FileManager.saveUncovered(current, uncovered, dcLength, errorThreshold);

        int size = evidence.size();
        int uncoveredSize = uncovered.size();
        System.out.println("All Evidence: " + size + "; All Covered: " + (size - uncoveredSize) + "; Not Covered: " + uncoveredSize);
        System.out.println((double) uncoveredSize / size);
    }

    public static void staticDC(int current, int errorThreshold, int dcLength) {
        System.out.println("Finding Static DCs...");
        Timer timer = new Timer();
        buildEvidenceSetAndSave(current);
        timer.evidenceSetComplete();
        findCover(current, errorThreshold, dcLength);
        timer.end();
    }

    public static void incDC(int current, int inc, int errorThreshold, int dcLength) {
        System.out.println("Finding Incremental DCs...");
        Timer timer = new Timer();
        mergeAndSave(current, inc);
        timer.evidenceSetComplete();
        findCoverInc(current, inc, errorThreshold, dcLength);
        timer.end();
    }
}
