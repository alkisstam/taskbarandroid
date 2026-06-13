package com.alkisstam.taskbar.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alkisstam.taskbar.data.PillEdgePosition
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    hasWriteSettingsPermission: Boolean,
    hasNotificationPolicyPermission: Boolean,
    hasNotificationsPermission: Boolean,
    hasUsageStatsPermission: Boolean,
    hasNotificationListenerPermission: Boolean,
    hasBatteryOptimizationExcluded: Boolean,
    selectedPosition: PillEdgePosition,
    onPositionSelected: (PillEdgePosition) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onRequestWriteSettingsPermission: () -> Unit,
    onRequestNotificationPolicyPermission: () -> Unit,
    onRequestNotificationsPermission: () -> Unit,
    onRequestUsageStatsPermission: () -> Unit,
    onRequestNotificationListenerPermission: () -> Unit,
    onRequestBatteryOptimizationExclusion: () -> Unit,
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    Scaffold(contentWindowInsets = WindowInsets(0)) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> WelcomePage()
                    1 -> HandlePositionPage(
                        selectedPosition = selectedPosition,
                        onPositionSelected = onPositionSelected
                    )
                    2 -> RequiredPermissionPage(
                        hasOverlayPermission = hasOverlayPermission,
                        hasAccessibilityPermission = hasAccessibilityPermission,
                        onRequestOverlayPermission = onRequestOverlayPermission,
                        onRequestAccessibilityPermission = onRequestAccessibilityPermission
                    )
                    else -> OptionalPermissionsPage(
                        hasWriteSettingsPermission = hasWriteSettingsPermission,
                        hasNotificationPolicyPermission = hasNotificationPolicyPermission,
                        hasNotificationsPermission = hasNotificationsPermission,
                        hasUsageStatsPermission = hasUsageStatsPermission,
                        hasNotificationListenerPermission = hasNotificationListenerPermission,
                        hasBatteryOptimizationExcluded = hasBatteryOptimizationExcluded,
                        onRequestWriteSettingsPermission = onRequestWriteSettingsPermission,
                        onRequestNotificationPolicyPermission = onRequestNotificationPolicyPermission,
                        onRequestNotificationsPermission = onRequestNotificationsPermission,
                        onRequestUsageStatsPermission = onRequestUsageStatsPermission,
                        onRequestNotificationListenerPermission = onRequestNotificationListenerPermission,
                        onRequestBatteryOptimizationExclusion = onRequestBatteryOptimizationExclusion
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PageDots(currentPage = pagerState.currentPage, totalPages = 4)
                Spacer(modifier = Modifier.weight(1f))
                if (pagerState.currentPage < 3) {
                    Button(onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }) {
                        Text("Next")
                    }
                } else {
                    Button(onClick = onComplete) {
                        Text("Get Started")
                    }
                }
            }
        }
    }
}

@Composable
private fun PageDots(currentPage: Int, totalPages: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(totalPages) { index ->
            val width by animateDpAsState(
                targetValue = if (index == currentPage) 20.dp else 6.dp,
                label = "dot_width"
            )
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(width)
                    .background(
                        color = if (index == currentPage)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PhoneWithDockGraphic()

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Welcome to Floating Dock",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "A floating dock that lives on top of every app. Launch apps, control music, and access quick settings — without leaving what you're doing.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HandlePositionPage(
    selectedPosition: PillEdgePosition,
    onPositionSelected: (PillEdgePosition) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Handle position",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Navigation gestures can interfere with this app. If you use them, select \"Left\" or \"Right\" to avoid conflicts.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select an edge to open the dock from:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(24.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(
                PillEdgePosition.LEFT to "Left",
                PillEdgePosition.RIGHT to "Right",
                PillEdgePosition.BOTH to "Both Sides",
                PillEdgePosition.BOTTOM to "Bottom"
            ).forEach { (pos, label) ->
                val isSelected = selectedPosition == pos
                if (isSelected) {
                    Button(
                        onClick = { onPositionSelected(pos) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label.uppercase(), style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onPositionSelected(pos) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label.uppercase(), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = if (selectedPosition == PillEdgePosition.BOTTOM)
                "Double tap the pill to open the dock"
            else
                "Swipe upwards to open the dock when the handle is on the left or right",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PhoneWithDockGraphic() {
    val frameColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    val borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)

    Box(
        modifier = Modifier
            .size(width = 130.dp, height = 220.dp)
            .background(frameColor, RoundedCornerShape(20.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Screen content area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Simulated status bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(borderColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.weight(1f))
            // App icon grid row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(iconBg, RoundedCornerShape(5.dp))
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            // Dock bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .background(primaryColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                    .border(1.dp, primaryColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(5) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(primaryColor.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun RequiredPermissionPage(
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit
) {
    var showAccessibilityDialog by remember { mutableStateOf(false) }

    if (showAccessibilityDialog) {
        AlertDialog(
            onDismissRequest = { showAccessibilityDialog = false },
            title = {
                Text(
                    "Accessibility Service",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    "Required to draw the dock above the navigation bar and enable screenshots and lock screen controls.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            dismissButton = {
                TextButton(onClick = { showAccessibilityDialog = false }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                Button(onClick = {
                    showAccessibilityDialog = false
                    onRequestAccessibilityPermission()
                }) {
                    Text("Accept")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ThreePillPositionsGraphic()

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Two permissions required",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "These two permissions are needed for the dock to work. Without them, nothing will function.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        PermissionCard(
            icon = {
                Icon(
                    Icons.Filled.Layers,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = "Draw over other apps",
            description = "Required to display the dock above all other apps.",
            granted = hasOverlayPermission,
            onGrant = onRequestOverlayPermission
        )

        Spacer(modifier = Modifier.height(12.dp))

        PermissionCard(
            icon = {
                Icon(
                    Icons.Filled.Accessibility,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = "Accessibility Service",
            description = "Required to draw the dock above the navigation bar and enable screenshots and lock screen controls.",
            granted = hasAccessibilityPermission,
            onGrant = { showAccessibilityDialog = true }
        )
    }
}

@Composable
private fun ThreePillPositionsGraphic() {
    val frameColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    val borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val pillColor = MaterialTheme.colorScheme.primary

    val frameW = 60.dp
    val frameH = 100.dp

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bottom pill
        Box(
            modifier = Modifier
                .size(frameW, frameH)
                .background(frameColor, RoundedCornerShape(10.dp))
                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 3.dp)
                    .width(28.dp)
                    .height(4.dp)
                    .background(pillColor, CircleShape)
            )
        }

        // Left pill
        Box(
            modifier = Modifier
                .size(frameW, frameH)
                .background(frameColor, RoundedCornerShape(10.dp))
                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 3.dp)
                    .width(4.dp)
                    .height(28.dp)
                    .background(pillColor, CircleShape)
            )
        }

        // Right pill
        Box(
            modifier = Modifier
                .size(frameW, frameH)
                .background(frameColor, RoundedCornerShape(10.dp))
                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 3.dp)
                    .width(4.dp)
                    .height(28.dp)
                    .background(pillColor, CircleShape)
            )
        }
    }
}

@Composable
private fun OptionalPermissionsPage(
    hasWriteSettingsPermission: Boolean,
    hasNotificationPolicyPermission: Boolean,
    hasNotificationsPermission: Boolean,
    hasUsageStatsPermission: Boolean,
    hasNotificationListenerPermission: Boolean,
    hasBatteryOptimizationExcluded: Boolean,
    onRequestWriteSettingsPermission: () -> Unit,
    onRequestNotificationPolicyPermission: () -> Unit,
    onRequestNotificationsPermission: () -> Unit,
    onRequestUsageStatsPermission: () -> Unit,
    onRequestNotificationListenerPermission: () -> Unit,
    onRequestBatteryOptimizationExclusion: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Optional features",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Grant these to unlock the full experience. You can always do this later in Settings.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        PermissionCard(
            icon = {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = "Modify System Settings",
            description = "Required for brightness and auto-rotate quick controls.",
            granted = hasWriteSettingsPermission,
            onGrant = onRequestWriteSettingsPermission
        )

        PermissionCard(
            icon = {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = "Do Not Disturb Access",
            description = "Required for the DND quick control.",
            granted = hasNotificationPolicyPermission,
            onGrant = onRequestNotificationPolicyPermission
        )

        PermissionCard(
            icon = {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = "Notifications",
            description = "Required for the music panel to access media sessions.",
            granted = hasNotificationsPermission,
            onGrant = onRequestNotificationsPermission
        )

        PermissionCard(
            icon = {
                Icon(
                    Icons.Filled.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = "Notification Listener",
            description = "Required for the music panel to show media controls.",
            granted = hasNotificationListenerPermission,
            onGrant = onRequestNotificationListenerPermission
        )

        PermissionCard(
            icon = {
                Icon(
                    Icons.Filled.QueryStats,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = "Usage Access",
            description = "Required for the Recent Apps section in the dock.",
            granted = hasUsageStatsPermission,
            onGrant = onRequestUsageStatsPermission
        )

        PermissionCard(
            icon = {
                Icon(
                    Icons.Filled.BatteryFull,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = "Disable Battery Optimization",
            description = "Prevents the system from killing the dock when running in the background.",
            granted = hasBatteryOptimizationExcluded,
            onGrant = onRequestBatteryOptimizationExclusion
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PermissionCard(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    granted: Boolean,
    onGrant: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(28.dp)) { icon() }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (granted) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                OutlinedButton(onClick = onGrant) {
                    Text("Grant")
                }
            }
        }
    }
}
