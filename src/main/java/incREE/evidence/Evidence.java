package incREE.evidence;

import java.util.List;

public record Evidence(PredicateBitmap predicates, int multiplicity) {
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
}
