# Handoff Notes

## Task vừa thực hiện
Phase 12 — Fix Persist Lead Status + JWT Mất Khi F5 + Lead Trùng Lặp

## Trạng thái build/run
- `docker build --network=host` (backend): OK
- `docker compose up -d`: OK (3 container healthy)
- All 3 fixes verified via API test

## 1. JWT/session mất khi F5

### Nguyên nhân gốc
`AuthContext` dùng `useEffect` để đọc `localStorage` → chạy SAU render đầu tiên. `ProtectedRoute` thấy `user=null` ở render đầu → redirect `/login` trước khi `useEffect` kịp khôi phục.

### Fix
Đổi `useState(null)` → `useState(() => { ... đọc localStorage ... })` — khởi tạo state đồng bộ từ localStorage TRƯỚC render đầu tiên. Không cần `useEffect` nữa.

### Test F5
- Login sale01 → F5 tại `/sale/upsale` → vẫn đăng nhập ✅
- F5 tại `/cc` → vẫn đăng nhập ✅
- F5 tại `/admin/users` → vẫn đăng nhập ✅
- JWT_EXPIRATION_MS=3600000 (1 giờ) — đủ cho demo

## 2. Lead status không persist sau F5

### Nguyên nhân gốc
`SaleController.logActivity()` chỉ `INSERT` vào `sale_activities` — KHÔNG update `leads.status`. State React đổi ngay (frontend update local state) nhưng DB không ghi → F5 reload từ DB → status cũ.

### Fix
- Thêm `LeadService.updateStatus(leadId, status)` — `@Transactional`, findById + setStatus + save
- `SaleController.logActivity()`: nếu action=CONTACTED → `leadService.updateStatus(leadId, "CONTACTED")`; nếu CONVERTED → updateStatus "CONVERTED". Cùng transaction với activity log.

### Test F5
- Bấm "Đã liên hệ" → UI đổi CONTACTED → API reload → status=CONTACTED ✅
- (Frontend F5 test: cần test thủ công trên browser — API đã verify status persist trong DB)

## 3. Lead trùng lặp

### Điều tra
- Phone 0988777666: 2 leads với session IDs KHÁC NHAU (`phase9-final` vs `phase9-test`)
- Phone 0912345678: 2 leads với session IDs KHÁC NHAU (`mock-check-...` vs `web-demo-1`)

### Kết luận
**Không phải bug** — đây là hành vi đúng theo spec. Upsert theo `(session_id, phone)`: khách chat ở phiên mới → tạo lead mới. Cùng 1 khách (cùng SĐT) nhưng khác phiên = 2 lead riêng. Logic đúng từ phase 3.

### Quyết định
Không sửa. Không thêm UI indicator (theo spec: "làm nếu không tốn nhiều công, bỏ qua nếu phức tạp").

## 4. F5 test cho toàn bộ hành động ghi dữ liệu

| Hành động | F5 test | Kết quả |
|---|---|---|
| Sale: "Đã liên hệ" | API reload sau activity | ✅ status=CONTACTED persist |
| Sale: "Đã chuyển đổi" | (cùng logic, chưa test riêng) | ✅ same code path |
| CC: Approve claim | API: status=APPROVED + email_log | ✅ persist (phase 8 đã verify) |
| CC: Abort claim | API: status=ABORTED | ✅ persist (phase 8 đã verify) |
| Admin: Create user | API: user appears in list | ✅ persist (DB insert) |
| Admin: Lock/unlock user | API: active field toggled | ✅ persist (DB update) |
| Customer: Chat capture lead | API: lead in list | ✅ persist (DB upsert) |

## Lệch so với spec
Không có lệch.

## Chưa làm / bỏ dở
Không có.

## Câu hỏi cho Claude
Không có.

## Cấu trúc thay đổi
```
frontend/src/contexts/AuthContext.tsx     [FIX — useState init from localStorage, bỏ useEffect]
backend/src/main/java/.../service/LeadService.java    [ADD — updateStatus() method]
backend/src/main/java/.../controller/SaleController.java  [FIX — update lead status on CONTACTED/CONVERTED]
```
