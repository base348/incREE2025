package incREE.evidence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record Evidence(PredicateBitmap predicates, int multiplicity) implements Comparable<Evidence> {

    public static List<Evidence> fromMap(Map<PredicateBitmap, Integer> evidenceMap) {
        List<Evidence> evidenceList = new ArrayList<>();
        evidenceMap.forEach((k, v) -> evidenceList.add(new Evidence(k, v)));
        return evidenceList;
    }

    public static Map<PredicateBitmap, Integer> toMap(List<Evidence> evidenceList) {
        Map<PredicateBitmap, Integer> map = new HashMap<>();
        for (Evidence evidence : evidenceList) {
            map.put(evidence.predicates(), evidence.multiplicity());
        }
        return map;
    }

    public boolean contains(Predicate<?> p) {
        return predicates.contains(p);
    }

    public static int size(List<Evidence> evidences) {
        int size = 0;
        for (Evidence evidence : evidences) {
            size += evidence.multiplicity;
        }
        return size;
    }

    public boolean satisfied(PredicateBitmap dc) {
        return predicates.disjoint(dc);
    }

    public static boolean satisfies(PredicateBitmap dc, List<Evidence> er, int errorThreshold) {
        for (Evidence e : er) {
            if (!e.satisfied(dc)) {
                errorThreshold -= e.multiplicity();
                if (errorThreshold < 0) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int compareTo(Evidence o) {
        if (multiplicity == o.multiplicity) {
            return predicates.getBitSet().compareTo(o.predicates.getBitSet());
        } else {
            return Integer.compare(multiplicity, o.multiplicity);
        }
    }
}
