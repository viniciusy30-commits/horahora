package com.horacerta.app

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : AppCompatActivity() {

    private lateinit var clock: ClockView
    private lateinit var tvTime: TextView
    private lateinit var tvSeconds: TextView
    private lateinit var tvAmPm: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvZone: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val ptBR = Locale("pt", "BR")
    private val prefs by lazy { getSharedPreferences("hora_prefs", MODE_PRIVATE) }

    private val cores = listOf(
        "Turquesa" to 0xFF4FD1C5.toInt(),
        "Vermelho" to 0xFFFF4D4D.toInt(),
        "Azul" to 0xFF5B9DFF.toInt(),
        "Roxo" to 0xFFB47CFF.toInt()
    )

    private var fmt24 = SimpleDateFormat("HH:mm", ptBR)
    private var fmt12 = SimpleDateFormat("hh:mm", ptBR)
    private var fmtSec = SimpleDateFormat("ss", ptBR)
    private var fmtAmPm = SimpleDateFormat("a", ptBR)
    private var fmtDate = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", ptBR)

    private val tick = object : Runnable {
        override fun run() {
            updateTexts()
            handler.postDelayed(this, 200)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clock = findViewById(R.id.clock)
        tvTime = findViewById(R.id.tvTime)
        tvSeconds = findViewById(R.id.tvSeconds)
        tvAmPm = findViewById(R.id.tvAmPm)
        tvDate = findViewById(R.id.tvDate)
        tvZone = findViewById(R.id.tvZone)
        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener { showSettings() }

        applyPrefs()

        val content = findViewById<View>(R.id.content)
        content.alpha = 0f
        content.translationY = 40f
        content.animate().alpha(1f).translationY(0f).setDuration(600).start()

        Updater.autoCheck(this)
        Novidades.showIfNew(this)
    }

    override fun onResume() {
        super.onResume()
        initFormats()
        handler.post(tick)
        Updater.resumePending(this)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
    }

    private fun initFormats() {
        fmt24 = SimpleDateFormat("HH:mm", ptBR)
        fmt12 = SimpleDateFormat("hh:mm", ptBR)
        fmtSec = SimpleDateFormat("ss", ptBR)
        fmtAmPm = SimpleDateFormat("a", ptBR)
        fmtDate = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", ptBR)
    }

    private fun accent(): Int = prefs.getInt("accent", cores[0].second)

    private fun applyAccent() {
        val c = accent()
        clock.accentColor = c
        tvSeconds.setTextColor(c)
    }

    private fun applyPrefs() {
        val showSec = prefs.getBoolean("seconds", true)
        clock.showSecondHand = showSec
        tvSeconds.visibility = if (showSec) View.VISIBLE else View.GONE
        applyAccent()
        updateTexts()
    }

    private fun setIfChanged(tv: TextView, value: String) {
        if (tv.text.toString() != value) tv.text = value
    }

    private fun updateTexts() {
        val now = Date()
        val is24 = prefs.getBoolean("h24", true)
        setIfChanged(tvTime, (if (is24) fmt24 else fmt12).format(now))
        setIfChanged(tvSeconds, fmtSec.format(now))
        if (is24) {
            tvAmPm.visibility = View.GONE
        } else {
            tvAmPm.visibility = View.VISIBLE
            setIfChanged(tvAmPm, fmtAmPm.format(now).uppercase(ptBR))
        }
        setIfChanged(tvDate, fmtDate.format(now).replaceFirstChar { it.uppercase(ptBR) })
        setIfChanged(tvZone, "Fuso horário: " + TimeZone.getDefault().id)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun showSettings() {
        val v = layoutInflater.inflate(R.layout.dialog_settings, null)

        val swH24 = v.findViewById<SwitchMaterial>(R.id.swH24)
        swH24.isChecked = prefs.getBoolean("h24", true)
        swH24.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("h24", checked).apply()
            updateTexts()
        }

        val swSeconds = v.findViewById<SwitchMaterial>(R.id.swSeconds)
        swSeconds.isChecked = prefs.getBoolean("seconds", true)
        swSeconds.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("seconds", checked).apply()
            applyPrefs()
        }

        val btnUpdate = v.findViewById<MaterialButton>(R.id.btnUpdate)
        btnUpdate.setOnClickListener { Updater.check(this, true) }

        val row = v.findViewById<LinearLayout>(R.id.colorRow)

        fun estilizarBotao() {
            val c = accent()
            btnUpdate.setBackgroundColor(c)
            btnUpdate.setTextColor(
                if (ColorUtils.calculateLuminance(c) > 0.5) 0xFF00201D.toInt() else Color.WHITE
            )
        }

        fun montarCores() {
            row.removeAllViews()
            for ((nome, cor) in cores) {
                val dot = View(this)
                val lp = LinearLayout.LayoutParams(dp(40), dp(40))
                lp.marginEnd = dp(14)
                dot.layoutParams = lp
                dot.contentDescription = nome
                dot.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(cor)
                    if (cor == accent()) setStroke(dp(3), Color.WHITE)
                }
                dot.setOnClickListener {
                    prefs.edit().putInt("accent", cor).apply()
                    applyAccent()
                    montarCores()
                    estilizarBotao()
                }
                row.addView(dot)
            }
        }

        montarCores()
        estilizarBotao()

        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "?"
        }
        v.findViewById<TextView>(R.id.tvVersion).text = "Versão $versionName"

        MaterialAlertDialogBuilder(this)
            .setTitle("Configurações")
            .setView(v)
            .setPositiveButton("Fechar", null)
            .show()
    }
}
