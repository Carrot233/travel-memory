package com.carrot233.travelmemory.data.place

data class PlaceSuggestion(
    val name: String,
    val address: String?,
    val district: String?,
    val latitude: Double,
    val longitude: Double,
) {
    fun subtitle(): String = listOfNotNull(address, district).joinToString(" · ")
}

sealed interface AmapTipsParse {
    data class Ok(val places: List<PlaceSuggestion>) : AmapTipsParse
    data class Error(val message: String) : AmapTipsParse
}

fun parseAmapInputTips(body: String): AmapTipsParse {
    val status = jsonStringField(body, "status")
    if (status == "0") {
        return AmapTipsParse.Error("搜索失败，请检查 AMAP_KEY 是否有效")
    }
    val places = extractTipsObjects(body).mapNotNull { tip -> tip.toSuggestion() }.take(5)
    return AmapTipsParse.Ok(places)
}

private fun String.toSuggestion(): PlaceSuggestion? {
    val name = jsonStringField(this, "name")?.takeIf { it.isNotBlank() } ?: return null
    val location = jsonStringField(this, "location") ?: return null
    val parts = location.split(',')
    if (parts.size != 2) return null
    val longitude = parts[0].trim().toDoubleOrNull() ?: return null
    val latitude = parts[1].trim().toDoubleOrNull() ?: return null
    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
    return PlaceSuggestion(
        name = name,
        address = jsonStringField(this, "address").blankPlaceText(),
        district = jsonStringField(this, "district").blankPlaceText(),
        latitude = latitude,
        longitude = longitude,
    )
}

private fun String?.blankPlaceText(): String? {
    val text = this?.trim().orEmpty()
    if (text.isEmpty() || text == "[]") return null
    return text
}

internal fun extractTipsObjects(json: String): List<String> {
    val marker = Regex(""""tips"\s*:\s*\[""").find(json) ?: return emptyList()
    var index = marker.range.last + 1
    val objects = mutableListOf<String>()
    while (index < json.length) {
        while (index < json.length && (json[index].isWhitespace() || json[index] == ',')) index++
        if (index >= json.length || json[index] == ']') break
        if (json[index] != '{') {
            index++
            continue
        }
        val end = matchingBrace(json, index) ?: break
        objects += json.substring(index, end + 1)
        index = end + 1
    }
    return objects
}

private fun matchingBrace(json: String, open: Int): Int? {
    var depth = 0
    var inString = false
    var escaped = false
    for (index in open until json.length) {
        val char = json[index]
        if (inString) {
            if (escaped) {
                escaped = false
            } else if (char == '\\') {
                escaped = true
            } else if (char == '"') {
                inString = false
            }
            continue
        }
        when (char) {
            '"' -> inString = true
            '{' -> depth++
            '}' -> {
                depth--
                if (depth == 0) return index
            }
        }
    }
    return null
}

internal fun jsonStringField(objectText: String, field: String): String? {
    val pattern = Regex(""""${Regex.escape(field)}"\s*:\s*"((?:\\.|[^"\\])*)"""")
    val raw = pattern.find(objectText)?.groupValues?.get(1) ?: return null
    return unescapeJson(raw)
}

private fun unescapeJson(raw: String): String {
    val out = StringBuilder()
    var index = 0
    while (index < raw.length) {
        val char = raw[index]
        if (char == '\\' && index + 1 < raw.length) {
            when (val next = raw[index + 1]) {
                '"', '\\', '/' -> {
                    out.append(next)
                    index += 2
                }
                'n' -> {
                    out.append('\n')
                    index += 2
                }
                'u' -> if (index + 5 < raw.length) {
                    val code = raw.substring(index + 2, index + 6).toIntOrNull(16)
                    if (code != null) out.append(code.toChar()) else out.append(raw.substring(index, index + 6))
                    index += 6
                } else {
                    out.append(char)
                    index++
                }
                else -> {
                    out.append(next)
                    index += 2
                }
            }
        } else {
            out.append(char)
            index++
        }
    }
    return out.toString()
}
