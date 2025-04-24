package incREE.dataset;

import incREE.FileManager;
import incREE.evidence.Predicate;
import incREE.evidence.PredicateBitmap;
import incREE.evidence.DataPredicateGroup;
import incREE.evidence.PredicateGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Relation {
    private static final double MINIMUM_SHARED_VALUE = 0.3d;
    public final String name;
    public int currentSize; // After currentSize updated, previous TuplePairId can never be used
//    public final int totalSize;
    public List<String> attributeNames = new ArrayList<String>();
    public List<Column<?>> attributes;
    int attributeCount;
    public final List<Predicate<?>> predicateSpace = new ArrayList<>();
    public final List<DataPredicateGroup> predicateGroups = new ArrayList<>();

    public Relation(String name, List<Column<?>> attributes, int currentSize) {
        this.name = name;
        this.attributes = attributes;
        this.currentSize = currentSize;
//        this.totalSize = totalSize;
        this.attributeCount = attributes.size();
        for (Column<?> attribute : attributes) {
            attributeNames.add(attribute.name);
        }

        loadPredicateSpace();
    }

    public void buildPredicateSpace() {
        List<ColumnPair> columnPairs = getColumnPairs(attributes);
        buildPredicateSpace(columnPairs);
    }

    public void buildPredicateSpace(List<ColumnPair> columnPairs) {
        if (!predicateSpace.isEmpty()) {
            return;
        }
        int length = 0;
        for (ColumnPair columnPair : columnPairs) {
            DataPredicateGroup newGroup;
            int newLength = 0;
            if (columnPair.firstColumn().type == RawColumn.Type.STRING) {
                newGroup = new DataPredicateGroup(PredicateGroup.Type.STRING, predicateSpace, length, columnPair);
                newLength = 2;
            } else {
                newGroup = new DataPredicateGroup(PredicateGroup.Type.NUMERIC, predicateSpace, length, columnPair);
                newLength = 6;
            }
            predicateGroups.add(newGroup);
            length += newLength;

            if (!newGroup.isReflexive()) {
                predicateGroups.add(newGroup.getReversed());
                length += newLength;
            }
        }
    }

    public Column<?> getAttribute(String attributeName) throws Exception {
        for (Column<?> attribute : attributes) {
            if (attribute.name.equals(attributeName)) {
                return attribute;
            }
        }
        throw new Exception("No such attribute: " + attributeName);
    }

    public void loadPredicateSpace() {
        if (!predicateSpace.isEmpty()) {
            return;
        }
        List<ColumnPair> columnPairs;
        try {
            columnPairs = FileManager.loadColumnPairs(this);
        } catch (Exception e) {
            System.err.println("Error reading ColumnGroups from file.");
            columnPairs = getColumnPairs(attributes);
        }
        buildPredicateSpace(columnPairs);
    }

    private List<ColumnPair> getColumnPairs(List<Column<?>> attributes) {
        List<ColumnPair> columnPairs = new ArrayList<>();
        // Reflexive
        for (Column<?> column : attributes) {
            columnPairs.add(new ColumnPair(column, column));
        }

        // Not Reflexive
        for (int i = 0; i < attributeCount; i++) {
            for (int j = i+1; j < attributeCount; j++) {
                Column<?> attribute = attributes.get(i);
                Column<?> attribute2 = attributes.get(j);
                if (comparable(attribute, attribute2)) {
                    columnPairs.add(new ColumnPair(attribute, attribute2));
                }
            }
        }
        return columnPairs;
    }

    private boolean comparable(Column<?> column1, Column<?> column2) {
        return column1.type.equals(column2.type) && column1.getSharedPercentage(column2) > MINIMUM_SHARED_VALUE;
    }

    public int getTuplePairId(int tidX, int tidY) {
        return tidX * currentSize + tidY;
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
        int terminal = getTuplePairId(currentSize -1, currentSize -1);
        for (int tpId = 0; tpId < terminal; tpId++) {
            if (tpId % (currentSize + 1) != 0) {
                action.accept(getTuplePair(tpId));
            }
        }
    }

    public int getTotalTuplePairs() {
        return currentSize * (currentSize - 1);
    }

    public int getMaxTuplePairId() {
        return getTuplePairId(currentSize -1, currentSize -1);
    }

    public boolean isReflexive(int tpId) {
        return (tpId % (currentSize + 1) == 0);
    }

    public <T extends Comparable<T>> boolean satisfies(int idX, int idY, Predicate<T> predicate) {
        Column<T> attribute1 = predicate.attribute1;
        Column<T> attribute2 = predicate.attribute2;
        if (!attribute1.type.equals(attribute2.type)) {
            throw new IllegalArgumentException("Invalid predicate: " + predicate + " has different types of attributes.");
        }

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

    public boolean satisfies(int tpId, Predicate<?> predicate) {
        int idX = tpId / currentSize;
        int idY = tpId % currentSize;
        return satisfies(idX, idY, predicate);
    }

    public boolean satisfies(int idX, int idY, PredicateBitmap predicateBitmap) {
        for (Predicate<?> predicate : predicateSpace) {
            boolean result = satisfies(idX, idY, predicate);
            boolean set = predicateBitmap.get(predicate.identifier);
            if ((!set && result) || (set && !result)) {
                return false;
            }
        }
        return true;
    }


    public void print() {
        System.out.println("Size: " + currentSize + " * " + attributeCount);
        System.out.println("Attributes: " + attributeNames);
        for (Column<?> attribute : attributes) {
            attribute.printPLI();
        }
    }
}
