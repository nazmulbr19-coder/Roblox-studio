package com.tetrazbloxy.mobilestudio.studio

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.tetrazbloxy.mobilestudio.core.RecentEntry
import com.tetrazbloxy.mobilestudio.model.BasePart
import com.tetrazbloxy.mobilestudio.model.CFrame
import com.tetrazbloxy.mobilestudio.model.Color3
import com.tetrazbloxy.mobilestudio.model.DataModel
import com.tetrazbloxy.mobilestudio.model.EnumValue
import com.tetrazbloxy.mobilestudio.model.Enums
import com.tetrazbloxy.mobilestudio.model.Folder
import com.tetrazbloxy.mobilestudio.model.Instance
import com.tetrazbloxy.mobilestudio.model.InstanceFactory
import com.tetrazbloxy.mobilestudio.model.LocalScript
import com.tetrazbloxy.mobilestudio.model.Model
import com.tetrazbloxy.mobilestudio.model.Part
import com.tetrazbloxy.mobilestudio.model.Script
import com.tetrazbloxy.mobilestudio.model.UDim2
import com.tetrazbloxy.mobilestudio.model.Vector3
import com.tetrazbloxy.mobilestudio.model.Workspace
import com.tetrazbloxy.mobilestudio.net.RobloxCloud
import com.tetrazbloxy.mobilestudio.playtest.PlaytestSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

/** All editor commands, wired to [StudioSession] and the core data model. */
class StudioController(private val session: StudioSession) {
    private var hiddenBeforePlay: Boolean? = null

    // ---- Document lifecycle ---------------------------------------------

    fun guardDiscard(after: () -> Unit) {
        if (!session.dirty) {
            after()
            return
        }
        session.dialog = AppDialog.Unsaved(
            onSave = {
                session.dialog = null
                session.afterSave = after
                session.requestSave?.invoke()
            },
            onDiscard = {
                session.dialog = null
                after()
            }
        )
    }

    fun newPlace(template: PlaceTemplate, confirm: Boolean = true) {
        val create = {
            if (session.playing) stopPlay()
            session.document = PlaceFactory.newPlace(template)
            session.documentName = when (template) {
                PlaceTemplate.BASEPLATE -> "Baseplate"
                PlaceTemplate.CLASSIC -> "Classic Place"
                PlaceTemplate.EMPTY -> "Empty Place"
            }
            session.fileUri = null
            session.fileIsXml = true
            session.cloudPlace = null
            session.selection = emptyList()
            session.openScripts.clear()
            session.activeScript = null
            session.showEditor = false
            session.undo.clear()
            session.dirty = false
            session.autosave.clear()
            session.focusCamera(Vector3(0f, 4f, 0f), 46f)
            session.touchScene()
            session.showStudio()
            session.notify("Created ${template.title} place")
        }
        if (confirm) guardDiscard(create) else create()
    }

    fun openRecent(entry: RecentEntry) {
        if (entry.uri.isBlank()) {
            session.dialog = AppDialog.Message(
                "Not available",
                "This recent entry has no local file. Open it from My Games instead."
            )
            return
        }
        guardDiscard { openUri(Uri.parse(entry.uri)) }
    }

    fun openUri(uri: Uri) {
        if (session.playing) stopPlay()
        session.scope.launch {
            val name = withContext(Dispatchers.IO) {
                DocumentFiles.displayName(session.app, uri)
            }
            DocumentFiles.takePermission(session.app, uri)
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = DocumentFiles.read(session.app, uri)
                    DocumentCodec.decode(bytes) to name
                }
            }
            result.onSuccess { (document, displayName) ->
                adoptDocument(
                    document = document,
                    name = cleanName(displayName ?: "Opened Place"),
                    uri = uri,
                    placeId = 0L,
                    universeId = 0L
                )
            }.onFailure { exception ->
                session.dialog = AppDialog.Message(
                    "Could not open file",
                    exception.message ?: "The selected file is not a supported place."
                )
            }
        }
    }

    fun openBytes(
        bytes: ByteArray,
        name: String,
        uri: Uri? = null,
        placeId: Long = 0L,
        universeId: Long = 0L
    ) {
        session.scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { DocumentCodec.decode(bytes) }
            }
            result.onSuccess { document ->
                adoptDocument(document, cleanName(name), uri, placeId, universeId)
            }.onFailure { exception ->
                session.dialog = AppDialog.Message(
                    "Could not open place",
                    exception.message ?: "The place body could not be decoded."
                )
            }
        }
    }

    private fun adoptDocument(
        document: DataModel,
        name: String,
        uri: Uri?,
        placeId: Long,
        universeId: Long
    ) {
        session.document = document
        session.documentName = name
        session.fileUri = uri
        session.fileIsXml = true
        session.selection = emptyList()
        session.openScripts.clear()
        session.activeScript = null
        session.showEditor = false
        session.undo.clear()
        session.dirty = false
        session.autosave.clear()
        session.focusCamera(Vector3(0f, 4f, 0f), 46f)
        session.touchScene()
        session.showStudio()
        session.recents.add(
            name = name,
            placeId = placeId,
            universeId = universeId,
            uri = uri?.toString() ?: ""
        )
        session.notify("Loaded $name")
    }

    fun save() {
        if (session.playing) {
            session.dialog = AppDialog.Message(
                "Playtest running",
                "Stop the playtest before saving."
            )
            return
        }
        commitScripts()
        val uri = session.fileUri
        if (uri != null) {
            writeDocument(uri, session.documentName)
        } else {
            val saveAs = session.requestSaveAs
            if (saveAs == null) {
                session.notify("Saving is not available right now")
            } else {
                saveAs(DocumentFiles.suggestedName(session.documentName)) { picked ->
                    if (picked != null) writeDocument(picked, session.documentName)
                }
            }
        }
    }

    fun saveAs() {
        val saveAs = session.requestSaveAs
        if (saveAs == null) {
            session.notify("Saving is not available right now")
            return
        }
        saveAs(DocumentFiles.suggestedName(session.documentName)) { picked ->
            if (picked != null) {
                val display = DocumentFiles.displayName(session.app, picked)
                writeDocument(picked, cleanName(display ?: session.documentName))
            }
        }
    }

    fun writeDocument(uri: Uri, name: String) {
        val xml = DocumentCodec.encode(session.document, name)
        val bytes = xml.toByteArray(Charsets.UTF_8)
        session.scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { DocumentFiles.write(session.app, uri, bytes) }
            }
            result.onSuccess {
                DocumentFiles.takePermission(session.app, uri)
                session.fileUri = uri
                session.documentName = cleanName(name)
                session.fileIsXml = true
                session.dirty = false
                session.autosave.clear()
                session.recents.add(
                    name = session.documentName,
                    placeId = session.cloudPlace?.placeId ?: 0L,
                    universeId = session.cloudPlace?.universeId ?: 0L,
                    uri = uri.toString()
                )
                session.notify("Saved ${session.documentName}")
                session.markSaved()
            }.onFailure { exception ->
                session.dialog = AppDialog.Message(
                    "Save failed",
                    exception.message ?: "The document could not be written."
                )
            }
        }
    }

    fun exportSelection() {
        val selected = session.selection
        if (selected.isEmpty()) {
            session.notify("Select something to export")
            return
        }
        val export = session.requestExport
        if (export == null) {
            session.notify("Export is not available right now")
            return
        }
        val firstName = selected.first().name
        export(DocumentFiles.suggestedName(firstName, "rbxm")) { picked ->
            if (picked == null) return@export
            val xml = DocumentCodec.encodeSelection(selected, firstName)
            session.scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        DocumentFiles.write(session.app, picked, xml.toByteArray(Charsets.UTF_8))
                    }
                }
                result.onSuccess { session.notify("Exported $firstName") }
                    .onFailure {
                        session.dialog = AppDialog.Message(
                            "Export failed",
                            it.message ?: "Could not write the model."
                        )
                    }
            }
        }
    }

    fun saveSelectionToLibrary(name: String) {
        if (session.selection.isEmpty()) {
            session.notify("Select something to save")
            return
        }
        val xml = DocumentCodec.encodeSelection(session.selection, name)
        val saved = session.library.save(name, xml)
        if (saved == null) {
            session.notify("Could not save to My Models")
        } else {
            session.notify("Saved ${saved.name} to My Models")
        }
    }

    fun insertLibraryModel(model: LibraryModel) {
        val xml = session.library.read(model) ?: run {
            session.notify("Could not read ${model.name}")
            return
        }
        session.scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { DocumentCodec.decode(xml.toByteArray(Charsets.UTF_8)) }
            }
            result.onSuccess { document ->
                val roots = document.workspace.children.toList().toMutableList()
                if (roots.isEmpty()) {
                    roots.addAll(document.children.filter { it !is Workspace })
                }
                for (root in roots) {
                    root.parent = null
                    insert(root)
                }
                if (roots.isEmpty()) session.notify("${model.name} was empty")
            }.onFailure {
                session.notify("Could not load ${model.name}")
            }
        }
    }

    // ---- Selection and editing ------------------------------------------

    fun selectAll() {
        session.setSelection(session.document.workspace.getDescendants())
    }

    fun clearSelection() {
        session.setSelection(emptyList())
    }

    fun copySelection() {
        if (session.selection.isEmpty()) return
        session.clipboard = session.selection.map { it.clone() }
        session.notify("Copied ${session.clipboard.size} instance(s)")
    }

    fun cutSelection() {
        copySelection()
        deleteSelection()
    }

    fun paste() {
        if (session.clipboard.isEmpty()) {
            session.notify("Clipboard is empty")
            return
        }
        val parent = insertionParent()
        val copies = session.clipboard.map { it.clone() }
        for (copy in copies) {
            translate(copy, Vector3(2f, 0f, 2f))
            parent.addChild(copy)
        }
        session.undo.record(
            { for (copy in copies) copy.parent = null; session.touchScene() },
            { for (copy in copies) parent.addChild(copy); session.touchScene() }
        )
        session.setSelection(copies)
        session.markDirty()
        session.notify("Pasted ${copies.size} instance(s)")
    }

    fun duplicateSelection() {
        if (session.selection.isEmpty()) return
        val copies = mutableListOf<Instance>()
        for (instance in session.selection) {
            val parent = instance.parent ?: session.document.workspace
            val copy = instance.clone()
            translate(copy, Vector3(2f, 0f, 2f))
            parent.addChild(copy)
            copies.add(copy)
        }
        session.undo.record(
            { for (copy in copies) copy.parent = null; session.touchScene() },
            { for (copy in copies) copy.parent?.addChild(copy); session.touchScene() }
        )
        session.setSelection(copies)
        session.markDirty()
        session.notify("Duplicated ${copies.size} instance(s)")
    }

    fun deleteSelection() {
        if (session.selection.isEmpty()) return
        val targets = session.selection.toList()
        val action = {
            val parents = targets.mapNotNull { it.parent }
            for (instance in targets) instance.parent = null
            session.undo.record(
                { for (instance in targets) instance.parent?.addChild(instance); session.touchScene() },
                { for (instance in targets) instance.parent = null; session.touchScene() }
            )
            session.setSelection(emptyList())
            session.markDirty()
            session.notify("Deleted ${targets.size} instance(s)")
        }
        if (session.settings.confirmDelete) {
            session.dialog = AppDialog.Confirm(
                title = "Delete selection?",
                message = "Delete ${targets.size} instance(s)?",
                positive = "Delete",
                onConfirm = {
                    session.dialog = null
                    action()
                }
            )
        } else {
            action()
        }
    }

    fun insert(instance: Instance, atCamera: Boolean = true) {
        val parent = insertionParent()
        if (atCamera) {
            translateTo(instance, session.camTarget)
        }
        parent.addChild(instance)
        session.undo.record(
            { instance.parent = null; session.touchScene() },
            { parent.addChild(instance); session.touchScene() }
        )
        session.setSelection(listOf(instance))
        session.markDirty()
        session.notify("Inserted ${instance.name}")
    }

    fun insertCatalogItem(item: CatalogItem) {
        insert(item.builder())
    }

    fun insertClass(className: String) {
        val instance = InstanceFactory.create(className)
        if (instance is BasePart) {
            when (instance.className) {
                "SpawnLocation" -> instance.size = Vector3(12f, 1f, 12f)
                "WedgePart" -> instance.size = Vector3(4f, 2f, 4f)
                "MeshPart" -> instance.size = Vector3(4f, 4f, 4f)
                else -> instance.size = Vector3(4f, 1f, 2f)
            }
            instance.anchored = true
        }
        if (instance is com.tetrazbloxy.mobilestudio.model.Script) {
            instance.source = "-- ${instance.className}\n"
        }
        insert(instance)
        if (instance is com.tetrazbloxy.mobilestudio.model.Script) openScript(instance)
    }

    fun insertAudio(item: AudioItem) {
        val sound = InstanceFactory.create("Sound")
        sound.name = item.name
        sound.setProperty("SoundId", "rbxassetid://${item.id}")
        sound.setProperty("Volume", 0.5f)
        sound.setProperty("Looped", false)
        val service = session.document.getService("SoundService")
        service.addChild(sound)
        session.undo.record(
            { sound.parent = null; session.touchScene() },
            { service.addChild(sound); session.touchScene() }
        )
        session.setSelection(listOf(sound))
        session.markDirty()
        session.notify("Inserted ${item.name} into SoundService")
    }

    fun importModelToLibrary(bytes: ByteArray, fileName: String) {
        session.scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { DocumentCodec.decode(bytes) }
            }
            result.onSuccess { document ->
                val roots = document.workspace.children.toList().ifEmpty {
                    document.children.filterNot { it is Workspace }
                }
                if (roots.isEmpty()) {
                    session.notify("$fileName contained no instances")
                    return@onSuccess
                }
                val name = cleanName(fileName)
                val xml = DocumentCodec.encodeSelection(roots, name)
                val saved = session.library.save(name, xml)
                if (saved == null) {
                    session.notify("Could not import $fileName")
                } else {
                    session.notify("Imported ${saved.name} into My Models")
                }
            }.onFailure { exception ->
                session.dialog = AppDialog.Message(
                    "Import failed",
                    exception.message ?: "The model could not be decoded."
                )
            }
        }
    }

    fun newPart(shape: Int = Enums.PartType.BLOCK.value) {
        val part = Part(
            when (shape) {
                Enums.PartType.SPHERE.value -> "Sphere"
                Enums.PartType.CYLINDER.value -> "Cylinder"
                Enums.PartType.WEDGE.value -> "Wedge"
                else -> "Part"
            }
        )
        part.shape = shape
        part.size = when (shape) {
            Enums.PartType.SPHERE.value -> Vector3(4f, 4f, 4f)
            Enums.PartType.CYLINDER.value -> Vector3(3f, 6f, 3f)
            else -> Vector3(4f, 1f, 2f)
        }
        part.anchored = true
        insert(part)
    }

    fun newScript(className: String = "Script") {
        val script = when (className) {
            "LocalScript" -> LocalScript("LocalScript")
            "ModuleScript" -> com.tetrazbloxy.mobilestudio.model.ModuleScript("ModuleScript")
            else -> Script("Script")
        }
        script.source = "-- ${script.className}\n"
        val parent = session.selection.firstOrNull() as? Model
            ?: session.document.getService("ServerScriptService")
        parent.addChild(script)
        session.undo.record(
            { script.parent = null; session.touchScene() },
            { parent.addChild(script); session.touchScene() }
        )
        session.setSelection(listOf(script))
        session.markDirty()
        openScript(script)
    }

    fun newFolder() {
        val folder = Folder("Folder")
        insert(folder)
    }

    fun newModel() {
        val model = Model("Model")
        insert(model)
    }

    fun newGui(className: String) {
        val gui = InstanceFactory.create(className)
        gui.name = className
        gui.setProperty("Size", UDim2.fromScale(0.42f, 0.3f))
        gui.setProperty("Position", UDim2.fromScale(0.29f, 0.35f))
        gui.setProperty("BackgroundColor3", Color3.fromRGB(30, 34, 46))
        val parent = session.document.getService("StarterGui")
        parent.addChild(gui)
        session.undo.record(
            { gui.parent = null; session.touchScene() },
            { parent.addChild(gui); session.touchScene() }
        )
        session.setSelection(listOf(gui))
        session.markDirty()
        session.notify("Inserted $className into StarterGui")
    }

    fun groupSelection() {
        if (session.selection.isEmpty()) return
        val parent = session.selection.firstOrNull()?.parent ?: session.document.workspace
        val model = Model("Model")
        val targets = session.selection.toList()
        parent.addChild(model)
        for (instance in targets) model.addChild(instance)
        session.undo.record(
            {
                for (instance in targets) parent.addChild(instance)
                model.parent = null
                session.touchScene()
            },
            {
                parent.addChild(model)
                for (instance in targets) model.addChild(instance)
                session.touchScene()
            }
        )
        session.setSelection(listOf(model))
        session.markDirty()
        session.notify("Grouped ${targets.size} instance(s)")
    }

    fun ungroupSelection() {
        val model = session.selection.singleOrNull() as? Model ?: run {
            session.notify("Select one Model to ungroup")
            return
        }
        val parent = model.parent ?: session.document.workspace
        val children = model.children.toList()
        for (child in children) parent.addChild(child)
        model.parent = null
        session.undo.record(
            {
                parent.addChild(model)
                for (child in children) model.addChild(child)
                session.touchScene()
            },
            {
                for (child in children) parent.addChild(child)
                model.parent = null
                session.touchScene()
            }
        )
        session.setSelection(children)
        session.markDirty()
        session.notify("Ungrouped ${model.name}")
    }

    fun solidOperation(operation: String) {
        when (operation) {
            "Union" -> {
                groupSelection()
                (session.selection.singleOrNull() as? Model)?.let {
                    it.name = "Union"
                    it.setProperty("SolidOperation", "Union")
                }
                session.notify("Union stored as a grouped model")
            }
            "Negate" -> {
                for (instance in session.selection) {
                    instance.setProperty("Negated", true)
                }
                session.markDirty()
                session.notify("Marked selection as negated")
            }
            "Separate" -> ungroupSelection()
        }
    }

    fun weldSelection() {
        val parts = session.selection.filterIsInstance<BasePart>()
        if (parts.size < 2) {
            session.notify("Select at least two parts to weld")
            return
        }
        val anchor = parts.first()
        for (index in 1 until parts.size) {
            val weld = InstanceFactory.create("WeldConstraint")
            weld.name = "WeldConstraint"
            weld.setProperty("Part0", anchor.name)
            weld.setProperty("Part1", parts[index].name)
            anchor.addChild(weld)
        }
        session.markDirty()
        session.notify("Welded ${parts.size} parts")
    }

    fun rename(instance: Instance, newName: String) {
        val name = newName.trim()
        if (name.isEmpty() || name == instance.name) return
        val previous = instance.name
        session.undo.record(
            { instance.name = previous; session.touchScene() },
            { instance.name = name; session.touchScene() }
        )
        instance.name = name
        session.markDirty()
        session.notify("Renamed to $name")
    }

    fun reparent(instance: Instance, newParent: Instance) {
        if (instance === newParent) return
        if (newParent.isDescendantOf(instance)) {
            session.notify("Cannot move a parent into its own child")
            return
        }
        val previous = instance.parent ?: run {
            session.notify("That instance has no parent")
            return
        }
        if (previous === newParent) return
        session.undo.record(
            { previous.addChild(instance); session.touchScene() },
            { newParent.addChild(instance); session.touchScene() }
        )
        newParent.addChild(instance)
        session.markDirty()
        session.notify("Moved ${instance.name} to ${newParent.name}")
    }

    fun <T> editProperty(
        instance: Instance,
        label: String,
        current: T,
        value: T,
        apply: (T) -> Unit
    ) {
        if (current == value) return
        session.undo.record(
            { apply(current); session.touchScene() },
            { apply(value); session.touchScene() }
        )
        apply(value)
        session.markDirty()
        session.notify("$label updated")
    }

    fun setPartColor(part: BasePart, color: Color3) {
        editProperty(part, "Color", part.color, color) { part.color = it }
    }

    fun setPartMaterial(part: BasePart, material: EnumValue) {
        editProperty(part, "Material", part.material, material) { part.material = it }
    }

    fun setTransparency(part: BasePart, value: Float) {
        editProperty(part, "Transparency", part.transparency, value.coerceIn(0f, 1f)) {
            part.transparency = it
        }
    }

    fun setAnchored(part: BasePart, value: Boolean) {
        editProperty(part, "Anchored", part.anchored, value) { part.anchored = it }
    }

    fun setShape(part: BasePart, shape: Int) {
        editProperty(part, "Shape", part.shape, shape) { part.shape = it }
    }

    fun moveSelection(delta: Vector3) {
        val parts = session.selection.filterIsInstance<BasePart>()
        if (parts.isEmpty()) {
            session.notify("Select a part to move")
            return
        }
        val step = if (session.settings.moveSnapOn) {
            Vector3(
                com.tetrazbloxy.mobilestudio.core.Snap.snap(delta.x, session.settings.moveSnap),
                com.tetrazbloxy.mobilestudio.core.Snap.snap(delta.y, session.settings.moveSnap),
                com.tetrazbloxy.mobilestudio.core.Snap.snap(delta.z, session.settings.moveSnap)
            )
        } else {
            delta
        }
        session.undo.begin("Move")
        for (part in parts) {
            val previous = part.cframe
            val next = previous + step
            session.undo.record(
                { part.cframe = previous; session.touchScene() },
                { part.cframe = next; session.touchScene() }
            )
            part.cframe = next
        }
        session.undo.commit()
        session.markDirty()
    }

    fun rotateSelection(degrees: Float) {
        val parts = session.selection.filterIsInstance<BasePart>()
        if (parts.isEmpty()) {
            session.notify("Select a part to rotate")
            return
        }
        val angle = if (session.settings.rotSnapOn) {
            com.tetrazbloxy.mobilestudio.core.Snap.snap(degrees, session.settings.rotSnap)
        } else {
            degrees
        }
        val radians = Math.toRadians(angle.toDouble()).toFloat()
        val rotation = CFrame.Angles(0f, radians, 0f)
        session.undo.begin("Rotate")
        for (part in parts) {
            val previous = part.cframe
            val next = previous * rotation
            session.undo.record(
                { part.cframe = previous; session.touchScene() },
                { part.cframe = next; session.touchScene() }
            )
            part.cframe = next
        }
        session.undo.commit()
        session.markDirty()
    }

    fun scaleSelection(factor: Float) {
        val parts = session.selection.filterIsInstance<BasePart>()
        if (parts.isEmpty()) return
        session.undo.begin("Scale")
        for (part in parts) {
            val previous = part.size
            val next = previous * factor
            session.undo.record(
                { part.size = previous; session.touchScene() },
                { part.size = next; session.touchScene() }
            )
            part.size = next
        }
        session.undo.commit()
        session.markDirty()
    }

    fun setSelectionPosition(part: BasePart, position: Vector3) {
        editProperty(part, "Position", part.cframe.position, position) {
            part.cframe = part.cframe.copy(x = it.x, y = it.y, z = it.z)
        }
    }

    fun setSelectionSize(part: BasePart, axis: Int, value: Float) {
        val next = when (axis) {
            0 -> part.size.copy(x = value.coerceAtLeast(0.05f))
            1 -> part.size.copy(y = value.coerceAtLeast(0.05f))
            else -> part.size.copy(z = value.coerceAtLeast(0.05f))
        }
        editProperty(part, "Size", part.size, next) { part.size = it }
    }

    fun focusSelection() {
        val part = session.selection.firstOrNull() as? BasePart
        if (part == null) {
            session.focusCamera(Vector3(0f, 4f, 0f), 46f)
        } else {
            val size = part.size.magnitude.coerceAtLeast(4f)
            session.focusCamera(part.cframe.position, size * 3.2f)
        }
        session.touchScene()
    }

    fun undo() {
        if (session.undo.undo()) {
            session.markDirty()
            session.notify("Undo")
        } else {
            session.notify("Nothing to undo")
        }
    }

    fun redo() {
        if (session.undo.redo()) {
            session.markDirty()
            session.notify("Redo")
        } else {
            session.notify("Nothing to redo")
        }
    }

    // ---- Scripts --------------------------------------------------------

    fun openScript(script: Script) {
        if (session.openScripts.none { it === script }) {
            session.openScripts.add(script)
        }
        session.activeScript = script
        session.showEditor = true
        session.setSelection(listOf(script))
    }

    fun closeScript(script: Script) {
        val index = session.openScripts.indexOfFirst { it === script }
        if (index < 0) return
        session.openScripts.removeAt(index)
        if (session.activeScript === script) {
            session.activeScript = session.openScripts.lastOrNull()
        }
        if (session.openScripts.isEmpty()) {
            session.showEditor = false
            session.activeScript = null
        }
    }

    fun updateScriptSource(script: Script, source: String) {
        if (script.source == source) return
        script.source = source
        session.markDirty()
    }

    fun commitScripts() {
        val active = session.activeScript
        if (active != null) {
            session.markDirty()
        }
    }

    // ---- Playtest -------------------------------------------------------

    fun togglePlay() {
        if (session.playing) stopPlay() else startPlay()
    }

    fun startPlay() {
        if (session.playing) return
        commitScripts()
        session.showEditor = false
        val world = session.document.clone() as? DataModel
        if (world == null) {
            session.notify("Could not start the playtest")
            return
        }
        if (session.settings.hideWindowsInPlaytest && !session.dock.getAllHidden()) {
            hiddenBeforePlay = session.dock.getAllHidden()
            session.dock.setAllHidden(true)
        }
        val play = PlaytestSession(world, session.avatarKind, session.scope)
        session.play = play
        session.playing = true
        play.start()
        session.output.info("Playtest started (${session.avatarKind.title})")
        session.notify("Playtest started")
    }

    fun stopPlay() {
        val play = session.play
        play?.stop()
        session.play = null
        session.playing = false
        session.audio.stopAll()
        hiddenBeforePlay?.let {
            session.dock.setAllHidden(it)
            hiddenBeforePlay = null
        }
        session.output.info("Playtest stopped")
        session.notify("Playtest stopped")
    }

    // ---- Cloud ----------------------------------------------------------

    fun downloadCloudGame(game: RobloxCloud.CloudGame) {
        session.scope.launch {
            session.notify("Downloading ${game.name}…")
            val result = session.cloud.downloadPlace(game.placeId)
            if (!result.ok || result.data == null) {
                session.dialog = AppDialog.Message(
                    "Download failed",
                    result.error ?: "Roblox did not return a place."
                )
                return@launch
            }
            guardDiscard {
                openBytes(
                    result.data,
                    game.name,
                    placeId = game.placeId,
                    universeId = game.universeId
                )
                session.cloudPlace = game
            }
        }
    }

    fun saveToRoblox() {
        val game = session.cloudPlace
        if (game == null) {
            session.dialog = AppDialog.Message(
                "No cloud place",
                "Open a game from My Games first, or use Publish to Roblox."
            )
            return
        }
        if (session.playing) {
            session.notify("Stop the playtest before saving")
            return
        }
        session.scope.launch {
            session.notify("Saving to Roblox…")
            val bytes = DocumentCodec.encode(session.document, game.name).toByteArray()
            val result = session.cloud.publishPlace(
                game.placeId,
                game.universeId,
                game.name,
                bytes
            )
            if (result.ok) {
                session.dirty = false
                session.notify("Saved ${game.name} to Roblox")
            } else {
                session.dialog = AppDialog.Message(
                    "Save to Roblox failed",
                    result.error ?: "Roblox rejected the upload."
                )
            }
        }
    }

    fun publishAs() {
        session.dialog = AppDialog.Prompt(
            title = "Publish to Roblox",
            label = "Game name",
            initial = session.documentName
        ) { name ->
            session.dialog = null
            if (!name.isNullOrBlank()) {
                session.scope.launch {
                    session.notify("Creating Roblox game…")
                    val created = session.cloud.createPlace(name)
                    val game = created.data
                    if (game == null) {
                        session.dialog = AppDialog.Message(
                            "Publish failed",
                            created.error ?: "Roblox did not return a place id."
                        )
                        return@launch
                    }
                    val bytes = DocumentCodec.encode(session.document, name).toByteArray()
                    val published = session.cloud.publishPlace(
                        game.placeId,
                        game.universeId,
                        name,
                        bytes
                    )
                    if (published.ok) {
                        session.cloudPlace = game
                        session.documentName = name
                        session.dirty = false
                        session.notify("Published $name")
                    } else {
                        session.dialog = AppDialog.Message(
                            "Publish failed",
                            published.error ?: "The place was created but not uploaded."
                        )
                    }
                }
            }
        }
    }

    fun showVersionHistory(game: RobloxCloud.CloudGame) {
        session.scope.launch {
            val result = session.cloud.versionHistory(game.universeId, game.placeId)
            val versions = result.data
            if (versions == null) {
                session.dialog = AppDialog.Message(
                    "Version history",
                    result.error ?: "No versions were returned."
                )
                return@launch
            }
            val text = if (versions.isEmpty()) {
                "No saved versions were returned."
            } else {
                versions.joinToString("\n") { version ->
                    "v${version.version}  ${version.date.take(10)}"
                }
            }
            session.dialog = AppDialog.Message("Version history — ${game.name}", text)
        }
    }

    fun installPluginFromRoblox(input: String) {
        val id = com.tetrazbloxy.mobilestudio.net.AssetIds.parseAssetId(input)
        if (id == null) {
            session.notify("That is not a Roblox asset id or link")
            return
        }
        session.scope.launch {
            session.notify("Downloading plugin $id…")
            val bytes = withContext(Dispatchers.IO) { session.assets.requestBlocking(id) }
            if (bytes == null) {
                session.dialog = AppDialog.Message(
                    "Plugin download failed",
                    "Could not download asset $id. Check your connection."
                )
                return@launch
            }
            if (session.pluginHost.install(bytes, "plugin-$id")) {
                session.notify("Installed plugin $id")
            }
        }
    }

    fun installPluginBytes(bytes: ByteArray, fileName: String) {
        if (session.pluginHost.install(bytes, fileName)) {
            session.notify("Installed $fileName")
        }
    }

    // ---- Helpers --------------------------------------------------------

    private fun insertionParent(): Instance {
        val candidate = session.selection.firstOrNull()
        return when {
            candidate is Workspace -> candidate
            candidate is Model -> candidate
            candidate is Folder -> candidate
            candidate != null && candidate.className == "DataModel" -> session.document.workspace
            candidate != null && candidate.parent != null &&
                com.tetrazbloxy.mobilestudio.model.ClassDb.isServiceName(candidate.className) -> candidate
            else -> session.document.workspace
        }
    }

    private fun translate(instance: Instance, delta: Vector3) {
        if (instance is BasePart) {
            instance.cframe += delta
        }
        for (child in instance.children) translate(child, delta)
    }

    private fun translateTo(instance: Instance, target: Vector3) {
        if (instance is BasePart) {
            instance.cframe = CFrame.new(target)
            return
        }
        val parts = if (instance is BasePart) emptyList() else instance.getDescendants().filterIsInstance<BasePart>()
        val centre = if (parts.isEmpty()) Vector3.ZERO else {
            val sum = parts.fold(Vector3.ZERO) { acc, part -> acc + part.cframe.position }
            sum / parts.size.toFloat()
        }
        translate(instance, target - centre)
    }

    private fun cleanName(name: String): String {
        val trimmed = name.substringAfterLast('/').substringAfterLast('\\')
        val withoutExtension = trimmed.substringBeforeLast('.', trimmed)
        return withoutExtension.ifBlank { "Untitled Place" }
    }
}
