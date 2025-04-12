package incREE.evidence;

import incREE.dataset.Relation;

import java.util.*;

public class EvidenceSetBuilder {
    private final Relation relation;
    private final int maxTPId;
    private final List<PredicateBitmap> evidenceSet;

    public EvidenceSetBuilder(Relation relation) {
        this.relation = relation;
        this.maxTPId = relation.getMaxTuplePairId();
        this.evidenceSet = new ArrayList<>(maxTPId);
    }

    private List<Integer> getAllPairs(TreeSet<Integer> set1, TreeSet<Integer> set2) {
        ArrayList<Integer> pairs = new ArrayList<>();
        Integer[] elements1 = set1.toArray(new Integer[0]);
        Integer[] elements2 = set2.toArray(new Integer[0]);
        if (set1.equals(set2)) {
            for (int i = 0; i < elements1.length; i++) {
                for (int j = i + 1; j < elements1.length; j++) {
                    pairs.add(relation.getTuplePairId(elements1[i], elements1[j]));
                    pairs.add(relation.getTuplePairId(elements1[j], elements1[i]));
                }
            }
        } else {
            for (Integer i1 : elements1) {
                for (Integer i2 : elements2) {
                    pairs.add(relation.getTuplePairId(i1, i2));
                }
            }
        }

        // always ignore case of tx and ty where x == y?
        pairs.removeIf(relation::isReflexive);
        return pairs;
    }

//    public static

    /**
     * Use PLI to find inconsistent TuplePairs
     * @param operatorGroup EQUAL or GREATER_THAN
     * @return List of TuplePair ID that inconsistent with given predicate
     */
    private <T extends Comparable<T>> List<Integer> getInconsistentTuplePairs(PredicateGroup predicateGroup, PredicateGroup.OperatorGroup operatorGroup) {
        // TODO: optimize this algorithm by decreasing compare operations
        @SuppressWarnings("unchecked")
        Map<T, TreeSet<Integer>> PLILeft = (Map<T, TreeSet<Integer>>) predicateGroup.columnPair.firstColumn().getPLI(relation.currentSize);
        @SuppressWarnings("unchecked")
        Map<T, TreeSet<Integer>> PLIRight = (Map<T, TreeSet<Integer>>) predicateGroup.columnPair.secondColumn().getPLI(relation.currentSize);
        List<Map.Entry<T, TreeSet<Integer>>> clustersLeft = new ArrayList<>(PLILeft.entrySet());
        List<Map.Entry<T, TreeSet<Integer>>> clustersRight = new ArrayList<>(PLIRight.entrySet());
        int leftSize = clustersLeft.size();
        int rightSize = clustersRight.size();
        int i = 0;
        int j = 0;
        ArrayList<Integer> inconsistentTuplePairs = new ArrayList<>();
        switch (operatorGroup) {
            case EQUAL:
                // Select all
                while (i < leftSize && j < rightSize) {
                    int compare = clustersLeft.get(i).getKey().compareTo(clustersRight.get(j).getKey());
                    if (compare == 0) {
                        inconsistentTuplePairs.addAll(getAllPairs(clustersLeft.get(i).getValue(), clustersRight.get(j).getValue()));
                        i = i + 1;
                        j = j + 1;
                    } else if (compare < 0) {
                        i = i + 1;
                    } else {
                        j = j + 1;
                    }
                }
                break;
            case GREATER_THAN:
                while (i < leftSize) {
                    T leftValue = clustersLeft.get(i).getKey();
                    for (j = 0; j < rightSize; j++) {
                        if (leftValue.compareTo(clustersRight.get(j).getKey()) > 0) {
                            inconsistentTuplePairs.addAll(getAllPairs(clustersLeft.get(i).getValue(), clustersRight.get(j).getValue()));
                        } else {
                            break;
                        }
                    }
                    i = i + 1;
                }
        }
        return inconsistentTuplePairs;
    }

    private PredicateBitmap getEvidenceHead() {
        PredicateBitmap head = new PredicateBitmap();
        for (PredicateGroup predicateGroup : relation.predicateGroups) {
            head.or(predicateGroup.head);
        }
        return head;
    }

    public void buildEvidenceSet() {
        // initialize all by eHead
        PredicateBitmap evidenceHead = getEvidenceHead();
        for (int i = 0; i < maxTPId; i++) {
            evidenceSet.add(evidenceHead.copy());
        }
        // Reconcile
        for (PredicateGroup predicateGroup : relation.predicateGroups) {
            for (PredicateGroup.OperatorGroup og : predicateGroup.getReconcileOperatorGroup()) {
                PredicateBitmap fix = predicateGroup.getFixSet(og);
                List<Integer> tps = getInconsistentTuplePairs(predicateGroup, og);
                for (Integer tpId : tps) {
                    if (tpId >= maxTPId) {
                        System.err.println("Index out of bounds: " + tpId);
                    }
                    evidenceSet.get(tpId).xor(fix);
                }
            }
        }
    }

    public void buildEvidenceSetNaive() {
        for (int i = 0; i < maxTPId; i++) {
            PredicateBitmap evidence = new PredicateBitmap();
            if (!relation.isReflexive(i)) {
                for (Predicate<?> predicate : relation.predicateSpace) {
                    if (relation.satisfies(i, predicate)) {
                        evidence.set(predicate.identifier);
                    }
                }
            }
            evidenceSet.add(evidence);
        }
    }

    public List<PredicateBitmap> getEvidenceSet() {
        return evidenceSet;
    }

    public Map<PredicateBitmap, Integer> collect() {
        Map<PredicateBitmap, Integer> evidenceMap = new HashMap<>();
        for (int i = 0; i < maxTPId; i++) {
            if (!relation.isReflexive(i)) {
                PredicateBitmap evidence = evidenceSet.get(i).copy();
                evidenceMap.merge(evidence, 1, Integer::sum);
            }
        }
        return evidenceMap;
    }
}
