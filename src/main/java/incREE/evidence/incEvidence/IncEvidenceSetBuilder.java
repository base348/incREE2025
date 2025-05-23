package incREE.evidence.incEvidence;

import incREE.dataset.Column;
import incREE.dataset.Relation;
import incREE.evidence.PredicateBitmap;
import incREE.evidence.DataPredicateGroup;

import java.util.*;

public class IncEvidenceSetBuilder {
    private final Relation relation;
    private final List<DataPredicateGroup> predicateGroups;
    private final int currentTupleSize;
    private final int incTupleSize;
    private final Map<PredicateBitmap, Integer> evidenceMap;

    // init with != < ≤
    private final PredicateBitmap head;

//    private long total = 0L;
//    private long diff = 0L;

    private static final TreeSet<Integer> EMPTY_SET = new TreeSet<>();

    private PredicateBitmap getEvidenceHead() {
        PredicateBitmap head = new PredicateBitmap();
        for (DataPredicateGroup predicateGroup : relation.predicateGroups) {
            head.or(predicateGroup.head);
        }
        return head;
    }

    private static int getSize(Map<PredicateBitmap, Integer> evidenceMap) {
        int size = 0;
        for (PredicateBitmap predicateBitmap : evidenceMap.keySet()) {
            size += evidenceMap.get(predicateBitmap);
        }
        return size;
    }

    public IncEvidenceSetBuilder(Relation relation, int incTupleSize) {
        this.relation = relation;
        this.predicateGroups = relation.predicateGroups;
        this.currentTupleSize = relation.currentSize;
        this.evidenceMap = new HashMap<>();
        this.incTupleSize = incTupleSize;
        this.head = getEvidenceHead();
    }

    public Map<PredicateBitmap, Integer> build() {
        // initialize contexts
        BitSet previous = new BitSet(currentTupleSize);
        previous.set(0, currentTupleSize);

        for (int i = currentTupleSize; i < currentTupleSize + incTupleSize; i++) {
            reconcileContexts(new EvidenceContext(i, (BitSet) previous.clone(), head.copy()));
            previous.set(i);
            if (i % 1000 == 0) {
                System.out.println((float)(i-currentTupleSize)/incTupleSize*100 + "% completed.");
            }
        }

        //cal image here
        getImage(evidenceMap).forEach((key, value) -> {
            evidenceMap.merge(key, value, Integer::sum);
        });

//        System.out.println("All evidence: " + this.predicateGroups.get(0).getAllPredicatesNum());
//        System.out.println("Diff: " + diff + "; Avg Diff: " + diff / total);

        return evidenceMap;
    }

    private boolean notLegal(PredicateBitmap predicateBitmap) {
        for (DataPredicateGroup predicateGroup : predicateGroups) {
            if (!predicateGroup.isLegal(predicateBitmap))
                return true;
        }
        return false;
    }

    private PredicateBitmap getImage(PredicateBitmap bitmap) {
        PredicateBitmap copy = new PredicateBitmap();
        for (DataPredicateGroup predicateGroup : relation.predicateGroups) {
            predicateGroup.setImage(bitmap, copy);
        }
        return copy;
    }

    private Map<PredicateBitmap, Integer> getImage(Map<PredicateBitmap, Integer> evidenceMap) {
        Map<PredicateBitmap, Integer> copy = new HashMap<>();
        for (PredicateBitmap predicateBitmap : evidenceMap.keySet()) {
            copy.put(getImage(predicateBitmap), evidenceMap.get(predicateBitmap));
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

    private void collect(List<EvidenceContext> contexts) {
        for (EvidenceContext context : contexts) {
            evidenceMap.merge(context.evidence(), context.tidRight().cardinality(), Integer::sum);
            // 测试
//            PredicateBitmap diff = context.evidence().copy();
//            diff.xor(head);
//            int diffSize = diff.size();
//            this.diff += diffSize;
//            this.total ++;
        }
    }

    private static void toBitSet(Set<Integer> set, BitSet aim) {
        set.forEach(aim::set);
    }

    /**
     * Use PLI to find inconsistent TuplePairs
     *
     * @param operatorGroup EQUAL or GREATER_THAN
     * @return Set of TuplePair ID that inconsistent with given predicate
     */
    private <T extends Comparable<T>> BitSet getInconsistentTuplePairs(DataPredicateGroup predicateGroup, DataPredicateGroup.OperatorGroup operatorGroup, int tidLeft) {
        // TODO: optimize this algorithm by decreasing compare operations
        @SuppressWarnings("unchecked")
        Column<T> columnLeft = (Column<T>) predicateGroup.columnPair.firstColumn();
        @SuppressWarnings("unchecked")
        Column<T> columnRight = (Column<T>) predicateGroup.columnPair.secondColumn();
        T val = columnLeft.get(tidLeft);
        TreeMap<T, TreeSet<Integer>> treeMap = columnRight.getPLI(tidLeft);
        BitSet ans = new BitSet();

        switch (operatorGroup) {
            case EQUAL:
                toBitSet(treeMap.getOrDefault(val, EMPTY_SET), ans);
                break;
            case GREATER_THAN:
                for (TreeSet<Integer> valueSet : treeMap.headMap(val, false).values()) {
                    toBitSet(valueSet, ans);
                }
                break;
        }
        return ans;
    }

    private void reconcileContexts(EvidenceContext context) {
        int leftId = context.tidLeft();
        List<EvidenceContext> reconciled = new ArrayList<>();
        reconciled.add(context);

        // all groups
        for (DataPredicateGroup predicateGroup : predicateGroups) {
            for (DataPredicateGroup.OperatorGroup og : predicateGroup.getReconcileOperatorGroup()) {
                PredicateBitmap fix = predicateGroup.getFixSet(og);
                BitSet inconsistent = getInconsistentTuplePairs(predicateGroup, og, leftId);

                reconcilePredicateGroup(reconciled, fix, inconsistent);
            }
        }

        collect(reconciled);
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
