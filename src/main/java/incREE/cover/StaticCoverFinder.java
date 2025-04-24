package incREE.cover;

import incREE.evidence.*;

import java.util.*;

public class StaticCoverFinder {

    private static final int AIM_DC_NUM = -1;
    private final int maxDcLength;

    private final int errorThreshold;
    private final List<Evidence> er;
    private final List<AbstractPredicateGroup> predicateGroups;
    private final int predicateNum;

    public static class Result {
        public List<Cover> covers = new ArrayList<>();
        public List<Cover> terminals = new ArrayList<>();
    }

    public StaticCoverFinder(int errorThreshold, List<Evidence> er, List<AbstractPredicateGroup> predicateGroups, int dcLength) {
        this.errorThreshold = errorThreshold;
        this.er = er;
        this.predicateGroups = predicateGroups;
        this.predicateNum = predicateGroups.get(0).getAllPredicatesNum();
        this.maxDcLength = dcLength;
    }

    public StaticCoverFinder(int errorThreshold, Map<PredicateBitmap, Integer> evidenceMap, List<AbstractPredicateGroup> predicateGroups, int dcLength) {
        this.errorThreshold = errorThreshold;
        this.er = Evidence.fromMap(evidenceMap);
        this.predicateGroups = predicateGroups;
        this.predicateNum = predicateGroups.get(0).getAllPredicatesNum();
        this.maxDcLength = dcLength;
    }

    private record IntegerPair(long left, int right) implements Comparable<IntegerPair> {
        @Override
        public int compareTo(IntegerPair other) {
            return Long.compare(left, other.left);
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
            if (Evidence.satisfies(pSub, er, errorThreshold)) {
                return false;
            }
        }
        return true;
    }

    private void findCover(PredicateBitmap pPath, List<Evidence> uncoveredEvidence, PredicateBitmap pForward, Result result) {
//        System.out.println("Node reached: Path = " +pPath.toSetString()+", Forward evidence = "+Evidence.size(uncoveredEvidence));

        long uncoveredSize = Evidence.size(uncoveredEvidence);
        if (uncoveredSize <= errorThreshold) {
            if (isMinimal(pPath)) {
                result.covers.add(new Cover(pPath, null, uncoveredSize));
            }
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
                if (p.left() * (maxDcLength - pPath.size()) < uncoveredSize - errorThreshold) {
                    result.terminals.add(new Cover(pPath, pForward, uncoveredSize));
                    return;
                }
                PredicateBitmap pPathNew = pPath.copy();
                pPathNew.set(p.right);
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
