package incREE.evidence.incEvidence;

import incREE.dataset.Column;
import incREE.dataset.Relation;
import incREE.evidence.EvidenceSetBuilder;
import incREE.evidence.Predicate;
import incREE.evidence.PredicateBitmap;
import incREE.evidence.PredicateGroup;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class IncEvidenceSetBuilder {
    private final Relation relation;
    private final List<PredicateGroup> predicateGroups;
//    private final int totalTupleSize;
    private final int currentTupleSize;
    private final int incTupleSize;
//    private final List<List<EvidenceContext>> contexts = new ArrayList<>();
    private final Map<PredicateBitmap, Integer> evidenceMap;

    private static final TreeSet<Integer> EMPTY_SET = new TreeSet<>();

    private PredicateBitmap getEvidenceHead() {
        PredicateBitmap head = new PredicateBitmap();
        for (PredicateGroup predicateGroup : relation.predicateGroups) {
            head.or(predicateGroup.head);
        }
        return head;
    }

    public IncEvidenceSetBuilder(Relation relation, int incTupleSize) {
        this.relation = relation;
        this.predicateGroups = relation.predicateGroups;
        this.currentTupleSize = relation.currentSize;
        this.evidenceMap = new HashMap<>();
        this.incTupleSize = incTupleSize;
    }

    public Map<PredicateBitmap, Integer> build() {
        // initialize contexts
        BitSet previous = new BitSet(currentTupleSize);
        previous.set(0, currentTupleSize);

        PredicateBitmap head = getEvidenceHead();
        for (int i = currentTupleSize; i < currentTupleSize + incTupleSize; i++) {
            reconcileContexts(new EvidenceContext(i, (BitSet) previous.clone(), head.copy()));
            previous.set(i);
        }

//        List<Evidence> degenerateEvidenceSet = new ArrayList<>();
//        evidenceMap.forEach((k, v) -> degenerateEvidenceSet.add(new Evidence(k, v)));
        return evidenceMap;
    }

    private EvidenceContext symmetryCopy(EvidenceContext context) {
        PredicateBitmap copy = new PredicateBitmap();
        for (PredicateGroup predicateGroup : relation.predicateGroups) {
            predicateGroup.setSymmetry(context.evidence(), copy);
        }
        return new EvidenceContext(context.tidLeft(), (BitSet) context.tidRight().clone(), copy);
    }

    private List<EvidenceContext> symmetryDeepCopyContexts(List<EvidenceContext> contexts) {
        List<EvidenceContext> copy = new ArrayList<>(contexts.size());
        for (EvidenceContext context : contexts) {
            copy.add(symmetryCopy(context));
        }
        return copy;
    }

    private int evidenceMapSize() {
        int size = 0;
        for (Integer count : evidenceMap.values()) {
            size += count;
        }
        return size;
    }

//    public void checkEvidence(List<EvidenceContext> contexts, boolean symmetry) {
//        for (EvidenceContext context : contexts) {
//            for (Integer tid : context.tidRight() ) {
//                if (!symmetry) {
//                    if (!relation.satisfies(context.tidLeft(), tid, context.evidence())) {
//                        // build correct evidence by naive method
//                        PredicateBitmap evidence = new PredicateBitmap();
//                        for (Predicate<?> predicate : relation.predicateSpace) {
//                            if (relation.satisfies(context.tidLeft(), tid, predicate)) {
//                                evidence.set(predicate.identifier);
//                            }
//                        }
//                        throw new RuntimeException("Evidence context is not correct.");
//                    }
//                } else {
//                    if (!relation.satisfies(tid, context.tidLeft(), context.evidence())) {
//                        PredicateBitmap evidence = new PredicateBitmap();
//                        for (Predicate<?> predicate : relation.predicateSpace) {
//                            if (relation.satisfies(tid, context.tidLeft(), predicate)) {
//                                evidence.set(predicate.identifier);
//                            }
//                        }
//                        throw new RuntimeException("Evidence context is not correct.");
//                    }
//                }
//            }
//        }
//    }

    private void collect(List<EvidenceContext> contexts) {
//        int mergeSize = 0;
//        for (EvidenceContext context : contexts) {
//            mergeSize += context.tidRight().size();
//        }
//        int evidenceSize = evidenceMapSize();
        for (EvidenceContext context : contexts) {
            evidenceMap.merge(context.evidence(), context.tidRight().cardinality(), Integer::sum);
        }
//        int evidenceSize2 = evidenceMapSize();
//        System.out.println("IncEvidenceSetBuilder.collect: " + mergeSize + "new evidence got, evidenceMap expanded from " + evidenceSize + " to " + evidenceSize2);
    }

    private static void setBitSet(BitSet aim, Set<Integer> set) {
        set.forEach(aim::set);
    }

    /**
     * Use PLI to find inconsistent TuplePairs
     * @param operatorGroup EQUAL or GREATER_THAN
     * @return Set of TuplePair ID that inconsistent with given predicate
     */
    private <T extends Comparable<T>> BitSet getInconsistentTuplePairs(PredicateGroup predicateGroup, PredicateGroup.OperatorGroup operatorGroup, int tidLeft, boolean isSymmetry) {
        // TODO: optimize this algorithm by decreasing compare operations
        @SuppressWarnings("unchecked")
        Column<T> columnLeft = (Column<T>) predicateGroup.columnPair.firstColumn();
        @SuppressWarnings("unchecked")
        Column<T> columnRight = (Column<T>) predicateGroup.columnPair.secondColumn();
        T val;
        TreeMap<T, TreeSet<Integer>> treeMap;
        BitSet ans = new BitSet();

        if (isSymmetry) {
            val = columnRight.get(tidLeft);
            treeMap = columnLeft.getPLI(tidLeft);
        } else {
            val = columnLeft.get(tidLeft);
            treeMap = columnRight.getPLI(tidLeft);
        }

        switch (operatorGroup) {
            case EQUAL:
                setBitSet(ans, treeMap.getOrDefault(val, EMPTY_SET));
            case GREATER_THAN:
                if (isSymmetry) {
                    for (TreeSet<Integer> valueSet : treeMap.tailMap(val, false).values()) {
                        setBitSet(ans, valueSet);
                    }
                } else {
                    for (TreeSet<Integer> valueSet : treeMap.headMap(val, false).values()) {
                        setBitSet(ans, valueSet);
                    }
                }
        }
        return ans;
    }

    private <T extends Comparable<T>> void reconcileContexts(EvidenceContext context) {
        // TODO: Use PLI of inc part to avoid repeat calculation?
        int leftId = context.tidLeft();
        List<EvidenceContext> reconciled = new ArrayList<>();
        List<EvidenceContext> reconciledCopy;
        reconciled.add(context);
        predicateGroups.stream().filter(PredicateGroup::isReflexive).forEach(predicateGroup -> {
            for (PredicateGroup.OperatorGroup og : predicateGroup.getReconcileOperatorGroup()) {
                PredicateBitmap fix = predicateGroup.getFixSet(og);
                BitSet inconsistent = getInconsistentTuplePairs(predicateGroup, og, leftId, false);

                reconcilePredicateGroup(reconciled, fix, inconsistent);
            }
        });

        // copy the reconciled list
        reconciledCopy = symmetryDeepCopyContexts(reconciled);

        if (predicateGroups.stream().filter(predicateGroup -> !predicateGroup.isReflexive()).findAny().isEmpty()) {
//            checkEvidence(reconciled, false);
            collect(reconciled);
//            checkEvidence(reconciledCopy, true);
            collect(reconciledCopy);
            return;
        }

        predicateGroups.stream().filter(predicateGroup -> !predicateGroup.isReflexive()).forEach(predicateGroup -> {
            for (PredicateGroup.OperatorGroup og : predicateGroup.getReconcileOperatorGroup()) {
                PredicateBitmap fix = predicateGroup.getFixSet(og);
                BitSet inconsistent = getInconsistentTuplePairs(predicateGroup, og, leftId, false);

                reconcilePredicateGroup(reconciled, fix, inconsistent);
            }
        });
//        checkEvidence(reconciled, false);
        collect(reconciled);

        predicateGroups.stream().filter(predicateGroup -> !predicateGroup.isReflexive()).forEach(predicateGroup -> {
            for (PredicateGroup.OperatorGroup og : predicateGroup.getReconcileOperatorGroup()) {
                PredicateBitmap fix = predicateGroup.getFixSet(og);
                BitSet inconsistent = getInconsistentTuplePairs(predicateGroup, og, leftId, true);

                reconcilePredicateGroup(reconciledCopy, fix, inconsistent);
            }
        });
//        checkEvidence(reconciledCopy, true);
        collect(reconciledCopy);
    }

    private void reconcilePredicateGroup(List<EvidenceContext> reconciled, PredicateBitmap fix, BitSet inconsistent) {
        List<EvidenceContext> splitList = new ArrayList<>();
        Iterator<EvidenceContext> iterator = reconciled.iterator();
        while (iterator.hasNext()) {
            EvidenceContext next = iterator.next();
            EvidenceContext split = next.split(inconsistent, fix);
            if (!split.isEmpty()) {
                splitList.add(split);
            }
            if (next.isEmpty()) {
                iterator.remove();
            }
        }

        reconciled.addAll(splitList);
    }
}
