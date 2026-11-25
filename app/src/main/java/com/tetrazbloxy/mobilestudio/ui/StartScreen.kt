package com.tetrazbloxy.mobilestudio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.tetrazbloxy.mobilestudio.core.RecentEntry
import com.tetrazbloxy.mobilestudio.net.RobloxCloud
import com.tetrazbloxy.mobilestudio.studio.PlaceTemplate
import com.tetrazbloxy.mobilestudio.studio.StudioController
import com.tetrazbloxy.mobilestudio.studio.StudioSession
import com.tetrazbloxy.mobilestudio.ui.StartCache.Kind
import java.util.Calendar

enum class StartPage(val title: String, val glyph: Glyph) {
    HOME("Home", Glyph.HOME),
    RECENT("Recent", Glyph.CLOCK),
    MY_GAMES("My Games", Glyph.CUBE),
    SHARED("Shared With Me", Glyph.PEOPLE),
    GROUPS("Group Games", Glyph.LAYERS)
}

@Composable
fun StartScreen(session: StudioSession, controller: StudioController) {
    val palette = LocalStudioPalette.current
    var page by remember { mutableStateOf(StartPage.HOME) }
    var refresh by remember { mutableIntStateOf(0) }
    val account = session.accounts
    val signedIn = account.signedIn
    val userId = account.userId

    var mine by remember(userId) {
        mutableStateOf(session.startCache.load(Kind.MINE, userId))
    }
    var shared by remember(userId) {
        mutableStateOf(session.startCache.load(Kind.SHARED, userId))
    }
    var groups by remember(userId) {
        mutableStateOf(session.startCache.loadGroups(userId))
    }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(page, signedIn, userId, refresh) {
        val cloudPage = page == StartPage.MY_GAMES || page == StartPage.SHARED ||
            page == StartPage.GROUPS
        if (!signedIn || !cloudPage) return@LaunchedEffect
        loading = true
        error = null
        when (page) {
            StartPage.MY_GAMES -> {
                val result = session.cloud.myGames()
                if (result.data != null) {
                    mine = result.data
                    session.startCache.save(Kind.MINE, userId, mine)
                } else {
                    error = result.error ?: "Could not load your games."
                }
            }
            StartPage.SHARED -> {
                val result = session.cloud.sharedGames()
                if (result.data != null) {
                    shared = result.data
                    session.startCache.save(Kind.SHARED, userId, shared)
                } else {
                    error = result.error ?: "Could not load shared games."
                }
            }
            StartPage.GROUPS -> {
                val result = session.cloud.groups()
                if (result.data != null) {
                    groups = result.data
                    session.startCache.saveGroups(userId, groups)
                } else {
                    error = result.error ?: "Could not load group games."
                }
            }
            else -> Unit
        }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(palette.bg)) {
        if (session.isPhone) {
            StartPhoneHeader(
                session = session,
                page = page,
                onPage = { page = it }
            )
        }
        Row(modifier = Modifier.fillMaxSize()) {
            if (!session.isPhone) {
                StartSidebar(
                    session = session,
                    page = page,
                    onPage = { page = it }
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (session.isPhone) 16.dp else 26.dp, vertical = 18.dp)
            ) {
                when (page) {
                    StartPage.HOME -> StartHome(
                        session = session,
                        controller = controller,
                        onOpenPage = { page = it }
                    )
                    StartPage.RECENT -> StartRecent(session, controller)
                    StartPage.MY_GAMES -> StartGames(
                        session = session,
                        controller = controller,
                        title = "My Games",
                        subtitle = "Places you own on Roblox",
                        games = mine,
                        loading = loading,
                        error = error,
                        signedIn = signedIn,
                        onRefresh = { refresh++ },
                        onSignIn = { session.sheet = AppSheet.ACCOUNT }
                    )
                    StartPage.SHARED -> StartGames(
                        session = session,
                        controller = controller,
                        title = "Shared With Me",
                        subtitle = "Team Create places shared with your account",
                        games = shared,
                        loading = loading,
                        error = error,
                        signedIn = signedIn,
                        onRefresh = { refresh++ },
                        onSignIn = { session.sheet = AppSheet.ACCOUNT }
                    )
                    StartPage.GROUPS -> StartGroups(
                        session = session,
                        controller = controller,
                        groups = groups,
                        loading = loading,
                        error = error,
                        signedIn = signedIn,
                        onRefresh = { refresh++ },
                        onSignIn = { session.sheet = AppSheet.ACCOUNT }
                    )
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun StartSidebar(
    session: StudioSession,
    page: StartPage,
    onPage: (StartPage) -> Unit
) {
    val palette = LocalStudioPalette.current
    Column(
        modifier = Modifier
            .width(226.dp)
            .fillMaxSize()
            .background(palette.panel)
            .border(
                width = 0.dp,
                color = Color.Transparent,
                shape = RoundedCornerShape(0.dp)
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.accent),
                contentAlignment = Alignment.Center
            ) {
                StudioIcon(Glyph.CUBE, Color.White, size = 23.dp)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "Mobile Studio",
                    color = palette.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text("v2.5", color = palette.dim, fontSize = 10.sp)
            }
        }

        Spacer(Modifier.height(24.dp))
        for (entry in StartPage.entries) {
            StartNavRow(entry, entry == page) { onPage(entry) }
            Spacer(Modifier.height(3.dp))
        }

        Spacer(Modifier.weight(1f))
        StudioDivider()
        Spacer(Modifier.height(10.dp))
        AccountChip(session) { session.sheet = AppSheet.ACCOUNT }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StudioIconButton(
                glyph = Glyph.SETTINGS,
                contentDescription = "Studio settings",
                onClick = { session.sheet = AppSheet.SETTINGS },
                tint = palette.dim,
                size = 32.dp,
                iconSize = 17.dp
            )
            StudioIconButton(
                glyph = Glyph.PLUGIN,
                contentDescription = "Plugins",
                onClick = { session.sheet = AppSheet.PLUGINS },
                tint = palette.dim,
                size = 32.dp,
                iconSize = 17.dp
            )
            StudioIconButton(
                glyph = Glyph.HELP,
                contentDescription = "About",
                onClick = { session.sheet = AppSheet.ABOUT },
                tint = palette.dim,
                size = 32.dp,
                iconSize = 17.dp
            )
            Spacer(Modifier.weight(1f))
            StudioIconButton(
                glyph = Glyph.FILE,
                contentDescription = "Open a file",
                onClick = { session.requestOpenFile?.invoke() },
                tint = palette.dim,
                size = 32.dp,
                iconSize = 17.dp
            )
        }
    }
}

@Composable
private fun StartPhoneHeader(
    session: StudioSession,
    page: StartPage,
    onPage: (StartPage) -> Unit
) {
    val palette = LocalStudioPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.panel)
            .padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.accent),
                contentAlignment = Alignment.Center
            ) {
                StudioIcon(Glyph.CUBE, Color.White, size = 19.dp)
            }
            Spacer(Modifier.width(9.dp))
            Text(
                "Mobile Studio",
                color = palette.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            StudioIconButton(
                glyph = Glyph.FILE,
                contentDescription = "Open a file",
                onClick = { session.requestOpenFile?.invoke() },
                tint = palette.dim,
                size = 32.dp,
                iconSize = 17.dp
            )
            StudioIconButton(
                glyph = Glyph.SETTINGS,
                contentDescription = "Studio settings",
                onClick = { session.sheet = AppSheet.SETTINGS },
                tint = palette.dim,
                size = 32.dp,
                iconSize = 17.dp
            )
            AccountChip(session) { session.sheet = AppSheet.ACCOUNT }
        }
        Spacer(Modifier.height(9.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            for (entry in StartPage.entries) {
                val active = entry == page
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (active) palette.select else Color.Transparent)
                        .clickable { onPage(entry) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StudioIcon(entry.glyph, if (active) palette.accent else palette.dim, size = 17.dp)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        entry.title,
                        color = if (active) palette.text else palette.dim,
                        fontSize = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun StartNavRow(page: StartPage, active: Boolean, onClick: () -> Unit) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) palette.select else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StudioIcon(page.glyph, if (active) palette.accent else palette.dim, size = 18.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            page.title,
            color = if (active) palette.text else palette.dim,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun StartHome(
    session: StudioSession,
    controller: StudioController,
    onOpenPage: (StartPage) -> Unit
) {
    val palette = LocalStudioPalette.current
    Text(
        greeting(),
        color = palette.text,
        fontSize = 26.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.5).sp
    )
    Spacer(Modifier.height(4.dp))
    Text(
        "Pick up where you left off, or start something new.",
        color = palette.dim,
        fontSize = 12.sp
    )
    Spacer(Modifier.height(18.dp))

    HomeTipCard(session)
    Spacer(Modifier.height(22.dp))

    SectionHeading("New place", "Templates")
    Spacer(Modifier.height(9.dp))
    GridCards(PlaceTemplate.entries, columns = if (session.isPhone) 1 else 3) { template ->
        TemplateCard(template) { controller.newPlace(template) }
    }

    Spacer(Modifier.height(22.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        SectionHeading("Recent places", "Your device", modifier = Modifier.weight(1f))
        StudioButton(
            "See all",
            onClick = { onOpenPage(StartPage.RECENT) },
            compact = true,
            glyph = Glyph.CHEVRON_RIGHT
        )
    }
    Spacer(Modifier.height(9.dp))
    val recents = session.recents.list().take(12)
    if (recents.isEmpty()) {
        EmptyState(
            glyph = Glyph.CLOCK,
            title = "No recent places",
            subtitle = "Places you open or save will appear here."
        )
    } else {
        GridCards(recents, columns = if (session.isPhone) 1 else 2) { entry ->
            RecentCard(entry, session.images) { controller.openRecent(entry) }
        }
    }

    Spacer(Modifier.height(22.dp))
    SectionHeading("Cloud", "Roblox")
    Spacer(Modifier.height(9.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        CloudShortcut(
            glyph = Glyph.CUBE,
            title = "My Games",
            subtitle = if (session.accounts.signedIn) "Browse your places" else "Sign in first",
            modifier = Modifier.weight(1f)
        ) { onOpenPage(StartPage.MY_GAMES) }
        CloudShortcut(
            glyph = Glyph.PEOPLE,
            title = "Shared",
            subtitle = "Team Create places",
            modifier = Modifier.weight(1f)
        ) { onOpenPage(StartPage.SHARED) }
        if (!session.isPhone) {
            CloudShortcut(
                glyph = Glyph.LAYERS,
                title = "Groups",
                subtitle = "Group-owned games",
                modifier = Modifier.weight(1f)
            ) { onOpenPage(StartPage.GROUPS) }
        }
    }

    Spacer(Modifier.height(18.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.card)
            .clickable { session.requestOpenFile?.invoke() }
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StudioIcon(Glyph.FILE, palette.accent, size = 24.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Open a file from this device", color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text("XML RBXLX and RBXMX documents are fully editable.", color = palette.dim, fontSize = 10.sp)
        }
        StudioIcon(Glyph.CHEVRON_RIGHT, palette.dim, size = 17.dp)
    }
}

@Composable
private fun HomeTipCard(session: StudioSession) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.card)
            .border(1.dp, palette.border, RoundedCornerShape(16.dp))
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlyphBadge(Glyph.BOLT, palette.accent, size = 42.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Tip: orbit with one finger, pinch to zoom",
                color = palette.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Double-tap the viewport to focus the selection. Long-press an Explorer row for actions.",
                color = palette.dim,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
        if (!session.isPhone) {
            Spacer(Modifier.width(10.dp))
            StatusPill("2.5", palette.info)
        }
    }
}

@Composable
private fun SectionHeading(
    title: String,
    hint: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalStudioPalette.current
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text(title, color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(8.dp))
        Text(hint, color = palette.dim, fontSize = 10.sp, modifier = Modifier.padding(bottom = 1.dp))
    }
}

@Composable
private fun TemplateCard(template: PlaceTemplate, onClick: () -> Unit) {
    val palette = LocalStudioPalette.current
    val accent = Color(template.accent)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.card)
            .border(1.dp, palette.border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(78.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.11f)),
            contentAlignment = Alignment.Center
        ) {
            StudioIcon(
                when (template) {
                    PlaceTemplate.BASEPLATE -> Glyph.GRID
                    PlaceTemplate.CLASSIC -> Glyph.SHAPES
                    PlaceTemplate.EMPTY -> Glyph.FILE
                },
                accent,
                size = 34.dp
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(accent.copy(alpha = 0.18f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(template.name, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(template.title, color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(template.description, color = palette.dim, fontSize = 10.sp, lineHeight = 14.sp)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StudioIcon(Glyph.PLUS, accent, size = 14.dp)
            Spacer(Modifier.width(4.dp))
            Text("Create", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun RecentCard(entry: RecentEntry, images: com.tetrazbloxy.mobilestudio.net.ImageLoader, onClick: () -> Unit) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(palette.card)
            .border(1.dp, palette.border, RoundedCornerShape(13.dp))
            .clickable(onClick = onClick)
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RemoteImage(
            loader = images,
            source = if (entry.placeId != 0L) {
                "https://www.roblox.com/asset-thumbnail/image?assetId=${entry.placeId}&width=150&height=150&format=png"
            } else {
                null
            },
            contentDescription = entry.name,
            modifier = Modifier.size(52.dp),
            fallbackGlyph = Glyph.CUBE
        )
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.name,
                color = palette.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                formatRelativeTime(entry.time),
                color = palette.dim,
                fontSize = 10.sp
            )
            if (entry.uri.isNotBlank()) {
                Text(
                    entry.uri.substringAfterLast('/'),
                    color = palette.dim.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        StudioIcon(Glyph.CHEVRON_RIGHT, palette.dim, size = 15.dp)
    }
}

@Composable
private fun CloudShortcut(
    glyph: Glyph,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(palette.card)
            .border(1.dp, palette.border, RoundedCornerShape(13.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlyphBadge(glyph, palette.info, size = 36.dp)
        Spacer(Modifier.width(9.dp))
        Column {
            Text(title, color = palette.text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = palette.dim, fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
private fun StartRecent(session: StudioSession, controller: StudioController) {
    val palette = LocalStudioPalette.current
    Text("Recent places", color = palette.text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(3.dp))
    Text("Local files and cloud places opened on this device.", color = palette.dim, fontSize = 12.sp)
    Spacer(Modifier.height(16.dp))

    val entries = session.recents.list()
    if (entries.isEmpty()) {
        EmptyState(
            glyph = Glyph.CLOCK,
            title = "Nothing here yet",
            subtitle = "Open a place or create one from a template to start your history.",
            action = {
                StudioButton("Open a file", onClick = { session.requestOpenFile?.invoke() }, primary = true, glyph = Glyph.FILE)
            }
        )
        return
    }

    GridCards(entries, columns = if (session.isPhone) 1 else 2) { entry ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                RecentCard(entry, session.images) { controller.openRecent(entry) }
            }
            Spacer(Modifier.width(6.dp))
            StudioIconButton(
                glyph = Glyph.TRASH,
                contentDescription = "Remove from recents",
                onClick = {
                    session.recents.remove(entry)
                    session.notify("Removed ${entry.name}")
                },
                tint = palette.dim,
                size = 32.dp,
                iconSize = 16.dp
            )
        }
    }
}

@Composable
private fun StartGames(
    session: StudioSession,
    controller: StudioController,
    title: String,
    subtitle: String,
    games: List<RobloxCloud.CloudGame>,
    loading: Boolean,
    error: String?,
    signedIn: Boolean,
    onRefresh: () -> Unit,
    onSignIn: () -> Unit
) {
    val palette = LocalStudioPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = palette.text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = palette.dim, fontSize = 12.sp)
        }
        if (signedIn) {
            StudioIconButton(
                glyph = Glyph.REFRESH,
                contentDescription = "Refresh",
                onClick = onRefresh,
                tint = palette.accent,
                size = 34.dp,
                iconSize = 18.dp
            )
        }
    }
    Spacer(Modifier.height(16.dp))

    if (!signedIn) {
        EmptyState(
            glyph = Glyph.ACCOUNT,
            title = "Sign in to Roblox",
            subtitle = "Use a .ROBLOSECURITY cookie or Open Cloud API key to browse and open cloud places.",
            action = { StudioButton("Sign in", onClick = onSignIn, primary = true, glyph = Glyph.ACCOUNT) }
        )
        return
    }

    if (loading && games.isEmpty()) {
        LoadingRow("Loading $title…")
        return
    }
    if (error != null) {
        ErrorCard(error) { onRefresh() }
        Spacer(Modifier.height(10.dp))
    }
    if (games.isEmpty()) {
        EmptyState(
            glyph = Glyph.CUBE,
            title = "No games found",
            subtitle = "Create a place and publish it from the file menu, then refresh.",
            action = {
                StudioButton("Refresh", onClick = onRefresh, compact = true, glyph = Glyph.REFRESH)
            }
        )
        return
    }

    GridCards(games, columns = if (session.isPhone) 1 else 2) { game ->
        CloudGameCard(
            game = game,
            session = session,
            onOpen = { controller.downloadCloudGame(game) },
            onHistory = { controller.showVersionHistory(game) }
        )
    }
}

@Composable
private fun StartGroups(
    session: StudioSession,
    controller: StudioController,
    groups: List<RobloxCloud.GameGroup>,
    loading: Boolean,
    error: String?,
    signedIn: Boolean,
    onRefresh: () -> Unit,
    onSignIn: () -> Unit
) {
    val palette = LocalStudioPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Group Games", color = palette.text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text("Places owned by your Roblox groups.", color = palette.dim, fontSize = 12.sp)
        }
        if (signedIn) {
            StudioIconButton(
                glyph = Glyph.REFRESH,
                contentDescription = "Refresh",
                onClick = onRefresh,
                tint = palette.accent,
                size = 34.dp,
                iconSize = 18.dp
            )
        }
    }
    Spacer(Modifier.height(16.dp))

    if (!signedIn) {
        EmptyState(
            glyph = Glyph.ACCOUNT,
            title = "Sign in to Roblox",
            subtitle = "Your group memberships are needed to list group games.",
            action = { StudioButton("Sign in", onClick = onSignIn, primary = true, glyph = Glyph.ACCOUNT) }
        )
        return
    }
    if (loading && groups.isEmpty()) {
        LoadingRow("Loading groups…")
        return
    }
    if (error != null) {
        ErrorCard(error) { onRefresh() }
        Spacer(Modifier.height(10.dp))
    }
    if (groups.isEmpty()) {
        EmptyState(
            glyph = Glyph.LAYERS,
            title = "No group games",
            subtitle = "Join a group with published places, then refresh.",
        )
        return
    }

    for (group in groups) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(palette.card)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlyphBadge(Glyph.PEOPLE, palette.accent, size = 38.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(group.name, color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (group.role.isBlank()) "Member" else "${group.role} · rank ${group.rank}",
                    color = palette.dim,
                    fontSize = 10.sp
                )
            }
            StatusPill("${group.games.size} places", palette.info)
        }
        Spacer(Modifier.height(8.dp))
        if (group.games.isEmpty()) {
            Text(
                "No places in ${group.name}.",
                color = palette.dim,
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
            )
        } else {
            GridCards(group.games, columns = if (session.isPhone) 1 else 2) { game ->
                CloudGameCard(
                    game = game,
                    session = session,
                    onOpen = { controller.downloadCloudGame(game) },
                    onHistory = { controller.showVersionHistory(game) }
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun CloudGameCard(
    game: RobloxCloud.CloudGame,
    session: StudioSession,
    onOpen: () -> Unit,
    onHistory: () -> Unit
) {
    val palette = LocalStudioPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.card)
            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RemoteImage(
                loader = session.images,
                source = game.thumbnailUrl ?: session.cloud.thumbnailUrl(game.placeId),
                contentDescription = game.name,
                modifier = Modifier.size(58.dp),
                fallbackGlyph = Glyph.CUBE
            )
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    game.name,
                    color = palette.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    game.creator.ifBlank { "Roblox" },
                    color = palette.dim,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (game.playing > 0) StatusPill("${game.playing} playing", palette.play)
                    if (game.visits > 0) StatusPill(formatCount(game.visits), palette.info)
                }
            }
        }
        ThinDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StudioButton("Open", onOpen, compact = true, glyph = Glyph.DOWNLOAD)
            Spacer(Modifier.weight(1f))
            StudioIconButton(
                glyph = Glyph.CLOCK,
                contentDescription = "Version history",
                onClick = onHistory,
                tint = palette.dim,
                size = 30.dp,
                iconSize = 15.dp
            )
        }
    }
}

@Composable
private fun LoadingRow(text: String) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.card)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StudioIcon(Glyph.REFRESH, palette.accent, size = 20.dp)
        Spacer(Modifier.width(10.dp))
        Text(text, color = palette.dim, fontSize = 12.sp)
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.error.copy(alpha = 0.1f))
            .border(1.dp, palette.error.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StudioIcon(Glyph.WARNING, palette.error, size = 18.dp)
        Spacer(Modifier.width(9.dp))
        Text(message, color = palette.error, fontSize = 11.sp, modifier = Modifier.weight(1f))
        StudioButton("Retry", onRetry, compact = true, glyph = Glyph.REFRESH)
    }
}

@Composable
private fun <T> GridCards(
    items: List<T>,
    columns: Int,
    content: @Composable (T) -> Unit
) {
    val safeColumns = columns.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        for (row in items.chunked(safeColumns)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                for (item in row) {
                    Box(modifier = Modifier.weight(1f)) {
                        content(item)
                    }
                }
                repeat(safeColumns - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        in 18..22 -> "Good evening"
        else -> "Late night build?"
    }
}

private fun formatCount(value: Long): String = when {
    value >= 1_000_000 -> "%.1fM".format(value / 1_000_000f)
    value >= 1_000 -> "%.1fK".format(value / 1_000f)
    else -> value.toString()
}
