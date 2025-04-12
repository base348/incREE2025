package incREE.evidence;

import ch.javasoft.bitset.BitSetFactory;
import ch.javasoft.bitset.IBitSet;
import ch.javasoft.bitset.LongBitSet.LongBitSetFactory;

import java.util.BitSet;

public class PredicateBitmap {

    private static BitSetFactory bf = new LongBitSetFactory();
    protected IBitSet bitset;

    public PredicateBitmap(IBitSet bitset) {
        this.bitset = bitset.clone();
    }

    public PredicateBitmap() {
        bitset = bf.create();
    }

    public IBitSet getBitSet() {
        return bitset;
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

    @Override
    public String toString() {
        String bitsetString = bitset.toString();
        return bitsetString.substring(1, bitsetString.length() - 1);
    }

    public boolean disjoint(PredicateBitmap other) {
        return bitset.getAndCardinality(other.bitset) > 0;
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

    public void set(int index, boolean value) {
        bitset.set(index, value);
    }

    public void set(int start, int end) {
        for (int i = start; i < end; i++) {
            bitset.set(i);
        }
    }

    public boolean get(int index) {
        return bitset.get(index);
    }

    public void andNot(PredicateBitmap other) {
        bitset.andNot(other.bitset);
    }

    public void or(PredicateBitmap other) {
        bitset.or(other.bitset);
    }

    public boolean isSubsetOf(PredicateBitmap other) {
        return bitset.isSubSetOf(other.bitset);
    }

    public boolean isEmpty() {
        return bitset.isEmpty();
    }

    public int size() {
        return bitset.cardinality();
    }

    public int nextSetBit(int n) {
        return bitset.nextSetBit(n);
    }
}
