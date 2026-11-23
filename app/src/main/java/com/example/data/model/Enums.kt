package com.example.data.model

enum class TransactionType(val labelAr: String) {
    EXPENSE("مصروف"),
    INCOME("دخل")
}

enum class PaymentMethod(val labelAr: String) {
    CASH("نقداً"),
    BANK_TRANSFER("تحويل بنكي"),
    CARD("بطاقة")
}

enum class RecurringRule(val labelAr: String) {
    NONE("بدون تكرار"),
    WEEKLY("أسبوعي"),
    MONTHLY("شهري")
}

enum class ThemeMode(val labelAr: String) {
    SYSTEM("حسب النظام"),
    LIGHT("فاتح"),
    DARK("داكن")
}

enum class DateRangePreset(val labelAr: String) {
    ALL("الكل"),
    TODAY("اليوم"),
    THIS_WEEK("هذا الأسبوع"),
    THIS_MONTH("هذا الشهر"),
    CUSTOM("فترة مخصصة")
}
