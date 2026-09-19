package com.example.demo.config;

import com.example.demo.entity.SuperAdminEntity;
import com.example.demo.repository.SuperAdminRepository;
import com.example.demo.util.PasswordUtil;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SuperAdminInitializer implements CommandLineRunner {

    private final SuperAdminRepository superAdminRepository;
    private final PasswordUtil passwordUtil;

    public SuperAdminInitializer(
            SuperAdminRepository superAdminRepository,
            PasswordUtil passwordUtil) {

    	
    	
        this.superAdminRepository = superAdminRepository;
        this.passwordUtil = passwordUtil;

        
    }

    @Override
    public void run(String... args) {

        long count = superAdminRepository.countByDeletedAtIsNull();

        if (count > 0) {
            return;
        }

        SuperAdminEntity superAdmin = new SuperAdminEntity();

        superAdmin.setFullName("System Super Admin");
        superAdmin.setEmail("superadmin@stanveerento.com");
        superAdmin.setPhone("+919999999999");

        superAdmin.setPasswordHash(
                passwordUtil.encode("SuperAdmin@12345")
        );

        superAdmin.setActive(true);

        superAdminRepository.save(superAdmin);

        System.out.println(
                "=============================================="
        );
        System.out.println(
                "INITIAL SUPER ADMIN CREATED"
        );
        System.out.println(
                "Email    : superadmin@stanveerento.com"
        );
        System.out.println(
                "Password : SuperAdmin@12345"
        );
        System.out.println(
                "=============================================="
        );
    }
}