package com.catenax.gpdm.service

import com.catenax.gpdm.Application
import com.catenax.gpdm.dto.ChangelogEntryDto
import com.catenax.gpdm.entity.ChangelogType
import com.catenax.gpdm.repository.PartnerChangelogEntryRepository
import com.catenax.gpdm.util.PostgreSQLContextInitializer
import com.catenax.gpdm.util.TestHelpers
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [Application::class, TestHelpers::class])
@ActiveProfiles("test")
@ContextConfiguration(initializers = [PostgreSQLContextInitializer::class])
class PartnerChangelogServiceTest @Autowired constructor(
    val partnerChangelogService: PartnerChangelogService,
    val partnerChangelogEntryRepository: PartnerChangelogEntryRepository,
    val testHelpers: TestHelpers
) {

    @BeforeEach
    fun beforeEach() {
        testHelpers.truncateDbTables()
    }

    /**
     * Given no changelog entries in db
     * When changelog entry created via service
     * Then changelog entry saved to db
     */
    @Test
    fun createChangelogEntry() {
        partnerChangelogService.createChangelogEntry(ChangelogEntryDto("testBpn", ChangelogType.CREATE))

        val changelogEntries = partnerChangelogEntryRepository.findAll()
        assertThat(changelogEntries).hasSize(1)
        assertThat(changelogEntries[0].bpn).isEqualTo("testBpn")
        assertThat(changelogEntries[0].changelogType).isEqualTo(ChangelogType.CREATE)
    }

    /**
     * Given no changelog entries in db
     * When changelog multiple entries created via service in bulk
     * Then all changelog entries saved to db
     */
    @Test
    fun createChangelogEntries() {
        partnerChangelogService.createChangelogEntries(
            listOf(
                ChangelogEntryDto("testBpn", ChangelogType.CREATE),
                ChangelogEntryDto("testBpn", ChangelogType.UPDATE)
            )
        )

        val changelogEntries = partnerChangelogEntryRepository.findAll()
        assertThat(changelogEntries).hasSize(2)
        assertThat(changelogEntries).anyMatch { it.changelogType == ChangelogType.CREATE }
        assertThat(changelogEntries).anyMatch { it.changelogType == ChangelogType.UPDATE }
    }

    /**
     * Given some changelog entries in db
     * When changelog entries retrieved paginated
     * Then correct changelog page retrieved
     */
    @Test
    fun getChangelogEntriesStartingAfterId() {
        val startId = partnerChangelogService.createChangelogEntry(ChangelogEntryDto("testBpn1", ChangelogType.CREATE)).id
        partnerChangelogService.createChangelogEntry(ChangelogEntryDto("testBpn1", ChangelogType.UPDATE))
        partnerChangelogService.createChangelogEntry(ChangelogEntryDto("testBpn2", ChangelogType.CREATE))

        val changelogEntryPage = partnerChangelogService.getChangelogEntriesStartingAfterId(startId = startId, pageIndex = 1, pageSize = 1)
        assertThat(changelogEntryPage.totalElements).isEqualTo(2)
        assertThat(changelogEntryPage.content.size).isEqualTo(1)
        assertThat(changelogEntryPage.content[0].bpn).isEqualTo("testBpn2")
    }
}