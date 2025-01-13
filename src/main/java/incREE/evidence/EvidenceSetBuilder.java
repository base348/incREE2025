package incREE.evidence;

import incREE.dataset.Relation;

import java.util.*;
import java.util.stream.Collectors;

public class EvidenceSetBuilder {
    private final Relation relation;
    private final int maxTPId;
    private final List<List<Predicate<?>>> evidenceSet;

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

        // TODO: always ignore case of tx and ty where x == y?
        pairs.removeIf(relation::isReflexive);
        return pairs;
    }

    private <T extends Comparable<T>> List<Integer> getInconsistentTuplePairs(Predicate<T> predicates) {
        // TODO: optimize this algorithm by decreasing compare operations
        Map<T, TreeSet<Integer>> PLTLeft = predicates.attribute1.getPLI();
        Map<T, TreeSet<Integer>> PLTRight = predicates.attribute2.getPLI();
        List<Map.Entry<T, TreeSet<Integer>>> clustersLeft = new ArrayList<>(PLTLeft.entrySet());
        List<Map.Entry<T, TreeSet<Integer>>> clustersRight = new ArrayList<>(PLTRight.entrySet());
        int leftSize = clustersLeft.size();
        int rightSize = clustersRight.size();
        int i = 0;
        int j = 0;
        ArrayList<Integer> inconsistentTuplePairs = new ArrayList<>();
        switch (predicates.operator) {
            case EQUAL:
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

    private List<Predicate<?>> getEvidenceHead() {
        return relation.predicateSpace.stream().filter(e ->
                (e.operator.equals(Operator.NOT_EQUAL) || e.operator.equals(Operator.LESS_THAN) || e.operator.equals(Operator.LESS_THAN_OR_EQUAL))
        ).collect(Collectors.toList());
    }

    private void symmetricDifference(List<Predicate<?>> e1, Set<Predicate<?>> e2) {
        // e1 <- e1 circlePlus e2
        List<Predicate<?>> result = new ArrayList<>();

        for (Predicate<?> item : e1) {
            if (!e2.contains(item)) {
                result.add(item);
            }
        }

        for (Predicate<?> item : e2) {
            if (!e1.contains(item)) {
                result.add(item);
            }
        }

        e1.clear();
        e1.addAll(result);
    }

    public void buildEvidenceSet() {
        // initialize all by eHead
        List<Predicate<?>> evidenceHead = getEvidenceHead();
        for (int i = 0; i < maxTPId; i++) {
            List<Predicate<?>> evidence = new ArrayList<>(evidenceHead);
            evidenceSet.add(evidence);
        }
        // reconstruction
        relation.predicateSpace.stream().filter(e ->
                (e.operator.equals(Operator.EQUAL) || e.operator.equals(Operator.GREATER_THAN))
        ).forEach(p -> {
            Set<Predicate<?>> fix = p.getFixSet(relation);
            List<Integer> tps = getInconsistentTuplePairs(p);
            tps.forEach(tpId -> {
                if (tpId >= maxTPId) {
                    System.err.println("Index out of bounds: " + tpId);
                }
                symmetricDifference(evidenceSet.get(tpId), fix);
            });
        });
    }

    public void buildEvidenceSetNaive() {
        for (int i = 0; i < maxTPId; i++) {
            List<Predicate<?>> evidence = new ArrayList<>();
            if (!relation.isReflexive(i)) {
                for (Predicate<?> predicate : relation.predicateSpace) {
                    if (relation.satisfies(i, predicate)) {
                        evidence.add(predicate);
                    }
                }
            }
            evidenceSet.add(evidence);
        }
    }

    public List<List<Predicate<?>>> getEvidenceSet() {
        return evidenceSet;
    }

    public List<Evidence> getDegenerateEvidenceSet() {
        Map<Set<Predicate<?>>, Integer> evidenceMap = new HashMap<>();
        List<Evidence> degenerateEvidenceSet = new ArrayList<>();
        for (int i = 0; i < maxTPId; i++) {
            if (!relation.isReflexive(i)) {
                Set<Predicate<?>> evidence = Set.copyOf(evidenceSet.get(i));
                evidenceMap.merge(evidence, 1, Integer::sum);
            }
        }
        evidenceMap.forEach((k, v) -> degenerateEvidenceSet.add(new Evidence(k, v)));
        return degenerateEvidenceSet;
    }


    public void test() {
        System.out.println(relation.predicateSpace);
        System.out.println("Total number of TP: " + relation.getTotalTuplePairs());
        System.out.println("Max id of TP: " + relation.getMaxTuplePairId());
        Predicate<?> aim = relation.predicateSpace.stream().filter(predicate -> predicate.operator.equals(Operator.GREATER_THAN)).findFirst().get();
        int index = relation.predicateSpace.indexOf(aim);
        System.out.println(aim);
        System.out.println("index-1: " + relation.predicateSpace.get(index-1));
        System.out.println("index-2: " + relation.predicateSpace.get(index-2));
        List<Integer> inconsistentTuplePairs = getInconsistentTuplePairs(aim);
        // test if all tp in inconsistentTuplePairs satisfy relation.predicateSpace.get(0)
        // test if all tp not in inconsistentTuplePairs not satisfy relation.predicateSpace.get(0)
        List<Integer> inconsistentTuplePairs2 = new ArrayList<>();
        relation.foreachTuplePair(
                tp -> {
                    if (tp.satisfies(aim)) {
                        inconsistentTuplePairs2.add(tp.getTpId());
                    }
                }
        );

        Collections.sort(inconsistentTuplePairs);
        // compare two lists
//        System.out.println("Length: " + inconsistentTuplePairs.size() + "; " + inconsistentTuplePairs2.size());
//        System.out.println("Algorithm:");
//        for (Integer i : inconsistentTuplePairs) {
//            System.out.println(relation.getTuplePair(i).toString());
//        }
//        System.out.println();
//        System.out.println("Verify:");
//        for (Integer i : inconsistentTuplePairs2) {
//            System.out.println(relation.getTuplePair(i).toString());
//        }
        Checker.checkLists(inconsistentTuplePairs, inconsistentTuplePairs2);
    }
}
