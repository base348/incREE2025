package incREE.dataset;

import incREE.evidence.Predicate;
import incREE.evidence.PredicateGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Relation {
    private static final double MINIMUM_SHARED_VALUE = 0.3d;
    int size;
    public List<String> attributeNames = new ArrayList<String>();
    public List<Column<?>> attributes;
    int attributeCount;
    public final List<Predicate<?>> predicateSpace = new ArrayList<>();
    public final List<PredicateGroup> predicateGroups = new ArrayList<>();

    public Relation(List<Column<?>> attributes, int size) {
        this.attributes = attributes;
        this.size = size;
        this.attributeCount = attributes.size();
        for (Column<?> attribute : attributes) {
            attributeNames.add(attribute.name);
        }

        List<ColumnPair> columnPairs = new ArrayList<>();
        for (int i = 0; i < attributeCount; i++) {
            for (int j = i; j < attributeCount; j++) {
                Column<?> attribute = attributes.get(i);
                Column<?> attribute2 = attributes.get(j);
                if (attribute.type.equals(attribute2.type)) {
                    columnPairs.add(new ColumnPair(attribute, attribute2));
                }
            }
        }

        Map<Boolean, List<ColumnPair>> partitioned = columnPairs.stream().filter(
                columnPair -> columnPair.firstColumn().getSharedPercentage(columnPair.secondColumn()) > MINIMUM_SHARED_VALUE
        ).collect(
                Collectors.partitioningBy(columnPair -> columnPair.firstColumn().type == RawColumn.Type.STRING)
        );

        List<ColumnPair> stringColumnPairs = partitioned.get(true);
        int length = 0;
        for (ColumnPair columnPair : stringColumnPairs) {
            predicateGroups.add(new PredicateGroup(PredicateGroup.Type.STRING, predicateSpace, length, columnPair));
            length += 2;
        }
        List<ColumnPair> numericColumnPairs = partitioned.get(false);
        for (ColumnPair columnPair : numericColumnPairs) {
            predicateGroups.add(new PredicateGroup(PredicateGroup.Type.NUMERIC, predicateSpace, length, columnPair));
            length += 6;
        }
    }

    public int getTuplePairId(int tidX, int tidY) {
        return tidX * size + tidY;
    }

    public Tuple getTuple(int tid) {
        return new Tuple(this, tid);
    }

    public TuplePair getTuplePair(int tidX, int tidY) {
        return new TuplePair(this, tidX, tidY);
    }

    public TuplePair getTuplePair(int tpId) {
        return new TuplePair(this, tpId);
    }

    /**
     * All TuplePair, including reversed pairs like (0, 1) and (1, 0), without repeated pairs like (1, 1)
     */
    public void foreachTuplePair(Consumer<TuplePair> action) {
        int terminal = getTuplePairId(size-1, size-1);
        for (int tpId = 0; tpId < terminal; tpId++) {
            if (tpId % (size + 1) != 0) {
                action.accept(getTuplePair(tpId));
            }
        }
    }

    public int getTotalTuplePairs() {
        return size * (size - 1);
    }

    public int getMaxTuplePairId() {
        return getTuplePairId(size-1, size-1);
    }

    public boolean isReflexive(int tpId) {
        return (tpId % (size + 1) == 0);
    }


    public <T extends Comparable<T>> boolean satisfies(int tpId, Predicate<T> predicate) {
        Column<T> attribute1 = predicate.attribute1;
        Column<T> attribute2 = predicate.attribute2;
        if (!attribute1.type.equals(attribute2.type)) {
            throw new IllegalArgumentException("Invalid predicate: " + predicate + " has different types of attributes.");
        }

        int idX = tpId / size;
        int idY = tpId % size;

        T o1 = attribute1.get(idX);
        T o2 = attribute2.get(idY);

        return switch (attribute1.type) {
            case STRING -> switch (predicate.operator) {
                case EQUAL -> o1.equals(o2);
                case NOT_EQUAL -> !o1.equals(o2);
                default ->
                        throw new IllegalArgumentException("Invalid predicate: " + predicate + " with string attributes and unsupported operator " + predicate.operator);
            };
            case LONG, NUMERIC -> predicate.operator.compareAttributes(o1, o2);
        };
    }


    public void print() {
        System.out.println("Size: " + size + " * " + attributeCount);
        System.out.println("Attributes: " + attributeNames);
        for (Column<?> attribute : attributes) {
            attribute.printPLI();
        }
    }
}
