import * as admin from "firebase-admin";
import {onDocumentCreated} from "firebase-functions/v2/firestore";
import {onCall, HttpsError} from "firebase-functions/v2/https";
import {onSchedule} from "firebase-functions/v2/scheduler";

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

// 🇪🇺 EU Region Configuration (Frankfurt)
const REGION = "europe-west3";

/**
 * 🤝 Buddy isteği oluşturulduğunda tetiklenir
 * Alıcıya push notification gönderir
 */
export const onBuddyRequestCreated = onDocumentCreated(
  {
    document: "buddy_requests/{requestId}",
    region: REGION,
  },
  async (event) => {
    const request = event.data?.data();
    if (!request) {
      console.warn("⚠️ Request data yok");
      return;
    }

    const requestId = event.params.requestId;

    console.log(`📬 Yeni buddy isteği: ${requestId}`);
    const from = request.fromUserId;
    const to = request.toUserId;
    console.log(`From: ${from} → To: ${to}`);

    try {
      // Alıcının FCM token'ını al
      const toUserDoc = await db.collection("users").doc(request.toUserId).get();
      const toUser = toUserDoc.data();

      if (!toUser) {
        console.warn("⚠️ Alıcı kullanıcı bulunamadı:", request.toUserId);
        return;
      }

      if (!toUser.fcmToken) {
        console.warn("⚠️ Alıcının FCM token'ı yok:", request.toUserId);
        return;
      }

      // Push notification gönder (data-only message)
      const fromName = request.fromUserName;
      const message = {
        token: toUser.fcmToken,
        data: {
          type: "buddy_request",
          requestId: requestId,
          fromUserId: request.fromUserId,
          fromUserName: request.fromUserName,
        },
        android: {
          priority: "high" as const,
        },
      };

      await messaging.send(message);
      console.log("✅ Buddy isteği bildirimi gönderildi");

      // Firestore'a notification kaydı oluştur
      await db.collection("notifications").add({
        userId: request.toUserId,
        type: "BUDDY_REQUEST",
        title: "🤝 Yeni Buddy İsteği",
        body: `${fromName} seni buddy olarak eklemek istiyor!`,
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
  }
);

/**
 * ✅ İlaç alındığında tetiklenir
 * Buddy'lere bildirim gönderir
 */
export const onMedicationTaken = onDocumentCreated(
  {
    document: "medication_logs/{logId}",
    region: REGION,
  },
  async (event) => {
    const log = event.data?.data();
    if (!log) {
      console.warn("⚠️ Log data yok");
      return;
    }

    const logId = event.params.logId;

    // Sadece "TAKEN" durumunda bildirim gönder
    if (log.status !== "TAKEN") {
      console.log(`⏭️ Log durumu TAKEN değil (${log.status}), atlıyorum`);
      return;
    }

    console.log(`💊 İlaç alındı: ${log.medicineName} - ${log.userId}`);

    try {
      const userId = log.userId;

      // Kullanıcının bilgilerini al
      const userDoc = await db.collection("users").doc(userId).get();
      const user = userDoc.data();

      if (!user) {
        console.warn("⚠️ Kullanıcı bulunamadı:", userId);
        return;
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
        return;
      }

      const promises: Promise<unknown>[] = [];

      for (const buddyDoc of buddiesSnapshot.docs) {
        const buddy = buddyDoc.data();

        // Buddy'nin bildirim tercihini kontrol et
        const prefs = buddy.notificationPreferences;
        if (!prefs?.onMedicationTaken) {
          const buddyUid = buddy.buddyUserId;
          console.log(`⏭️ Buddy bildirim almak istemiyor: ${buddyUid}`);
          continue;
        }

        // Buddy'nin FCM token'ını al
        const buddyUid = buddy.buddyUserId;
        const buddyUserDoc = await db.collection("users").doc(buddyUid).get();
        const buddyUser = buddyUserDoc.data();

        if (!buddyUser || !buddyUser.fcmToken) {
          console.warn(`⚠️ Buddy kullanıcı/token yok: ${buddyUid}`);
          continue;
        }

        // Push notification gönder
        const userName = user.name || "Buddy'niz";
        const medName = log.medicineName;
        const notifBody = `${userName} ${medName} ilacını aldı`;
        const message = {
          token: buddyUser.fcmToken,
          notification: {
            title: "✅ İlaç Alındı",
            body: notifBody,
          },
          data: {
            type: "medication_taken",
            userId: userId,
            logId: logId,
            medicineName: log.medicineName,
            buddyName: user.name || "",
          },
          android: {
            priority: "normal" as const,
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
              const uid = buddy.buddyUserId;
              console.log(`✅ Bildirim gönderildi: ${uid}`);

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
              const uid = buddy.buddyUserId;
              console.error(`❌ Bildirim hatası (${uid}):`, error);
            })
        );
      }

      await Promise.all(promises);
      const count = promises.length;
      console.log(`✅ ${count} buddy'ye bildirim gönderildi`);
    } catch (error) {
      console.error("❌ onMedicationTaken hatası:", error);
    }
  }
);

/**
 * 💊 İlaç hatırlatması buddy'lere gönder
 * Android app'ten callable function olarak çağrılır
 */
export const sendMedicationReminderToBuddies = onCall(
  {region: REGION},
  async (request) => {
    // Auth kontrolü
    if (!request.auth) {
      throw new HttpsError(
        "unauthenticated",
        "Kullanıcı giriş yapmamış"
      );
    }

    const {medicineId, medicineName, dosage, time} = request.data;
    const userId = request.auth.uid;

    console.log(`💊 İlaç hatırlatması: ${medicineName} - ${userId}`);

    try {
      // Kullanıcının bilgilerini al
      const userDoc = await db.collection("users").doc(userId).get();
      const user = userDoc.data();

      if (!user) {
        throw new HttpsError("not-found", "Kullanıcı bulunamadı");
      }

      // Kullanıcının aktif buddy'lerini al
      const buddiesSnapshot = await db
        .collection("buddies")
        .where("userId", "==", userId)
        .where("status", "==", "ACTIVE")
        .get();

      console.log(`👥 ${buddiesSnapshot.size} aktif buddy bulundu`);

      if (buddiesSnapshot.empty) {
        return {success: true, sentCount: 0, message: "Aktif buddy yok"};
      }

      const promises: Promise<unknown>[] = [];

      for (const buddyDoc of buddiesSnapshot.docs) {
        const buddy = buddyDoc.data();

        // Buddy'nin bildirim tercihini kontrol et
        const prefs = buddy.notificationPreferences;
        if (!prefs?.onMedicationTime) {
          const buddyUid = buddy.buddyUserId;
          console.log(`⏭️ Buddy bildirim almak istemiyor: ${buddyUid}`);
          continue;
        }

        // Buddy'nin FCM token'ını al
        const buddyUid = buddy.buddyUserId;
        const buddyUserDoc = await db.collection("users").doc(buddyUid).get();
        const buddyUser = buddyUserDoc.data();

        if (!buddyUser || !buddyUser.fcmToken) {
          console.warn(`⚠️ Buddy kullanıcı/token yok: ${buddyUid}`);
          continue;
        }

        // Push notification gönder
        const userName = user.name || "Buddy'niz";
        const body = `${userName} - ${medicineName} ${dosage} (${time})`;
        const message = {
          token: buddyUser.fcmToken,
          notification: {
            title: "💊 Buddy İlaç Hatırlatması",
            body: body,
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
              const uid = buddy.buddyUserId;
              console.log(`✅ Hatırlatma gönderildi: ${uid}`);

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
              const uid = buddy.buddyUserId;
              console.error(`❌ Hatırlatma hatası (${uid}):`, error);
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
      throw new HttpsError("internal", "Bildirim gönderilemedi");
    }
  }
);

/**
 * ⚠️ İlaç kaçırma kontrolü
 * Her 15 dakikada bir çalışır
 */
export const checkMissedMedications = onSchedule(
  {
    schedule: "every 15 minutes",
    region: REGION,
  },
  async () => {
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
        return;
      }

      const promises: Promise<unknown>[] = [];

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
          const buddyUserDoc = await db
            .collection("users")
            .doc(buddy.buddyUserId)
            .get();
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
      const count = promises.length;
      console.log(`✅ ${count} kaçırma bildirimi gönderildi`);
    } catch (error) {
      console.error("❌ checkMissedMedications hatası:", error);
    }
  }
);
