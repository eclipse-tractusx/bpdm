package com.catenax.gpdm.repository.entity

import com.catenax.gpdm.entity.BankAccount
import org.springframework.data.repository.PagingAndSortingRepository

interface BankAccountRepository : PagingAndSortingRepository<BankAccount, Long> {
}