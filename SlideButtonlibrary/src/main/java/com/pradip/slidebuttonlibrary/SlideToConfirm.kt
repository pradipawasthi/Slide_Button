package com.pradip.slidebuttonlibrary

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable

class SlideToConfirm : RelativeLayout {
    private var mContext: Context? = null
    private val DEFAULT_BORDER_WIDTH = 2
    private val DEFAULT_BORDER_RADIUS = 8
    private val DEFAULT_SLIDER_BACKGROUND_COLOR = Color.TRANSPARENT
    private val DEFAULT_SLIDER_COLOR = Color.parseColor("#484EAA")
    private val DEFAULT_SLIDER_WIDTH = Util.dp2px(60).toInt()
    private val DEFAULT_SLIDER_LOTTIE = "slide_right.json"
    private val DEFAULT_RESET_DURATION = 300
    private val DEFAULT_VIBRATION_DURATION = 50
    private val DEFAULT_ENGAGED_TEXT = "Slide to confirm"
    private val DEFAULT_ENGAGED_TEXT_SIZE = 17
    private val DEFAULT_ENGAGED_TEXT_COLOR = DEFAULT_SLIDER_COLOR
    private val DEFAULT_COMPLETED_TEXT = "Confirmed"
    private val DEFAULT_COMPLETED_TEXT_SIZE = 17
    private val DEFAULT_COMPLETED_TEXT_COLOR = Color.WHITE

    // Border
    private var mBorderWidth = 0f
    private var mBorderRadius = 0f
    private val mBorderCornerRadii =
        floatArrayOf(mBorderRadius, mBorderRadius, mBorderRadius, mBorderRadius)

    // Slider anchor
    private var mSliderBackgroundColor = 0
    private var mSliderColor = 0
    private var mSliderWidth = 0
    private var mSliderImageWidth = 0 // Not in real use for now, simply equals mSliderWidth
    private var mSliderLottie: String? = null
    private var mSliderImageResId = 0
    private var mVibrationDuration = 0
    private var mSliderResetDuration = 0
    private var mSliderThreshold = 0f

    // Engaged text
    private var mEngagedText: String? = null
    private var mEngagedTextSize = 0f
    private var mEngagedTextColor = 0
    private var mEngagedTextTypeFace: Typeface? = null

    // Confirmed
    private var mCompletedText: String? = null
    private var mCompletedTextSize = 0f
    private var mCompletedTextColor = 0
    private var mCompletedTextTypeFace: Typeface? = null
    private var mEngagedTextView: TextView? = null
    private var mCTA: TextView? = null
    private var mSwipedView: RelativeLayout? = null
    private var mSlider: View? = null
    private var mTotalWidth = 0
    private var mDownX = 0
    private var mDeltaX = 0
    private var mResetting = false
    private var mStartDrag = false
    private var mUnlocked = false
    var slideListener: ISlideListener? = null

    constructor(context: Context?) : super(context)

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int = 0) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initAttrs(context, attrs, defStyleAttr)
    }

    private fun initAttrs(context: Context, attrs: AttributeSet, defStyleAttr: Int) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.SlideToConfirm, defStyleAttr, 0)
        mSliderLottie = ta.getString(R.styleable.SlideToConfirm_slider_lottie)
        if (TextUtils.isEmpty(mSliderLottie)) {
            mSliderLottie = DEFAULT_SLIDER_LOTTIE
        }
        val sliderImageResId = ta.getResourceId(R.styleable.SlideToConfirm_slider_image, 0)
        if (sliderImageResId != 0) {
            mSliderImageResId = sliderImageResId
        }
        mSliderBackgroundColor = ta.getColor(
            R.styleable.SlideToConfirm_slider_background_color,
            DEFAULT_SLIDER_BACKGROUND_COLOR
        )
        mSliderColor = ta.getColor(R.styleable.SlideToConfirm_slider_color, DEFAULT_SLIDER_COLOR)
        mSliderWidth =
            ta.getDimension(R.styleable.SlideToConfirm_slider_width, DEFAULT_SLIDER_WIDTH.toFloat())
                .toInt()
        mSliderWidth =
            if (mSliderWidth >= DEFAULT_SLIDER_WIDTH) mSliderWidth else DEFAULT_SLIDER_WIDTH
        mSliderImageWidth = mSliderWidth
        mSliderResetDuration =
            ta.getInteger(R.styleable.SlideToConfirm_slider_reset_duration, DEFAULT_RESET_DURATION)
        if (mSliderResetDuration < 0) {
            mSliderResetDuration = DEFAULT_RESET_DURATION
        }
        mVibrationDuration = ta.getInteger(
            R.styleable.SlideToConfirm_slider_vibration_duration,
            DEFAULT_VIBRATION_DURATION
        )
        mSliderThreshold = ta.getDimension(R.styleable.SlideToConfirm_slider_threshold, 0f)
        if (mSliderThreshold < 0) {
            mSliderThreshold = 0f
        }
        mBorderWidth =
            ta.getDimension(R.styleable.SlideToConfirm_border_width, DEFAULT_BORDER_WIDTH.toFloat())
        mBorderRadius = ta.getDimension(
            R.styleable.SlideToConfirm_border_radius,
            DEFAULT_BORDER_RADIUS.toFloat()
        )
        for (i in 0..3) {
            mBorderCornerRadii[i] = mBorderRadius
        }
        mEngagedText = ta.getString(R.styleable.SlideToConfirm_engage_text)
        if (mEngagedText == null) {
            mEngagedText = DEFAULT_ENGAGED_TEXT
        }
        mEngagedTextColor =
            ta.getColor(R.styleable.SlideToConfirm_engage_text_color, DEFAULT_ENGAGED_TEXT_COLOR)
        mEngagedTextSize = ta.getDimension(
            R.styleable.SlideToConfirm_engage_text_size,
            DEFAULT_ENGAGED_TEXT_SIZE.toFloat()
        )
        var typefaceResId = ta.getResourceId(R.styleable.SlideToConfirm_engaged_text_font, -1)
        if (typefaceResId != -1) {
            try {
                mEngagedTextTypeFace = ResourcesCompat.getFont(context, typefaceResId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mCompletedText = ta.getString(R.styleable.SlideToConfirm_completed_text)
        if (mCompletedText == null) {
            mCompletedText = DEFAULT_COMPLETED_TEXT
        }
        mCompletedTextColor = ta.getColor(
            R.styleable.SlideToConfirm_completed_text_color,
            DEFAULT_COMPLETED_TEXT_COLOR
        )
        mCompletedTextSize = ta.getDimension(
            R.styleable.SlideToConfirm_completed_text_size,
            DEFAULT_COMPLETED_TEXT_SIZE.toFloat()
        )
        typefaceResId = ta.getResourceId(R.styleable.SlideToConfirm_completed_text_font, -1)
        if (typefaceResId != -1) {
            try {
                mCompletedTextTypeFace = ResourcesCompat.getFont(context, typefaceResId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        ta.recycle()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val handler = Handler {
            init()
            true
        }
        handler.sendEmptyMessage(0)
    }

    private fun addSwipedView() {
        mSwipedView = RelativeLayout(mContext)
        val layoutParams = LayoutParams(mSliderWidth, LayoutParams.MATCH_PARENT)
        layoutParams.addRule(ALIGN_PARENT_LEFT)
        mSwipedView!!.setLayoutParams(layoutParams)

        // set bg
        val bg = GradientDrawable()
        bg.setColor(mSliderColor)
        val cornerRadii = FloatArray(8)
        var index = 0
        for (r in mBorderCornerRadii) {
            cornerRadii[index++] = r
            cornerRadii[index++] = r
        }
        bg.setCornerRadii(cornerRadii)
        mSwipedView!!.background = bg


        // CTA text view
        mCTA = TextView(mContext)
        val ctaLayoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        mCTA!!.setLayoutParams(ctaLayoutParams)
        mCTA!!.setGravity(Gravity.CENTER)
        mCTA!!.setTextSize(TypedValue.COMPLEX_UNIT_PX, mCompletedTextSize)
        mCTA!!.setTextColor(mCompletedTextColor)
        if (mCompletedTextTypeFace != null) {
            mCTA!!.setTypeface(mCompletedTextTypeFace)
        }
        mSwipedView!!.addView(mCTA)

        // slider
        mSlider = slider
        mSwipedView!!.addView(mSlider)
        this.addView(mSwipedView)
    }

    private val slider: View
        private get() {
            val sliderView: ImageView
            val layoutParams = LayoutParams(mSliderImageWidth, LayoutParams.MATCH_PARENT)
            if (mSliderImageResId != 0) {
                sliderView = ImageView(mContext)
                sliderView.setImageResource(mSliderImageResId)
                layoutParams.addRule(ALIGN_PARENT_RIGHT)
                layoutParams.addRule(CENTER_VERTICAL)
            } else {
                sliderView = LottieAnimationView(mContext)
                sliderView.setAnimation(mSliderLottie)
                sliderView.setRepeatCount(LottieDrawable.INFINITE)
                sliderView.playAnimation()
                layoutParams.addRule(ALIGN_PARENT_RIGHT)
            }
            sliderView.setLayoutParams(layoutParams)
            return sliderView
        }

    private fun addEngagedTextView() {
        mEngagedTextView = TextView(mContext)
        val width = measuredWidth - mSliderWidth
        val layoutParams = LayoutParams(width, LayoutParams.MATCH_PARENT)
        layoutParams.addRule(ALIGN_PARENT_RIGHT)
        mEngagedTextView!!.setLayoutParams(layoutParams)
        mEngagedTextView!!.setGravity(Gravity.CENTER)
        mEngagedTextView!!.text = mEngagedText
        mEngagedTextView!!.setTextSize(TypedValue.COMPLEX_UNIT_PX, mEngagedTextSize)
        mEngagedTextView!!.setTextColor(mEngagedTextColor)
        if (mEngagedTextTypeFace != null) {
            mEngagedTextView!!.setTypeface(mEngagedTextTypeFace)
        }
        this.addView(mEngagedTextView)
    }

    private fun init() {
        mContext = this.context
        mTotalWidth = measuredWidth
        addEngagedTextView()
        addSwipedView()

        // bg
        setBackgroundResource(R.drawable.stc_bg)
        val bg = this.background as GradientDrawable
        bg.setStroke(mBorderWidth.toInt(), mSliderColor)
        bg.setColor(mSliderBackgroundColor)


        // bg corners
        val cornerRadii = FloatArray(8)
        var index = 0
        for (r in mBorderCornerRadii) {
            cornerRadii[index++] = r
            cornerRadii[index++] = r
        }
        bg.setCornerRadii(cornerRadii)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (mUnlocked || mResetting) {
            return false
        }
        val action = ev.action
        if (action == MotionEvent.ACTION_DOWN
            && inSliderArea(ev)
        ) {
            mDownX = ev.x.toInt()
            mStartDrag = true
            Log.w("XX", "onInterceptTouchEvent: true")
            return true
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!mStartDrag || mUnlocked) {
            return true
        }
        var reachEnd = false
        val action = ev.action
        if (action == MotionEvent.ACTION_DOWN) {
            requestDisallowInterceptTouchEvent(true)
            notifySliderStart()
        } else if (action == MotionEvent.ACTION_MOVE) {
            val moveX = ev.x.toInt()
            mDeltaX += moveX - mDownX
            Log.w("XX", "deltaX == $mDeltaX")
            mDownX = moveX
            var left = mDeltaX
            notifySliderMove()
            if (left + mSliderWidth >= mTotalWidth) {
                left = mTotalWidth - mSliderWidth
            } else if (left < 0) {
                left = 0
            }
            val swipedLayoutParams = mSwipedView!!.layoutParams as LayoutParams
            swipedLayoutParams.width = left + mSliderWidth
            mSwipedView!!.setLayoutParams(swipedLayoutParams)
        } else if (action == MotionEvent.ACTION_UP) {
            if (mDeltaX + mSliderWidth >= mTotalWidth - mSliderThreshold) {
                reachEnd = true
            }
            if (reachEnd) {
                setUnlockedStatus()
                return true
            }
            resetSlider()
        }
        performClick()
        return true
    }

    private fun resetSlider() {
        mDownX = 0
        mDeltaX = 0
        mStartDrag = false
        mResetting = true
        val translationXNow = (mSwipedView!!.width - mSliderWidth).toFloat()
        mSlider!!.animate().translationX(0f)
            .setDuration(mSliderResetDuration.toLong())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mResetting = false
                }
            })
            .setUpdateListener { animation ->
                val animatedValue = animation.getAnimatedValue()
                if (animatedValue is Float) {
                    val width = translationXNow * (1 - animatedValue) + mSliderWidth
                    val swipedLayoutParams = mCTA!!.layoutParams as LayoutParams
                    swipedLayoutParams.width = width.toInt()
                    mSwipedView!!.setLayoutParams(swipedLayoutParams)
                }
            }
            .start()
        notifySliderCancel()
    }

    private fun setUnlockedStatus() {
        val swipedLayoutParams = mSwipedView!!.layoutParams as LayoutParams
        swipedLayoutParams.width = mTotalWidth
        mSwipedView!!.setLayoutParams(swipedLayoutParams)
        mUnlocked = true
        handleVibration()
        mEngagedTextView!!.visibility = GONE
        mSlider!!.visibility = GONE
        mCTA!!.text = mCompletedText
        mCTA!!.visibility = VISIBLE
        notifySliderDone()
    }

    fun reset() {
        mDownX = 0
        mDeltaX = 0
        mUnlocked = false
        mStartDrag = false
        val swipedLayoutParams = mSwipedView!!.layoutParams as LayoutParams
        swipedLayoutParams.width = mSliderWidth
        mSwipedView!!.setLayoutParams(swipedLayoutParams)
        mEngagedTextView!!.visibility = VISIBLE
        mSlider!!.visibility = VISIBLE
        mCTA!!.visibility = GONE
    }

    fun setEngageText(engageText: String?) {
        if (TextUtils.isEmpty(engageText)) {
            return
        }
        mEngagedText = engageText
        if (mEngagedTextView != null) {
            mEngagedTextView!!.text = engageText
        }
    }

    fun setCompletedText(completedText: String?) {
        if (TextUtils.isEmpty(completedText)) {
            return
        }
        mCompletedText = completedText
        if (mCTA != null) {
            mCTA!!.text = completedText
        }
    }

    private fun handleVibration() {
        if (ContextCompat.checkSelfPermission(mContext!!, Manifest.permission.VIBRATE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val vibrator = mContext!!.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    mVibrationDuration.toLong(),
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            vibrator.vibrate(mVibrationDuration.toLong())
        }
    }

    private fun inSliderArea(ev: MotionEvent): Boolean {
        val x = ev.x
        return 0 < x && x < mSliderWidth
    }

    /* -------------------------------- *
     * Slider callbacks
     * -------------------------------- */
    private fun notifySliderStart() {
        if (slideListener != null) {
            slideListener!!.onSlideStart()
        }
    }

    private fun notifySliderMove() {
        val percent = mDeltaX.toFloat() / mTotalWidth.toFloat()
        if (null != slideListener) {
            slideListener!!.onSlideMove(percent)
        }
    }

    private fun notifySliderCancel() {
        if (slideListener != null) {
            slideListener!!.onSlideCancel()
        }
    }

    private fun notifySliderDone() {
        if (slideListener != null) {
            slideListener!!.onSlideDone()
        }
    }
}
