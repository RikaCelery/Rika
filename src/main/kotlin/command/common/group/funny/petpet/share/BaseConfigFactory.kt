package command.common.group.funny.petpet.share

object BaseConfigFactory {
    @Deprecated("")
    fun getAvatarExtraDataFromUrls(
        fromAvatarUrl: String?,
        toAvatarUrl: String?,
        groupAvatarUrl: String?,
        botAvatarUrl: String?
    ): AvatarExtraDataProvider? {
        return try {
            AvatarExtraDataProvider(
                if (fromAvatarUrl != null) {
                    {
                        ImageSynthesisCore.getWebImage(fromAvatarUrl)
                    }
                } else null,
                if (toAvatarUrl != null) {
                    {
                        ImageSynthesisCore.getWebImage(toAvatarUrl)
                    }
                } else null,
                if (groupAvatarUrl != null) {
                    {
                        ImageSynthesisCore.getWebImage(groupAvatarUrl)
                    }
                } else null,
                if (botAvatarUrl != null) {
                    {
                        ImageSynthesisCore.getWebImage(botAvatarUrl)
                    }
                } else null
            )
        } catch (e: Exception) {
            null
        }
    }

    fun getGifAvatarExtraDataFromUrls(
        fromAvatarUrl: String?,
        toAvatarUrl: String?,
        groupAvatarUrl: String?,
        botAvatarUrl: String?
    ): GifAvatarExtraDataProvider? {
        return try {
            GifAvatarExtraDataProvider(
                if (fromAvatarUrl != null) {
                    {
                        ImageSynthesisCore.getWebImageAsList(fromAvatarUrl)
                    }
                } else null,
                if (toAvatarUrl != null) {
                    {
                        ImageSynthesisCore.getWebImageAsList(toAvatarUrl)
                    }
                } else null,
                if (groupAvatarUrl != null) {
                    {
                        ImageSynthesisCore.getWebImageAsList(groupAvatarUrl)
                    }
                } else null,
                if (botAvatarUrl != null) {
                    {
                        ImageSynthesisCore.getWebImageAsList(botAvatarUrl)
                    }
                } else null
            )
        } catch (e: Exception) {
            null
        }
    }
}