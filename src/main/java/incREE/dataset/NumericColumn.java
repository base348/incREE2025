package incREE.dataset;

import java.util.TreeMap;

public class NumericColumn<T extends Comparable<T>> extends Column<T> {
    public NumericColumn(String name, RawColumn.Type type) {
        super(name, type);
        this.PLI = new TreeMap<>();
    }
}
