package incREE.evidence;

public enum Operator {
    EQUAL("="),
    NOT_EQUAL("≠"),
    LESS_THAN("<"),
    LESS_THAN_OR_EQUAL("≤"),
    GREATER_THAN(">"),
    GREATER_THAN_OR_EQUAL("≥");

    private final String symbol;

    Operator(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public boolean implication(Operator other) {
        return switch (this) {
            case EQUAL -> other == EQUAL || other == LESS_THAN_OR_EQUAL || other == GREATER_THAN_OR_EQUAL;
            case NOT_EQUAL -> other == NOT_EQUAL;
            case LESS_THAN -> other == LESS_THAN || other == LESS_THAN_OR_EQUAL || other == NOT_EQUAL;
            case LESS_THAN_OR_EQUAL -> other == LESS_THAN_OR_EQUAL;
            case GREATER_THAN -> other == GREATER_THAN || other == GREATER_THAN_OR_EQUAL || other == NOT_EQUAL;
            case GREATER_THAN_OR_EQUAL -> other == GREATER_THAN_OR_EQUAL;
            default -> throw new IllegalArgumentException("Unknown operator: " + this);
        };
    }

    public Operator negation() {
        return switch (this) {
            case EQUAL -> NOT_EQUAL;
            case NOT_EQUAL -> EQUAL;
            case LESS_THAN -> GREATER_THAN_OR_EQUAL;
            case LESS_THAN_OR_EQUAL -> GREATER_THAN;
            case GREATER_THAN -> LESS_THAN_OR_EQUAL;
            case GREATER_THAN_OR_EQUAL -> LESS_THAN;
            default -> throw new IllegalArgumentException("Unknown operator: " + this);
        };
    }

    @Override
    public String toString() {
        return symbol;
    }
}
