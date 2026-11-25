package com.tetrazbloxy.mobilestudio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tetrazbloxy.mobilestudio.model.BrickColors
import com.tetrazbloxy.mobilestudio.model.ClassDb
import com.tetrazbloxy.mobilestudio.model.Color3
import com.tetrazbloxy.mobilestudio.studio.AppDialog
import com.tetrazbloxy.mobilestudio.studio.StudioController
import com.tetrazbloxy.mobilestudio.studio.StudioSession

@Composable
fun DialogHost(
    session: StudioSession,
    controller: StudioController,
    dialog: AppDialog
) {
    val palette = LocalStudioPalette.current
    when (dialog) {
        is AppDialog.Message -> AlertDialog(
            onDismissRequest = {
                session.dialog = null
                dialog.onDismiss()
            },
            containerColor = palette.card,
            titleContentColor = palette.text,
            textContentColor = palette.dim,
            title = { Text(dialog.title, fontWeight = FontWeight.SemiBold) },
            text = { Text(dialog.message, fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = {
                    session.dialog = null
                    dialog.onDismiss()
                }) { Text("OK", color = palette.accent) }
            }
        )

        is AppDialog.Confirm -> AlertDialog(
            onDismissRequest = {
                session.dialog = null
                dialog.onCancel()
            },
            containerColor = palette.card,
            titleContentColor = palette.text,
            textContentColor = palette.dim,
            title = { Text(dialog.title, fontWeight = FontWeight.SemiBold) },
            text = { Text(dialog.message, fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = { dialog.onConfirm() }) {
                    Text(dialog.positive, color = palette.stop)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    session.dialog = null
                    dialog.onCancel()
                }) { Text("Cancel", color = palette.dim) }
            }
        )

        is AppDialog.Prompt -> {
            var value by remember(dialog) { mutableStateOf(dialog.initial) }
            AlertDialog(
                onDismissRequest = { session.dialog = null },
                containerColor = palette.card,
                titleContentColor = palette.text,
                textContentColor = palette.dim,
                title = { Text(dialog.title, fontWeight = FontWeight.SemiBold) },
                text = {
                    Column {
                        Text(dialog.label, color = palette.dim, fontSize = 11.sp)
                        Spacer(Modifier.height(7.dp))
                        StudioField(value = value, onValueChange = { value = it })
                    }
                },
                confirmButton = {
                    TextButton(onClick = { dialog.onResult(value) }) {
                        Text("OK", color = palette.accent)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { session.dialog = null }) {
                        Text("Cancel", color = palette.dim)
                    }
                }
            )
        }

        is AppDialog.Number -> {
            var value by remember(dialog) { mutableStateOf(formatNumber(dialog.initial)) }
            AlertDialog(
                onDismissRequest = { session.dialog = null },
                containerColor = palette.card,
                titleContentColor = palette.text,
                textContentColor = palette.dim,
                title = { Text(dialog.title, fontWeight = FontWeight.SemiBold) },
                text = {
                    Column {
                        Text(dialog.label, color = palette.dim, fontSize = 11.sp)
                        Spacer(Modifier.height(7.dp))
                        StudioField(
                            value = value,
                            onValueChange = { value = it },
                            keyboardType = KeyboardType.Decimal
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        dialog.onResult(value.trim().toFloatOrNull())
                    }) { Text("OK", color = palette.accent) }
                },
                dismissButton = {
                    TextButton(onClick = { session.dialog = null }) {
                        Text("Cancel", color = palette.dim)
                    }
                }
            )
        }

        is AppDialog.ColorPick -> ColorPickerDialog(session, dialog)

        is AppDialog.PickClass -> ClassPickerDialog(session, dialog)

        is AppDialog.WebPermission -> AlertDialog(
            onDismissRequest = { session.dismissDialog() },
            containerColor = palette.card,
            titleContentColor = palette.text,
            textContentColor = palette.dim,
            title = { Text("Allow access to ${dialog.host}?", fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    if (dialog.who.isNullOrBlank()) {
                        "A script is requesting access to this host."
                    } else {
                        "${dialog.who} is requesting access to this host."
                    },
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    session.webPermissions.answer(dialog.host, true)
                    session.dialog = null
                }) { Text("Allow", color = palette.accent) }
            },
            dismissButton = {
                TextButton(onClick = {
                    session.webPermissions.answer(dialog.host, false)
                    session.dialog = null
                }) { Text("Block", color = palette.stop) }
            }
        )

        is AppDialog.Unsaved -> AlertDialog(
            onDismissRequest = { session.dialog = null },
            containerColor = palette.card,
            titleContentColor = palette.text,
            textContentColor = palette.dim,
            title = { Text("Unsaved changes", fontWeight = FontWeight.SemiBold) },
            text = { Text("Save changes before continuing?", fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = { dialog.onSave() }) { Text("Save", color = palette.accent) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { dialog.onDiscard() }) {
                        Text("Discard", color = palette.stop)
                    }
                    TextButton(onClick = { session.dialog = null }) {
                        Text("Cancel", color = palette.dim)
                    }
                }
            }
        )

        is AppDialog.Recovery -> AlertDialog(
            onDismissRequest = { session.dialog = null },
            containerColor = palette.card,
            titleContentColor = palette.text,
            textContentColor = palette.dim,
            title = { Text("Recover place?", fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    "An autosaved copy of ${dialog.name} was found (${formatRelativeTime(dialog.time)}). " +
                        "Recover it to continue where you left off.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { dialog.onRecover() }) {
                    Text("Recover", color = palette.accent)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { dialog.onDiscard() }) {
                        Text("Discard", color = palette.stop)
                    }
                    TextButton(onClick = { session.dialog = null }) {
                        Text("Cancel", color = palette.dim)
                    }
                }
            }
        )
    }
}

@Composable
private fun ColorPickerDialog(session: StudioSession, dialog: AppDialog.ColorPick) {
    val palette = LocalStudioPalette.current
    var red by remember(dialog) { mutableFloatStateOf(dialog.initial.r) }
    var green by remember(dialog) { mutableFloatStateOf(dialog.initial.g) }
    var blue by remember(dialog) { mutableFloatStateOf(dialog.initial.b) }
    var hex by remember(dialog) {
        mutableStateOf(dialog.initial.toHex())
    }

    fun syncHex() {
        hex = Color3(red, green, blue).toHex()
    }

    AlertDialog(
        onDismissRequest = { session.dialog = null },
        containerColor = palette.card,
        titleContentColor = palette.text,
        textContentColor = palette.dim,
        title = { Text(dialog.title, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(Color3(red, green, blue).toArgb()))
                )
                Spacer(Modifier.height(10.dp))
                ColorSlider("Red", red, palette.error) {
                    red = it
                    syncHex()
                }
                ColorSlider("Green", green, palette.play) {
                    green = it
                    syncHex()
                }
                ColorSlider("Blue", blue, palette.info) {
                    blue = it
                    syncHex()
                }
                Spacer(Modifier.height(6.dp))
                StudioField(value = hex, onValueChange = { input ->
                    hex = input
                    Color3.fromHex(input)?.let { parsed ->
                        red = parsed.r
                        green = parsed.g
                        blue = parsed.b
                    }
                })
                Spacer(Modifier.height(10.dp))
                Text("BrickColors", color = palette.dim, fontSize = 10.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScrollCompat(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (brick in BrickColors.table) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(Color(brick.color.toArgb()))
                                .clickable {
                                    red = brick.color.r
                                    green = brick.color.g
                                    blue = brick.color.b
                                    syncHex()
                                }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = Color3.fromHex(hex) ?: Color3(red, green, blue)
                dialog.onResult(parsed)
            }) { Text("OK", color = palette.accent) }
        },
        dismissButton = {
            TextButton(onClick = { session.dialog = null }) {
                Text("Cancel", color = palette.dim)
            }
        }
    )
}

@Composable
private fun Modifier.horizontalScrollCompat(): Modifier =
    this.then(androidx.compose.foundation.horizontalScroll(androidx.compose.foundation.rememberScrollState()))

@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    accent: Color,
    onChange: (Float) -> Unit
) {
    val palette = LocalStudioPalette.current
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = palette.dim, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("${(value * 255).toInt()}", color = accent, fontSize = 10.sp)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = palette.border
            )
        )
    }
}

@Composable
private fun ClassPickerDialog(session: StudioSession, dialog: AppDialog.PickClass) {
    val palette = LocalStudioPalette.current
    var query by remember { mutableStateOf("") }
    val classes = remember(query) {
        val names = (ClassDb.allClasses())
        if (query.isBlank()) names else names.filter { it.contains(query, true) }
    }
    AlertDialog(
        onDismissRequest = { session.dialog = null },
        containerColor = palette.card,
        titleContentColor = palette.text,
        textContentColor = palette.dim,
        title = { Text("Choose a class", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                SearchField(value = query, onValueChange = { query = it }, placeholder = "Filter classes")
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(classes) { name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dialog.onResult(name) }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StudioIcon(classGlyph(name), classAccent(name), size = 16.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(name, color = palette.text, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { session.dialog = null }) {
                Text("Cancel", color = palette.dim)
            }
        }
    )
}

private fun formatNumber(value: Float): String {
    if (value.isNaN() || value.isInfinite()) return "0"
    return if (value == value.toInt().toFloat()) value.toInt().toString() else value.toString()
}
