package com.aismartmath.board.ui.boards

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import com.aismartmath.board.data.Folder
import com.aismartmath.board.ui.components.PopIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aismartmath.board.data.BoardMeta
import com.aismartmath.board.data.BoardStore
import com.aismartmath.board.model.Topics
import com.aismartmath.board.ui.components.IconBtn
import com.aismartmath.board.ui.components.slab
import com.aismartmath.board.ui.components.RenameDialog
import com.aismartmath.board.ui.theme.BoardColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The saved boards. Open, rename, copy or delete a board; make folders and move boards into them; and with the
 * three dots (⋮) pick boards - or all of them - to delete or move together. Long-press a board to start picking.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BoardsScreen(store: BoardStore, onOpen: (BoardMeta) -> Unit, onClose: () -> Unit) {
    var tick by remember { mutableIntStateOf(0) }
    val all = remember(tick) { store.list() }
    val folders = remember(tick) { store.folders() }
    var openFolder by remember { mutableStateOf<String?>(null) }
    val cur = folders.firstOrNull { it.id == openFolder }
    val curId = cur?.id ?: ""
    val visible = all.filter { it.folder == curId }

    var menu by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var note by remember { mutableStateOf<String?>(null) }

    var renaming by remember { mutableStateOf<BoardMeta?>(null) }
    var deleting by remember { mutableStateOf<BoardMeta?>(null) }
    var newFolder by remember { mutableStateOf(false) }
    var renamingFolder by remember { mutableStateOf<Folder?>(null) }
    var deletingFolder by remember { mutableStateOf<Folder?>(null) }
    var confirmDeleteSel by remember { mutableStateOf(false) }
    var confirmDeleteAll by remember { mutableStateOf(false) }
    var moving by remember { mutableStateOf(false) }
    var moveToNew by remember { mutableStateOf(false) }

    fun stopSelecting() {
        selecting = false
        selected = emptySet()
    }

    BackHandler(enabled = menu || selecting || cur != null) {
        when {
            menu -> menu = false
            selecting -> stopSelecting()
            else -> openFolder = null
        }
    }
    LaunchedEffect(note) {
        if (note != null) {
            delay(2000)
            note = null
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BoardColors.Bg)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(Modifier.fillMaxSize()) {
            // ---------------- top bar ----------------
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (selecting) {
                    IconBtn("✕") { stopSelecting() }
                    Text(
                        if (selected.isEmpty()) "Pick boards" else "${selected.size} selected",
                        color = Color(0xFFF1D3A5),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                    val allOn = visible.isNotEmpty() && selected.containsAll(visible.map { it.id })
                    IconBtn(if (allOn) "☑" else "☐", dim = visible.isEmpty()) {
                        selected = if (allOn) selected - visible.map { it.id }.toSet() else selected + visible.map { it.id }
                    }
                    IconBtn("📁", dim = selected.isEmpty()) { if (selected.isNotEmpty()) moving = true }
                    IconBtn("🗑", dim = selected.isEmpty()) { if (selected.isNotEmpty()) confirmDeleteSel = true }
                } else {
                    IconBtn("←") { if (cur != null) openFolder = null else onClose() }
                    Text(
                        if (cur != null) "📁 ${cur.name}" else "📂 My boards",
                        color = Color(0xFFF1D3A5),
                        fontSize = 19.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    IconBtn("⋮") { menu = !menu }
                }
            }
            // a hint line while picking
            AnimatedVisibility(selecting, enter = expandVertically(tween(180)) + fadeIn(), exit = shrinkVertically(tween(150)) + fadeOut()) {
                Text(
                    "Tap boards to pick them · ☐ picks all here · 📁 moves · 🗑 deletes",
                    color = BoardColors.Muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }

            if (visible.isEmpty() && (cur != null || folders.isEmpty())) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (cur != null) "This folder is empty.\nLong-press a board, pick it, then tap 📁 to move it here."
                        else "No saved boards yet.\nOpen a topic and draw — boards save by themselves.",
                        color = BoardColors.Muted,
                        fontSize = 15.sp,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (cur == null) {
                        itemsIndexed(folders, key = { _, f -> "f" + f.id }) { i, f ->
                            PopIn(minOf(i, 8), Modifier.fillMaxWidth()) {
                                FolderRow(
                                    f, all.count { it.folder == f.id }, selecting,
                                    onOpen = { openFolder = f.id },
                                    onRename = { renamingFolder = f },
                                    onDelete = { deletingFolder = f },
                                )
                            }
                        }
                    }
                    itemsIndexed(visible, key = { _, b -> b.id }) { i, b ->
                        PopIn(minOf(i + folders.size, 8), Modifier.fillMaxWidth()) {
                            BoardRow(
                                b,
                                selecting = selecting,
                                picked = b.id in selected,
                                onOpen = {
                                    if (selecting) selected = if (b.id in selected) selected - b.id else selected + b.id
                                    else onOpen(b)
                                },
                                onPick = {
                                    selecting = true
                                    selected = selected + b.id
                                },
                                onRename = { renaming = b },
                                onCopy = {
                                    store.duplicate(b.id)
                                    tick++
                                },
                                onDelete = { deleting = b },
                            )
                        }
                    }
                }
            }
        }

        // ---------------- the ⋮ menu ----------------
        AnimatedVisibility(menu, enter = fadeIn(tween(120)), exit = fadeOut(tween(100))) {
            Box(Modifier.fillMaxSize().clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { menu = false })
        }
        AnimatedVisibility(
            menu,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 58.dp, end = 12.dp),
            enter = scaleIn(tween(180), initialScale = 0.7f, transformOrigin = TransformOrigin(1f, 0f)) + fadeIn(tween(150)),
            exit = scaleOut(tween(130), targetScale = 0.8f, transformOrigin = TransformOrigin(1f, 0f)) + fadeOut(tween(110)),
        ) {
            Column(
                Modifier
                    .widthIn(min = 210.dp)
                    .slab(RoundedCornerShape(14.dp), Color(0xFF2E1E15), Color(0xFF6B4630), 12.dp)
                    .padding(vertical = 6.dp),
            ) {
                MenuItem("📁", "New folder") { menu = false; newFolder = true }
                MenuItem("☑", "Select boards", enabled = visible.isNotEmpty()) {
                    menu = false
                    selecting = true
                }
                MenuItem("✅", "Select all boards", enabled = visible.isNotEmpty()) {
                    menu = false
                    selecting = true
                    selected = visible.map { it.id }.toSet()
                }
                MenuItem("🗑", "Delete all boards", enabled = all.isNotEmpty(), danger = true) {
                    menu = false
                    confirmDeleteAll = true
                }
                if (cur != null) {
                    MenuItem("✎", "Rename this folder") { menu = false; renamingFolder = cur }
                    MenuItem("🗑", "Delete this folder", danger = true) { menu = false; deletingFolder = cur }
                }
            }
        }

        // a little message at the bottom
        AnimatedVisibility(
            note != null,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
            enter = slideInVertically(tween(200)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(150)),
        ) {
            Text(
                note ?: "",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.slab(RoundedCornerShape(20.dp), Color(0xFF3A2519), Color(0xFF6B4630), 8.dp).padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }

    // ---------------- dialogs ----------------
    renaming?.let { b ->
        RenameDialog(
            initial = b.name,
            onDismiss = { renaming = null },
            onSave = { name ->
                store.rename(b.id, name)
                renaming = null
                tick++
            },
        )
    }
    deleting?.let { b ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete this board?") },
            text = { Text("“${b.name}” will be removed from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        store.delete(b.id)
                        deleting = null
                        tick++
                    },
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
    if (newFolder) {
        RenameDialog(
            initial = "",
            title = "New folder",
            onDismiss = { newFolder = false },
            onSave = { name ->
                store.newFolder(name)
                newFolder = false
                note = "Folder “$name” made"
                tick++
            },
        )
    }
    renamingFolder?.let { f ->
        RenameDialog(
            initial = f.name,
            title = "Rename folder",
            onDismiss = { renamingFolder = null },
            onSave = { name ->
                store.renameFolder(f.id, name)
                renamingFolder = null
                tick++
            },
        )
    }
    deletingFolder?.let { f ->
        val n = all.count { it.folder == f.id }
        AlertDialog(
            onDismissRequest = { deletingFolder = null },
            title = { Text("Delete folder “${f.name}”?") },
            text = { Text(if (n == 0) "The folder is empty." else "It has $n board${if (n == 1) "" else "s"}. Keep them in the main list, or delete them too?") },
            confirmButton = {
                Row {
                    if (n > 0) {
                        TextButton(
                            onClick = {
                                store.deleteFolder(f.id, true)
                                deletingFolder = null
                                if (openFolder == f.id) openFolder = null
                                note = "Folder and $n board${if (n == 1) "" else "s"} deleted"
                                tick++
                            },
                        ) { Text("Delete boards too") }
                    }
                    TextButton(
                        onClick = {
                            store.deleteFolder(f.id, false)
                            deletingFolder = null
                            if (openFolder == f.id) openFolder = null
                            note = if (n > 0) "Folder deleted — boards moved to the main list" else "Folder deleted"
                            tick++
                        },
                    ) { Text(if (n > 0) "Keep boards" else "Delete") }
                }
            },
            dismissButton = { TextButton(onClick = { deletingFolder = null }) { Text("Cancel") } },
        )
    }
    if (confirmDeleteSel) {
        val n = selected.size
        val everything = n == all.size
        AlertDialog(
            onDismissRequest = { confirmDeleteSel = false },
            title = { Text(if (everything) "Delete all boards?" else "Delete $n board${if (n == 1) "" else "s"}?") },
            text = { Text(if (everything) "Every saved board will be removed from this device. This cannot be undone." else "They will be removed from this device. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        store.deleteAll(selected)
                        confirmDeleteSel = false
                        note = "Deleted $n board${if (n == 1) "" else "s"}"
                        stopSelecting()
                        tick++
                    },
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteSel = false }) { Text("Cancel") } },
        )
    }
    if (confirmDeleteAll) {
        val n = all.size
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("Delete all $n board${if (n == 1) "" else "s"}?") },
            text = { Text("Every saved board, including the ones inside folders, will be removed from this device. The folders stay, empty. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        store.deleteAll(all.map { it.id })
                        confirmDeleteAll = false
                        note = "Deleted all $n board${if (n == 1) "" else "s"}"
                        stopSelecting()
                        tick++
                    },
                ) { Text("Delete all") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteAll = false }) { Text("Cancel") } },
        )
    }
    if (moving) {
        val n = selected.size
        AlertDialog(
            onDismissRequest = { moving = false },
            title = { Text("Move $n board${if (n == 1) "" else "s"} to …") },
            text = {
                Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                    if (curId != "") MoveChoice("📂", "Main list") {
                        store.move(selected, "")
                        note = "Moved to the main list"
                        moving = false
                        stopSelecting()
                        tick++
                    }
                    folders.filter { it.id != curId }.forEach { f ->
                        MoveChoice("📁", f.name) {
                            store.move(selected, f.id)
                            note = "Moved to “${f.name}”"
                            moving = false
                            stopSelecting()
                            tick++
                        }
                    }
                    MoveChoice("＋", "New folder …") {
                        moving = false
                        moveToNew = true
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { moving = false }) { Text("Cancel") } },
        )
    }
    if (moveToNew) {
        RenameDialog(
            initial = "",
            title = "New folder",
            onDismiss = { moveToNew = false },
            onSave = { name ->
                val f = store.newFolder(name)
                store.move(selected, f.id)
                note = "Moved to “$name”"
                moveToNew = false
                stopSelecting()
                tick++
            },
        )
    }
}

@Composable
private fun MenuItem(icon: String, label: String, enabled: Boolean = true, danger: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(icon, fontSize = 17.sp, color = Color.White.copy(alpha = if (enabled) 1f else 0.35f))
        Text(
            label,
            fontSize = 15.sp,
            color = (if (danger) Color(0xFFFF8A7A) else Color.White).copy(alpha = if (enabled) 1f else 0.35f),
        )
    }
}

@Composable
private fun MoveChoice(icon: String, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(icon, fontSize = 18.sp)
        Text(label, fontSize = 16.sp)
    }
}

@Composable
private fun FolderRow(f: Folder, count: Int, selecting: Boolean, onOpen: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .widthIn(max = 700.dp)
            .slab(shape, Color(0xFF3A2519), Color(0xFF8A5A38), 8.dp)
            .clickable(enabled = !selecting, onClick = onOpen)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(40.dp).slab(RoundedCornerShape(10.dp), BoardColors.Indigo, Color(0xFF5C3D28), 4.dp),
            contentAlignment = Alignment.Center,
        ) { Text("📁", fontSize = 22.sp, color = Color.White) }
        Column(Modifier.weight(1f)) {
            Text(f.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (count == 1) "1 board" else "$count boards", color = BoardColors.Muted, fontSize = 12.sp)
        }
        if (!selecting) {
            IconBtn("✎", compact = true) { onRename() }
            IconBtn("🗑", compact = true) { onDelete() }
            Text("›", color = BoardColors.Muted, fontSize = 22.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BoardRow(
    b: BoardMeta,
    selecting: Boolean,
    picked: Boolean,
    onOpen: () -> Unit,
    onPick: () -> Unit,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    val topic = Topics.byId[b.topicId]
    val date = remember(b.updated) { SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(b.updated)) }
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .widthIn(max = 700.dp)
            .slab(shape, if (picked) Color(0xFF4A2E1C) else Color(0xFF2E1E15), if (picked) BoardColors.Indigo else Color(0xFF5C3D28), 8.dp)
            .combinedClickable(onClick = onOpen, onLongClick = { if (!selecting) onPick() })
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AnimatedVisibility(selecting, enter = scaleIn(tween(160)) + expandHorizontallyCompat(), exit = scaleOut(tween(120)) + shrinkHorizontallyCompat()) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (picked) BoardColors.Indigo else Color.Transparent)
                    .border(2.dp, if (picked) BoardColors.Indigo else BoardColors.Muted, CircleShape),
                contentAlignment = Alignment.Center,
            ) { if (picked) Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        }
        Box(
            Modifier.size(40.dp).slab(RoundedCornerShape(10.dp), topic?.color ?: BoardColors.Indigo, Color(0xFF5C3D28), 4.dp),
            contentAlignment = Alignment.Center,
        ) { Text(topic?.icon ?: "▦", fontSize = 22.sp, color = Color.White) }
        Column(Modifier.weight(1f)) {
            Text(b.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                (topic?.name ?: "Board") + " · " + b.count + " figures · " + date,
                color = BoardColors.Muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!selecting) {
            IconBtn("✎", compact = true) { onRename() }
            IconBtn("⧉", compact = true) { onCopy() }
            IconBtn("🗑", compact = true) { onDelete() }
        }
    }
}

private fun expandHorizontallyCompat() = androidx.compose.animation.expandHorizontally(tween(160))
private fun shrinkHorizontallyCompat() = androidx.compose.animation.shrinkHorizontally(tween(120))
