package com.xxxx.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxxx.server.pojo.Menu;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author czy
 * @since 2022-12-08
 */
public interface IMenuService extends IService<Menu> {

    /**
     * 根据用户id查询菜单列表
     *
     * @return
     */
    List<Menu> getMenusByAdminId();

    /**
     * 根据角色获取菜单列表
     *
     * @return
     */
    List<Menu> getMenusWithRole();

    /**
     * 查询所所有菜单
     *
     * @return
     */
    List<Menu> getAllMenus();
}
