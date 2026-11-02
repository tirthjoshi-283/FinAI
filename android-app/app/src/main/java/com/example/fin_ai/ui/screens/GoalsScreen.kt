package com.example.fin_ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fin_ai.data.GoalDto
import com.example.fin_ai.insights.GoalMath
import com.example.fin_ai.ui.anim.ConfettiOverlay
import com.example.fin_ai.ui.anim.CountUpText
import com.example.fin_ai.ui.anim.ProgressRing
import com.example.fin_ai.ui.anim.appear
import com.example.fin_ai.ui.anim.popOn
import com.example.fin_ai.ui.theme.AccentCyan
import com.example.fin_ai.ui.theme.GreenCredit
import com.example.fin_ai.ui.theme.OutlineViolet
import com.example.fin_ai.ui.theme.SoftRed
import com.example.fin_ai.ui.theme.VioletLight
import com.example.fin_ai.ui.theme.VioletPrimary
import com.example.fin_ai.ui.util.Fmt
import com.example.fin_ai.ui.viewmodel.Celebration
import com.example.fin_ai.ui.viewmodel.GoalsViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val Amber = Color(0xFFF59E0B)
private val Gold = Color(0xFFFBBF24)

/** Which bottom sheet is open. */
private sealed interface GoalSheet {
    data class New(val template: Template? = null) : GoalSheet
    data class Edit(val goal: GoalDto) : GoalSheet
    data class Funds(val goal: GoalDto, val withdraw: Boolean) : GoalSheet
}

private data class Template(val emoji: String, val title: String, val target: Int)

private val TEMPLATES = listOf(
    Template("🛟", "Emergency fund", 50_000),
    Template("📱", "New phone", 30_000),
    Template("✈️", "Dream trip", 60_000),
    Template("🏍️", "New bike", 1_20_000),
    Template("💻", "Laptop", 70_000),
    Template("🎁", "Gift for family", 10_000),
)

private val EMOJIS = listOf("🎯", "🏍️", "🚗", "🏠", "💻", "📱", "✈️", "💍", "🛟", "🎓", "🎁", "🎮", "📷", "📈", "⌚", "🐶")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(viewModel: GoalsViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    var sheet by remember { mutableStateOf<GoalSheet?>(null) }
    var confirmDelete by remember { mutableStateOf<GoalDto?>(null) }
    var confetti by remember { mutableIntStateOf(0) }
    var banner by remember { mutableStateOf<Celebration?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.celebrations.collect { c ->
            confetti++
            banner = c
            delay(3800)
            if (banner == c) banner = null
        }
    }

    val active = state.goals.filter { !it.isComplete }.sortedByDescending { it.progress }
    val done = state.goals.filter { it.isComplete }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { sheet = GoalSheet.New() },
                    containerColor = VioletPrimary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("New goal", fontWeight = FontWeight.SemiBold) }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Column(Modifier.appear()) {
                        Text("Savings Goals", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text("Dream it, plan it, fund it.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
                when {
                    state.loading -> item { Notice("Loading your goals...") }
                    state.error != null && state.goals.isEmpty() -> item { Notice(state.error!!, isError = true, onRetry = viewModel::refresh) }
                    state.goals.isEmpty() -> item { EmptyGoals { t -> sheet = GoalSheet.New(t) } }
                    else -> {
                        item { GoalsHero(state.goals, state.pace, Modifier.appear(1)) }
                        if (active.isNotEmpty()) item { SectionTitle("In progress · ${active.size}") }
                        itemsIndexed(active, key = { _, g -> g.id }) { i, g ->
                            GoalCard(
                                goal = g,
                                pace = state.pace,
                                modifier = Modifier.appear(i + 2).popOn(if (state.lastChangedId == g.id) state.changeCount else null),
                                onQuickAdd = { amount -> viewModel.changeFunds(g, amount) },
                                onCustom = { withdraw -> sheet = GoalSheet.Funds(g, withdraw) },
                                onEdit = { sheet = GoalSheet.Edit(g) },
                                onDelete = { confirmDelete = g },
                            )
                        }
                        if (done.isNotEmpty()) {
                            item { SectionTitle("Completed 🏆 · ${done.size}") }
                            itemsIndexed(done, key = { _, g -> g.id }) { i, g ->
                                CompletedCard(g, Modifier.appear(i + 2), onEdit = { sheet = GoalSheet.Edit(g) }, onDelete = { confirmDelete = g })
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
        }

        // Celebration banner + confetti float above everything.
        AnimatedVisibility(
            visible = banner != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(16.dp)
        ) {
            banner?.let { CelebrationBanner(it) }
        }
        ConfettiOverlay(confetti)
    }

    sheet?.let { current ->
        ModalBottomSheet(
            onDismissRequest = { sheet = null; viewModel.clearSaveError() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            when (current) {
                is GoalSheet.New -> GoalEditor(
                    existing = null,
                    template = current.template,
                    saving = state.saving,
                    error = state.saveError,
                    onSave = { title, target, emoji, deadline -> viewModel.createGoal(title, target, emoji, deadline) { sheet = null } }
                )
                is GoalSheet.Edit -> GoalEditor(
                    existing = current.goal,
                    template = null,
                    saving = state.saving,
                    error = state.saveError,
                    onSave = { title, target, emoji, deadline -> viewModel.editGoal(current.goal, title, target, emoji, deadline) { sheet = null } }
                )
                is GoalSheet.Funds -> FundsEditor(
                    goal = current.goal,
                    startWithdraw = current.withdraw,
                    saving = state.saving,
                    error = state.saveError,
                    onSave = { amount -> viewModel.changeFunds(current.goal, amount) { sheet = null } }
                )
            }
        }
    }

    confirmDelete?.let { g ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            title = { Text("Delete \"${g.title}\"?", fontSize = 18.sp) },
            text = { Text("${Fmt.rupees(g.currentAmount)} saved so far. This can't be undone.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = { TextButton(onClick = { viewModel.deleteGoal(g.id); confirmDelete = null }) { Text("Delete", color = SoftRed) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        )
    }
}

@Composable
private fun GoalsHero(goals: List<GoalDto>, pace: Double, modifier: Modifier) {
    val saved = goals.sumOf { it.currentAmount.coerceAtMost(it.targetAmount) }
    val target = goals.sumOf { it.targetAmount }
    val progress = if (target > 0) (saved / target).toFloat() else 0f
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(VioletPrimary.copy(alpha = 0.45f), AccentCyan.copy(alpha = 0.18f))))
            .border(1.dp, OutlineViolet, RoundedCornerShape(26.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProgressRing(progress, 104.dp, 12.dp, listOf(VioletLight, AccentCyan, GreenCredit), Color.White.copy(alpha = 0.12f)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CountUpText(
                    progress * 100.0, { "${it.toInt()}%" },
                    TextStyle(color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                )
                Text("overall", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text("Total saved", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
            CountUpText(saved, { Fmt.rupees(it) }, TextStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp))
            Text("of ${Fmt.rupees(target)}", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            val doneCount = goals.count { it.isComplete }
            Text(
                "${goals.size - doneCount} active · $doneCount completed",
                color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium
            )
            if (pace > 0) {
                com.example.fin_ai.ui.util.AutoSizeText(
                    "You save ~${Fmt.rupees(pace)}/month", color = GreenCredit, fontSize = 12.sp,
                    fontWeight = FontWeight.Medium, minFontSize = 9.sp, modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalCard(
    goal: GoalDto,
    pace: Double,
    modifier: Modifier,
    onQuickAdd: (Double) -> Unit,
    onCustom: (withdraw: Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val p = remember(goal, pace) { GoalMath.project(goal, pace) }
    var menu by remember { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, OutlineViolet, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(goal.progress, 62.dp, 6.dp, listOf(VioletLight, AccentCyan), MaterialTheme.colorScheme.surfaceContainer) {
                Text(GoalMath.emojiFor(goal), fontSize = 24.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(goal.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                com.example.fin_ai.ui.util.AutoSizeText(
                    "${Fmt.rupees(goal.currentAmount)} of ${Fmt.rupees(goal.targetAmount)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
                    minFontSize = 10.sp, modifier = Modifier.fillMaxWidth()
                )
                Text("${(goal.progress * 100).toInt()}% saved", color = VioletLight, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Goal options", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Edit goal") }, leadingIcon = { Icon(Icons.Filled.Edit, null) }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text("Withdraw funds") }, onClick = { menu = false; onCustom(true) })
                    DropdownMenuItem(text = { Text("Delete", color = SoftRed) }, leadingIcon = { Icon(Icons.Filled.Delete, null, tint = SoftRed) }, onClick = { menu = false; onDelete() })
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        MilestoneTrack(goal.progress)

        Spacer(Modifier.height(12.dp))
        ForecastLine(p, goal)

        Spacer(Modifier.height(12.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(500.0, 1_000.0, 5_000.0).forEach { amt ->
                Chip("+${Fmt.rupees(amt)}", selected = true) { onQuickAdd(amt) }
            }
            Chip("Custom", selected = false) { onCustom(false) }
        }
    }
}

/** A bar with 25/50/75/100% markers that light up as they're reached. */
@Composable
private fun MilestoneTrack(progress: Float) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(1000), label = "track")
    val track = MaterialTheme.colorScheme.surfaceContainer
    Column {
        Canvas(Modifier.fillMaxWidth().height(14.dp)) {
            val h = 8.dp.toPx()
            val y = (size.height - h) / 2
            drawRoundRect(track, Offset(0f, y), Size(size.width, h), CornerRadius(h, h))
            drawRoundRect(
                Brush.horizontalGradient(listOf(VioletLight, AccentCyan)),
                Offset(0f, y), Size(size.width * animated, h), CornerRadius(h, h)
            )
            GoalMath.MILESTONES.forEach { m ->
                val x = (size.width * m / 100f).coerceIn(7f, size.width - 7f)
                val reached = animated * 100 >= m
                drawCircle(if (reached) Gold else track, radius = 7.dp.toPx() / 1.4f, center = Offset(x, size.height / 2))
                drawCircle(Color(0xFF0B0B14), radius = 2.5f, center = Offset(x, size.height / 2))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("0", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            GoalMath.MILESTONES.forEach { m ->
                Text("$m%", color = if (progress * 100 >= m) Gold else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ForecastLine(p: com.example.fin_ai.insights.GoalProjection, goal: GoalDto) {
    val text: String
    val color: Color
    when {
        p.daysLeft != null && p.neededPerMonth != null -> {
            val due = Fmt.format(Fmt.millis(goal.deadline!!), "dd MMM yyyy")
            text = if (p.daysLeft < 0) "⏰ Target date ($due) has passed · ${Fmt.rupees(p.remaining)} to go"
            else "🗓 ${p.daysLeft} days left (by $due) · save ${Fmt.rupees(p.neededPerMonth)}/month"
            color = when (p.onTrack) {
                true -> GreenCredit
                false -> Amber
                null -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        }
        p.monthsAtPace != null && p.monthsAtPace > 120 -> {
            text = "🧗 10+ years at your current pace — add a target date to plan monthly savings"
            color = MaterialTheme.colorScheme.onSurfaceVariant
        }
        p.monthsAtPace != null -> {
            text = "🔮 At your savings pace: ~${GoalMath.monthsLabel(p.monthsAtPace)} to go"
            color = AccentCyan
        }
        else -> {
            text = "💡 Set a target date or save regularly to get a forecast"
            color = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = color, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.weight(1f))
        p.onTrack?.takeIf { p.neededPerMonth != null }?.let { ok ->
            Spacer(Modifier.width(8.dp))
            Text(
                if (ok) "On track ✓" else "Behind",
                color = if (ok) GreenCredit else Amber,
                fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background((if (ok) GreenCredit else Amber).copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun CompletedCard(goal: GoalDto, modifier: Modifier, onEdit: () -> Unit, onDelete: () -> Unit) {
    val glow by rememberInfiniteTransition(label = "trophy").animateFloat(
        0.92f, 1.08f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "trophyScale"
    )
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Gold.copy(alpha = 0.18f), MaterialTheme.colorScheme.surfaceContainerLowest)))
            .border(1.dp, Gold.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .clickable(onClick = onEdit)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🏆", fontSize = 30.sp, modifier = Modifier.scale(glow))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("${GoalMath.emojiFor(goal)} ${goal.title}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text("Reached ${Fmt.rupees(goal.targetAmount)} 🎉", color = Gold, fontSize = 12.sp)
        }
        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete goal", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun CelebrationBanner(c: Celebration) {
    val (title, sub) = if (c.milestone >= 100) {
        "🎉 Goal reached!" to "You've fully funded \"${c.goal.title}\". Amazing!"
    } else {
        "🔥 ${c.milestone}% milestone!" to "\"${c.goal.title}\" is ${c.milestone}% funded. Keep going!"
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(VioletPrimary, AccentCyan)))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(sub, color = Color.White.copy(alpha = 0.92f), fontSize = 13.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyGoals(onTemplate: (Template?) -> Unit) {
    val bob by rememberInfiniteTransition(label = "bob").animateFloat(
        -6f, 6f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "bobY"
    )
    Column(
        Modifier
            .fillMaxWidth()
            .appear(1)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, OutlineViolet, RoundedCornerShape(24.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🎯", fontSize = 54.sp, modifier = Modifier.offset(y = bob.dp))
        Spacer(Modifier.height(10.dp))
        Text("What are you saving for?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(
            "Pick a template or create your own goal.",
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TEMPLATES.forEach { t -> Chip("${t.emoji} ${t.title}", selected = false) { onTemplate(t) } }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun GoalEditor(
    existing: GoalDto?,
    template: Template?,
    saving: Boolean,
    error: String?,
    onSave: (title: String, target: String, emoji: String?, deadline: String?) -> Unit,
) {
    var title by remember { mutableStateOf(existing?.title ?: template?.title.orEmpty()) }
    var target by remember { mutableStateOf(existing?.targetAmount?.toLong()?.toString() ?: template?.target?.toString().orEmpty()) }
    var emoji by remember { mutableStateOf(existing?.emoji ?: template?.emoji) }
    var deadline by remember { mutableStateOf(existing?.deadline?.take(10)) }
    var pickDate by remember { mutableStateOf(false) }

    Column(
        Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(if (existing == null) "New goal" else "Edit goal", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        if (existing == null) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TEMPLATES.forEach { t ->
                    Chip("${t.emoji} ${t.title}", selected = title == t.title) {
                        title = t.title; target = t.target.toString(); emoji = t.emoji
                    }
                }
            }
        }
        Text("Icon", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val shown = emoji ?: GoalMath.emojiFor(title)
            EMOJIS.forEach { e ->
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (e == shown) VioletPrimary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, if (e == shown) VioletLight else Color.Transparent, CircleShape)
                        .clickable { emoji = e },
                    contentAlignment = Alignment.Center
                ) { Text(e, fontSize = 18.sp) }
            }
        }
        FinTextField(value = title, onValueChange = { title = it.take(60) }, placeholder = "Goal name, e.g. New Laptop")
        FinTextField(value = target, onValueChange = { target = it.filter(Char::isDigit).take(9) }, placeholder = "Target amount (₹)", numeric = true)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Chip(
                deadline?.let { "🗓 By ${Fmt.format(Fmt.millis(it + "T00:00:00"), "dd MMM yyyy")}" } ?: "🗓 Add a target date (optional)",
                selected = deadline != null, tint = AccentCyan
            ) { pickDate = true }
            if (deadline != null) {
                Spacer(Modifier.width(8.dp))
                Text("Clear", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.clickable { deadline = null }.padding(6.dp))
            }
        }
        error?.let { Text(it, color = SoftRed, fontSize = 13.sp) }
        PrimaryButton(if (existing == null) "Create goal" else "Save changes", saving) { onSave(title, target, emoji, deadline) }
    }

    if (pickDate) {
        val today = remember { Fmt.startOfDay(System.currentTimeMillis()) }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = deadline?.let { Fmt.millis(it + "T00:00:00") },
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= today
            }
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        deadline = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(it)
                    }
                    pickDate = false
                }) { Text("Set date", color = VioletLight) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun FundsEditor(goal: GoalDto, startWithdraw: Boolean, saving: Boolean, error: String?, onSave: (Double) -> Unit) {
    var withdraw by remember { mutableStateOf(startWithdraw) }
    var amount by remember { mutableStateOf("") }
    Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("${GoalMath.emojiFor(goal)} ${goal.title}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(
            "${Fmt.rupees(goal.currentAmount)} saved · ${Fmt.rupees((goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0))} to go",
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("➕ Add money", selected = !withdraw, tint = GreenCredit) { withdraw = false }
            Chip("➖ Withdraw", selected = withdraw, tint = Amber) { withdraw = true }
        }
        FinTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' }.take(10) }, placeholder = "Amount (₹)", numeric = true)
        if (!withdraw) {
            val left = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
            if (left > 0) Text(
                "Tap to fill the rest: ${Fmt.rupees(left)}",
                color = VioletLight, fontSize = 12.sp,
                modifier = Modifier.clickable { amount = "%.0f".format(kotlin.math.ceil(left)) }.padding(vertical = 4.dp)
            )
        }
        error?.let { Text(it, color = SoftRed, fontSize = 13.sp) }
        PrimaryButton(if (withdraw) "Withdraw" else "Add to goal", saving) {
            val v = amount.toDoubleOrNull() ?: 0.0
            onSave(if (withdraw) -v else v)
        }
    }
}

@Composable
private fun PrimaryButton(label: String, busy: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(VioletPrimary, AccentCyan)))
            .clickable(enabled = !busy, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
        else Text(label, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun Notice(message: String, isError: Boolean = false, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, OutlineViolet, RoundedCornerShape(20.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            message,
            color = if (isError) SoftRed else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        if (onRetry != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Tap to retry",
                color = VioletLight,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}
