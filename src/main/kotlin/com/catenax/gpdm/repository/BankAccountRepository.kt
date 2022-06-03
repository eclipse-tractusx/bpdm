package com.catenax.gpdm.repository

import com.catenax.gpdm.entity.BankAccount
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository

interface BankAccountRepository : PagingAndSortingRepository<BankAccount, Long> {

    @Query("SELECT DISTINCT b FROM BankAccount b LEFT JOIN FETCH b.trustScores WHERE b IN :bankAccounts")
    fun joinTrustScores(bankAccounts: Set<BankAccount>): Set<BankAccount>

}