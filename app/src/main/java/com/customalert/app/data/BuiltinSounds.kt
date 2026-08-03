package com.customalert.app.data

object BuiltinSounds {
    val all: List<SoundAsset> = listOf(
        SoundAsset("builtin_cling", "Cling", SoundKind.BUILTIN, rawName = "notification_tone_cling"),
        SoundAsset("builtin_ding", "Ding", SoundKind.BUILTIN, rawName = "notification_tone_ding"),
        SoundAsset("builtin_discover", "Discover", SoundKind.BUILTIN, rawName = "notification_tone_discover"),
        SoundAsset("builtin_idea", "Idea", SoundKind.BUILTIN, rawName = "notification_tone_idea"),
        SoundAsset("builtin_poppy", "Poppy", SoundKind.BUILTIN, rawName = "notification_tone_poppy"),
        SoundAsset("builtin_post", "Post", SoundKind.BUILTIN, rawName = "notification_tone_post"),
        SoundAsset("builtin_refresh", "Refresh", SoundKind.BUILTIN, rawName = "notification_tone_refresh"),
        SoundAsset("builtin_welcome", "Welcome", SoundKind.BUILTIN, rawName = "notification_tone_welcome")
    )
}
