package incREE.evidence;

import incREE.dataset.Column;
import incREE.dataset.ColumnPair;
import incREE.dataset.RawColumn;
import incREE.dataset.Relation;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Predicate<T extends Comparable<T>> {
    private static final double MINIMUM_SHARED_VALUE = 0.3d;

    public final Column<T> attribute1;
    public final Operator operator;
    public final Column<T> attribute2;
    public final int index;

    private Predicate(Column<T> attribute1, Operator operator, Column<T> attribute2, int index) {
        this.attribute1 = attribute1;
        this.operator = operator;
        this.attribute2 = attribute2;
        this.index = index;
    }

    private static <T extends Comparable<T>> Predicate<T> build(Column<?> attribute1, Operator operator, Column<?> attribute2, int index) {
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

    /**
     * Only attributes with same type and share at least MINIMUM_SHARED_VALUE can be combined
     */
    public static List<Predicate<?>> getPredicatesSpace(Relation relation) {
        List<Predicate<?>> predicates = new ArrayList<>();
        int i = 0;

        Map<Boolean, List<ColumnPair>> partitioned = relation.getColumnPairs().stream().filter(
                columnPair -> columnPair.firstColumn().getSharedPercentage(columnPair.secondColumn()) > MINIMUM_SHARED_VALUE
        ).collect(
                Collectors.partitioningBy(columnPair -> columnPair.firstColumn().type == RawColumn.Type.STRING)
        );

        List<ColumnPair> stringColumnPairs = partitioned.get(true);
        for (ColumnPair columnPair : stringColumnPairs) {
            predicates.add(build(columnPair.firstColumn(), Operator.EQUAL, columnPair.secondColumn(), i++));
            predicates.add(build(columnPair.firstColumn(), Operator.NOT_EQUAL, columnPair.secondColumn(), i++));
        }

        List<ColumnPair> numericColumnPairs = partitioned.get(false);
        for (ColumnPair columnPair : numericColumnPairs) {
            predicates.add(build(columnPair.firstColumn(), Operator.EQUAL, columnPair.secondColumn(), i++));
            predicates.add(build(columnPair.firstColumn(), Operator.NOT_EQUAL, columnPair.secondColumn(), i++));
            predicates.add(build(columnPair.firstColumn(), Operator.GREATER_THAN, columnPair.secondColumn(), i++));
            predicates.add(build(columnPair.firstColumn(), Operator.LESS_THAN, columnPair.secondColumn(), i++));
            predicates.add(build(columnPair.firstColumn(), Operator.GREATER_THAN_OR_EQUAL, columnPair.secondColumn(), i++));
            predicates.add(build(columnPair.firstColumn(), Operator.LESS_THAN_OR_EQUAL, columnPair.secondColumn(), i++));
        }
        return predicates;
    }

    public Set<Predicate<?>> getFixSet(Relation relation) {
        if (this.attribute1.type.equals(RawColumn.Type.STRING)) {
            // non-numeric equality
            if (this.operator.equals(Operator.EQUAL)) {
                return Set.of(this, relation.predicateSpace.get(this.index+1));
            } else {
                throw new IllegalArgumentException("Only equal operators are supported for non-numeric");
            }
        } else {
            if (this.operator.equals(Operator.EQUAL)) {
                // numeric equality
                return Set.of(this
                        , relation.predicateSpace.get(this.index+1)
                        , relation.predicateSpace.get(this.index+3)
                        , relation.predicateSpace.get(this.index+4));
            } else {
                // numeric gt
                return Set.of(this
                        , relation.predicateSpace.get(this.index+1)
                        , relation.predicateSpace.get(this.index+2)
                        , relation.predicateSpace.get(this.index+3));
            }
        }
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
        return String.format("tx.%s %s ty.%s", attribute1,
                operator, attribute2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Predicate<?> that = (Predicate<?>) o;
        return that.index == index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(index);
    }
}
