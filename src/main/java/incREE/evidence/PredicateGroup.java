package incREE.evidence;

import incREE.dataset.ColumnPair;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;

public class PredicateGroup {

    public enum Type {
        NUMERIC,
        STRING,
    }

    /*
    public enum Operator {
        EQUAL,
        NOT_EQUAL,
        GREATER,
        LESS,
    }
     */

//    List<Predicate<?>> allPredicates;
    Type type;
    private final ColumnPair columnPair;
    private final int offset;
    private final int length;
    private final List<Predicate<?>> allPredicates;

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
    }

    public void setHead(BitSet bitSet) {
        if (type.equals(Type.NUMERIC)) {
            // ≠ < ≤
            bitSet.set(offset + 1);
            bitSet.set(offset + 3);
            bitSet.set(offset + 5);
        } else {
            bitSet.set(offset + 1);
        }
    }

    public List<Predicate<?>> getPredicates(List<Operator> operators) {
        List<Predicate<?>> predicates = new ArrayList<Predicate<?>>();
        for (Operator operator : operators) {
            if (operator.value < this.length) {
                predicates.add(allPredicates.get(offset + operator.value));
            }
        }
        return predicates;
    }

    public PredicateBitmap getFixSet(Predicate<?> predicate) {
        if (predicate.index < this.offset || predicate.index >= this.offset + this.length) {
            System.err.println("Error: invalid fix set");
            return null;
        }
        if (type.equals(Type.STRING)) {
            // non-numeric equality: = ≠
            BitSet bitSet = new BitSet();
            bitSet.set(offset);
            bitSet.set(offset + 1);
            return new PredicateBitmap(bitSet);
        } else if (predicate.operator.equals(Operator.EQUAL)) {
            // numeric equality: = ≠ < ≥
            BitSet bitSet = new BitSet();
            bitSet.set(offset);
            bitSet.set(offset + 1);
            bitSet.set(offset + 3);
            bitSet.set(offset + 4);
            return new PredicateBitmap(bitSet);
        } else {
            // numeric gt: > < ≥ ≤
            BitSet bitSet = new BitSet();
            bitSet.set(offset + 2);
            bitSet.set(offset + 3);
            bitSet.set(offset + 4);
            bitSet.set(offset + 5);
            return new PredicateBitmap(bitSet);
        }
    }
}
