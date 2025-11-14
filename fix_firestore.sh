#!/bin/bash

# BuddyRepository, MedicationLogRepository ve NotificationRepository'deki
# tüm .where() çağrılarını yeni API'ye çevir

find app/src/main/java/com/bardino/dozi/core/data/repository/ -name "*.kt" -type f -exec sed -i \
  -e 's/\.where("\([^"]*\)", "==", \([^)]*\))/\.whereEqualTo("\1", \2)/g' \
  -e 's/\.where("\([^"]*\)", ">=", \([^)]*\))/\.whereGreaterThanOrEqualTo("\1", \2)/g' \
  -e 's/\.where("\([^"]*\)", "<=", \([^)]*\))/\.whereLessThanOrEqualTo("\1", \2)/g' \
  -e 's/\.where("\([^"]*\)", ">", \([^)]*\))/\.whereGreaterThan("\1", \2)/g' \
  -e 's/\.where("\([^"]*\)", "<", \([^)]*\))/\.whereLessThan("\1", \2)/g' \
  {} \;

echo "✅ Firestore API güncellendi!"
