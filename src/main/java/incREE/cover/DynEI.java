package incREE.cover;

import incREE.evidence.AbstractPredicateGroup;
import incREE.evidence.Evidence;
import incREE.evidence.Predicate;
import incREE.evidence.PredicateBitmap;

import java.util.*;

public class DynEI {
    private final List<Evidence> erAll;
    private final List<Evidence> erInc;
    private final List<Cover> coverCurrent;
    private final List<AbstractPredicateGroup> predicateGroups;
    private final int threshold;
    private final int dcLength;

    public DynEI(List<Evidence> erAll, List<Evidence> erOld, List<Evidence> erUnc, List<Cover> coverCurrent, List<AbstractPredicateGroup> predicateGroups, int threshold, int dcLength) {
//        this.erAll = erAll;
        Map<PredicateBitmap, Integer> inc = Evidence.toMap(erAll);
        Map<PredicateBitmap, Integer> all = new HashMap<>(inc);
        Map<PredicateBitmap, Integer> old = Evidence.toMap(erOld);
        subtract(inc, old);
        subtract(old, Evidence.toMap(erUnc));
        remove(inc, old);
        remove(all, old);
        this.erAll = Evidence.fromMap(all);
        this.erInc = Evidence.fromMap(inc);
        this.coverCurrent = coverCurrent;
        this.predicateGroups = predicateGroups;
        this.threshold = threshold;
        this.dcLength = dcLength;
    }

    private static boolean isImplied(PredicateBitmap dc, List<Cover> cover) {
        for (Cover c : cover) {
            if (c.containing.isSubsetOf(dc)) {
                return true;
            }
        }
        return false;
    }

    private static void subtract(Map<PredicateBitmap, Integer> map1, Map<PredicateBitmap, Integer> map2) {
        map2.forEach((key, value) -> {
            map1.merge(key, -value, Integer::sum);
            map1.remove(key, 0);
        });
    }

    private static void remove(Map<PredicateBitmap, Integer> map1, Map<PredicateBitmap, Integer> map2) {
        map2.forEach((key, value) -> {
            map1.remove(key);
        });
    }

    private static PredicateBitmap fromString(String bitStr){
        PredicateBitmap ans = new PredicateBitmap();
        for (int i = 0; i < bitStr.length(); i++) {
            if (bitStr.charAt(i) == '1') {
                ans.set(i, true);
            }
        }
        return ans;
    }

    public static boolean notAllCovered(Evidence evidence, List<Cover> cover) {
        for (Cover c : cover) {
            if (!evidence.isCoveredBy(c.containing)) {
                return true;
            }
        }
        return false;
    }

    // for debug
    private boolean findAncestor(PredicateBitmap aim, List<Cover> ancestors) {
        List<Cover> candidate = new ArrayList<>();
        for (Cover c : ancestors) {
            if (c.containing.isSubsetOf(aim)) {
                candidate.add(c);
            }
        }
        return candidate.isEmpty();
    }

    public StaticCoverFinder.Result run() {
        StaticCoverFinder.Result result = new StaticCoverFinder.Result();
        StaticCoverFinder finder = new StaticCoverFinder(threshold, erAll, predicateGroups, dcLength, false);
        List<Cover> violated = new ArrayList<>();

        int numCurrent = coverCurrent.size();

        for (Cover c : coverCurrent) {
            int numUncovered = Evidence.numUncover(c.containing, erInc);
            if (numUncovered != 0) {
                if (numUncovered + c.uncovered <= threshold) {
                    // not violated
                    c.uncovered += numUncovered;
                } else {
                    violated.add(c);
                }
            }
        }
        coverCurrent.removeAll(violated);

        // uncovered by not invalid
        List<Evidence> uncovered = new ArrayList<>();
        for (Evidence e : erAll) {
            if (notAllCovered(e, coverCurrent)) {
                uncovered.add(e);
            }
        }

        int numViolated = violated.size();
        int numKeep =  numCurrent - numViolated;

        violated.sort(Comparator.comparingInt(cover -> cover.containing.size()));

        result.covers = coverCurrent;
        result.uncovered = Evidence.toMap(uncovered);

        for (Cover v: violated) {
            if (v.containing.size() >= dcLength) {
                break;
            }
            finder.findCoverInc(v.containing, result);
        }

        int numFinal = coverCurrent.size();

        System.out.println("Cover Find finished: " + numCurrent + " to " + numFinal + ", "
                + (numFinal - numKeep) + "+; " + numViolated + "-;" );

//        List<Evidence> uncovered = new ArrayList<>();
//        for (Evidence e : erAll) {
//            if (notAllCovered(e, coverCurrent)) {
//                uncovered.add(e);
//            }
//        }
//        result.uncovered = Evidence.toMap(uncovered);
        return result;
    }
}
