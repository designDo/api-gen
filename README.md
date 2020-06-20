# api-gen
基于retrofit-dart RestAPI开发的 IDEA 插件，用来生成代码

**由于dart没有反射机制，不能像原生一样实现Retrofit那样简单，而是要借助一些工具提高开发效率。**
**Retrofit官方提供了一种方式，[Retrofit](https://pub.dev/packages/retrofit)**
**[retrofit-dart](https://github.com/trevorwang/retrofit.dart) 代码生成工具。**
**他是基于source-gen等dart packages 用来生成代码的工具，有些不足，所以想办法搞一个AS插件**

# IDEA插件
![img](https://github.com/designDo/api-gen/blob/master/gif/Untitled.gif)

接口方法
```
@POST("/users/get")
Future<UserModel> getUser(@Field('id') String userId);
```
生成的代码
```
@override
  Future<UserModel> getUser(String userId) async {
    final _headers = <String, dynamic>{};
    final queryParameters = <String, dynamic>{};
    final _data = <String, dynamic>{r'id': userId};
    //DioManager().requestDio 可修改源码替换为自己项目封装的请求管理类
    final Response<Map<String, dynamic>> _result = await DioManager()
        .requestDio('/users/get',
            queryParameters: queryParameters,
            options: RequestOptions(method: 'POST', headers: _headers),
            data: _data,
            defaultValue: {});
    final value = UserModel.fromJson(_result.data);
    return value;
  }
```
使用方式
```
MyService().getUser("user_id").asStream().listen((user) {
    print(user.city);
  },onError: (e) {
    print(e);
  });
```

#注意

1. 这里对Retrofit -->http.dart 里面的注解方法做了一些阉割。
去掉了baseurl的定义，将Headers类替换为HEADERS类，原因是和dio导包冲突。
2. 将抽象接口类和实现类写在一个文件中，上下关系，不然无法将生成的代码放在实现类中。
3. AndroidStudio bug ,将Flutter项目下的.idea文件夹删除，重新打开项目，在flutter环境中AndroidStudio
**Generate** --> **implement methods** 将不可用
4. **要么适应生成的代码，要么修改生成的代码。**


#注解类 直接Copy到项目中即可
```

class HttpMethod {
  static const String GET = "GET";
  static const String POST = "POST";
  static const String PATCH = "PATCH";
  static const String PUT = "PUT";
  static const String DELETE = "DELETE";
  static const String HEAD = "HEAD";
  static const String OPTIONS = "OPTIONS";
}
class Method {
  final String method;
  final String path;
  const Method(
      this.method,
      this.path);


}

/// Make a `GET` request
///
/// ```
/// @GET("ip")
/// Future<String> ip(@Query('query1') String query)
/// ```
class GET extends Method {
  const GET(String path, {bool autoCastResponse = true})
      : super(HttpMethod.GET, path);
}

/// Make a `POST` request
class POST extends Method {
  const POST(String path)
      : super(HttpMethod.POST, path);
}

/// Make a `PATCH` request
class PATCH extends Method {
  const PATCH(final String path)
      : super(HttpMethod.PATCH, path);
}

/// Make a `PUT` request
class PUT extends Method {
  const PUT(final String path)
      : super(HttpMethod.PUT, path);
}

/// Make a `DELETE` request
class DELETE extends Method {
  const DELETE(final String path)
      : super(HttpMethod.DELETE, path);
}


/// Adds headers specified in the [value] map.
class HEADERS {
  final Map<String, dynamic> value;
  const HEADERS([this.value]);
}

/// Replaces the header with the value of its target.
///
/// Header parameters may be `null` which will omit them from the request.
class Header {
  final String value;
  const Header(this.value);
}

/// Use this annotation on a service method param when you want to directly control the request body
/// of a POST/PUT request (instead of sending in as request parameters or form-style request
/// body).
///
/// Body parameters may not be `null`.
class Body {
  const Body();
}

/// Named pair for a form request.
///
/// ```
/// @POST("/post")
/// Future<String> example(
///   @Field() int foo,
///   @Field("bar") String barbar},
/// )
/// ```
/// Calling with `foo.example("Bob Smith", "President")` yields a request body of
/// `foo=Bob+Smith&bar=President`.
class Field {
  final String value;
  const Field([this.value]);
}

/// Named replacement in a URL path segment.
///
/// Path parameters may not be `null`.
class Path {
  final String value;
  const Path([this.value]);
}

/// Query parameter appended to the URL.
///
/// Simple Example:
///
///```
/// @GET("/get")
/// Future<String> foo(@Query('bar') String query)
///```
/// Calling with `foo.friends(1)` yields `/get?bar=1`.
class Query {
  final String value;
  final bool encoded;
  const Query(this.value, {this.encoded = false});
}

/// Query parameter keys and values appended to the URL.
///
/// A `null` value for the map, as a key, or as a value is not allowed.
class Queries {
  final bool encoded;
  const Queries({this.encoded = false});
}


class Part {
  @Deprecated('future release')
  final String value;
  final String name;

  /// If this field is a file, optionally specify it's name. otherwise the name
  /// will be derived from the actual file.
  final String fileName;

  // To identify the content type of a file
  final String contentType;
  const Part({this.value, this.name, this.fileName, this.contentType});
}
```