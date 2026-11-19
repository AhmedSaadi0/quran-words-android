package com.quranwords.domain.usecase

import com.quranwords.domain.repository.DbUpdateRepository
import javax.inject.Inject

class CheckDbUpdateUseCase @Inject constructor(
    private val repository: DbUpdateRepository
) {
    suspend operator fun invoke() = repository.checkForUpdate()
}
