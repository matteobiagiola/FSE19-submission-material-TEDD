package org.mb.tedd.statement;

import org.apache.log4j.Logger;
import org.mb.tedd.utils.EditDistance;
import org.mb.tedd.utils.Properties;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LiteralParameter implements ActionParameter {

    private String value;
    private boolean isRegex = false;
    private final static Logger logger = Logger.getLogger(LiteralParameter.class.getName());
    private Matcher matcher;
    private Pattern pattern;

    public LiteralParameter(String value){
        this.value = value;
    }

    public LiteralParameter(String value, boolean isRegex){
        this(value);
        this.isRegex = isRegex;
        if(this.isRegex){
            pattern = Pattern.compile("(\\w+){2}");
        }
    }

    public String getValue() {
        if(this.isRegex){
            this.matcher = this.pattern.matcher(this.value);
            StringBuilder builder = new StringBuilder();
            while(this.matcher.find()){
                builder.append(this.matcher.group());
                builder.append(" ");
            }
            if(!builder.toString().isEmpty()){
                // remove the last space
                return builder.toString().substring(0, builder.toString().length() - 1);
            }
            return this.value;
        } else {
            return this.value;
        }
    }

    @Override
    public String toString(){
        return this.getValue();
    }

    @Override
    public boolean isLiteral() {
        return true;
    }

    @Override
    public boolean isAction() {
        return false;
    }

    @Override
    public boolean equals(Object other){

        if(other == this) return true;

        if(other instanceof LiteralParameter){
            LiteralParameter otherLiteralParameter = (LiteralParameter) other;
            if(this.isRegex && !otherLiteralParameter.isRegex){
                return this.value.replaceAll("\"", "")
                        .contains(otherLiteralParameter.value.replaceAll("\"", ""));
            } else if(otherLiteralParameter.isRegex && !this.isRegex){
                return otherLiteralParameter.value.replaceAll("\"", "")
                        .contains(this.value.replaceAll("\"", ""));
            }

            if(Properties.EDIT_DISTANCE_STRING_ANALYSIS){
                if(!Objects.equals(this.value, otherLiteralParameter.value) && !this.isRegex
                        && !otherLiteralParameter.isRegex){
                    int editDistance = EditDistance.calculate(this.value, otherLiteralParameter.value);
                    if(editDistance == 1){
                        // edit, negative (type wrong values) operations
                        logger.debug("Edit distance = 1. This value: " + this.value
                                + "; other value: " + otherLiteralParameter.value);
                        return true;
                    }
                    return false;
                }
            }

            return Objects.equals(this.value, otherLiteralParameter.value);
        }
        return false;
    }

    @Override
    public int hashCode(){
        return Objects.hash(this.value);
    }


    public boolean contains(LiteralParameter otherLiteralParameter){
        if(this.equals(otherLiteralParameter))
            return true;

        for (String separator: Properties.knownWordsSeparators){
            List<String> words = Arrays.asList(this.value.split(separator));
            for(String word: words){
                if(word.replaceAll("\"","")
                        .equals(otherLiteralParameter.value.replaceAll("\"",""))){
                    return true;
                }
            }
        }
        return false;
    }
}
