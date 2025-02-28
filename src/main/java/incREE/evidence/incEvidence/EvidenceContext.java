package incREE.evidence.incEvidence;

import incREE.evidence.PredicateBitmap;

import java.util.Set;

public class EvidenceContext {
    public final int tidLeft;
    public final Set<Integer> tidRight;
    public final PredicateBitmap evidence;


    public EvidenceContext(int tidLeft, Set<Integer> tidRight, PredicateBitmap evidence) {
        this.tidLeft = tidLeft;
        this.tidRight = tidRight;
        this.evidence = evidence;
    }

    // Can this really improve efficiency ?
    public EvidenceContext split(Set<Integer> tid, PredicateBitmap fix) {
        this.tidRight.removeAll(tid);
        PredicateBitmap newEvidence = evidence.copy();
        newEvidence.xor(fix);
        return new EvidenceContext(tidLeft, tid, newEvidence);
    }
}
