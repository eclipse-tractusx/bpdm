package com.catenax.gpdm.repository

import com.catenax.gpdm.entity.Relation
import org.springframework.data.repository.PagingAndSortingRepository

interface RelationRepository : PagingAndSortingRepository<Relation, Long> {
}