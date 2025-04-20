package incREE.dataset;

public record ColumnPair(Column<?> firstColumn, Column<?> secondColumn) implements Comparable<ColumnPair> {
    public boolean isReflexive() {
        return firstColumn == secondColumn;
    }

    @Override
    public int compareTo(ColumnPair o) {
        return this.firstColumn.type.value - o.firstColumn.type.value;
    }

    public ColumnPair getReversed() {
        return new ColumnPair(this.secondColumn, this.firstColumn);
    }
}
