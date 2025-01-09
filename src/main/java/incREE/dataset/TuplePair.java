package incREE.dataset;

import incREE.evidence.Predicate;

public class TuplePair {
    final int tpId;
    final Relation relation;

    public TuplePair(Relation relation, int idX, int idY) {
        this.relation = relation;
        this.tpId = relation.getTuplePairId(idX, idY);
    }

    public TuplePair(Relation relation, int tpId) {
        this.relation = relation;
        this.tpId = tpId;
    }

    public int getTpId() {
        return tpId;
    }

    public boolean satisfies(Predicate predicate) {
        Column<?> attribute1 = predicate.attribute1;
        Column<?> attribute2 = predicate.attribute2;
        if (!attribute1.type.equals(attribute2.type)) {
            throw new IllegalArgumentException("Invalid predicate: " + predicate + " has different types of attributes.");
        }

        int idX = tpId / relation.size;
        int idY = tpId % relation.size;

        switch (attribute1.type) {
            case STRING:
                String s1 = (String) attribute1.get(idX);
                String s2 = (String) attribute2.get(idY);
                return switch (predicate.operator) {
                    case EQUAL -> s1.equals(s2);
                    case NOT_EQUAL -> !s1.equals(s2);
                    default ->
                            throw new IllegalArgumentException("Invalid predicate: " + predicate + " with string attributes and unsupported operator " + predicate.operator);
                };
            case LONG:
                Long l1 = (Long) attribute1.get(idX);
                Long l2 = (Long) attribute2.get(idY);
                return predicate.operator.compareAttributes(l1, l2);
            case NUMERIC:
                Double d1 = (Double) attribute1.get(idX);
                Double d2 = (Double) attribute2.get(idY);
                return predicate.operator.compareAttributes(d1, d2);
            default:
                throw new IllegalArgumentException("Unknown type");
        }
    }

}
