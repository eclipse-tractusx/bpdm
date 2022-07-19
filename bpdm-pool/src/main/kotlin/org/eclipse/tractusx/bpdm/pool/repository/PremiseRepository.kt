package org.eclipse.tractusx.bpdm.pool.repository

import org.eclipse.tractusx.bpdm.pool.entity.Premise
import org.springframework.data.repository.PagingAndSortingRepository

interface PremiseRepository : PagingAndSortingRepository<Premise, Long> {
}