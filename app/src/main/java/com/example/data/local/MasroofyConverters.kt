package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurringRule
import com.example.data.model.TransactionType

class MasroofyConverters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String = value.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod = PaymentMethod.valueOf(value)

    @TypeConverter
    fun fromRecurringRule(value: RecurringRule): String = value.name

    @TypeConverter
    fun toRecurringRule(value: String): RecurringRule = RecurringRule.valueOf(value)
}
