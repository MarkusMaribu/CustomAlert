package com.customalert.app.domain

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.customalert.app.data.SoundAsset
import com.customalert.app.data.SoundKind
import java.io.File

object SoundResolver {
    fun resolveUri(context: Context, asset: SoundAsset): Uri? {
        return when (asset.kind) {
            SoundKind.BUILTIN -> {
                val raw = asset.rawName ?: return null
                val resId = context.resources.getIdentifier(raw, "raw", context.packageName)
                if (resId == 0) null else ("android.resource://${context.packageName}/$resId").toUri()
            }
            SoundKind.CUSTOM -> {
                val path = asset.filePath ?: return null
                val file = File(path)
                if (file.exists()) file.toUri() else null
            }
        }
    }
}
