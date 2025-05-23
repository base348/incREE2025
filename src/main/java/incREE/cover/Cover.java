package incREE.cover;

import incREE.evidence.AbstractPredicate;
import incREE.evidence.AbstractPredicateGroup;
import incREE.evidence.PredicateBitmap;
import incREE.evidence.PredicateGroup;

import java.util.List;

public class Cover {
    public PredicateBitmap containing;
    public int uncovered;
//    public PredicateBitmap forwards;

    public Cover(PredicateBitmap containing, long uncovered) {
        this.containing = containing;
        this.uncovered = (int) uncovered;
//        this.forwards = forwards;
    }

    public Cover(PredicateBitmap containing, int uncovered) {
        this.containing = containing;
        this.uncovered = uncovered;
//        this.forwards = forwards;
    }

    public boolean equals(Cover other) {
        return this.containing.equals(other.containing) && this.uncovered == other.uncovered;
    }
}
