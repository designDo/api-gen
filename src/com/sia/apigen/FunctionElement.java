package com.sia.apigen;

public class FunctionElement {
    public static final String PATH = "PATH";
    public static final String POST = "POST";
    public static final String GET = "GET";
    public static final String PATCH = "PATCH";
    public static final String PUT = "PUT";
    public static final String DELETE = "DELETE";
    public static final String HEADERS = "HEADERS";

    // @ POST('/tasks' )
    //POST
    public String annotation = "";
    // /tasks
    public String annotationValue = "";
}
