package org.mb.tedd.graph.dot;

import org.mb.tedd.algorithm.extraction.string.StringConstantValue;
import org.mb.tedd.algorithm.extraction.string.StringValue;
import org.mb.tedd.statement.LiteralParameter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EdgeLabel {

    public static final String import_separator = "-";
    public static final String manifest_dependency_color = "blue";
    public static final String uninteresting_dependency_color = "red";

    public static String buildLabel(StringValue dependentValue){
        return dependentValue.getValues().stream()
                .map(value -> value.replaceAll("\"", ""))
                .collect(Collectors.joining(" \\n "));
    }

    public static List<StringValue> getValues(String edgeLabel){
        String[] values = edgeLabel.split(import_separator);
        return Arrays.stream(values)
                .map(value -> new StringConstantValue(new LiteralParameter(value))).collect(Collectors.toList());
    }
}
