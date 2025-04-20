package incREE.evidence;

import incREE.dataset.Column;

public class AbstractPredicate {
    public final String attribute1;
    public final Operator operator;
    public final String attribute2;
    public final int identifier;

    private static int counter = 0;

    private AbstractPredicate(String attribute1, Operator operator, String attribute2, int identifier) {
        this.attribute1 = attribute1;
        this.operator = operator;
        this.attribute2 = attribute2;
        this.identifier = identifier;
    }

    public static AbstractPredicate build(String attribute1, Operator operator, String attribute2) {
        return new AbstractPredicate(attribute1, operator, attribute2, counter++);
    }
}
