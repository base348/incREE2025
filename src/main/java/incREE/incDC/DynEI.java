package incREE.incDC;

import incREE.evidence.Predicate;
import incREE.evidence.PredicateBitmap;

import java.util.*;

public class DynEI {
    private final Map<PredicateBitmap, Integer> er;
    private final Map<PredicateBitmap, Integer> erInc;
    private final List<Predicate<?>> predicateSpace;
    private final List<PredicateBitmap> DCCurrent;

    private static List<PredicateBitmap> toBitmap(List<List<Predicate<?>>> dcs) {
        List<PredicateBitmap> bitmaps = new ArrayList<PredicateBitmap>();
        for (List<Predicate<?>> dc : dcs) {
            PredicateBitmap bits = new PredicateBitmap();
            for (Predicate<?> p : dc) {
                bits.set(p.identifier);
            }
            bitmaps.add(bits);
        }
        return bitmaps;
    }

    public DynEI(Map<PredicateBitmap, Integer> er, Map<PredicateBitmap, Integer> erInc, List<Predicate<?>> predicateSpace, List<List<Predicate<?>>> DCCurrent) {
        this.er = er;
        this.erInc = erInc;
        this.predicateSpace = predicateSpace;
        this.DCCurrent = toBitmap(DCCurrent);
    }

    private static boolean isImplied(PredicateBitmap dc, List<PredicateBitmap> cover) {
        for (PredicateBitmap pb : cover) {
            if (pb.isSubsetOf(dc)) {
                return true;
            }
        }
        return false;
    }

    public List<PredicateBitmap> DynDC() {
        Set<PredicateBitmap> incEvidenceSet = new HashSet<>(erInc.keySet());
        incEvidenceSet.removeAll(er.keySet());

        for (PredicateBitmap e : incEvidenceSet) {
            List<PredicateBitmap> DCInv = new ArrayList<>();
            for (PredicateBitmap dc : DCCurrent) {
                if (dc.isSubsetOf(e)) {
                    DCInv.add(dc);
                }
            }
            // dc.isSubset(e) is more efficient?
            DCCurrent.removeAll(DCInv);

            int count = 0;
            for (PredicateBitmap dc : DCInv) {
                count = 0;
                for (Predicate<?> p : predicateSpace) {
                    if (!e.get(p.identifier)){
                        PredicateBitmap expandDC = dc.copy();
                        expandDC.set(p.identifier);
                        if (!isImplied(expandDC, DCCurrent)) {
                            DCCurrent.add(expandDC);
                            count++;
                        }
                    }
                }
                System.out.println(count + " new DC found.");
            }
        }
        return DCCurrent;
    }
}
