package incREE.cover;

import incREE.evidence.PredicateBitmap;

public class Cover {
    public PredicateBitmap containing;
    public PredicateBitmap forwards;
    public long uncovered;

    public Cover(PredicateBitmap containing, PredicateBitmap forwards, long uncovered) {
        this.containing = containing;
        this.forwards = forwards;
        this.uncovered = uncovered;
    }
}
