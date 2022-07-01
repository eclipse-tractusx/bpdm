package org.eclipse.tractusx.bpdm.pool.repository

import org.eclipse.tractusx.bpdm.pool.entity.ConfigurationEntry
import org.springframework.data.repository.CrudRepository

interface ConfigurationEntryRepository : CrudRepository<ConfigurationEntry, Long> {

    fun findByKey(key: String): ConfigurationEntry?
}