package com.example.data.sync

enum class SyncRole {
    STANDALONE, // Mode mandiri / 1 perangkat offline
    SERVER,     // Perangkat 1 sebagai Server / Master Toko
    CLIENT      // Perangkat berikutnya sebagai Kasir Klien
}

data class SyncConfig(
    val role: SyncRole = SyncRole.STANDALONE,
    val deviceId: String = "",
    val deviceName: String = "Kasir 1",
    val serverHost: String = "192.168.1.100",
    val serverPort: Int = 8888,
    val autoSyncIntervalSec: Int = 5,
    val isAutoSyncEnabled: Boolean = true
)

data class DevicePeer(
    val deviceId: String,
    val deviceName: String,
    val role: String, // "SERVER" atau "CLIENT"
    val cashierNik: String,
    val cashierName: String,
    val ipAddress: String,
    val lastSeenTimestamp: Long,
    val pendingCount: Int, // Jumlah transaksi / keranjang pending di kasir ini
    val isOnline: Boolean = true,
    val isShiftOpen: Boolean = false,
    val hasClerek: Boolean = false
)

data class SyncStatusInfo(
    val isServerRunning: Boolean = false,
    val isConnectedToServer: Boolean = false,
    val localIpAddress: String = "127.0.0.1",
    val serverPort: Int = 8888,
    val connectedDevices: List<DevicePeer> = emptyList(),
    val lastSyncTimestamp: Long = 0,
    val lastSyncStatus: String = "Siap sinkron",
    val isSuccess: Boolean = true
)

data class EodPendingCheckResult(
    val canProceed: Boolean,
    val localPendingCount: Int,
    val totalPendingAcrossAllDevices: Int,
    val devicesWithPending: List<DevicePeer>,
    val activeDevicesCount: Int,
    val message: String,
    val devicesWithoutClerek: List<DevicePeer> = emptyList()
)
