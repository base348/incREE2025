package incREE.cover;

import incREE.evidence.*;

import java.util.*;

public class StaticCoverFinder {

    private static final int AIM_DC_NUM = -1;
    private final int maxDcLength;

    private final int errorThreshold;
    private final List<Evidence> er;
    private final List<AbstractPredicateGroup> predicateGroups;
    private final List<AbstractPredicate> allPredicates;
    private final int predicateNum;
    private final boolean useSymmetry;

    private final List<PredicateBitmap> previous = new ArrayList<>();

    private final PredicateBitmap images = new PredicateBitmap();

    public static class Result {
        public List<Cover> covers = new ArrayList<>();
//        public List<Cover> terminals = new ArrayList<>();
        public Map<PredicateBitmap, Integer> uncovered = new HashMap<>();
    }

    public StaticCoverFinder(int errorThreshold, List<Evidence> er, List<AbstractPredicateGroup> predicateGroups, int dcLength, boolean useSymmetry) {
        this.errorThreshold = errorThreshold;
        this.er = er;
        this.predicateGroups = predicateGroups;
        this.allPredicates = predicateGroups.get(0).allPredicates;
        this.predicateNum = predicateGroups.get(0).getAllPredicatesNum();
        this.maxDcLength = dcLength;
        this.useSymmetry = useSymmetry;

        initImage();
    }

    private void initImage() {
        for (AbstractPredicateGroup group : predicateGroups) {
            if (group.isReflexive()) {
                if (group.isNumeric()) {
                    images.or(group.getImages());
                }
            } else {
                if (!group.isMajor()) {
                    images.or(group.bits);
                }
            }
        }
    }

    private record IntegerPair(long left, int right) implements Comparable<IntegerPair> {
        @Override
        public int compareTo(IntegerPair other) {
            return Long.compare(left, other.left);
        }
    }

    /**
     * q is super or equal set of any PredicateBitmap from cover
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
            if (Evidence.isApproximateCover(pSub, er, errorThreshold)) {
                return false;
            }
        }
        return true;
    }

//    private boolean inUpClosure(PredicateBitmap pPath,  List<PredicateBitmap>  covers) {
//        if (covers.isEmpty())
//            return false;
//    }

    public void findCover(PredicateBitmap pPath, List<Evidence> uncoveredEvidence, PredicateBitmap pForward,
                          Result result, boolean imageLock) {
//        System.out.println("Node reached: Path = " +pPath.toSetString()+", Forward evidence = "+Evidence.size(uncoveredEvidence));
//        if (check && isImplied(pPath, result.covers)) {
//            return;
//        }
        if (!previous.isEmpty()) {
            for (PredicateBitmap p : previous) {
                if (p.isSubsetOf(pPath)) {
                    return;
                }
            }
        }
        long uncoveredSize = Evidence.size(uncoveredEvidence);
        if (uncoveredSize <= errorThreshold) {
            if (isMinimal(pPath)) {
                result.covers.add(new Cover(pPath, uncoveredSize));
                uncoveredEvidence.forEach((evidence) -> {
                    result.uncovered.put(evidence.predicates(), evidence.multiplicity());
                });
            }
        } else {
            // sort pForward
            List<IntegerPair> coverage = new ArrayList<>();
            // for all true bits from pForward
            if (!imageLock) {
                getCoverage(uncoveredEvidence, coverage, pForward);
            } else {
                PredicateBitmap available = pForward.copy();
                available.andNot(images);
                getCoverage(uncoveredEvidence, coverage, available);
            }
            coverage.sort(Comparator.reverseOrder());

            for (IntegerPair p : coverage) {
                if (p.left() * (maxDcLength - pPath.size()) < uncoveredSize - errorThreshold) {
//                    result.terminals.add(new Cover(pPath, pForward, uncoveredSize));
                    return;
                }
                PredicateBitmap pPathNew = pPath.copy();
                pPathNew.set(p.right);

                boolean next = imageLock;
                if (useSymmetry) {
                    if (!allPredicates.get(p.right).isSelfSymmetry()){
                        if (imageLock) {
                            next = false;
                        } else {
                            if (pPathNew.isSelfSymmetry(predicateGroups)){
                                next = true;
                            }
                        }
                    }
                }

                List<Evidence> uncoveredEvidenceNew = new ArrayList<>(uncoveredEvidence);
                uncoveredEvidenceNew.removeIf(evidence -> evidence.predicates().get(p.right));
                PredicateBitmap pForwardNew = pForward.copy();

                // remove all predicate from the same group
                AbstractPredicateGroup group = AbstractPredicateGroup.findGroup(p.right, predicateGroups);
                pForwardNew.andNot(group.bits);

                pForward.getBitSet().set(p.right, false);

                findCover(pPathNew, uncoveredEvidenceNew, pForwardNew, result, next);
                if (AIM_DC_NUM > 0 && result.covers.size() >= AIM_DC_NUM) {
                    return;
                }
            }
        }
    }

    private void getCoverage(List<Evidence> uncoveredEvidence, List<IntegerPair> coverage, PredicateBitmap pForward) {
        for (int i = pForward.nextSetBit(0); i >= 0; i = pForward.nextSetBit(i + 1)) {
            long coverageCount = 0;
            for (Evidence e : uncoveredEvidence) {
                if (e.predicates().get(i)) {
                    coverageCount += e.multiplicity();
                }
            }
            coverage.add(new IntegerPair(coverageCount, i));
        }
    }

    public void findCoverInc(PredicateBitmap start, Result result) {
        List<Evidence> uncovered = new ArrayList<>(er);
        uncovered.removeIf(evidence -> evidence.isCoveredBy(start));

        PredicateBitmap pForward = new PredicateBitmap();
        pForward.set(0, predicateNum);
        for (int i = start.nextSetBit(0); i >= 0; i = start.nextSetBit(i + 1)) {
            pForward.andNot(AbstractPredicateGroup.findGroup(i, predicateGroups).bits);
        }

        // false to disable Symmetry
        findCover(start, uncovered, pForward, result, useSymmetry && start.isSelfSymmetry(predicateGroups));
        previous.add(start);
    }

    public Result findCover(PredicateBitmap start) {
        Result covers = new Result();

        List<Evidence> uncovered = new ArrayList<>(er);
        uncovered.removeIf(evidence -> evidence.isCoveredBy(start));

        PredicateBitmap pForward = new PredicateBitmap();
        pForward.set(0, predicateNum);
        for (int i = start.nextSetBit(0); i >= 0; i = start.nextSetBit(i + 1)) {
            pForward.andNot(AbstractPredicateGroup.findGroup(i, predicateGroups).bits);
        }

        // false to disable Symmetry
        findCover(start, uncovered, pForward, covers, useSymmetry && start.isSelfSymmetry(predicateGroups));
        return covers;
    }

    public Result findCover() {
        Result covers = new Result();
        PredicateBitmap q = new PredicateBitmap();
        PredicateBitmap pForward = new PredicateBitmap();
        pForward.set(0, predicateNum);
        // if useSymmetry, init lock with true; else, lock is always false.
        findCover(q, er, pForward, covers, useSymmetry);
        return covers;
    }
}
