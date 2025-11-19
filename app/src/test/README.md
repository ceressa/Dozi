# Dozi Test Suite

## Overview

This directory contains unit tests for the Dozi application. We use modern Android testing libraries to ensure code quality and prevent regressions.

## Test Structure

```
app/src/test/
├── notifications/
│   └── ReminderSchedulerTest.kt    # Alarm scheduling logic tests
└── core/data/repository/
    ├── BadiRepositoryTest.kt       # Buddy system tests
    └── MedicineRepositoryTest.kt   # Medicine CRUD tests

app/src/androidTest/
└── ui/
    └── BadiFlowTest.kt             # UI integration tests
```

## Dependencies

- **JUnit 4**: Test framework
- **MockK**: Mocking library for Kotlin
- **Coroutines Test**: Testing suspend functions and flows
- **Turbine**: Flow testing library
- **Hilt Test**: Dependency injection in tests
- **Compose Test**: UI testing for Jetpack Compose
- **Espresso**: Android UI testing

## Running Tests

### Run All Unit Tests
```bash
./gradlew test
```

### Run Specific Test Class
```bash
./gradlew test --tests ReminderSchedulerTest
./gradlew test --tests BadiRepositoryTest
./gradlew test --tests MedicineRepositoryTest
```

### Run All Android Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### Run Specific UI Test
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bardino.dozi.ui.BadiFlowTest
```

### Run Tests with Coverage
```bash
./gradlew testDebugUnitTestCoverage
```

## Test Coverage

Current test coverage:

| Component | Tests | Coverage |
|-----------|-------|----------|
| ReminderScheduler | 10 | Critical path |
| BadiRepository | 12 | Data models |
| MedicineRepository | 13 | CRUD operations |
| UI Flows | 5 | Key journeys |
| **Total** | **40** | Foundation |

## Writing New Tests

### Unit Test Template

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MyComponentTest {

    @Before
    fun setup() {
        // Setup mocks and test data
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `descriptive test name using backticks`() {
        // Given: Setup test conditions

        // When: Perform action

        // Then: Assert expected outcome
    }
}
```

### UI Test Template

```kotlin
@RunWith(AndroidJUnit4::class)
class MyUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun myUITest() {
        // Set content
        composeTestRule.setContent {
            MyScreen()
        }

        // Find and interact with UI
        composeTestRule
            .onNodeWithText("Button")
            .performClick()

        // Assert UI state
        composeTestRule
            .onNodeWithText("Result")
            .assertIsDisplayed()
    }
}
```

## Priority Areas for Testing

1. **ReminderScheduler** ⚠️ CRITICAL
   - Alarm scheduling logic
   - Frequency calculations
   - Edge cases (empty lists, invalid times)

2. **BadiRepository** 🔴 HIGH
   - Request flow (send, accept, reject)
   - Permissions management
   - Data consistency

3. **MedicineRepository** 🔴 HIGH
   - CRUD operations
   - Data validation
   - Concurrent operations

4. **ViewModels** 🟡 MEDIUM
   - State management
   - Error handling
   - User actions

5. **UI Flows** 🟡 MEDIUM
   - Navigation
   - User journeys
   - Edge cases

## CI/CD Integration

Add to your CI pipeline:

```yaml
# GitHub Actions example
- name: Run Unit Tests
  run: ./gradlew test

- name: Upload Test Results
  uses: actions/upload-artifact@v3
  with:
    name: test-results
    path: app/build/test-results/
```

## Mocking Firebase

For tests that need Firebase:

```kotlin
// Mock Firestore
val mockFirestore = mockk<FirebaseFirestore>(relaxed = true)
val mockCollection = mockk<CollectionReference>(relaxed = true)
every { mockFirestore.collection(any()) } returns mockCollection

// Or use Firebase Emulator
// See: https://firebase.google.com/docs/emulator-suite
```

## Known Issues

1. **ReminderScheduler Tests**: Some tests require Android framework mocking. Actual alarm functionality should be tested on device/emulator.

2. **Firebase Tests**: Unit tests use mocks. For integration testing, use Firebase Emulator Suite.

3. **UI Tests**: Currently placeholders. Implement with proper navigation and data setup.

## Resources

- [Android Testing Guide](https://developer.android.com/training/testing)
- [MockK Documentation](https://mockk.io/)
- [Compose Testing](https://developer.android.com/jetpack/compose/testing)
- [Hilt Testing](https://developer.android.com/training/dependency-injection/hilt-testing)

## Contributing

When adding new features:
1. Write tests first (TDD approach recommended)
2. Aim for >70% code coverage on critical paths
3. Include edge cases and error scenarios
4. Document any test-specific setup requirements
