package incREE.dataset;

import java.util.*;

public abstract class Column<T> {
    public String name;
    private int size = 0;
    protected Map<T, TreeSet<Integer>> PLI;
    private final List<T> values = new ArrayList<>();

    public Column(String name) {
        this.name = name;
    }


    public void addLine(T value) {
        values.add(value);
        PLI.compute(value, (k, v) -> v == null ? new TreeSet<>() : v).add(size++);
    }

    public T get(int index) {
        return values.get(index);
    }

    public boolean isNumeric() {
        return false;
    }

    public void printPLI() {
        System.out.println(name);
        PLI.forEach((k, l) -> {
            System.out.println("k = \"" + k + "\", l=" + l);
        });
        System.out.println("------------------------------------");
    }
}
