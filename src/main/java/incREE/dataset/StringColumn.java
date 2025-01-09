package incREE.dataset;

import java.util.HashMap;

public class StringColumn extends Column<String> {
    public StringColumn(String name) {
        super(name, RawColumn.Type.STRING);
        this.PLI = new HashMap<>();
    }
}
