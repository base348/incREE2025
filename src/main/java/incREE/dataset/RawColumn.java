package incREE.dataset;

import incREE.helpers.ParserHelper;

import java.util.ArrayList;
import java.util.List;

public class RawColumn {

    public enum Type {
        STRING, NUMERIC, LONG
    }

    private final String name;
    private final List<String> values = new ArrayList<String>();
    private Type type = Type.LONG;

    public RawColumn(String name) {
        this.name = name;
    }

    public boolean isNumeric() {
        return false;
    }

    public Type getTypeByName() {
        if (name.contains("String"))
            return Type.STRING;
        if (name.contains("Double"))
            return Type.NUMERIC;
        if (name.contains("Integer"))
            return Type.LONG;
        return type;
    }

    public void addLine(String string) {
        if (type == Type.LONG && !ParserHelper.isInteger(string))
            type = Type.NUMERIC;
        if (type == Type.NUMERIC && !ParserHelper.isDouble(string))
            type = Type.STRING;
        values.add(string);
    }


    public Long getLong(int line) {
        return Long.valueOf(values.get(line));
    }

    public Double getDouble(int line) {
        return Double.valueOf(values.get(line));
    }

    public String getString(int line) {
        return values.get(line) == null ? "" : values.get(line);
    }

    public Column<?> build(int lineCount) {
        switch (this.getTypeByName()) {
            case STRING:
                StringColumn column = new StringColumn(name);
                for (int line = 0; line < lineCount; line++) {
                    column.addLine(getString(line));
                }
                return column;
            case NUMERIC:
                NumericColumn<Double> column1 = new NumericColumn<>(name, Type.NUMERIC);
                for (int line = 0; line < lineCount; line++) {
                    column1.addLine(getDouble(line));
                }
                return column1;
            case LONG:
                NumericColumn<Long> column2 = new NumericColumn<>(name, Type.LONG);
                for (int line = 0; line < lineCount; line++) {
                    column2.addLine(getLong(line));
                }
                return column2;
            default:
                return null;
        }
    }

    @Override
    public String toString() {
        return "RawColumn [name=" + name + ", type=" + type + "]";
    }
}
