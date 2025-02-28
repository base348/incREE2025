package incREE.evidence;

import java.util.BitSet;

public class PredicateBitmap {
    protected BitSet bitset;

    public PredicateBitmap(BitSet bitset) {
        this.bitset = (BitSet) bitset.clone();
    }

    public PredicateBitmap() {
        bitset = new BitSet();
    }

    public boolean contains(Predicate<?> predicate) {
        return bitset.get(predicate.identifier);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bitset == null) ? 0 : bitset.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PredicateBitmap other = (PredicateBitmap) obj;
        if (bitset == null) {
            return other.bitset == null;
        } else return bitset.equals(other.bitset);
    }

    public boolean disjoint(PredicateBitmap other) {
        BitSet copy = (BitSet) other.bitset.clone();
        copy.and(bitset);
        return !copy.isEmpty();
    }

    public PredicateBitmap copy() {
        return new PredicateBitmap(bitset);
    }

    public void xor(PredicateBitmap other) {
        bitset.xor(other.bitset);
    }

    public void set(int index) {
        bitset.set(index);
    }
    
}
