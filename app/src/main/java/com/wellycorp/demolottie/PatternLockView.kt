package com.wellycorp.demolottie

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.os.Debug
import android.os.SystemClock
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import androidx.annotation.Dimension
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import com.wellycorp.demolottie.R
import java.util.ArrayList
import kotlin.Int
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class PatternLockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var dotStates: Array<Array<DotState>>
    private var patternSize: Int
    private var drawingProfilingStarted = false
    private var animatingPeriodStart: Long = 0
    private var hitFactor = 0.4f
    private var aspectRatioEnabled: Boolean
    @AspectRatio
    private var aspectRatio: Int
    private var normalStateColor: Int
    private var wrongStateColor: Int
    private var correctStateColor: Int
    private var pathWidth: Int
    private var dotNormalSize: Int
    private var dotSelectedSize: Int
    private var dotAnimationDuration: Int
    private var pathEndAnimationDuration: Int

    private val dotPaint = Paint()
    private val pathPaint = Paint()

    private val patternListeners: MutableList<PatternLockViewListener> = ArrayList()
    private var pattern: ArrayList<Dot>

    private var patternDrawLookup: Array<BooleanArray>

    private var inProgressX = -1f
    private var inProgressY = -1f

    @PatternViewMode
    private var patternViewMode: Int = PatternViewMode.AUTO_DRAW
    private var inputEnabled = true
    private var inStealthMode = false
    private var enableHapticFeedback = true
    private var patternInProgress = false

    private var viewWidth = 0f
    private var viewHeight = 0f

    private val currentPath = Path()
    private val invalidate = Rect()
    private val tempInvalidateRect = Rect()

    private var fastOutSlowInInterpolator: Interpolator = LinearInterpolator()
    private var linearOutSlowInInterpolator: Interpolator = LinearInterpolator()

    private var dotCount: Int = 0
    private val dots: Array<Array<Dot>> by lazy {
        Array(dotCount) { row ->
            Array(dotCount) { col ->
                Dot(row, col)
            }
        }
    }


    init {
        val typedArray: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.PatternLockView)
        try {
            dotCount = typedArray.getInt(
                R.styleable.PatternLockView_dotCount,
                DEFAULT_PATTERN_DOT_COUNT
            )
            aspectRatioEnabled = typedArray.getBoolean(
                R.styleable.PatternLockView_aspectRatioEnabled,
                false
            )
            aspectRatio = typedArray.getInt(
                R.styleable.PatternLockView_aspectRatio,
                AspectRatio.ASPECT_RATIO_SQUARE
            )
            pathWidth = typedArray.getDimension(
                R.styleable.PatternLockView_pathWidth,
                context.resources.getDimension(R.dimen.pattern_lock_path_width)
            ).toInt()
            normalStateColor = typedArray.getColor(
                R.styleable.PatternLockView_normalStateColor,
                ContextCompat.getColor(context, R.color.white)
            )
            correctStateColor = typedArray.getColor(
                R.styleable.PatternLockView_correctStateColor,
                ContextCompat.getColor(context, R.color.white)
            )
            wrongStateColor = typedArray.getColor(
                R.styleable.PatternLockView_wrongStateColor,
                ContextCompat.getColor(context, R.color.pomegranate)
            )
            dotNormalSize = typedArray.getDimension(
                R.styleable.PatternLockView_dotNormalSize,
                context.resources.getDimension(R.dimen.pattern_lock_dot_size)
            ).toInt()
            dotSelectedSize = typedArray.getDimension(
                R.styleable.PatternLockView_dotSelectedSize,
                context.resources.getDimension(R.dimen.pattern_lock_dot_selected_size)
            ).toInt()
            dotAnimationDuration = typedArray.getInt(
                R.styleable.PatternLockView_dotAnimationDuration,
                DEFAULT_DOT_ANIMATION_DURATION
            )
            pathEndAnimationDuration = typedArray.getInt(
                R.styleable.PatternLockView_pathEndAnimationDuration,
                DEFAULT_PATH_END_ANIMATION_DURATION
            )
        } finally {
            typedArray.recycle()
        }

        patternSize = dotCount * dotCount
        pattern = ArrayList(patternSize)
        patternDrawLookup = Array(dotCount) { BooleanArray(dotCount) }

        dotStates = Array(dotCount) { row ->
            Array(dotCount) { col ->
                DotState().apply {
                    size = dotNormalSize.toFloat()
                }
            }
        }

        initView()
    }

    private fun initView() {
        isClickable = true

        pathPaint.apply {
            isAntiAlias = true
            isDither = true
            color = normalStateColor
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = pathWidth.toFloat()
        }

        dotPaint.apply {
            isAntiAlias = true
            isDither = true
        }

        if (!isInEditMode) {
            fastOutSlowInInterpolator = AnimationUtils.loadInterpolator(
                context, android.R.interpolator.fast_out_slow_in
            )
            linearOutSlowInInterpolator = AnimationUtils.loadInterpolator(
                context, android.R.interpolator.linear_out_slow_in
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (!aspectRatioEnabled) return

        val oldWidth = resolveMeasured(widthMeasureSpec, suggestedMinimumWidth)
        val oldHeight = resolveMeasured(heightMeasureSpec, suggestedMinimumHeight)

        val (newWidth, newHeight) = when (aspectRatio) {
            AspectRatio.ASPECT_RATIO_SQUARE -> {
                val size = minOf(oldWidth, oldHeight)
                size to size
            }

            AspectRatio.ASPECT_RATIO_WIDTH_BIAS -> {
                oldWidth to minOf(oldWidth, oldHeight)
            }

            AspectRatio.ASPECT_RATIO_HEIGHT_BIAS -> {
                minOf(oldWidth, oldHeight) to oldHeight
            }

            else -> throw IllegalStateException("Unknown aspect ratio")
        }

        setMeasuredDimension(newWidth, newHeight)
    }

    override fun onDraw(canvas: Canvas) {
        val pattern = this.pattern
        val patternSize = pattern.size
        val drawLookupTable = this.patternDrawLookup

        if (this.patternViewMode == PatternViewMode.AUTO_DRAW) {
            val oneCycle = (patternSize + 1) * MILLIS_PER_CIRCLE_ANIMATING
            val spotInCycle =
                ((SystemClock.elapsedRealtime() - this.animatingPeriodStart) % oneCycle).toInt()
            val numCircles = spotInCycle / MILLIS_PER_CIRCLE_ANIMATING
            clearPatternDrawLookup()
            for (i in 0 until numCircles) {
                val dot = pattern[i]
                drawLookupTable[dot.row][dot.column] = true
            }

            val needToUpdateInProgressPoint = numCircles > 0 && numCircles < patternSize

            if (needToUpdateInProgressPoint) {
                val percentageOfNextCircle =
                    (spotInCycle % MILLIS_PER_CIRCLE_ANIMATING).toFloat() /
                            MILLIS_PER_CIRCLE_ANIMATING.toFloat()

                val currentDot = pattern[numCircles - 1]
                val centerX = getCenterXForColumn(currentDot.column)
                val centerY = getCenterYForRow(currentDot.row)

                val nextDot = pattern[numCircles]
                val dx = percentageOfNextCircle *
                        (getCenterXForColumn(nextDot.column) - centerX)
                val dy = percentageOfNextCircle *
                        (getCenterYForRow(nextDot.row) - centerY)
                this.inProgressX = centerX + dx
                this.inProgressY = centerY + dy
            }
            invalidate()
        }

        val currentPath = this.currentPath
        currentPath.rewind()

        // Draw dots
        for (i in 0 until this.dotCount) {
            val centerY = getCenterYForRow(i)
            for (j in 0 until this.dotCount) {
                val dotState = this.dotStates[i][j]
                val centerX = getCenterXForColumn(j)
                val size = dotState.size * dotState.scale
                val translationY = dotState.translateY
                drawCircle(
                    canvas,
                    centerX.toInt().toFloat(),
                    centerY + translationY,
                    size,
                    drawLookupTable[i][j],
                    dotState.alpha
                )
            }
        }

        val drawPath = !this.inStealthMode
        if (drawPath) {
            this.pathPaint.color = getCurrentColor(true)

            var anyCircles = false
            var lastX = 0f
            var lastY = 0f

            for (i in 0 until patternSize) {
                val dot = pattern[i]

                if (!drawLookupTable[dot.row][dot.column]) break
                anyCircles = true

                val centerX = getCenterXForColumn(dot.column)
                val centerY = getCenterYForRow(dot.row)
                if (i != 0) {
                    val state = this.dotStates[dot.row][dot.column]
                    currentPath.rewind()
                    currentPath.moveTo(lastX, lastY)
                    if (state.lineEndX != Float.MIN_VALUE &&
                        state.lineEndY != Float.MIN_VALUE
                    ) {
                        currentPath.lineTo(state.lineEndX, state.lineEndY)
                    } else {
                        currentPath.lineTo(centerX, centerY)
                    }
                    canvas.drawPath(currentPath, this.pathPaint)
                }
                lastX = centerX
                lastY = centerY
            }

            if ((this.patternInProgress || this.patternViewMode == PatternViewMode.AUTO_DRAW) && anyCircles) {
                currentPath.rewind()
                currentPath.moveTo(lastX, lastY)
                currentPath.lineTo(this.inProgressX, this.inProgressY)

                this.pathPaint.alpha = (calculateLastSegmentAlpha(
                    this.inProgressX,
                    this.inProgressY,
                    lastX,
                    lastY
                ) * 255f).toInt()
                canvas.drawPath(currentPath, this.pathPaint)
            }
        }
    }

    fun getDotCount(): Int = this.dotCount

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        val adjustedWidth = width - paddingLeft - paddingRight
        this.viewWidth = adjustedWidth / this.dotCount.toFloat()

        val adjustedHeight = height - paddingTop - paddingBottom
        this.viewHeight = adjustedHeight / this.dotCount.toFloat()
    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        if (am.isTouchExplorationEnabled) {
            val action = event.action
            when (action) {
                MotionEvent.ACTION_HOVER_ENTER -> event.action = MotionEvent.ACTION_DOWN
                MotionEvent.ACTION_HOVER_MOVE -> event.action = MotionEvent.ACTION_MOVE
                MotionEvent.ACTION_HOVER_EXIT -> event.action = MotionEvent.ACTION_UP
            }
            onTouchEvent(event)
            event.action = action
        }
        return super.onHoverEvent(event)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!this.inputEnabled || !isEnabled) return false

        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                handleActionDown(event)
                true
            }

            MotionEvent.ACTION_UP -> {
                handleActionUp(event)
                true
            }

            MotionEvent.ACTION_MOVE -> {
                handleActionMove(event)
                true
            }

            MotionEvent.ACTION_CANCEL -> {
                this.patternInProgress = false
                resetPattern()
                notifyPatternCleared()

                if (PROFILE_DRAWING && this.drawingProfilingStarted) {
                    Debug.stopMethodTracing()
                    this.drawingProfilingStarted = false
                }
                true
            }

            else -> false
        }
    }

    fun setPattern(@PatternViewMode patternViewMode: Int, pattern: List<Dot>) {
        this.pattern.clear()
        this.pattern.addAll(pattern)
        clearPatternDrawLookup()
        for (dot in pattern) {
            this.patternDrawLookup[dot.row][dot.column] = true
        }
        setViewMode(patternViewMode)
    }

    fun setViewMode(@PatternViewMode patternViewMode: Int) {
        this@PatternLockView.patternViewMode = patternViewMode
        if (patternViewMode == PatternViewMode.AUTO_DRAW) {
            if (this.pattern.isNotEmpty()) {
                this.animatingPeriodStart = SystemClock.elapsedRealtime()
                val first = pattern[0]
                this.inProgressX = getCenterXForColumn(first.column)
                this.inProgressY = getCenterYForRow(first.row)
                clearPatternDrawLookup()
            }
            invalidate()
        }
    }

    fun setDotCount(dotCount: Int) {
        this@PatternLockView.dotCount = dotCount
        this.patternSize = this@PatternLockView.dotCount * this@PatternLockView.dotCount
        this.pattern = ArrayList(patternSize)
        this.patternDrawLookup = Array(this@PatternLockView.dotCount) { BooleanArray(this@PatternLockView.dotCount) }

        this.dotStates = Array(this@PatternLockView.dotCount) {
            Array(this@PatternLockView.dotCount) {
                DotState().apply {
                    size = this@PatternLockView.dotNormalSize.toFloat()
                }
            }
        }

        requestLayout()
        invalidate()
    }

    fun setAspectRatioEnabled(aspectRatioEnabled: Boolean) {
        this@PatternLockView.aspectRatioEnabled = aspectRatioEnabled
        requestLayout()
    }

    fun setAspectRatio(@AspectRatio aspectRatio: Int) {
        this@PatternLockView.aspectRatio = aspectRatio
        requestLayout()
    }

    fun setPathWidth(@Dimension pathWidth: Int) {
        this@PatternLockView.pathWidth = pathWidth
        initView()
        invalidate()
    }

    fun setDotNormalSize(@Dimension dotNormalSize: Int) {
        this@PatternLockView.dotNormalSize = dotNormalSize

        for (i in 0 until this@PatternLockView.dotCount) {
            for (j in 0 until this@PatternLockView.dotCount) {
                this@PatternLockView.dotStates[i][j] = DotState().apply {
                    size = dotNormalSize.toFloat()
                }
            }
        }

        invalidate()
    }

    fun setDotAnimationDuration(dotAnimationDuration: Int) {
        this@PatternLockView.dotAnimationDuration = dotAnimationDuration
        invalidate()
    }

    fun clearPattern() {
        resetPattern()
    }

    private fun resolveMeasured(measureSpec: Int, desired: Int): Int {
        val specSize = MeasureSpec.getSize(measureSpec)
        return when (MeasureSpec.getMode(measureSpec)) {
            MeasureSpec.UNSPECIFIED -> desired
            MeasureSpec.AT_MOST -> maxOf(specSize, desired)
            MeasureSpec.EXACTLY -> specSize
            else -> specSize
        }
    }

    private fun notifyPatternProgress() {
        sendAccessEvent(R.string.message_pattern_dot_added)
        notifyListenersProgress(this@PatternLockView.pattern)
    }

    private fun notifyPatternStarted() {
        sendAccessEvent(R.string.message_pattern_started)
        notifyListenersStarted()
    }

    private fun notifyPatternDetected() {
        sendAccessEvent(R.string.message_pattern_detected)
        notifyListenersComplete(this@PatternLockView.pattern)
    }

    private fun notifyPatternCleared() {
        sendAccessEvent(R.string.message_pattern_cleared)
        notifyListenersCleared()
    }

    private fun resetPattern() {
        this@PatternLockView.pattern.clear()
        clearPatternDrawLookup()
        this@PatternLockView.patternViewMode = PatternViewMode.CORRECT
        invalidate()
    }

    private fun notifyListenersStarted() {
        this@PatternLockView.patternListeners.forEach { it.onStarted() }
    }

    private fun notifyListenersProgress(pattern: List<Dot>) {
        this@PatternLockView.patternListeners.forEach { it.onProgress(pattern) }
    }

    private fun notifyListenersComplete(pattern: List<Dot>) {
        this@PatternLockView.patternListeners.forEach { it.onComplete(pattern) }
    }

    private fun notifyListenersCleared() {
        this@PatternLockView.patternListeners.forEach { it.onCleared() }
    }

    private fun clearPatternDrawLookup() {
        for (i in 0 until this@PatternLockView.dotCount) {
            for (j in 0 until this@PatternLockView.dotCount) {
                this@PatternLockView.patternDrawLookup[i][j] = false
            }
        }
    }

    private fun detectAndAddHit(x: Float, y: Float): Dot? {
        val dot = checkForNewHit(x, y) ?: return null

        var fillInGapDot: Dot? = null
        if (this@PatternLockView.pattern.isNotEmpty()) {
            val lastDot = this@PatternLockView.pattern[pattern.size - 1]
            val dRow = dot.row - lastDot.row
            val dColumn = dot.column - lastDot.column

            var fillInRow = lastDot.row
            var fillInColumn = lastDot.column

            if (abs(dRow) == 2 && abs(dColumn) != 1) {
                fillInRow = lastDot.row + if (dRow > 0) 1 else -1
            }

            if (abs(dColumn) == 2 && abs(dRow) != 1) {
                fillInColumn = lastDot.column + if (dColumn > 0) 1 else -1
            }

            fillInGapDot = dotOf(fillInRow, fillInColumn)
        }

        if (fillInGapDot != null &&
            !this@PatternLockView.patternDrawLookup[fillInGapDot.row][fillInGapDot.column]
        ) {
            addCellToPattern(fillInGapDot)
        }

        addCellToPattern(dot)

        if (this@PatternLockView.enableHapticFeedback) {
            performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING or
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }
        return dot
    }

    private fun addCellToPattern(newDot: Dot) {
        this@PatternLockView.patternDrawLookup[newDot.row][newDot.column] = true
        this@PatternLockView.pattern.add(newDot)
        if (!this@PatternLockView.inStealthMode) {
            startDotSelectedAnimation(newDot)
        }
        notifyPatternProgress()
    }

    private fun startDotSelectedAnimation(dot: Dot) {
        val dotState = this@PatternLockView.dotStates[dot.row][dot.column]
        startSizeAnimation(
            this@PatternLockView.dotNormalSize.toFloat(),
            this@PatternLockView.dotSelectedSize.toFloat(),
            this@PatternLockView.dotAnimationDuration.toLong(),
            this@PatternLockView.linearOutSlowInInterpolator,
            dotState
        ) {
            startSizeAnimation(
                this@PatternLockView.dotSelectedSize.toFloat(),
                this@PatternLockView.dotNormalSize.toFloat(),
                this@PatternLockView.dotAnimationDuration.toLong(),
                this@PatternLockView.fastOutSlowInInterpolator,
                dotState,
                null
            )
        }
        startLineEndAnimation(
            dotState,
            this@PatternLockView.inProgressX,
            this@PatternLockView.inProgressY,
            getCenterXForColumn(dot.column),
            getCenterYForRow(dot.row)
        )
    }

    private fun startLineEndAnimation(
        state: DotState,
        startX: Float,
        startY: Float,
        targetX: Float,
        targetY: Float
    ) {
        val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
        valueAnimator.addUpdateListener { animation ->
            val t = animation.animatedValue as Float
            state.lineEndX = (1 - t) * startX + t * targetX
            state.lineEndY = (1 - t) * startY + t * targetY
            invalidate()
        }
        valueAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                state.lineAnimator = null
            }
        })
        valueAnimator.interpolator = this@PatternLockView.fastOutSlowInInterpolator
        valueAnimator.duration = this@PatternLockView.pathEndAnimationDuration.toLong()
        valueAnimator.start()
        state.lineAnimator = valueAnimator
    }

    private fun startSizeAnimation(
        start: Float,
        end: Float,
        duration: Long,
        interpolator: Interpolator,
        state: DotState,
        endRunnable: (() -> Unit)?
    ) {
        val valueAnimator = ValueAnimator.ofFloat(start, end)
        valueAnimator.addUpdateListener { animation ->
            state.size = animation.animatedValue as Float
            invalidate()
        }
        if (endRunnable != null) {
            valueAnimator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    endRunnable()
                }
            })
        }
        valueAnimator.interpolator =
            interpolator
        valueAnimator.duration = duration
        valueAnimator.start()
    }

    private fun checkForNewHit(x: Float, y: Float): Dot? {
        val rowHit = getRowHit(y)
        if (rowHit < 0) return null

        val columnHit = getColumnHit(x)
        if (columnHit < 0) return null

        if (this@PatternLockView.patternDrawLookup[rowHit][columnHit]) return null

        return dotOf(rowHit, columnHit)
    }

    private fun getRowHit(y: Float): Int {
        val squareHeight = this@PatternLockView.viewHeight
        val hitSize = squareHeight * this@PatternLockView.hitFactor

        val offset = paddingTop + (squareHeight - hitSize) / 2f
        for (i in 0 until this@PatternLockView.dotCount) {
            val hitTop = offset + squareHeight * i
            if (y >= hitTop && y <= hitTop + hitSize) {
                return i
            }
        }
        return -1
    }

    private fun getColumnHit(x: Float): Int {
        val squareWidth = this@PatternLockView.viewWidth
        val hitSize = squareWidth * this@PatternLockView.hitFactor

        val offset = paddingLeft + (squareWidth - hitSize) / 2f
        for (i in 0 until this@PatternLockView.dotCount) {
            val hitLeft = offset + squareWidth * i
            if (x >= hitLeft && x <= hitLeft + hitSize) {
                return i
            }
        }
        return -1
    }

    private fun handleActionMove(event: MotionEvent) {
        val radius = this@PatternLockView.pathWidth.toFloat()
        val historySize = event.historySize
        this@PatternLockView.tempInvalidateRect.setEmpty()
        var invalidateNow = false

        for (i in 0..historySize) {
            val x = if (i < historySize) event.getHistoricalX(i) else event.x
            val y = if (i < historySize) event.getHistoricalY(i) else event.y

            val hitDot = detectAndAddHit(x, y)
            val patternSize = pattern.size
            if (hitDot != null && patternSize == 1) {
                this@PatternLockView.patternInProgress = true
                notifyPatternStarted()
            }

            val dx = abs(x - this@PatternLockView.inProgressX)
            val dy = abs(y - this@PatternLockView.inProgressY)
            if (dx > DEFAULT_DRAG_THRESHOLD || dy > DEFAULT_DRAG_THRESHOLD) {
                invalidateNow = true
            }

            if (this@PatternLockView.patternInProgress && patternSize > 0) {
                val pattern = this@PatternLockView.pattern
                val lastDot = pattern[patternSize - 1]
                val lastCellCenterX = getCenterXForColumn(lastDot.column)
                val lastCellCenterY = getCenterYForRow(lastDot.row)

                var left = min(lastCellCenterX, x) - radius
                var right = max(lastCellCenterX, x) + radius
                var top = min(lastCellCenterY, y) - radius
                var bottom = max(lastCellCenterY, y) + radius

                if (hitDot != null) {
                    val width = this@PatternLockView.viewWidth * 0.5f
                    val height = this@PatternLockView.viewHeight * 0.5f
                    val hitCellCenterX = getCenterXForColumn(hitDot.column)
                    val hitCellCenterY = getCenterYForRow(hitDot.row)

                    left = min(hitCellCenterX - width, left)
                    right = max(hitCellCenterX + width, right)
                    top = min(hitCellCenterY - height, top)
                    bottom = max(hitCellCenterY + height, bottom)
                }

                this@PatternLockView.tempInvalidateRect.union(
                    left.toInt(),
                    top.toInt(),
                    right.toInt(),
                    bottom.toInt()
                )
            }
        }

        this@PatternLockView.inProgressX = event.x
        this@PatternLockView.inProgressY = event.y

        if (invalidateNow) {
            this@PatternLockView.invalidate.union(this@PatternLockView.tempInvalidateRect)
            invalidate(this@PatternLockView.invalidate)
            this@PatternLockView.invalidate.set(this@PatternLockView.tempInvalidateRect)
        }
    }

    private fun sendAccessEvent(resId: Int) {
        announceForAccessibility(context.getString(resId))
    }

    private fun handleActionUp(event: MotionEvent) {
        if (this@PatternLockView.pattern.isNotEmpty()) {
            this@PatternLockView.patternInProgress = false
            cancelLineAnimations()
            notifyPatternDetected()
            invalidate()
        }
        if (PROFILE_DRAWING && this@PatternLockView.drawingProfilingStarted) {
            Debug.stopMethodTracing()
            this@PatternLockView.drawingProfilingStarted = false
        }
    }

    private fun cancelLineAnimations() {
        for (i in 0 until this@PatternLockView.dotCount) {
            for (j in 0 until this@PatternLockView.dotCount) {
                val state = this@PatternLockView.dotStates[i][j]
                state.lineAnimator?.let {
                    it.cancel()
                    state.lineEndX = Float.MIN_VALUE
                    state.lineEndY = Float.MIN_VALUE
                }
            }
        }
    }

    private fun handleActionDown(event: MotionEvent) {
        resetPattern()
        val x = event.x
        val y = event.y
        val hitDot = detectAndAddHit(x, y)
        if (hitDot != null) {
            this@PatternLockView.patternInProgress = true
            this@PatternLockView.patternViewMode = PatternViewMode.CORRECT
            notifyPatternStarted()
        } else {
            this@PatternLockView.patternInProgress = false
            notifyPatternCleared()
        }
        if (hitDot != null) {
            val startX = getCenterXForColumn(hitDot.column)
            val startY = getCenterYForRow(hitDot.row)

            val widthOffset = this@PatternLockView.viewWidth / 2f
            val heightOffset = this@PatternLockView.viewHeight / 2f

            invalidate(
                (startX - widthOffset).toInt(),
                (startY - heightOffset).toInt(),
                (startX + widthOffset).toInt(),
                (startY + heightOffset).toInt()
            )
        }
        this@PatternLockView.inProgressX = x
        this@PatternLockView.inProgressY = y
        if (PROFILE_DRAWING && !this@PatternLockView.drawingProfilingStarted) {
            Debug.startMethodTracing("PatternLockDrawing")
            this@PatternLockView.drawingProfilingStarted = true
        }
    }

    private fun getCenterXForColumn(column: Int): Float =
        paddingLeft + column * this@PatternLockView.viewWidth + this@PatternLockView.viewWidth / 2f

    private fun getCenterYForRow(row: Int): Float =
        paddingTop + row * this@PatternLockView.viewHeight + this@PatternLockView.viewHeight / 2f

    private fun calculateLastSegmentAlpha(
        x: Float,
        y: Float,
        lastX: Float,
        lastY: Float
    ): Float {
        val diffX = x - lastX
        val diffY = y - lastY
        val dist = sqrt(diffX * diffX + diffY * diffY)
        val fraction = dist / this@PatternLockView.viewWidth
        return min(1f, max(0f, (fraction - 0.3f) * 4f))
    }

    private fun getCurrentColor(partOfPattern: Boolean): Int {
        return if (!partOfPattern || this@PatternLockView.inStealthMode || this@PatternLockView.patternInProgress) {
            this@PatternLockView.normalStateColor
        } else if (this@PatternLockView.patternViewMode == PatternViewMode.WRONG) {
            this@PatternLockView.wrongStateColor
        } else if (this@PatternLockView.patternViewMode == PatternViewMode.CORRECT ||
            this@PatternLockView.patternViewMode == PatternViewMode.AUTO_DRAW
        ) {
            this@PatternLockView.correctStateColor
        } else {
            throw IllegalStateException("Unknown view mode $patternViewMode")
        }
    }

    private fun drawCircle(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        partOfPattern: Boolean,
        alpha: Float
    ) {
        this@PatternLockView.dotPaint.color = getCurrentColor(partOfPattern)
        this@PatternLockView.dotPaint.alpha = (alpha * 255).toInt()
        canvas.drawCircle(centerX, centerY, size / 2f, dotPaint)
    }

    private fun Dot.id(): Int{
        return row * this@PatternLockView.dotCount + column
    }

    private fun dotOf(row: Int, column: Int): Dot {
        checkRange(row, column)
        return this@PatternLockView.dots[row][column]
    }

    private fun dotOf(id: Int): Dot {
        val count = this@PatternLockView.dotCount
        return dotOf(id / count, id % count)
    }

    private fun checkRange(row: Int, column: Int) {
        val max = this@PatternLockView.dotCount - 1
        require(row in 0..max) {
            "mRow must be in range 0-$max"
        }
        require(column in 0..max) {
            "mColumn must be in range 0-$max"
        }
    }

    data class Dot(
        val row: Int,
        val column: Int
    )

    data class DotState(
        val scale: Float = 1.0f,
        val translateY: Float = 0.0f,
        val alpha: Float = 1.0f,
        var size: Float = 0f,
        var lineEndX: Float = Float.MIN_VALUE,
        var lineEndY: Float = Float.MIN_VALUE,
        var lineAnimator: ValueAnimator? = null
    )

    interface PatternLockViewListener {
        /**
         * Fired when the pattern drawing has just started.
         */
        fun onStarted()

        /**
         * Fired when the pattern is progressing (user connected a new dot).
         */
        fun onProgress(progressPattern: List<Dot>)

        /**
         * Fired when the user has completed drawing the pattern.
         */
        fun onComplete(pattern: List<Dot>)

        /**
         * Fired when the pattern has been cleared.
         */
        fun onCleared()
    }

    @IntDef(
        AspectRatio.ASPECT_RATIO_SQUARE,
        AspectRatio.ASPECT_RATIO_WIDTH_BIAS,
        AspectRatio.ASPECT_RATIO_HEIGHT_BIAS
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class AspectRatio {
        companion object {
            const val ASPECT_RATIO_SQUARE = 0
            const val ASPECT_RATIO_WIDTH_BIAS = 1
            const val ASPECT_RATIO_HEIGHT_BIAS = 2
        }
    }

    @IntDef(PatternViewMode.CORRECT, PatternViewMode.AUTO_DRAW, PatternViewMode.WRONG)
    @Retention(AnnotationRetention.SOURCE)
    annotation class PatternViewMode {
        companion object {
            const val CORRECT = 0
            const val AUTO_DRAW = 1
            const val WRONG = 2
        }
    }

    companion object {
        private const val DEFAULT_PATTERN_DOT_COUNT = 3
        private const val PROFILE_DRAWING = false
        private const val MILLIS_PER_CIRCLE_ANIMATING = 700
        private const val DEFAULT_DOT_ANIMATION_DURATION = 190
        private const val DEFAULT_PATH_END_ANIMATION_DURATION = 100
        private const val DEFAULT_DRAG_THRESHOLD = 0.0f
    }

}
