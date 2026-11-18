package com.example.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.example.ui.AppTheme
import com.example.ui.ThemeStyles
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

/**
 * Shared 120Hz-tuned physics motion specs across the app.
 */
object MotionSpecs {
    val SnappyFloatSpring = spring<Float>(
        dampingRatio = 0.74f,
        stiffness = 780f
    )
    val BouncyPopSpring = spring<Float>(
        dampingRatio = 0.54f,
        stiffness = 680f
    )
    val SlideIntOffsetSpring = spring<IntOffset>(
        dampingRatio = 0.86f,
        stiffness = 580f
    )
    val CardRevealOffsetSpring = spring<IntOffset>(
        dampingRatio = 0.82f,
        stiffness = 540f
    )
    val CardRevealSizeSpring = spring<IntSize>(
        dampingRatio = 0.80f,
        stiffness = 520f
    )
}

/**
 * Hardware-accelerated GPU press compression + subtle 3D physical press sink on tap.
 * Reads PointerEventPass.Initial without consuming events and releases immediately if a scroll gesture begins.
 */
fun Modifier.springPressScale(
    pressedScale: Float = 0.96f,
    depressDp: Dp = 1.dp,
    enabled: Boolean = true
): Modifier = composed {
    if (!enabled) return@composed this
    var isPressed by remember { mutableStateOf(false) }
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.58f,
            stiffness = 920f
        ),
        label = "spring_press_progress"
    )
    this
        .graphicsLayer {
            val s = 1f - (1f - pressedScale) * pressProgress
            scaleX = s
            scaleY = s
            translationY = depressDp.toPx() * pressProgress
        }
        .pointerInput(enabled) {
            val slopPx = viewConfiguration.touchSlop
            awaitPointerEventScope {
                var downPos = Offset.Zero
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    when (event.type) {
                        PointerEventType.Press -> {
                            downPos = event.changes.firstOrNull()?.position ?: Offset.Zero
                            isPressed = true
                        }
                        PointerEventType.Move -> {
                            if (isPressed) {
                                val first = event.changes.firstOrNull()
                                if (first == null ||
                                    first.isConsumed ||
                                    abs(first.position.x - downPos.x) > slopPx ||
                                    abs(first.position.y - downPos.y) > slopPx
                                ) {
                                    isPressed = false
                                }
                            }
                        }
                        PointerEventType.Release, PointerEventType.Exit -> isPressed = false
                        else -> {
                            if (isPressed && event.changes.none { it.pressed }) {
                                isPressed = false
                            }
                        }
                    }
                }
            }
        }
}

/**
 * Lightweight 2D soft-sheen studio card surface:
 * - Uniform full-surface diagonal sheen clipped strictly to the shape outline (eliminates corner glow + unlit center box).
 * - Crisp 2D top specular bevel & bottom rim drawn via drawOutline (follows circles, pills, squircles & diamonds cleanly).
 * - Zero RenderNode shadowElevation passes during list/grid scrolling for locked 120Hz frame pacing.
 */
fun Modifier.surface3DCard(
    shape: Shape,
    isLight: Boolean,
    primaryColor: Color = Color.Unspecified,
    isGlass: Boolean = false,
    elevationDp: Dp = 6.dp
): Modifier = this.drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val strength = (elevationDp.value / 8f).coerceIn(0.45f, 1.25f)

    val topSheenAlpha = when {
        isGlass && isLight -> 0.24f
        isGlass && !isLight -> 0.12f
        isLight -> 0.20f
        else -> 0.08f
    } * strength.coerceAtMost(1.1f)

    val midAccent = if (primaryColor != Color.Unspecified) {
        primaryColor.copy(alpha = if (isLight) 0.045f else 0.06f)
    } else {
        Color.White.copy(alpha = if (isLight) 0.06f else 0.025f)
    }

    val fullSurfaceSheen = Brush.linearGradient(
        0f to Color.White.copy(alpha = topSheenAlpha.coerceIn(0f, 0.30f)),
        0.52f to midAccent,
        1f to Color.Black.copy(alpha = if (isLight) 0.03f else 0.09f),
        start = Offset.Zero,
        end = Offset(size.width, size.height)
    )

    val bevelTop = Color.White.copy(
        alpha = when {
            isGlass && !isLight -> 0.24f
            isGlass && isLight -> 0.68f
            isLight -> 0.56f
            else -> 0.14f
        }
    )
    val bevelBottom = Color.Black.copy(alpha = if (isLight) 0.07f else 0.26f)
    val bevelRimBrush = Brush.verticalGradient(
        0f to bevelTop,
        0.45f to Color.Transparent,
        1f to bevelBottom
    )
    val strokePx = 1.dp.toPx()

    onDrawWithContent {
        drawContent()
        drawOutline(outline = outline, brush = fullSurfaceSheen)
        drawOutline(
            outline = outline,
            brush = bevelRimBrush,
            style = Stroke(width = strokePx)
        )
    }
}

/**
 * Lightweight 2D recessed well for progress tracks, segmented control trays, empty cells & search fields.
 * Draws directly via drawOutline so edges stay curved and crisp with zero Path/blur overhead.
 */
fun Modifier.recessedWell(
    shape: Shape,
    isLight: Boolean,
    wellTint: Color = Color.Transparent
): Modifier = this.drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val topShadow = Color.Black.copy(alpha = if (isLight) 0.08f else 0.24f)
    val bottomCatch = Color.White.copy(alpha = if (isLight) 0.36f else 0.08f)
    val insetBrush = Brush.verticalGradient(
        0f to topShadow,
        0.35f to Color.Transparent,
        0.82f to Color.Transparent,
        1f to bottomCatch
    )
    val innerRim = Brush.verticalGradient(
        0f to Color.Black.copy(alpha = if (isLight) 0.10f else 0.28f),
        0.5f to Color.Transparent,
        1f to Color.White.copy(alpha = if (isLight) 0.36f else 0.08f)
    )
    val strokePx = 1.dp.toPx()

    onDrawWithContent {
        if (wellTint != Color.Transparent) {
            drawOutline(outline = outline, color = wellTint)
        }
        drawContent()
        drawOutline(outline = outline, brush = insetBrush)
        drawOutline(
            outline = outline,
            brush = innerRim,
            style = Stroke(width = strokePx)
        )
    }
}

/**
 * Smooth rolling vertical digit/counter animation.
 */
@Composable
fun AnimatedNumberText(
    value: Int,
    suffix: String = "",
    color: Color,
    fontSize: TextUnit,
    fontFamily: FontFamily,
    fontWeight: FontWeight = FontWeight.Bold,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            val up = targetState >= initialState
            (slideInVertically(
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 700f),
                initialOffsetY = { if (up) it / 2 else -it / 2 }
            ) + fadeIn(tween(150))) togetherWith (slideOutVertically(
                animationSpec = tween(130),
                targetOffsetY = { if (up) -it / 2 else it / 2 }
            ) + fadeOut(tween(120)))
        },
        modifier = modifier,
        label = "animated_number_text"
    ) { target ->
        Text(
            text = "$target$suffix",
            color = color,
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            maxLines = 1
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object AgslShaderLibrary {
    private const val LIQUID_SILK_AGSL = """
        uniform float2 iResolution;
        uniform float iTime;
        uniform half4 colorStart;
        uniform half4 colorEnd;
        uniform half4 colorAccent;
        uniform half4 colorTertiary;

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / max(iResolution, float2(1.0, 1.0));
            float t = iTime * 0.45;
            float wave1 = sin(uv.x * 3.4 + t + cos(uv.y * 2.6 - t * 0.7));
            float wave2 = cos(uv.y * 3.1 - t * 0.8 + sin(uv.x * 2.3 + t * 0.5));
            float blend = clamp(0.5 + 0.35 * (wave1 * 0.6 + wave2 * 0.4), 0.0, 1.0);
            half4 base = mix(colorStart, colorEnd, uv.y * 0.7 + uv.x * 0.3);
            half4 silk = mix(colorAccent, colorTertiary, blend);
            float centerMask = smoothstep(0.0, 0.22, uv.x) * smoothstep(1.0, 0.78, uv.x) *
                               smoothstep(0.0, 0.16, uv.y) * smoothstep(1.0, 0.84, uv.y);
            float highlight = smoothstep(0.28, 0.92, 0.5 + 0.5 * sin(uv.x * 4.5 + uv.y * 3.2 - t * 1.1));
            return mix(base, silk, highlight * 0.32 * (0.35 + 0.65 * centerMask));
        }
    """

    private const val FROZEN_LIQUID_AGSL = """
        uniform float2 iResolution;
        uniform float iTime;
        uniform half4 colorStart;
        uniform half4 colorEnd;
        uniform half4 colorAccent;
        uniform half4 colorTertiary;

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / max(iResolution, float2(1.0, 1.0));
            float t = iTime * 0.36;
            float p1 = sin(uv.x * 5.2 + uv.y * 4.0 + t);
            float p2 = cos(uv.x * 4.6 - uv.y * 5.0 - t * 0.85);
            float caustic = pow(abs(sin((p1 + p2) * 1.7)), 3.0);
            float frostWave = 0.5 + 0.5 * sin(uv.y * 3.4 + cos(uv.x * 2.8 + t * 0.6));
            float centerMask = smoothstep(0.02, 0.24, uv.x) * smoothstep(0.98, 0.76, uv.x) *
                               smoothstep(0.02, 0.20, uv.y) * smoothstep(0.98, 0.80, uv.y);
            half4 base = mix(colorStart, colorEnd, uv.y * 0.82 + frostWave * 0.18);
            half4 iceGlow = mix(colorAccent, colorTertiary, caustic);
            return mix(base, iceGlow, clamp((caustic * 0.38 + frostWave * 0.12) * centerMask, 0.0, 0.48));
        }
    """

    private const val VOLCANO_MAGMA_AGSL = """
        uniform float2 iResolution;
        uniform float iTime;
        uniform half4 colorStart;
        uniform half4 colorEnd;
        uniform half4 colorAccent;
        uniform half4 colorTertiary;

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / max(iResolution, float2(1.0, 1.0));
            float t = iTime * 0.52;
            float flow = uv.y * 1.4 - t * 0.42;
            float vein1 = sin(uv.x * 5.5 + sin(flow * 3.2 + uv.x * 2.1));
            float vein2 = cos(uv.x * 3.7 - flow * 4.0 + cos(uv.y * 5.0));
            float magmaMask = smoothstep(0.15, 0.95, 0.5 + 0.5 * (vein1 * 0.55 + vein2 * 0.45));
            float heatPool = smoothstep(0.25, 0.92, uv.y) * 0.62 + 0.22;
            half4 basalt = mix(colorStart, colorEnd, uv.y);
            half4 lava = mix(colorAccent, colorTertiary, magmaMask * magmaMask);
            return mix(basalt, lava, clamp(magmaMask * heatPool * 0.58, 0.0, 0.60));
        }
    """

    private const val AURORA_PLASMA_AGSL = """
        uniform float2 iResolution;
        uniform float iTime;
        uniform half4 colorStart;
        uniform half4 colorEnd;
        uniform half4 colorAccent;
        uniform half4 colorTertiary;

        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / max(iResolution, float2(1.0, 1.0));
            float t = iTime * 0.46;
            float curtain = sin(uv.x * 4.2 + t + sin(uv.y * 3.0 - t * 0.5) * 1.3);
            float ribbon = smoothstep(-0.2, 0.85, curtain);
            float verticalGlow = exp(-pow((uv.y - (0.42 + 0.16 * sin(uv.x * 3.0 + t * 0.7))) * 2.8, 2.0));
            float safeX = smoothstep(0.0, 0.18, uv.x) * smoothstep(1.0, 0.82, uv.x);
            half4 sky = mix(colorStart, colorEnd, uv.y);
            half4 auroraCol = mix(colorAccent, colorTertiary, 0.5 + 0.5 * cos(uv.x * 2.5 - t));
            return mix(sky, auroraCol, clamp((ribbon * 0.24 + verticalGlow * 0.40) * (0.4 + 0.6 * safeX), 0.0, 0.58));
        }
    """

    fun createShader(effectStyle: String): RuntimeShader? {
        val source = when (effectStyle) {
            "frozen_liquid" -> FROZEN_LIQUID_AGSL
            "volcano_magma" -> VOLCANO_MAGMA_AGSL
            "aurora_plasma", "cosmic_prism" -> AURORA_PLASMA_AGSL
            else -> LIQUID_SILK_AGSL
        }
        return try {
            RuntimeShader(source)
        } catch (_: Throwable) {
            null
        }
    }

    fun updateBrush(
        shader: RuntimeShader,
        width: Float,
        height: Float,
        timeSeconds: Float,
        startArgb: Int,
        endArgb: Int,
        accentArgb: Int,
        tertiaryArgb: Int
    ): ShaderBrush? {
        if (width <= 1f || height <= 1f) return null
        return try {
            shader.setFloatUniform("iResolution", width, height)
            shader.setFloatUniform("iTime", timeSeconds)
            shader.setColorUniform("colorStart", startArgb)
            shader.setColorUniform("colorEnd", endArgb)
            shader.setColorUniform("colorAccent", accentArgb)
            shader.setColorUniform("colorTertiary", tertiaryArgb)
            ShaderBrush(shader)
        } catch (_: Throwable) {
            null
        }
    }
}

/**
 * Draws lightweight procedural atmospheric particles well inside safe display bounds (no hazy corner bleed).
 */
private fun DrawScope.drawDynamicAtmosphericParticles(
    effectStyle: String,
    phase: Float,
    isDark: Boolean,
    accent: Color,
    tertiary: Color
) {
    val twoPi = (2.0 * PI).toFloat()
    when (effectStyle) {
        "volcano_magma" -> {
            for (i in 0 until 7) {
                val seedX = (14 + (i * 73 + 19) % 72) / 100f
                val speed = 1f + (i % 3) * 0.30f
                val progress = (phase * speed + i * 0.14f) % 1f
                val emberY = size.height * (0.94f - progress * 0.84f)
                val emberX = size.width * seedX + sin((phase * 3f + i) * twoPi) * 8f
                val fade = (1f - progress).coerceIn(0f, 1f) * sin(progress * PI.toFloat()).coerceAtLeast(0.15f)
                val emberColor = if (i % 2 == 0) tertiary else accent
                val emberRadius = 2.0f + (i % 3) * 1.0f
                drawCircle(
                    color = emberColor.copy(alpha = fade * 0.40f),
                    radius = emberRadius * 1.8f,
                    center = Offset(emberX, emberY)
                )
                drawCircle(
                    color = Color.White.copy(alpha = fade * 0.65f),
                    radius = emberRadius * 0.55f,
                    center = Offset(emberX, emberY)
                )
            }
        }
        "frozen_liquid" -> {
            for (i in 0 until 6) {
                val baseX = (14 + (i * 61 + 11) % 72) / 100f
                val baseY = (14 + (i * 37 + 17) % 72) / 100f
                val x = size.width * baseX + cos((phase + i * 0.17f) * twoPi) * 6f
                val y = size.height * baseY + sin((phase + i * 0.23f) * twoPi) * 5f
                val twinkle = 0.35f + 0.65f * ((sin((phase * 2f + i) * twoPi) + 1f) * 0.5f)
                val r = 2.6f + (i % 3) * 1.0f
                val glintColor = (if (isDark) Color(0xFFD6F3FF) else accent).copy(alpha = twinkle * 0.42f)
                drawLine(glintColor, Offset(x - r, y), Offset(x + r, y), strokeWidth = 1.2f)
                drawLine(glintColor, Offset(x, y - r), Offset(x, y + r), strokeWidth = 1.2f)
            }
        }
        "aurora_plasma", "cosmic_prism" -> {
            for (i in 0 until 6) {
                val sx = (12 + (i * 89 + 7) % 76) / 100f * size.width
                val sy = (12 + (i * 53 + 23) % 76) / 100f * size.height
                val pulse = 0.25f + 0.75f * ((sin((phase * 2f + i * 0.4f) * twoPi) + 1f) * 0.5f)
                drawCircle(
                    color = Color.White.copy(alpha = pulse * (if (isDark) 0.42f else 0.48f)),
                    radius = 1.5f + (i % 2) * 0.8f,
                    center = Offset(sx, sy)
                )
            }
        }
    }
}

/**
 * Full-bleed zero-recomposition ambient backdrop calibrated for smooth scrolling & tab switching.
 * - Defaults to static cached gradients via drawWithCache (zero continuous GPU invalidation).
 * - When [animateMotion] is enabled on a Glass/Elemental theme, opts into the gentle AGSL shader drift.
 */
@Composable
fun LiquidAmbientBackdrop(
    selectedTheme: String,
    customPrimaryHex: String = "",
    customBackgroundHex: String = "",
    animateMotion: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isGlass = ThemeStyles.isGlassTheme(selectedTheme)
    val appTheme = ThemeStyles.getAppTheme(selectedTheme)
    val baseBg = ThemeStyles.getBackgroundColor(selectedTheme, customBackgroundHex)
    val primary = ThemeStyles.getPrimaryColor(selectedTheme, customPrimaryHex)

    if (!isGlass || !animateMotion) {
        val bgStart = if (isGlass) appTheme.ambientStart else baseBg
        val bgEnd = if (isGlass) appTheme.ambientEnd else baseBg
        val highlight = if (isGlass) appTheme.ambientHighlight else primary
        val tertiary = if (isGlass) appTheme.ambientTertiary else primary

        Box(modifier = modifier) {
            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        if (!isGlass) {
                            onDrawBehind {
                                drawRect(color = bgStart)
                            }
                        } else {
                            val verticalWash = Brush.verticalGradient(
                                0f to bgStart,
                                0.55f to bgEnd,
                                1f to bgStart
                            )
                            val highlightRadius = size.maxDimension * 0.72f
                            val highlightCenter = Offset(size.width * 0.22f, size.height * 0.18f)
                            val highlightOrb = Brush.radialGradient(
                                colors = listOf(highlight.copy(alpha = if (appTheme.isDark) 0.22f else 0.18f), Color.Transparent),
                                center = highlightCenter,
                                radius = highlightRadius.coerceAtLeast(1f)
                            )
                            val tertiaryRadius = size.maxDimension * 0.68f
                            val tertiaryCenter = Offset(size.width * 0.84f, size.height * 0.78f)
                            val tertiaryOrb = Brush.radialGradient(
                                colors = listOf(tertiary.copy(alpha = if (appTheme.isDark) 0.18f else 0.14f), Color.Transparent),
                                center = tertiaryCenter,
                                radius = tertiaryRadius.coerceAtLeast(1f)
                            )
                            onDrawBehind {
                                drawRect(brush = verticalWash)
                                drawCircle(brush = highlightOrb, radius = highlightRadius, center = highlightCenter)
                                drawCircle(brush = tertiaryOrb, radius = tertiaryRadius, center = tertiaryCenter)
                            }
                        }
                    }
            )

            content()
        }
        return
    }

    val animBgStartState: State<Color> = animateColorAsState(
        targetValue = appTheme.ambientStart,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "liquid_bg_start"
    )
    val animBgEndState: State<Color> = animateColorAsState(
        targetValue = appTheme.ambientEnd,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "liquid_bg_end"
    )
    val animOrbAState: State<Color> = animateColorAsState(
        targetValue = appTheme.ambientHighlight,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "liquid_orb_a"
    )
    val animTertiaryState: State<Color> = animateColorAsState(
        targetValue = appTheme.ambientTertiary,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "liquid_orb_c"
    )

    val driftTransition = rememberInfiniteTransition(label = "liquid_ambient_motion")
    val phaseState: State<Float> = driftTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "liquid_continuous_phase"
    )

    val cachedShader = remember(appTheme.effectStyle) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AgslShaderLibrary.createShader(appTheme.effectStyle)
        } else {
            null
        }
    }

    Box(modifier = modifier) {
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val bgStart = animBgStartState.value
                    val bgEnd = animBgEndState.value
                    val orbA = animOrbAState.value
                    val tertiary = animTertiaryState.value

                    val phase = phaseState.value
                    val shaderBrush = if (cachedShader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        AgslShaderLibrary.updateBrush(
                            shader = cachedShader,
                            width = size.width,
                            height = size.height,
                            timeSeconds = phase * 16f,
                            startArgb = bgStart.toArgb(),
                            endArgb = bgEnd.toArgb(),
                            accentArgb = orbA.toArgb(),
                            tertiaryArgb = tertiary.toArgb()
                        )
                    } else {
                        null
                    }

                    if (shaderBrush != null) {
                        drawRect(brush = shaderBrush)
                    } else {
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(bgStart, orbA.copy(alpha = 0.22f), bgEnd),
                                start = Offset.Zero,
                                end = Offset(size.width, size.height)
                            )
                        )
                    }

                    drawDynamicAtmosphericParticles(
                        effectStyle = appTheme.effectStyle,
                        phase = phase,
                        isDark = appTheme.isDark,
                        accent = orbA,
                        tertiary = tertiary
                    )
                }
        )

        content()
    }
}

/**
 * Compact preview canvas for Theme Studio rich cards & hero banner.
 * - Grid swatch cards pass animated = false -> zero infinite animations or shaders during scroll.
 * - Hero banner passes animated = true -> cached RuntimeShader + draw-phase-only reads.
 */
@Composable
fun ThemeEffectPreviewCanvas(
    theme: AppTheme,
    animated: Boolean = true,
    modifier: Modifier = Modifier
) {
    val phaseState: State<Float>? = if (animated && theme.isGlass) {
        val transition = rememberInfiniteTransition(label = "preview_fx_${theme.id}")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 8000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "preview_phase_${theme.id}"
        )
    } else {
        null
    }

    val cachedShader = remember(animated, theme.isGlass, theme.effectStyle) {
        if (animated && theme.isGlass && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AgslShaderLibrary.createShader(theme.effectStyle)
        } else {
            null
        }
    }

    val startArgb = remember(theme.ambientStart) { theme.ambientStart.toArgb() }
    val endArgb = remember(theme.ambientEnd) { theme.ambientEnd.toArgb() }
    val accentArgb = remember(theme.ambientHighlight) { theme.ambientHighlight.toArgb() }
    val tertiaryArgb = remember(theme.ambientTertiary) { theme.ambientTertiary.toArgb() }

    Spacer(
        modifier = modifier.drawBehind {
            if (theme.isGlass) {
                val phase = phaseState?.value ?: 0.25f
                val shaderBrush = if (cachedShader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    AgslShaderLibrary.updateBrush(
                        shader = cachedShader,
                        width = size.width,
                        height = size.height,
                        timeSeconds = phase * 8f,
                        startArgb = startArgb,
                        endArgb = endArgb,
                        accentArgb = accentArgb,
                        tertiaryArgb = tertiaryArgb
                    )
                } else {
                    null
                }
                if (shaderBrush != null) {
                    drawRect(brush = shaderBrush)
                } else {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(theme.ambientStart, theme.ambientEnd),
                            start = Offset.Zero,
                            end = Offset(size.width, size.height)
                        )
                    )
                }
                val twoPi = (2.0 * PI).toFloat()
                val c1 = Offset(
                    size.width * (0.42f + 0.14f * cos(phase * twoPi)),
                    size.height * (0.42f + 0.10f * sin(phase * twoPi))
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(theme.ambientHighlight.copy(alpha = 0.36f), Color.Transparent),
                        center = c1,
                        radius = size.minDimension * 0.56f
                    ),
                    center = c1,
                    radius = size.minDimension * 0.56f
                )
                if (animated) {
                    drawDynamicAtmosphericParticles(
                        effectStyle = theme.effectStyle,
                        phase = phase,
                        isDark = theme.isDark,
                        accent = theme.ambientHighlight,
                        tertiary = theme.ambientTertiary
                    )
                }
            } else {
                drawRect(color = theme.background)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(theme.primary.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(size.width * 0.65f, size.height * 0.38f),
                        radius = size.minDimension * 0.55f
                    ),
                    center = Offset(size.width * 0.65f, size.height * 0.38f),
                    radius = size.minDimension * 0.55f
                )
            }
        }
    )
}

/**
 * Zero-delay container wrapper so LazyColumn / scroll views never hitch or lag on tab switch or fling.
 */
@Composable
fun StaggeredReveal(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
    }
}
