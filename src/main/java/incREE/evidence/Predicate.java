package incREE.evidence;

import incREE.dataset.Column;
import incREE.dataset.RawColumn;
import incREE.dataset.Relation;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Predicate<T extends Comparable<T>> {
    private static final double MINIMUM_SHARED_VALUE = 0.3d;
    private static int count = 0;

    public final Column<T> attribute1;
    public final Operator operator;
    public final Column<T> attribute2;
    public final int identifier;

    private Predicate(Column<T> attribute1, Operator operator, Column<T> attribute2, int identifier) {
        this.attribute1 = attribute1;
        this.operator = operator;
        this.attribute2 = attribute2;
        this.identifier = identifier;
    }

    public static <T extends Comparable<T>> Predicate<T> build(Column<?> attribute1, Operator operator, Column<?> attribute2, int index) {
        if (!attribute1.type.equals(attribute2.type)) {
            throw new IllegalArgumentException("Column types must match: " +
                    attribute1.type + " vs " + attribute2.type);
        }

        @SuppressWarnings("unchecked")
        Column<T> typedAttribute1 = (Column<T>) attribute1;

        @SuppressWarnings("unchecked")
        Column<T> typedAttribute2 = (Column<T>) attribute2;

        return new Predicate<>(typedAttribute1, operator, typedAttribute2, index);
    }

    public static <T extends Comparable<T>> Predicate<T> build(Column<?> attribute1, Operator operator, Column<?> attribute2) {
        return build(attribute1, operator, attribute2, count++);
    }

    public Set<Predicate<?>> getFixSet(Relation relation) {
        if (this.attribute1.type.equals(RawColumn.Type.STRING)) {
            // non-numeric equality
            if (this.operator.equals(Operator.EQUAL)) {
                return Set.of(this, relation.predicateSpace.get(this.identifier +1));
            } else {
                throw new IllegalArgumentException("Only equal operators are supported for non-numeric");
            }
        } else {
            if (this.operator.equals(Operator.EQUAL)) {
                // numeric equality
                return Set.of(this
                        , relation.predicateSpace.get(this.identifier +1)
                        , relation.predicateSpace.get(this.identifier +3)
                        , relation.predicateSpace.get(this.identifier +4));
            } else {
                // numeric gt
                return Set.of(this
                        , relation.predicateSpace.get(this.identifier +1)
                        , relation.predicateSpace.get(this.identifier +2)
                        , relation.predicateSpace.get(this.identifier +3));
            }
        }
    }

    public boolean isDependent(Predicate<?> predicate) {
        return (this.attribute1 == predicate.attribute1) && (this.attribute2 == predicate.attribute2);
    }

    /**
     * percent of tuple pairs that satisfy this predicate
     */
    public double getSelectivity(Relation relation) {
        AtomicInteger satisfied = new AtomicInteger();
        int total = relation.getTotalTuplePairs();
        relation.foreachTuplePair(tuplePair -> {
            if (tuplePair.satisfies(this)) {
                satisfied.getAndIncrement();
//                        System.out.println(tuplePair.getTpId() + " satisfied.");
            }
        });
        return (double) satisfied.get() / total;
    }


    @Override
    public String toString() {
//        return String.format("tx.%s %s ty.%s", attribute1,
//                operator, attribute2);
        return String.format("p%d", identifier);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Predicate<?> that = (Predicate<?>) o;
        return that.identifier == identifier;
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }
}
