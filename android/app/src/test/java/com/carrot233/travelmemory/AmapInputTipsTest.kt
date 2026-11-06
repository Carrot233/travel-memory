package com.carrot233.travelmemory

import com.carrot233.travelmemory.data.place.jsStringLiteral
import com.carrot233.travelmemory.data.place.parsePlaceSuggestions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AmapInputTipsTest {
    @Test
    fun keepsSuggestionsThatHaveCoordinatesAndLimitsToFive() {
        val extras = (1..4).joinToString(",") { index ->
            """{"name":"点$index","address":"路$index","district":"区$index","latitude":30.$index,"longitude":120.$index}"""
        }
        val body = """
            [
              {"name":"西湖","district":"浙江省杭州市","address":"西湖风景名胜区","latitude":30.242,"longitude":120.148},
              {"name":"没有坐标","district":"杭州市","address":"某路"},
              {"name":"断桥","district":"杭州市西湖区","address":"北山街","latitude":30.258,"longitude":120.145},
              $extras
            ]
        """.trimIndent()
        val places = parsePlaceSuggestions(body)
        assertEquals(5, places.size)
        assertEquals(listOf("西湖", "断桥", "点1", "点2", "点3"), places.map { it.name })
        assertEquals(30.242, places[0].latitude, 0.0001)
        assertEquals(120.148, places[0].longitude, 0.0001)
        assertEquals("西湖风景名胜区", places[0].address)
        assertEquals("浙江省杭州市", places[0].district)
        assertFalse(places.any { it.name == "没有坐标" })
    }

    @Test
    fun quoteInAPlaceNameStaysInsideTheJsString() {
        val literal = jsStringLiteral("西湖\"断桥")
        assertTrue(literal.startsWith("\""))
        assertTrue(literal.endsWith("\""))
        assertTrue(literal.contains("\\\""))
        assertFalse(literal.contains("AMAP_"))
    }

    @Test
    fun androidSourcesDoNotEmbedAnAmapKey() {
        val root = listOf(File("."), File("app"), File("android"))
            .first { File(it, "src/main/AndroidManifest.xml").isFile || File(it, "app/src/main/AndroidManifest.xml").isFile }
        val module = if (File(root, "src/main/AndroidManifest.xml").isFile) root else File(root, "app")
        val project = module.parentFile ?: module
        val assignment = Regex("""AMAP_(KEY|SECURITY_CODE)["']?\s*[:=]\s*["'][A-Za-z0-9]{8,}""")
        project.walk().onEnter { dir ->
            dir.name != "build" && dir.name != ".gradle"
        }.filter { file ->
            file.isFile &&
                file.name != "local.properties" &&
                file.extension in setOf("kt", "kts", "properties", "md", "xml", "html", "example")
        }.forEach { file ->
            val text = file.readText()
            assertNull("${file.path} embeds an Amap secret", assignment.find(text))
        }
    }
}
