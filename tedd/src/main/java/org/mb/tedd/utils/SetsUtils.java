package org.mb.tedd.utils;

import java.util.LinkedHashSet;
import java.util.Set;

public class SetsUtils {

    /**
     * @return symmetric difference between a and b = union(a,b) - intersection(a,b)
     * */
    public static <T> Set<T> symmetricDifference(Set<T> a, Set<T> b){
        return setsDifference(union(a,b), intersection(a,b));
    }

    public static <T> boolean equals(Set<T> a, Set<T> b){
        if(a.size() != b.size()){
            return false;
        }
        for (T itemA : a) {
            if(!b.contains(itemA)){
                return false;
            }
        }
        return true;
    }

    /**
     * @return computes the difference between a and b = a - b
     * */
    public static <T> Set<T> setsDifference(Set<T> a, Set<T> b){
        Set<T> result = new LinkedHashSet<>(a);
        result.removeAll(b);
        return result;
    }

    /**
     * @return intersection between a and b
     * */
    public static <T> Set<T> intersection(Set<T> a, Set<T> b){
        Set<T> result = new LinkedHashSet<>(a);
        result.retainAll(b);
        return result;
    }

    /**
     * @return union between a and b
     * */
    public static <T> Set<T> union(Set<T> a, Set<T> b){
        Set<T> result = new LinkedHashSet<>(a);
        result.addAll(b);
        return result;
    }
}
