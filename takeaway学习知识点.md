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

```java
// 查询条件构造器
QueryWrapper<BannerItem> wrapper = new QueryWrapper<>();
wrapper.eq("banner_id", id);
// 查询操作
List<BannerItem> bannerItems = bannerItemMapper.selectList(wrapper);
12345
```

然后我们可以引入[lambda](https://so.csdn.net/so/search?q=lambda&spm=1001.2101.3001.7020)，避免我们在代码中写类似的于banner_id的硬编码

```java
QueryWrapper<BannerItem> wrapper = new QueryWrapper<>();
wrapper.lambda().eq(BannerItem::getBannerId, id);
List<BannerItem> bannerItems = bannerItemMapper.selectList(wrapper);
123
```

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

## 问题

employee的name不应该是unique索引

# 员工管理