package com.bardino.dozi.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.bardino.dozi.core.data.OnboardingPreferences
import com.bardino.dozi.core.ui.components.DoziBottomBar
import com.bardino.dozi.core.ui.screens.home.HomeScreen
import com.bardino.dozi.core.ui.screens.medicine.MedicineDetailScreen
import com.bardino.dozi.core.ui.screens.medicine.MedicineEditScreen
import com.bardino.dozi.core.ui.screens.medicine.MedicineListScreen
import com.bardino.dozi.core.ui.screens.medicine.MedicineLookupScreen
import com.bardino.dozi.core.ui.screens.premium.PremiumScreen
import com.bardino.dozi.core.ui.screens.profile.LocationsScreen
import com.bardino.dozi.core.ui.screens.profile.ProfileScreen
import com.bardino.dozi.core.ui.screens.reminder.AddReminderScreen
import com.bardino.dozi.core.ui.screens.reminder.ReminderListScreen
import com.bardino.dozi.core.ui.screens.settings.AboutScreen
import com.bardino.dozi.core.ui.screens.settings.AdvancedNotificationSettingsScreen
import com.bardino.dozi.core.ui.screens.settings.NotificationSettingsScreen
import com.bardino.dozi.core.ui.screens.settings.SettingsScreen
import com.bardino.dozi.core.ui.screens.badi.BadiListScreen
import com.bardino.dozi.core.ui.screens.badi.AddBadiScreen
import com.bardino.dozi.core.ui.screens.badi.BadiPermissionsScreen
import com.bardino.dozi.core.ui.screens.badi.BadiMedicationTrackingScreen
import com.bardino.dozi.core.ui.screens.stats.StatsScreen
import com.bardino.dozi.onboarding.screens.OnboardingHomeTourScreen
import com.bardino.dozi.onboarding.screens.OnboardingMedicineScreen
import com.bardino.dozi.onboarding.screens.OnboardingNameScreen
import com.bardino.dozi.onboarding.screens.OnboardingPremiumScreen
import com.bardino.dozi.onboarding.screens.OnboardingReminderScreen
import com.bardino.dozi.onboarding.screens.OnboardingWelcomeScreen
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavGraph(
    navController: NavHostController,
    onGoogleSignInClick: () -> Unit,
    startDestination: String = Screen.Home.route
) {
    // Bottom bar sadece ana ekranlarda gösterilecek (onboarding ekranlarında değil)
    val bottomBarRoutes = setOf(
        Screen.Home.route,
        Screen.MedicineList.route,
        Screen.ReminderList.route,
        Screen.Stats.route,
        Screen.BadiList.route,
        Screen.Profile.route
    )
    val context = LocalContext.current

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                DoziBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo(Screen.Home.route) {
                                    saveState = true // ✅ State'i koru
                                }
                                launchSingleTop = true
                                restoreState = true // ✅ State'i geri yükle
                            }
                        }
                    },
                    onLoginRequired = {
                        // Login olmayan kullanıcılar profil sayfasına yönlendir (login ekranı)
                        navController.navigate(Screen.Profile.route) {
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {

            // 🏠 Ana Sayfa
            composable(Screen.Home.route) {
                HomeScreen(
                    navController = navController,
                    contentPadding = padding,
                    onNavigateToMedicines = { navController.navigate(Screen.MedicineList.route) },
                    onNavigateToReminders = { navController.navigate(Screen.ReminderList.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                )
            }

            // 💊 İlaç Listesi
            composable(Screen.MedicineList.route) {
                MedicineListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.MedicineDetail.createRoute(id))
                    },
                    onNavigateToAddMedicine = {
                        navController.navigate(Screen.MedicineLookup.route)
                    },
                    onNavigateToAddReminder = { medicineId ->
                        navController.navigate("editReminder/$medicineId")
                    },
                    onNavigateToReminderDetail = { reminderId ->
                        navController.navigate("editReminder/$reminderId")
                    }
                )
            }


            // 💊 İlaç Detayı
            composable(
                route = Screen.MedicineDetail.route,
                arguments = listOf(navArgument("medicineId") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("medicineId").orEmpty()
                MedicineDetailScreen(
                    medicineId = id,
                    onNavigateBack = { navController.popBackStack() },
                    onEditMedicine = { medicineId ->
                        navController.navigate(Screen.MedicineEdit.createRoute(medicineId))
                    }
                )
            }

            // 🧾 İlaç Düzenleme
            composable(
                route = Screen.MedicineEdit.route,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: ""
                MedicineEditScreen(
                    medicineId = id,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToReminder = { savedMedicineId ->
                        // İlaç eklendikten sonra hatırlatma ekranına git
                        navController.navigate("editReminder/$savedMedicineId") {
                            popUpTo(Screen.MedicineList.route)
                        }
                    },
                    savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
                )
            }

            // ⏰ Hatırlatıcı Listesi
            composable(Screen.ReminderList.route) {
                ReminderListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAddReminder = { navController.navigate(Screen.AddReminder.route) },
                    onNavigateToEditReminder = { medicineId ->
                        navController.navigate(Screen.EditReminder.createRoute(medicineId))
                    }
                )
            }

            // ➕ Hatırlatıcı Ekle
            composable(Screen.AddReminder.route) {
                AddReminderScreen(
                    navController = navController,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ✏️ Hatırlatıcı Düzenle
            composable(
                route = Screen.EditReminder.route,
                arguments = listOf(navArgument("medicineId") { type = NavType.StringType })
            ) { backStackEntry ->
                val medicineId = backStackEntry.arguments?.getString("medicineId") ?: ""
                AddReminderScreen(
                    navController = navController,
                    onNavigateBack = { navController.popBackStack() },
                    medicineId = medicineId  // Edit mode aktif
                )
            }

            // 💊 İlaç Aksiyonu (Bildirimden)
            composable(
                route = Screen.MedicationAction.route,
                arguments = listOf(navArgument("time") { type = NavType.StringType })
            ) { backStackEntry ->
                val time = backStackEntry.arguments?.getString("time") ?: ""
                com.bardino.dozi.core.ui.screens.medication.MedicationActionScreen(
                    time = time,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 👤 Profil
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToLocations = { navController.navigate(Screen.Locations.route) },
                    onNavigateToPremium = { navController.navigate(Screen.Premium.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.NotificationSettings.route) },
                    onNavigateToAbout = { navController.navigate(Screen.About.route) },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onGoogleSignInClick = onGoogleSignInClick
                )
            }


            // 🌟 Premium Sayfası
            composable(Screen.Premium.route) {
                PremiumScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onPurchase = { planType ->
                        // burada satın alma işlemini veya yönlendirmeyi yapabilirsin
                    }
                )
            }

            // 🔍 İlaç Arama Ekranı
            composable(Screen.MedicineLookup.route) {
                MedicineLookupScreen(
                    navController = navController,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Onboarding akışı
            composable(Screen.OnboardingWelcome.route) {
                OnboardingWelcomeScreen(
                    onStartTour = { navController.navigate(Screen.OnboardingMedicine.route) },
                    onSkip = {
                        OnboardingPreferences.skipOnboarding(context)
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.OnboardingWelcome.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.OnboardingMedicine.route) {
                OnboardingMedicineScreen(
                    onNext = { navController.navigate(Screen.OnboardingName.route) },
                    onTryNow = {
                        // İlaç ekleme ekranına git
                        navController.navigate(Screen.MedicineLookup.route)
                    }
                )
            }

            composable(Screen.OnboardingName.route) {
                OnboardingNameScreen(
                    onNext = { name ->
                        // ✅ SharedPreferences'a kaydet
                        OnboardingPreferences.saveUserName(context, name)

                        // ✅ Firebase User profiline de kaydet
                        val userRepository = com.bardino.dozi.core.data.repository.UserRepository()
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            userRepository.updateUserField("name", name)
                            android.util.Log.d("OnboardingName", "✅ User name saved to Firebase: $name")
                        }

                        navController.navigate(Screen.OnboardingReminder.route)
                    }
                )
            }

            composable(Screen.OnboardingReminder.route) {
                OnboardingReminderScreen(
                    onNext = { navController.navigate(Screen.OnboardingPremium.route) },
                    onTryNow = {
                        // Hatırlatma ekleme ekranına git
                        navController.navigate(Screen.AddReminder.route)
                    }
                )
            }

            composable(Screen.OnboardingHomeTour.route) {
                OnboardingHomeTourScreen(
                    onNext = { navController.navigate(Screen.OnboardingPremium.route) }
                )
            }

            composable(Screen.OnboardingPremium.route) {
                OnboardingPremiumScreen(
                    onGoogleSignIn = {
                        // Onboarding tamamlandı olarak işaretle
                        OnboardingPreferences.setFirstTimeComplete(context)
                        // Google giriş yap
                        onGoogleSignInClick()
                        // Ana ekrana git
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.OnboardingWelcome.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Locations.route) {
                LocationsScreen(onNavigateBack = { navController.popBackStack() })
            }

            // ⚙️ Ayarlar
            composable(Screen.Settings.route) {
                SettingsScreen(onNavigateBack = { navController.popBackStack() })
            }

            // 🔔 Bildirim Ayarları
            composable(Screen.NotificationSettings.route) {
                NotificationSettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAdvanced = { navController.navigate(Screen.AdvancedNotificationSettings.route) }
                )
            }

            // 🔔 Gelişmiş Bildirim Ayarları
            composable(Screen.AdvancedNotificationSettings.route) {
                AdvancedNotificationSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ℹ️ Hakkında
            composable(Screen.About.route) {
                AboutScreen(onNavigateBack = { navController.popBackStack() })
            }

            // 📊 İstatistikler
            composable(Screen.Stats.route) {
                StatsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 👥 Badi Listesi
            composable(Screen.BadiList.route) {
                BadiListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAddBadi = { navController.navigate(Screen.AddBadi.route) },
                    onNavigateToBadiDetail = { badiId ->
                        navController.navigate(Screen.BadiMedicationTracking.createRoute(badiId))
                    },
                    onNavigateToBadiPermissions = { badiId ->
                        navController.navigate(Screen.BadiPermissions.createRoute(badiId))
                    }
                )
            }

            // ➕ Badi Ekle
            composable(Screen.AddBadi.route) {
                AddBadiScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ⚙️ Badi İzinleri
            composable(
                route = Screen.BadiPermissions.route,
                arguments = listOf(navArgument("badiId") { type = NavType.StringType })
            ) { backStackEntry ->
                val badiId = backStackEntry.arguments?.getString("badiId") ?: ""
                BadiPermissionsScreen(
                    badiId = badiId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 📊 Badi İlaç Takibi
            composable(
                route = Screen.BadiMedicationTracking.route,
                arguments = listOf(navArgument("badiId") { type = NavType.StringType })
            ) { backStackEntry ->
                val badiId = backStackEntry.arguments?.getString("badiId") ?: ""
                BadiMedicationTrackingScreen(
                    badiId = badiId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

        }
    }
}