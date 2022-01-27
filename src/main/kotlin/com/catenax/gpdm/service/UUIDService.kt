package com.catenax.gpdm.service

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

enum class Namespace(private val uuid: UUID) {
    DNS(UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")),
    URL(UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8")),
    OID(UUID.fromString("6ba7b812-9dad-11d1-80b4-00c04fd430c8")),
    X500(UUID.fromString("6ba7b814-9dad-11d1-80b4-00c04fd430c8"));

    fun uuid3(name: String): UUID {
        val nsBytes = toBytes(this.uuid)
        val nameBytes = name.toByteArray()
        return UUID.nameUUIDFromBytes(nsBytes + nameBytes)
    }

    fun uuid5(name: String): UUID {
        val md: MessageDigest
        try {
            md = MessageDigest.getInstance("SHA-1")
        } catch (ex: NoSuchAlgorithmException) {
            throw InternalError("SHA-1 not supported", ex)
        }

        md.update(toBytes(this.uuid))
        md.update(name.toByteArray())
        val bytes = md.digest()
        bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x50).toByte() /* clear version; set to version 5 */
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte() /* clear variant; set to IETF variant */
        return fromBytes(bytes)
    }

    companion object {
        private fun fromBytes(data: ByteArray): UUID {
            // Based on the private UUID(bytes[]) constructor
            assert(data.size >= 16)
            var msb = 0L
            var lsb = 0L
            for (i in 0..7)
                msb = msb shl 8 or (data[i].toLong() and 0xff)
            for (i in 8..15)
                lsb = lsb shl 8 or (data[i].toLong() and 0xff)
            return UUID(msb, lsb)
        }

        private fun toBytes(uuid: UUID): ByteArray {
            // inverted logic of fromBytes()
            val out = ByteArray(16)
            val msb = uuid.mostSignificantBits
            val lsb = uuid.leastSignificantBits
            for (i in 0..7)
                out[i] = (msb shr (7 - i) * 8 and 0xff).toByte()
            for (i in 8..15)
                out[i] = (lsb shr (15 - i) * 8 and 0xff).toByte()
            return out
        }
    }
}

fun uuid3(ns: Namespace, name: String): UUID = ns.uuid3(name)
fun uuid4(): UUID = UUID.randomUUID()
fun uuid5(ns: Namespace, name: String): UUID = ns.uuid5(name)