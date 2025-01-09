package incREE.dataset;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Relation {
    int size;
    public List<String> attributeNames = new ArrayList<String>();
    public List<Column<?>> attributes;
    int attributeCount;

    public Relation(List<Column<?>> attributes, int size) {
        this.attributes = attributes;
        this.size = size;
        this.attributeCount = attributes.size();
        for (Column<?> attribute : attributes) {
            attributeNames.add(attribute.name);
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

    public List<ColumnPair> getColumnPairs() {
        List<ColumnPair> pairs = new ArrayList<>();
        for (int i = 0; i < attributeCount; i++) {
            for (int j = i; j < attributeCount; j++) {
                Column<?> attribute = attributes.get(i);
                Column<?> attribute2 = attributes.get(j);
                if (attribute.type.equals(attribute2.type)) {
                    pairs.add(new ColumnPair(attribute, attribute2));
                }
            }
        }
        return pairs;
    }

    public void print() {
        System.out.println("Size: " + size + " * " + attributeCount);
        System.out.println("Attributes: " + attributeNames);
        for (Column<?> attribute : attributes) {
            attribute.printPLI();
        }
    }
}
