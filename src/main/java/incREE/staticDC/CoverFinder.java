package incREE.staticDC;

import incREE.evidence.*;

import java.util.*;

public class CoverFinder {

    private static final int AIM_DC_NUM = -1;
    private static final int MAX_DC_LENGTH = 6;

    private final double errorThreshold;
    private final List<Evidence> er;
    private final List<AbstractPredicateGroup> predicateGroups;
    private final int predicateNum;

//    public record Cover(
//            PredicateBitmap containing,
//            PredicateBitmap forwards,
//            int uncovered
//    ) {}
    public static class Cover {
        public PredicateBitmap containing;
        public PredicateBitmap forwards;
        public int uncovered;

        public Cover(PredicateBitmap containing, PredicateBitmap forwards, int uncovered) {
            this.containing = containing;
            this.forwards = forwards;
            this.uncovered = uncovered;
        }
}

    public static class Result {
        public List<Cover> covers = new ArrayList<>();
        public List<Cover> terminals = new ArrayList<>();
    }

    public CoverFinder(double errorRateThreshold, int totalTuplePairsNum, List<Evidence> er, List<AbstractPredicateGroup> predicateGroups) {
        this.errorThreshold = errorRateThreshold * totalTuplePairsNum;
        this.er = er;
        this.predicateGroups = predicateGroups;
        this.predicateNum = predicateGroups.get(0).getAllPredicatesNum();
    }

    public CoverFinder(double errorRateThreshold, int totalTuplePairsNum, Map<PredicateBitmap, Integer> evidenceMap, List<AbstractPredicateGroup> predicateGroups) {
        this.errorThreshold = errorRateThreshold * totalTuplePairsNum;
        this.er = Evidence.fromMap(evidenceMap);
        this.predicateGroups = predicateGroups;
        this.predicateNum = predicateGroups.get(0).getAllPredicatesNum();
    }

    private record IntegerPair(int left, int right) implements Comparable<IntegerPair> {
        @Override
        public int compareTo(IntegerPair other) {
            return Integer.compare(left, other.left);
        }
    }

    /**
     * q is super set of any PredicateBitmap from cover
     */
    private boolean isImplied(PredicateBitmap q, List<Cover> cover) {
        for (Cover c : cover) {
            if (c.containing.isSubsetOf(q)) {
                return true;
            }
        }
        return false;
    }

    /**
     * no subset of size |q|-1 cover this.er
     */
    private boolean isMinimal(PredicateBitmap q) {
        for (int i = q.nextSetBit(0); i >= 0; i = q.nextSetBit(i + 1)) {
            PredicateBitmap pSub = q.copy();
            pSub.getBitSet().set(i, false);
            if (Evidence.satisfies(pSub, er, (int) errorThreshold)) {
                return false;
            }
        }
        return true;
    }

    private void findCover(PredicateBitmap pPath, List<Evidence> uncoveredEvidence, PredicateBitmap pForward, Result result) {
//        System.out.println("Node reached: Path = " +pPath.toSetString()+", Forward evidence = "+Evidence.size(uncoveredEvidence));

        int uncoveredSize = Evidence.size(uncoveredEvidence);
        if (uncoveredSize <= errorThreshold) {
            if (isMinimal(pPath)) {
                result.covers.add(new Cover(pPath, null, uncoveredSize));
            }
            return;
//        } else if (pForward.isEmpty() || pPath.size() >= MAX_DC_LENGTH) {
//            System.err.println("CoverFinder: 88");
//            result.terminals.add(new Cover(pPath, uncoveredSize));
//            return;
        } else {
            // sort pForward
            List<IntegerPair> coverage = new ArrayList<>();
            // for all true bits from pForward
            for (int i = pForward.nextSetBit(0); i >= 0; i = pForward.nextSetBit(i + 1)) {
                int coverageCount = 0;
                for (Evidence e : uncoveredEvidence) {
                    if (e.predicates().get(i)) {
                        coverageCount += e.multiplicity();
                    }
                }
                coverage.add(new IntegerPair(coverageCount, i));
            }
            coverage.sort(Comparator.reverseOrder());

            for (IntegerPair p : coverage) {
                if (p.left() * (MAX_DC_LENGTH - pPath.size()) < uncoveredSize - errorThreshold) {
                    result.terminals.add(new Cover(pPath, pForward, uncoveredSize));
                    return;
                }
                PredicateBitmap pPathNew = pPath.copy();
                pPathNew.set(p.right);
//                if (isImplied(pPathNew, result.covers)) {
//                    // pPathNew.remove(p); // Useless
//                    System.err.println("CoverFinder: 115");
//                    continue;
//                }
//                if (isImplied(pPathNew, result.terminals)) {
//                    System.err.println("CoverFinder: 119");
//                    // pPathNew.remove(p); // Useless
//                    continue;
//                }
                List<Evidence> uncoveredEvidenceNew = new ArrayList<>(uncoveredEvidence);
                uncoveredEvidenceNew.removeIf(evidence -> evidence.predicates().get(p.right));
                PredicateBitmap pForwardNew = pForward.copy();

                // remove all predicate from the same group
                AbstractPredicateGroup group = AbstractPredicateGroup.findGroup(p.right, predicateGroups);
                pForwardNew.andNot(group.bits);

                pForward.getBitSet().set(p.right, false);

                findCover(pPathNew, uncoveredEvidenceNew, pForwardNew, result);
                if (AIM_DC_NUM > 0 && result.covers.size() >= AIM_DC_NUM) {
                    return;
                }
            }
        }
    }

    public Result findCover() {
        Result covers = new Result();
        PredicateBitmap q = new PredicateBitmap();
        PredicateBitmap pForward = new PredicateBitmap();
        pForward.set(0, predicateNum);
        findCover(q, er, pForward, covers);
        return covers;
    }
}
