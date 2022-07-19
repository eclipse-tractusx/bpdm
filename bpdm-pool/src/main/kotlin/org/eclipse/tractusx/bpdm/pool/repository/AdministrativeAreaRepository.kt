package org.eclipse.tractusx.bpdm.pool.repository

import org.eclipse.tractusx.bpdm.pool.entity.AdministrativeArea
import org.springframework.data.repository.PagingAndSortingRepository

interface AdministrativeAreaRepository : PagingAndSortingRepository<AdministrativeArea, Long> {
}