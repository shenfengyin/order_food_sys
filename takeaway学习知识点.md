# 后台员工登录：



## IService

IService的使用需要另外两个接口的配合：`baseMapper`和`ServiceImpl`

```java
public interface EmployeeMapper extends BaseMapper<Employee>{
}
```

```java
public interface EmployeeService extends IService<Employee> {
}
```

```java
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper,Employee> implements EmployeeService{
}
```

## QueryWrapper

最基础的使用方式是这样

？？？？？new QueryWrapper<>();直接就和数据库有关？

```java
// 查询条件构造器
QueryWrapper<BannerItem> wrapper = new QueryWrapper<>();
wrapper.eq("banner_id", id);
// 查询操作
List<BannerItem> bannerItems = bannerItemMapper.selectList(wrapper);
12345
```

然后我们可以引入[lambda](https://so.csdn.net/so/search?q=lambda&spm=1001.2101.3001.7020)，避免我们在代码中写类似的于banner_id的硬编码（避免写错

```java
QueryWrapper<BannerItem> wrapper = new QueryWrapper<>();
wrapper.lambda().eq(BannerItem::getBannerId, id);
List<BannerItem> bannerItems = bannerItemMapper.selectList(wrapper);
123
```

当然，不是所有情况都能使用`::`[方法引用](https://so.csdn.net/so/search?q=方法引用&spm=1001.2101.3001.7020)。

```java
List<User> userList = userMapper.selectList(null);
        Assert.assertEquals(5, userList.size());
        userList.forEach(System.out::println);
```

其使用有一定的条件：lambada表达式的主体仅包含一个表达式，且lambada表达式只调用一个已经存在的方法；被引用的方法的参数列表与lambada表达式的输入输出一致。

![img](takeaway%E5%AD%A6%E4%B9%A0%E7%9F%A5%E8%AF%86%E7%82%B9.assets/4f12376d30264e17aa9f16a42697452d-16850783446453.png)

通过上面这个图，可以很清晰的观察到方法引用使用的条件–》必须进行单纯的引用。像getter和setter方法就是单纯的。

##### LambdaQueryWrapper

为了简化lambda的使用，我们可以改写成LambdaQueryWrapper构造器，语法如下：

```java
LambdaQueryWrapper<BannerItem> wrapper = new QueryWrapper<BannerItem>().lambda();
wrapper.eq(BannerItem::getBannerId, id);
List<BannerItem> bannerItems = bannerItemMapper.selectList(wrapper);
123
```

我们可以再次将QueryWrapper.lambda()简化，变成这个样子

```java
LambdaQueryWrapper<BannerItem> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(BannerItem::getBannerId, id);
List<BannerItem> bannerItems = bannerItemMapper.selectList(wrapper);
```

```
//2、根据页面提交的用户名username查询数据库
LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
queryWrapper.eq(Employee::getUsername,employee.getUsername());
Employee emp = employeeService.getOne(queryWrapper);
```

### queryWrapper是mybatis plus中实现查询的对象封装操作类。

上述代码：1.新建一个querywrapper对象（类型为Employee，即要查找的实体数据） 2.第二行是要Employee对象对应的数据库表中的username列，是否等于employee#name 3.入参为querywrapper（新建好的查询对象封装类

# 改进

1.employee的name不应该是unique索引



2.实现页面也不可以看

```
//定义不需要处理的请求路径
String[] urls = new String[]{
        "/employee/login",
        "/employee/logout",
        "/backend/**",    //页面可以看，看不到动态数据就行
        "/front/**"
};
```



3.修改admin密码

# 员工管理

## 解决能直接访问localhost:8080/backend/index.html的问题

——》

使用**过滤器**或者拦截器实现

```
filterChain.doFilter(request,response);
```

1、新建一个类，实现javax.serlvet.filter接口

  Filter中有三个方法：

  A．void  init(FilterConfig config) //设置filter 的配置对象；

  b．void destory() //销毁filter对象；

  c．void doFilter(ServletRequestreq,ServletResponse res,FilterChain chain) //执行filter 的工作。

  doFilter方法中，过滤器可以对请求和响应做它想做的一切，通过调用他们的方法收集数据，或者给对象添加新的行为。Filter通过调用chain.doFilter()将控制权传送给下一个过滤器，如果过滤器想要终止请求的处理或得到对响应的完全控制，则可以不调用下一个过滤器，而将其重定向至其它一些页面。当链中的最后一个过滤器调用chain.doFilter()方法时，将运行最初请求的Servlet。



























# session与cookie区别

**一、共同之处：**
cookie和[session](https://so.csdn.net/so/search?q=session&spm=1001.2101.3001.7020)都是用来跟踪浏览器用户身份的会话方式。

**二、工作原理：**
**1.Cookie的工作原理**
（1）浏览器端第一次发送请求到服务器端
（2）服务器端创建Cookie，该Cookie中包含用户的信息，然后将该Cookie发送到浏览器端
（3）浏览器端再次访问服务器端时会携带服务器端创建的Cookie
（4）服务器端通过Cookie中携带的数据区分不同的用户
![在这里插入图片描述](C:/Users/Sfy12/Desktop/javaSE/assets/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2NoZW4xMzMzMzMzNjY3Nw==,size_16,color_FFFFFF,t_70.png)
**2.Session的工作原理**
（1）浏览器端第一次发送请求到服务器端，服务器端创建一个Session，同时会创建一个特殊的Cookie（name为JSESSIONID的固定值，value为session对象的ID），然后将该Cookie发送至浏览器端
（2）浏览器端发送第N（N>1）次请求到服务器端,浏览器端访问服务器端时就会携带该name为JSESSIONID的Cookie对象
（3）服务器端根据name为JSESSIONID的Cookie的value(sessionId),去查询Session对象，从而区分不同用户。
name为JSESSIONID的Cookie不存在（**关闭或更换浏览器**），返回1中重新去创建Session与特殊的Cookie
name为JSESSIONID的Cookie存在，根据value中的SessionId去寻找session对象
value为SessionId不存在**（Session对象默认存活30分钟）**，返回1中重新去创建Session与特殊的Cookie
value为SessionId存在，返回session对象
**Session的工作原理图**
![在这里插入图片描述](C:/Users/Sfy12/Desktop/javaSE/assets/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2NoZW4xMzMzMzMzNjY3Nw==,size_16,color_FFFFFF,t_70-16850211719701.png)
![在这里插入图片描述](C:/Users/Sfy12/Desktop/javaSE/assets/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2NoZW4xMzMzMzMzNjY3Nw==,size_16,color_FFFFFF,t_70-16850211719712.png)
**三、区别：**

cookie数据保存在客户端，session数据保存在服务端。

session
简单的说，当你登陆一个网站的时候，如果web服务器端使用的是session，那么所有的数据都保存在服务器上，客户端每次请求服务器的时候会发送当前会话sessionid，服务器根据当前sessionid判断相应的用户数据标志，以确定用户是否登陆或具有某种权限。由于数据是存储在服务器上面，所以你不能伪造。

cookie
sessionid是服务器和客户端连接时候随机分配的，如果浏览器使用的是cookie，那么所有数据都保存在浏览器端，比如你登陆以后，服务器设置了cookie用户名，那么当你再次请求服务器的时候，浏览器会将用户名一块发送给服务器，这些变量有一定的特殊标记。服务器会解释为cookie变量，所以只要不关闭浏览器，那么cookie变量一直是有效的，所以能够保证长时间不掉线。

如果你能够截获某个用户的cookie变量，然后伪造一个数据包发送过去，那么服务器还是 认为你是合法的。所以，使用cookie被攻击的可能性比较大。

如果cookie设置了有效值，那么cookie会保存到客户端的硬盘上，下次在访问网站的时候，浏览器先检查有没有cookie，如果有的话，读取cookie，然后发送给服务器。

所以你在机器上面保存了某个论坛cookie，有效期是一年，如果有人入侵你的机器，将你的cookie拷走，放在他机器下面，那么他登陆该网站的时候就是用你的身份登陆的。当然，伪造的时候需要注意，直接copy cookie文件到 cookie目录，浏览器是不认的，他有一个index.dat文件，存储了 cookie文件的建立时间，以及是否有修改，所以你必须先要有该网站的 cookie文件，并且要从保证时间上骗过浏览器

两个都可以用来存私密的东西，session过期与否，取决于服务器的设定。cookie过期与否，可以在cookie生成的时候设置进去。

**四、区别对比：**
(1)cookie数据存放在客户的浏览器上，session数据放在服务器上
(2)cookie不是很安全，别人可以分析存放在本地的COOKIE并进行COOKIE欺骗,如果主要考虑到安全应当使用session
(3)session会在一定时间内保存在服务器上。当访问增多，会比较占用你服务器的性能，如果主要考虑到减轻服务器性能方面，应当使用COOKIE
(4)单个cookie在客户端的限制是3K，就是说一个站点在客户端存放的COOKIE不能3K。
(5)所以：将登陆信息等重要信息存放为SESSION;其他信息如果需要保留，可以放在COOKIE中





## page（mybatis plus）



## 语法问题？

```
//添加过滤条件
queryWrapper.like(StringUtils.isNotEmpty(name), Employee::getName, name);
queryWrapper.orderByDesc(Employee::getUpdateTime);
```




import lombok.Data;

## @Data

加了@Data注解的类，编译后会自动给我们加上下列方法：

- 所有属性的get和set方法
- toString 方法
- hashCode方法
- equals方法

![img](takeaway%E5%AD%A6%E4%B9%A0%E7%9F%A5%E8%AF%86%E7%82%B9.assets/format,png.png)