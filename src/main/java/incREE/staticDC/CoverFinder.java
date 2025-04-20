package incREE.staticDC;

import incREE.evidence.*;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

public class CoverFinder {

    private static final int AIM_DC_NUM = -1;
    private static final int MAX_DC_LENGTH = 6;

    private final double errorThreshold;
    private final List<Evidence> er;
    private final List<AbstractPredicateGroup> predicateGroups;
    private final int predicateNum;

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

    private boolean isImplied(PredicateBitmap q, List<PredicateBitmap> cover) {
        for (PredicateBitmap c : cover) {
            if (c.isSubsetOf(q)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMinimal(PredicateBitmap q) {
        // no subset of size |q|-1 cover this.er
        for (int i = q.nextSetBit(0); i >= 0; i = q.nextSetBit(i + 1)) {
            PredicateBitmap pSub = q.copy();
            pSub.getBitSet().set(i, false);
            if (Evidence.satisfies(pSub, er, (int) errorThreshold)) {
                return false;
            }
        }
        return true;
    }

    private void findCover(PredicateBitmap pPath, List<Evidence> uncoveredEvidence, PredicateBitmap pForward, List<PredicateBitmap> cover) {
//        System.out.println(pPath);
        if (Evidence.size(uncoveredEvidence) <= errorThreshold) {
            if (isMinimal(pPath)) {
                cover.add(pPath);
            }
            return;
        } else if (pForward.isEmpty() || pPath.size() >= MAX_DC_LENGTH) {
            return;
        } else {
            // sort pForward
            List<IntegerPair> coverage = new ArrayList<>();
            // for all true bits from pForward
            for (int i = pForward.nextSetBit(0); i >= 0; i = pForward.nextSetBit(i + 1)) {
                int coverageCount = 0;
                for (Evidence e : uncoveredEvidence) {
                    if (e.predicates().get(i)) {
                        coverageCount += 1;
                    }
                }
                coverage.add(new IntegerPair(coverageCount, i));
            }
            coverage.sort(Comparator.reverseOrder());

            for (IntegerPair p : coverage) {
                PredicateBitmap pPathNew = pPath.copy();
                pPathNew.set(p.right);
                if (isImplied(pPathNew, cover)) {
                    // pPathNew.remove(p); // Useless
                    continue;
                }
                List<Evidence> uncoveredEvidenceNew = new ArrayList<>(uncoveredEvidence);
                uncoveredEvidenceNew.removeIf(evidence -> evidence.predicates().get(p.right));
                PredicateBitmap pForwardNew = pForward.copy();

                // remove all predicate from the same group
                AbstractPredicateGroup group = AbstractPredicateGroup.findGroup(p.right, predicateGroups);
                pForwardNew.andNot(group.bits);

                pForward.getBitSet().set(p.right, false);

                findCover(pPathNew, uncoveredEvidenceNew, pForwardNew, cover);
                if (AIM_DC_NUM > 0 && cover.size() >= AIM_DC_NUM) {
                    return;
                }
            }
        }
    }

    public List<PredicateBitmap> findCover() {
        try {
            System.setOut(new PrintStream(new FileOutputStream("log.txt")));
            List<PredicateBitmap> cover = new ArrayList<>();
            PredicateBitmap q = new PredicateBitmap();
            PredicateBitmap pForward = new PredicateBitmap();
            pForward.set(0, predicateNum);
            findCover(q, er, pForward, cover);
            return cover;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
