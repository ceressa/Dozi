import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

/**
 * 🤝 Buddy isteği oluşturulduğunda tetiklenir
 * Alıcıya push notification gönderir
 */
export const onBuddyRequestCreated = functions.firestore
  .document("buddy_requests/{requestId}")
  .onCreate(async (snap, context) => {
    const request = snap.data();
    const requestId = context.params.requestId;

    console.log(`📬 Yeni buddy isteği: ${requestId}`);
    console.log(`From: ${request.fromUserId} → To: ${request.toUserId}`);

    try {
      // Alıcının FCM token'ını al
      const toUserDoc = await db.collection("users").doc(request.toUserId).get();
      const toUser = toUserDoc.data();

      if (!toUser) {
        console.warn("⚠️ Alıcı kullanıcı bulunamadı:", request.toUserId);
        return null;
      }

      if (!toUser.fcmToken) {
        console.warn("⚠️ Alıcının FCM token'ı yok:", request.toUserId);
        return null;
      }

      // Push notification gönder
      const message = {
        token: toUser.fcmToken,
        notification: {
          title: "🤝 Yeni Buddy İsteği",
          body: `${request.fromUserName} seni buddy olarak eklemek istiyor!`,
        },
        data: {
          type: "buddy_request",
          requestId: requestId,
          fromUserId: request.fromUserId,
          fromUserName: request.fromUserName,
        },
        android: {
          priority: "high" as const,
          notification: {
            sound: "default",
            channelId: "dozi_med_channel",
            clickAction: "FLUTTER_NOTIFICATION_CLICK",
          },
        },
      };

      await messaging.send(message);
      console.log("✅ Buddy isteği bildirimi gönderildi");

      // Firestore'a notification kaydı oluştur
      await db.collection("notifications").add({
        userId: request.toUserId,
        type: "BUDDY_REQUEST",
        title: message.notification.title,
        body: message.notification.body,
        data: message.data,
        isRead: false,
        isSent: true,
        sentAt: admin.firestore.FieldValue.serverTimestamp(),
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        priority: "HIGH",
      });

      console.log("✅ Notification kaydı oluşturuldu");
    } catch (error) {
      console.error("❌ Bildirim gönderme hatası:", error);
    }

    return null;
  });

/**
 * ✅ İlaç alındığında tetiklenir
 * Buddy'lere bildirim gönderir
 */
export const onMedicationTaken = functions.firestore
  .document("medication_logs/{logId}")
  .onCreate(async (snap, context) => {
    const log = snap.data();
    const logId = context.params.logId;

    // Sadece "TAKEN" durumunda bildirim gönder
    if (log.status !== "TAKEN") {
      console.log(`⏭️ Log durumu TAKEN değil (${log.status}), atlıyorum`);
      return null;
    }

    console.log(`💊 İlaç alındı: ${log.medicineName} - ${log.userId}`);

    try {
      const userId = log.userId;

      // Kullanıcının bilgilerini al
      const userDoc = await db.collection("users").doc(userId).get();
      const user = userDoc.data();

      if (!user) {
        console.warn("⚠️ Kullanıcı bulunamadı:", userId);
        return null;
      }

      // Kullanıcının aktif buddy'lerini al
      const buddiesSnapshot = await db
        .collection("buddies")
        .where("userId", "==", userId)
        .where("status", "==", "ACTIVE")
        .get();

      console.log(`👥 ${buddiesSnapshot.size} aktif buddy bulundu`);

      if (buddiesSnapshot.empty) {
        console.log("ℹ️ Aktif buddy yok, bildirim gönderilmeyecek");
        return null;
      }

      const promises: Promise<any>[] = [];

      for (const buddyDoc of buddiesSnapshot.docs) {
        const buddy = buddyDoc.data();

        // Buddy'nin bildirim tercihini kontrol et
        if (!buddy.notificationPreferences?.onMedicationTaken) {
          console.log(`⏭️ Buddy bildirim almak istemiyor: ${buddy.buddyUserId}`);
          continue;
        }

        // Buddy'nin FCM token'ını al
        const buddyUserDoc = await db.collection("users").doc(buddy.buddyUserId).get();
        const buddyUser = buddyUserDoc.data();

        if (!buddyUser || !buddyUser.fcmToken) {
          console.warn(`⚠️ Buddy kullanıcı/token yok: ${buddy.buddyUserId}`);
          continue;
        }

        // Push notification gönder
        const message = {
          token: buddyUser.fcmToken,
          notification: {
            title: "✅ İlaç Alındı",
            body: `${user.name || "Buddy'niz"} ${log.medicineName} ilacını aldı`,
          },
          data: {
            type: "medication_taken",
            userId: userId,
            logId: logId,
            medicineName: log.medicineName,
            buddyName: user.name || "",
          },
          android: {
            priority: "default" as const,
            notification: {
              sound: "default",
              channelId: "dozi_med_channel",
            },
          },
        };

        promises.push(
          messaging
            .send(message)
            .then(async () => {
              console.log(`✅ Bildirim gönderildi: ${buddy.buddyUserId}`);

              // Notification kaydı oluştur
              await db.collection("notifications").add({
                userId: buddy.buddyUserId,
                type: "MEDICATION_TAKEN",
                title: message.notification.title,
                body: message.notification.body,
                data: message.data,
                isRead: false,
                isSent: true,
                sentAt: admin.firestore.FieldValue.serverTimestamp(),
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
                priority: "NORMAL",
              });
            })
            .catch((error) => {
              console.error(`❌ Bildirim hatası (${buddy.buddyUserId}):`, error);
            })
        );
      }

      await Promise.all(promises);
      console.log(`✅ ${promises.length} buddy'ye bildirim gönderildi`);
    } catch (error) {
      console.error("❌ onMedicationTaken hatası:", error);
    }

    return null;
  });

/**
 * 💊 İlaç hatırlatması buddy'lere gönder
 * Android app'ten callable function olarak çağrılır
 */
export const sendMedicationReminderToBuddies = functions.https.onCall(
  async (data, context) => {
    // Auth kontrolü
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Kullanıcı giriş yapmamış"
      );
    }

    const { medicineId, medicineName, dosage, time } = data;
    const userId = context.auth.uid;

    console.log(`💊 İlaç hatırlatması: ${medicineName} - ${userId}`);

    try {
      // Kullanıcının bilgilerini al
      const userDoc = await db.collection("users").doc(userId).get();
      const user = userDoc.data();

      if (!user) {
        throw new functions.https.HttpsError("not-found", "Kullanıcı bulunamadı");
      }

      // Kullanıcının aktif buddy'lerini al
      const buddiesSnapshot = await db
        .collection("buddies")
        .where("userId", "==", userId)
        .where("status", "==", "ACTIVE")
        .get();

      console.log(`👥 ${buddiesSnapshot.size} aktif buddy bulundu`);

      if (buddiesSnapshot.empty) {
        return { success: true, sentCount: 0, message: "Aktif buddy yok" };
      }

      const promises: Promise<any>[] = [];

      for (const buddyDoc of buddiesSnapshot.docs) {
        const buddy = buddyDoc.data();

        // Buddy'nin bildirim tercihini kontrol et
        if (!buddy.notificationPreferences?.onMedicationTime) {
          console.log(`⏭️ Buddy bildirim almak istemiyor: ${buddy.buddyUserId}`);
          continue;
        }

        // Buddy'nin FCM token'ını al
        const buddyUserDoc = await db.collection("users").doc(buddy.buddyUserId).get();
        const buddyUser = buddyUserDoc.data();

        if (!buddyUser || !buddyUser.fcmToken) {
          console.warn(`⚠️ Buddy kullanıcı/token yok: ${buddy.buddyUserId}`);
          continue;
        }

        // Push notification gönder
        const message = {
          token: buddyUser.fcmToken,
          notification: {
            title: "💊 Buddy İlaç Hatırlatması",
            body: `${user.name || "Buddy'niz"} - ${medicineName} ${dosage} alma zamanı (${time})`,
          },
          data: {
            type: "buddy_medication_reminder",
            userId: userId,
            medicineId: medicineId || "",
            medicineName: medicineName,
            time: time,
            buddyName: user.name || "",
          },
          android: {
            priority: "high" as const,
            notification: {
              sound: "default",
              channelId: "dozi_med_channel",
            },
          },
        };

        promises.push(
          messaging
            .send(message)
            .then(async () => {
              console.log(`✅ Hatırlatma gönderildi: ${buddy.buddyUserId}`);

              // Notification kaydı oluştur
              await db.collection("notifications").add({
                userId: buddy.buddyUserId,
                type: "BUDDY_MEDICATION_ALERT",
                title: message.notification.title,
                body: message.notification.body,
                data: message.data,
                isRead: false,
                isSent: true,
                sentAt: admin.firestore.FieldValue.serverTimestamp(),
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
                priority: "HIGH",
              });
            })
            .catch((error) => {
              console.error(`❌ Hatırlatma hatası (${buddy.buddyUserId}):`, error);
            })
        );
      }

      await Promise.all(promises);
      const sentCount = promises.length;

      console.log(`✅ ${sentCount} buddy'ye hatırlatma gönderildi`);

      return {
        success: true,
        sentCount: sentCount,
        message: `${sentCount} buddy'ye bildirim gönderildi`,
      };
    } catch (error) {
      console.error("❌ sendMedicationReminderToBuddies hatası:", error);
      throw new functions.https.HttpsError("internal", "Bildirim gönderilemedi");
    }
  }
);

/**
 * ⚠️ İlaç kaçırma kontrolü
 * Her 15 dakikada bir çalışır
 */
export const checkMissedMedications = functions.pubsub
  .schedule("every 15 minutes")
  .onRun(async (context) => {
    console.log("🔍 Kaçırılan ilaçlar kontrol ediliyor...");

    try {
      const now = admin.firestore.Timestamp.now();
      const fifteenMinutesAgo = new Date(now.toMillis() - 15 * 60 * 1000);

      // Son 15 dakikada kaçırılan ilaçları bul
      const missedLogsSnapshot = await db
        .collection("medication_logs")
        .where("status", "==", "MISSED")
        .where("scheduledTime", ">", fifteenMinutesAgo)
        .get();

      console.log(`📋 ${missedLogsSnapshot.size} kaçırılan ilaç bulundu`);

      if (missedLogsSnapshot.empty) {
        console.log("✅ Kaçırılan ilaç yok");
        return null;
      }

      const promises: Promise<any>[] = [];

      for (const logDoc of missedLogsSnapshot.docs) {
        const log = logDoc.data();
        const userId = log.userId;

        // Kullanıcının bilgilerini al
        const userDoc = await db.collection("users").doc(userId).get();
        const user = userDoc.data();

        if (!user) continue;

        // Kullanıcının buddy'lerini al
        const buddiesSnapshot = await db
          .collection("buddies")
          .where("userId", "==", userId)
          .where("status", "==", "ACTIVE")
          .get();

        for (const buddyDoc of buddiesSnapshot.docs) {
          const buddy = buddyDoc.data();

          // Buddy'nin bildirim tercihini kontrol et
          if (!buddy.notificationPreferences?.onMedicationMissed) {
            continue;
          }

          // Buddy'nin FCM token'ını al
          const buddyUserDoc = await db.collection("users").doc(buddy.buddyUserId).get();
          const buddyUser = buddyUserDoc.data();

          if (!buddyUser || !buddyUser.fcmToken) continue;

          // Push notification gönder
          const message = {
            token: buddyUser.fcmToken,
            notification: {
              title: "⚠️ İlaç Kaçırıldı",
              body: `${user.name || "Buddy'niz"} ${log.medicineName} ilacını kaçırdı`,
            },
            data: {
              type: "medication_missed",
              userId: userId,
              logId: logDoc.id,
              medicineName: log.medicineName,
              buddyName: user.name || "",
            },
            android: {
              priority: "high" as const,
              notification: {
                sound: "default",
                channelId: "dozi_med_channel",
              },
            },
          };

          promises.push(
            messaging.send(message).then(async () => {
              await db.collection("notifications").add({
                userId: buddy.buddyUserId,
                type: "MEDICATION_MISSED",
                title: message.notification.title,
                body: message.notification.body,
                data: message.data,
                isRead: false,
                isSent: true,
                sentAt: admin.firestore.FieldValue.serverTimestamp(),
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
                priority: "HIGH",
              });
            })
          );
        }
      }

      await Promise.all(promises);
      console.log(`✅ ${promises.length} kaçırma bildirimi gönderildi`);
    } catch (error) {
      console.error("❌ checkMissedMedications hatası:", error);
    }

    return null;
  });
