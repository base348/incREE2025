package incREE.dataset;

import com.google.common.collect.Sets;

import java.util.*;

public class Column<T extends Comparable<T>> {
    public String name;
    private int size = 0;
    private final TreeMap<T, TreeSet<Integer>> PLI;
    private final List<T> values = new ArrayList<>();
    public final RawColumn.Type type;
    private int currentLineNumber;

    /**
     *
     * @param currentLineNumber Only tuples with id smaller than currentLineNumber will be calculated with PLI
     */
    public Column(String name, RawColumn.Type type, int currentLineNumber) {
        this.name = name;
        this.type = type;
        PLI = new TreeMap<>();
        this.currentLineNumber = currentLineNumber;
    }

    private void expandPLI(T value) {
        PLI.compute(value, (k, v) -> v == null ? new TreeSet<>() : v).add(size++);
    }

    public void addLine(T value) {
        values.add(value);
        if (size < currentLineNumber) {
            expandPLI(value);
        }
    }

    public T get(int index) {
        return values.get(index);
    }

    /**
     * Realized by PLI; need to deal with PLI update
     */
    @SuppressWarnings("unchecked")
    public double getSharedPercentage(Column<?> column) {
        if (column.type.equals(type)) {
            if (this.equals(column)) {
                return 1;
            }
            Set<T> keys = PLI.keySet();
            Set<T> keys2 = (Set<T>) column.PLI.keySet();
            Set<T> intersection = Sets.intersection(keys, keys2);
            double share = intersection.size() / (double) Math.min(keys.size(), keys2.size());
            return share;
        } else {
            return 0;
        }
    }

    public TreeMap<T, TreeSet<Integer>> getPLI(int aimLineNumber) {
        if (aimLineNumber < this.size) {
            throw new IllegalArgumentException("Column.getPLI: currentLineNumber > " + aimLineNumber + ", maybe you want delete tuples.");
        } else {
            // expand PLI here
            while (aimLineNumber > this.size) {
                expandPLI(this.values.get(this.size));
            }
            return PLI;
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
