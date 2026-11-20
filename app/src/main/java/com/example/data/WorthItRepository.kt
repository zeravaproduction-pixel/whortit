package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AppMetadata
import com.example.model.WorthItAppData

class WorthItRepository(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun createEmptyData(nowMs: Long = System.currentTimeMillis()): WorthItAppData {
        return WorthItAppData(
            appVersion = "1.0.0",
            metadata = AppMetadata(
                createdAt = nowMs,
                updatedAt = nowMs,
                lastBackupAt = null
            )
        )
    }

    fun loadData(): LoadResult {
        return try {
            val raw = prefs.getString(STORAGE_KEY, null)
            if (raw.isNullOrBlank()) {
                val fresh = createEmptyData()
                return LoadResult.Fresh(fresh)
            }
            val parsed = WorthItJsonCodec.decodeAndValidate(raw)
            parsed.fold(
                onSuccess = { LoadResult.Loaded(it) },
                onFailure = { err ->
                    LoadResult.Corrupted(
                        fallback = createEmptyData(),
                        message = "Data lokal tidak dapat dibaca (${err.message})."
                    )
                }
            )
        } catch (e: Exception) {
            LoadResult.Corrupted(
                fallback = createEmptyData(),
                message = "Gagal mengakses penyimpanan lokal: ${e.message ?: "kesalahan tidak diketahui"}."
            )
        }
    }

    // PRD 8.3: Wrap in try/catch, never auto-delete existing data on failure, report error clearly
    fun saveData(data: WorthItAppData, nowMs: Long = System.currentTimeMillis()): Result<WorthItAppData> {
        return try {
            val updated = data.copy(
                metadata = data.metadata.copy(updatedAt = nowMs)
            )
            val encoded = WorthItJsonCodec.encode(updated)
            val committed = prefs.edit().putString(STORAGE_KEY, encoded).commit()
            if (!committed) {
                Result.failure(
                    IllegalStateException(
                        "Penyimpanan lokal penuh atau gagal menulis data. Data lama dipertahankan; segera lakukan Ekspor JSON."
                    )
                )
            } else {
                Result.success(updated)
            }
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    "Gagal menyimpan data (${e.message ?: "kapasitas penuh"}). Data sebelumnya tidak dihapus; silakan Ekspor JSON."
                )
            )
        }
    }

    fun savePreReplaceBackup(currentData: WorthItAppData): Result<Unit> {
        return try {
            val encoded = WorthItJsonCodec.encode(currentData)
            val committed = prefs.edit().putString(PRE_REPLACE_BACKUP_KEY, encoded).commit()
            if (committed) Result.success(Unit)
            else Result.failure(IllegalStateException("Gagal membuat backup lokal sebelum Replace."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun hasPreReplaceBackup(): Boolean {
        return try {
            !prefs.getString(PRE_REPLACE_BACKUP_KEY, null).isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }

    fun loadPreReplaceBackup(): Result<WorthItAppData> {
        return try {
            val raw = prefs.getString(PRE_REPLACE_BACKUP_KEY, null)
                ?: return Result.failure(IllegalStateException("Belum ada backup sebelum Replace."))
            WorthItJsonCodec.decodeAndValidate(raw)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clearAllData(nowMs: Long = System.currentTimeMillis()): Result<WorthItAppData> {
        val fresh = createEmptyData(nowMs)
        return saveData(fresh, nowMs)
    }

    companion object {
        const val PREFS_NAME = "worthit_storage_prefs"
        const val STORAGE_KEY = "worthit_app_v1"
        const val PRE_REPLACE_BACKUP_KEY = "worthit_app_v1_pre_replace_backup"
    }
}

sealed class LoadResult {
    data class Fresh(val data: WorthItAppData) : LoadResult()
    data class Loaded(val data: WorthItAppData) : LoadResult()
    data class Corrupted(val fallback: WorthItAppData, val message: String) : LoadResult()
}
