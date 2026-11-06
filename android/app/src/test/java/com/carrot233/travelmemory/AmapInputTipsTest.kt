package com.carrot233.travelmemory

import com.carrot233.travelmemory.data.place.AmapTipsParse
import com.carrot233.travelmemory.data.place.parseAmapInputTips
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AmapInputTipsTest {
    @Test
    fun keepsTipsThatHaveCoordinatesAndDropsTheRest() {
        val body = """
            {"status":"1","info":"OK","tips":[
              {"name":"西湖","district":"浙江省杭州市","address":"西湖风景名胜区","location":"120.148000,30.242000"},
              {"name":"西湖区","district":"杭州市","address":[],"location":[]},
              {"name":"断桥","district":"杭州市西湖区","address":"北山街","location":"120.145000,30.258000"},
              {"name":"没有坐标","district":"杭州市","address":"某路"}
            ]}
        """.trimIndent()
        val parsed = parseAmapInputTips(body) as AmapTipsParse.Ok
        assertEquals(listOf("西湖", "断桥"), parsed.places.map { it.name })
        assertEquals(30.242, parsed.places[0].latitude, 0.0001)
        assertEquals(120.148, parsed.places[0].longitude, 0.0001)
        assertEquals("西湖风景名胜区", parsed.places[0].address)
        assertEquals("浙江省杭州市", parsed.places[0].district)
        assertEquals("北山街", parsed.places[1].address)
    }

    @Test
    fun reportsInvalidKeyWithoutEchoingIt() {
        val parsed = parseAmapInputTips("""{"status":"0","info":"INVALID_USER_KEY"}""")
        assertTrue(parsed is AmapTipsParse.Error)
        assertFalse((parsed as AmapTipsParse.Error).message.contains("INVALID_USER_KEY"))
    }

    @Test
    fun androidSourcesDoNotEmbedAnAmapKey() {
        val root = listOf(File("."), File("app"), File("android"))
            .first { File(it, "src/main/AndroidManifest.xml").isFile || File(it, "app/src/main/AndroidManifest.xml").isFile }
        val module = if (File(root, "src/main/AndroidManifest.xml").isFile) root else File(root, "app")
        val project = module.parentFile ?: module
        val assignment = Regex("""AMAP_KEY["']?\s*[:=]\s*["'][A-Za-z0-9]{8,}""")
        project.walk().onEnter { dir ->
            dir.name != "build" && dir.name != ".gradle"
        }.filter { file ->
            file.isFile &&
                file.name != "local.properties" &&
                file.extension in setOf("kt", "kts", "properties", "md", "xml", "example")
        }.forEach { file ->
            val text = file.readText()
            assertNull("${file.path} embeds AMAP_KEY", assignment.find(text))
        }
    }
}
