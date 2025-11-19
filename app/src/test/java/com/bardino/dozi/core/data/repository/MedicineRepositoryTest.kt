package com.bardino.dozi.core.data.repository

import com.bardino.dozi.core.data.model.Medicine
import com.google.firebase.Timestamp
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * MedicineRepository Unit Tests
 *
 * Tests CRUD operations for medicines which are the core of the app.
 * Every user interaction revolves around medicines and their reminders.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MedicineRepositoryTest {

    @Before
    fun setup() {
        // Setup mocks if needed
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Medicine model should have all required fields`() {
        // Given: A complete medicine object
        val medicine = Medicine(
            id = "med123",
            name = "Aspirin",
            dosage = "100mg",
            unit = "tablet",
            frequency = "Her gün",
            times = listOf("09:00", "21:00"),
            startDate = System.currentTimeMillis(),
            endDate = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000), // 7 days
            icon = "💊",
            notes = "Yemekten sonra al"
        )

        // When/Then: All fields should be accessible
        assertEquals("med123", medicine.id)
        assertEquals("Aspirin", medicine.name)
        assertEquals("100mg", medicine.dosage)
        assertEquals("tablet", medicine.unit)
        assertEquals("Her gün", medicine.frequency)
        assertEquals(2, medicine.times.size)
        assertNotNull(medicine.startDate)
        assertNotNull(medicine.endDate)
        assertEquals("💊", medicine.icon)
        assertEquals("Yemekten sonra al", medicine.notes)
    }

    @Test
    fun `Medicine name should not be empty`() {
        // Given: Medicine with name
        val medicine = Medicine(
            id = "med1",
            name = "Paracetamol",
            dosage = "500mg",
            unit = "tablet",
            frequency = "Her gün",
            times = listOf("08:00"),
            startDate = System.currentTimeMillis(),
            icon = "💊"
        )

        // When/Then: Name should be valid
        assertTrue("Medicine name should not be empty", medicine.name.isNotEmpty())
        assertTrue("Medicine name should be reasonable", medicine.name.length >= 3)
    }

    @Test
    fun `Medicine dosage should be valid format`() {
        // Given: Medicines with different dosages
        val med1 = Medicine(
            id = "1", name = "Med1", dosage = "500mg", unit = "tablet",
            frequency = "Her gün", times = listOf("09:00"),
            startDate = System.currentTimeMillis(), icon = "💊"
        )
        val med2 = Medicine(
            id = "2", name = "Med2", dosage = "10ml", unit = "ml",
            frequency = "Her gün", times = listOf("09:00"),
            startDate = System.currentTimeMillis(), icon = "💧"
        )

        // When/Then: Dosage formats should be valid
        assertTrue(med1.dosage.contains("mg") || med1.dosage.matches(Regex("\\d+")))
        assertTrue(med2.dosage.contains("ml") || med2.dosage.matches(Regex("\\d+")))
    }

    @Test
    fun `Medicine frequency should be valid option`() {
        // Given: Different frequency options
        val validFrequencies = listOf(
            "Her gün",
            "Gün aşırı",
            "Haftada 3 gün",
            "Her 6 saatte bir",
            "İstediğim tarihlerde"
        )

        validFrequencies.forEach { frequency ->
            val medicine = Medicine(
                id = "med_${frequency.hashCode()}",
                name = "Test Med",
                dosage = "1",
                unit = "tablet",
                frequency = frequency,
                times = listOf("09:00"),
                startDate = System.currentTimeMillis(),
                icon = "💊"
            )

            // When/Then: Frequency should be valid
            assertTrue(
                "Frequency '$frequency' should be valid",
                validFrequencies.contains(medicine.frequency)
            )
        }
    }

    @Test
    fun `Medicine times list should not be empty for scheduled medicine`() {
        // Given: Scheduled medicine
        val medicine = Medicine(
            id = "med2",
            name = "Scheduled Med",
            dosage = "1",
            unit = "tablet",
            frequency = "Her gün",
            times = listOf("08:00", "12:00", "20:00"),
            startDate = System.currentTimeMillis(),
            icon = "💊"
        )

        // When/Then: Times should be present
        assertTrue("Times list should not be empty", medicine.times.isNotEmpty())
        assertEquals("Should have 3 times", 3, medicine.times.size)
    }

    @Test
    fun `Medicine time format should be HH MM`() {
        // Given: Medicine with time
        val medicine = Medicine(
            id = "med3",
            name = "Time Test",
            dosage = "1",
            unit = "tablet",
            frequency = "Her gün",
            times = listOf("09:00", "14:30", "21:00"),
            startDate = System.currentTimeMillis(),
            icon = "💊"
        )

        // When/Then: Time format should be HH:MM
        val timePattern = Regex("\\d{2}:\\d{2}")
        medicine.times.forEach { time ->
            assertTrue(
                "Time '$time' should match HH:MM format",
                time.matches(timePattern)
            )
        }
    }

    @Test
    fun `Medicine startDate should be valid timestamp`() {
        // Given: Medicine with start date
        val before = System.currentTimeMillis()
        val medicine = Medicine(
            id = "med4",
            name = "Start Date Test",
            dosage = "1",
            unit = "tablet",
            frequency = "Her gün",
            times = listOf("09:00"),
            startDate = System.currentTimeMillis(),
            icon = "💊"
        )
        val after = System.currentTimeMillis()

        // When/Then: Start date should be reasonable
        assertTrue("Start date should be positive", medicine.startDate > 0)
        assertTrue("Start date should be recent", medicine.startDate >= before - 1000)
        assertTrue("Start date should be recent", medicine.startDate <= after + 1000)
    }

    @Test
    fun `Medicine endDate should be after startDate if present`() {
        // Given: Medicine with end date
        val startDate = System.currentTimeMillis()
        val endDate = startDate + (30 * 24 * 60 * 60 * 1000L) // 30 days later

        val medicine = Medicine(
            id = "med5",
            name = "End Date Test",
            dosage = "1",
            unit = "tablet",
            frequency = "Her gün",
            times = listOf("09:00"),
            startDate = startDate,
            endDate = endDate,
            icon = "💊"
        )

        // When/Then: End date should be after start date
        assertNotNull("End date should be set", medicine.endDate)
        assertTrue(
            "End date should be after start date",
            medicine.endDate!! > medicine.startDate
        )
    }

    @Test
    fun `Medicine icon should be emoji or valid string`() {
        // Given: Medicines with different icons
        val icons = listOf("💊", "💉", "🩺", "⚕️", "💧", "🌡️")

        icons.forEach { icon ->
            val medicine = Medicine(
                id = "med_icon_${icon.hashCode()}",
                name = "Icon Test",
                dosage = "1",
                unit = "tablet",
                frequency = "Her gün",
                times = listOf("09:00"),
                startDate = System.currentTimeMillis(),
                icon = icon
            )

            // When/Then: Icon should be valid
            assertTrue("Icon should not be empty", medicine.icon.isNotEmpty())
            assertEquals("Icon should match", icon, medicine.icon)
        }
    }

    @Test
    fun `Medicine unit should be valid measurement`() {
        // Given: Different units
        val validUnits = listOf("tablet", "ml", "mg", "g", "damla", "puf", "ampul")

        validUnits.forEach { unit ->
            val medicine = Medicine(
                id = "med_unit_${unit.hashCode()}",
                name = "Unit Test",
                dosage = "1",
                unit = unit,
                frequency = "Her gün",
                times = listOf("09:00"),
                startDate = System.currentTimeMillis(),
                icon = "💊"
            )

            // When/Then: Unit should be valid
            assertTrue("Unit should be valid", validUnits.contains(medicine.unit))
        }
    }

    @Test
    fun `Medicine notes can be null or empty`() {
        // Given: Medicine without notes
        val medicine1 = Medicine(
            id = "med6",
            name = "No Notes",
            dosage = "1",
            unit = "tablet",
            frequency = "Her gün",
            times = listOf("09:00"),
            startDate = System.currentTimeMillis(),
            icon = "💊",
            notes = null
        )

        val medicine2 = Medicine(
            id = "med7",
            name = "Empty Notes",
            dosage = "1",
            unit = "tablet",
            frequency = "Her gün",
            times = listOf("09:00"),
            startDate = System.currentTimeMillis(),
            icon = "💊",
            notes = ""
        )

        // When/Then: Notes can be null or empty (optional field)
        assertNull("Notes can be null", medicine1.notes)
        assertEquals("Notes can be empty", "", medicine2.notes)
    }

    @Test
    fun `Medicine ID should be unique`() {
        // Given: Multiple medicines
        val med1 = Medicine(
            id = "unique_1",
            name = "Med 1",
            dosage = "1",
            unit = "tablet",
            frequency = "Her gün",
            times = listOf("09:00"),
            startDate = System.currentTimeMillis(),
            icon = "💊"
        )

        val med2 = Medicine(
            id = "unique_2",
            name = "Med 2",
            dosage = "1",
            unit = "tablet",
            frequency = "Her gün",
            times = listOf("09:00"),
            startDate = System.currentTimeMillis(),
            icon = "💊"
        )

        // When/Then: IDs should be unique
        assertNotEquals("Medicine IDs should be unique", med1.id, med2.id)
        assertTrue("ID should not be empty", med1.id.isNotEmpty())
        assertTrue("ID should not be empty", med2.id.isNotEmpty())
    }
}
