package org.eclipse.tractusx.bpdm.pool.repository

import org.eclipse.tractusx.bpdm.pool.entity.Relation
import org.springframework.data.repository.PagingAndSortingRepository

interface RelationRepository : PagingAndSortingRepository<Relation, Long> {
}