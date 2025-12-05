package com.wellycorp.demolottie.patternlock

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

    private var mDotStates: Array<Array<DotState>>
    private var mPatternSize: Int
    private var mDrawingProfilingStarted = false
    private var mAnimatingPeriodStart: Long = 0
    private var mHitFactor = 0.6f

    private var mAspectRatioEnabled: Boolean
    @AspectRatio
    private var mAspectRatio: Int
    private var mNormalStateColor: Int
    private var mWrongStateColor: Int
    private var mCorrectStateColor: Int
    private var mPathWidth: Int
    private var mDotNormalSize: Int
    private var mDotSelectedSize: Int
    private var mDotAnimationDuration: Int
    private var mPathEndAnimationDuration: Int

    private val mDotPaint = Paint()
    private val mPathPaint = Paint()

    private val mPatternListeners: MutableList<PatternLockViewListener> = ArrayList()
    private var mPattern: ArrayList<Dot>

    private var mPatternDrawLookup: Array<BooleanArray>

    private var mInProgressX = -1f
    private var mInProgressY = -1f

    @PatternViewMode
    private var mPatternViewMode: Int = PatternViewMode.AUTO_DRAW
    private var mInputEnabled = true
    private var mInStealthMode = false
    private var mEnableHapticFeedback = true
    private var mPatternInProgress = false

    private var mViewWidth = 0f
    private var mViewHeight = 0f

    private val mCurrentPath = Path()
    private val mInvalidate = Rect()
    private val mTempInvalidateRect = Rect()

    private var mFastOutSlowInInterpolator: Interpolator = LinearInterpolator()
    private var mLinearOutSlowInInterpolator: Interpolator = LinearInterpolator()

    private var sDotCount: Int = 0
    private val sDots: Array<Array<Dot>> by lazy {
        Array(sDotCount) { row ->
            Array(sDotCount) { col ->
                Dot(row, col)
            }
        }
    }


    init {
        val typedArray: TypedArray =
            context.obtainStyledAttributes(attrs, R.styleable.PatternLockView)
        try {
            sDotCount = typedArray.getInt(
                R.styleable.PatternLockView_dotCount,
                DEFAULT_PATTERN_DOT_COUNT
            )
            mAspectRatioEnabled = typedArray.getBoolean(
                R.styleable.PatternLockView_aspectRatioEnabled,
                false
            )
            mAspectRatio = typedArray.getInt(
                R.styleable.PatternLockView_aspectRatio,
                AspectRatio.ASPECT_RATIO_SQUARE
            )
            mPathWidth = typedArray.getDimension(
                R.styleable.PatternLockView_pathWidth,
                context.resources.getDimension(R.dimen.pattern_lock_path_width)
            ).toInt()
            mNormalStateColor = typedArray.getColor(
                R.styleable.PatternLockView_normalStateColor,
                ContextCompat.getColor(context, R.color.white)
            )
            mCorrectStateColor = typedArray.getColor(
                R.styleable.PatternLockView_correctStateColor,
                ContextCompat.getColor(context, R.color.white)
            )
            mWrongStateColor = typedArray.getColor(
                R.styleable.PatternLockView_wrongStateColor,
                ContextCompat.getColor(context, R.color.pomegranate)
            )
            mDotNormalSize = typedArray.getDimension(
                R.styleable.PatternLockView_dotNormalSize,
                context.resources.getDimension(R.dimen.pattern_lock_dot_size)
            ).toInt()
            mDotSelectedSize = typedArray.getDimension(
                R.styleable.PatternLockView_dotSelectedSize,
                context.resources.getDimension(R.dimen.pattern_lock_dot_selected_size)
            ).toInt()
            mDotAnimationDuration = typedArray.getInt(
                R.styleable.PatternLockView_dotAnimationDuration,
                DEFAULT_DOT_ANIMATION_DURATION
            )
            mPathEndAnimationDuration = typedArray.getInt(
                R.styleable.PatternLockView_pathEndAnimationDuration,
                DEFAULT_PATH_END_ANIMATION_DURATION
            )
        } finally {
            typedArray.recycle()
        }

        mPatternSize = sDotCount * sDotCount
        mPattern = ArrayList(mPatternSize)
        mPatternDrawLookup = Array(sDotCount) { BooleanArray(sDotCount) }

        mDotStates = Array(sDotCount) { row ->
            Array(sDotCount) { col ->
                DotState().apply {
                    mSize = mDotNormalSize.toFloat()
                }
            }
        }

        initView()
    }

    private fun initView() {
        isClickable = true

        mPathPaint.apply {
            isAntiAlias = true
            isDither = true
            color = mNormalStateColor
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = mPathWidth.toFloat()
        }

        mDotPaint.apply {
            isAntiAlias = true
            isDither = true
        }

        if (!isInEditMode) {
            mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(
                context, android.R.interpolator.fast_out_slow_in
            )
            mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(
                context, android.R.interpolator.linear_out_slow_in
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (!mAspectRatioEnabled) return

        val oldWidth = resolveMeasured(widthMeasureSpec, suggestedMinimumWidth)
        val oldHeight = resolveMeasured(heightMeasureSpec, suggestedMinimumHeight)

        val (newWidth, newHeight) = when (mAspectRatio) {
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
        val pattern = mPattern
        val patternSize = pattern.size
        val drawLookupTable = mPatternDrawLookup

        if (mPatternViewMode == PatternViewMode.AUTO_DRAW) {
            val oneCycle = (patternSize + 1) * MILLIS_PER_CIRCLE_ANIMATING
            val spotInCycle =
                ((SystemClock.elapsedRealtime() - mAnimatingPeriodStart) % oneCycle).toInt()
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
                mInProgressX = centerX + dx
                mInProgressY = centerY + dy
            }
            invalidate()
        }

        val currentPath = mCurrentPath
        currentPath.rewind()

        // Draw dots
        for (i in 0 until sDotCount) {
            val centerY = getCenterYForRow(i)
            for (j in 0 until sDotCount) {
                val dotState = mDotStates[i][j]
                val centerX = getCenterXForColumn(j)
                val size = dotState.mSize * dotState.mScale
                val translationY = dotState.mTranslateY
                drawCircle(
                    canvas,
                    centerX.toInt().toFloat(),
                    centerY + translationY,
                    size,
                    drawLookupTable[i][j],
                    dotState.mAlpha
                )
            }
        }

        val drawPath = !mInStealthMode
        if (drawPath) {
            mPathPaint.color = getCurrentColor(true)

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
                    val state = mDotStates[dot.row][dot.column]
                    currentPath.rewind()
                    currentPath.moveTo(lastX, lastY)
                    if (state.mLineEndX != Float.MIN_VALUE &&
                        state.mLineEndY != Float.MIN_VALUE
                    ) {
                        currentPath.lineTo(state.mLineEndX, state.mLineEndY)
                    } else {
                        currentPath.lineTo(centerX, centerY)
                    }
                    canvas.drawPath(currentPath, mPathPaint)
                }
                lastX = centerX
                lastY = centerY
            }

            if ((mPatternInProgress || mPatternViewMode == PatternViewMode.AUTO_DRAW) && anyCircles) {
                currentPath.rewind()
                currentPath.moveTo(lastX, lastY)
                currentPath.lineTo(mInProgressX, mInProgressY)

                mPathPaint.alpha = (calculateLastSegmentAlpha(
                    mInProgressX,
                    mInProgressY,
                    lastX,
                    lastY
                ) * 255f).toInt()
                canvas.drawPath(currentPath, mPathPaint)
            }
        }
    }

    fun getDotCount(): Int = sDotCount

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        val adjustedWidth = width - paddingLeft - paddingRight
        mViewWidth = adjustedWidth / sDotCount.toFloat()

        val adjustedHeight = height - paddingTop - paddingBottom
        mViewHeight = adjustedHeight / sDotCount.toFloat()
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
        if (!mInputEnabled || !isEnabled) return false

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
                mPatternInProgress = false
                resetPattern()
                notifyPatternCleared()

                if (PROFILE_DRAWING && mDrawingProfilingStarted) {
                    Debug.stopMethodTracing()
                    mDrawingProfilingStarted = false
                }
                true
            }

            else -> false
        }
    }

    fun setPattern(@PatternViewMode patternViewMode: Int, pattern: List<Dot>) {
        mPattern.clear()
        mPattern.addAll(pattern)
        clearPatternDrawLookup()
        for (dot in pattern) {
            mPatternDrawLookup[dot.row][dot.column] = true
        }
        setViewMode(patternViewMode)
    }

    fun setViewMode(@PatternViewMode patternViewMode: Int) {
        mPatternViewMode = patternViewMode
        if (patternViewMode == PatternViewMode.AUTO_DRAW) {
            if (mPattern.isNotEmpty()) {
                mAnimatingPeriodStart = SystemClock.elapsedRealtime()
                val first = mPattern[0]
                mInProgressX = getCenterXForColumn(first.column)
                mInProgressY = getCenterYForRow(first.row)
                clearPatternDrawLookup()
            }
            invalidate()
        }
    }

    fun setDotCount(dotCount: Int) {
        sDotCount = dotCount
        mPatternSize = sDotCount * sDotCount
        mPattern = ArrayList(mPatternSize)
        mPatternDrawLookup = Array(sDotCount) { BooleanArray(sDotCount) }

        mDotStates = Array(sDotCount) {
            Array(sDotCount) {
                DotState().apply {
                    mSize = mDotNormalSize.toFloat()
                }
            }
        }

        requestLayout()
        invalidate()
    }

    fun setAspectRatioEnabled(aspectRatioEnabled: Boolean) {
        mAspectRatioEnabled = aspectRatioEnabled
        requestLayout()
    }

    fun setAspectRatio(@AspectRatio aspectRatio: Int) {
        mAspectRatio = aspectRatio
        requestLayout()
    }

    fun setPathWidth(@Dimension pathWidth: Int) {
        mPathWidth = pathWidth
        initView()
        invalidate()
    }

    fun setDotNormalSize(@Dimension dotNormalSize: Int) {
        mDotNormalSize = dotNormalSize

        for (i in 0 until sDotCount) {
            for (j in 0 until sDotCount) {
                mDotStates[i][j] = DotState().apply {
                    mSize = mDotNormalSize.toFloat()
                }
            }
        }

        invalidate()
    }

    fun setDotAnimationDuration(dotAnimationDuration: Int) {
        mDotAnimationDuration = dotAnimationDuration
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
        notifyListenersProgress(mPattern)
    }

    private fun notifyPatternStarted() {
        sendAccessEvent(R.string.message_pattern_started)
        notifyListenersStarted()
    }

    private fun notifyPatternDetected() {
        sendAccessEvent(R.string.message_pattern_detected)
        notifyListenersComplete(mPattern)
    }

    private fun notifyPatternCleared() {
        sendAccessEvent(R.string.message_pattern_cleared)
        notifyListenersCleared()
    }

    private fun resetPattern() {
        mPattern.clear()
        clearPatternDrawLookup()
        mPatternViewMode = PatternViewMode.CORRECT
        invalidate()
    }

    private fun notifyListenersStarted() {
        mPatternListeners.forEach { it.onStarted() }
    }

    private fun notifyListenersProgress(pattern: List<Dot>) {
        mPatternListeners.forEach { it.onProgress(pattern) }
    }

    private fun notifyListenersComplete(pattern: List<Dot>) {
        mPatternListeners.forEach { it.onComplete(pattern) }
    }

    private fun notifyListenersCleared() {
        mPatternListeners.forEach { it.onCleared() }
    }

    private fun clearPatternDrawLookup() {
        for (i in 0 until sDotCount) {
            for (j in 0 until sDotCount) {
                mPatternDrawLookup[i][j] = false
            }
        }
    }

    private fun detectAndAddHit(x: Float, y: Float): Dot? {
        val dot = checkForNewHit(x, y) ?: return null

        var fillInGapDot: Dot? = null
        if (mPattern.isNotEmpty()) {
            val lastDot = mPattern[mPattern.size - 1]
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
            !mPatternDrawLookup[fillInGapDot.row][fillInGapDot.column]
        ) {
            addCellToPattern(fillInGapDot)
        }

        addCellToPattern(dot)

        if (mEnableHapticFeedback) {
            performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING or
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }
        return dot
    }

    private fun addCellToPattern(newDot: Dot) {
        mPatternDrawLookup[newDot.row][newDot.column] = true
        mPattern.add(newDot)
        if (!mInStealthMode) {
            startDotSelectedAnimation(newDot)
        }
        notifyPatternProgress()
    }

    private fun startDotSelectedAnimation(dot: Dot) {
        val dotState = mDotStates[dot.row][dot.column]
        startSizeAnimation(
            mDotNormalSize.toFloat(),
            mDotSelectedSize.toFloat(),
            mDotAnimationDuration.toLong(),
            mLinearOutSlowInInterpolator,
            dotState
        ) {
            startSizeAnimation(
                mDotSelectedSize.toFloat(),
                mDotNormalSize.toFloat(),
                mDotAnimationDuration.toLong(),
                mFastOutSlowInInterpolator,
                dotState,
                null
            )
        }
        startLineEndAnimation(
            dotState,
            mInProgressX,
            mInProgressY,
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
            state.mLineEndX = (1 - t) * startX + t * targetX
            state.mLineEndY = (1 - t) * startY + t * targetY
            invalidate()
        }
        valueAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                state.mLineAnimator = null
            }
        })
        valueAnimator.interpolator = mFastOutSlowInInterpolator
        valueAnimator.duration = mPathEndAnimationDuration.toLong()
        valueAnimator.start()
        state.mLineAnimator = valueAnimator
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
            state.mSize = animation.animatedValue as Float
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

        if (mPatternDrawLookup[rowHit][columnHit]) return null

        return dotOf(rowHit, columnHit)
    }

    private fun getRowHit(y: Float): Int {
        val squareHeight = mViewHeight
        val hitSize = squareHeight * mHitFactor

        val offset = paddingTop + (squareHeight - hitSize) / 2f
        for (i in 0 until sDotCount) {
            val hitTop = offset + squareHeight * i
            if (y >= hitTop && y <= hitTop + hitSize) {
                return i
            }
        }
        return -1
    }

    private fun getColumnHit(x: Float): Int {
        val squareWidth = mViewWidth
        val hitSize = squareWidth * mHitFactor

        val offset = paddingLeft + (squareWidth - hitSize) / 2f
        for (i in 0 until sDotCount) {
            val hitLeft = offset + squareWidth * i
            if (x >= hitLeft && x <= hitLeft + hitSize) {
                return i
            }
        }
        return -1
    }

    private fun handleActionMove(event: MotionEvent) {
        val radius = mPathWidth.toFloat()
        val historySize = event.historySize
        mTempInvalidateRect.setEmpty()
        var invalidateNow = false

        for (i in 0..historySize) {
            val x = if (i < historySize) event.getHistoricalX(i) else event.x
            val y = if (i < historySize) event.getHistoricalY(i) else event.y

            val hitDot = detectAndAddHit(x, y)
            val patternSize = mPattern.size
            if (hitDot != null && patternSize == 1) {
                mPatternInProgress = true
                notifyPatternStarted()
            }

            val dx = abs(x - mInProgressX)
            val dy = abs(y - mInProgressY)
            if (dx > DEFAULT_DRAG_THRESHOLD || dy > DEFAULT_DRAG_THRESHOLD) {
                invalidateNow = true
            }

            if (mPatternInProgress && patternSize > 0) {
                val pattern = mPattern
                val lastDot = pattern[patternSize - 1]
                val lastCellCenterX = getCenterXForColumn(lastDot.column)
                val lastCellCenterY = getCenterYForRow(lastDot.row)

                var left = min(lastCellCenterX, x) - radius
                var right = max(lastCellCenterX, x) + radius
                var top = min(lastCellCenterY, y) - radius
                var bottom = max(lastCellCenterY, y) + radius

                if (hitDot != null) {
                    val width = mViewWidth * 0.5f
                    val height = mViewHeight * 0.5f
                    val hitCellCenterX = getCenterXForColumn(hitDot.column)
                    val hitCellCenterY = getCenterYForRow(hitDot.row)

                    left = min(hitCellCenterX - width, left)
                    right = max(hitCellCenterX + width, right)
                    top = min(hitCellCenterY - height, top)
                    bottom = max(hitCellCenterY + height, bottom)
                }

                mTempInvalidateRect.union(
                    left.toInt(),
                    top.toInt(),
                    right.toInt(),
                    bottom.toInt()
                )
            }
        }

        mInProgressX = event.x
        mInProgressY = event.y

        if (invalidateNow) {
            mInvalidate.union(mTempInvalidateRect)
            invalidate(mInvalidate)
            mInvalidate.set(mTempInvalidateRect)
        }
    }

    private fun sendAccessEvent(resId: Int) {
        announceForAccessibility(context.getString(resId))
    }

    private fun handleActionUp(event: MotionEvent) {
        if (mPattern.isNotEmpty()) {
            mPatternInProgress = false
            cancelLineAnimations()
            notifyPatternDetected()
            invalidate()
        }
        if (PROFILE_DRAWING && mDrawingProfilingStarted) {
            Debug.stopMethodTracing()
            mDrawingProfilingStarted = false
        }
    }

    private fun cancelLineAnimations() {
        for (i in 0 until sDotCount) {
            for (j in 0 until sDotCount) {
                val state = mDotStates[i][j]
                state.mLineAnimator?.let {
                    it.cancel()
                    state.mLineEndX = Float.MIN_VALUE
                    state.mLineEndY = Float.MIN_VALUE
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
            mPatternInProgress = true
            mPatternViewMode = PatternViewMode.CORRECT
            notifyPatternStarted()
        } else {
            mPatternInProgress = false
            notifyPatternCleared()
        }
        if (hitDot != null) {
            val startX = getCenterXForColumn(hitDot.column)
            val startY = getCenterYForRow(hitDot.row)

            val widthOffset = mViewWidth / 2f
            val heightOffset = mViewHeight / 2f

            invalidate(
                (startX - widthOffset).toInt(),
                (startY - heightOffset).toInt(),
                (startX + widthOffset).toInt(),
                (startY + heightOffset).toInt()
            )
        }
        mInProgressX = x
        mInProgressY = y
        if (PROFILE_DRAWING && !mDrawingProfilingStarted) {
            Debug.startMethodTracing("PatternLockDrawing")
            mDrawingProfilingStarted = true
        }
    }

    private fun getCenterXForColumn(column: Int): Float =
        paddingLeft + column * mViewWidth + mViewWidth / 2f

    private fun getCenterYForRow(row: Int): Float =
        paddingTop + row * mViewHeight + mViewHeight / 2f

    private fun calculateLastSegmentAlpha(
        x: Float,
        y: Float,
        lastX: Float,
        lastY: Float
    ): Float {
        val diffX = x - lastX
        val diffY = y - lastY
        val dist = sqrt(diffX * diffX + diffY * diffY)
        val fraction = dist / mViewWidth
        return min(1f, max(0f, (fraction - 0.3f) * 4f))
    }

    private fun getCurrentColor(partOfPattern: Boolean): Int {
        return if (!partOfPattern || mInStealthMode || mPatternInProgress) {
            mNormalStateColor
        } else if (mPatternViewMode == PatternViewMode.WRONG) {
            mWrongStateColor
        } else if (mPatternViewMode == PatternViewMode.CORRECT ||
            mPatternViewMode == PatternViewMode.AUTO_DRAW
        ) {
            mCorrectStateColor
        } else {
            throw IllegalStateException("Unknown view mode $mPatternViewMode")
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
        mDotPaint.color = getCurrentColor(partOfPattern)
        mDotPaint.alpha = (alpha * 255).toInt()
        canvas.drawCircle(centerX, centerY, size / 2f, mDotPaint)
    }

    private fun Dot.id(): Int{
        return row * sDotCount + column
    }

    private fun dotOf(row: Int, column: Int): Dot {
        checkRange(row, column)
        return sDots[row][column]
    }

    private fun dotOf(id: Int): Dot {
        val count = sDotCount
        return dotOf(id / count, id % count)
    }

    private fun checkRange(row: Int, column: Int) {
        val max = sDotCount - 1
        require(row in 0..max) {
            "mRow must be in range 0-$max"
        }
        require(column in 0..max) {
            "mColumn must be in range 0-$max"
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
