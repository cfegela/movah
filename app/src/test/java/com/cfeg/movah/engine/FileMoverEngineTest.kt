package com.cfeg.movah.engine

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class FileMoverEngineTest {

    private lateinit var tempBaseDir: File
    private lateinit var sourceDir: File
    private lateinit var targetDir: File

    @Before
    fun setUp() {
        tempBaseDir = Files.createTempDirectory("movah_test_").toFile()
        sourceDir = File(tempBaseDir, "source").apply { mkdir() }
        targetDir = File(tempBaseDir, "target").apply { mkdir() }
    }

    @After
    fun tearDown() {
        tempBaseDir.deleteRecursively()
    }

    @Test
    fun `test moving files from source to target`() {
        val file1 = File(sourceDir, "document.txt").apply { writeText("Hello World") }
        val file2 = File(sourceDir, "image.png").apply { writeText("Mock image bytes") }

        val result = FileMoverEngine.executeMove(sourceDir.absolutePath, targetDir.absolutePath)

        assertEquals("SUCCESS", result.status)
        assertEquals(2, result.itemsMoved)

        // Source files should no longer exist
        assertFalse(file1.exists())
        assertFalse(file2.exists())

        // Target files must exist with exact contents
        val dest1 = File(targetDir, "document.txt")
        val dest2 = File(targetDir, "image.png")
        assertTrue(dest1.exists())
        assertTrue(dest2.exists())
        assertEquals("Hello World", dest1.readText())
        assertEquals("Mock image bytes", dest2.readText())
    }

    @Test
    fun `test overwriting existing files with same name in target`() {
        // Target already has a file with the same name but old content
        File(targetDir, "overwrite_me.txt").apply { writeText("Old Content") }

        // Source has new content
        val sourceFile = File(sourceDir, "overwrite_me.txt").apply { writeText("New Fresh Content") }

        val result = FileMoverEngine.executeMove(sourceDir.absolutePath, targetDir.absolutePath)

        assertEquals("SUCCESS", result.status)
        assertEquals(1, result.itemsMoved)

        assertFalse(sourceFile.exists())
        val targetFile = File(targetDir, "overwrite_me.txt")
        assertTrue(targetFile.exists())
        assertEquals("New Fresh Content", targetFile.readText())
    }

    @Test
    fun `test moving subdirectories in source`() {
        val subDir = File(sourceDir, "photos").apply { mkdir() }
        File(subDir, "pic1.jpg").apply { writeText("Pic 1") }

        val result = FileMoverEngine.executeMove(sourceDir.absolutePath, targetDir.absolutePath)

        assertEquals("SUCCESS", result.status)
        assertEquals(1, result.itemsMoved)

        assertFalse(subDir.exists())
        val targetSubDir = File(targetDir, "photos")
        assertTrue(targetSubDir.exists())
        val targetPic = File(targetSubDir, "pic1.jpg")
        assertTrue(targetPic.exists())
        assertEquals("Pic 1", targetPic.readText())
    }

    @Test
    fun `test empty source directory`() {
        val result = FileMoverEngine.executeMove(sourceDir.absolutePath, targetDir.absolutePath)
        assertEquals("SUCCESS", result.status)
        assertEquals(0, result.itemsMoved)
        assertTrue(result.details.contains("empty"))
    }

    @Test
    fun `test non-existent source directory returns FAILED`() {
        val ghostDir = File(tempBaseDir, "non_existent")
        val result = FileMoverEngine.executeMove(ghostDir.absolutePath, targetDir.absolutePath)
        assertEquals("FAILED", result.status)
        assertEquals(0, result.itemsMoved)
    }

    @Test
    fun `test same source and target directories returns FAILED`() {
        val result = FileMoverEngine.executeMove(sourceDir.absolutePath, sourceDir.absolutePath)
        assertEquals("FAILED", result.status)
        assertEquals(0, result.itemsMoved)
    }
}
