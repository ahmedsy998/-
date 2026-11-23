package com.example.data.local

import com.example.data.local.entity.CategoryEntity
import com.example.data.model.TransactionType

object DefaultCategories {
    val all: List<CategoryEntity> = listOf(
        // Built-in Expense Categories (12)
        CategoryEntity(nameAr = "طعام وشراب", type = TransactionType.EXPENSE, icon = "restaurant", color = 0xFFE53935, isCustom = false),
        CategoryEntity(nameAr = "مواصلات", type = TransactionType.EXPENSE, icon = "directions_bus", color = 0xFFFB8C00, isCustom = false),
        CategoryEntity(nameAr = "فواتير", type = TransactionType.EXPENSE, icon = "receipt_long", color = 0xFF8E24AA, isCustom = false),
        CategoryEntity(nameAr = "كهرباء", type = TransactionType.EXPENSE, icon = "bolt", color = 0xFFF9A825, isCustom = false),
        CategoryEntity(nameAr = "إنترنت وهاتف", type = TransactionType.EXPENSE, icon = "wifi", color = 0xFF039BE5, isCustom = false),
        CategoryEntity(nameAr = "إيجار", type = TransactionType.EXPENSE, icon = "home", color = 0xFF5E35B1, isCustom = false),
        CategoryEntity(nameAr = "تسوق", type = TransactionType.EXPENSE, icon = "shopping_bag", color = 0xFFD81B60, isCustom = false),
        CategoryEntity(nameAr = "صحة", type = TransactionType.EXPENSE, icon = "medical_services", color = 0xFF00897B, isCustom = false),
        CategoryEntity(nameAr = "عائلة", type = TransactionType.EXPENSE, icon = "family_restroom", color = 0xFF3949AB, isCustom = false),
        CategoryEntity(nameAr = "تعليم", type = TransactionType.EXPENSE, icon = "school", color = 0xFF00ACC1, isCustom = false),
        CategoryEntity(nameAr = "ترفيه", type = TransactionType.EXPENSE, icon = "sports_esports", color = 0xFF7CB342, isCustom = false),
        CategoryEntity(nameAr = "أخرى", type = TransactionType.EXPENSE, icon = "category", color = 0xFF546E7A, isCustom = false),

        // Built-in Income Categories (4)
        CategoryEntity(nameAr = "راتب", type = TransactionType.INCOME, icon = "payments", color = 0xFF15803D, isCustom = false),
        CategoryEntity(nameAr = "عمل", type = TransactionType.INCOME, icon = "work", color = 0xFF0D9488, isCustom = false),
        CategoryEntity(nameAr = "مبيعات", type = TransactionType.INCOME, icon = "storefront", color = 0xFF2E7D32, isCustom = false),
        CategoryEntity(nameAr = "دخل آخر", type = TransactionType.INCOME, icon = "add_card", color = 0xFF0284C7, isCustom = false)
    )
}
