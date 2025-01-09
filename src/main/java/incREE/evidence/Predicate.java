package incREE.evidence;

import incREE.Main;
import incREE.dataset.Column;
import incREE.dataset.ColumnPair;
import incREE.dataset.RawColumn;
import incREE.dataset.Relation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Predicate {
    private static final double MINIMUM_SHARED_VALUE = 0.3d;

    public Column<?> attribute1;
    public Operator operator;
    public Column<?> attribute2;

    public Predicate(Column<?> attribute1, Operator operator, Column<?> attribute2) {
        this.attribute1 = attribute1;
        this.operator = operator;
        this.attribute2 = attribute2;
    }

    /**
     * Only attributes with same type and share at least MINIMUM_SHARED_VALUE can be combined
     */
    public static List<Predicate> getPredicatesSpace(Relation relation) {
        List<Predicate> predicates = new ArrayList<>();

        Map<Boolean, List<ColumnPair>> partitioned = relation.getColumnPairs().stream().filter(
                columnPair -> columnPair.firstColumn().getSharedPercentage(columnPair.secondColumn()) > MINIMUM_SHARED_VALUE
        ).collect(
                Collectors.partitioningBy(columnPair -> columnPair.firstColumn().type == RawColumn.Type.STRING)
        );

        List<ColumnPair> stringColumnPairs = partitioned.get(true);
        for (ColumnPair columnPair : stringColumnPairs) {
            predicates.add(new Predicate(columnPair.firstColumn(), Operator.EQUAL, columnPair.secondColumn()));
            predicates.add(new Predicate(columnPair.firstColumn(), Operator.NOT_EQUAL, columnPair.secondColumn()));
        }

        List<ColumnPair> numericColumnPairs = partitioned.get(false);
        for (ColumnPair columnPair : numericColumnPairs) {
            predicates.add(new Predicate(columnPair.firstColumn(), Operator.EQUAL, columnPair.secondColumn()));
            predicates.add(new Predicate(columnPair.firstColumn(), Operator.NOT_EQUAL, columnPair.secondColumn()));
            predicates.add(new Predicate(columnPair.firstColumn(), Operator.GREATER_THAN, columnPair.secondColumn()));
            predicates.add(new Predicate(columnPair.firstColumn(), Operator.LESS_THAN, columnPair.secondColumn()));
            predicates.add(new Predicate(columnPair.firstColumn(), Operator.GREATER_THAN_OR_EQUAL, columnPair.secondColumn()));
            predicates.add(new Predicate(columnPair.firstColumn(), Operator.LESS_THAN_OR_EQUAL, columnPair.secondColumn()));
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
        return String.format("tx.%s %s ty.%s satisfied by %.2f%% tuple pairs.\n", attribute1,
                operator, attribute2, getSelectivity(Main.relation)*100);
    }
}
