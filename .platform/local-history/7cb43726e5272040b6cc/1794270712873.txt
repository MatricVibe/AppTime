// === Файл: UIComponents.kt ===
package com.example.apptime

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun rememberAnimatedAppColorScheme(ui: UiSettings): ColorScheme {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val isDark = when (ui.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.SYSTEM -> systemDark
    }
    val isAmoled = ui.themeMode == ThemeMode.AMOLED

    val target = remember(isDark, isAmoled, ui.dynamicColorEnabled, ui.accentIndex, ui.designStyle) {
        val useWallpaperDynamic = ui.dynamicColorEnabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ui.designStyle != DesignStylePreset.MATERIAL_YOU_3

        val baseScheme = if (useWallpaperDynamic) {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            val seed = AccentPresets.getOrElse(ui.accentIndex) { AccentPresets[0] }
            val containerAlpha = when (ui.designStyle) {
                DesignStylePreset.MATERIAL_YOU -> if (isDark) 0.30f else 0.14f
                DesignStylePreset.MATERIAL_YOU_3 -> if (isDark) 0.36f else 0.18f
                DesignStylePreset.MATERIAL_EXPRESSIVE -> if (isDark) 0.48f else 0.24f
            }

            if (isDark) {
                val primaryDark = Color(
                    red = (seed.red * 0.62f + 0.38f).coerceIn(0f, 1f),
                    green = (seed.green * 0.62f + 0.38f).coerceIn(0f, 1f),
                    blue = (seed.blue * 0.62f + 0.38f).coerceIn(0f, 1f)
                )
                val bg = when (ui.designStyle) {
                    DesignStylePreset.MATERIAL_YOU -> Color(0xFF131316)
                    DesignStylePreset.MATERIAL_YOU_3 -> Color(0xFF111318)
                    DesignStylePreset.MATERIAL_EXPRESSIVE -> Color(0xFF0E1017)
                }
                val surfHigh = when (ui.designStyle) {
                    DesignStylePreset.MATERIAL_YOU -> Color(0xFF26262C)
                    DesignStylePreset.MATERIAL_YOU_3 -> Color(0xFF252830)
                    DesignStylePreset.MATERIAL_EXPRESSIVE -> Color(0xFF282B3A)
                }
                val surfHighest = when (ui.designStyle) {
                    DesignStylePreset.MATERIAL_YOU -> Color(0xFF32323A)
                    DesignStylePreset.MATERIAL_YOU_3 -> Color(0xFF31353F)
                    DesignStylePreset.MATERIAL_EXPRESSIVE -> Color(0xFF35394C)
                }
                darkColorScheme(
                    primary = primaryDark,
                    onPrimary = Color(0xFF141218),
                    primaryContainer = seed.copy(alpha = containerAlpha),
                    onPrimaryContainer = Color(0xFFF4F0FF),
                    secondary = primaryDark,
                    secondaryContainer = seed.copy(alpha = containerAlpha * 0.85f),
                    onSecondaryContainer = Color(0xFFECE8F8),
                    background = bg,
                    surface = bg,
                    surfaceContainerLow = Color(0xFF191B22),
                    surfaceContainer = Color(0xFF1E2029),
                    surfaceContainerHigh = surfHigh,
                    surfaceContainerHighest = surfHighest,
                    onBackground = Color(0xFFE5E3EC),
                    onSurface = Color(0xFFE5E3EC),
                    onSurfaceVariant = Color(0xFFC5C4D0),
                    outlineVariant = Color(0xFF424554)
                )
            } else {
                val bg = when (ui.designStyle) {
                    DesignStylePreset.MATERIAL_YOU -> Color(0xFFFAF8FF)
                    DesignStylePreset.MATERIAL_YOU_3 -> Color(0xFFF7F8FC)
                    DesignStylePreset.MATERIAL_EXPRESSIVE -> Color(0xFFFDF7FF)
                }
                val surfHigh = when (ui.designStyle) {
                    DesignStylePreset.MATERIAL_YOU -> Color(0xFFEBE8F2)
                    DesignStylePreset.MATERIAL_YOU_3 -> Color(0xFFE7E9F0)
                    DesignStylePreset.MATERIAL_EXPRESSIVE -> Color(0xFFEAE5F4)
                }
                val surfHighest = when (ui.designStyle) {
                    DesignStylePreset.MATERIAL_YOU -> Color(0xFFE2DFE9)
                    DesignStylePreset.MATERIAL_YOU_3 -> Color(0xFFDEDFE8)
                    DesignStylePreset.MATERIAL_EXPRESSIVE -> Color(0xFFE0D9EE)
                }
                lightColorScheme(
                    primary = seed,
                    onPrimary = Color.White,
                    primaryContainer = seed.copy(alpha = containerAlpha),
                    onPrimaryContainer = Color(0xFF1B1728),
                    secondary = seed,
                    secondaryContainer = seed.copy(alpha = containerAlpha * 0.85f),
                    onSecondaryContainer = Color(0xFF1B1728),
                    background = bg,
                    surface = bg,
                    surfaceContainerLow = Color(0xFFF3F1F8),
                    surfaceContainer = Color(0xFFF0EDF6),
                    surfaceContainerHigh = surfHigh,
                    surfaceContainerHighest = surfHighest,
                    onBackground = Color(0xFF1A1B21),
                    onSurface = Color(0xFF1A1B21),
                    onSurfaceVariant = Color(0xFF46464F),
                    outlineVariant = Color(0xFFC9C5D4)
                )
            }
        }

        if (isAmoled) {
            baseScheme.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceContainerLow = Color(0xFF09090D),
                surfaceContainer = Color(0xFF101016),
                surfaceContainerHigh = Color(0xFF16161E),
                surfaceContainerHighest = Color(0xFF22222E)
            )
        } else {
            baseScheme
        }
    }

    val spec = tween<Color>(durationMillis = 340, easing = FastOutSlowInEasing)
    return target.copy(
        primary = animateColorAsState(target.primary, spec, label = "primary").value,
        onPrimary = animateColorAsState(target.onPrimary, spec, label = "onPrimary").value,
        primaryContainer = animateColorAsState(target.primaryContainer, spec, label = "primaryContainer").value,
        onPrimaryContainer = animateColorAsState(target.onPrimaryContainer, spec, label = "onPrimaryContainer").value,
        secondary = animateColorAsState(target.secondary, spec, label = "secondary").value,
        secondaryContainer = animateColorAsState(target.secondaryContainer, spec, label = "secondaryContainer").value,
        onSecondaryContainer = animateColorAsState(target.onSecondaryContainer, spec, label = "onSecondaryContainer").value,
        background = animateColorAsState(target.background, spec, label = "background").value,
        surface = animateColorAsState(target.surface, spec, label = "surface").value,
        surfaceContainerLow = animateColorAsState(target.surfaceContainerLow, spec, label = "surfLow").value,
        surfaceContainer = animateColorAsState(target.surfaceContainer, spec, label = "surf").value,
        surfaceContainerHigh = animateColorAsState(target.surfaceContainerHigh, spec, label = "surfHigh").value,
        surfaceContainerHighest = animateColorAsState(target.surfaceContainerHighest, spec, label = "surfHighest").value,
        onBackground = animateColorAsState(target.onBackground, spec, label = "onBg").value,
        onSurface = animateColorAsState(target.onSurface, spec, label = "onSurf").value,
        onSurfaceVariant = animateColorAsState(target.onSurfaceVariant, spec, label = "onSurfVar").value,
        outlineVariant = animateColorAsState(target.outlineVariant, spec, label = "outlineVar").value
    )
}

@Composable
fun rememberAppTypography(ui: UiSettings): Typography {
    val activeFontFamily = remember(ui.fontChoice, ui.customFontPath) {
        when (ui.fontChoice) {
            FontChoice.SANS -> FontFamily.SansSerif
            FontChoice.SERIF -> FontFamily.Serif
            FontChoice.MONO -> FontFamily.Monospace
            FontChoice.CUSTOM -> {
                if (ui.customFontPath.isNotEmpty() && File(ui.customFontPath).exists()) {
                    runCatching { FontFamily(Typeface.createFromFile(File(ui.customFontPath))) }
                        .getOrDefault(FontFamily.Default)
                } else {
                    FontFamily.Default
                }
            }
        }
    }

    val isExpressive = ui.designStyle == DesignStylePreset.MATERIAL_EXPRESSIVE
    return remember(activeFontFamily, isExpressive) {
        val base = Typography()
        val headerWeight = if (isExpressive) FontWeight.Black else FontWeight.Bold
        val subHeaderWeight = if (isExpressive) FontWeight.ExtraBold else FontWeight.SemiBold
        Typography(
            displaySmall = base.displaySmall.copy(
                fontFamily = activeFontFamily,
                fontWeight = headerWeight,
                letterSpacing = if (isExpressive) (-0.5).sp else 0.sp
            ),
            headlineLarge = base.headlineLarge.copy(fontFamily = activeFontFamily, fontWeight = headerWeight),
            headlineMedium = base.headlineMedium.copy(fontFamily = activeFontFamily, fontWeight = subHeaderWeight),
            headlineSmall = base.headlineSmall.copy(fontFamily = activeFontFamily, fontWeight = FontWeight.Bold),
            titleLarge = base.titleLarge.copy(fontFamily = activeFontFamily, fontWeight = subHeaderWeight),
            titleMedium = base.titleMedium.copy(fontFamily = activeFontFamily, fontWeight = FontWeight.Bold),
            titleSmall = base.titleSmall.copy(fontFamily = activeFontFamily, fontWeight = FontWeight.SemiBold),
            bodyLarge = base.bodyLarge.copy(fontFamily = activeFontFamily),
            bodyMedium = base.bodyMedium.copy(fontFamily = activeFontFamily),
            bodySmall = base.bodySmall.copy(fontFamily = activeFontFamily),
            labelLarge = base.labelLarge.copy(fontFamily = activeFontFamily, fontWeight = FontWeight.Bold),
            labelMedium = base.labelMedium.copy(fontFamily = activeFontFamily, fontWeight = FontWeight.SemiBold),
            labelSmall = base.labelSmall.copy(fontFamily = activeFontFamily)
        )
    }
}

// Верхняя плашка автора: "Автор:   " (серый) и "@matrixgit" (голубой) со ссылкой t.me/matrixgit
@Composable
fun AuthorCreditTopBanner(onHaptic: () -> Unit) {
    val context = LocalContext.current
    val authorGray = Color(0xFF9AA0A6)
    val matrixBlue = Color(0xFF29B6F6)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable {
                onHaptic()
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/matrixgit")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }.onFailure {
                    Toast.makeText(context, "Ссылка: t.me/matrixgit", Toast.LENGTH_SHORT).show()
                }
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Автор:   ",
                style = MaterialTheme.typography.labelLarge,
                color = authorGray
            )
            Text(
                text = "@matrixgit",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = matrixBlue,
                textDecoration = TextDecoration.Underline
            )
        }
        Surface(
            shape = CircleShape,
            color = matrixBlue.copy(alpha = 0.15f)
        ) {
            Text(
                text = "t.me/matrixgit ↗",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = matrixBlue,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveTopBar(
    selectedScreen: ScreenTab,
    ui: UiSettings,
    readyToClaimCount: Int,
    scrollBehavior: TopAppBarScrollBehavior,
    onOpenProfile: () -> Unit,
    onRefresh: () -> Unit
) {
    val isExpressive = ui.designStyle == DesignStylePreset.MATERIAL_EXPRESSIVE
    val titleContent: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedContent(
                targetState = selectedScreen.headerTitle,
                transitionSpec = {
                    (fadeIn(tween(220)) + scaleIn(initialScale = 0.94f))
                        .togetherWith(fadeOut(tween(150)))
                },
                label = "TopBarTitleAnim",
                modifier = Modifier.weight(1f, fill = false)
            ) { headerText ->
                Text(
                    text = headerText,
                    fontWeight = if (isExpressive) FontWeight.Black else FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = ui.designStyle.badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }
        }
    }

    val actionsContent: @Composable () -> Unit = {
        if (readyToClaimCount > 0 && selectedScreen != ScreenTab.PROFILE) {
            FilledTonalButton(
                onClick = onOpenProfile,
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Text("🎁 +$readyToClaimCount", maxLines = 1)
            }
        }
        if (selectedScreen == ScreenTab.TRACKER ||
            selectedScreen == ScreenTab.CUSTOM_PERIOD ||
            selectedScreen == ScreenTab.LEADERBOARD
        ) {
            FilledTonalIconButton(
                onClick = onRefresh,
                shape = CircleShape,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = AppIcons.Refresh,
                    contentDescription = "Обновить"
                )
            }
        }
    }

    if (ui.topBarStyle == TopBarStyle.LARGE) {
        LargeTopAppBar(
            title = titleContent,
            actions = { actionsContent() },
            colors = TopAppBarDefaults.largeTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            scrollBehavior = scrollBehavior
        )
    } else {
        CenterAlignedTopAppBar(
            title = titleContent,
            actions = { actionsContent() },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )
    }
}

// Плавающая нижняя панель навигации со строго фиксированной высотой 64.dp и пружинной капсулой
@Composable
fun FloatingExpressiveNavigationBar(
    selectedScreen: ScreenTab,
    ui: UiSettings,
    animatedCornerRadius: Float,
    readyBadgesCount: Int,
    onSelectScreen: (ScreenTab) -> Unit
) {
    val barShape = RoundedCornerShape(animatedCornerRadius.coerceIn(20f, 38f).dp)
    val containerColor = if (ui.glassEffectEnabled) {
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.90f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val tabs = ScreenTab.values()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = ui.navBarBottomPaddingDp.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(ui.navBarWidthFraction.coerceIn(0.78f, 0.98f))
                .height(64.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = barShape,
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
                ),
            shape = barShape,
            color = containerColor,
            tonalElevation = 8.dp
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                val totalWidth = maxWidth
                val weights = remember(selectedScreen, ui.navLabelVisibility) {
                    if (ui.navLabelVisibility == NavLabelVisibility.SELECTED_ONLY) {
                        tabs.map { if (it == selectedScreen) 0.32f else 0.17f }
                    } else {
                        List(tabs.size) { 1f / tabs.size }
                    }
                }

                val slotWidths = weights.map { totalWidth * it }
                val slotOffsets = remember(slotWidths) {
                    val offsets = ArrayList<Dp>(tabs.size)
                    var acc = 0.dp
                    for (w in slotWidths) {
                        offsets.add(acc)
                        acc += w
                    }
                    offsets
                }

                val activeSlotWidth = slotWidths[selectedScreen.index]
                val activeSlotOffset = slotOffsets[selectedScreen.index]

                val targetPillWidth = when (ui.navLabelVisibility) {
                    NavLabelVisibility.HIDE -> (activeSlotWidth * 0.76f).coerceIn(44.dp, 72.dp)
                    else -> (activeSlotWidth - 4.dp).coerceAtLeast(48.dp)
                }
                val targetPillOffset = activeSlotOffset + ((activeSlotWidth - targetPillWidth) / 2f)

                val animatedPillWidth by animateDpAsState(
                    targetValue = targetPillWidth,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "NavPillWidth"
                )

                val animatedPillOffset by animateDpAsState(
                    targetValue = targetPillOffset,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "NavPillOffset"
                )

                Box(
                    modifier = Modifier
                        .offset(x = animatedPillOffset)
                        .width(animatedPillWidth)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = selectedScreen == tab
                        val icon = when (tab) {
                            ScreenTab.TRACKER -> AppIcons.Home
                            ScreenTab.CUSTOM_PERIOD -> AppIcons.History
                            ScreenTab.LEADERBOARD -> AppIcons.Trophy
                            ScreenTab.PROFILE -> AppIcons.Person
                            ScreenTab.SETTINGS -> AppIcons.Settings
                        }

                        val animatedSlotWidth by animateDpAsState(
                            targetValue = slotWidths[index],
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "SlotWidth_${tab.name}"
                        )

                        val iconScale by animateFloatAsState(
                            targetValue = when {
                                isSelected && ui.navLabelVisibility == NavLabelVisibility.HIDE -> 1.24f
                                isSelected -> 1.08f
                                else -> 0.94f
                            },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "IconScale_${tab.name}"
                        )

                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = tween(220),
                            label = "NavContent_${tab.name}"
                        )

                        val interactionSource = remember { MutableInteractionSource() }

                        Box(
                            modifier = Modifier
                                .width(animatedSlotWidth)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) { onSelectScreen(tab) },
                            contentAlignment = Alignment.Center
                        ) {
                            when (ui.navLabelVisibility) {
                                NavLabelVisibility.ALWAYS -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.TopEnd) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = tab.title,
                                                tint = contentColor,
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .scale(iconScale)
                                            )
                                            if (tab == ScreenTab.PROFILE && readyBadgesCount > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .offset(x = 3.dp, y = (-2).dp)
                                                        .size(7.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFFF6B6B))
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = tab.title,
                                            color = contentColor,
                                            fontSize = 10.sp,
                                            lineHeight = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                NavLabelVisibility.SELECTED_ONLY -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.TopEnd) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = tab.title,
                                                tint = contentColor,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .scale(iconScale)
                                            )
                                            if (tab == ScreenTab.PROFILE && readyBadgesCount > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .offset(x = 3.dp, y = (-2).dp)
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFFF6B6B))
                                                )
                                            }
                                        }
                                        AnimatedVisibility(
                                            visible = isSelected,
                                            enter = expandHorizontally(
                                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                            ) + fadeIn(tween(180)),
                                            exit = shrinkHorizontally(
                                                animationSpec = tween(150)
                                            ) + fadeOut(tween(120))
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = tab.title,
                                                    color = contentColor,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }

                                NavLabelVisibility.HIDE -> {
                                    Box(contentAlignment = Alignment.TopEnd) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = tab.title,
                                            tint = contentColor,
                                            modifier = Modifier
                                                .size(22.dp)
                                                .scale(iconScale)
                                        )
                                        if (tab == ScreenTab.PROFILE && readyBadgesCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .offset(x = 4.dp, y = (-2).dp)
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFFF6B6B))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThemedCard(
    ui: UiSettings,
    cornerRadius: Float,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    val containerColor = when (ui.cardSurfaceStyle) {
        CardSurfaceStyle.TONAL -> MaterialTheme.colorScheme.surfaceContainerHigh
        CardSurfaceStyle.GLASS -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f)
        CardSurfaceStyle.GRADIENT -> MaterialTheme.colorScheme.surfaceContainerHigh
        CardSurfaceStyle.FLAT -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    val border = when {
        ui.showCardBorder -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ui.cardSurfaceStyle == CardSurfaceStyle.GLASS -> BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
        )
        else -> null
    }

    val cardModifier = if (onClick != null) {
        modifier
            .clip(shape)
            .clickable { onClick() }
    } else {
        modifier
    }

    Card(
        modifier = cardModifier,
        shape = shape,
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = ui.cardElevationDp.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        if (ui.cardSurfaceStyle == CardSurfaceStyle.GRADIENT) {
            val gradientBrush = Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                    MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(gradientBrush),
                content = content
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    }
}

@Composable
fun SettingCard(
    title: String,
    ui: UiSettings,
    cornerRadius: Float,
    content: @Composable ColumnScope.() -> Unit
) {
    ThemedCard(ui = ui, cornerRadius = cornerRadius) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
fun <T> AdaptiveChoiceGrid(
    items: List<T>,
    selectedItem: T,
    labelProvider: (T) -> String,
    columns: Int = 2,
    onSelect: (T) -> Unit
) {
    val rows = remember(items, columns) { items.chunked(columns) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    val isSelected = item == selectedItem
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        animationSpec = tween(220),
                        label = "ChoiceBg"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        animationSpec = tween(220),
                        label = "ChoiceContent"
                    )

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(bgColor)
                            .clickable { onSelect(item) }
                            .padding(horizontal = 10.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = AppIcons.Check,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                        Text(
                            text = labelProvider(item),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                if (rowItems.size < columns) {
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingSliderItem(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@Composable
fun SegmentedPeriodSelector(
    selectedPeriod: StatsPeriod,
    onSelectPeriod: (StatsPeriod) -> Unit
) {
    val periods = StatsPeriod.values()
    val selectedIndex = periods.indexOf(selectedPeriod)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp)
    ) {
        val segmentWidth = maxWidth / periods.size
        val indicatorOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "SegmentOffset"
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            periods.forEach { period ->
                val isSelected = period == selectedPeriod
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(200),
                    label = "PeriodTextColor_${period.name}"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable { onSelectPeriod(period) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = period.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun TotalTimeSummaryCard(
    totalMillis: Long,
    previousMillis: Long,
    period: StatsPeriod,
    ui: UiSettings,
    cornerRadius: Float
) {
    val periodMultiplier = when (period) {
        StatsPeriod.DAY -> 1
        StatsPeriod.WEEK -> 7
        StatsPeriod.MONTH -> 30
    }
    val periodGoalMillis = (ui.dailyGoalHours * 3600_000f * periodMultiplier).toLong().coerceAtLeast(60_000L)
    val effectiveBaseline = if (previousMillis > 60_000L) previousMillis else periodGoalMillis
    val maxRef = max(totalMillis, effectiveBaseline).coerceAtLeast(1L)
    val rawDiffRatio = ((totalMillis - effectiveBaseline).toDouble() / maxRef.toDouble()) * 100.0
    val isIncrease = rawDiffRatio >= 0.0
    val normalizedDiffPercent = if (totalMillis == 0L) 0 else abs(rawDiffRatio).roundToInt().coerceIn(1, 100)

    val arrow = if (isIncrease) "▲" else "▼"
    val badgeColor = if (isIncrease) Color(0xFFFF6B6B) else Color(0xFF00C896)

    val quotaPercent = ((totalMillis.toDouble() / periodGoalMillis.toDouble()) * 100.0)
        .roundToInt()
        .coerceIn(0, 100)
    val animatedQuotaFraction by animateFloatAsState(
        targetValue = (quotaPercent / 100f).coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "QuotaProgress"
    )

    ThemedCard(ui = ui, cornerRadius = cornerRadius) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Итого за ${period.genitiveTitle}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.18f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$arrow $normalizedDiffPercent%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = badgeColor,
                        maxLines = 1
                    )
                }
            }

            AnimatedContent(
                targetState = formatDuration(totalMillis, ui.timeFormatStyle),
                transitionSpec = {
                    fadeIn(tween(220)).togetherWith(fadeOut(tween(160)))
                },
                label = "TotalTimeValueAnim"
            ) { formattedTotal ->
                Text(
                    text = formattedTotal,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (previousMillis > 60_000L) {
                            "Прошлый: ${formatDuration(previousMillis, ui.timeFormatStyle)}"
                        } else {
                            "Ориентир: ${formatDuration(periodGoalMillis, ui.timeFormatStyle)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    Text(
                        text = "Норма: $quotaPercent%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }

                LinearProgressIndicator(
                    progress = { animatedQuotaFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = if (quotaPercent >= 95) Color(0xFFFF6B6B) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun WeeklyUsageBarChartCard(
    dailyMillis: List<Long>,
    ui: UiSettings,
    cornerRadius: Float
) {
    val dayLabels = remember { listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс") }
    val maxDayTime = (dailyMillis.maxOrNull() ?: 1L).coerceAtLeast(1L)
    val nonZeroDays = dailyMillis.filter { it > 60_000L }
    val avgDayTime = if (nonZeroDays.isNotEmpty()) nonZeroDays.sum() / nonZeroDays.size else 0L

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondaryContainer
    val trendColor = MaterialTheme.colorScheme.onPrimaryContainer
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val labelColorInt = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val valueColorInt = MaterialTheme.colorScheme.primary.toArgb()
    val density = LocalDensity.current

    val dayTextPaint = remember(labelColorInt, density) {
        android.graphics.Paint().apply {
            isAntiAlias = true
            color = labelColorInt
            textSize = with(density) { 12.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
        }
    }

    val valueTextPaint = remember(valueColorInt, density) {
        android.graphics.Paint().apply {
            isAntiAlias = true
            color = valueColorInt
            textSize = with(density) { 10.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
        }
    }

    val animatedFractions = List(7) { index ->
        val dayMs = dailyMillis.getOrElse(index) { 0L }
        val rawFraction = if (dayMs <= 0L) 0.04f else (dayMs.toFloat() / maxDayTime.toFloat()).coerceIn(0.06f, 1f)
        animateFloatAsState(
            targetValue = rawFraction,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "BarHeight_$index"
        ).value
    }

    ThemedCard(ui = ui, cornerRadius = cornerRadius) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Динамика за неделю",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                if (avgDayTime > 0L) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "Среднее: ${formatShortBarTime(avgDayTime)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val count = 7
                val slotWidth = size.width / count
                val barWidth = slotWidth * 0.48f
                val topReserved = if (ui.showChartValues) 24.dp.toPx() else 6.dp.toPx()
                val bottomReserved = 30.dp.toPx()
                val maxBarHeight = size.height - topReserved - bottomReserved
                val trendPoints = ArrayList<Offset>(count)

                for (i in 0 until count) {
                    val centerX = slotWidth * i + slotWidth / 2f
                    val barLeft = centerX - barWidth / 2f

                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(barLeft, topReserved),
                        size = Size(barWidth, maxBarHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )

                    val barHeight = maxBarHeight * animatedFractions[i]
                    val barTop = topReserved + (maxBarHeight - barHeight)

                    if (ui.gradientChartBars) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor, secondaryColor),
                                startY = barTop,
                                endY = topReserved + maxBarHeight
                            ),
                            topLeft = Offset(barLeft, barTop),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )
                    } else {
                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(barLeft, barTop),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )
                    }

                    val dayMs = dailyMillis.getOrElse(i) { 0L }
                    if (dayMs > 0L) {
                        trendPoints.add(Offset(centerX, barTop + (barWidth / 3f).coerceAtMost(barHeight / 2f)))
                    }

                    if (ui.showChartValues) {
                        val shortLabel = formatShortBarTime(dayMs)
                        drawContext.canvas.nativeCanvas.drawText(
                            shortLabel,
                            centerX,
                            (barTop - 6.dp.toPx()).coerceAtLeast(12.dp.toPx()),
                            valueTextPaint
                        )
                    }

                    drawContext.canvas.nativeCanvas.drawText(
                        dayLabels[i],
                        centerX,
                        size.height - 6.dp.toPx(),
                        dayTextPaint
                    )
                }

                if (ui.showChartAverageLine && avgDayTime > 0L) {
                    val avgFraction = (avgDayTime.toFloat() / maxDayTime.toFloat()).coerceIn(0.05f, 1f)
                    val avgY = topReserved + maxBarHeight * (1f - avgFraction)
                    drawLine(
                        color = primaryColor.copy(alpha = 0.55f),
                        start = Offset(0f, avgY),
                        end = Offset(size.width, avgY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                    )
                }

                if (ui.showChartTrendLine && trendPoints.size >= 2) {
                    val path = Path().apply {
                        moveTo(trendPoints[0].x, trendPoints[0].y)
                        for (i in 1 until trendPoints.size) {
                            lineTo(trendPoints[i].x, trendPoints[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = trendColor.copy(alpha = 0.70f),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    trendPoints.forEach { pt ->
                        drawCircle(
                            color = trendColor,
                            radius = 3.5.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppUsageItemCard(
    rankIndex: Int,
    app: AppUsageModel,
    limitMinutes: Int,
    totalPeriodMillis: Long,
    maxTimeMillis: Long,
    ui: UiSettings,
    cornerRadius: Float,
    showInstallDate: Boolean,
    showRankMedal: Boolean,
    onClick: () -> Unit
) {
    val clipShape: Shape = when (ui.iconShape) {
        AppIconShape.CIRCLE -> CircleShape
        AppIconShape.ROUNDED -> RoundedCornerShape((ui.appIconSizeDp * 0.32f).dp)
        AppIconShape.SQUARE -> RectangleShape
    }

    val targetProgress = (app.timeInMillis.toFloat() / maxTimeMillis.toFloat()).coerceIn(0.02f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "AppProgress_${app.packageName}"
    )

    val sharePercent = ((app.timeInMillis.toDouble() / totalPeriodMillis.toDouble()) * 100.0)
        .roundToInt()
        .coerceIn(1, 100)
    val barShape = if (ui.roundedProgressBar) CircleShape else RectangleShape

    val isLimitExceeded = limitMinutes > 0 && (app.timeInMillis / 60_000L) >= limitMinutes
    val rankMedal = when (rankIndex) {
        0 -> "🥇 #1"
        1 -> "🥈 #2"
        2 -> "🥉 #3"
        else -> "#${rankIndex + 1}"
    }

    val installDateFormatted = remember(app.installTimeMillis) {
        if (app.installTimeMillis > 0L) {
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(app.installTimeMillis))
        } else null
    }

    ThemedCard(
        ui = ui,
        cornerRadius = cornerRadius,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ui.listDensity.cardPaddingDp.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(ui.appIconSizeDp.dp)
                        .clip(clipShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    if (app.icon != null) {
                        AndroidView(
                            factory = { ctx ->
                                ImageView(ctx).apply {
                                    scaleType = ImageView.ScaleType.FIT_CENTER
                                }
                            },
                            update = { imageView ->
                                if (imageView.drawable !== app.icon) {
                                    imageView.setImageDrawable(app.icon)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = AppIcons.PlayArrow,
                            contentDescription = app.appName,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (showRankMedal && rankIndex < 5) {
                            Surface(
                                shape = CircleShape,
                                color = if (rankIndex < 3) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                }
                            ) {
                                Text(
                                    text = rankMedal,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    if (ui.showPackageNames) {
                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (showInstallDate && installDateFormatted != null) {
                        Text(
                            text = "Скачано: $installDateFormatted",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (limitMinutes > 0) {
                        Text(
                            text = if (isLimitExceeded) "⚠️ Лимит ${limitMinutes}м превышен"
                            else "⏳ Лимит: ${limitMinutes}м",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isLimitExceeded) Color(0xFFFF6B6B) else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = CircleShape,
                        color = if (isLimitExceeded) {
                            Color(0xFFFF6B6B).copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    ) {
                        Text(
                            text = formatDuration(app.timeInMillis, ui.timeFormatStyle),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isLimitExceeded) {
                                Color(0xFFFF6B6B)
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                        )
                    }
                    if (ui.showSharePercent) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$sharePercent% от общего",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (ui.showProgressBar) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ui.progressThickness.dp)
                        .clip(barShape),
                    color = if (isLimitExceeded) Color(0xFFFF6B6B) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = if (ui.roundedProgressBar) StrokeCap.Round else StrokeCap.Butt
                )
            }
        }
    }
}

@Composable
fun PlayerSubmitCard(
    playerName: String,
    todayMinutes: Long,
    isLoading: Boolean,
    statusMessage: String?,
    ui: UiSettings,
    cornerRadius: Float,
    onNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onRefresh: () -> Unit
) {
    ThemedCard(ui = ui, cornerRadius = cornerRadius) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Глобальный рейтинг Firebase",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Ваше время за сегодня: ${formatDuration(todayMinutes * 60_000L, ui.timeFormatStyle)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "$todayMinutes мин",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            OutlinedTextField(
                value = playerName,
                onValueChange = onNameChange,
                label = { Text("Ваш никнейм в Топ-50") },
                leadingIcon = {
                    Icon(
                        imageVector = AppIcons.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSubmit,
                    enabled = !isLoading,
                    shape = CircleShape,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Отправить результат",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = onRefresh,
                    enabled = !isLoading,
                    shape = CircleShape
                ) {
                    Text("Обновить")
                }
            }

            AnimatedVisibility(visible = isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    strokeCap = StrokeCap.Round
                )
            }

            AnimatedVisibility(visible = !statusMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LeaderboardItemCard(
    rank: Int,
    entry: LeaderboardEntry,
    maxMinutesInTop: Long,
    isCurrentUser: Boolean,
    ui: UiSettings,
    cornerRadius: Float
) {
    val medalText = when (rank) {
        1 -> "🥇 #1"
        2 -> "🥈 #2"
        3 -> "🥉 #3"
        else -> "#$rank"
    }

    val fraction = (entry.minutes.toFloat() / maxMinutesInTop.coerceAtLeast(1L).toFloat()).coerceIn(0.04f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "LeaderProgress_${entry.uid}"
    )

    val barShape = if (ui.roundedProgressBar) CircleShape else RectangleShape

    ThemedCard(ui = ui, cornerRadius = cornerRadius) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isCurrentUser) {
                        Modifier.border(
                            1.5.dp,
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(cornerRadius.dp)
                        )
                    } else Modifier
                )
                .padding(ui.listDensity.cardPaddingDp.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (rank <= 3) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    }
                ) {
                    Text(
                        text = medalText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = if (isCurrentUser) "${entry.name} (Вы)" else entry.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (ui.showPackageNames) {
                        Text(
                            text = "UID: ${entry.uid.take(8).ifEmpty { "local" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = formatDuration(entry.minutes * 60_000L, ui.timeFormatStyle),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            if (ui.showProgressBar) {
                LinearProgressIndicator(
                    progress = { animatedFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ui.progressThickness.dp)
                        .clip(barShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = if (ui.roundedProgressBar) StrokeCap.Round else StrokeCap.Butt
                )
            }
        }
    }
}

@Composable
fun AppDetailsAndLimitDialog(
    app: AppUsageModel,
    periodLabel: String,
    totalTimeMillis: Long,
    timeFormatStyle: TimeFormatStyle,
    existingLimitMinutes: Int,
    onDismiss: () -> Unit,
    onSaveLimit: (Int) -> Unit
) {
    val context = LocalContext.current
    val sharePercent = ((app.timeInMillis.toDouble() / totalTimeMillis.coerceAtLeast(1L).toDouble()) * 100.0)
        .roundToInt()
        .coerceIn(1, 100)
    var sliderLimitMins by remember(app.packageName) {
        mutableFloatStateOf(existingLimitMinutes.toFloat())
    }
    val installDateStr = remember(app.installTimeMillis) {
        if (app.installTimeMillis > 0L) {
            SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(app.installTimeMillis))
        } else {
            "Неизвестно"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = app.appName,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Пакет: ${app.packageName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Скачано: $installDateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Время ($periodLabel):",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatDuration(app.timeInMillis, timeFormatStyle),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Доля от общего времени: $sharePercent%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val snappedMins = ((sliderLimitMins / 15f).roundToInt() * 15).coerceIn(0, 240)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Дневной лимит",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (snappedMins == 0) "Без лимита" else "$snappedMins мин",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = sliderLimitMins,
                        onValueChange = { sliderLimitMins = it },
                        valueRange = 0f..240f,
                        steps = 15
                    )
                    Text(
                        text = "Установите лимит > 0 мин, чтобы контролировать время и открыть достижение «Контролер лимитов».",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val snappedMins = ((sliderLimitMins / 15f).roundToInt() * 15).coerceIn(0, 240)
                    onSaveLimit(snappedMins)
                },
                shape = CircleShape
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            FilledTonalButton(
                onClick = {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                    if (launchIntent != null) {
                        context.startActivity(launchIntent)
                    } else {
                        Toast.makeText(context, "Не удалось запустить приложение", Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                },
                shape = CircleShape
            ) {
                Text("Запустить")
            }
        }
    )
}

@Composable
fun AccentColorCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "AccentScale"
    )
    Box(
        modifier = Modifier
            .size(48.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = AppIcons.Check,
                contentDescription = "Выбрано",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

fun formatDuration(
    millis: Long,
    style: TimeFormatStyle = TimeFormatStyle.STANDARD
): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0L)
    val totalMinutes = totalSeconds / 60
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return when (style) {
        TimeFormatStyle.STANDARD -> when {
            hours > 0 -> "${hours}ч ${minutes}м"
            minutes > 0 -> "${minutes}м ${seconds}с"
            else -> "${seconds}с"
        }
        TimeFormatStyle.PRECISE -> when {
            hours > 0 -> "${hours}ч ${minutes}м ${seconds}с"
            minutes > 0 -> "${minutes}м ${seconds}с"
            else -> "${seconds}с"
        }
        TimeFormatStyle.MINUTES -> {
            if (totalMinutes > 0) "$totalMinutes мин" else "$seconds сек"
        }
        TimeFormatStyle.DIGITAL -> {
            String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
        }
    }
}

fun formatShortBarTime(millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    return when {
        hours > 0 -> "${hours}ч"
        mins > 0 -> "${mins}м"
        else -> "0м"
    }
}

object AppIcons {

    val Search: ImageVector by lazy {
        ImageVector.Builder("Search", 24.dp, 24.dp, 24f, 24f).path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(11f, 4f)
            arcTo(7f, 7f, 0f, isMoreThanHalf = true, isPositiveArc = true, 10.99f, 4f)
            close()
            moveTo(16.2f, 16.2f); lineTo(21f, 21f)
        }.build()
    }

    val DateRange: ImageVector by lazy {
        ImageVector.Builder("DateRange", 24.dp, 24.dp, 24f, 24f).path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(4f, 6f); lineTo(20f, 6f); lineTo(20f, 20f); lineTo(4f, 20f); close()
            moveTo(16f, 3f); lineTo(16f, 7f)
            moveTo(8f, 3f); lineTo(8f, 7f)
            moveTo(4f, 10f); lineTo(20f, 10f)
            moveTo(8f, 14f); lineTo(10f, 14f)
            moveTo(14f, 14f); lineTo(16f, 14f)
        }.build()
    }

    val History: ImageVector by lazy {
        ImageVector.Builder("History", 24.dp, 24.dp, 24f, 24f).path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.1f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 4f)
            arcTo(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = true, 5.3f, 7.6f)
            moveTo(4f, 4f); lineTo(4f, 8f); lineTo(8f, 8f)
            moveTo(12f, 8f); lineTo(12f, 12.5f); lineTo(15.5f, 14.5f)
        }.build()
    }

    val Trophy: ImageVector by lazy {
        ImageVector.Builder("Trophy", 24.dp, 24.dp, 24f, 24f).path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(7f, 4f); lineTo(17f, 4f); lineTo(17f, 10f)
            arcTo(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 7f, 10f)
            close()
            moveTo(7f, 6f); lineTo(4f, 6f); lineTo(4f, 9f)
            arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = false, 7f, 11f)
            moveTo(17f, 6f); lineTo(20f, 6f); lineTo(20f, 9f)
            arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 17f, 11f)
            moveTo(12f, 15f); lineTo(12f, 20f)
            moveTo(8f, 20f); lineTo(16f, 20f)
        }.build()
    }

    val Settings: ImageVector by lazy {
        ImageVector.Builder("Settings", 24.dp, 24.dp, 24f, 24f).path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 8.5f)
            arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 11.99f, 8.5f)
            close()
            moveTo(12f, 2f); lineTo(12f, 5f)
            moveTo(12f, 19f); lineTo(12f, 22f)
            moveTo(2f, 12f); lineTo(5f, 12f)
            moveTo(19f, 12f); lineTo(22f, 12f)
            moveTo(4.9f, 4.9f); lineTo(7.1f, 7.1f)
            moveTo(16.9f, 16.9f); lineTo(19.1f, 19.1f)
            moveTo(19.1f, 4.9f); lineTo(16.9f, 7.1f)
            moveTo(7.1f, 16.9f); lineTo(4.9f, 19.1f)
        }.build()
    }

    val Refresh: ImageVector by lazy {
        ImageVector.Builder("Refresh", 24.dp, 24.dp, 24f, 24f).path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(20f, 11f)
            arcTo(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = false, 18f, 17.5f)
            moveTo(20f, 5f); lineTo(20f, 11f); lineTo(14f, 11f)
        }.build()
    }

    val PlayArrow: ImageVector by lazy {
        ImageVector.Builder("PlayArrow", 24.dp, 24.dp, 24f, 24f).path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(8f, 5f); lineTo(19f, 12f); lineTo(8f, 19f); close()
        }.build()
    }

    val Notifications: ImageVector by lazy {
        ImageVector.Builder("Notifications", 24.dp, 24.dp, 24f, 24f).path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(18f, 8f)
            arcTo(6f, 6f, 0f, isMoreThanHalf = false, isPositiveArc = false, 6f, 8f)
            lineTo(6f, 15f); lineTo(4f, 17f); lineTo(20f, 17f); lineTo(18f, 15f); close()
            moveTo(10f, 20f); lineTo(14f, 20f)
        }.build()
    }

    val Star: ImageVector by lazy {
        ImageVector.Builder("Star", 24.dp, 24.dp, 24f, 24f).path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(12f, 2f); lineTo(15.09f, 8.26f); lineTo(22f, 9.27f)
            lineTo(17f, 14.14f); lineTo(18.18f, 21.02f); lineTo(12f, 17.77f)
            lineTo(5.82f, 21.02f); lineTo(7f, 14.14f); lineTo(2f, 9.27f)
            lineTo(8.91f, 8.26f); close()
        }.build()
    }

    val Lock: ImageVector by lazy {
        ImageVector.Builder("Lock", 24.dp, 24.dp, 24f, 24f).path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(5f, 11f); lineTo(19f, 11f); lineTo(19f, 21f); lineTo(5f, 21f); close()
            moveTo(8f, 11f); lineTo(8f, 7f)
            arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 16f, 7f)
            lineTo(16f, 11f)
        }.build()
    }

    val Check: ImageVector by lazy {
        ImageVector.Builder("Check", 24.dp, 24.dp, 24f, 24f).path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(20f, 6f); lineTo(9f, 17f); lineTo(4f, 12f)
        }.build()
    }

    val Close: ImageVector by lazy {
        ImageVector.Builder("Close", 24.dp, 24.dp, 24f, 24f).path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(18f, 6f); lineTo(6f, 18f)
            moveTo(6f, 6f); lineTo(18f, 18f)
        }.build()
    }

    val Home: ImageVector by lazy {
        ImageVector.Builder("Home", 24.dp, 24.dp, 24f, 24f).path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(3f, 10f); lineTo(12f, 3f); lineTo(21f, 10f); lineTo(21f, 20f)
            lineTo(15f, 20f); lineTo(15f, 14f); lineTo(9f, 14f); lineTo(9f, 20f)
            lineTo(3f, 20f); close()
        }.build()
    }

    val Person: ImageVector by lazy {
        ImageVector.Builder("Person", 24.dp, 24.dp, 24f, 24f).path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 4f)
            arcTo(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 11.99f, 4f)
            close()
            moveTo(4f, 20f)
            arcTo(8f, 7f, 0f, isMoreThanHalf = false, isPositiveArc = true, 20f, 20f)
        }.build()
    }
}
