package ro.trom.cuplaj

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.TextView
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

fun Context.tv(text: String, size: Float = 16f, color: Int = Color.WHITE): TextView {
    val t = TextView(this)
    t.text = text
    t.setTextSize(TypedValue.COMPLEX_UNIT_SP, size * App.textScale)
    t.setTextColor(color)
    return t
}

fun Context.btn(text: String, bg: Int = Color.parseColor("#3A3F47"), fg: Int = Color.WHITE, onClick: () -> Unit): Button {
    val b = Button(this)
    b.text = text
    b.isAllCaps = false
    b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f * App.textScale)
    b.setTextColor(fg)
    styleBtn(b, bg)
    b.setOnClickListener { onClick() }
    return b
}

fun Context.styleBtn(b: Button, bg: Int) {
    val g = GradientDrawable()
    g.setColor(bg)
    g.cornerRadius = dp(10).toFloat()
    b.background = g
}

fun Context.box(bg: Int, radius: Int = 10): GradientDrawable {
    val g = GradientDrawable()
    g.setColor(bg)
    g.cornerRadius = dp(radius).toFloat()
    return g
}

/**
 * Disc rotativ: atingi o gaură, discul se rotește până la opritor și revine;
 * în timpul revenirii se aud impulsurile (tot atâtea câte cifra aleasă, 0 = 10).
 */
class RotaryDialView(ctx: Context, private val onDigit: (Char) -> Unit, private val onPulses: (Int) -> Unit) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var rot = 0f
    private var busy = false
    var active = true
        set(v) {
            field = v
            invalidate()
        }

    private fun holeAngle(d: Int): Double = 60.0 - 30.0 * ((if (d == 0) 10 else d) - 1)

    override fun onDraw(c: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) / 2f - 4f
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#1B1410")
        c.drawCircle(cx, cy, r, paint)
        // cifrele fixe, sub disc
        paint.color = Color.parseColor("#E8D9B8")
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = r * 0.16f
        for (d in 0..9) {
            val a = holeAngle(d) * PI / 180.0
            val hx = cx + (r * 0.72f * sin(a)).toFloat()
            val hy = cy - (r * 0.72f * cos(a)).toFloat()
            c.drawText(d.toString(), hx, hy + paint.textSize * 0.35f, paint)
        }
        // discul mobil, cu găuri decupate
        c.save()
        c.rotate(rot, cx, cy)
        val path = Path()
        path.fillType = Path.FillType.EVEN_ODD
        path.addCircle(cx, cy, r * 0.97f, Path.Direction.CW)
        for (d in 0..9) {
            val a = holeAngle(d) * PI / 180.0
            path.addCircle(cx + (r * 0.72f * sin(a)).toFloat(), cy - (r * 0.72f * cos(a)).toFloat(), r * 0.125f, Path.Direction.CW)
        }
        paint.color = if (active) Color.parseColor("#D8C9A3") else Color.parseColor("#7F7762")
        c.drawPath(path, paint)
        paint.color = Color.parseColor("#1B1410")
        c.drawCircle(cx, cy, r * 0.3f, paint)
        paint.color = Color.parseColor("#B8A97F")
        c.drawCircle(cx, cy, r * 0.22f, paint)
        c.restore()
        // opritorul degetului (la ora 4)
        val sa = 120.0 * PI / 180.0
        paint.color = Color.parseColor("#C9C9C9")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = r * 0.04f
        paint.strokeCap = Paint.Cap.ROUND
        c.drawLine(
            cx + (r * 0.88f * sin(sa)).toFloat(), cy - (r * 0.88f * cos(sa)).toFloat(),
            cx + (r * 1.0f * sin(sa + 0.12)).toFloat(), cy - (r * 1.0f * cos(sa + 0.12)).toFloat(), paint
        )
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN) return active && !busy
        if (e.action != MotionEvent.ACTION_UP || !active || busy) return true
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) / 2f - 4f
        for (d in 0..9) {
            val a = holeAngle(d) * PI / 180.0
            val hx = cx + (r * 0.72f * sin(a)).toFloat()
            val hy = cy - (r * 0.72f * cos(a)).toFloat()
            if (hypot(e.x - hx, e.y - hy) < r * 0.16f) {
                dial(d)
                break
            }
        }
        return true
    }

    private fun dial(d: Int) {
        busy = true
        val pulses = if (d == 0) 10 else d
        val target = ((120.0 - holeAngle(d) + 360.0) % 360.0).toFloat()
        val pull = ValueAnimator.ofFloat(0f, target)
        pull.duration = 250L + (target * 2.2f).toLong()
        pull.interpolator = AccelerateDecelerateInterpolator()
        pull.addUpdateListener {
            rot = it.animatedValue as Float
            invalidate()
        }
        pull.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onPulses(pulses)
                val back = ValueAnimator.ofFloat(target, 0f)
                back.duration = pulses * 100L
                back.interpolator = LinearInterpolator()
                back.addUpdateListener {
                    rot = it.animatedValue as Float
                    invalidate()
                }
                back.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        rot = 0f
                        busy = false
                        invalidate()
                        onDigit(('0' + d))
                    }
                })
                back.start()
            }
        })
        pull.start()
    }
}


/** Shake the phone to answer or hang up (2045 wearables era). Two strong jolts within 700 ms. */
class ShakeToAnswer(private val a: android.content.Context, private val onShake: () -> Unit) : android.hardware.SensorEventListener {
    private var last = 0L
    private var running = false

    fun start() {
        if (running) return
        val sm = a.getSystemService(android.content.Context.SENSOR_SERVICE) as? android.hardware.SensorManager ?: return
        val s = sm.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER) ?: return
        sm.registerListener(this, s, android.hardware.SensorManager.SENSOR_DELAY_GAME)
        running = true
    }

    fun stop() {
        if (!running) return
        (a.getSystemService(android.content.Context.SENSOR_SERVICE) as? android.hardware.SensorManager)?.unregisterListener(this)
        running = false
    }

    override fun onSensorChanged(e: android.hardware.SensorEvent) {
        val g = Math.sqrt((e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2]).toDouble()) / 9.81
        if (g < 2.6) return
        val now = System.currentTimeMillis()
        if (now - last in 120..700) {
            last = 0
            onShake()
        } else if (now - last > 700) last = now
    }

    override fun onAccuracyChanged(s: android.hardware.Sensor?, accuracy: Int) {}
}
