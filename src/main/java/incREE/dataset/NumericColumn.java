package incREE.dataset;

import java.util.TreeMap;

public class NumericColumn<T extends Comparable<T>> extends Column<T> {
    public NumericColumn(String name) {
        super(name);
        this.PLI = new TreeMap<>();
    }

    @Override
    public boolean isNumeric() {
        return true;
    }
}
