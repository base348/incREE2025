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

    private static final int CURRENT_LINES = 2000;
    private static final int INC_LINES = 10;

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
//        FileManager.saveUncovered(tupleLines, r.uncovered);
//        FileManager.saveTerminal(tupleLines, r.terminals, dcLength, errorThreshold);
//        FileManager.writeExpression(tupleLines, r.covers);
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
//        List<Cover> coverInc = FileManager.loadCover(CURRENT_LINES + INC_LINES, dcLength, errorThreshold);

        // find cover by inc method
        List<Evidence> evidence = FileManager.loadEvidence(current);
        List<Evidence> evidenceAll = FileManager.loadEvidence(current + inc);
        List<AbstractPredicateGroup> predicateGroups = FileManager.loadAbstractPredicateGroups();

        DynEI finder = new DynEI(evidenceAll, evidence, cover, predicateGroups, errorThreshold, dcLength);
        finder.run();

//        List<Cover> notfound = new ArrayList<>();

//        for (Cover c : coverInc) {
//            if (cover.contains(c) || in(c, cover)) {
//                continue;
//            } else {
//                notfound.add(c);
//            }
//
//        }

//        System.out.println("Cover size: " + cover.size() + ", inc cover size: " +coverInc.size());
    }

    private static void findCoverInc(int current, int inc, int errorThreshold, int dcLength) throws IOException {
        List<Cover> cover = FileManager.loadCover(current, dcLength, errorThreshold);
        List<Evidence> evidence = FileManager.loadEvidence(current);
        List<Evidence> evidenceAll = FileManager.loadEvidence(current + inc);
        List<AbstractPredicateGroup> predicateGroups = FileManager.loadAbstractPredicateGroups();
        DynEI finder = new DynEI(evidenceAll, evidence, cover, predicateGroups, errorThreshold, dcLength);
        cover = finder.run();
//        System.out.println("Cover find complete.");
//        System.out.println(r.covers.size() + " covers found.");
        FileManager.saveCover(current + inc, cover, dcLength, errorThreshold);
    }

    private static void analyzeCover(Cover cover, List<Evidence> er, List<AbstractPredicateGroup> predicateGroups) {
        List<Evidence> uncovered = new ArrayList<>();
        int length = cover.containing.size();
        String rep = getCoverRepresentation(cover, predicateGroups.get(0).allPredicates);
        int uncoveredCount = 0;
        for (Evidence evidence : er) {
            if (!evidence.isCoveredBy(cover.containing)) {
                uncovered.add(evidence);
                uncoveredCount += evidence.multiplicity();
            }
        }
        PredicateBitmap bits = new PredicateBitmap();
        for (int i = cover.containing.nextSetBit(0); i >= 0; i = cover.containing.nextSetBit(i + 1)) {
            bits.or(AbstractPredicateGroup.findGroup(i, predicateGroups).bits);
        }
        System.out.println(bits.toSetString());
        uncovered.forEach(evidence -> {
//            System.out.println(evidence.predicates().toSetString());
            evidence.predicates().andNot(bits);
//            System.out.println(evidence.predicates().toSetString());
//            System.out.println();
        });
//        System.out.println(uncoveredCount);

        StaticCoverFinder coverFinder = new StaticCoverFinder(0, er, predicateGroups, 6, true);
        StaticCoverFinder.Result r = coverFinder.findCover(cover.containing);
        System.out.println("Cover find complete.");
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

    private static boolean isAllCovered(Evidence evidence, List<Cover> cover) {
        for (Cover c : cover) {
            if (!evidence.isCoveredBy(c.containing)) {
                return false;
            }
        }
        return true;
    }

    private static void saveUncoveredEvidence(int current, int errorThreshold, int dcLength) throws IOException {
        List<Cover> cover = FileManager.loadCover(current, dcLength, errorThreshold);
        List<Evidence> evidence = FileManager.loadEvidence(current);

        List<Evidence> uncovered = new ArrayList<>();
        for (Evidence e : evidence) {
            if (isAllCovered(e, cover)) {
                uncovered.add(e);
            }
        }

        FileManager.saveUncovered(current, uncovered, dcLength, errorThreshold);

        int size = evidence.size();
        int allCoveredSize = uncovered.size();
        System.out.println("All Evidence: " + size + "; All Covered: " + allCoveredSize + "; Not Covered: " + (size - allCoveredSize));
        System.out.println((double) allCoveredSize / size);
    }

    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();

//        saveColumnPairs();
//        buildEvidenceSetAndSave();
//        mergeAndSave();
//        findCover(2000, 5, 6);
//        findCoverInc(2000, 80, 5, 6);
        analyzeIncCover (5000, 0,5,6);
//        saveUncoveredEvidence(5000, 5, 6);

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        System.out.println("Complete in " + elapsedTime + " ms.");

//        List<Evidence> evidence = FileManager.loadEvidence(CURRENT_LINES);

//        List<AbstractPredicateGroup> predicateGroups = FileManager.loadAbstractPredicateGroups();
//        analyzeSymmetric(predicateGroups);
    }
}
