package com.jwenfeng.library

import android.content.Context
import android.util.AttributeSet
import android.view.*
import android.widget.Scroller

/**
 * 当前类注释:
 * 作者：jinwenfeng on 2018/9/6 15:33
 * 邮箱：823546371@qq.com
 * QQ： 823546371
 * 公司：南京穆尊信息科技有限公司
 * © 2018 jinwenfeng
 * © 版权所有，未经允许不得传播
 */
class PullToDetailsLayout:ViewGroup {

    // 最小滑动距离
    private var mTouchSlop = 0
    private var velocityTracker: VelocityTracker
    private var scroller: Scroller

    private lateinit var mFrontView: View
    private lateinit var mBehindView: View

    // 0：第一页 1：第二页
    private var status = 0
    private var lastY = 0
    private val speed = 200

    var onPageChangeListener: OnPageChangeListener? = null

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    constructor(context: Context, attributeSet: AttributeSet?, styleRes: Int) : super(context, attributeSet, styleRes) {
        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
        velocityTracker = VelocityTracker.obtain()
        scroller = Scroller(context)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (childCount != 2) {
            throw RuntimeException("child view only support two!!")
        }
        mFrontView = getChildAt(0)
        mBehindView = getChildAt(1)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 获取父布局的大小
        val rootWidth = MeasureSpec.getSize(widthMeasureSpec)
        val rootHeight = MeasureSpec.getSize(heightMeasureSpec)
        // 设置子布局的大小模式
        val childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(rootWidth, MeasureSpec.EXACTLY)
        val childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(rootHeight, MeasureSpec.EXACTLY)

        for (i in 0 until childCount) {
            val childView = getChildAt(i)
            if (childView.visibility == View.GONE) {
                continue
            }
            // 设置子布局和父布局的大小一致
            measureChild(childView, childWidthMeasureSpec, childHeightMeasureSpec)
        }
        setMeasuredDimension(rootWidth, rootHeight)

    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var top = t
        for (i in 0 until childCount) {
            val childView = getChildAt(i)

            if (childView.visibility == View.GONE) {
                continue
            }
            // 设置子布局的位置，按照顺序从上到下
            childView.layout(l, top, r, top + childView.measuredHeight)
            top += childView.measuredHeight
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        //防止子View禁止父view拦截事件
        this.requestDisallowInterceptTouchEvent(false);
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        var shouldIntercept = false
        val y = ev.y.toInt()
        val action = ev.action
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                lastY = y
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = lastY - y

                if (dy > 0 && status == 0) {
                    if (!mFrontView.canScrollVertically(1)) {
                        if (dy >= mTouchSlop) {
                            shouldIntercept = true
                        }
                    }
                } else if (dy < 0 && status == 1) {
                    if (!mBehindView.canScrollVertically(-1)) {
                        if (Math.abs(dy) >= mTouchSlop) {
                            shouldIntercept = true
                        }
                    }
                }
            }
        }
        return shouldIntercept
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val y = ev.y.toInt()
        velocityTracker.addMovement(ev)

        when (ev.action) {
            MotionEvent.ACTION_MOVE -> {
                var dy = lastY - y
                dy /= 3
                if (dy + scrollY < 0) {
                    dy = -(scrollY + dy + Math.abs(dy))
                }
                scrollBy(0, dy)
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                velocityTracker.computeCurrentVelocity(1000)
                val yVelocity = velocityTracker.yVelocity
                if (status == 0) {
                    if (yVelocity < 0 && yVelocity < -speed) {
                        smoothScroll(mFrontView.bottom);
                        status = 1;
                        onPageChangeListener?.onChange(status)
                    } else {
                        smoothScroll(0);
                    }
                } else {
                    if (yVelocity > 0 && yVelocity > speed) {
                        smoothScroll(0);
                        status = 0;
                        onPageChangeListener?.onChange(status)
                    } else {
                        smoothScroll(mFrontView.bottom);
                    }
                }

            }

        }
        lastY = y

        return super.onTouchEvent(ev)
    }


    //通过Scroller实现弹性滑动
    private fun smoothScroll(tartY: Int) {
        val dy = tartY - scrollY
        scroller.startScroll(scrollX, scrollY, 0, dy)
        invalidate()
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.currX, scroller.currY)
            postInvalidate()
        }
    }

    interface OnPageChangeListener{
        fun onChange(status:Int)
    }
}