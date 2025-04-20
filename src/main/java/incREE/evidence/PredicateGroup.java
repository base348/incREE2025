package incREE.evidence;

import ch.javasoft.bitset.IBitSet;
import incREE.dataset.Column;

import java.util.List;

public abstract class PredicateGroup {
    public enum Type {
        NUMERIC("NUMERIC"),
        STRING("STRING");

        public final String name;
        Type(String name) {
            this.name = name;
        }
    }

    /**
     * Predicate from the same group can not combine freely.
     * There are 2 cases for STRING: EQUAL or NOT_EQUAL.
     * There are only 3 cases for NUMERIC: EQUAL, GREATER_THAN or LESS_THAN.
     * EQUAL includes predicate = >= <=
     * GREATER includes predicate != > >=
     * LESS_THAN includes predicate != < <=
     */
    public enum OperatorGroup {
        EQUAL,
        NOT_EQUAL,
        GREATER_THAN,
        LESS_THAN,
    }

    private static final IBitSet eq = PredicateBitmap.bf.create();
    private static final IBitSet gt = PredicateBitmap.bf.create();
    private static final IBitSet lt = PredicateBitmap.bf.create();

    static {
        eq.set(0);
        eq.set(4);
        eq.set(5);

        gt.set(1);
        gt.set(2);
        gt.set(4);

        lt.set(1);
        lt.set(3);
        lt.set(5);
    }

    Type type;
    int offset;
    int length;

    boolean isReflexive;
    boolean isMajor = true;
    public final PredicateBitmap bits = new PredicateBitmap();
    public final PredicateBitmap head = new PredicateBitmap();
    public final PredicateBitmap eqFix = new PredicateBitmap();
    public final PredicateBitmap gtFix = new PredicateBitmap();

    void init() {
        this.bits.set(offset, offset +length);

        if (type.equals(Type.NUMERIC)) {
            // OperatorGroup.LESS_THAN: != < ≤
            this.head.set(offset + 1);
            this.head.set(offset + 3);
            this.head.set(offset + 5);

            // LESS_THAN to EQUAL: = != < ≥
            this.eqFix.set(offset);
            this.eqFix.set(offset + 1);
            this.eqFix.set(offset + 3);
            this.eqFix.set(offset + 4);

            // LESS_THAN to GREATER_THAN: > < ≥ ≤
            this.gtFix.set(offset + 2);
            this.gtFix.set(offset + 3);
            this.gtFix.set(offset + 4);
            this.gtFix.set(offset + 5);

        } else {
            // OperatorGroup.NOT_EQUAL
            this.head.set(offset + 1);

            this.eqFix.set(offset);
            this.eqFix.set(offset + 1);
        }
    }

    public boolean isMajor() {
        return this.isMajor;
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

    /**
     * Set the corresponding bits to head in the whole bitSet.
     * Initialize bit set with correct bits as much as possible, so avoid EQUAL case.
     * @param aim Whole bitSet for whole predicate space. Will be changed in place.
     */
    public void setHead(PredicateBitmap aim) {
        aim.or(head);
    }

    private boolean groupGet(PredicateBitmap bitSet, int pos) {
        if (pos < this.length) {
            return bitSet.get(this.offset+pos);
        }
        return false;
    }

    private IBitSet groupGet(PredicateBitmap bitSet) {
        IBitSet bs = PredicateBitmap.bf.create();
        for (int i = 0; i < this.length; i++) {
            if (bitSet.get(this.offset + i)) bs.set(i);
        }
        return bs;
    }

    private void groupSet(PredicateBitmap bitSet, int pos) {
        if (pos < this.length) {
            bitSet.set(this.offset+pos);
        }
    }

    private void groupSet(PredicateBitmap bitSet, IBitSet groupSet) {
        for (int i = 0; i < this.length; i++) {
            if (groupSet.get(i)) groupSet(bitSet,i);
        }
    }

    public void setImage(PredicateBitmap bitSet, PredicateBitmap aim) {
        IBitSet bs;
        if (this.type.equals(PredicateGroup.Type.NUMERIC)) {
            bs = PredicateBitmap.bf.create();
            // > < ≥ ≤
            if (groupGet(bitSet, 0)) bs.set(0);
            if (groupGet(bitSet, 1)) bs.set(1);
            if (groupGet(bitSet, 2)) bs.set(3);
            if (groupGet(bitSet, 3)) bs.set(2);
            if (groupGet(bitSet, 4)) bs.set(5);
            if (groupGet(bitSet, 5)) bs.set(4);
        } else {
            bs = groupGet(bitSet);
        }
        this.getReversed().groupSet(aim, bs);
    }

    /**
     * @return Cases used for evidence reconcile. Not include cases which is used as head.
     */
    public OperatorGroup[] getReconcileOperatorGroup() {
        if (type.equals(PredicateGroup.Type.STRING)) {
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
        if (this.type.equals(PredicateGroup.Type.STRING)) {
            if (operatorGroup.equals(OperatorGroup.EQUAL)) {
                return eqFix;
            } else {
                System.err.println("PredicateGroup.getFixSet: unexpected operator group");
            }
        } else if (this.type.equals(PredicateGroup.Type.NUMERIC)) {
            if (operatorGroup.equals(OperatorGroup.EQUAL)) {
                return eqFix;
            } else if (operatorGroup.equals(OperatorGroup.GREATER_THAN)) {
                return gtFix;
            } else {
                System.err.println("PredicateGroup.getFixSet: unexpected operator group");
            }
        }
        return null;
    }

    public boolean isLegal(PredicateBitmap predicateBitmap) {
        IBitSet bs = this.groupGet(predicateBitmap);
        if (this.type.equals(PredicateGroup.Type.STRING)) {
            return bs.get(0) ^ bs.get(1);
        } else return bs.equals(eq) || bs.equals(gt) || bs.equals(lt);
    }

    public abstract int getAllPredicatesNum();

    public abstract PredicateGroup getReversed();

    public static class JsonDTO {
        public String firstColumn;
        public String secondColumn;
        public PredicateGroup.Type type;

        public JsonDTO(String firstColumn, String secondColumn, PredicateGroup.Type type) {
            this.firstColumn = firstColumn;
            this.secondColumn = secondColumn;
            this.type = type;
        }

        public void setFirstColumn(String firstColumn) {
            this.firstColumn = firstColumn;
        }
        public void setSecondColumn(String secondColumn) {
            this.secondColumn = secondColumn;
        }
        public void setType(PredicateGroup.Type type) {
            this.type = type;
        }
        public String getFirstColumn() {
            return firstColumn;
        }
        public String getSecondColumn() {
            return secondColumn;
        }
        public PredicateGroup.Type getType() {
            return type;
        }
    }

    public abstract JsonDTO toJsonDTO();
}
