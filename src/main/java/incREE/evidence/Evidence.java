package incREE.evidence;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public record Evidence(Set<Predicate<?>> predicates, int multiplicity) {
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

    public boolean satisfied(List<Predicate<?>> dc) {
        return !Collections.disjoint(dc, this.predicates);
    }

    public static boolean satisfies(List<Predicate<?>> dc, List<Evidence> er, int errorThreshold) {
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
