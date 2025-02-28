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

    public <T extends Comparable<T>> boolean satisfies(Predicate<T> predicate) {
        return relation.satisfies(tpId, predicate);
    }

    @Override
    public String toString() {
        int idX = tpId / relation.currentSize;
        int idY = tpId % relation.currentSize;
        return "(" + idX + ", " + idY + ")";
    }

}
