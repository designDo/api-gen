package com.sia.apigen;

import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.lang.dart.psi.*;
import com.jetbrains.lang.dart.util.DartClassResolveResult;
import com.jetbrains.lang.dart.util.DartGenericSpecialization;
import com.jetbrains.lang.dart.util.DartPresentableUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RetrofitActionFix {
    private final Project project;
    private final Editor editor;
    private final DartMethodDeclaration method;
    protected final DartGenericSpecialization specializations;
    protected final int offset;

    /**
     * 方法参数中解析出来的path参数，之后用在 生成 请求 path上
     */
    private final List<String> pathNames = new ArrayList<>();

    public RetrofitActionFix(Project project, Editor editor, DartMethodDeclaration method, DartClass dartClass, int offset) {
        this.project = project;
        this.editor = editor;
        this.method = method;
        specializations = DartClassResolveResult.create(dartClass).getSpecialization();
        this.offset = offset;
    }

    public void process() {
        if (project == null) {
            return;
        }
        TemplateManager templateManager = TemplateManager.getInstance(project);
        Template template = buildFunctionsText(templateManager, method);
        if (template != null) {
            //修改指针位置
            editor.getCaretModel().moveToOffset(offset == 0 ? method.getTextRange().getEndOffset() : offset);
            templateManager.startTemplate(editor, template);
        }
    }


    @Nullable
    protected Template buildFunctionsText(TemplateManager templateManager, @NotNull DartComponent element) {
        Template template = templateManager.createTemplate(this.getClass().getName(), "Dart");
        template.setToReformat(true);
        template.addTextSegment("@override\n");
        // Future<User> returnType
        DartReturnType returnType = PsiTreeUtil.getChildOfType(element, DartReturnType.class);
        DartType dartType = PsiTreeUtil.getChildOfType(element, DartType.class);
        String returnString = "";
        if (returnType != null) {
            returnString = DartPresentableUtil.buildTypeText(element, returnType, this.specializations);
            template.addTextSegment(returnString);
            template.addTextSegment(" ");
        } else if (dartType != null) {
            returnString = DartPresentableUtil.buildTypeText(element, dartType, this.specializations);
            template.addTextSegment(returnString);
            template.addTextSegment(" ");
        }
        //方法名
        template.addTextSegment(element.getName() == null ? "fun" : element.getName());
        // ParameterList
        template.addTextSegment("(");
        String paramsString = DartPresentableUtil.getPresentableParameterList(element, this.specializations, false, true, true);
        template.addTextSegment(paramsString);
        template.addTextSegment(")");
        template.addTextSegment(" async");
        template.addTextSegment("{\n");

        //中间内容
        if (element instanceof DartMethodDeclaration) {
            DartMethodDeclaration method = (DartMethodDeclaration) element;

            // 参数生成
            buildArguments(method, template);

            List<FunctionElement> functionElements = getRequestMethod(method);

            List<FunctionElement> httpMethodElements = new ArrayList<>();
            List<FunctionElement> headersElements = new ArrayList<>();
            for (FunctionElement functionElement : functionElements) {
                if (functionElement.annotation.equals(FunctionElement.HEADERS)) {
                    headersElements.add(functionElement);
                } else {
                    httpMethodElements.add(functionElement);
                }
            }

            //获取PATH参数
            FunctionElement httpMethod = new FunctionElement();
            FunctionElement pathMethod = new FunctionElement();
            for (FunctionElement httpMethodElement : httpMethodElements) {
                if (!httpMethodElement.annotation.equals(FunctionElement.PATH)) {
                    //其他类型注解
                    httpMethod = httpMethodElement;
                } else {
                    //获取到PATH注解
                    pathMethod = httpMethodElement;
                }
            }

            String baseUrl = pathMethod.annotationValue;
            String path = httpMethod.annotationValue;
            if (pathNames.size() > 0) {
                for (String pathName : pathNames) {
                    path = path.replace("{" + pathName + "}", "$" + pathName);
                }
            }
            if (!headersElements.isEmpty()) {
                for (FunctionElement headersElement : headersElements) {
                    template.addTextSegment("_headers.addAll(" + headersElement.annotationValue + ");\n");
                }
            }

            //添加baseUrl
            if (baseUrl.length() > 0) {
                template.addTextSegment("final baseUrl = '" + baseUrl + "';\n");
            }
            template.addTextSegment(getResponseType(returnString));
            template.addTextSegment(" _result = await DioManager().requestDio(");
            template.addTextSegment("'" + path + "', \n");
            if (baseUrl.length() > 0) {
                template.addTextSegment("baseUrl: baseUrl,\n");
            }
            template.addTextSegment("queryParameters: queryParameters,\n options: RequestOptions(");
            template.addTextSegment("method: '" + httpMethod.annotation + "',");
            template.addTextSegment("headers: _headers");
            template.addTextSegment("),\n");
            template.addTextSegment(" data: _data,\n");
            template.addTextSegment(" defaultValue: " + getDefaultResponseValue(returnString) + ");\n");
            template.addTextSegment(getReturnValue(returnString));

        }

        template.addTextSegment("\n} ");

        return template;
    }


    /**
     * 生成 _queryParams  _headers _data
     *
     * @param element  方法
     * @param template template
     */
    private void buildArguments(DartMethodDeclaration element, Template template) {
        DartFormalParameterList parameterList = PsiTreeUtil.getChildOfType(element, DartFormalParameterList.class);
        if (parameterList == null) {
            return;
        }
        List<DartNormalFormalParameter> list = parameterList.getNormalFormalParameterList();
        if (list.size() > 0) {
            //拿到所有参数
            List<ArgumentElement> argumentElements = new ArrayList<>();
            for (DartNormalFormalParameter parameter : list) {
                argumentElements.add(getArgumentElement(parameter.getText()));
            }

            //get Query
            List<ArgumentElement> queryElements = new ArrayList<>();
            List<ArgumentElement> queriesElements = new ArrayList<>();
            List<ArgumentElement> partElements = new ArrayList<>();
            List<ArgumentElement> fieldElements = new ArrayList<>();
            List<ArgumentElement> bodyElements = new ArrayList<>();
            List<ArgumentElement> pathElements = new ArrayList<>();
            List<ArgumentElement> headElements = new ArrayList<>();

            for (ArgumentElement argumentElement : argumentElements) {
                switch (argumentElement.annotation) {
                    case ArgumentElement.QUERY:
                        queryElements.add(argumentElement);
                        break;
                    case ArgumentElement.QUERIES:
                        queriesElements.add(argumentElement);
                        break;
                    case ArgumentElement.PART:
                        partElements.add(argumentElement);
                        break;
                    case ArgumentElement.FIELD:
                        fieldElements.add(argumentElement);
                        break;
                    case ArgumentElement.BODY:
                        bodyElements.add(argumentElement);
                        break;
                    case ArgumentElement.PATH:
                        pathElements.add(argumentElement);
                        break;
                    case ArgumentElement.HEADER:
                        headElements.add(argumentElement);
                        break;
                }
            }

            //找 Path
            if (!pathElements.isEmpty()) {
                for (ArgumentElement pathElement : pathElements) {
                    pathNames.add(pathElement.argument);
                }
            }

            //Header
            if (headElements.isEmpty()) {
                template.addTextSegment("final _headers = <String, dynamic>{};");
            } else {
                template.addTextSegment("final _headers = <String, dynamic>{\n");
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < headElements.size(); i++) {
                    ArgumentElement headerElement = headElements.get(i);
                    builder.append("r'").append(headerElement.annotationValue).append("': ").append(headerElement.argument);
                    if (i < headElements.size() - 1) {
                        builder.append(",\n");
                    }
                }
                template.addTextSegment(builder.toString());
                template.addTextSegment("\n};");
            }

            //找 Query
            if (queryElements.isEmpty()) {
                template.addTextSegment("final queryParameters = <String, dynamic>{};");
            } else {
                template.addTextSegment("final queryParameters = <String, dynamic>{\n");
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < queryElements.size(); i++) {
                    ArgumentElement queryElement = queryElements.get(i);
                    builder.append("r'").append(queryElement.annotationValue).append("': ").append(queryElement.argument);
                    if (i < queryElements.size() - 1) {
                        builder.append(",\n");
                    }
                }
                template.addTextSegment(builder.toString());
                template.addTextSegment("\n};");
            }
            //找 Queries
            if (!queriesElements.isEmpty()) {
                for (ArgumentElement queriesElement : queriesElements) {
                    template.addTextSegment("queryParameters.addAll(" + queriesElement.argument + " ?? <String, dynamic>{});");
                }
            }

            //文件 返回
            if (!partElements.isEmpty()) {
                template.addTextSegment("final _data = FormData();\n");
                for (ArgumentElement partElement : partElements) {
                    template.addTextSegment("_data.files.add(MapEntry(\n" +
                            "'file',\n" +
                            "MultipartFile.fromFileSync(" + partElement.argument + ".path,\n" +
                            "filename: " + partElement.argument + ".path.split(Platform.pathSeparator).last)));");
                }
                return;
            }

            //Field
            if (fieldElements.isEmpty()) {
                template.addTextSegment("final _data = <String, dynamic>{};\n");
            } else {
                template.addTextSegment("final _data = <String, dynamic>{\n");
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < fieldElements.size(); i++) {
                    ArgumentElement fieldElement = fieldElements.get(i);

                    builder.append("r'").append(fieldElement.annotationValue).append("': ").append(fieldElement.argument);
                    if (i < fieldElements.size() - 1) {
                        builder.append(",\n");
                    }
                }
                template.addTextSegment(builder.toString());
                template.addTextSegment("\n};");
            }

            //Body Map 直接添加 其他实体类 需要 toJson
            if (!bodyElements.isEmpty()) {
                for (ArgumentElement bodyElement : bodyElements) {
                    template.addTextSegment("_data.addAll(" + bodyElement.argument
                            + (bodyElement.type.startsWith("Map") ? "" : "?.toJson()") + " ?? <String, dynamic>{});");
                }
            }


        } else {
            template.addTextSegment("final _headers = <String, dynamic>{};");
            template.addTextSegment("final queryParameters = <String, dynamic>{};");
            template.addTextSegment(" final _data = <String, dynamic>{};");
        }
    }

    /**
     * 获取方法上修饰的注解
     *
     * @param method 方法
     * @return @GET('/tasks') /@POST('/tasks') eg.. and @HEADERS
     */
    private List<FunctionElement> getRequestMethod(DartMethodDeclaration method) {
        List<FunctionElement> functionElements = new ArrayList<>();
        for (PsiElement child : method.getChildren()) {
            FunctionElement element = getFunctionElements(child.getText());
            if (element != null) {
                functionElements.add(element);
            }
        }
        return functionElements;
    }


    private boolean isBasicReturnType(String type) {
        switch (type) {
            case "String":
            case "int":
            case "bool":
            case "double":
            case "Float":
            case "Object":
            case "Double":
                return true;
        }
        return false;
    }

    private String getBasicReturnType(String type) {
        switch (type) {
            case "String":
                return "\"\"";
            case "int":
                return "0";
            case "bool":
                return "false";
            case "double":
                return "0.0";
            case "Float":
                return "Float()";
            case "Object":
                return "Object()";
            case "Double":
                return "Double()";
        }
        return "";
    }


    /**
     * @param type Future<User>  Future<List<User>> Future<int> Future<List<int>>
     * @return User List<User>  int  List<int>
     */
    private String getReturnType(String type) {
        return type.substring(type.indexOf("<") + 1, type.lastIndexOf(">"));
    }

    private String getResponseType(String type) {
        String realType = getReturnType(type);
        if (realType.startsWith("List")) {
            String childType = getReturnType(realType);
            if (isBasicReturnType(childType)) {
                return "final Response<List<" + childType + ">>";
            } else {
                return "final Response<List<dynamic>>";
            }
        } else if (isBasicReturnType(realType)) {
            return "final Response<" + realType + ">";
        } else {
            return "final Response<Map<String, dynamic>>";
        }
    }

    private String getDefaultResponseValue(String type) {
        String realType = getReturnType(type);
        if (realType.startsWith("List")) {
            return "[]";
        } else if (isBasicReturnType(realType)) {
            return getBasicReturnType(realType);
        } else {
            return "{}";
        }
    }

    private String getReturnValue(String type) {
        String realType = getReturnType(type);
        if (realType.startsWith("List")) {
            String childType = getReturnType(realType);
            if (isBasicReturnType(childType)) {
                return "final value = _result.data;\n return value;";
            } else {
                return "var value = _result.data\n" +
                        "        .map((dynamic i) => " + childType + ".fromJson(i as Map<String, dynamic>))\n" +
                        "        .toList();\n" +
                        "    return value;";
            }
        } else if (isBasicReturnType(realType)) {
            return "final value = _result.data;\n" +
                    "    return value;";
        } else {
            return "final value = " + realType + ".fromJson(_result.data);\n" +
                    "    return value;";
        }
    }

    /**
     * 方法参数解析
     *
     * @param realS @Field('name') String name
     * @return ArgumentElement（'Field', 'name', 'String' , 'name'）
     */
    private ArgumentElement getArgumentElement(String realS) {
        ArgumentElement element = new ArgumentElement();

        realS = realS.trim();
        element.argument = realS.substring(realS.lastIndexOf(" ") + 1);

        String first = realS.substring(0, realS.lastIndexOf(" ")).replaceAll(" ", "");

        element.annotation = first.substring(first.indexOf("@") + 1, first.indexOf("("));

        if (first.indexOf(")") - first.indexOf("(") == 1) {
            //如果（）中没有值，默认给参数名
            element.annotationValue = element.argument;
        } else {

            element.annotationValue = first.substring(first.indexOf("(") + 2, first.indexOf(")") - 1);
        }

        element.type = first.substring(first.indexOf(")") + 1);
        return element;
    }

    /**
     * 方法注解解析
     *
     * @param realS @POST('/tasks')
     * @return FunctionElement(' POST ', ' / tasks ')
     */
    private FunctionElement getFunctionElements(String realS) {
        FunctionElement element = new FunctionElement();

        realS = realS.trim().replaceAll(" ", "");

        if (!realS.startsWith("@")) {
            return null;
        }
        element.annotation = realS.substring(realS.indexOf("@") + 1, realS.indexOf("("));

        if (realS.indexOf(")") - realS.indexOf("(") == 1) {
            System.out.println("null annotationValue");
        } else {
            if (realS.startsWith("@HEADERS")) {
                element.annotationValue = realS.substring(realS.indexOf("(") + 1, realS.indexOf(")"));
            } else {
                element.annotationValue = realS.substring(realS.indexOf("(") + 2, realS.indexOf(")") - 1);
            }
        }
        return element;
    }
}


