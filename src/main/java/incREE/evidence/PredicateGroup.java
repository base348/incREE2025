package incREE.evidence;

import incREE.dataset.ColumnPair;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

// Records a part of whole predicate space
// Not include data tuple information
public class PredicateGroup {

    public enum Type {
        NUMERIC,
        STRING,
    }

    /**
     * Predicate from the same group can not combine freely.
     * There are 2 cases for STRING: EQUAL or NOT_EQUAL.
     * There are only 3 cases for NUMERIC: EQUAL, GREATER_THAN or LESS_THAN.
     * EQUAL includes predicate = ≠ ≤
     * GREATER includes predicate ≠ > ≥
     * LESS_THAN includes predicate ≠ < ≤
     */
    public enum OperatorGroup {
        EQUAL,
        NOT_EQUAL,
        GREATER_THAN,
        LESS_THAN,
    }

    Type type;
    public final ColumnPair columnPair;
    private final int offset;
    private final int length;
    private final List<Predicate<?>> allPredicates;
    private final boolean isReflexive;
    public final BitSet bits = new BitSet();

    public PredicateGroup(Type type, List<Predicate<?>> allPredicates, int offset, ColumnPair columnPair) {
        this.type = type;
        this.columnPair = columnPair;
        this.offset = offset;

        if (type.equals(Type.NUMERIC)) {
            this.length = 6;
            allPredicates.add(Predicate.build(columnPair.firstColumn(), Operator.EQUAL, columnPair.secondColumn()));
            allPredicates.add(Predicate.build(columnPair.firstColumn(), Operator.NOT_EQUAL, columnPair.secondColumn()));
            allPredicates.add(Predicate.build(columnPair.firstColumn(), Operator.GREATER_THAN, columnPair.secondColumn()));
            allPredicates.add(Predicate.build(columnPair.firstColumn(), Operator.LESS_THAN, columnPair.secondColumn()));
            allPredicates.add(Predicate.build(columnPair.firstColumn(), Operator.GREATER_THAN_OR_EQUAL, columnPair.secondColumn()));
            allPredicates.add(Predicate.build(columnPair.firstColumn(), Operator.LESS_THAN_OR_EQUAL, columnPair.secondColumn()));
        } else {
            this.length = 2;
            allPredicates.add(Predicate.build(columnPair.firstColumn(), Operator.EQUAL, columnPair.secondColumn()));
            allPredicates.add(Predicate.build(columnPair.firstColumn(), Operator.NOT_EQUAL, columnPair.secondColumn()));
        }

        this.allPredicates = allPredicates;
        this.isReflexive = columnPair.isReflexive();
        this.bits.set(offset, offset+length);
    }

    public boolean isReflexive() {
        return isReflexive;
    }

    public int contains(int predicate) {
        if (predicate < this.offset) {
            return -1;
        }  else if (predicate >= this.offset + this.length) {
            return 1;
        }  else {
            return 0;
        }
    }

    public static PredicateGroup findGroup(int predicate, List<PredicateGroup> predicateGroups) {
        //  TODO: use binary search
//        int left = 0;
//        int right = predicateGroups.size();
//        while (left < right) {
//            int mid = left + (right - left) / 2;
//            int contain = predicateGroups.get(mid).contains(predicate);
//            if (contain == -1) {
//                 = mid;
//            } else if (contain == 1) {
//                left = mid + 1;
//            }
//        }
        for (PredicateGroup predicateGroup : predicateGroups) {
            if (predicateGroup.contains(predicate) == 0) {
                return predicateGroup;
            }
        }
        throw new IllegalArgumentException("No predicate group found");
    }

    /**
     * Set the corresponding bits to head in the whole bitSet.
     * Initialize bit set with correct bits as much as possible, so avoid EQUAL case.
     * @param bitSet Whole bitSet for whole predicate space. Will be changed in place.
     */
    public void setHead(BitSet bitSet) {
        if (type.equals(Type.NUMERIC)) {
            // OperatorGroup.LESS_THAN: ≠ < ≤
            bitSet.set(offset + 1);
            bitSet.set(offset + 3);
            bitSet.set(offset + 5);
        } else {
            // OperatorGroup.NOT_EQUAL
            bitSet.set(offset + 1);
        }
    }

    public void setSymmetry(PredicateBitmap bitSet, BitSet aim) {
        if (this.isReflexive() && this.type.equals(Type.NUMERIC)) {
            // > < ≥ ≤
            if (bitSet.get(offset)) aim.set(offset);
            if (bitSet.get(offset + 1)) aim.set(offset + 1);
            if (bitSet.get(offset + 2)) aim.set(offset + 3);
            if (bitSet.get(offset + 3)) aim.set(offset + 2);
            if (bitSet.get(offset + 4)) aim.set(offset + 5);
            if (bitSet.get(offset + 5)) aim.set(offset + 4);
        } else {
            for (int i = 0; i < this.length; i++) {
                if (bitSet.get(offset + i)) aim.set(offset + i);
            }
        }
    }

    /**
     * @return Cases used for evidence reconcile. Not include cases which is used as head.
     */
    public OperatorGroup[] getReconcileOperatorGroup() {
        if (type.equals(Type.STRING)) {
            return new OperatorGroup[]{OperatorGroup.EQUAL};
        } else {
            return new OperatorGroup[]{OperatorGroup.EQUAL, OperatorGroup.GREATER_THAN};
        }
    }

    /**
     * @param operatorGroup Groups from getReconcileOperatorGroup()
     * @return Fix set between head and provided operatorGroup
     */
    public PredicateBitmap getFixSet(OperatorGroup operatorGroup) {
        BitSet bitSet = new BitSet();

        if (this.type.equals(Type.STRING)) {
            if (operatorGroup.equals(OperatorGroup.EQUAL)) {
                // NOT_EQUAL to EQUAL: = ≠
                bitSet.set(offset);
                bitSet.set(offset + 1);
            } else {
                System.err.println("PredicateGroup.getFixSet: unexpected operator group");
            }
        } else if (this.type.equals(Type.NUMERIC)) {
            if (operatorGroup.equals(OperatorGroup.EQUAL)) {
                // LESS_THAN to EQUAL: = ≠ < ≥
                bitSet.set(offset);
                bitSet.set(offset + 1);
                bitSet.set(offset + 3);
                bitSet.set(offset + 4);
            } else if (operatorGroup.equals(OperatorGroup.GREATER_THAN)) {
                // LESS_THAN to GREATER_THAN: > < ≥ ≤
                bitSet.set(offset + 2);
                bitSet.set(offset + 3);
                bitSet.set(offset + 4);
                bitSet.set(offset + 5);
            } else {
                System.err.println("PredicateGroup.getFixSet: unexpected operator group");
            }
        }

        return new PredicateBitmap(bitSet);
    }

    public int getAllPredicatesNum() {
        return allPredicates.size();
    }
}
