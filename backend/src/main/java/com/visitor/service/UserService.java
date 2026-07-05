package com.visitor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.visitor.common.PageResult;
import com.visitor.entity.User;
import com.visitor.mapper.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {

    public PageResult<User> pageEmployees(int page, int size, String keyword, Integer departmentId) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<User>()
                .eq(User::getRole, "host")
                .like(keyword != null, User::getName, keyword)
                .eq(departmentId != null, User::getDepartmentId, departmentId)
                .orderByDesc(User::getId);
        IPage<User> p = page(new Page<>(page, size), qw);
        return new PageResult<>(p.getRecords(), p.getTotal(), page, size);
    }

    public PageResult<User> pageAdmins(int page, int size) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<User>()
                .eq(User::getRole, "admin")
                .orderByDesc(User::getId);
        IPage<User> p = page(new Page<>(page, size), qw);
        return new PageResult<>(p.getRecords(), p.getTotal(), page, size);
    }
}
