package com.token.erp.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.token.erp.entity.User;
import com.token.erp.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        User admin = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, "admin"));
        if (admin == null) {
            admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRealName("超级管理员");
            admin.setStatus(1);
            admin.setRemark("初始化超级管理员");
            userMapper.insert(admin);
            log.info("已创建超级管理员 admin / admin123");
        } else if (admin.getPassword().contains("placeholder")) {
            admin.setPassword(passwordEncoder.encode("admin123"));
            userMapper.updateById(admin);
            log.info("已重置超级管理员密码为 admin123");
        }
    }
}
