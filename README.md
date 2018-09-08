# PullToDetailsLayout 仿京东天猫上拉加载详情

## 有图有真相

![20180908153637620554347.gif](http://blog-qiniu.jwenfeng.com/20180908153637620554347.gif)

## 使用步骤

1. 引入 PullToDetailsLayout 
``` 
implementation 'com.jwenfeng.library:PullToDetails:1.0.0'
```
2. 使用 PullToDetailsLayout 

``` xml
<com.jwenfeng.library.PullToDetailsLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    
    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:overScrollMode="never">
    </ScrollView>
    
    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:overScrollMode="never">
    </ScrollView>
    
</com.jwenfeng.library.PullToDetailsLayout>
```
> 只能放入两个 View 支持 ScrollView、ListView、RecyclerView 等

3. 滚动监听

``` kotlin
main_pull_to_details.onPageChangeListener = object:PullToDetailsLayout.OnPageChangeListener{
    override fun onChange(status: Int) {
        Log.d("MainActivity","status:$status")
    }
}
```


## 实现原理
1. 自定义一个Layout 继承 ViewGroup

``` kotlin
class PullToDetailsLayout:ViewGroup {
     // 最小滑动距离
    private var mTouchSlop = 0
    private var velocityTracker: VelocityTracker
    private var scroller: Scroller
    
    constructor(context: Context) : this(context, null)

    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    constructor(context: Context, attributeSet: AttributeSet?, styleRes: Int) : super(context, attributeSet, styleRes) {
        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
        velocityTracker = VelocityTracker.obtain()
        scroller = Scroller(context)
    }
}
```

2. 测量 Layout 的大小，设置子 View 的大小和自身一样大

``` kotlin
class PullToDetailsLayout:ViewGroup {
     
    ...
    
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
    
}
```

3. 让子 View 重新布局，从上到下分布

``` kotlin
class PullToDetailsLayout:ViewGroup {
     
    ...
    
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
    
}
```

4. 触摸事件分发

> `onInterceptTouchEvent` 用来判断是否拦截某个事件，如果当前的 View 拦截了某个事件，那么在同一个事件序列中，此方法不会再次被调用。返回结果表示是否拦截当前事件。

 在这里我们需要判断第一页的 View 是否可以向上继续滑动，如果第一页的 View 不能继续滑动，那么自身 View 就需要拦截当前事件处理滑动；
 
 同理我们也需要判断第二页的 View 是否可以向下继续滑动，如不能滑动那么自身的 View 就需要拦截当前事件来处理滑动。
 
 ``` kotlin
 class PullToDetailsLayout:ViewGroup {
     
    ...
    // 0：第一页 1：第二页
    private var status = 0
    private var lastY = 0
    
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
    
}
 ```
 
5. 处理滑动

父 View 获取到滑动通过 `scrollBy()`  方法进行滑动，这里 `dy /= 3` 是增加滑动的弹性

当手指离屏幕的时候根据滑动速度来决定是回弹到第一页还是第二页，通过 `velocityTracker.yVelocity` 来获取Y轴的滑动速度。


``` kotlin
class PullToDetailsLayout:ViewGroup {
     
    ...
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
                    } else {
                        smoothScroll(0);
                    }
                } else {
                    if (yVelocity > 0 && yVelocity > speed) {
                        smoothScroll(0);
                        status = 0;
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
    
}
```

完整代码详见GitHub：[PullToDetailsLayout 仿京东天猫上拉加载详情](https://github.com/823546371/PullToDetailsLayout)


---


## 微信公众号


<img src="Screenshot/qrcode.jpg"/>
佛系开发
