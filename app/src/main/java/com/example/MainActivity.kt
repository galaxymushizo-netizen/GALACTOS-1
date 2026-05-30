package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.HealthRecord
import com.example.data.UserProfile
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import com.example.ui.DashboardUiState
import com.example.ui.HealthViewModel
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    HealthTrackerApp(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun HealthTrackerApp(
    modifier: Modifier = Modifier,
    viewModel: HealthViewModel = viewModel()
) {
    val state by viewModel.dashboardState.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    var showAddLogDialog by remember { mutableStateOf(false) }
    var showGoalsDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        if (profile == null) {
            LoginOnboardingScreen(
                onLoginCompleted = { name, focus ->
                    viewModel.registerProfile(name, name, focus)
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Section
                HeaderSection(
                    profileName = profile?.name ?: "",
                    onEditGoalsClick = { showGoalsDialog = true },
                    onLogoutClick = { viewModel.logout() }
                )

                // Progress Circular Overview Card
                OverviewProgressCard(state = state)

                // Dynamic Fitness Cards For Stats Logging
                StatsGridSection(
                    state = state,
                    onAddSteps = { viewModel.addRecord("STEPS", 1000f, "Quick Walk") },
                    onAddWater = { viewModel.addRecord("WATER", 250f, "Quick Drink") },
                    onAddSleep = { viewModel.addRecord("SLEEP", 60f, "Quick Nap") },
                    onAddCalories = { viewModel.addRecord("CALORIES", 150f, "Quick Log") },
                    onAddCustomValue = { type ->
                        // Auto open custom add dialog with type selected
                        showAddLogDialog = true
                    }
                )

                // Heart Rate Section (Unique Metric showing pulse tracking)
                HeartRateCard(
                    state = state,
                    onLogHeartRate = { bpm ->
                        viewModel.addRecord("HEART_RATE", bpm.toFloat(), "Manual Reading")
                    }
                )

                // Action FAB/Button Inline representation to avoid complex layers
                Button(
                    onClick = { showAddLogDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("add_log_fab"),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Log")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Log Custom Activity",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Interactive Recent History Timestamps
                HistorySection(
                    records = state.recentRecords,
                    onDeleteClick = { record -> viewModel.deleteRecord(record) }
                )
            }
        }
    }

    // Custom Log Dialog
    if (showAddLogDialog) {
        AddLogDialog(
            onDismiss = { showAddLogDialog = false },
            onConfirm = { type, value, notes ->
                viewModel.addRecord(type, value, notes)
                showAddLogDialog = false
            }
        )
    }

    // Goals Modifier Dialog
    if (showGoalsDialog) {
        GoalsDialog(
            currentState = state,
            onDismiss = { showGoalsDialog = false },
            onSave = { steps, water, sleep, calories ->
                viewModel.updateGoal("STEPS", steps)
                viewModel.updateGoal("WATER", water)
                viewModel.updateGoal("SLEEP", sleep)
                viewModel.updateGoal("CALORIES", calories)
                showGoalsDialog = false
            }
        )
    }
}

@Composable
fun HeaderSection(
    profileName: String,
    onEditGoalsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }
    val todayStr = remember { dateFormat.format(Date()) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Habari, $profileName! 👋",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Health Tracker",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = todayStr,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onEditGoalsClick,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .testTag("edit_goals_button")
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Edit Goals",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .testTag("logout_button")
            ) {
                Icon(
                    Icons.Default.ExitToApp,
                    contentDescription = "Logout",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun OverviewProgressCard(
    state: DashboardUiState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TODAY'S ACTIVITY COMPLETION",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Steps Progress
                SimpleDonutProgress(
                    progress = if (state.stepsGoal > 0) state.stepsValue.toFloat() / state.stepsGoal else 0f,
                    color = MaterialTheme.colorScheme.primary,
                    label = "Steps",
                    value = if (state.stepsValue >= 1000) String.format("%.1fk", state.stepsValue / 1000f) else state.stepsValue.toString()
                )

                // Water Progress
                SimpleDonutProgress(
                    progress = if (state.waterGoal > 0) state.waterValue.toFloat() / state.waterGoal else 0f,
                    color = MaterialTheme.colorScheme.secondary,
                    label = "Water",
                    value = if (state.waterValue >= 1000) String.format("%.1fL", state.waterValue / 1000f) else "${state.waterValue}ml"
                )

                // Sleep Progress
                SimpleDonutProgress(
                    progress = if (state.sleepGoal > 0) state.sleepValue.toFloat() / state.sleepGoal else 0f,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                    label = "Sleep",
                    value = String.format("%.1fh", state.sleepValue / 60f)
                )

                // Calories Progress
                SimpleDonutProgress(
                    progress = if (state.caloriesGoal > 0) state.caloriesValue.toFloat() / state.caloriesGoal else 0f,
                    color = MaterialTheme.colorScheme.tertiary,
                    label = "Calories",
                    value = "${state.caloriesValue}"
                )
            }
        }
    }
}

@Composable
fun SimpleDonutProgress(
    progress: Float,
    color: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.size(72.dp)) {
            // Background ring track
            drawArc(
                color = color.copy(alpha = 0.12f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )
            // Progress highlight track
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = (progress.coerceIn(0f, 1f) * 360f),
                useCenter = false,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun StatsGridSection(
    state: DashboardUiState,
    onAddSteps: () -> Unit,
    onAddWater: () -> Unit,
    onAddSleep: () -> Unit,
    onAddCalories: () -> Unit,
    onAddCustomValue: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "DAILY STATS",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        // Steps Card
        MetricCard(
            label = "Steps",
            current = state.stepsValue,
            target = state.stepsGoal,
            unit = "steps",
            color = MaterialTheme.colorScheme.primary,
            icon = Icons.Default.DirectionsWalk,
            quickActionText = "+1,000 steps",
            onQuickAction = onAddSteps,
            onCardClick = { onAddCustomValue("STEPS") },
            testTag = "steps_card"
        )

        // Water hydration Card
        MetricCard(
            label = "Water Intake",
            current = state.waterValue,
            target = state.waterGoal,
            unit = "ml",
            color = MaterialTheme.colorScheme.secondary,
            icon = Icons.Default.WaterDrop,
            quickActionText = "+250 ml",
            onQuickAction = onAddWater,
            onCardClick = { onAddCustomValue("WATER") },
            testTag = "water_card"
        )

        // Sleep Duration Card
        MetricCard(
            label = "Sleep Duration",
            current = state.sleepValue,
            target = state.sleepGoal,
            unit = "min",
            displayFormat = { "${it / 60}h ${it % 60}m" },
            targetFormat = { "${it / 60}h ${it % 60}m goal" },
            color = MaterialTheme.colorScheme.primaryContainer,
            icon = Icons.Default.Bedtime,
            quickActionText = "+1 hour",
            onQuickAction = onAddSleep,
            onCardClick = { onAddCustomValue("SLEEP") },
            testTag = "sleep_card"
        )

        // Calories Card
        MetricCard(
            label = "Calories Burned",
            current = state.caloriesValue,
            target = state.caloriesGoal,
            unit = "kcal",
            color = MaterialTheme.colorScheme.tertiary,
            icon = Icons.Default.LocalFireDepartment,
            quickActionText = "+150 kcal",
            onQuickAction = onAddCalories,
            onCardClick = { onAddCustomValue("CALORIES") },
            testTag = "calories_card"
        )
    }
}

@Composable
fun MetricCard(
    label: String,
    current: Int,
    target: Int,
    unit: String,
    displayFormat: (Int) -> String = { "$it $unit" },
    targetFormat: (Int) -> String = { "Goal: $it $unit" },
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    quickActionText: String,
    onQuickAction: () -> Unit,
    onCardClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val progress = if (target > 0) current.toFloat() / target else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(color.copy(alpha = 0.15f), CircleShape)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = label, tint = color)
                    }
                    Column {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = targetFormat(target),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Text(
                    text = displayFormat(current),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = color,
                trackColor = color.copy(alpha = 0.12f),
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(progress * 100).toInt()}% completed",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (progress >= 1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = if (progress >= 1f) FontWeight.Bold else FontWeight.Normal
                )

                Button(
                    onClick = {
                        onQuickAction()
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = color.copy(alpha = 0.1f),
                        contentColor = color
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add amount",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = quickActionText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun HeartRateCard(
    state: DashboardUiState,
    onLogHeartRate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var inputTemp by remember { mutableStateOf("") }
    val dateStr = if (state.heartRateLastTimestamp > 0) {
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        formatter.format(Date(state.heartRateLastTimestamp))
    } else "No records"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = "Heart Rate", tint = Color(0xFFEF4444))
                    }
                    Column {
                        Text(
                            text = "Heart Rate Log",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Last: $dateStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (state.heartRateLastValue > 0) state.heartRateLastValue.toString() else "--",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color(0xFFEF4444)
                    )
                    Text(
                        text = "bpm",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(
                visible = isEditing,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputTemp,
                        onValueChange = { inputTemp = it.take(3).filter { char -> char.isDigit() } },
                        label = { Text("Log Pulse (BPM)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            val pulse = inputTemp.toIntOrNull()
                            if (pulse != null && pulse in 40..220) {
                                onLogHeartRate(pulse)
                                inputTemp = ""
                                isEditing = false
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("Save")
                    }
                }
            }

            AnimatedVisibility(visible = !isEditing) {
                Button(
                    onClick = { isEditing = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background)
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Measure & Log Pulse",
                        color = Color(0xFFEF4444),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun HistorySection(
    records: List<HealthRecord>,
    onDeleteClick: (HealthRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LATEST LOGS",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Swipe/Tap to clean",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }

        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Empty",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your health canvas is currently fresh.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Create quick logs above to chart metrics!",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            // Keep a clean compact history feed limited to maximum 10 elements to prevent extreme scrolling
            records.take(12).forEach { record ->
                HistoryItem(
                    record = record,
                    onDelete = { onDeleteClick(record) }
                )
            }
        }
    }
}

@Composable
fun HistoryItem(
    record: HealthRecord,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val formattedTime = remember(record.timestamp) { timeFormat.format(Date(record.timestamp)) }

    val details = when (record.type) {
        "STEPS" -> Pair("Steps Logged", "+${record.value.toInt()} steps")
        "WATER" -> Pair("Water Logged", "+${record.value.toInt()} ml")
        "SLEEP" -> Pair("Sleep Logged", "${record.value.toInt() / 60}h ${record.value.toInt() % 60}m")
        "CALORIES" -> Pair("Calories Burned", "+${record.value.toInt()} kcal")
        "HEART_RATE" -> Pair("Pulse Measured", "${record.value.toInt()} bpm")
        else -> Pair("Log Entry", "${record.value}")
    }

    val themeColor = when (record.type) {
        "STEPS" -> MaterialTheme.colorScheme.primary
        "WATER" -> MaterialTheme.colorScheme.secondary
        "SLEEP" -> MaterialTheme.colorScheme.primaryContainer
        "CALORIES" -> MaterialTheme.colorScheme.tertiary
        "HEART_RATE" -> Color(0xFFEF4444)
        else -> MaterialTheme.colorScheme.outline
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(themeColor, CircleShape)
            )
            Column {
                Text(
                    text = details.second,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${details.first} • $formattedTime" + if (record.notes.isNotEmpty()) " (${record.notes})" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun AddLogDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Float, String) -> Unit
) {
    var selectedType by remember { mutableStateOf("STEPS") }
    var valueStr by remember { mutableStateOf("") }
    var notesStr by remember { mutableStateOf("") }
    val types = listOf(
        Triple("STEPS", "Steps", "Count"),
        Triple("WATER", "Water Intake", "ml"),
        Triple("SLEEP", "Sleep Duration", "minutes"),
        Triple("CALORIES", "Calories Burned", "kcal"),
        Triple("HEART_RATE", "Heart Rate", "bpm")
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Log Health Activity",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Horizontal selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Activity Type",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        types.forEach { (typeKey, label, _) ->
                            val isSelected = selectedType == typeKey
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedType = typeKey }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                // Input Field
                val selectedLabel = types.first { it.first == selectedType }.second
                val selectedUnit = types.first { it.first == selectedType }.third

                OutlinedTextField(
                    value = valueStr,
                    onValueChange = { valueStr = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Value ($selectedUnit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notesStr,
                    onValueChange = { notesStr = it },
                    label = { Text("Notes (e.g. morning workout)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val value = valueStr.toFloatOrNull()
                            if (value != null && value > 0f) {
                                onConfirm(selectedType, value, notesStr)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("submit_button")
                    ) {
                        Text("Log Entry")
                    }
                }
            }
        }
    }
}

@Composable
fun GoalsDialog(
    currentState: DashboardUiState,
    onDismiss: () -> Unit,
    onSave: (Float, Float, Float, Float) -> Unit
) {
    var stepsGoal by remember { mutableStateOf(currentState.stepsGoal.toString()) }
    var waterGoal by remember { mutableStateOf(currentState.waterGoal.toString()) }
    var sleepGoal by remember { mutableStateOf(currentState.sleepGoal.toString()) }
    var caloriesGoal by remember { mutableStateOf(currentState.caloriesGoal.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Update Daily Targets",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = stepsGoal,
                    onValueChange = { stepsGoal = it.filter { char -> char.isDigit() } },
                    label = { Text("Daily Steps Goal") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = waterGoal,
                    onValueChange = { waterGoal = it.filter { char -> char.isDigit() } },
                    label = { Text("Daily Hydration Goal (ml)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = sleepGoal,
                    onValueChange = { sleepGoal = it.filter { char -> char.isDigit() } },
                    label = { Text("Daily Sleep Goal (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = caloriesGoal,
                    onValueChange = { caloriesGoal = it.filter { char -> char.isDigit() } },
                    label = { Text("Daily Calories Goal (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Dismiss")
                    }
                    Button(
                        onClick = {
                            val steps = stepsGoal.toFloatOrNull() ?: 10000f
                            val water = waterGoal.toFloatOrNull() ?: 2500f
                            val sleep = sleepGoal.toFloatOrNull() ?: 480f
                            val calories = caloriesGoal.toFloatOrNull() ?: 2200f
                            onSave(steps, water, sleep, calories)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Targets")
                    }
                }
            }
        }
    }
}

// A helper modifier function to simulate modern scroll behavior inline
@Composable
fun Modifier.horizontalScrollStateShim(): Modifier {
    return this.then(Modifier.padding(vertical = 4.dp))
}

@Composable
fun WelcomeGestureSlider(
    onSwipeSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var swipeOffset by remember { mutableStateOf(0f) }
    val maxSwipeDistanceDp = 220.dp
    val density = LocalDensity.current
    val maxSwipePx = with(density) { maxSwipeDistanceDp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(32.dp))
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Track hint text
        Text(
            text = if (swipeOffset > maxSwipePx * 0.8f) "Imekubaliwa! 🎉" else "Swipe kulia kutoa Ishara 👋",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.Center)
        )

        // Draggable handle
        val animatedOffset by animateFloatAsState(targetValue = swipeOffset)

        Box(
            modifier = Modifier
                .offset(x = with(density) { animatedOffset.toDp() })
                .size(56.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (swipeOffset >= maxSwipePx * 0.75f) {
                                swipeOffset = maxSwipePx
                                onSwipeSuccess()
                            } else {
                                swipeOffset = 0f
                            }
                        },
                        onDragCancel = {
                            swipeOffset = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            swipeOffset = (swipeOffset + dragAmount).coerceIn(0f, maxSwipePx)
                        }
                    )
                }
                .testTag("welcome_gesture_handle"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = "Swipe Right",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun LoginOnboardingScreen(
    onLoginCompleted: (name: String, focus: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var nameInput by remember { mutableStateOf("") }
    var selectedFocus by remember { mutableStateOf("STEPS") }
    val focusOptions = remember {
        listOf(
            Pair("STEPS", "Kutembea (Steps) 🚶"),
            Pair("WATER", "Kunywa Maji (Water) 💧"),
            Pair("SLEEP", "Dr. Sleep (Sleep) 😴"),
            Pair("CALORIES", "Kupunguza Calories 🏃‍♂️")
        )
    }
    var showGestureHint by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .size(96.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "👋",
                style = MaterialTheme.typography.displayMedium
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Karibu Health Tracker!",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Weka wasifu wako na uteleze kishale kutoa ishara ya Karibu (Welcome Gesture) ili kuanza.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Taarifa Zako",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Jina lako Kamili") },
                    placeholder = { Text("Mfano: Yohana") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "Chagua Lengo Kuu la Kiafya:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                focusOptions.forEach { (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFocus = key }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedFocus == key),
                            onClick = { selectedFocus = key },
                            modifier = Modifier.testTag("focus_radio_$key")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selectedFocus == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (showGestureHint) {
            Text(
                text = "⚠️ Tafadhali weka jina lako kwanza kabla ya kutelezesha mshale!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        WelcomeGestureSlider(
            onSwipeSuccess = {
                if (nameInput.trim().isNotEmpty()) {
                    onLoginCompleted(nameInput.trim(), selectedFocus)
                } else {
                    showGestureHint = true
                }
            },
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}
