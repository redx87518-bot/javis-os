package com.javis.os.service

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.javis.os.MainActivity

class JavisTileService : TileService() {

    override fun onTileAdded() {
        super.onTileAdded()
        updateTile(false)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile(JavisAssistantService.NOTIFICATION_ID > 0)
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            action = "com.javis.os.ACTION_START_LISTENING"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivityAndCollapse(intent)
    }

    private fun updateTile(active: Boolean) {
        qsTile?.apply {
            state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "JAVIS"
            updateTile()
        }
    }
}
