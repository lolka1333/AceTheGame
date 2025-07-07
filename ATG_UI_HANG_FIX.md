# ATG Fix: UI Hang During Process Attach

## Проблема
При попытке подключения к процессу (attach to pid) в ATG возникали следующие проблемы:

1. **Зависание приложения**: После нажатия "OK" в диалоге подтверждения приложение зависало
2. **Постоянные сообщения "ATG предоставлены root права"**: Приложение "застревало" в проверке root прав
3. **Сбой приложения**: В итоге ATG выдавало сообщение о сбое
4. **Непредсказуемость**: Иногда работало нормально, иногда зависало

## Корневая причина
Функция `AttachToProcess` выполнялась в **UI потоке (Main Thread)**, что приводило к блокировке пользовательского интерфейса. Операция attach включает в себя:
- Запуск ACE сервера
- Создание сокетных соединений
- Проверку подключения
- Верификацию attachment

Эти операции могут занимать несколько секунд, что блокировало UI поток и приводило к зависанию.

## Решение

### 1. Перенос в фоновый поток
Сделал функцию `AttachToProcess` приостанавливаемой (`suspend function`) и перенес все тяжелые операции в `Dispatchers.IO`:

```kotlin
private suspend fun AttachToProcess(
    ace: ACE?,
    pid: Long,
    onProcessNoExistAnymore: () -> Unit,
    onAttachSuccess: () -> Unit,
    onAttachFailure: (msg: String) -> Unit,
) {
    // Perform all heavy operations in background thread
    withContext(Dispatchers.IO) {
        // Все операции attach выполняются здесь
        // UI callbacks выполняются в Main потоке
        withContext(Dispatchers.Main) {
            onAttachSuccess()
        }
    }
}
```

### 2. Использование корутин в UI
Добавил `rememberCoroutineScope()` для запуска attach операции в фоновом режиме:

```kotlin
val coroutineScope = rememberCoroutineScope()

// При нажатии "OK" в диалоге подтверждения
coroutineScope.launch {
    // Attach выполняется в фоновом потоке
    AttachToProcess(...)
}
```

### 3. Защита от множественных операций
Добавил флаг `isAttachInProgress` для предотвращения одновременных attach операций:

```kotlin
val isAttachInProgress = remember { mutableStateOf(false) }

// Проверка перед началом attach
if (isAttachInProgress.value) {
    // Показать предупреждение
    return
}
```

### 4. Таймаут для операций
Добавил таймаут в 30 секунд для предотвращения бесконечного зависания:

```kotlin
val result = withTimeoutOrNull(30000) {
    AttachToProcess(...)
}

if (result == null) {
    // Операция превысила таймаут
    showTimeoutError()
}
```

### 5. Улучшенная обратная связь
- Немедленное обновление статуса: "Attaching to PID - ProcessName..."
- Подробное логирование всех этапов операции
- Информативные сообщения об ошибках

## Технические детали

### Использование потоков:
- **UI Thread**: Обновления интерфейса, показ диалогов
- **IO Thread**: Attach операции, сетевые вызовы, файловые операции
- **Main Thread**: Callbacks от фоновых операций

### Обработка ошибок:
```kotlin
try {
    withContext(Dispatchers.IO) {
        // Фоновая операция
    }
} catch (e: Exception) {
    // Обработка в UI потоке
    showError(e.message)
}
```

### Логирование:
```kotlin
android.util.Log.d("ATG", "Starting attach process for PID: $pid")
android.util.Log.d("ATG", "Attaching to process $pid...")
android.util.Log.d("ATG", "Successfully attached to process $pid")
android.util.Log.d("ATG", "Verified attachment to PID: $attachedPid")
```

## Результат

### До исправления:
- ❌ Зависание UI при attach
- ❌ Сбои приложения
- ❌ Непредсказуемое поведение
- ❌ Отсутствие обратной связи

### После исправления:
- ✅ UI остается отзывчивым во время attach
- ✅ Немедленная обратная связь пользователю
- ✅ Защита от множественных операций
- ✅ Таймаут предотвращает бесконечное зависание
- ✅ Подробное логирование для диагностики
- ✅ Стабильная работа без сбоев

## Файлы изменены
- `ATG/app/src/main/java/com/kuhakupixel/atg/ui/menu/Process.kt`

## Дополнительные улучшения
1. **Визуальная обратная связь**: Статус "Attaching to..." показывается немедленно
2. **Предотвращение дублирования**: Нельзя запустить attach если уже выполняется
3. **Автоматическое восстановление**: При ошибке статус сбрасывается на "None"
4. **Информативные ошибки**: Разные сообщения для разных типов ошибок

## Проверка работоспособности
1. Запустить ATG
2. Нажать на процесс в списке
3. Нажать "OK" в диалоге подтверждения
4. UI должен остаться отзывчивым
5. Статус должен показать "Attaching to..."
6. После завершения должен появиться диалог с результатом

Теперь ATG корректно обрабатывает процесс attach без зависания интерфейса!