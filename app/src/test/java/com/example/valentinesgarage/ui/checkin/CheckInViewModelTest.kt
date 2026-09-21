package com.example.valentinesgarage.ui.checkin

import com.example.valentinesgarage.domain.repository.GarageRepository
import io.mockk.mockk
import io.mockk.coVerify
import org.junit.jupiter.api.Test

class CheckInViewModelTest {
    private val repository = mockk<GarageRepository>(relaxed = true)
    private val viewModel = CheckInViewModel(repository)

    @Test fun `invalid intake never reaches the repository`() {
        viewModel.submit(mockk(relaxed = true))
        coVerify(exactly = 0) { repository.checkIn(any(), any()) }
    }
}
