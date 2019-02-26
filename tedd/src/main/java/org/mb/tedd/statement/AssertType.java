package org.mb.tedd.statement;

public enum AssertType {
    TRUE ("True"),
    EQUALS ("Equals"),
    FALSE ("False"),
    THAT ("That");

    private String name;

    AssertType(String name){
        this.name = name;
    }

    public String getValue(){
        return name;
    }
}
