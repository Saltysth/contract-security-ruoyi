package com.ruoyi.feign.service;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.feign.config.AuthFeignConstants;
import com.ruoyi.feign.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 用户服务Feign客户端
 * 
 * @author Saltyfish
 */
@FeignClient(name = AuthFeignConstants.FEIGN_NAME, contextId = "userFeignService", configuration = com.ruoyi.feign.config.FeignConfiguration.class, path = AuthFeignConstants.FEIGN_PREFIX)
public interface UserFeignService
{
    /**
     * 获取用户列表
     * 
     * @param userDTO 用户查询条件
     * @return 用户列表
     */
    @GetMapping("/system/user/list")
    TableDataInfo list(UserDTO userDTO);

    /**
     * 根据用户编号获取详细信息
     * 
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping("/system/user/{userId}")
    AjaxResult getInfo(@PathVariable("userId") Long userId);

    /**
     * 新增用户
     * 
     * @param userDTO 用户信息
     * @return 操作结果
     */
    @PostMapping("/system/user")
    AjaxResult add(@RequestBody UserDTO userDTO);

    /**
     * 修改用户
     * 
     * @param userDTO 用户信息
     * @return 操作结果
     */
    @PutMapping("/system/user")
    AjaxResult edit(@RequestBody UserDTO userDTO);

    /**
     * 删除用户
     * 
     * @param userIds 用户ID数组
     * @return 操作结果
     */
    @DeleteMapping("/system/user/{userIds}")
    AjaxResult remove(@PathVariable("userIds") Long[] userIds);

    /**
     * 重置密码
     * 
     * @param userDTO 用户信息（包含用户ID和新密码）
     * @return 操作结果
     */
    @PutMapping("/system/user/resetPwd")
    AjaxResult resetPwd(@RequestBody UserDTO userDTO);

    /**
     * 状态修改
     * 
     * @param userDTO 用户信息（包含用户ID和状态）
     * @return 操作结果
     */
    @PutMapping("/system/user/changeStatus")
    AjaxResult changeStatus(@RequestBody UserDTO userDTO);

    /**
     * 根据用户编号获取授权角色
     * 
     * @param userId 用户ID
     * @return 角色列表
     */
    @GetMapping("/system/user/authRole/{userId}")
    AjaxResult authRole(@PathVariable("userId") Long userId);

    /**
     * 用户授权角色
     * 
     * @param userId 用户ID
     * @param roleIds 角色ID数组
     * @return 操作结果
     */
    @PutMapping("/system/user/authRole")
    AjaxResult insertAuthRole(@RequestParam("userId") Long userId, @RequestParam("roleIds") Long[] roleIds);

    /**
     * 批量查询用户信息
     * 
     * @param userIds 用户ID数组
     * @return 用户列表
     */
    @PostMapping("/system/user/batch")
    AjaxResult batchGetUsers(@RequestBody Long[] userIds);

    /**
     * 根据用户名查询用户信息
     * 
     * @param userName 用户名
     * @return 用户信息
     */
    @GetMapping("/system/user/byName/{userName}")
    AjaxResult getUserByName(@PathVariable("userName") String userName);
}