package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "member_point_logs")
data class MemberPointLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberNo: String,
    val memberNama: String,
    val type: String, // REDEEM, EARN, BONUS, ADJUSTMENT
    val pointsDelta: Int, // e.g. -50, +20, etc.
    val balanceAfter: Int,
    val referenceNo: String = "", // Nomor Bon / Referensi
    val note: String = "",
    val tanggal: String = "", // "dd/MM/yyyy HH:mm"
    val timestamp: Long = System.currentTimeMillis()
)
