package incREE.dataset;

import java.util.ArrayList;
import java.util.List;

public class Relation {
    int size;
    List<String> attributeNames = new ArrayList<String>();
    List<Column<?>> attributes;
    int attributeCount;

    public Relation(List<Column<?>> attributes, int size) {
        this.attributes = attributes;
        this.size = size;
        this.attributeCount = attributes.size();
        for (Column<?> attribute : attributes) {
            attributeNames.add(attribute.name);
        }
    }

    public Tuple getTuple(int tid) {
        return new Tuple(this, tid);
    }

    public int getTuplePairId(int tidX, int tidY) {
        return tidX * size + tidY;
    }

//    public TuplePair getTuplePair(int tidX, int tidY) {}

//    public TuplePair getTuplePair(int tpid) {}

//    public List<Predicate> getPredicateSpace() {}

    public void print() {
        System.out.println("Size: " + size + " * " + attributeCount);
        System.out.println("Attributes: " + attributeNames);
        for (Column<?> attribute : attributes) {
            attribute.printPLI();
        }
    }
}
