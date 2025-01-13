package incREE.evidence;

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
}
