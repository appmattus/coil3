package com.appmattus.coil3.heif

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import okio.Buffer
import okio.FileSystem
import kotlin.test.Test
import kotlin.test.assertNull

class HeifDecoderFactoryJvmTest {

    @Test
    fun factoryDoesNotDecodeOnTheJvm() {
        val context = PlatformContext.INSTANCE
        val result = SourceFetchResult(
            source = ImageSource(Buffer().write(HeifFixtures.quadrants), FileSystem.SYSTEM),
            mimeType = "image/heic",
            dataSource = DataSource.MEMORY,
        )

        assertNull(HeifDecoder.Factory().create(result, Options(context), ImageLoader(context)))
    }
}
