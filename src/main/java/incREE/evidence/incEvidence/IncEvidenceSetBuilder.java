package incREE.evidence.incEvidence;

import incREE.dataset.ColumnPair;
import incREE.dataset.Relation;
import incREE.evidence.PredicateBitmap;
import incREE.evidence.PredicateGroup;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class IncEvidenceSetBuilder {
    private final Relation relation;
    private final List<PredicateGroup> predicateGroups;
    private final int totalTupleSize;
    private final int currentTupleSize;
    private final int incTupleSize;
    private final List<List<EvidenceContext>> contexts = new ArrayList<>();

    private PredicateBitmap getEvidenceHead() {
        BitSet bitSet = new BitSet(relation.predicateSpace.size());
        for (PredicateGroup predicateGroup : relation.predicateGroups) {
            predicateGroup.setHead(bitSet);
        }
        return new PredicateBitmap(bitSet);
    }

    public IncEvidenceSetBuilder(Relation relation, int incTupleSize) {
        this.relation = relation;
        this.predicateGroups = relation.predicateGroups;
        this.totalTupleSize = relation.totalSize;
        this.currentTupleSize = relation.currentSize;
        this.incTupleSize = incTupleSize;

        // initialize contexts
        Set<Integer> previous = IntStream.rangeClosed(0, currentTupleSize-1)
                .boxed()
                .collect(Collectors.toSet());

        PredicateBitmap head = getEvidenceHead();

        for (int i = currentTupleSize; i < currentTupleSize + incTupleSize; i++) {
            contexts.add(reconcileContexts(new EvidenceContext(i, new TreeSet<>(previous), head.copy())));
            previous.add(i);
        }
    }

    public List<EvidenceContext> reconcileContexts(EvidenceContext context) {
        List<EvidenceContext> reconciled = new ArrayList<>();
        reconciled.add(context);

        for (PredicateGroup predicateGroup : predicateGroups) {}

        return reconciled;
    }
}
