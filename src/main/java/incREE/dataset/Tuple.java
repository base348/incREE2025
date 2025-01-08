package incREE.dataset;

public class Tuple {
    final int id;
    final Relation relation;

    public Tuple(Relation relation, int id) {
        this.relation = relation;
        this.id = id;
    }

    public Object getAttribute(int aid) {
        return relation.attributes.get(aid).get(id);
    }
}
