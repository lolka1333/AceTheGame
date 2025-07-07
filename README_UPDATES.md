# Google Play Billing Library 7+ Compatibility Updates

## Обзор / Overview

Этот проект был обновлен для поддержки последних версий Google Play Billing Library (7 и 8), которые станут обязательными с 31 августа 2025 года.

This project has been updated to support the latest Google Play Billing Library versions (7 and 8), which become mandatory from August 31, 2025.

## 🚀 Основные улучшения / Key Improvements

### ✅ Полная поддержка современных версий Billing Library
- Google Play Billing Library 7.x (полная поддержка)
- Google Play Billing Library 8.x (готов к использованию)
- Обратная совместимость с версиями 5.x и 6.x

### ✅ Новая модель подписок
- **Base Plans**: Несколько вариантов оплаты для одной подписки
- **Offers**: Гибкие предложения с различными ценами и условиями
- **Regional Pricing**: Разные цены для разных регионов
- **Installment Subscriptions**: Поддержка рассрочки (ограниченные регионы)

### ✅ Улучшенное обнаружение паттернов
- Обнаружение как старых, так и новых паттернов API
- Поддержка обфусцированного кода
- Многоуровневое обнаружение для лучшей совместимости

### ✅ Современная обработка платежей
- Поддержка отложенных покупок (Pending Purchases)
- Новые коды ошибок (NETWORK_ERROR)
- Альтернативные способы оплаты

## 📁 Обновленные файлы / Updated Files

### Конфигурация сборки / Build Configuration
- `billing-hack/build.gradle` - Обновлены версии SDK и зависимости
- `billing-hack/app/build.gradle` - Современные настройки Android
- `Modder/modder/build.gradle` - Совместимость с новыми инструментами

### AIDL интерфейс / AIDL Interface
- `billing-hack/app/src/main/aidl/.../IInAppBillingService.aidl` - Новые методы API

### Реализация сервиса / Service Implementation
- `billing-hack/app/src/main/java/org/billinghack/BillingService.kt` - Полная поддержка новых API

### Утилиты для патчинга / Patching Utilities
- `Modder/modder/src/main/java/modder/InAppPurchaseUtil.kt` - Улучшенное обнаружение и патчинг

## 🛠 Использование / Usage

### Сборка проекта / Building the Project

```bash
# Сборка billing-hack APK
cd billing-hack
./gradlew assembleRelease

# Сборка Modder (требует Android Studio)
cd Modder
./gradlew build
```

### Тестирование / Testing

```bash
# Запуск тестового скрипта
./test_billing_patch.sh your_app.apk

# Это протестирует:
# - Сборку billing-hack APK
# - Обнаружение паттернов в целевом APK
# - Установку на устройство (если подключено)
```

### Использование Modder / Using Modder

```kotlin
// Стандартный процесс патчинга
val apktool = Apktool(apkFile)
val success = InAppPurchaseUtil.patchApk(apktool, redirectToLuckyPatcher = false)

// Проверка успешности патчинга
if (success) {
    val verified = InAppPurchaseUtil.verifyPatch(apktool)
    println("Patch verified: $verified")
}
```

## 🔧 Настройка и требования / Setup and Requirements

### Системные требования / System Requirements
- Android Studio Hedgehog (2023.1.1) или новее
- JDK 17 или новее
- Android SDK 34
- Kotlin 2.0.21+

### Зависимости / Dependencies
Все зависимости обновлены автоматически при сборке проекта.

## 📊 Совместимость / Compatibility Matrix

| Billing Library Version | Status | Notes |
|-------------------------|--------|-------|
| 5.x | ✅ Поддерживается | Сохранены старые паттерны |
| 6.x | ✅ Поддерживается | Улучшенная совместимость |
| 7.x | ✅ Полная поддержка | Основная целевая версия |
| 8.x | ✅ Готов | Будущая совместимость |

## 🆕 Новые функции / New Features

### 1. Улучшенная модель подписок / Enhanced Subscription Model
```json
{
  "subscriptionOfferDetails": [
    {
      "basePlanId": "monthly-base",
      "offerId": null,
      "offerToken": "monthly_offer_token",
      "pricingPhases": [...]
    }
  ]
}
```

### 2. Поддержка новых методов API / New API Methods Support
- `queryProductDetails()` - Новая модель продуктов
- `isFeatureSupported()` - Проверка доступности функций
- `getBillingConfig()` - Конфигурация биллинга

### 3. Расширенное обнаружение / Enhanced Detection
```kotlin
// Новые паттерны для обнаружения
val NEW_BILLING_PATTERNS = listOf(
    "queryProductDetailsAsync",
    "ProductDetails",
    "BillingClient",
    "PurchasesUpdatedListener"
)
```

## 🔍 Диагностика и отладка / Diagnostics and Debugging

### Проверка паттернов / Pattern Detection
```kotlin
// Проверка наличия паттернов биллинга
val hasPatterns = InAppPurchaseUtil.containsBillingPatterns(smaliCode)
val hasNewPatterns = InAppPurchaseUtil.containsNewBillingPatterns(smaliCode)
```

### Логирование / Logging
```kotlin
// Включение подробного логирования
Log.d("BillingHack", "Detailed debugging information")
```

### Верификация / Verification
```bash
# Использование тестового скрипта для анализа APK
./test_billing_patch.sh app.apk
```

## ⚠️ Важные замечания / Important Notes

### Обязательные изменения / Mandatory Changes
С 31 августа 2025 года все новые приложения должны использовать Billing Library 7+.

### Региональные ограничения / Regional Limitations
- Installment subscriptions: только Бразилия, Франция, Италия, Испания
- Alternative billing: региональные ограничения

### Миграция / Migration
Существующие приложения продолжат работать, но обновления потребуют новую версию библиотеки.

## 📞 Поддержка / Support

### Решение проблем / Troubleshooting

1. **Паттерн не найден / Pattern Not Found**
   ```bash
   # Включите подробное логирование
   export DEBUG=1
   ./test_billing_patch.sh app.apk
   ```

2. **Ошибка сборки / Build Error**
   ```bash
   # Обновите инструменты сборки
   ./gradlew wrapper --gradle-version=8.7
   ```

3. **Патч не применился / Patch Failed**
   ```kotlin
   // Проверьте обфусцированный код
   val patterns = InAppPurchaseUtil.NEW_BILLING_PATTERNS
   ```

### Полезные команды / Useful Commands
```bash
# Проверка версий
./gradlew --version

# Очистка проекта
./gradlew clean

# Сборка с отладочной информацией
./gradlew assembleRelease --info
```

## 🔮 Планы развития / Future Roadmap

- ✅ Поддержка Billing Library 9 (готовность)
- 🔄 Дополнительные функции Google
- 🔄 Оптимизация производительности
- 🔄 Расширенная диагностика

## 📄 Лицензия / License

Проект сохраняет оригинальную лицензию. Все изменения совместимы с существующими условиями использования.

---

## 🎯 Заключение / Conclusion

Этот апгрейд обеспечивает полную поддержку современных версий Google Play Billing Library, сохраняя при этом обратную совместимость. Улучшенное обнаружение паттернов, поддержка современных API и улучшенная обработка ошибок обеспечивают надежную работу с широким спектром приложений.

This upgrade provides comprehensive support for modern Google Play Billing Library versions while maintaining backward compatibility. Enhanced pattern detection, modern API support, and improved error handling ensure reliable operation across a wide range of applications.

**Готов к использованию с Google Play Billing Library 7+ ✅**  
**Ready for Google Play Billing Library 7+ ✅**