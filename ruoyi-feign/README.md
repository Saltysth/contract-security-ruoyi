# ruoyi-feign 模块

下游引用此模块后需要做到几下几点
1. 启动类增加@EnableFeignClients并且扫描路径包括com.ruoyi.feign.service
2. 有一个最基本的SpringSecurityConfig配置类
## 概述

ruoyi-feign模块提供了用户管理相关的Feign客户端接口，方便其他服务调用用户管理的相关功能。

用户功能没有测试谨慎使用，鉴权功能放心使用

## 功能特性

- 提供用户CRUD操作的Feign接口
- 统一的DTO和VO对象
- 完整的用户状态管理
- 用户角色授权功能

## 使用方式

### 1. 添加依赖

在需要调用用户服务的模块中添加依赖：

```xml
<dependency>
    <groupId>com.ruoyi</groupId>
    <artifactId>ruoyi-feign</artifactId>
</dependency>
```

### 2. 启用Feign客户端

在启动类上添加`@EnableFeignClients`注解：

```java
@SpringBootApplication
@EnableFeignClients(basePackages = "com.ruoyi.feign.service")
public class Application
{
    public static void main(String[] args)
    {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3. 注入并使用Feign客户端

```java
@Service
public class UserService
{
    @Autowired
    private UserFeignService userFeignService;
    
    public TableDataInfo getUserList(UserDTO userDTO)
    {
        return userFeignService.list(userDTO);
    }
    
    public AjaxResult addUser(UserDTO userDTO)
    {
        return userFeignService.add(userDTO);
    }
    
    public AjaxResult updateUser(UserDTO userDTO)
    {
        return userFeignService.edit(userDTO);
    }
    
    public AjaxResult changeUserStatus(Long userId, String status)
    {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(userId);
        userDTO.setStatus(status);
        return userFeignService.changeStatus(userDTO);
    }
}
```

## API接口说明

### UserFeignService接口

| 方法 | 请求方式 | 路径 | 说明 |
|------|----------|------|------|
| list | GET | /system/user/list | 获取用户列表 |
| getInfo | GET | /system/user/{userId} | 根据用户编号获取详细信息 |
| add | POST | /system/user | 新增用户 |
| edit | PUT | /system/user | 修改用户 |
| remove | DELETE | /system/user/{userIds} | 删除用户 |
| resetPwd | PUT | /system/user/resetPwd | 重置密码 |
| changeStatus | PUT | /system/user/changeStatus | 状态修改 |
| authRole | GET | /system/user/authRole/{userId} | 根据用户编号获取授权角色 |
| insertAuthRole | PUT | /system/user/authRole | 用户授权角色 |
| batchGetUsers | POST | /system/user/batch | 批量查询用户信息 |
| getUserByName | GET | /system/user/byName/{userName} | 根据用户名查询用户信息 |

## DTO和VO对象

### UserDTO（数据传输对象）

用于接收和传递用户数据，包含用户的完整信息。

### UserVO（视图对象）

用于返回用户数据，包含用户信息以及关联的部门和角色信息。

### DeptVO（部门视图对象）

用于返回部门信息。

### RoleVO（角色视图对象）

用于返回角色信息。

## 配置说明

Feign客户端的配置在`FeignConfiguration`类中：

- 日志级别：BASIC（记录请求方法、URL、响应状态代码以及执行时间）
- 重试机制：不重试

## 注意事项

1. 使用前请确保目标服务（ruoyi-system）已启动并正常运行
2. 请确保网络连接正常，能够访问目标服务的接口
3. 调用接口时请确保有相应的权限
4. 密码字段在传输过程中会被自动加密处理