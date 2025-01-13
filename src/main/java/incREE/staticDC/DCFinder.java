package incREE.staticDC;

import incREE.dataset.Relation;
import incREE.evidence.Evidence;
import incREE.evidence.EvidenceSetBuilder;
import incREE.evidence.Predicate;

import java.util.*;

public class DCFinder {

    private static final int MAX_DC_NUM = 200;
    private static final int MAX_DC_LENGTH = 5;

    private final double errorThreshold;
    private final Relation relation;
    private final List<Evidence> er;

    public DCFinder(double errorThreshold, Relation relation) {
        this.errorThreshold = errorThreshold;
        this.relation = relation;
        EvidenceSetBuilder builder = new EvidenceSetBuilder(relation);
        builder.buildEvidenceSet();
        er = builder.getDegenerateEvidenceSet();
    }

    private <T> boolean isSubset(List<T> subset, List<T> set) {
        for (T t : subset) {
            // TODO: change into set to improve efficiency?
            if (!set.contains(t)) {
                return false;
            }
        }
        return true;
    }

    private boolean isImplied(List<Predicate<?>> q, List<List<Predicate<?>>> cover) {
        for (List<Predicate<?>> c : cover) {
            if (isSubset(q, c)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCover(List<Predicate<?>> q) {
        int threshold = (int) (errorThreshold * relation.getTotalTuplePairs());
        for (Evidence e : er) {
            if (Collections.disjoint(e.predicates(), q)) {
                threshold -= e.multiplicity();
                if (threshold < 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isMinimal(List<Predicate<?>> q) {
        // no subset of size |q|-1 cover this.er
        for (Predicate<?> p : q) {
            List<Predicate<?>> sub = new ArrayList<>(q);
            sub.remove(p);
            if (isCover(sub)) {
                return false;
            }
        }
        return true;
    }

    private void findCover(List<Predicate<?>> q, List<Evidence> ePath, List<Predicate<?>> pPath, List<List<Predicate<?>>> cover) {
        if (Evidence.size(ePath) <= errorThreshold * relation.getTotalTuplePairs()) {
            if (isMinimal(q)) {
                cover.add(q);
                return;
            }
        } else if (pPath.isEmpty() || q.size() >= MAX_DC_LENGTH) {
            return;
        } else {
            // sort pPath
            Map<Predicate<?>, Integer> coverage = new HashMap<>();
            for (Predicate<?> p : pPath) {
                int coverageCount = 0;
                for (Evidence e : ePath) {
                    if (e.contains(p)) {
                        coverageCount += e.multiplicity();
                    }
                }
                coverage.put(p, coverageCount);
            }
            pPath.sort((p1, p2) -> Integer.compare(coverage.get(p2), coverage.get(p1)));

            for (Predicate<?> p : pPath) {
                List<Predicate<?>> qNew = new ArrayList<>(q);
                qNew.add(p);
                if (isImplied(qNew, cover)) {
                    qNew.remove(p);
                    continue;
                }
                List<Evidence> eNew = new ArrayList<>(ePath);
                eNew.removeIf(evidence -> evidence.contains(p));
                List<Predicate<?>> pNew = new ArrayList<>(pPath);
                pNew.removeIf(p::isDependent);
                findCover(qNew, eNew, pNew, cover);
                if (cover.size() >= MAX_DC_NUM) {
                    return;
                }
            }
        }
    }

    public List<List<Predicate<?>>> findCover() {
        List<List<Predicate<?>>> cover = new ArrayList<>();
        List<Predicate<?>> q = new ArrayList<>();
        findCover(q, er, relation.predicateSpace, cover);
        return cover;
    }
}
