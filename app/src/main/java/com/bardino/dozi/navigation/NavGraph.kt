package com.bardino.dozi.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.bardino.dozi.core.ui.components.DoziBottomBar
import com.bardino.dozi.core.ui.screens.home.HomeScreen
import com.bardino.dozi.core.ui.screens.medicine.CustomMedicineAddScreen
import com.bardino.dozi.core.ui.screens.medicine.MedicineDetailScreen
import com.bardino.dozi.core.ui.screens.medicine.MedicineEditScreen
import com.bardino.dozi.core.ui.screens.medicine.MedicineListScreen
import com.bardino.dozi.core.ui.screens.medicine.MedicineLookupScreen
import com.bardino.dozi.core.ui.screens.premium.PremiumScreen
import com.bardino.dozi.core.ui.screens.premium.PremiumIntroScreen
import com.bardino.dozi.core.ui.screens.login.LoginScreen
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
import com.bardino.dozi.core.ui.screens.family.FamilyManagementScreen
import com.bardino.dozi.core.ui.screens.support.SupportScreen
import com.bardino.dozi.core.ui.screens.onboarding.WelcomeScreen
import com.bardino.dozi.core.ui.screens.onboarding.FirstMedicineWizardScreen

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
                    },
                    onPremiumRequired = {
                        // Premium olmayan kullanıcılar PremiumIntro'ya yönlendir
                        navController.navigate(Screen.PremiumIntro.route) {
                            launchSingleTop = true
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
                        navController.navigate(Screen.EditReminder.createRoute(medicineId))
                    },
                    onNavigateToReminderDetail = { reminderId ->
                        navController.navigate(Screen.EditReminder.createRoute(reminderId))
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
                        navController.navigate(Screen.EditReminder.createRoute(savedMedicineId)) {
                            popUpTo(Screen.MedicineList.route)
                        }
                    },
                    onNavigateToPremium = {
                        navController.navigate(Screen.PremiumIntro.route)
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
                    onNavigateToFamilyManagement = { navController.navigate(Screen.FamilyManagement.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.NotificationSettings.route) },
                    onNavigateToAbout = { navController.navigate(Screen.About.route) },
                    onNavigateToSupport = { navController.navigate(Screen.Support.route) },
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

            // 👨‍👩‍👧‍👦 Aile Yönetimi
            composable(Screen.FamilyManagement.route) {
                FamilyManagementScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 🔍 İlaç Arama Ekranı
            composable(Screen.MedicineLookup.route) {
                MedicineLookupScreen(
                    navController = navController,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ✨ Özel İlaç Ekleme Ekranı
            composable(Screen.CustomMedicineAdd.route) {
                CustomMedicineAddScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToReminder = { savedMedicineId ->
                        navController.navigate(Screen.EditReminder.createRoute(savedMedicineId)) {
                            popUpTo(Screen.MedicineList.route)
                        }
                    }
                )
            }

            // 🎉 Welcome Ekranı (Onboarding)
            composable(Screen.Welcome.route) {
                WelcomeScreen(
                    onContinue = {
                        navController.navigate(Screen.FirstMedicineWizard.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    },
                    onSkip = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                )
            }

            // 💊 İlk İlaç Wizard (Onboarding)
            composable(Screen.FirstMedicineWizard.route) {
                FirstMedicineWizardScreen(
                    onAddMedicine = {
                        navController.navigate(Screen.MedicineLookup.route) {
                            popUpTo(Screen.FirstMedicineWizard.route) { inclusive = true }
                        }
                    },
                    onSkip = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.FirstMedicineWizard.route) { inclusive = true }
                        }
                    }
                )
            }

            // 🔐 Login Ekranı
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        // İlk kez giriş yapan kullanıcıları PremiumIntro'ya yönlendir
                        navController.navigate(Screen.PremiumIntro.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onSkip = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            // 🎁 Premium Intro Ekranı (ilk kez giriş yapan kullanıcılar için)
            composable(Screen.PremiumIntro.route) {
                PremiumIntroScreen(
                    onContinue = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.PremiumIntro.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Locations.route) {
                LocationsScreen(onNavigateBack = { navController.popBackStack() })
            }

            // ⚙️ Ayarlar
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
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
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSupport = { navController.navigate(Screen.Support.route) }
                )
            }

            // 🆘 Destek / SSS
            composable(Screen.Support.route) {
                SupportScreen(onNavigateBack = { navController.popBackStack() })
            }

            // 📊 İstatistikler
            composable(Screen.Stats.route) {
                StatsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    contentPadding = padding
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
                    onNavigateBack = { navController.popBackStack() },
                    navController = navController
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