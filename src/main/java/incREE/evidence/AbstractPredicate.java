package incREE.evidence;

import incREE.dataset.Column;

import java.util.List;

public class AbstractPredicate {
    public final String attribute1;
    public final Operator operator;
    public final String attribute2;
    public final int identifier;
    public final int groupId;
    private final boolean isSelfSymmetry;

    private static int counter = 0;

    private AbstractPredicate(String attribute1, Operator operator, String attribute2, int identifier, int groupId) {
        this.attribute1 = attribute1;
        this.operator = operator;
        this.attribute2 = attribute2;
        this.identifier = identifier;
        this.groupId = groupId;
        this.isSelfSymmetry = this.operator.isSelfSymmetry() && this.attribute2.equals(this.attribute1);
    }

    public static AbstractPredicate build(String attribute1, Operator operator, String attribute2, int groupId) {
        return new AbstractPredicate(attribute1, operator, attribute2, counter++, groupId);
    }

    public String getNegativeExpression() {
        return String.format("tx.%s %s ty.%s", attribute1,
                operator.negation(), attribute2);
    }

    public String getExpression() {
        return String.format("tx.%s %s ty.%s", attribute1,
                operator, attribute2);
    }

    /**
     * This hold for tuple pair (t2, t1) if and only if this hold for tuple pair (t2, t1)
     */
    public boolean isSelfSymmetry() {
        return this.isSelfSymmetry;
    }

    public AbstractPredicateGroup findGroup(List<AbstractPredicateGroup> predicateGroups) {
        return predicateGroups.get(groupId);
    }
}
