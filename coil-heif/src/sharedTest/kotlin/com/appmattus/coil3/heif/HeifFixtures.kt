package com.appmattus.coil3.heif

import kotlin.io.encoding.Base64

/**
 * Small HEIC images made with macOS `sips -s format heic -s formatOptions 90` from lossless PNGs.
 *
 * They use the HEVC Main Still Picture profile with 4:2:0 chroma, like iPhone photos. (At quality 100, sips switches
 * to the 4:4:4 Range Extensions profile, which Android can't decode.) They're rectangular so that a swapped width
 * and height would show.
 */
internal object HeifFixtures {
    const val WIDTH: Int = 64
    const val HEIGHT: Int = 48

    /** Opaque: red top left, green top right, blue bottom left, white bottom right. */
    val quadrants: ByteArray = Base64.decode(
        "AAAAJGZ0eXBoZWljAAAAAG1pZjFNaVBybWlhZk1pSEJoZWljAAABhm1ldGEAAAAAAAAAIWhkbHIAAAAAAAAAAHBpY3QAAAAAAAAA" +
            "AAAAAAAAAAAAJGRpbmYAAAAcZHJlZgAAAAAAAAABAAAADHVybCAAAAABAAAADnBpdG0AAAAAAAEAAAAjaWluZgAAAAAAAQAAABVp" +
            "bmZlAgAAAAABAABodmMxAAAAAOZpcHJwAAAAxWlwY28AAAATY29scm5jbHgAAgACAAaAAAAADGNsbGkAywBAAAAAFGlzcGUAAAAA" +
            "AAAAQAAAADAAAAAJaXJvdAAAAAAQcGl4aQAAAAADCAgIAAAAcWh2Y0MBA3AAAACwAAAAAAAe8AD8/fj4AAALA6AAAQAXQAEMAf//" +
            "A3AAAAMAsAAAAwAAAwAecCShAAEAI0IBAQNwAAADALAAAAMAAAMAHqAUIEHBjE4h7kWVTcCAgYAgogABAAlEAcBghLIQFMkAAAAZ" +
            "aXBtYQAAAAAAAAABAAEGgQIDBYaEAAAAHmlsb2MAAAAARAAAAQABAAAAAQAAAboAAABrAAAAAW1kYXQAAAAAAAAAewAAAGcoAa+j" +
            "yoA71Yn5ez//2MfofeZW+7x6tnouv20bS+E8/+w7mqpgR4Ib8S///G/f/S9wS/8Br6vavtPCuY51Sml1zpNl8z/3MYh/VRXfRemq" +
            "p8A9YLfLefvsG/2Uwb9MrBtpAADGAIOA",
    )

    /** With an alpha plane: opaque red on the left, fully transparent on the right. */
    val halfTransparent: ByteArray = Base64.decode(
        "AAAAKGZ0eXBoZWljAAAAAG1pZjFNaUhFTWlQcm1pYWZNaUhCaGVpYwAAAoFtZXRhAAAAAAAAACFoZGxyAAAAAAAAAABwaWN0AAAA" +
            "AAAAAAAAAAAAAAAAACRkaW5mAAAAHGRyZWYAAAAAAAAAAQAAAAx1cmwgAAAAAQAAAA5waXRtAAAAAAABAAAAOGlpbmYAAAAAAAIA" +
            "AAAVaW5mZQIAAAAAAQAAaHZjMQAAAAAVaW5mZQIAAAEAAgAAaHZjMQAAAAAaaXJlZgAAAAAAAAAOYXV4bAACAAEAAQAAAaRpcHJw" +
            "AAABe2lwY28AAAATY29scm5jbHgAAgACAAaAAAAADGNsbGkAywBAAAAAFGlzcGUAAAAAAAAAQAAAADAAAAAJaXJvdAAAAAAQcGl4" +
            "aQAAAAADCAgIAAAADnBpeGkAAAAAAQgAAAA3YXV4QwAAAAB1cm46bXBlZzpoZXZjOjIwMTU6YXV4aWQ6MQAAAAAMAAAACE4BpQQA" +
            "Af5AAAAAcWh2Y0MBA3AAAACwAAAAAAAe8AD8/fj4AAALA6AAAQAXQAEMAf//A3AAAAMAsAAAAwAAAwAecCShAAEAI0IBAQNwAAAD" +
            "ALAAAAMAAAMAHqAUIEHBjE4h7kWVTcCAgYAgogABAAlEAcBghLIQFMkAAABxaHZjQwEECAAAAL/IAAAAAB7wAPz8+PgAAAsDoAAB" +
            "ABdAAQwB//8ECAAAAwC/yAAAAwAAHhcCQKEAAQAjQgEBBAgAAAMAv8gAAAMAAB7AUIEHAwwjiBe5FlU3AgICAICiAAEACUQBwGHS" +
            "yEBTJAAAACFpcG1hAAAAAAAAAAIAAQaBAgMFiIQAAgUDBoeJhAAAACxpbG9jAAAAAEQAAAIAAQAAAAEAAAK5AAAATwACAAAAAQAA" +
            "AwgAAAAkAAAAAW1kYXQAAAAAAAAAgwAAAEsoAa+jV2MnJv8J2//9/3dl+SjqxKefwUf0v7vtv/9VUwzmH7Hhv/7upf/2Thh3H/6A" +
            "LEfuPWFr/6ngn/eMP/9vgbbx+FvlzyreDXwAAAAgKAGvRNiR/hP1Ghr1//7A0mJHKjLFuHMv78wGrEiKJsA=",
    )
}
