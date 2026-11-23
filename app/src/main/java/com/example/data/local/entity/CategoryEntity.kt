package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.TransactionType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val nameAr: String,
    val type: TransactionType,
    val icon: String,
    val color: Long,
    val isCustom: Boolean = false
)
