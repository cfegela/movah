package com.cfeg.movah.engine

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object FileMoverEngine {

    data class MoveResult(
        val status: String, // "SUCCESS", "PARTIAL", "FAILED"
        val itemsMoved: Int,
        val details: String
    )

    fun executeMove(sourcePath: String, targetPath: String): MoveResult {
        val sourceDir = File(sourcePath.trim())
        val targetDir = File(targetPath.trim())

        if (!sourceDir.exists()) {
            return MoveResult(
                status = "FAILED",
                itemsMoved = 0,
                details = "Source directory does not exist: $sourcePath"
            )
        }

        if (!sourceDir.isDirectory) {
            return MoveResult(
                status = "FAILED",
                itemsMoved = 0,
                details = "Source path is not a directory: $sourcePath"
            )
        }

        if (sourceDir.absolutePath == targetDir.absolutePath) {
            return MoveResult(
                status = "FAILED",
                itemsMoved = 0,
                details = "Source and target directories cannot be identical: $sourcePath"
            )
        }

        if (!targetDir.exists()) {
            val created = targetDir.mkdirs()
            if (!created && !targetDir.exists()) {
                return MoveResult(
                    status = "FAILED",
                    itemsMoved = 0,
                    details = "Failed to create target directory: $targetPath"
                )
            }
        }

        val items = sourceDir.listFiles()
        if (items == null) {
            return MoveResult(
                status = "FAILED",
                itemsMoved = 0,
                details = "Permission denied or unable to read contents of: $sourcePath"
            )
        }

        if (items.isEmpty()) {
            return MoveResult(
                status = "SUCCESS",
                itemsMoved = 0,
                details = "Source directory was empty; 0 items moved."
            )
        }

        var successCount = 0
        var failCount = 0
        val errorMessages = mutableListOf<String>()

        for (item in items) {
            val destinationItem = File(targetDir, item.name)

            try {
                // User requirement: overwrite files with the same name
                if (destinationItem.exists()) {
                    if (destinationItem.isDirectory) {
                        destinationItem.deleteRecursively()
                    } else {
                        destinationItem.delete()
                    }
                }

                // 1. Attempt atomic rename (same filesystem)
                val renamed = item.renameTo(destinationItem)
                if (renamed) {
                    successCount++
                    continue
                }

                // 2. Fallback to stream copy + delete
                if (item.isFile) {
                    val copied = copyFileWithTempFallback(item, destinationItem)
                    if (copied) {
                        item.delete()
                        successCount++
                    } else {
                        failCount++
                        errorMessages.add("Failed copying ${item.name}")
                    }
                } else if (item.isDirectory) {
                    val copied = copyDirectoryRecursively(item, destinationItem)
                    if (copied) {
                        item.deleteRecursively()
                        successCount++
                    } else {
                        failCount++
                        errorMessages.add("Failed copying folder ${item.name}")
                    }
                }
            } catch (e: Exception) {
                failCount++
                errorMessages.add("${item.name}: ${e.message ?: "Unknown error"}")
            }
        }

        val overallStatus = when {
            failCount == 0 -> "SUCCESS"
            successCount > 0 -> "PARTIAL"
            else -> "FAILED"
        }

        val detailsBuilder = StringBuilder()
        detailsBuilder.append("Moved $successCount of ${items.size} item(s).")
        if (errorMessages.isNotEmpty()) {
            detailsBuilder.append(" Errors: ").append(errorMessages.take(5).joinToString("; "))
            if (errorMessages.size > 5) {
                detailsBuilder.append(" ... and ${errorMessages.size - 5} more.")
            }
        }

        return MoveResult(
            status = overallStatus,
            itemsMoved = successCount,
            details = detailsBuilder.toString()
        )
    }

    private fun copyFileWithTempFallback(source: File, destination: File): Boolean {
        val tempDest = File(destination.parentFile, ".tmp_${System.nanoTime()}_${destination.name}")
        try {
            FileInputStream(source).use { inStream ->
                FileOutputStream(tempDest).use { outStream ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    while (inStream.read(buffer).also { bytesRead = it } != -1) {
                        outStream.write(buffer, 0, bytesRead)
                    }
                    outStream.flush()
                }
            }

            if (tempDest.length() != source.length()) {
                tempDest.delete()
                return false
            }

            if (destination.exists()) {
                destination.delete()
            }

            val renameSuccess = tempDest.renameTo(destination)
            if (!renameSuccess) {
                // If rename fails even for temp, copy temp to destination directly
                tempDest.copyTo(destination, overwrite = true)
                tempDest.delete()
            }
            return true
        } catch (e: IOException) {
            if (tempDest.exists()) {
                tempDest.delete()
            }
            return false
        }
    }

    private fun copyDirectoryRecursively(sourceDir: File, targetDir: File): Boolean {
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            return false
        }
        val children = sourceDir.listFiles() ?: return true
        for (child in children) {
            val destChild = File(targetDir, child.name)
            if (child.isDirectory) {
                if (!copyDirectoryRecursively(child, destChild)) return false
            } else {
                if (!copyFileWithTempFallback(child, destChild)) return false
            }
        }
        return true
    }
}
