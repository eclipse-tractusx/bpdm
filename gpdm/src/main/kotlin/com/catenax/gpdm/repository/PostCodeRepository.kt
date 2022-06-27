package com.catenax.gpdm.repository

import com.catenax.gpdm.entity.PostCode
import org.springframework.data.repository.PagingAndSortingRepository

interface PostCodeRepository : PagingAndSortingRepository<PostCode, Long> {
}