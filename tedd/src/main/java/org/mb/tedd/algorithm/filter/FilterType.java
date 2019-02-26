package org.mb.tedd.algorithm.filter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FilterType {

    public enum Type {
        NLP ("nlp"),
        COMMON_VALUES ("common_values");

        private String type;

        Type(String type){
            this.type = type;
        }

        public String getTypeName(){
            return this.type;
        }
    }

    public static boolean isFilterType(String type){
        return Arrays.stream(Type.values())
                .map(Type::getTypeName)
                .collect(Collectors.toList())
                .contains(type);
    }

    public static List<String> getValues(){
        return Arrays.stream(Type.values())
                .map(Type::getTypeName)
                .collect(Collectors.toList());
    }
}
