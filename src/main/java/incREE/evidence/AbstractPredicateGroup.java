package incREE.evidence;

import java.util.ArrayList;
import java.util.List;

public class AbstractPredicateGroup extends PredicateGroup  {
    public final String attribute1;
    public final String attribute2;
    public final List<AbstractPredicate> allPredicates;

    AbstractPredicateGroup reversed = null;
    public final PredicateBitmap coverCandidates = new PredicateBitmap();

    public AbstractPredicateGroup(PredicateGroup.Type type, List<AbstractPredicate> allPredicates, int offset, String attribute1, String attribute2) {
        this.type = type;
        this.attribute1 = attribute1;
        this.attribute2 = attribute2;
        this.offset = offset;
        this.allPredicates = allPredicates;
        this.isReflexive = (attribute1.equals(attribute2));

        if (type.equals(PredicateGroup.Type.NUMERIC)) {
            this.length = 6;
            allPredicates.add(AbstractPredicate.build(attribute1, Operator.EQUAL, attribute2));
            allPredicates.add(AbstractPredicate.build(attribute1, Operator.NOT_EQUAL, attribute2));
            allPredicates.add(AbstractPredicate.build(attribute1, Operator.GREATER_THAN, attribute2));
            allPredicates.add(AbstractPredicate.build(attribute1, Operator.LESS_THAN, attribute2));
            allPredicates.add(AbstractPredicate.build(attribute1, Operator.GREATER_THAN_OR_EQUAL, attribute2));
            allPredicates.add(AbstractPredicate.build(attribute1, Operator.LESS_THAN_OR_EQUAL, attribute2));
        } else {
            this.length = 2;
            allPredicates.add(AbstractPredicate.build(attribute1, Operator.EQUAL, attribute2));
            allPredicates.add(AbstractPredicate.build(attribute1, Operator.NOT_EQUAL, attribute2));
        }
        
        init();
    }

    @Override
    void init() {
        super.init();
        this.coverCandidates.set(offset);
        this.coverCandidates.set(offset + 1);
        this.coverCandidates.set(offset + 3);
        this.coverCandidates.set(offset + 5);
    }

    public static AbstractPredicateGroup findGroup(int predicate, List<AbstractPredicateGroup> predicateGroups) {
        //  TODO: use binary search
//        int left = 0;
//        int right = predicateGroups.size();
//        while (left < right) {
//            int mid = left + (right - left) / 2;
//            int contain = predicateGroups.get(mid).contains(predicate);
//            if (contain == -1) {
//                 = mid;
//            } else if (contain == 1) {
//                left = mid + 1;
//            }
//        }
        for (AbstractPredicateGroup predicateGroup : predicateGroups) {
            if (predicateGroup.contains(predicate) == 0) {
                return predicateGroup;
            }
        }
        throw new IllegalArgumentException("No predicate group found");
    }

    @Override
    public AbstractPredicateGroup getReversed() {
        if (this.isReflexive) {
            return this;
        }
        if (this.reversed == null) {
            this.reversed = new AbstractPredicateGroup(this.type, allPredicates, offset+length, attribute2, attribute1);
            this.reversed.isMajor = false;
            this.reversed.reversed = this;
        }
        return this.reversed;
    }

    @Override
    public int getAllPredicatesNum() {
        return allPredicates.size();
    }

    @Override
    public JsonDTO toJsonDTO() {
        return new JsonDTO(attribute1, attribute2, type);
    }

    public static List<AbstractPredicateGroup> fromJsonDTO(List<JsonDTO> jsonDTOs) {
        int offset = 0;
        List<AbstractPredicate> allPredicates = new ArrayList<>();
        List<AbstractPredicateGroup> predicateGroups = new ArrayList<>();
        for (JsonDTO jsonDTO : jsonDTOs) {
            AbstractPredicateGroup newGroup = new AbstractPredicateGroup(jsonDTO.type, allPredicates, offset, jsonDTO.firstColumn,  jsonDTO.secondColumn);
            predicateGroups.add(newGroup);
            int newLength = jsonDTO.type.equals(PredicateGroup.Type.NUMERIC) ? 6 : 2;
            offset += newLength;

            if (!newGroup.isReflexive()) {
                predicateGroups.add(newGroup.getReversed());
                offset += newLength;
            }
        }
        return predicateGroups;
    }
}
