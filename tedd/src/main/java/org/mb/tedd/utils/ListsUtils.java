package org.mb.tedd.utils;


import java.util.ArrayList;
import java.util.List;

public class ListsUtils {

    /**
     * @return computes the difference between a and b = a - b
     * */
    public static <T> List<T> listsDifference(List<T> a, List<T> b){
        List<T> result = new ArrayList<>(a);
        result.removeAll(b);
        return result;
    }
}
