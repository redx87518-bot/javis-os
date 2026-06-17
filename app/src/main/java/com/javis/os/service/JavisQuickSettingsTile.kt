package com.javis.os.service

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.javis.os.MainActivity

class JavisQuickSettingsTile : TileService() {

    override fun onStartListening() {
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = "JAVIS"
            contentDescription = "Activate JAVIS AI Assistant"
            updateTile()
        }
    }

    override fun onClick() {
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            updateTile()
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            action = "com.javis.os.ACTIVATE_VOICE"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        if (isLocked) {
            unlockAndRun { startActivity(intent) }
        } else {
            startActivity(intent)
        }
    }

    override fun onStopListening() {
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }
}
