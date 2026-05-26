package com.example.fajrapp.localization

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test

class LocaleResourcesEncodingTest {

    private val suspiciousPatterns = listOf(
        Regex("Р[А-Яа-яЁё]"),
        Regex("С[А-Яа-яЁё]"),
        Regex("[ЃЌЉЊЎўЈЅћќњ]")
    )

    @Test
    fun localeStringResources_areValidUtf8AndNotMojibake() {
        val resDir = resolveResDirectory()
        val valuesDirs = Files.list(resDir).use { stream ->
            stream
                .filter { Files.isDirectory(it) && it.fileName.toString().startsWith("values") }
                .collect(Collectors.toList())
        }

        assertTrue("No values* directories found in $resDir", valuesDirs.isNotEmpty())

        valuesDirs.forEach { valuesDir ->
            val stringsFile = valuesDir.resolve("strings.xml")
            if (!Files.exists(stringsFile)) return@forEach

            val text = String(Files.readAllBytes(stringsFile), StandardCharsets.UTF_8)
            assertTrue("Found UTF-8 replacement char in $stringsFile", !text.contains('\uFFFD'))

            // If this ratio is high, text is likely UTF-8 bytes decoded as Windows-1251.
            val suspiciousCount = suspiciousPatterns.sumOf { it.findAll(text).count() }
            val ratio = suspiciousCount.toDouble() / text.length.coerceAtLeast(1)
            assertTrue(
                "Possible mojibake detected in $stringsFile (ratio=$ratio, suspiciousCount=$suspiciousCount)",
                ratio < 0.02
            )

            parseXml(stringsFile)
        }
    }

    private fun resolveResDirectory(): Path {
        val candidates = listOf(
            Paths.get("src", "main", "res"),
            Paths.get("app", "src", "main", "res")
        )
        return candidates.firstOrNull { Files.isDirectory(it) }
            ?: error("Cannot resolve res directory from: $candidates")
    }

    private fun parseXml(stringsFile: Path) {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isExpandEntityReferences = false
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        val builder = factory.newDocumentBuilder()
        Files.newInputStream(stringsFile).use { input ->
            builder.parse(input)
        }
    }
}
