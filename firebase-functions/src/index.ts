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
 * 🤝 Badi isteği oluşturulduğunda tetiklenir
 * Alıcıya push notification gönderir
 */
export const onBadiRequestCreated = onDocumentCreated(
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

    console.log(`📬 Yeni badi isteği: ${requestId}`);
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
          type: "badi_request",
          requestId: requestId,
          fromUserId: request.fromUserId,
          fromUserName: request.fromUserName,
        },
        android: {
          priority: "high" as const,
        },
      };

      await messaging.send(message);
      console.log("✅ Badi isteği bildirimi gönderildi");

      // Firestore'a notification kaydı oluştur
      await db.collection("notifications").add({
        userId: request.toUserId,
        type: "BADI_REQUEST",
        title: "🤝 Yeni Badi İsteği",
        body: `${fromName} seni badi olarak eklemek istiyor!`,
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
 * Badilere bildirim gönderir
 * Badi tarafından işaretlendiyse kullanıcıya da bildirim gönderir
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
      const markedByBuddyId = log.markedByBuddyId;

      // Kullanıcının bilgilerini al
      const userDoc = await db.collection("users").doc(userId).get();
      const user = userDoc.data();

      if (!user) {
        console.warn("⚠️ Kullanıcı bulunamadı:", userId);
        return;
      }

      const promises: Promise<unknown>[] = [];

      // 🔔 Badi tarafından işaretlendiyse kullanıcıya bildirim gönder
      if (markedByBuddyId) {
        const buddyUserDoc = await db.collection("users").doc(markedByBuddyId).get();
        const buddyUser = buddyUserDoc.data();
        const buddyName = buddyUser?.name || "Badin";

        if (user.fcmToken) {
          const userMessage = {
            token: user.fcmToken,
            notification: {
              title: "✅ İlaç Badi Tarafından İşaretlendi",
              body: `${buddyName} senin için ${log.medicineName} ilacını aldı olarak işaretledi`,
            },
            data: {
              type: "medication_marked_by_buddy",
              medicineId: log.medicineId || "",
              medicineName: log.medicineName,
              markedByBuddyId: markedByBuddyId,
              markedByBuddyName: buddyName,
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
            messaging.send(userMessage).then(async () => {
              console.log(`✅ Kullanıcıya badi işaretleme bildirimi gönderildi`);
              await db.collection("notifications").add({
                userId: userId,
                type: "MEDICATION_MARKED_BY_BUDDY",
                title: userMessage.notification.title,
                body: userMessage.notification.body,
                data: userMessage.data,
                isRead: false,
                isSent: true,
                sentAt: admin.firestore.FieldValue.serverTimestamp(),
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
                priority: "NORMAL",
              });
            })
          );
        }
      }

      // Kullanıcının aktif badilerini al
      const badisSnapshot = await db
        .collection("buddies")
        .where("userId", "==", userId)
        .where("status", "==", "ACTIVE")
        .get();

      console.log(`👥 ${badisSnapshot.size} aktif badi bulundu`);

      if (badisSnapshot.empty && promises.length === 0) {
        console.log("ℹ️ Aktif badi yok, bildirim gönderilmeyecek");
        return;
      }

      for (const badiDoc of badisSnapshot.docs) {
        const badi = badiDoc.data();
        const badiUid = badi.buddyUserId;

        // Badi kendisi işaretlediyse ona bildirim gönderme
        if (badiUid === markedByBuddyId) {
          console.log(`⏭️ Badi kendisi işaretledi, atlıyorum: ${badiUid}`);
          continue;
        }

        // 🔐 Rol bazlı izin kontrolü - canReceiveNotifications
        const permissions = badi.permissions;
        if (permissions && !permissions.canReceiveNotifications) {
          console.log(`⏭️ Badi bildirim alma izni yok: ${badiUid}`);
          continue;
        }

        // Badinin bildirim tercihini kontrol et
        const prefs = badi.notificationPreferences;
        if (!prefs?.onMedicationTaken) {
          console.log(`⏭️ Badi bildirim almak istemiyor: ${badiUid}`);
          continue;
        }

        // Badinin FCM token'ını al
        const badiUserDoc = await db.collection("users").doc(badiUid).get();
        const badiUser = badiUserDoc.data();

        if (!badiUser || !badiUser.fcmToken) {
          console.warn(`⚠️ Badi kullanıcı/token yok: ${badiUid}`);
          continue;
        }

        // Push notification gönder - kullanıcı adı ön planda
        const userName = user.name || "Badin";
        const medName = log.medicineName;
        const notifBody = `${medName} ilacını aldı`;
        const message = {
          token: badiUser.fcmToken,
          notification: {
            title: `✅ ${userName} İlacını Aldı`,
            body: notifBody,
          },
          data: {
            type: "medication_taken",
            userId: userId,
            userName: userName,
            logId: logId,
            medicineName: log.medicineName,
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
              console.log(`✅ Bildirim gönderildi: ${badiUid}`);

              // Notification kaydı oluştur
              await db.collection("notifications").add({
                userId: badi.buddyUserId,
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
              console.error(`❌ Bildirim hatası (${badiUid}):`, error);
            })
        );
      }

      await Promise.all(promises);
      const count = promises.length;
      console.log(`✅ ${count} bildirim gönderildi`);
    } catch (error) {
      console.error("❌ onMedicationTaken hatası:", error);
    }
  }
);

/**
 * 💊 İlaç hatırlatması badilere gönder
 * Android app'ten callable function olarak çağrılır
 * Rol bazlı izin kontrolü yapar
 */
export const sendMedicationReminderToBadis = onCall(
  {region: REGION},
  async (request) => {
    // Auth kontrolü
    if (!request.auth) {
      throw new HttpsError(
        "unauthenticated",
        "Kullanıcı giriş yapmamış"
      );
    }

    const {medicineId, medicineName, dosage, time, escalationLevel} = request.data;
    const userId = request.auth.uid;

    console.log(`💊 İlaç hatırlatması: ${medicineName} - ${userId} (Eskalasyon: ${escalationLevel || 0})`);

    try {
      // Kullanıcının bilgilerini al
      const userDoc = await db.collection("users").doc(userId).get();
      const user = userDoc.data();

      if (!user) {
        throw new HttpsError("not-found", "Kullanıcı bulunamadı");
      }

      // Kullanıcının aktif badilerini al
      const badisSnapshot = await db
        .collection("buddies")
        .where("userId", "==", userId)
        .where("status", "==", "ACTIVE")
        .get();

      console.log(`👥 ${badisSnapshot.size} aktif badi bulundu`);

      if (badisSnapshot.empty) {
        return {success: true, sentCount: 0, message: "Aktif badi yok"};
      }

      const promises: Promise<unknown>[] = [];

      for (const badiDoc of badisSnapshot.docs) {
        const badi = badiDoc.data();
        const badiUid = badi.buddyUserId;

        // 🔐 Rol bazlı izin kontrolü - canReceiveNotifications
        const permissions = badi.permissions;
        if (permissions && !permissions.canReceiveNotifications) {
          console.log(`⏭️ Badi bildirim alma izni yok: ${badiUid}`);
          continue;
        }

        // Badinin bildirim tercihini kontrol et
        const prefs = badi.notificationPreferences;
        if (!prefs?.onMedicationTime) {
          console.log(`⏭️ Badi bildirim almak istemiyor: ${badiUid}`);
          continue;
        }

        // Badinin FCM token'ını al
        const badiUserDoc = await db.collection("users").doc(badiUid).get();
        const badiUser = badiUserDoc.data();

        if (!badiUser || !badiUser.fcmToken) {
          console.warn(`⚠️ Badi kullanıcı/token yok: ${badiUid}`);
          continue;
        }

        // Push notification gönder - kullanıcı adı ön planda
        const userName = user.name || "Badin";
        const body = `${medicineName} ${dosage} - Saat ${time}`;

        // Eskalasyon seviyesine göre başlık ayarla
        let title = `💊 ${userName} - İlaç Hatırlatması`;
        let priority: "high" | "normal" = "high";

        if (escalationLevel && escalationLevel >= 2) {
          title = `⚠️ ${userName} - İlaç Hatırlatması (${escalationLevel}. uyarı)`;
        } else if (escalationLevel && escalationLevel >= 3) {
          title = `🚨 ${userName} - ACİL İlaç Hatırlatması`;
        }

        const message = {
          token: badiUser.fcmToken,
          notification: {
            title: title,
            body: body,
          },
          data: {
            type: "badi_medication_reminder",
            userId: userId,
            userName: userName,
            medicineId: medicineId || "",
            medicineName: medicineName,
            time: time,
            escalationLevel: String(escalationLevel || 0),
          },
          android: {
            priority: priority,
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
              console.log(`✅ Hatırlatma gönderildi: ${badiUid}`);

              // Notification kaydı oluştur
              await db.collection("notifications").add({
                userId: badi.buddyUserId,
                type: "BADI_MEDICATION_ALERT",
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
              console.error(`❌ Hatırlatma hatası (${badiUid}):`, error);
            })
        );
      }

      await Promise.all(promises);
      const sentCount = promises.length;

      console.log(`✅ ${sentCount} badiye hatırlatma gönderildi`);

      return {
        success: true,
        sentCount: sentCount,
        message: `${sentCount} badiye bildirim gönderildi`,
      };
    } catch (error) {
      console.error("❌ sendMedicationReminderToBadis hatası:", error);
      throw new HttpsError("internal", "Bildirim gönderilemedi");
    }
  }
);

/**
 * 🎯 Badiye "dürtme" göndermek için callable function
 * Kullanıcı badisine hatırlatma göndermek istediğinde çağrılır
 */
export const sendBadiNudge = onCall(
  {region: REGION},
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Kullanıcı giriş yapmamış");
    }

    const {buddyUserId, message} = request.data;
    const fromUserId = request.auth.uid;

    console.log(`👋 Badi nudge: ${fromUserId} → ${buddyUserId}`);

    try {
      // Gönderen kullanıcının bilgilerini al
      const fromUserDoc = await db.collection("users").doc(fromUserId).get();
      const fromUser = fromUserDoc.data();

      if (!fromUser) {
        throw new HttpsError("not-found", "Kullanıcı bulunamadı");
      }

      // Alıcının FCM token'ını al
      const badiUserDoc = await db.collection("users").doc(buddyUserId).get();
      const badiUser = badiUserDoc.data();

      if (!badiUser || !badiUser.fcmToken) {
        throw new HttpsError("not-found", "Badi bulunamadı veya FCM token yok");
      }

      // Push notification gönder
      const fromName = fromUser.name || "Badin";
      const notificationMessage = {
        token: badiUser.fcmToken,
        notification: {
          title: `💌 ${fromName} seni düşünüyor`,
          body: message || "Bugün ilacını almayı unutma!",
        },
        data: {
          type: "badi_nudge",
          fromUserId: fromUserId,
          fromUserName: fromName,
          message: message || "",
        },
        android: {
          priority: "high" as const,
          notification: {
            sound: "default",
            channelId: "dozi_med_channel",
          },
        },
      };

      await messaging.send(notificationMessage);
      console.log("✅ Badi nudge gönderildi");

      // Notification kaydı oluştur
      await db.collection("notifications").add({
        userId: buddyUserId,
        type: "BADI_NUDGE",
        title: notificationMessage.notification.title,
        body: notificationMessage.notification.body,
        data: notificationMessage.data,
        isRead: false,
        isSent: true,
        sentAt: admin.firestore.FieldValue.serverTimestamp(),
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        priority: "HIGH",
      });

      return {success: true, message: "Badine hatırlatma gönderildi"};
    } catch (error) {
      console.error("❌ sendBadiNudge hatası:", error);
      throw new HttpsError("internal", "Bildirim gönderilemedi");
    }
  }
);

/**
 * ⚠️ İlaç kaçırma kontrolü
 * Her 15 dakikada bir çalışır
 * Rol bazlı izin kontrolü yapar
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

        // Kullanıcının badilerini al
        const badisSnapshot = await db
          .collection("buddies")
          .where("userId", "==", userId)
          .where("status", "==", "ACTIVE")
          .get();

        for (const badiDoc of badisSnapshot.docs) {
          const badi = badiDoc.data();
          const badiUid = badi.buddyUserId;

          // 🔐 Rol bazlı izin kontrolü - canReceiveNotifications
          const permissions = badi.permissions;
          if (permissions && !permissions.canReceiveNotifications) {
            console.log(`⏭️ Badi bildirim alma izni yok: ${badiUid}`);
            continue;
          }

          // Badinin bildirim tercihini kontrol et
          if (!badi.notificationPreferences?.onMedicationMissed) {
            continue;
          }

          // Badinin FCM token'ını al
          const badiUserDoc = await db
            .collection("users")
            .doc(badiUid)
            .get();
          const badiUser = badiUserDoc.data();

          if (!badiUser || !badiUser.fcmToken) continue;

          // Push notification gönder - kullanıcı adı ön planda
          const userName = user.name || "Badin";
          const message = {
            token: badiUser.fcmToken,
            notification: {
              title: `⚠️ ${userName} - İlaç Kaçırıldı`,
              body: `${log.medicineName} ilacını almadı`,
            },
            data: {
              type: "medication_missed",
              userId: userId,
              userName: userName,
              logId: logDoc.id,
              medicineName: log.medicineName,
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
              console.log(`✅ Kaçırma bildirimi gönderildi: ${badiUid}`);
              await db.collection("notifications").add({
                userId: badi.buddyUserId,
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

/**
 * 📦 Stok uyarısı badilere gönder
 * Android app'ten callable function olarak çağrılır
 * Rol bazlı izin kontrolü yapar
 */
export const sendStockWarningToBadis = onCall(
  {region: REGION},
  async (request) => {
    // Auth kontrolü
    if (!request.auth) {
      throw new HttpsError(
        "unauthenticated",
        "Kullanıcı giriş yapmamış"
      );
    }

    const {medicineId, medicineName, stockCount, daysRemaining, stockPercentage, warningLevel} = request.data;
    const userId = request.auth.uid;

    console.log(`📦 Stok uyarısı: ${medicineName} - ${userId} (Stok: ${stockCount}, Kalan gün: ${daysRemaining})`);

    try {
      // Kullanıcının bilgilerini al
      const userDoc = await db.collection("users").doc(userId).get();
      const user = userDoc.data();

      if (!user) {
        throw new HttpsError("not-found", "Kullanıcı bulunamadı");
      }

      // Kullanıcının aktif badilerini al
      const badisSnapshot = await db
        .collection("buddies")
        .where("userId", "==", userId)
        .where("status", "==", "ACTIVE")
        .get();

      console.log(`👥 ${badisSnapshot.size} aktif badi bulundu`);

      if (badisSnapshot.empty) {
        return {success: true, sentCount: 0, message: "Aktif badi yok"};
      }

      const promises: Promise<unknown>[] = [];

      for (const badiDoc of badisSnapshot.docs) {
        const badi = badiDoc.data();
        const badiUid = badi.buddyUserId;

        // 🔐 Rol bazlı izin kontrolü - canReceiveNotifications
        const permissions = badi.permissions;
        if (permissions && !permissions.canReceiveNotifications) {
          console.log(`⏭️ Badi bildirim alma izni yok: ${badiUid}`);
          continue;
        }

        // Badinin FCM token'ını al
        const badiUserDoc = await db.collection("users").doc(badiUid).get();
        const badiUser = badiUserDoc.data();

        if (!badiUser || !badiUser.fcmToken) {
          console.warn(`⚠️ Badi kullanıcı/token yok: ${badiUid}`);
          continue;
        }

        // Push notification gönder - kullanıcı adı ön planda
        const userName = user.name || "Badin";

        // Uyarı seviyesine göre mesaj oluştur
        let title: string;
        let body: string;
        let priority: "high" | "normal" = "normal";

        if (warningLevel === "empty") {
          title = `🚨 ${userName} - İlaç Stoğu Bitti`;
          body = `${medicineName} stoğu tamamen bitti! Yenilemeyi hatırlat.`;
          priority = "high";
        } else if (warningLevel === "critical") {
          const percentMsg = stockPercentage ? ` (%${stockPercentage})` : "";
          title = `🔴 ${userName} - Kritik Stok Uyarısı`;
          body = `${medicineName} stoğu kritik${percentMsg} - ${daysRemaining} gün kaldı`;
          priority = "high";
        } else {
          const percentMsg = stockPercentage ? ` (%${stockPercentage})` : "";
          title = `🟡 ${userName} - Stok Uyarısı`;
          body = `${medicineName} stoğu azaldı${percentMsg} - ${daysRemaining} gün kaldı`;
        }

        const message = {
          token: badiUser.fcmToken,
          notification: {
            title: title,
            body: body,
          },
          data: {
            type: "stock_warning",
            userId: userId,
            userName: userName,
            medicineId: medicineId || "",
            medicineName: medicineName,
            stockCount: String(stockCount || 0),
            daysRemaining: String(daysRemaining || 0),
            warningLevel: warningLevel || "low",
          },
          android: {
            priority: priority,
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
              console.log(`✅ Stok uyarısı gönderildi: ${badiUid}`);

              // Notification kaydı oluştur
              await db.collection("notifications").add({
                userId: badi.buddyUserId,
                type: "STOCK_WARNING",
                title: message.notification.title,
                body: message.notification.body,
                data: message.data,
                isRead: false,
                isSent: true,
                sentAt: admin.firestore.FieldValue.serverTimestamp(),
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
                priority: priority === "high" ? "HIGH" : "NORMAL",
              });
            })
            .catch((error) => {
              console.error(`❌ Stok uyarısı hatası (${badiUid}):`, error);
            })
        );
      }

      await Promise.all(promises);
      const sentCount = promises.length;

      console.log(`✅ ${sentCount} badiye stok uyarısı gönderildi`);

      return {
        success: true,
        sentCount: sentCount,
        message: `${sentCount} badiye stok uyarısı gönderildi`,
      };
    } catch (error) {
      console.error("❌ sendStockWarningToBadis hatası:", error);
      throw new HttpsError("internal", "Stok uyarısı gönderilemedi");
    }
  }
);
