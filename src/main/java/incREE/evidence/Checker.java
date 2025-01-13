package incREE.evidence;

import incREE.dataset.Relation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Checker {

    private static <T> boolean hasDuplicates(List<T> list) {
        Set<T> set = new HashSet<>(list);
        return set.size() != list.size();
    }

    public static void checkLists(List<Integer> list1, List<Integer> list2) {
        // 检查 list1 是否有重复元素
        if (hasDuplicates(list1)) {
            throw new IllegalArgumentException("List1 contains duplicate elements.");
        }

        // 检查 list2 是否有重复元素
        if (hasDuplicates(list2)) {
            throw new IllegalArgumentException("List2 contains duplicate elements.");
        }

        // 检查两个 List 是否包含相同的元素
        if (!new HashSet<>(list1).equals(new HashSet<>(list2))) {
            throw new IllegalArgumentException("Lists do not contain the same elements.");
        }
    }

    public static void checkDCNaive(Relation relation, List<Predicate<?>> dc) {
        int inconsistent = 0;
        for (int tpId = 0; tpId < relation.getMaxTuplePairId(); tpId++ ) {
            if (!relation.isReflexive(tpId)) {
                boolean consistent = false;
                for (Predicate<?> predicate : dc) {
                    if (relation.satisfies(tpId, predicate)) {
                        consistent = true;
                        break;
                    }
                }
                if (!consistent) {
                    inconsistent++;
                }
            }
        }
        System.out.println("Inconsistent percent of " + dc + ": " + inconsistent / relation.getTotalTuplePairs());
    }
}
