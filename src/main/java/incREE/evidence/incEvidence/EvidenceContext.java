package incREE.evidence.incEvidence;

import incREE.evidence.PredicateBitmap;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public record EvidenceContext(int tidLeft, BitSet tidRight, PredicateBitmap evidence) {
    public static final EvidenceContext EMPTY = new EvidenceContext(-1, new BitSet(), null);

    public boolean isEmpty() {
        return tidRight.isEmpty();
    }

    // Can this really improve efficiency ?

    /**
     * Delete some TuplePairs from tidRight and return a new EvidenceContext containing these
     *
     * @param tid Set of TuplePairs that inconsistent with current evidence. Unnecessary to be a subset of tidRight
     * @param fix Predicate fix set from PredicateGroup.getFixSet
     * @return New EvidenceContext with fixed evidence
     */
    public EvidenceContext split(BitSet tid, PredicateBitmap fix) {
        if (tid.isEmpty()) {
            return EMPTY;
        }
        BitSet newRight = (BitSet) tidRight.clone();
        newRight.and(tid);
//        Iterator<Integer> iterator = tidRight.iterator(); // assume tidRight is smaller
//        while (iterator.hasNext()) {
//            Integer i = iterator.next();
//            if (tid.contains(i)) {
//                iterator.remove();
//                newRight.add(i);
//            }
//        }
        if (newRight.isEmpty()) return EMPTY;
        tidRight.andNot(tid);
        PredicateBitmap newEvidence = evidence.copy();
        newEvidence.xor(fix);
        return new EvidenceContext(tidLeft, newRight, newEvidence);
    }

//    public EvidenceContext symmetryCopy(PredicateBitmap fix) {
//        PredicateBitmap newEvidence = evidence.copy();
//        newEvidence.xor(fix);
//        return new EvidenceContext(tidLeft, new TreeSet<>(tidRight), newEvidence);
//    }
}
