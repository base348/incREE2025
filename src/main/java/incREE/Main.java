package incREE;

import incREE.cover.Cover;
import incREE.cover.DynEI;
import incREE.dataset.Input;
import incREE.dataset.Relation;
import incREE.evidence.*;
import incREE.evidence.incEvidence.IncEvidenceSetBuilder;
import incREE.cover.StaticCoverFinder;

import java.io.IOException;
import java.util.*;

public class Main {

    private static final int CURRENT_LINES = 5000;
    private static final int INC_LINES = 10;

    private static void buildEvidenceSetAndSave(int lines) {
        Map<PredicateBitmap, Integer> e = buildEvidenceSet(lines);
        FileManager.saveEvidence(lines, e);
    }

    private static void mergeAndSave(int cur, int inc) {
        Map<PredicateBitmap, Integer> evidence;
        Map<PredicateBitmap, Integer> incEvidence;
        try {
            evidence = Evidence.toMap(FileManager.loadEvidence(cur));
        } catch (IOException e) {
            System.out.println("Building the required evidence map instead.");
            evidence = buildEvidenceSet(cur);
        }
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

    private static void findCover(int tupleLines, int errorThreshold, int dcLength) throws IOException {
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

    private static void analyzeIncCover(int current, int inc, int errorThreshold, int dcLength) throws IOException {
        List<Cover> cover = FileManager.loadCover(current, dcLength, errorThreshold);
        List<Evidence> evidence = FileManager.loadEvidence(current);
        List<Evidence> evidenceAll = FileManager.loadEvidence(current + inc);
        List<Evidence> evidenceUncovered;
        try {
            evidenceUncovered = FileManager.loadUncovered(current, dcLength, errorThreshold);
        } catch (IOException ex) {
            System.err.println("Error loading uncovered evidence. Calculating instead.");
            evidenceUncovered = new ArrayList<>();
            for (Evidence e : evidence) {
                if (DynEI.notAllCovered(e, cover)) {
                    evidenceUncovered.add(e);
                }
            }
        }
        List<AbstractPredicateGroup> predicateGroups = FileManager.loadAbstractPredicateGroups();

        DynEI finder = new DynEI(evidenceAll, evidence, evidenceUncovered, cover, predicateGroups, errorThreshold, dcLength);
        finder.run();
    }

    private static void findCoverInc(int current, int inc, int errorThreshold, int dcLength) throws IOException {
        List<Cover> cover = FileManager.loadCover(current, dcLength, errorThreshold);
        List<Evidence> evidence = FileManager.loadEvidence(current);
        List<Evidence> evidenceAll = FileManager.loadEvidence(current + inc);
        List<Evidence> evidenceUncovered;
        try {
            evidenceUncovered = FileManager.loadUncovered(current, dcLength, errorThreshold);
        } catch (IOException ex) {
            System.err.println("Error loading uncovered evidence. Calculating instead.");
            evidenceUncovered = new ArrayList<>();
            for (Evidence e : evidence) {
                if (DynEI.notAllCovered(e, cover)) {
                    evidenceUncovered.add(e);
                }
            }
        }
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

    private static void analyzeSymmetric(List<AbstractPredicateGroup> predicateGroups) throws IOException {
        List<Cover> cover5 = FileManager.loadCover(CURRENT_LINES, 6, 0);
        System.out.println("Total number of covers: " + cover5.size());
        List<AbstractPredicate> allPredicates = predicateGroups.get(0).allPredicates;
        int selfSymmetric = 0;
        int notSymmetric = 0;
        int notSymmetric2 = 0;
        for (int i = 0; ; i++) {
            if (i >= cover5.size()) {
                break;
            }
            Cover c = cover5.get(i);
            if (!c.containing.isSelfSymmetry(predicateGroups)) {
                Cover image = new Cover(c.containing.getImage(predicateGroups), c.uncovered);
                boolean success = false;
                for (int j = i+1; j < cover5.size(); j++) {
                    Cover c2 = cover5.get(j);
                    if (c2.containing.equals(image.containing) && c2.uncovered == image.uncovered) {
                        cover5.remove(j);
                        success = true;
                        break;
                    }
                }

//                selfSymmetric ++;
                if (success) {
                    notSymmetric ++;
//                    System.err.println("Image found: " + c.containing.toSetString() + ", " + getCoverRepresentation(c, allPredicates));
//                    System.err.println("Image found: " + image.containing.toSetString() + ", " + getCoverRepresentation(image, allPredicates));
                } else {
                    notSymmetric2 ++;
                }
            } else {
                selfSymmetric ++;
            }
        }
        System.out.println("Self symmetric :"  + selfSymmetric  + " paired not symmetric :"  + notSymmetric +  "  not symmetric 2 :" + notSymmetric2);
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

    private static void incDC(int cur, int inc, int errorThreshold, int dcLength) throws IOException {
        mergeAndSave(cur, inc);
        findCoverInc(cur, inc, errorThreshold, dcLength);
    }

    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();

//        saveColumnPairs();
        buildEvidenceSetAndSave(20000);
//        mergeAndSave(30000, 20000);
        findCover(20000, 5, 6);
//        findCoverInc(30000, 20000, 5, 6);
//        analyzeIncCover (2000, 1000,5,6);
//        saveUncoveredEvidence(2000, 5, 6);
//        analyzeCover();
//        incDC(50000, 2500, 5, 6);

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        System.out.println("Complete in " + elapsedTime + " ms.");

//        List<Evidence> evidence = FileManager.loadEvidence(CURRENT_LINES);

//        List<AbstractPredicateGroup> predicateGroups = FileManager.loadAbstractPredicateGroups();
//        analyzeSymmetric(predicateGroups);
    }
}
