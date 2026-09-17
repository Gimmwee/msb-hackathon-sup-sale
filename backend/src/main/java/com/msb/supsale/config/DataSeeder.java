package com.msb.supsale.config;

import com.msb.supsale.model.*;
import com.msb.supsale.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    CommandLineRunner seedData(ProductRepository productRepo, CustomerRepository customerRepo,
                               LeadRepository leadRepo, UserRepository userRepo,
                               CicRecordRepository cicRepo, PasswordEncoder passwordEncoder) {
        return args -> {
            if (productRepo.count() == 0) {
                productRepo.save(buildProduct("Vay tín chấp", "Vay không cần tài sản đảm bảo", "VAY",
                        "Có thu nhập ổn định, từ 18 tuổi", "CCCD, sao kê lương, giấy tờ chứng minh thu nhập"));
                productRepo.save(buildProduct("Vay mua ô tô", "Vay mua xe ô tô với tài sản là xe", "VAY",
                        "Có thu nhập, xe mua từ đại lý uy tín", "CCCD, hợp đồng mua xe, sao kê lương"));
                productRepo.save(buildProduct("Vay mua nhà", "Vay mua nhà đất với tài sản là BĐS", "VAY",
                        "Có thu nhập ổn định, BĐS có giấy tờ pháp lý", "CCCD, sổ hồng, sao kê lương, hợp đồng mua bán"));
                productRepo.save(buildProduct("Thẻ tín dụng", "Thẻ tín dụng MSB Mastercard/Visa", "THE",
                        "Có thu nhập từ 6 triệu/tháng", "CCCD, sao kê lương"));
                log.info("Seeded 4 products");
            }

            if (customerRepo.count() == 0) {
                customerRepo.save(buildCustomer("Nguyễn Văn Demo", "0912345678"));
                customerRepo.save(buildCustomer("Trần Thị Sample", "0987654321"));
                log.info("Seeded 2 demo customers");
            }

            if (leadRepo.count() == 0) {
                leadRepo.save(buildLead("web-demo-1", "Nguyễn Văn An", "0912345678", "Vay tín chấp mua xe"));
                leadRepo.save(buildLead("web-demo-2", "Trần Thị Bình", "0987654321", "Thẻ tín dụng MSB"));
                leadRepo.save(buildLead("web-demo-3", "Lê Hoàng Cường", "0977123456", "Vay mua nhà"));
                log.info("Seeded 3 demo leads");
            }

            if (userRepo.count() == 0) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
                admin.setFullName("Administrator");
                admin.setRole(User.Role.ADMIN);
                admin.setActive(true);
                userRepo.save(admin);

                User staff = new User();
                staff.setUsername("staff01");
                staff.setPasswordHash(passwordEncoder.encode("Staff@123"));
                staff.setFullName("Staff User 01");
                staff.setRole(User.Role.STAFF);
                staff.setActive(true);
                userRepo.save(staff);

                log.info("Seeded 2 users (admin/Admin@123, staff01/Staff@123)");
                log.warn("⚠️ Đổi mật khẩu mặc định trước khi demo/production!");
            }

            if (cicRepo.count() == 0) {
                cicRepo.save(buildCic("002201008550", "0912345678", 750, "Nhóm 1", 0));
                cicRepo.save(buildCic("001234567890", "0987654321", 620, "Nhóm 2", 150_000_000));
                cicRepo.save(buildCic("003456789012", "0977123456", 580, "Nhóm 3", 300_000_000));
                cicRepo.save(buildCic("009876543210", "0327369446", 810, "Nhóm 1", 0));
                log.info("Seeded 4 CIC records");
            }
        };
    }

    private Product buildProduct(String name, String desc, String cat, String elig, String docs) {
        Product p = new Product();
        p.setName(name); p.setDescription(desc); p.setCategory(cat);
        p.setEligibilityInfo(elig); p.setRequiredDocuments(docs);
        return p;
    }

    private Customer buildCustomer(String name, String phone) {
        Customer c = new Customer();
        c.setName(name); c.setPhone(phone);
        return c;
    }

    private Lead buildLead(String session, String name, String phone, String interest) {
        Lead l = new Lead();
        l.setSessionId(session); l.setCustomerName(name); l.setPhone(phone);
        l.setProductInterest(interest); l.setStatus("NEW");
        return l;
    }

    private CicRecord buildCic(String idNumber, String phone, int score, String group, long loans) {
        CicRecord r = new CicRecord();
        r.setIdNumber(idNumber); r.setPhone(phone);
        r.setCreditScore(score); r.setDebtGroup(group);
        r.setOutstandingLoans(loans);
        return r;
    }
}
