package org.eclipse.tractusx.bpdm.pool.repository

import org.eclipse.tractusx.bpdm.pool.entity.PostCode
import org.springframework.data.repository.PagingAndSortingRepository

interface PostCodeRepository : PagingAndSortingRepository<PostCode, Long> {
}