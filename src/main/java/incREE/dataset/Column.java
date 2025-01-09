package incREE.dataset;

import com.google.common.collect.Sets;

import java.util.*;

public abstract class Column<T> {
    public String name;
    private int size = 0;
    protected Map<T, TreeSet<Integer>> PLI;
    private final List<T> values = new ArrayList<>();
    public final RawColumn.Type type;

    public Column(String name, RawColumn.Type type) {
        this.name = name;
        this.type = type;
    }


    public void addLine(T value) {
        values.add(value);
        PLI.compute(value, (k, v) -> v == null ? new TreeSet<>() : v).add(size++);
    }

    public T get(int index) {
        return values.get(index);
    }

    @SuppressWarnings("unchecked")
    public double getSharedPercentage(Column<?> column) {
        if (column.type.equals(type)) {
            if (this.equals(column)) {
                return 1;
            }
            Set<T> keys = PLI.keySet();
            Set<T> keys2 = (Set<T>) column.PLI.keySet();
            Set<T> intersection = Sets.intersection(keys, keys2);
            return intersection.size() / (double) Math.min(keys.size(), keys2.size());
        } else {
            return 0;
        }
    }

    public void printPLI() {
        System.out.println(name);
        PLI.forEach((k, l) -> System.out.println("k = \"" + k + "\", l=" + l));
        System.out.println("------------------------------------");
    }

    @Override
    public String toString() {
        return name;
    }
}
