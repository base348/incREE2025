package incREE.evidence;

import incREE.dataset.Relation;

import java.util.*;

public class EvidenceSetBuilder {
    private final Relation relation;

    public EvidenceSetBuilder(Relation relation) {
        this.relation = relation;
    }
    private List<Integer> getAllPairs(TreeSet<Integer> set1, TreeSet<Integer> set2) {
        ArrayList<Integer> pairs = new ArrayList<>();
        Integer[] elements1 = set1.toArray(new Integer[0]);
        Integer[] elements2 = set2.toArray(new Integer[0]);
        if (set1.equals(set2)) {
            for (int i = 0; i < elements1.length; i++) {
                for (int j = i + 1; j < elements1.length; j++) {
                    pairs.add(relation.getTuplePairId(elements1[i], elements1[j]));
                    pairs.add(relation.getTuplePairId(elements1[j], elements2[i]));
                }
            }
        } else {
            for (Integer i1 : elements1) {
                for (Integer i2 : elements2) {
                    pairs.add(relation.getTuplePairId(i1, i2));
                }
            }
        }
        return pairs;
    }

    private <T extends Comparable<T>> List<Integer> getInconsistentTuplePairs(Predicate<T> predicates) {
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
        }
        return inconsistentTuplePairs;
    }

    public void test() {
        System.out.println(relation.predicateSpace);
        List<Integer> inconsistentTuplePairs = getInconsistentTuplePairs(relation.predicateSpace.get(0));
        // test if all tp in inconsistentTuplePairs satisfy relation.predicateSpace.get(0)
        // test if all tp not in inconsistentTuplePairs not satisfy relation.predicateSpace.get(0)
        List<Integer> inconsistentTuplePairs2 = new ArrayList<>();
        relation.foreachTuplePair(
                tp -> {
                    if (tp.satisfies(relation.predicateSpace.get(0))) {
                        inconsistentTuplePairs2.add(tp.getTpId());
                    }
                }
        );
        // compare two lists
        System.out.println("Length: " + inconsistentTuplePairs.size() + "; " + inconsistentTuplePairs2.size());
        Checker.checkLists(inconsistentTuplePairs, inconsistentTuplePairs2);
    }
}
