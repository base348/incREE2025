package incREE.evidence;

import incREE.Main;
import incREE.dataset.Column;
import incREE.dataset.ColumnPair;
import incREE.dataset.RawColumn;
import incREE.dataset.Relation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Predicate<T extends Comparable<T>> {
    private static final double MINIMUM_SHARED_VALUE = 0.3d;

    public Column<T> attribute1;
    public Operator operator;
    public Column<T> attribute2;

    private Predicate(Column<T> attribute1, Operator operator, Column<T> attribute2) {
        this.attribute1 = attribute1;
        this.operator = operator;
        this.attribute2 = attribute2;
    }

    private static <T extends Comparable<T>> Predicate<T> build(Column<?> attribute1, Operator operator, Column<?> attribute2) {
        if (!attribute1.type.equals(attribute2.type)) {
            throw new IllegalArgumentException("Column types must match: " +
                    attribute1.type + " vs " + attribute2.type);
        }

        @SuppressWarnings("unchecked")
        Column<T> typedAttribute1 = (Column<T>) attribute1;

        @SuppressWarnings("unchecked")
        Column<T> typedAttribute2 = (Column<T>) attribute2;

        // 调用原始构造函数
        return new Predicate<>(typedAttribute1, operator, typedAttribute2);
    }

    /**
     * Only attributes with same type and share at least MINIMUM_SHARED_VALUE can be combined
     */
    public static List<Predicate<?>> getPredicatesSpace(Relation relation) {
        List<Predicate<?>> predicates = new ArrayList<>();

        Map<Boolean, List<ColumnPair>> partitioned = relation.getColumnPairs().stream().filter(
                columnPair -> columnPair.firstColumn().getSharedPercentage(columnPair.secondColumn()) > MINIMUM_SHARED_VALUE
        ).collect(
                Collectors.partitioningBy(columnPair -> columnPair.firstColumn().type == RawColumn.Type.STRING)
        );

        List<ColumnPair> stringColumnPairs = partitioned.get(true);
        for (ColumnPair columnPair : stringColumnPairs) {
            predicates.add(build(columnPair.firstColumn(), Operator.EQUAL, columnPair.secondColumn()));
            predicates.add(build(columnPair.firstColumn(), Operator.NOT_EQUAL, columnPair.secondColumn()));
        }

        List<ColumnPair> numericColumnPairs = partitioned.get(false);
        for (ColumnPair columnPair : numericColumnPairs) {
            predicates.add(build(columnPair.firstColumn(), Operator.EQUAL, columnPair.secondColumn()));
            predicates.add(build(columnPair.firstColumn(), Operator.NOT_EQUAL, columnPair.secondColumn()));
            predicates.add(build(columnPair.firstColumn(), Operator.GREATER_THAN, columnPair.secondColumn()));
            predicates.add(build(columnPair.firstColumn(), Operator.LESS_THAN, columnPair.secondColumn()));
            predicates.add(build(columnPair.firstColumn(), Operator.GREATER_THAN_OR_EQUAL, columnPair.secondColumn()));
            predicates.add(build(columnPair.firstColumn(), Operator.LESS_THAN_OR_EQUAL, columnPair.secondColumn()));
        }
        return predicates;
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
}
