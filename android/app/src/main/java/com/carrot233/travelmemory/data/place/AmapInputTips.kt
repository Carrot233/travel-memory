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

fun parsePlaceSuggestions(body: String): List<PlaceSuggestion> {
    return extractJsonObjects(body).mapNotNull { it.toSuggestion() }.take(5)
}

private fun String.toSuggestion(): PlaceSuggestion? {
    val name = jsonStringField(this, "name")?.takeIf { it.isNotBlank() } ?: return null
    val latitude = jsonNumberField(this, "latitude") ?: return null
    val longitude = jsonNumberField(this, "longitude") ?: return null
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

internal fun extractJsonObjects(json: String): List<String> {
    val objects = mutableListOf<String>()
    var index = 0
    while (index < json.length) {
        if (json[index] == '{') {
            val end = matchingBrace(json, index) ?: break
            objects += json.substring(index, end + 1)
            index = end + 1
        } else {
            index++
        }
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

private fun jsonNumberField(objectText: String, field: String): Double? {
    val pattern = Regex(""""${Regex.escape(field)}"\s*:\s*(-?\d+(?:\.\d+)?)""")
    return pattern.find(objectText)?.groupValues?.get(1)?.toDoubleOrNull()
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

internal fun jsStringLiteral(value: String): String {
    val out = StringBuilder("\"")
    value.forEach { char ->
        when (char) {
            '\\' -> out.append("\\\\")
            '"' -> out.append("\\\"")
            '\n' -> out.append("\\n")
            '\r' -> out.append("\\r")
            '<' -> out.append("\\u003c")
            else -> out.append(char)
        }
    }
    out.append('"')
    return out.toString()
}
