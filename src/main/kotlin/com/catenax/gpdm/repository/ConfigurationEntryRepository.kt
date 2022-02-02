package com.catenax.gpdm.repository

import com.catenax.gpdm.entity.ConfigurationEntry
import org.springframework.data.repository.CrudRepository

interface ConfigurationEntryRepository : CrudRepository<ConfigurationEntry, Long> {

    fun findByKey(key: String): ConfigurationEntry?
}