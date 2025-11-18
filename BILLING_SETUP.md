# 💳 Google Play Billing Setup Guide

## Overview

The Dozi app now has Google Play Billing integration for premium subscriptions. This guide explains how to complete the setup.

## ✅ What's Already Done

1. ✅ **Billing Library Added** - `billing-ktx:6.2.1` in build.gradle.kts
2. ✅ **BillingManager Created** - Handles all billing operations
3. ✅ **PremiumManager Ready** - Feature gates configured
4. ✅ **PremiumScreen UI** - User-facing subscription selection
5. ✅ **Purchase Flow** - Complete purchase handling logic

## 🔧 Required Setup Steps

### 1. Google Play Console Configuration

#### A. Create App in Play Console
1. Go to [Google Play Console](https://play.google.com/console)
2. Create/select your app
3. Complete store listing requirements

#### B. Configure Subscription Products

Navigate to **Monetize > Products > Subscriptions** and create:

| Product ID | Name | Price | Billing Period |
|-----------|------|-------|----------------|
| `dozi_weekly_premium` | Dozi Ekstra - Haftalık | 49₺ | 1 week |
| `dozi_monthly_premium` | Dozi Ekstra - Aylık | 149₺ | 1 month |
| `dozi_yearly_family_premium` | Dozi Ekstra - Yıllık Aile | 999₺ | 1 year |

**Important**: Product IDs must match exactly with those in `BillingManager.kt`.

#### C. Set Up Subscription Offers
For each product:
1. Create a base plan
2. Set the billing period and price
3. Add trial period (optional, e.g., 7 days free)
4. Configure grace period (recommended: 3 days)

### 2. Test With License Testers

Before production:

1. **Add Test Accounts**
   - Go to **Setup > License testing**
   - Add Gmail accounts for testing
   - These accounts can make test purchases without charges

2. **Use Internal Testing Track**
   - Upload signed APK to Internal testing
   - Add testers to the track
   - Share testing link with team

3. **Test Purchase Flow**
   ```
   Test scenarios:
   - Successful purchase
   - Purchase cancellation
   - Already owned product
   - Subscription renewal
   - Purchase restoration
   ```

### 3. Server-Side Receipt Verification

⚠️ **CRITICAL**: Currently, the app acknowledges purchases locally. For production, you MUST verify purchases on your server.

#### Recommended Flow:
```
1. User makes purchase
2. App sends purchase token to your server
3. Server verifies with Google Play Developer API
4. Server updates user premium status in Firestore
5. App refreshes premium status
```

#### Firebase Function Example:

```javascript
// functions/index.js
const {google} = require('googleapis');
const androidpublisher = google.androidpublisher('v3');

exports.verifyPurchase = functions.https.onCall(async (data, context) => {
  const {packageName, productId, purchaseToken} = data;

  // Authenticate with Google Play API
  const auth = new google.auth.GoogleAuth({
    keyFile: 'service-account-key.json',
    scopes: ['https://www.googleapis.com/auth/androidpublisher'],
  });

  const authClient = await auth.getClient();
  google.options({auth: authClient});

  // Verify subscription
  const result = await androidpublisher.purchases.subscriptions.get({
    packageName,
    subscriptionId: productId,
    token: purchaseToken,
  });

  // Update Firestore
  if (result.data.paymentState === 1) { // Payment received
    await admin.firestore()
      .collection('users')
      .doc(context.auth.uid)
      .update({
        isPremium: true,
        planType: productId,
        premiumStartDate: new Date(parseInt(result.data.startTimeMillis)),
        premiumEndDate: new Date(parseInt(result.data.expiryTimeMillis))
      });

    return {success: true, verified: true};
  }

  return {success: false, verified: false};
});
```

### 4. Initialize BillingManager

The BillingManager needs to be initialized when the app starts.

#### Option A: In MainActivity (Quick Setup)
```kotlin
// MainActivity.kt
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        billingManager.initialize()
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.endConnection()
    }
}
```

#### Option B: In Application Class (Recommended)
```kotlin
// DoziApplication.kt
@HiltAndroidApp
class DoziApplication : Application() {
    @Inject
    lateinit var billingManager: BillingManager

    override fun onCreate() {
        super.onCreate()
        billingManager.initialize()
    }
}
```

### 5. Connect PremiumScreen to BillingManager

Create a `PremiumViewModel`:

```kotlin
// PremiumViewModel.kt
@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val billingManager: BillingManager
) : ViewModel() {

    val products = billingManager.products
    val purchaseResult = billingManager.purchaseResult
    val isReady = billingManager.isReady

    fun purchaseProduct(activity: Activity, planType: String) {
        val productId = when (planType) {
            "weekly" -> BillingManager.PRODUCT_WEEKLY
            "monthly" -> BillingManager.PRODUCT_MONTHLY
            "yearly" -> BillingManager.PRODUCT_YEARLY_FAMILY
            else -> return
        }

        billingManager.launchPurchaseFlow(activity, productId)
    }
}
```

Update `PremiumScreen.kt`:
```kotlin
@Composable
fun PremiumScreen(
    onNavigateBack: () -> Unit,
    viewModel: PremiumViewModel = hiltViewModel()
) {
    val activity = LocalContext.current as Activity
    val purchaseResult by viewModel.purchaseResult.collectAsState()

    // Handle purchase result
    LaunchedEffect(purchaseResult) {
        when (purchaseResult) {
            is PurchaseResult.Success -> {
                // Show success message
                // Navigate back or refresh
            }
            is PurchaseResult.Error -> {
                // Show error message
            }
            // Handle other cases
        }
    }

    // ... existing UI code

    Button(onClick = {
        viewModel.purchaseProduct(activity, selectedPlan)
    }) {
        Text("Satın Al")
    }
}
```

## 🧪 Testing Checklist

- [ ] Products load successfully in PremiumScreen
- [ ] Purchase flow launches without errors
- [ ] Successful purchase acknowledges correctly
- [ ] User premium status updates in Firestore
- [ ] Cancelled purchases don't charge
- [ ] Already owned subscriptions detected
- [ ] Subscription renewal works automatically
- [ ] Grace period handles payment failures
- [ ] Receipt verification works on server

## 📝 Important Notes

### Subscription States
- **Purchased**: Payment successful, subscription active
- **Pending**: Payment processing (e.g., bank transfer)
- **Cancelled**: User cancelled but may still have access until expiry
- **Expired**: Subscription ended, no longer active
- **Grace Period**: Payment failed, but subscription still active temporarily

### Best Practices
1. **Always verify receipts server-side** - Never trust client-only verification
2. **Handle edge cases** - Network errors, cancelled purchases, refunds
3. **Provide restore purchases** - Let users restore on new devices
4. **Clear pricing** - Show price, billing period, and trial info clearly
5. **Privacy compliance** - Handle subscription data per GDPR/privacy laws

### Common Issues
- **Products not loading**: Check product IDs match exactly
- **Purchase fails immediately**: App not published (even to internal track)
- **Test purchases not working**: Ensure test account added to license testers
- **Already owned error**: Previous test purchase, wait 24h or use different account

## 🔗 Resources

- [Google Play Billing Library Docs](https://developer.android.com/google/play/billing)
- [Google Play Developer API](https://developers.google.com/android-publisher)
- [Play Console](https://play.google.com/console)
- [Billing Testing Guide](https://developer.android.com/google/play/billing/test)
- [Server-Side Verification](https://developer.android.com/google/play/billing/security)

## 🚀 Deployment Checklist

Before going live:
- [ ] All products configured in Play Console
- [ ] Server-side verification implemented
- [ ] Tested with license testers
- [ ] Tested on internal track
- [ ] Subscription management tested
- [ ] Error handling verified
- [ ] Privacy policy updated
- [ ] Terms of service updated
- [ ] App review guidelines checked
- [ ] Production signing key configured

## 🆘 Support

If you encounter issues:
1. Check Play Console status messages
2. Review logcat for billing errors
3. Verify product IDs match exactly
4. Ensure app version has been uploaded to Play Console
5. Contact Google Play support for payment issues
