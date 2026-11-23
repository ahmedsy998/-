package com.example.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AddCard
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryIconOption(
    val key: String,
    val icon: ImageVector,
    val labelAr: String
)

object CategoryIconMapper {
    val options: List<CategoryIconOption> = listOf(
        CategoryIconOption("restaurant", Icons.Rounded.Restaurant, "طعام"),
        CategoryIconOption("coffee", Icons.Rounded.Coffee, "قهوة"),
        CategoryIconOption("directions_bus", Icons.Rounded.DirectionsBus, "مواصلات"),
        CategoryIconOption("directions_car", Icons.Rounded.DirectionsCar, "سيارة"),
        CategoryIconOption("receipt_long", Icons.Rounded.ReceiptLong, "فواتير"),
        CategoryIconOption("bolt", Icons.Rounded.Bolt, "كهرباء"),
        CategoryIconOption("wifi", Icons.Rounded.Wifi, "إنترنت"),
        CategoryIconOption("home", Icons.Rounded.Home, "منزل وإيجار"),
        CategoryIconOption("shopping_bag", Icons.Rounded.ShoppingBag, "تسوق"),
        CategoryIconOption("checkroom", Icons.Rounded.Checkroom, "ملابس"),
        CategoryIconOption("medical_services", Icons.Rounded.MedicalServices, "صحة"),
        CategoryIconOption("family_restroom", Icons.Rounded.FamilyRestroom, "عائلة"),
        CategoryIconOption("school", Icons.Rounded.School, "تعليم"),
        CategoryIconOption("sports_esports", Icons.Rounded.SportsEsports, "ترفيه"),
        CategoryIconOption("fitness_center", Icons.Rounded.FitnessCenter, "رياضة"),
        CategoryIconOption("flight", Icons.Rounded.Flight, "سفر"),
        CategoryIconOption("pets", Icons.Rounded.Pets, "حيوانات"),
        CategoryIconOption("card_giftcard", Icons.Rounded.CardGiftcard, "هدايا"),
        CategoryIconOption("payments", Icons.Rounded.Payments, "راتب"),
        CategoryIconOption("work", Icons.Rounded.Work, "عمل"),
        CategoryIconOption("storefront", Icons.Rounded.Storefront, "مبيعات"),
        CategoryIconOption("account_balance", Icons.Rounded.AccountBalance, "بنك"),
        CategoryIconOption("add_card", Icons.Rounded.AddCard, "دخل إضافي"),
        CategoryIconOption("category", Icons.Rounded.Category, "أخرى")
    )

    val colorPalette: List<Long> = listOf(
        0xFFE53935,
        0xFFD81B60,
        0xFF8E24AA,
        0xFF5E35B1,
        0xFF3949AB,
        0xFF039BE5,
        0xFF00ACC1,
        0xFF00897B,
        0xFF15803D,
        0xFF7CB342,
        0xFFF9A825,
        0xFFFB8C00,
        0xFF6D4C41,
        0xFF546E7A
    )

    fun resolveIcon(key: String): ImageVector {
        return options.firstOrNull { it.key == key }?.icon ?: Icons.Rounded.Category
    }
}
