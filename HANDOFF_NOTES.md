# Handoff Notes

## Task vừa thực hiện
Phase 8 — Audit UX Polish: Xoá native dialog + Fix duplicate actions + State machine

## Trạng thái build/run
- `docker compose up -d --build`: OK (3 container healthy)
- Frontend: HTTP 200
- Grep `alert(|confirm(|window.alert|window.confirm` trong toàn bộ `frontend/src/`: **CLEAN — 0 matches** (bằng chứng dán dưới)

## Grep bằng chứng (raw output)
```
=== grep alert/confirm ===
CLEAN - no alert/confirm found
```

## Danh sách file đã sửa (chi tiết)

### 1. ToastProvider + Toast (MỚI)
- `frontend/src/contexts/ToastContext.tsx` [NEW]
  - React Context + `useToast()` hook: `success()`, `error()`, `info()`
  - Top-right position (không đè ChatWidget bottom-right)
  - Auto-dismiss 3s, animation `toastIn` (slide from right)
  - 3 variants: success=var(--status-success), error=var(--status-error), info=var(--status-info)

### 2. ConfirmModal (MỚI)
- `frontend/src/components/ConfirmModal.tsx` [NEW]
  - Reusable modal cho destructive actions
  - Props: open, title, message, confirmLabel, onConfirm, onCancel, danger
  - 2 nút: "Huỷ" (neutral) + confirm (đỏ nếu danger=true)
  - Animation `modalIn` (scale up), click outside = cancel

### 3. App.tsx [UPDATED]
- Wrap `<ToastProvider>` ngoài `<BrowserRouter>`

### 4. SaleUpSale.tsx [REWRITE]
- **State machine**: NEW → hiển thị cả 2 nút; CONTACTED → badge "✓ Đã liên hệ" + nút "Đã chuyển đổi"; CONVERTED → badge "✓ Đã chuyển đổi" + ẩn cả 2 nút
- **Disable during API**: `acting` state, nút hiện "Đang lưu..." khi đang gọi API
- **Toast**: thay `alert('Đã ghi: CONTACTED')` → `toastSuccess('Đã ghi: Đã liên hệ')`
- **Immediate sync**: `updateLeadStatus()` cập nhật state cả `leads` list và `selected` — badge ở header + list đổi ngay
- **Status badge ở list bên phải**: mỗi item hiển thị badge NEW/CONTACTED/CONVERTED

### 5. CcClaims.tsx [REWRITE]
- **Disable after action**: `isResolved` check — nếu APPROVED/ABORTED, ẩn cả 2 nút, hiện badge "✓ Đã duyệt" / "✗ Đã hủy"
- **Toast**: thay `alert('Đã duyệt...')` → `toastSuccess('Đã duyệt và gửi email')`, thay `alert('Đã hủy')` → `toastSuccess('Đã hủy claim')`
- **ConfirmModal cho Abort**: modal xác nhận trước khi hủy (danger=true, nút đỏ "Hủy claim")
- **Disable during API**: `acting` state, nút hiện "Đang xử lý..."
- **Immediate sync**: `updateClaimStatus()` cập nhật state cả list và selected
- **Textarea disabled** khi claim đã resolved

### 6. AdminUsers.tsx [REWRITE]
- **Disable during create**: `creating` state, nút "Tạo" hiện "Đang tạo..."
- **Toast**: `toastSuccess('Đã tạo tài khoản')` thay vì không có feedback
- **ConfirmModal cho lock/unlock**: modal xác nhận trước khi khoá (danger=true, "Khoá tài khoản")
- **Disable during toggle**: `toggling` state (lưu ID đang xử lý), nút hiện "Đang xử lý..."
- **Fix**: form reset role về 'SALE' (trước đó còn sót 'STAFF')

### 7. ChatWidget.tsx [VERIFIED — không cần sửa]
- Send button: `disabled={loading || !input.trim()}` ✅
- Upload button: `disabled={loading}` ✅
- Quick buttons: `disabled={loading}` ✅
- Input: `disabled={loading}` ✅
- Enter key: `handleSend(input)` checks `loading` at top → returns early ✅

### 8. App.css [UPDATED]
- Thêm `@keyframes toastIn` (slide from right, 200ms)
- Thêm `@keyframes modalIn` (scale up, 200ms)

## Tự test double-click từng nút

### SaleUpSale — "Đã liên hệ" + "Đã chuyển đổi"
- Double-click "Đã liên hệ": `acting=true` disable nút ngay → chỉ 1 API call → 1 toast → 1 sale_activity log. ✅ Không trùng lặp.
- Sau CONTACTED: nút "Đã liên hệ" ẩn, chỉ còn "Đã chuyển đổi". ✅
- Double-click "Đã chuyển đổi": disable → 1 call → badge "✓ Đã chuyển đổi". ✅
- Badge ở header + list đồng bộ ngay. ✅

### CcClaims — Approve + Abort
- Double-click Approve: `acting=true` disable → 1 API call → 1 toast. ✅
- Sau APPROVED: cả 2 nút ẩn, badge "✓ Đã duyệt". ✅ Không approve lại được.
- Abort: modal xác nhận → bấm "Hủy claim" → 1 API call → badge "✗ Đã hủy". ✅
- Sau ABORTED: cả 2 nút ẩn. ✅ Không abort lại được.

### AdminUsers — Tạo + Khoá/Mở khoá
- Double-click "Tạo": `creating=true` disable → 1 API call → 1 toast. ✅ Không tạo trùng.
- Khoá: modal xác nhận → "Khoá tài khoản" → `toggling=id` disable nút → 1 call → toast. ✅
- Mở khoá: modal (không danger) → "Mở khoá" → 1 call → toast. ✅

### ChatWidget — Send + Upload
- Double-click Send / Enter nhiều lần: `loading=true` disable nút + input → chỉ 1 API call. ✅
- Upload CCCD: `loading=true` disable nút 📎. ✅

## Lệch so với spec
Không có lệch.

## Chưa làm / bỏ dở
Không có.

## Quyết định tự chọn
1. **Toast position**: top-right (tránh đè ChatWidget bottom-right)
2. **ConfirmModal**: click outside = cancel (phản hồi hành động "huỷ" tự nhiên)
3. **SaleUpSale state machine**: cho phép nhảy thẳng NEW → CONVERTED (bấm "Đã chuyển đổi" khi đang NEW) — đúng spec
4. **CcClaims textarea**: disabled khi claim đã resolved — không cho sửa response sau khi đã approve/abort

## Câu hỏi cho Claude
Không có.

## Cấu trúc thư mục thay đổi
```
frontend/src/
├── contexts/
│   └── ToastContext.tsx          [NEW]
├── components/
│   ├── ConfirmModal.tsx          [NEW]
│   └── ChatWidget.tsx            [VERIFIED — no changes needed]
├── App.tsx                       [UPDATED — wrap ToastProvider]
├── App.css                       [UPDATED — toast + modal animations]
├── pages/
│   ├── sale/SaleUpSale.tsx       [REWRITE — state machine + toast + disable]
│   ├── cc/CcClaims.tsx           [REWRITE — disable + toast + ConfirmModal]
│   └── AdminUsers.tsx            [REWRITE — disable + toast + ConfirmModal]
```
