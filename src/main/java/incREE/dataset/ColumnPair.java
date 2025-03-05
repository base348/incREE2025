package incREE.dataset;

public record ColumnPair(Column<?> firstColumn, Column<?> secondColumn) {
    public boolean isReflexive() {
        return firstColumn == secondColumn;
    }
}
