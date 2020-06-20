package com.sia.apigen;

public class ArgumentElement {

    public static final String QUERY = "Query";
    public static final String QUERIES = "Queries";
    public static final String FIELD = "Field";
    public static final String BODY = "Body";
    public static final String PATH = "Path";
    public static final String PART = "Part";
    public static final String HEADER ="Header";

    public ArgumentElement() {

    }

    // @ Field( )   String a
    //Field
    public String annotation;
    //name
    public String annotationValue;
    //String
    public String type;
    //a
    public String argument;
}
