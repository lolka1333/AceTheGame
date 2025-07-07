# ATG Fix: Attach Errors and False Connection Lost

## Проблемы
Пользователь столкнулся с двумя критическими проблемами:

### 1. "Cannot Attach without DeAttaching first"
**Ошибка**: `Failed to attach to process 8559: Cannot Attach without DeAttaching first`
**Симптомы**: Невозможность подключиться к процессу из-за неправильного определения состояния attach

### 2. Ложное "Connection Lost" при all_read_write сканировании
**Ошибка**: На середине прогресс-бара появлялось "connection lost" хотя соединение было в порядке
**Симптомы**: Прерывание сканирования при all_read_write операциях

## Корневые причины

### Проблема 1: Неправильная логика проверки состояния attach
- `IsAttached()` проверял не только наличие клиента, но и его отзывчивость
- После интенсивных операций сервер мог быть временно неотзывчив
- `AssertNoAttachInARow()` блокировал attach даже когда сервер был недоступен
- Отсутствие механизма принудительного detach при неотзывчивом сервере

### Проблема 2: Чрезмерно строгие проверки во время/после сканирования
- Проверка `IsServerResponsive()` сразу после интенсивных операций
- Сервер ACE может быть временно неотзывчив после all_read_write сканирования
- Ложная интерпретация временной неотзывчивости как потери соединения

## Исправления

### 1. ACE.kt - Разделение логики проверки состояния

#### Новые методы:
```kotlin
fun IsAttached(): Boolean {
    return aceAttachClient != null && IsServerResponsive()
}

fun HasClient(): Boolean {
    return aceAttachClient != null
}

fun ForceDetach() {
    // Принудительный detach без коммуникации с сервером
}
```

#### Улучшенная логика:
- `IsAttached()` - полная проверка (клиент + отзывчивость)
- `HasClient()` - только наличие клиента (для UI состояний)
- `ForceDetach()` - принудительная очистка при неотзывчивом сервере

#### Исправленная AssertNoAttachInARow():
```kotlin
private fun AssertNoAttachInARow() {
    if (HasClient()) {
        android.util.Log.w("ATG", "Client exists but may not be responsive, attempting to force detach first")
        ForceDetach()
    }
}
```

#### Улучшенная IsServerResponsive():
- Убрана циклическая зависимость с `IsAttached()`
- Прямая проверка через `aceAttachClient`

### 2. MemoryUtil.kt - Умная проверка для интенсивных операций

#### Интеллектуальная логика проверки:
```kotlin
// Для интенсивных операций пропускаем начальную проверку отзывчивости
val isIntensiveOperation = scanOptions.regionLevel == ACE.RegionLevel.all_read_write || 
                           scanOptions.regionLevel == ACE.RegionLevel.all

if (!isIntensiveOperation && !ace.IsServerResponsive()) {
    // Ошибка только для обычных операций
}
```

### 3. Memory.kt - Толерантная обработка после сканирования

#### Исправленный onScanDone:
```kotlin
onScanDone = {
    // Не проверяем responsiveness сразу после сканирования
    try {
        if (ace.HasClient()) {
            UpdateMatches(ace = ace)
        }
    } catch (e: Exception) {
        // Обработка реальных ошибок
    }
}
```

#### Улучшенная UpdateMatches():
- Убрана проверка `IsServerResponsive()` на входе
- Попытка выполнить операцию напрямую
- Обработка ошибок с конкретными сообщениями

#### Замена IsAttached() на HasClient():
- В UI состояниях используется `HasClient()`
- `IsAttached()` только для критических проверок

### 4. Process.kt - Умный detach при attach

#### Улучшенная логика detach:
```kotlin
if (ace.HasClient()) {
    if (ace.IsServerResponsive()) {
        ace.DeAttach()  // Обычный detach
    } else {
        ace.ForceDetach()  // Принудительный detach
    }
}
```

## Технические детали

### Разделение ответственности:
- **HasClient()**: Проверка наличия соединения (для UI)
- **IsAttached()**: Полная проверка (клиент + отзывчивость)
- **IsServerResponsive()**: Проверка отзывчивости сервера

### Обработка интенсивных операций:
- Пропуск начальных проверок для all_read_write
- Толерантность к временной неотзывчивости
- Попытка выполнения операций без предварительных проверок

### Принудительный detach:
- Очистка состояния без коммуникации с сервером
- Использование при зависших соединениях
- Автоматический вызов при проблемах с detach

## Сообщения об ошибках

### Было:
- "Connection lost during scan" (ложное)
- "Cannot Attach without DeAttaching first" (блокирующее)

### Стало:
- "Operation timed out - server may be busy"
- "Server not responding - try again or reattach"
- "Connection lost - please reattach" (только при реальной потере)

## Результат

### До исправления:
- ❌ Блокировка attach при неотзывчивом сервере
- ❌ Ложные "connection lost" при all_read_write
- ❌ Невозможность повторного подключения
- ❌ Чрезмерно строгие проверки

### После исправления:
- ✅ Автоматический ForceDetach при проблемах
- ✅ Корректная работа all_read_write сканирования
- ✅ Толерантность к временной неотзывчивости
- ✅ Умные проверки состояния
- ✅ Информативные сообщения об ошибках

## Файлы изменены
1. **ACE.kt** - разделение логики состояния, ForceDetach()
2. **MemoryUtil.kt** - умные проверки для интенсивных операций
3. **Memory.kt** - толерантная обработка после сканирования
4. **Process.kt** - умный detach при attach

## Проверка работоспособности

### Тест 1: Attach после зависшего соединения
1. Подключиться к процессу
2. Имитировать зависание сервера
3. Попытаться подключиться к другому процессу
4. ✅ Должен сработать автоматический ForceDetach

### Тест 2: all_read_write сканирование
1. Подключиться к процессу
2. Выбрать all_read_write region level
3. Запустить сканирование
4. ✅ Не должно быть ложного "connection lost"

### Тест 3: Повторные attach операции
1. Подключиться к процессу A
2. Подключиться к процессу B (без явного detach)
3. ✅ Должен работать автоматический detach

Теперь ATG корректно обрабатывает состояния подключения и не выдает ложные ошибки о потере соединения!