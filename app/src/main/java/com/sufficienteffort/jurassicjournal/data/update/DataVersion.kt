package com.sufficienteffort.jurassicjournal.data.update

/**
 * A parsed OTA data tag: `data-vSCHEMA.PATCH`, e.g. `data-v9.27`.
 *
 * SCHEMA is a compatibility marker: only releases with the same SCHEMA as the
 * running APK are accepted. A higher SCHEMA means the data requires a newer APK.
 * PATCH is the OTA counter within that schema.
 */
data class DataVersion(val schema: Int, val patch: Int) {

    companion object {
        private val TAG_REGEX = Regex("""data-v(\d+)\.(\d+)""")

        fun parse(tag: String?): DataVersion? {
            val m = TAG_REGEX.matchEntire(tag ?: return null) ?: return null
            return DataVersion(m.groupValues[1].toInt(), m.groupValues[2].toInt())
        }

        /** True when [candidate] is a same-schema, higher-patch release than [current]. */
        fun isNewer(candidate: String, current: String): Boolean {
            val c = parse(candidate) ?: return false
            val k = parse(current) ?: return false
            return c.schema == k.schema && c.patch > k.patch
        }

        /**
         * True when a stored data version cannot be trusted against [bundled]:
         * unparseable (e.g. a locally staged test DB) or from a different schema
         * (an APK upgrade replaced the game DB). In both cases the stored value
         * must be reset to [bundled] or OTA checks would silently stop matching.
         */
        fun isStale(stored: String?, bundled: String): Boolean {
            val s = parse(stored) ?: return true
            val b = parse(bundled) ?: return false
            return s.schema != b.schema
        }
    }
}
