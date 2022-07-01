package org.eclipse.tractusx.bpdm.pool.repository

import org.eclipse.tractusx.bpdm.pool.entity.Name
import org.springframework.data.repository.PagingAndSortingRepository

interface NameRepository : PagingAndSortingRepository<Name, Long> {
}