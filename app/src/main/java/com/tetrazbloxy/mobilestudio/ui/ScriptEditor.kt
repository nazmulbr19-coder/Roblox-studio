package com.tetrazbloxy.mobilestudio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tetrazbloxy.mobilestudio.model.Script
import com.tetrazbloxy.mobilestudio.studio.AppDialog
import com.tetrazbloxy.mobilestudio.studio.StudioController
import com.tetrazbloxy.mobilestudio.studio.StudioSession
import kotlin.math.roundToInt

private val LUA_KEYWORDS = listOf(
    "and", "break", "do", "else", "elseif", "end", "false", "for", "function",
    "if", "in", "local", "nil", "not", "or", "repeat", "return", "then", "true",
    "until", "while", "continue", "self"
)

private val LUA_GLOBALS = listOf(
    "game", "workspace", "script", "print", "warn", "error", "require", "pairs",
    "ipairs", "next", "select", "tostring", "tonumber", "type", "typeof", "wait",
    "task", "spawn", "delay", "Instance", "Enum", "Vector3", "CFrame", "Color3",
    "UDim2", "UDim", "Ray", "Region3", "TweenInfo", "NumberSequence", "ColorSequence"
)

private val LUA_LIBRARIES = listOf(
    "math", "string", "table", "os", "coroutine", "utf8", "bit32", "debug"
)

private val COMMON_MEMBERS = listOf(
    "Name", "Parent", "ClassName", "Archivable", "ClearAllChildren", "Clone",
    "Destroy", "FindFirstChild", "FindFirstChildOfClass", "GetChildren",
    "GetDescendants", "GetPropertyChangedSignal", "IsA", "WaitForChild",
    "Changed", "AncestryChanged", "ChildAdded", "DescendantAdded"
)

private val BASE_PART_MEMBERS = listOf(
    "Anchored", "BrickColor", "CFrame", "Color", "Material", "Orientation",
    "Position", "Rotation", "Size", "Transparency", "CanCollide", "Massless",
    "Touched", "TouchEnded"
)

private val SCRIPT_MEMBERS = listOf(
    "Source", "Disabled", "Enabled", "RunContext", "GetDebugInfo", "Error"
)

private class LuaHighlight(
    private val keyword: Color,
    private val number: Color,
    private val stringColor: Color,
    private val comment: Color,
    private val builtin: Color
) : VisualTransformation {
    private val keywordRegex = Regex(
        "\\b(" + LUA_KEYWORDS.joinToString("|") + ")\\b"
    )
    private val numberRegex = Regex("\\b\\d+(?:\\.\\d+)?\\b")
    private val stringRegex = Regex("\"[^\"\\n]*\"|'[^'\\n]*'")
    private val commentRegex = Regex("--[^\\n]*")
    private val builtinRegex = Regex("\\b(" + (LUA_GLOBALS + LUA_LIBRARIES).joinToString("|") + ")\\b")

    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text.text)
        for (match in commentRegex.findAll(text.text)) {
            builder.addStyle(SpanStyle(color = comment), match.range.first, match.range.last + 1)
        }
        for (match in stringRegex.findAll(text.text)) {
            builder.addStyle(SpanStyle(color = stringColor), match.range.first, match.range.last + 1)
        }
        for (match in numberRegex.findAll(text.text)) {
            builder.addStyle(SpanStyle(color = number), match.range.first, match.range.last + 1)
        }
        for (match in keywordRegex.findAll(text.text)) {
            builder.addStyle(
                SpanStyle(color = keyword, fontWeight = FontWeight.SemiBold),
                match.range.first,
                match.range.last + 1
            )
        }
        for (match in builtinRegex.findAll(text.text)) {
            builder.addStyle(SpanStyle(color = builtin), match.range.first, match.range.last + 1)
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

@Composable
fun ScriptEditorPane(session: StudioSession, controller: StudioController) {
    val palette = LocalStudioPalette.current
    val script = session.activeScript

    Column(modifier = Modifier.fillMaxSize().background(palette.editorBg)) {
        ScriptTabs(session, controller)
        if (script == null) {
            EmptyState(
                glyph = Glyph.CODE,
                title = "No script open",
                subtitle = "Select a Script, LocalScript or ModuleScript in the Explorer to edit it."
            )
            return@Column
        }
        ScriptWorkspace(session, controller, script)
    }
}

@Composable
private fun ScriptTabs(session: StudioSession, controller: StudioController) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(palette.header)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StudioIconButton(
            glyph = Glyph.ARROW_LEFT,
            contentDescription = "Back to viewport",
            onClick = { session.showEditor = false },
            tint = palette.dim,
            size = 34.dp,
            iconSize = 16.dp
        )
        for (script in session.openScripts) {
            val active = session.activeScript === script
            Row(
                modifier = Modifier
                    .height(30.dp)
                    .padding(end = 3.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (active) palette.panel else Color.Transparent)
                    .clickable { session.activeScript = script }
                    .padding(start = 9.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StudioIcon(
                    if (script.className == "ModuleScript") Glyph.FILE else Glyph.CODE,
                    if (active) palette.accent else palette.dim,
                    size = 13.dp
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    script.name,
                    color = if (active) palette.text else palette.dim,
                    fontSize = 10.sp,
                    maxLines = 1
                )
                Spacer(Modifier.width(4.dp))
                StudioIconButton(
                    glyph = Glyph.CLOSE,
                    contentDescription = "Close ${script.name}",
                    onClick = { controller.closeScript(script) },
                    tint = palette.dim,
                    size = 20.dp,
                    iconSize = 11.dp
                )
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun ScriptWorkspace(
    session: StudioSession,
    controller: StudioController,
    script: Script
) {
    val palette = LocalStudioPalette.current
    var field by remember(script) {
        mutableStateOf(TextFieldValue(script.source, TextRange(script.source.length)))
    }
    var fontSize by remember(script) {
        mutableFloatStateOf(session.settings.editorFontSize.coerceIn(9f, 26f))
    }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var suggestionStart by remember { mutableIntStateOf(0) }
    var findQuery by remember { mutableStateOf("") }
    var showFind by remember { mutableStateOf(false) }
    val highlight = remember(palette) {
        LuaHighlight(
            keyword = Color(0xFFC792EA),
            number = Color(0xFFF78C6C),
            stringColor = Color(0xFFC3E88D),
            comment = Color(0xFF7A8699),
            builtin = Color(0xFF82AAFF)
        )
    }

    fun commit(value: TextFieldValue) {
        field = value
        controller.updateScriptSource(script, value.text)
    }

    fun findNext() {
        val needle = findQuery.trim()
        if (needle.isEmpty()) return
        val text = field.text
        val start = (field.selection.end).coerceIn(0, text.length)
        val index = text.indexOf(needle, start, ignoreCase = true)
            .let { if (it < 0) text.indexOf(needle, 0, ignoreCase = true) else it }
        if (index >= 0) {
            field = field.copy(selection = TextRange(index, index + needle.length))
            session.notify("Found \"$needle\"")
        } else {
            session.notify("No matches for \"$needle\"")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.panel)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            StudioButton(
                "Save",
                {
                    controller.commitScripts()
                    controller.save()
                },
                compact = true,
                glyph = Glyph.SAVE
            )
            StudioIconButton(
                glyph = Glyph.SEARCH,
                contentDescription = "Find",
                onClick = { showFind = !showFind },
                tint = if (showFind) palette.accent else palette.dim,
                size = 30.dp,
                iconSize = 16.dp
            )
            StudioIconButton(
                glyph = Glyph.ARROW_DOWN,
                contentDescription = "Go to line",
                onClick = {
                    session.dialog = AppDialog.Prompt(
                        title = "Go to line",
                        label = "Line number",
                        initial = "1"
                    ) { input ->
                        session.dialog = null
                        val line = input?.toIntOrNull() ?: return@Prompt
                        val lines = field.text.split('\n')
                        val target = line.coerceIn(1, lines.size)
                        var index = 0
                        for (i in 0 until target - 1) {
                            index += lines[i].length + 1
                        }
                        field = field.copy(selection = TextRange(index))
                    }
                },
                tint = palette.dim,
                size = 30.dp,
                iconSize = 16.dp
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${script.className} · ${script.name}",
                color = palette.dim,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            StudioIconButton(
                glyph = Glyph.MINUS,
                contentDescription = "Smaller text",
                onClick = {
                    fontSize = (fontSize - 1f).coerceAtLeast(9f)
                    session.settings.editorFontSize = fontSize
                },
                tint = palette.dim,
                size = 28.dp,
                iconSize = 14.dp
            )
            Text(
                fontSize.roundToInt().toString(),
                color = palette.dim,
                fontSize = 9.sp
            )
            StudioIconButton(
                glyph = Glyph.PLUS,
                contentDescription = "Larger text",
                onClick = {
                    fontSize = (fontSize + 1f).coerceAtMost(26f)
                    session.settings.editorFontSize = fontSize
                },
                tint = palette.dim,
                size = 28.dp,
                iconSize = 14.dp
            )
        }

        if (showFind) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.panel)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchField(
                    value = findQuery,
                    onValueChange = { findQuery = it },
                    placeholder = "Find in script",
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                StudioButton("Find", { findNext() }, compact = true, glyph = Glyph.SEARCH)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState())
                    .verticalScroll(rememberScrollState())
            ) {
                val lineCount = field.text.count { it == '\n' } + 1
                val gutter = (1..lineCount).joinToString("\n")
                Text(
                    gutter,
                    style = editorTextStyle(fontSize, palette.dim.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .background(palette.editorGutter)
                        .padding(start = 9.dp, end = 8.dp, top = 10.dp, bottom = 40.dp),
                    maxLines = Int.MAX_VALUE
                )
                BasicTextField(
                    value = field,
                    onValueChange = { next ->
                        var adjusted = next
                        val previous = field.text
                        if (session.settings.autoCloseBrackets &&
                            next.text.length == previous.length + 1 &&
                            next.selection.collapsed
                        ) {
                            val cursor = next.selection.start
                            val inserted = if (cursor > 0) next.text.getOrNull(cursor - 1) else null
                            val closing = when (inserted) {
                                '(' -> ")"
                                '[' -> "]"
                                '{' -> "}"
                                '"' -> "\""
                                '\'' -> "'"
                                else -> null
                            }
                            if (closing != null) {
                                adjusted = TextFieldValue(
                                    next.text.substring(0, cursor) + closing +
                                        next.text.substring(cursor),
                                    TextRange(cursor)
                                )
                            }
                        }
                        if (session.settings.autoIndent &&
                            adjusted.text.length == previous.length + 1 &&
                            adjusted.selection.collapsed
                        ) {
                            val cursor = adjusted.selection.start
                            if (cursor > 0 && adjusted.text.getOrNull(cursor - 1) == '\n') {
                                val lineStart = previous.lastIndexOf('\n', (cursor - 2).coerceAtLeast(0)) + 1
                                val line = previous.substring(lineStart, (cursor - 1).coerceAtLeast(0))
                                val indent = line.takeWhile { it == ' ' || it == '\t' }
                                if (indent.isNotEmpty()) {
                                    adjusted = TextFieldValue(
                                        adjusted.text.substring(0, cursor) + indent +
                                            adjusted.text.substring(cursor),
                                        TextRange(cursor + indent.length)
                                    )
                                }
                            }
                        }
                        commit(adjusted)
                        if (session.settings.autocomplete) {
                            val completion = completeFor(adjusted.text, adjusted.selection.start)
                            suggestions = completion?.second ?: emptyList()
                            suggestionStart = completion?.first ?: 0
                        } else {
                            suggestions = emptyList()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 240.dp)
                        .padding(start = 10.dp, end = 12.dp, top = 10.dp, bottom = 40.dp),
                    textStyle = editorTextStyle(fontSize, palette.editorText),
                    cursorBrush = SolidColor(palette.accent),
                    visualTransformation = highlight,
                    maxLines = Int.MAX_VALUE,
                    decorationBox = { inner ->
                        if (field.text.isEmpty()) {
                            Text(
                                "-- Write Lua here",
                                style = editorTextStyle(fontSize, palette.dim.copy(alpha = 0.5f))
                            )
                        }
                        inner()
                    }
                )
            }

            if (suggestions.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 52.dp, top = 8.dp)
                        .widthIn(min = 170.dp, max = 280.dp),
                    color = palette.card,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, palette.border),
                    shadowElevation = 10.dp
                ) {
                    Column {
                        for (item in suggestions.take(8)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val cursor = field.selection.start.coerceIn(
                                            suggestionStart,
                                            field.text.length
                                        )
                                        val before = field.text.substring(0, suggestionStart)
                                        val after = field.text.substring(cursor)
                                        val next = before + item + after
                                        commit(
                                            TextFieldValue(
                                                next,
                                                TextRange(before.length + item.length)
                                            )
                                        )
                                        suggestions = emptyList()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StudioIcon(Glyph.CODE, palette.accent, size = 12.dp)
                                Spacer(Modifier.width(6.dp))
                                Text(item, color = palette.text, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.card.copy(alpha = 0.92f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val line = field.text.take(field.selection.start).count { it == '\n' } + 1
                val column = field.selection.start -
                    (field.text.lastIndexOf('\n', (field.selection.start - 1).coerceAtLeast(0)) + 1) + 1
                Text(
                    "Ln $line, Col $column",
                    color = palette.dim,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private fun completeFor(text: String, cursor: Int): Pair<Int, List<String>>? {
    if (cursor !in 0..text.length) return null
    val before = text.substring(0, cursor)
    val chain = Regex("([A-Za-z_][A-Za-z0-9_]*(?:[.:][A-Za-z_][A-Za-z0-9_]*)*)[.:]$")
        .find(before)
    if (chain != null) {
        val chainText = chain.groupValues[1]
        val members = membersOf(chainText)
        val start = cursor
        return start to members.take(12)
    }
    val match = Regex("[A-Za-z_][A-Za-z0-9_]*$").find(before) ?: return null
    if (match.value.length < 2) return null
    val prefix = match.value
    val locals = Regex("\\blocal\\s+([A-Za-z_][A-Za-z0-9_]*)")
        .findAll(text)
        .map { it.groupValues[1] }
        .filter { !it.equals(prefix, true) }
        .toList()
    val pool = (LUA_KEYWORDS + LUA_GLOBALS + LUA_LIBRARIES + COMMON_MEMBERS + locals)
    return (cursor - prefix.length) to pool
        .filter { it.startsWith(prefix, true) && !it.equals(prefix, true) }
        .distinct()
        .take(12)
}

private fun membersOf(chain: String): List<String> {
    val last = chain.substringAfterLast('.').substringAfterLast(':')
    return when (last) {
        "workspace", "Workspace" -> COMMON_MEMBERS + BASE_PART_MEMBERS + listOf(
            "Gravity", "CurrentCamera", "Terrain", "Raycast", "GetPartBoundsInBox"
        )
        "game" -> listOf(
            "Workspace", "Players", "Lighting", "ReplicatedStorage", "ServerScriptService",
            "StarterGui", "StarterPack", "SoundService", "GetService", "HttpService"
        )
        "script" -> SCRIPT_MEMBERS
        "math" -> listOf("abs", "ceil", "floor", "max", "min", "random", "sqrt", "sin", "cos", "pi", "huge")
        "string" -> listOf("format", "find", "gsub", "len", "lower", "upper", "rep", "split", "sub")
        "table" -> listOf("insert", "remove", "concat", "sort", "find", "create", "unpack")
        "task" -> listOf("wait", "spawn", "defer", "delay", "cancel")
        "Color3" -> listOf("new", "fromRGB", "fromHSV", "fromHex")
        "Vector3" -> listOf("new", "zero", "one", "xAxis", "yAxis", "zAxis")
        "CFrame" -> listOf("new", "Angles", "fromEulerAnglesXYZ", "lookAt", "identity")
        "UDim2" -> listOf("new", "fromScale", "fromOffset")
        "Enum" -> listOf("Material", "PartType", "RunContext", "EasingStyle", "CameraType")
        else -> COMMON_MEMBERS
    }
}
