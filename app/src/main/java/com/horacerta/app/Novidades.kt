package com.horacerta.app

import android.app.Activity
import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object Novidades {

    fun showIfNew(activity: Activity) {
        try {
            val text = activity.assets.open("novidades.txt").bufferedReader().use { it.readText() }
            var id: String? = null
            var titulo = ""
            val itens = ArrayList<String>()
            for (raw in text.lines()) {
                val line = raw.trim()
                if (line.startsWith("#id ")) {
                    if (id != null) break
                    id = line.removePrefix("#id ").trim()
                } else if (line.startsWith("#titulo ") && id != null) {
                    titulo = line.removePrefix("#titulo ").trim()
                } else if (line.startsWith("- ") && id != null) {
                    itens.add(line.removePrefix("- ").trim())
                }
            }
            val currentId = id ?: return

            val prefs = activity.getSharedPreferences("hora_prefs", Context.MODE_PRIVATE)
            val last = prefs.getString("novidade_id", null)
            prefs.edit().putString("novidade_id", currentId).apply()

            // Primeira instalação: só registra, não mostra
            if (last == null || last == currentId) return

            val msg = titulo + "\n\n" + itens.joinToString("\n") { "• $it" }
            MaterialAlertDialogBuilder(activity)
                .setTitle("O que mudou")
                .setMessage(msg)
                .setPositiveButton("Legal!", null)
                .show()
        } catch (e: Exception) {
            // sem novidades, segue o jogo
        }
    }
}
