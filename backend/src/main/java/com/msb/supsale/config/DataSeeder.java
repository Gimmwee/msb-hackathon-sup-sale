package com.msb.supsale.config;

import com.msb.supsale.model.*;
import com.msb.supsale.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Configuration
public class DataSeeder {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    CommandLineRunner seedData(ProductRepository productRepo, CustomerRepository customerRepo,
                               LeadRepository leadRepo, UserRepository userRepo,
                               CicRecordRepository cicRepo, TransactionRepository txnRepo,
                               ClaimRepository claimRepo, MessageRepository msgRepo,
                               ConversationRepository convRepo, SaleActivityRepository saleActRepo,
                               EmailLogRepository emailLogRepo, PasswordEncoder passwordEncoder) {
        return args -> {
            if (productRepo.count() == 0) {
                productRepo.save(buildProduct("Vay tín chấp", "Vay không cần tài sản đảm bảo", "VAY", "Có thu nhập ổn định, từ 18 tuổi", "CCCD, sao kê lương, giấy tờ chứng minh thu nhập"));
                productRepo.save(buildProduct("Vay mua ô tô", "Vay mua xe ô tô với tài sản là xe", "VAY", "Có thu nhập, xe mua từ đại lý uy tín", "CCCD, hợp đồng mua xe, sao kê lương"));
                productRepo.save(buildProduct("Vay mua nhà", "Vay mua nhà đất với tài sản là BĐS", "VAY", "Có thu nhập ổn định, BĐS có giấy tờ pháp lý", "CCCD, sổ hồng, sao kê lương, hợp đồng mua bán"));
                productRepo.save(buildProduct("Thẻ tín dụng", "Thẻ tín dụng MSB Mastercard/Visa", "THE", "Có thu nhập từ 6 triệu/tháng", "CCCD, sao kê lương"));
                log.info("Seeded 4 products");
            }

            if (userRepo.count() == 0) {
                User admin = new User(); admin.setUsername("admin"); admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
                admin.setFullName("Administrator"); admin.setRole(User.Role.ADMIN); admin.setActive(true); userRepo.save(admin);
                User sale = new User(); sale.setUsername("sale01"); sale.setPasswordHash(passwordEncoder.encode("Sale@123"));
                sale.setFullName("Sale User 01"); sale.setRole(User.Role.SALE); sale.setActive(true); userRepo.save(sale);
                User cc = new User(); cc.setUsername("cc01"); cc.setPasswordHash(passwordEncoder.encode("Cc@12345"));
                cc.setFullName("Contact Center 01"); cc.setRole(User.Role.CONTACT_CENTER); cc.setActive(true); userRepo.save(cc);
                log.info("Seeded 3 users");
                log.warn("⚠️ Đổi mật khẩu mặc định trước khi demo/production!");
            }

            if (customerRepo.count() == 0) {
                seedCustomers(customerRepo);
                log.info("Seeded 18 customers");
            }

            if (leadRepo.count() == 0) {
                seedLeads(leadRepo);
                log.info("Seeded 14 leads");
            }

            if (convRepo.count() == 0) {
                seedConversations(convRepo, msgRepo);
                log.info("Seeded conversations + messages");
            }

            if (cicRepo.count() == 0) {
                seedCicRecords(cicRepo);
                log.info("Seeded 18 CIC records");
            }

            if (txnRepo.count() == 0) {
                seedTransactions(txnRepo);
                log.info("Seeded transactions");
            }

            if (claimRepo.count() == 0) {
                seedClaims(claimRepo, emailLogRepo, userRepo);
                log.info("Seeded 7 claims");
            }

            if (saleActRepo.count() == 0) {
                User saleUser = userRepo.findByUsername("sale01").orElseThrow();
                seedSaleActivities(saleActRepo, leadRepo, saleUser.getId());
                log.info("Seeded sale activities");
            }
        };
    }

    private void seedCustomers(CustomerRepository repo) {
        Object[][] data = {
            {"Nguyễn Văn An", "0912345678", "002201008550", "an.nguyen@email.com"},
            {"Trần Thị Bình", "0987654321", "001234567890", "binh.tran@email.com"},
            {"Lê Hoàng Cường", "0977123456", "003456789012", null},
            {"Phạm Thị Dung", "0327369446", "009876543210", "dung.pham@email.com"},
            {"Vũ Minh Đức", "0901234567", "001111222233", "duc.vu@email.com"},
            {"Hoàng Thị Lan", "0915678901", "002222333344", null},
            {"Đỗ Văn Hùng", "0982345671", "003333444455", "hung.do@email.com"},
            {"Bùi Thị Mai", "0973456782", "004444555566", "mai.bui@email.com"},
            {"Ngô Minh Nam", "0904567893", "005555666677", null},
            {"Dương Thị Hoa", "0916789012", "006666777788", "hoa.duong@email.com"},
            {"Lý Văn Phúc", "0987890123", "007777888899", "phuc.ly@email.com"},
            {"Trịnh Thị Quỳnh", "0978901234", "008888999900", null},
            {"Phan Minh Tâm", "0909012345", "009990001111", "tam.phan@email.com"},
            {"Tô Thị Ngọc", "0910123456", "001000111122", "ngoc.to@email.com"},
            {"Lương Văn Bình", "0981234567", "002000222233", null},
            {"Cao Thị Hồng", "0972345678", "003000333344", "hong.cao@email.com"},
            {"Mạc Văn Khôi", "0903456789", "004000444455", "khoi.mac@email.com"},
            {"Chung Thị Diệu", "0914567890", "005000555566", null},
        };
        for (Object[] d : data) {
            Customer c = new Customer();
            c.setName((String) d[0]); c.setPhone((String) d[1]);
            c.setIdNumber((String) d[2]); c.setEmail((String) d[3]);
            repo.save(c);
        }
    }

    private void seedLeads(LeadRepository repo) {
        Object[][] data = {
            {"web-demo-1", "Nguyễn Văn An", "0912345678", "Vay mua ô tô", "NEW"},
            {"web-demo-2", "Trần Thị Bình", "0987654321", "Thẻ tín dụng", "NEW"},
            {"web-demo-3", "Lê Hoàng Cường", "0977123456", "Vay mua nhà", "CONTACTED"},
            {"web-demo-4", "Phạm Thị Dung", "0327369446", "Vay tín chấp", "NEW"},
            {"web-demo-5", "Vũ Minh Đức", "0901234567", "Thẻ tín dụng", "CONTACTED"},
            {"web-demo-6", "Hoàng Thị Lan", "0915678901", "Vay mua ô tô", "CONVERTED"},
            {"web-demo-7", "Đỗ Văn Hùng", "0982345671", "Vay tín chấp", "NEW"},
            {"web-demo-8", "Bùi Thị Mai", "0973456782", "Vay mua nhà", "CONTACTED"},
            {"web-demo-9", "Ngô Minh Nam", "0904567893", "Thẻ tín dụng", "NEW"},
            {"web-demo-10", "Dương Thị Hoa", "0916789012", "Vay mua ô tô", "CONVERTED"},
            {"web-demo-11", "Lý Văn Phúc", "0987890123", "Vay tín chấp", "CONTACTED"},
            {"web-demo-12", "Trịnh Thị Quỳnh", "0978901234", "Vay mua nhà", "NEW"},
            {"web-demo-13", "Phan Minh Tâm", "0909012345", "Thẻ tín dụng", "CONTACTED"},
            {"web-demo-14", "Tô Thị Ngọc", "0910123456", "Vay mua ô tô", "NEW"},
        };
        for (Object[] d : data) {
            Lead l = new Lead();
            l.setSessionId((String) d[0]); l.setCustomerName((String) d[1]);
            l.setPhone((String) d[2]); l.setProductInterest((String) d[3]);
            l.setStatus((String) d[4]);
            repo.save(l);
        }
    }

    private void seedConversations(ConversationRepository convRepo, MessageRepository msgRepo) {
        Instant now = Instant.now();
        Object[][][] chatData = {
            { // web-demo-1: An — vay mua ô tô
                {"user", "Chào shop, mình muốn vay mua ô tô nhưng chưa biết điều kiện thế nào.", -3},
                {"assistant", "Chào anh! Em là sup-sale MSB. Sản phẩm vay mua ô tô của MSB yêu cầu có thu nhập ổn định và xe mua từ đại lý uy tín. Hồ sơ cần: CCCD, hợp đồng mua xe, sao kê lương. Anh cho em xin tên và số điện thoại để em ghi nhận nhé!", -3},
                {"user", "Mình tên Nguyễn Văn An, SĐT 0912345678", -2},
                {"assistant", "Cảm ơn anh Nguyễn Văn A! Em đã ghi nhận nhu cầu vay mua ô tô. Bộ phận tư vấn sẽ liên hệ anh qua 0912345678 sớm ạ!", -2},
            },
            { // web-demo-2: Bình — thẻ tín dụng
                {"user", "Cho mình hỏi về thẻ tín dụng MSB", -5},
                {"assistant", "Chào chị! Thẻ tín dụng MSB Mastercard/Visa phù hợp với khách hàng có thu nhập từ 6 triệu/tháng. Chị có thể tải ảnh CCCD để hệ thống tự trích xuất thông tin nhanh hơn nhé.", -5},
                {"user", "Mình tên Trần Thị Bình, SĐT 0987654321, muốn mở thẻ", -4},
                {"assistant", "Cảm ơn chị Bình! Em đã ghi nhận nhu cầu mở thẻ tín dụng MSB. Chị có thể tải ảnh CCCD để hoàn tất hồ sơ nhanh hơn nhé.", -4},
                {"user", "Phí thường niên của thẻ là bao nhiêu?", -4},
                {"assistant", "Em xin lỗi, thông tin chi tiết về phí thường niên cần được bộ phận thẻ xác nhận. Chị có thể gọi hotline 1900 1088 để biết chính xác ạ.", -3},
            },
            { // web-demo-3: Cường — vay mua nhà
                {"user", "Mình muốn vay mua nhà, cần hồ sơ gì?", -7},
                {"assistant", "Chào anh! Vay mua nhà MSB cần: CCCD, sổ hồng, sao kê lương, hợp đồng mua bán. Điều kiện: có thu nhập ổn định, BĐS có giấy tờ pháp lý. Anh cho em xin tên và SĐT nhé!", -7},
                {"user", "Lê Hoàng Cường, 0977123456", -6},
                {"assistant", "Cảm ơn anh Cường! Em đã ghi nhận nhu cầu vay mua nhà. Bộ phận tín dụng sẽ liên hệ anh sớm ạ.", -6},
            },
            { // web-demo-4: Dung — vay tín chấp
                {"user", "Chào, mình muốn vay tín chấp không cần tài sản đảm bảo", -8},
                {"assistant", "Chào chị! Vay tín chấp MSB phù hợp cho khách hàng có thu nhập ổn định, từ 18 tuổi. Hồ sơ: CCCD, sao kê lương. Chị cho em xin tên và SĐT nhé!", -8},
                {"user", "Phạm Thị Dung, 0327369446", -7},
                {"assistant", "Cảm ơn chị Dung! Em đã ghi nhận nhu cầu vay tín chấp. Tỷ lệ duyệt phụ thuộc vào thu nhập và lịch sử tín dụng của chị ạ.", -7},
            },
            { // web-demo-5: Đức — thẻ tín dụng
                {"user", "Mình muốn mở thẻ Mastercard", -10},
                {"assistant", "Chào anh! Thẻ MSB Mastercard cần thu nhập từ 6 triệu/tháng. Anh cho em xin tên và SĐT để ghi nhận nhé!", -10},
                {"user", "Vũ Minh Đức, 0901234567", -9},
                {"assistant", "Cảm ơn anh Đức! Đã ghi nhận nhu cầu mở thẻ Mastercard. Anh có thể tải ảnh CCCD để xử lý hồ sơ nhanh hơn ạ.", -9},
            },
            { // web-demo-6: Lan — vay mua ô tô (converted)
                {"user", "Cho mình tư vấn vay mua xe", -15},
                {"assistant", "Chào chị! Vay mua ô tô MSB cần: CCCD, hợp đồng mua xe, sao kê lương. Chị cho em xin tên và SĐT nhé!", -15},
                {"user", "Hoàng Thị Lan, 0915678901, muốn vay mua Toyota Vios", -14},
                {"assistant", "Cảm ơn chị Lan! Em đã ghi nhận nhu cầu vay mua Toyota Vios. Bộ phận tín dụng sẽ liên hệ chị sớm ạ!", -14},
                {"user", "Cảm ơn shop, mình đã được nhân viên liên hệ và duyệt hồ sơ rồi", -10},
                {"assistant", "Tuyệt vời chị! Chúc mừng chị đã duyệt hồ sơ thành công. Cảm ơn chị đã tin tưởng MSB ạ!", -10},
            },
            { // web-demo-7: Hùng — vay tín chấp
                {"user", "Vay tín chấp được bao nhiêu tiền?", -6},
                {"assistant", "Chào anh! Hạn mức vay tín chấp phụ thuộc vào thu nhập và lịch sử tín dụng. Anh cho em xin tên và SĐT để tư vấn chi tiết nhé!", -6},
                {"user", "Đỗ Văn Hùng, 0982345671", -5},
                {"assistant", "Cảm ơn anh Hùng! Đã ghi nhận. Anh có thể tải CCCD để hệ thống kiểm tra nhanh hơn ạ.", -5},
            },
            { // web-demo-8: Mai — vay mua nhà
                {"user", "Mình muốn vay mua căn hộ", -12},
                {"assistant", "Chào chị! Vay mua nhà MSB cần CCCD, sổ hồng, sao kê lương. Chị cho em xin tên và SĐT nhé!", -12},
                {"user", "Bùi Thị Mai, 0973456782", -11},
                {"assistant", "Cảm ơn chị Mai! Đã ghi nhận nhu cầu vay mua căn hộ. Bộ phận tín dụng sẽ liên hệ chị ạ.", -11},
            },
            { // web-demo-9: Nam — thẻ tín dụng
                {"user", "Mở thẻ Visa điều kiện gì?", -4},
                {"assistant", "Chào anh! Thẻ MSB Visa cần thu nhập từ 6 triệu/tháng, hồ sơ: CCCD, sao kê lương. Anh cho em xin tên và SĐT nhé!", -4},
                {"user", "Ngô Minh Nam, 0904567893", -3},
                {"assistant", "Cảm ơn anh Nam! Đã ghi nhận nhu cầu mở thẻ Visa ạ.", -3},
            },
            { // web-demo-10: Hoa — vay mua ô tô (converted)
                {"user", "Mình muốn vay mua Honda CR-V", -20},
                {"assistant", "Chào chị! Vay mua ô tô MSB hỗ trợ mua xe từ đại lý uy tín. Chị cho em xin tên và SĐT nhé!", -20},
                {"user", "Dương Thị Hoa, 0916789012", -19},
                {"assistant", "Cảm ơn chị Hoa! Đã ghi nhận nhu cầu vay mua Honda CR-V ạ.", -19},
                {"user", "Mình đã được duyệt và nhận xe rồi, cảm ơn MSB!", -12},
                {"assistant", "Chúc mừng chị! Cảm ơn chị đã chọn MSB đồng hành ạ!", -12},
            },
            { // web-demo-11: Phúc — vay tín chấp
                {"user", "Vay tín chấp lãi suất thế nào?", -9},
                {"assistant", "Chào anh! Lãi suất vay tín chấp phụ thuộc vào hồ sơ. Anh cho em xin tên và SĐT để tư vấn chi tiết nhé!", -9},
                {"user", "Lý Văn Phúc, 0987890123", -8},
                {"assistant", "Cảm ơn anh Phúc! Đã ghi nhận ạ.", -8},
            },
            { // web-demo-12: Quỳnh — vay mua nhà
                {"user", "Vay mua nhà cần trả trước bao nhiêu %?", -5},
                {"assistant", "Chào chị! Tỷ lệ trả trước tuỳ thuộc vào chính sách hiện hành. Chị cho em xin tên và SĐT để tư vấn chi tiết nhé!", -5},
                {"user", "Trịnh Thị Quỳnh, 0978901234", -4},
                {"assistant", "Cảm ơn chị Quỳnh! Đã ghi nhận nhu cầu vay mua nhà ạ.", -4},
            },
            { // web-demo-13: Tâm — thẻ tín dụng
                {"user", "Thẻ tín dụng có chương trình hoàn tiền không?", -7},
                {"assistant", "Chào anh! MSB có các thẻ hoàn tiền và tích điểm. Anh cho em xin tên và SĐT để ghi nhận nhé!", -7},
                {"user", "Phan Minh Tâm, 0909012345", -6},
                {"assistant", "Cảm ơn anh Tâm! Đã ghi nhận nhu cầu mở thẻ ạ.", -6},
            },
            { // web-demo-14: Ngọc — vay mua ô tô
                {"user", "Mình muốn vay mua xe máy có được không?", -3},
                {"assistant", "Chào chị! MSB chủ yếu hỗ trợ vay mua ô tô. Vay tín chấp có thể phù hợp hơn cho nhu cầu mua xe máy. Chị cho em xin tên và SĐT nhé!", -3},
                {"user", "Tô Thị Ngọc, 0910123456", -2},
                {"assistant", "Cảm ơn chị Ngọc! Đã ghi nhận. Bộ phận tư vấn sẽ liên hệ chị ạ.", -2},
            },
        };

        for (Object[][] chat : chatData) {
            String sessionId = (String) chat[0][0];
            String platform = "WEB";
            Conversation conv = new Conversation();
            conv.setSessionId(sessionId); conv.setPlatform(platform);
            convRepo.save(conv);

            for (Object[] msg : chat) {
                Message m = new Message();
                m.setSessionId(sessionId);
                m.setRole((String) msg[0]);
                m.setContent((String) msg[1]);
                m.setCreatedAt(now.plus((int) msg[2], ChronoUnit.DAYS));
                msgRepo.save(m);
            }
        }
    }

    private void seedCicRecords(CicRecordRepository repo) {
        Object[][] data = {
            {"002201008550", "0912345678", 750, "Nhóm 1", 0},
            {"001234567890", "0987654321", 620, "Nhóm 2", 150_000_000},
            {"003456789012", "0977123456", 580, "Nhóm 3", 300_000_000},
            {"009876543210", "0327369446", 810, "Nhóm 1", 0},
            {"001111222233", "0901234567", 720, "Nhóm 1", 0},
            {"002222333344", "0915678901", 680, "Nhóm 2", 80_000_000},
            {"003333444455", "0982345671", 540, "Nhóm 3", 250_000_000},
            {"004444555566", "0973456782", 790, "Nhóm 1", 0},
            {"005555666677", "0904567893", 610, "Nhóm 2", 120_000_000},
            {"006666777788", "0916789012", 830, "Nhóm 1", 0},
            {"007777888899", "0987890123", 590, "Nhóm 3", 200_000_000},
            {"008888999900", "0978901234", 660, "Nhóm 2", 90_000_000},
            {"009990001111", "0909012345", 740, "Nhóm 1", 0},
            {"001000111122", "0910123456", 470, "Nhóm 4", 400_000_000},
            {"002000222233", "0981234567", 700, "Nhóm 1", 0},
            {"003000333344", "0972345678", 550, "Nhóm 2", 110_000_000},
            {"004000444455", "0903456789", 630, "Nhóm 2", 130_000_000},
            {"005000555566", "0914567890", 480, "Nhóm 4", 350_000_000},
        };
        for (Object[] d : data) {
            CicRecord r = new CicRecord();
            r.setIdNumber((String) d[0]); r.setPhone((String) d[1]);
            r.setCreditScore((int) d[2]); r.setDebtGroup((String) d[3]);
            r.setOutstandingLoans(((Number) d[4]).longValue());
            repo.save(r);
        }
    }

    private void seedTransactions(TransactionRepository repo) {
        Instant now = Instant.now();
        Object[][] allTxns = {
            {"0912345678", 5, 5_000_000, "Du lịch", "Đặt vé máy bay Đà Nẵng"},
            {"0912345678", 12, 3_000_000, "Du lịch", "Khách sạn biển"},
            {"0912345678", 18, 2_500_000, "Du lịch", "Tour núi Bà Đen"},
            {"0912345678", 25, 4_000_000, "Du lịch", "Vé máy bay Hà Nội"},
            {"0912345678", 30, 1_500_000, "Ăn uống", "Nhà hàng cao cấp"},
            {"0912345678", 35, 2_000_000, "Mua sắm", "Siêu thị Coopmart"},
            {"0912345678", 42, 1_200_000, "Ăn uống", "Cafe bạn bè"},
            {"0912345678", 50, 500_000, "Tiện ích", "Điện nước"},
            {"0912345678", 55, 800_000, "Tiện ích", "Internet + TV"},

            {"0987654321", 3, 8_000_000, "Chuyển khoản", "Chuyển tiền cho đối tác"},
            {"0987654321", 8, 6_000_000, "Chuyển khoản", "Thanh toán nhà cung cấp"},
            {"0987654321", 14, 5_000_000, "Chuyển khoản", "Chuyển tiền kinh doanh"},
            {"0987654321", 20, 2_000_000, "Mua sắm", "Online shopping Shopee"},
            {"0987654321", 28, 7_000_000, "Chuyển khoản", "Chuyển tiền"},
            {"0987654321", 35, 1_000_000, "Tiện ích", "Điện thoại"},
            {"0987654321", 40, 3_000_000, "Chuyển khoản", "Thanh toán đơn hàng"},
            {"0987654321", 48, 1_500_000, "Mua sắm", "Tiki"},
            {"0987654321", 55, 600_000, "Tiện ích", "Điện nước"},

            {"0977123456", 4, 3_000_000, "Mua sắm", "Nội thất phòng khách"},
            {"0977123456", 10, 4_000_000, "Mua sắm", "Tivi Samsung"},
            {"0977123456", 16, 2_500_000, "Mua sắm", "Tủ lạnh Panasonic"},
            {"0977123456", 22, 1_200_000, "Ăn uống", "Cafe"},
            {"0977123456", 30, 3_500_000, "Mua sắm", "Máy giặt"},
            {"0977123456", 38, 800_000, "Tiện ích", "Internet"},
            {"0977123456", 45, 1_800_000, "Ăn uống", "Nhà hàng"},
            {"0977123456", 52, 2_000_000, "Mua sắm", "Điện gia dụng"},

            {"0327369446", 2, 6_000_000, "Du lịch", "Tour Nhật Bản"},
            {"0327369446", 9, 4_000_000, "Du lịch", "Vé máy bay quốc tế"},
            {"0327369446", 15, 3_500_000, "Du lịch", "Resort Phú Quốc"},
            {"0327369446", 21, 2_500_000, "Ăn uống", "Nhà hàng Nhật"},
            {"0327369446", 28, 5_000_000, "Du lịch", "Spa & massage"},
            {"0327369446", 35, 2_000_000, "Ăn uống", "Fine dining"},
            {"0327369446", 42, 3_000_000, "Du lịch", "Tour Đà Lạt"},
            {"0327369446", 50, 1_500_000, "Tiện ích", "Điện nước"},
            {"0327369446", 58, 4_500_000, "Du lịch", "Vé máy bay"},

            {"0901234567", 6, 2_000_000, "Mua sắm", "Zara"},
            {"0901234567", 13, 1_500_000, "Ăn uống", "Food court"},
            {"0901234567", 20, 3_000_000, "Mua sắm", "Nike"},
            {"0901234567", 27, 800_000, "Tiện ích", "Điện thoại"},
            {"0901234567", 34, 2_500_000, "Mua sắm", "Uniqlo"},
            {"0901234567", 41, 1_200_000, "Ăn uống", "Cafe"},
            {"0901234567", 48, 1_800_000, "Mua sắm", "H&M"},
            {"0901234567", 55, 500_000, "Tiện ích", "Điện nước"},

            {"0915678901", 5, 7_000_000, "Du lịch", "Tour Singapore"},
            {"0915678901", 12, 4_000_000, "Du lịch", "Khách sạn 5 sao"},
            {"0915678901", 20, 3_000_000, "Du lịch", "Vé máy bay"},
            {"0915678901", 28, 2_000_000, "Ăn uống", "Nhà hàng"},
            {"0915678901", 36, 1_500_000, "Mua sắm", "Trang sức"},
            {"0915678901", 44, 1_000_000, "Tiện ích", "Điện nước"},
            {"0915678901", 52, 5_000_000, "Du lịch", "Tour Thái Lan"},
            {"0915678901", 58, 2_500_000, "Ăn uống", "Quán ăn"},

            {"0982345671", 7, 1_500_000, "Tiện ích", "Điện nước"},
            {"0982345671", 14, 2_000_000, "Mua sắm", "Điện tử"},
            {"0982345671", 22, 800_000, "Tiện ích", "Internet"},
            {"0982345671", 30, 1_200_000, "Ăn uống", "Cafe"},
            {"0982345671", 38, 3_000_000, "Mua sắm", "Điện thoại"},
            {"0982345671", 46, 600_000, "Tiện ích", "Điện thoại"},
            {"0982345671", 54, 1_800_000, "Ăn uống", "Nhà hàng"},

            {"0973456782", 4, 10_000_000, "Chuyển khoản", "Thanh toán BĐS"},
            {"0973456782", 11, 5_000_000, "Chuyển khoản", "Chuyển tiền"},
            {"0973456782", 18, 8_000_000, "Chuyển khoản", "Đặt cọc nhà"},
            {"0973456782", 26, 3_000_000, "Mua sắm", "Nội thất"},
            {"0973456782", 33, 6_000_000, "Chuyển khoản", "Thanh toán"},
            {"0973456782", 40, 2_000_000, "Ăn uống", "Tiệc"},
            {"0973456782", 48, 4_000_000, "Chuyển khoản", "Chuyển tiền"},
            {"0973456782", 56, 1_500_000, "Tiện ích", "Điện nước"},

            {"0904567893", 8, 3_500_000, "Mua sắm", "Laptop"},
            {"0904567893", 15, 2_000_000, "Mua sắm", "Điện thoại"},
            {"0904567893", 23, 1_500_000, "Ăn uống", "Food court"},
            {"0904567893", 31, 4_000_000, "Mua sắm", "Gaming PC"},
            {"0904567893", 39, 1_200_000, "Ăn uống", "Cafe"},
            {"0904567893", 47, 2_500_000, "Mua sắm", "Tablet"},
            {"0904567893", 55, 800_000, "Tiện ích", "Internet"},

            {"0916789012", 6, 5_000_000, "Du lịch", "Tour Hàn Quốc"},
            {"0916789012", 13, 3_000_000, "Du lịch", "Khách sạn Seoul"},
            {"0916789012", 20, 4_500_000, "Du lịch", "Vé máy bay"},
            {"0916789012", 28, 2_000_000, "Ăn uống", "Quán Hàn"},
            {"0916789012", 36, 3_500_000, "Du lịch", "Tour đảo"},
            {"0916789012", 44, 1_500_000, "Mua sắm", "Cosmetics"},
            {"0916789012", 52, 2_500_000, "Du lịch", "Spa"},
            {"0916789012", 58, 1_000_000, "Tiện ích", "Điện nước"},

            {"0987890123", 5, 2_000_000, "Tiện ích", "Điện nước"},
            {"0987890123", 12, 1_500_000, "Ăn uống", "Cafe"},
            {"0987890123", 19, 800_000, "Tiện ích", "Internet"},
            {"0987890123", 27, 1_200_000, "Tiện ích", "Điện thoại"},
            {"0987890123", 35, 600_000, "Tiện ích", "Rác thải"},
            {"0987890123", 43, 2_500_000, "Ăn uống", "Nhà hàng"},
            {"0987890123", 51, 1_000_000, "Tiện ích", "Điện nước"},

            {"0978901234", 7, 6_000_000, "Chuyển khoản", "Chuyển tiền"},
            {"0978901234", 14, 4_000_000, "Chuyển khoản", "Thanh toán"},
            {"0978901234", 21, 3_000_000, "Mua sắm", "Nội thất"},
            {"0978901234", 29, 5_000_000, "Chuyển khoản", "Chuyển tiền"},
            {"0978901234", 37, 2_000_000, "Ăn uống", "Tiệc"},
            {"0978901234", 45, 7_000_000, "Chuyển khoản", "Thanh toán"},
            {"0978901234", 53, 1_500_000, "Mua sắm", "Decor"},
            {"0978901234", 59, 800_000, "Tiện ích", "Điện nước"},

            {"0909012345", 4, 3_000_000, "Mua sắm", "Thời trang"},
            {"0909012345", 11, 2_500_000, "Mua sắm", "Giày"},
            {"0909012345", 18, 1_500_000, "Ăn uống", "Cafe"},
            {"0909012345", 26, 4_000_000, "Mua sắm", "Đồng hồ"},
            {"0909012345", 34, 2_000_000, "Mua sắm", "Túi xách"},
            {"0909012345", 42, 1_200_000, "Ăn uống", "Nhà hàng"},
            {"0909012345", 50, 3_500_000, "Mua sắm", "Trang sức"},
            {"0909012345", 57, 800_000, "Tiện ích", "Internet"},

            {"0910123456", 6, 1_500_000, "Tiện ích", "Điện nước"},
            {"0910123456", 13, 2_000_000, "Ăn uống", "Cafe"},
            {"0910123456", 20, 800_000, "Tiện ích", "Internet"},
            {"0910123456", 28, 1_200_000, "Tiện ích", "Điện thoại"},
            {"0910123456", 36, 600_000, "Tiện ích", "Rác"},
            {"0910123456", 44, 1_800_000, "Ăn uống", "Nhà hàng"},
            {"0910123456", 52, 500_000, "Tiện ích", "Điện nước"},

            {"0981234567", 5, 4_000_000, "Du lịch", "Tour miền Tây"},
            {"0981234567", 12, 3_000_000, "Du lịch", "Khách sạn Cần Thơ"},
            {"0981234567", 19, 2_500_000, "Du lịch", "Vé tàu"},
            {"0981234567", 27, 1_500_000, "Ăn uống", "Đặc sản"},
            {"0981234567", 35, 3_500_000, "Du lịch", "Tour Vũng Tàu"},
            {"0981234567", 43, 2_000_000, "Ăn uống", "Hải sản"},
            {"0981234567", 51, 1_000_000, "Tiện ích", "Điện nước"},
            {"0981234567", 58, 4_500_000, "Du lịch", "Vé máy bay"},

            {"0972345678", 8, 2_000_000, "Ăn uống", "Nhà hàng"},
            {"0972345678", 15, 3_000_000, "Mua sắm", "Lazada"},
            {"0972345678", 22, 1_500_000, "Ăn uống", "Cafe"},
            {"0972345678", 30, 4_000_000, "Mua sắm", "Shopee"},
            {"0972345678", 38, 2_500_000, "Ăn uống", "Food court"},
            {"0972345678", 46, 1_800_000, "Mua sắm", "Tiki"},
            {"0972345678", 54, 1_200_000, "Ăn uống", "Cafe"},

            {"0903456789", 7, 5_000_000, "Chuyển khoản", "Chuyển tiền"},
            {"0903456789", 14, 3_000_000, "Mua sắm", "Điện tử"},
            {"0903456789", 21, 6_000_000, "Chuyển khoản", "Thanh toán"},
            {"0903456789", 29, 2_000_000, "Ăn uống", "Nhà hàng"},
            {"0903456789", 37, 4_000_000, "Chuyển khoản", "Chuyển tiền"},
            {"0903456789", 45, 1_500_000, "Mua sắm", "Online"},
            {"0903456789", 53, 2_500_000, "Chuyển khoản", "Thanh toán"},

            {"0914567890", 6, 1_000_000, "Tiện ích", "Điện nước"},
            {"0914567890", 13, 800_000, "Tiện ích", "Internet"},
            {"0914567890", 20, 1_500_000, "Ăn uống", "Cafe"},
            {"0914567890", 28, 600_000, "Tiện ích", "Điện thoại"},
            {"0914567890", 36, 2_000_000, "Ăn uống", "Nhà hàng"},
            {"0914567890", 44, 500_000, "Tiện ích", "Rác"},
            {"0914567890", 52, 1_200_000, "Ăn uống", "Cafe"},
        };
        for (Object[] d : allTxns) {
            repo.save(buildTxn((String) d[0], now.minus((int) d[1], ChronoUnit.DAYS), ((Number) d[2]).longValue(), (String) d[3], (String) d[4]));
        }
        log.info("Seeded {} transactions", allTxns.length);
    }

    private void seedClaims(ClaimRepository claimRepo, EmailLogRepository emailLogRepo, UserRepository userRepo) {
        Instant now = Instant.now();
        UUID ccUserId = userRepo.findByUsername("cc01").map(User::getId).orElse(null);

        Object[][] data = {
            {"web-demo-1", "Nguyễn Văn An", "0912345678", "Lỗi giao dịch", -2, "PENDING", null,
             "Tôi khiếu nại về giao dịch chuyển tiền bị sai số tiền 5 triệu đồng. Giao dịch ngày 12/09 nhưng đến nay chưa được giải quyết, thái độ nhân viên tổng đài quá tệ!!!",
             "Kính chào anh Nguyễn Văn An, MSB xin chân thành xin lỗi về sự cố giao dịch sai số tiền. Chúng tôi đã ghi nhận khiếu nại và sẽ kiểm tra lại giao dịch trong vòng 24 giờ làm việc. Anh vui lòng giữ lại biên lai giao dịch để đối chiếu. Hotline hỗ trợ khẩn: 1900 1088."},
            {"web-demo-2", "Trần Thị Bình", "0987654321", "Phí dịch vụ", -1, "PENDING", null,
             "Không hài lòng về phí dịch vụ thẻ tín dụng bị trừ mà không được thông báo trước. Đây là lần thứ 2 rồi, rất thất vọng!",
             "Kính chào chị Trần Thị Bình, MSB xin lỗi vì thiếu sót thông báo phí dịch vụ thẻ. Chúng tôi sẽ xem xét lại quy trình thông báo phí và liên hệ chị để giải thích chi tiết. Nếu phí bị trừ sai, MSB sẽ hoàn lại đầy đủ."},
            {"web-demo-3", "Lê Hoàng Cường", "0977123456", "Thái độ nhân viên", -1, "PENDING", null,
             "Nhân viên phòng giao dịch xử lý hồ sơ vay mua nhà thái độ không chuyên nghiệp, trả lời câu hỏi rất hời hợt. Tôi mong muốn được phục vụ tốt hơn!",
             "Kính chào anh Lê Hoàng Cường, MSB rất tiếc khi nghe về trải nghiệm không hài lòng tại phòng giao dịch. Chúng tôi sẽ ghi nhận phản hồi và đào tạo lại nhân viên. Trưởng phòng sẽ liên hệ anh để hỗ trợ hồ sơ vay mua nhà."},
            {"web-demo-5", "Vũ Minh Đức", "0901234567", "Lỗi thẻ", -3, "PENDING", null,
             "Thẻ tín dụng MSB Mastercard của tôi bị khóa mà không có thông báo, không thể thanh toán được. Rất bất tiện!",
             "Kính chào anh Vũ Minh Đức, MSB xin lỗi về sự cố thẻ bị khóa. Chúng tôi sẽ kiểm tra nguyên nhân và mở khóa thẻ nếu không có vấn đề. Anh vui lòng liên hệ hotline 1900 1088 để được hỗ trợ khẩn."},
            {"web-demo-7", "Đỗ Văn Hùng", "0982345671", "Khác", -2, "PENDING", null,
             "App MSB mBank bị lỗi không đăng nhập được, báo lỗi hệ thống liên tục. Tôi cần chuyển tiền gấp!",
             "Kính chào anh Đỗ Văn Hùng, MSB xin lỗi về sự cố app mBank. Chúng tôi đang khắc phục và sẽ thông báo khi ổn định. Anh có thể sử dụng SMS Banking hoặc đến PGD gần nhất để giao dịch khẩn."},
            {"web-demo-6", "Hoàng Thị Lan", "0915678901", "Lỗi giao dịch", -5, "APPROVED", "Kính chào chị Hoàng Thị Lan, MSB đã kiểm tra và xác nhận giao dịch sai số tiền. Chúng tôi xin lỗi và đã hoàn lại 5.000.000đ vào tài khoản chị. Cảm ơn chị đã thông cảm.",
             "Giao dịch chuyển tiền bị trừ 2 lần, đến khiếu nại thì nhân viên nói chờ xử lý nhưng gần 1 tuần rồi chưa thấy kết quả!!!", null},
            {"web-demo-8", "Bùi Thị Mai", "0973456782", "Phí dịch vụ", -4, "APPROVED", "Kính chào chị Bùi Thị Mai, MSB xin lỗi về phí duy trì tài khoản bị trừ sai. Chúng tôi đã hoàn phí 200.000đ và cập nhật lại hệ thống. Cảm ơn chị đã phản hồi.",
             "Phí duy trì tài khoản bị trừ dù tôi đã miễn phí theo điều kiện, phải khiếu nại mới được xử lý!", null},
            {"web-demo-9", "Ngô Minh Nam", "0904567893", "Khác", -3, "ABORTED", null,
             "Khiếu nại về thời gian chờ gọi tổng đài quá lâu, 30 phút mới nghe máy!",
             "Kính chào anh Ngô Minh Nam, MSB xin lỗi về thời gian chờ tổng đài. Tuy nhiên, sau khi kiểm tra, cuộc gọi của anh đã được tiếp nhận trong 5 phút. Chúng tôi xin phép đóng khiếu nại này."},
        };

        for (Object[] d : data) {
            Claim claim = new Claim();
            claim.setSessionId((String) d[0]);
            claim.setCustomerName((String) d[1]);
            claim.setCustomerPhone((String) d[2]);
            claim.setTopic((String) d[3]);
            claim.setCreatedAt(now.plus((int) d[4], ChronoUnit.DAYS));
            claim.setStatus((String) d[5]);

            if (d[7] != null) {
                claim.setClaimContent((String) d[7]);
                claim.setSuggestedResponse((String) d[6]);
            } else {
                claim.setClaimContent((String) d[6]);
                claim.setSuggestedResponse((String) d[5]);
            }

            if ("APPROVED".equals(d[5])) {
                claim.setResolvedAt(now.plus((int) d[4] + 1, ChronoUnit.DAYS));
                claim.setResolvedBy(ccUserId);
                String response = (String) d[6];
                claim.setSuggestedResponse(response);
                claimRepo.save(claim);

                EmailLog emailLog = new EmailLog();
                emailLog.setClaimId(claim.getId());
                emailLog.setToEmail(claim.getCustomerPhone() + "@sms.msb.demo");
                emailLog.setSubject("Phản hồi khiếu nại từ MSB");
                emailLog.setBody(response);
                emailLog.setMock(true);
                emailLogRepo.save(emailLog);
            } else if ("ABORTED".equals(d[5])) {
                claim.setResolvedAt(now.plus((int) d[4] + 1, ChronoUnit.DAYS));
                claim.setResolvedBy(ccUserId);
                claim.setSuggestedResponse((String) d[6]);
                claimRepo.save(claim);
            } else {
                claim.setSuggestedResponse((String) d[6]);
                claimRepo.save(claim);
            }
        }
    }

    private void seedSaleActivities(SaleActivityRepository repo, LeadRepository leadRepo, UUID saleUserId) {
        var leads = leadRepo.findAll();
        Instant now = Instant.now();
        Object[][] data = {
            {0, "CONTACTED", "Đã gọi điện tư vấn vay mua ô tô"}, {0, "NOTE", "Khách quan tâm Toyota Vios"},
            {1, "CONTACTED", "Gửi thông tin thẻ qua Zalo"}, {1, "NOTE", "Khách hỏi thêm về phí"},
            {2, "CONTACTED", "Tư vấn vay mua nhà"}, {2, "CONVERTED", "Khách đã duyệt hồ sơ"},
            {3, "CONTACTED", "Gọi điện tư vấn vay tín chấp"}, {3, "NOTE", "Khách cần thời gian suy nghĩ"},
            {4, "CONTACTED", "Gửi brochure thẻ"}, {4, "CONVERTED", "Khách đã mở thẻ"},
            {5, "CONTACTED", "Tư vấn vay mua ô tô"}, {5, "CONVERTED", "Đã duyệt vay Honda CR-V"},
            {6, "NOTE", "Khách bận, hẹn gọi lại"}, {6, "CONTACTED", "Đã liên hệ lại"},
            {7, "CONTACTED", "Tư vấn vay mua nhà"}, {7, "NOTE", "Khách cần thêm hồ sơ"},
            {8, "CONTACTED", "Gửi thông tin thẻ Visa"}, {8, "NOTE", "Khách so sánh với ngân hàng khác"},
            {9, "CONTACTED", "Tư vấn vay mua ô tô"}, {9, "CONVERTED", "Đã ký hợp đồng vay"},
            {10, "CONTACTED", "Gửi thông tin vay tín chấp"}, {10, "NOTE", "Khách hỏi lãi suất"},
            {11, "CONTACTED", "Tư vấn vay mua nhà"}, {11, "NOTE", "Khách cần kiểm tra BĐS"},
            {12, "CONTACTED", "Gửi thông tin thẻ hoàn tiền"}, {12, "CONVERTED", "Khách đã mở thẻ"},
            {13, "NOTE", "Khách mới, chưa liên hệ"}, {13, "CONTACTED", "Đã giới thiệu sản phẩm"},
        };

        int[] dayOffsets = {0,0,1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8,9,9,10,10,11,11,12,12,13,13};
        for (int i = 0; i < data.length; i++) {
            Object[] d = data[i];
            int leadIdx = (int) d[0];
            if (leadIdx >= leads.size()) continue;
            SaleActivity a = new SaleActivity();
            a.setLeadId(leads.get(leadIdx).getId());
            a.setSaleUserId(saleUserId);
            a.setAction((String) d[1]);
            a.setNote((String) d[2]);
            a.setCreatedAt(now.minus(dayOffsets[i % dayOffsets.length], ChronoUnit.DAYS));
            repo.save(a);
        }
        log.info("Seeded {} sale activities", data.length);
    }

    private Product buildProduct(String name, String desc, String cat, String elig, String docs) {
        Product p = new Product();
        p.setName(name); p.setDescription(desc); p.setCategory(cat);
        p.setEligibilityInfo(elig); p.setRequiredDocuments(docs);
        return p;
    }

    private Transaction buildTxn(String phone, Instant date, long amount, String category, String desc) {
        Transaction t = new Transaction();
        t.setCustomerPhone(phone); t.setTransactionDate(date);
        t.setAmount(amount); t.setCategory(category); t.setDescription(desc);
        return t;
    }
}
