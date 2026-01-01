package co.aospa.glyph.Composer

class GlyphPattern {

    var version: Int = 0
    var audioFile: String? = null
    var duration: Long = 0L
    var frames: List<GlyphFrame>? = null

    constructor()

    constructor(version: Int, audioFile: String?, duration: Long, frames: List<GlyphFrame>?) {
        this.version = version
        this.audioFile = audioFile
        this.duration = duration
        this.frames = frames
    }

    class GlyphFrame {
        var timestamp: Long = 0L
        var zones: IntArray? = null
        var brightness: Int = 0
        var duration: Int = 0

        constructor()

        constructor(timestamp: Long, zones: IntArray?, brightness: Int, duration: Int) {
            this.timestamp = timestamp
            this.zones = zones
            this.brightness = brightness
            this.duration = duration
        }
    }
}
