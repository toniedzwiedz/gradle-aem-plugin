package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.ZoneId
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

abstract class AbstractInstance(
    @Transient
    @JsonIgnore
    protected val aem: AemExtension
) : Instance {

    override var properties = mutableMapOf<String, String?>()

    override val systemProperties: Map<String, String>
        get() = sync.status.systemProperties

    override fun property(key: String, value: String?) {
        properties[key] = value
    }

    final override fun property(key: String): String? = properties[key] ?: systemProperties[key]

    override val zoneId: ZoneId
        get() = property("user.timezone")?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()

    override val sync: InstanceSync
        get() = InstanceSync(aem, this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Instance

        return EqualsBuilder()
                .append(name, other.name)
                .append(httpUrl, other.httpUrl)
                .isEquals
    }

    override fun hashCode(): Int {
        return HashCodeBuilder()
                .append(name)
                .append(httpUrl)
                .toHashCode()
    }
}