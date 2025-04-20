package incREE.evidence;

import ch.javasoft.bitset.IBitSet;
import incREE.dataset.Column;
import incREE.dataset.ColumnPair;

import java.util.List;

// Records a part of whole predicate space
// Not include data tuple information
public class DataPredicateGroup extends PredicateGroup {

    public final ColumnPair columnPair;
    private final List<Predicate<?>> allPredicates;

    DataPredicateGroup reversed = null;

    public DataPredicateGroup(PredicateGroup.Type type, List<Predicate<?>> allPredicates, int offset, ColumnPair columnPair) {
        this.type = type;
        this.columnPair = columnPair;
        this.offset = offset;

        if (type.equals(PredicateGroup.Type.NUMERIC)) {
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
        init();
    }

//    public static List<AbstractPredicateGroup> toAbstract(List<AbstractPredicateGroup> allPredicates) {
//
//    }

    @Override
    public DataPredicateGroup getReversed() {
        if (this.isReflexive) {
            return this;
        }
        if (this.reversed == null) {
            this.reversed = new DataPredicateGroup(this.type, allPredicates, offset+length, columnPair.getReversed());
            this.reversed.isMajor = false;
            this.reversed.reversed = this;
        }
        return this.reversed;
    }

    @Override
    public String toString() {
        return columnPair.firstColumn() + "," + columnPair.secondColumn() + "," + offset;
    }

    @Override
    public int getAllPredicatesNum() {
        return allPredicates.size();
    }

    @Override
    public JsonDTO toJsonDTO() {
        return new JsonDTO(columnPair.firstColumn().name, columnPair.secondColumn().name, type);
    }

}
