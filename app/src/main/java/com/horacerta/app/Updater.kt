package com.horacerta.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object Updater {

    private const val REPO = "viniciusy30-commits/horahora"
    private const val PREFS = "hora_prefs"
    private const val SEIS_HORAS = 6L * 60 * 60 * 1000

    private var pendingFile: File? = null
    @Volatile private var checking = false

    @Suppress("DEPRECATION")
    fun installedVersion(ctx: Context): Long {
        val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        return if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else pi.versionCode.toLong()
    }

    private fun ui(activity: Activity, block: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) return
        activity.runOnUiThread(block)
    }

    private fun toast(activity: Activity, msg: String) {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
    }

    /** Chamado ao abrir o app: no máximo a cada 6 horas, só avisa se houver versão nova. */
    fun autoCheck(activity: Activity) {
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now - prefs.getLong("last_update_check", 0L) < SEIS_HORAS) return
        prefs.edit().putLong("last_update_check", now).apply()
        check(activity, false)
    }

    fun check(activity: Activity, manual: Boolean) {
        if (checking) return
        checking = true
        if (manual) toast(activity, "Procurando atualização…")

        Thread {
            try {
                val conn = URL("https://api.github.com/repos/$REPO/releases/latest")
                    .openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.setRequestProperty("User-Agent", "HoraCerta-App")

                if (conn.responseCode != 200) {
                    conn.disconnect()
                    ui(activity) { if (manual) toast(activity, "Ainda não há versão publicada.") }
                    return@Thread
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val json = JSONObject(body)
                val remote = json.getString("tag_name").removePrefix("v").toLongOrNull() ?: 0L
                var apkUrl: String? = null
                val assets = json.getJSONArray("assets")
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.getString("name").endsWith(".apk")) {
                        apkUrl = a.getString("browser_download_url")
                        break
                    }
                }
                val installed = installedVersion(activity)

                ui(activity) {
                    if (remote > installed && apkUrl != null) {
                        showUpdateDialog(activity, remote, installed, apkUrl)
                    } else if (manual) {
                        toast(activity, "Você já está na versão mais nova.")
                    }
                }
            } catch (e: Exception) {
                ui(activity) { if (manual) toast(activity, "Sem internet. Tente de novo mais tarde.") }
            } finally {
                checking = false
            }
        }.start()
    }

    private fun showUpdateDialog(activity: Activity, remote: Long, installed: Long, url: String) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("Nova versão disponível")
            .setMessage("A versão $remote está pronta para instalar (você está na $installed).")
            .setPositiveButton("Atualizar") { _, _ -> download(activity, url) }
            .setNegativeButton("Depois", null)
            .show()
    }

    private fun download(activity: Activity, url: String) {
        val pad = (24 * activity.resources.displayMetrics.density).toInt()
        val label = TextView(activity).apply { text = "Baixando…" }
        val bar = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            isIndeterminate = true
        }
        val box = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
            addView(label)
            addView(bar)
        }
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle("Baixando atualização")
            .setView(box)
            .setCancelable(false)
            .create()
        dialog.show()

        Thread {
            try {
                val dir = File(activity.cacheDir, "updates").apply { mkdirs() }
                dir.listFiles()?.forEach { it.delete() }
                val file = File(dir, "update.apk")

                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                conn.setRequestProperty("User-Agent", "HoraCerta-App")
                val total = conn.contentLengthLong

                conn.inputStream.use { input ->
                    FileOutputStream(file).use { out ->
                        val buf = ByteArray(16384)
                        var done = 0L
                        var lastPct = -1
                        var read = input.read(buf)
                        while (read != -1) {
                            out.write(buf, 0, read)
                            done += read
                            if (total > 0) {
                                val pct = (done * 100 / total).toInt()
                                if (pct != lastPct) {
                                    lastPct = pct
                                    ui(activity) {
                                        bar.isIndeterminate = false
                                        bar.progress = pct
                                        label.text = "$pct%"
                                    }
                                }
                            }
                            read = input.read(buf)
                        }
                    }
                }
                conn.disconnect()

                ui(activity) {
                    dialog.dismiss()
                    install(activity, file)
                }
            } catch (e: Exception) {
                ui(activity) {
                    dialog.dismiss()
                    toast(activity, "Falha ao baixar. Verifique a internet.")
                }
            }
        }.start()
    }

    private fun canInstall(activity: Activity): Boolean {
        return Build.VERSION.SDK_INT < 26 || activity.packageManager.canRequestPackageInstalls()
    }

    private fun install(activity: Activity, file: File) {
        if (!canInstall(activity)) {
            pendingFile = file
            MaterialAlertDialogBuilder(activity)
                .setTitle("Permissão necessária")
                .setMessage(
                    "Para instalar a atualização, o Android precisa da sua permissão. " +
                        "Toque em \"Abrir configuração\", ative \"Permitir deste app\" e volte aqui."
                )
                .setPositiveButton("Abrir configuração") { _, _ ->
                    activity.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:" + activity.packageName)
                        )
                    )
                }
                .setNegativeButton("Agora não") { _, _ -> pendingFile = null }
                .show()
            return
        }
        launchInstaller(activity, file)
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun launchInstaller(activity: Activity, file: File) {
        val uri = FileProvider.getUriForFile(activity, activity.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }

    /** Se o usuário acabou de liberar a permissão, continua a instalação pendente. */
    fun resumePending(activity: Activity) {
        val file = pendingFile ?: return
        if (!canInstall(activity)) return
        pendingFile = null
        if (file.exists()) launchInstaller(activity, file)
    }
}
