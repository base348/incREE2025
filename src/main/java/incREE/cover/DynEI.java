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

    public DynEI(List<Evidence> erAll, List<Evidence> erOld, List<Cover> coverCurrent, List<AbstractPredicateGroup> predicateGroups, int threshold, int dcLength) {
        this.erAll = erAll;
        Map<PredicateBitmap, Integer> tmp = Evidence.toMap(erAll);
        subtract(tmp, Evidence.toMap(erOld));
        this.erInc = Evidence.fromMap(tmp);
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

    private static PredicateBitmap fromString(String bitStr){
        PredicateBitmap ans = new PredicateBitmap();
        for (int i = 0; i < bitStr.length(); i++) {
            if (bitStr.charAt(i) == '1') {
                ans.set(i, true);
            }
        }
        return ans;
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

    public List<Cover> run() {
//        StaticCoverFinder.Result covers = new StaticCoverFinder.Result();
        StaticCoverFinder finder = new StaticCoverFinder(threshold, erAll, predicateGroups, dcLength, false);
        List<Cover> violated = new ArrayList<>();

        int numCurrent = coverCurrent.size();

        for (Cover c : coverCurrent) {
            int numUncovered = Evidence.numUncover(c.containing, erInc);
            if (numUncovered + c.uncovered <= threshold) {
                // not violated
                c.uncovered += numUncovered;
            } else {
                violated.add(c);
            }
        }

//        System.out.println("violated: " + violated.size());

        coverCurrent.removeAll(violated);

        int numViolated = violated.size();
        int numKeep =  numCurrent - numViolated;

//        findAncestor(fromString("00000001100001011"), violated);
        violated.sort(Comparator.comparingInt(cover -> cover.containing.size()));

        for (Cover v: violated) {
            if (v.containing.size() >= dcLength) {
                break;
            }
            finder.findCoverInc(v.containing, coverCurrent);
        }

        int numFinal = coverCurrent.size();

        System.out.println("Cover Find finished: " + numCurrent + " to " + numFinal + ", "
                + (numFinal - numKeep) + "+; " + numViolated + "-;" );

//        for (Evidence e : erInc) {
//            List<Cover> violated = new ArrayList<>();
//            for (Cover dc : coverCurrent) {
//                if (dc.containing.isSubsetOf(e.predicates())) {
//                    violated.add(dc);
//                }
//            }
//            coverCurrent.removeAll(violated);
//
//            int count = 0;
//            for (Cover dc : violated) {
//                count = 0;
//                for (Predicate<?> p : predicateSpace) {
//                    if (!e.get(p.identifier)){
//                        PredicateBitmap expandDC = dc.copy();
//                        expandDC.set(p.identifier);
//                        if (!isImplied(expandDC, coverCurrent)) {
//                            coverCurrent.add(expandDC);
//                            count++;
//                        }
//                    }
//                }
//                System.out.println(count + " new DC found.");
//            }
//        }
        return coverCurrent;
    }
}
