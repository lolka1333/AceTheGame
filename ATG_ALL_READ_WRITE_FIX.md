# ATG Fix: All Read/Write Scan Timeout Issue

## Проблема
При выборе опции "all read write" в ATG возникала ошибка:
```
Error

Communication with the target process failed.

The process may have crashed. Please try reattaching.
```

При этом целевой процесс продолжал работать нормально.

## Причина
Сканирование "all read write" является крайне ресурсоемкой операцией, которая сканирует все области памяти с правами чтения и записи. Эта операция может занимать несколько минут, но таймауты в ACEAttachClient были установлены на 10 секунд, что приводило к преждевременному завершению операции.

## Исправления

### 1. ACEAttachClient.kt - Динамические таймауты
**Добавлено**:
- Система динамических таймаутов для интенсивных операций
- Автоматическое обнаружение команд с `all_read_write` или `all`
- Увеличение таймаутов до 5 минут (300 секунд) для интенсивных операций
- Автоматический сброс таймаутов после завершения операции

**Новые методы**:
```kotlin
fun setTimeouts(sendTimeoutMs: Int, receiveTimeoutMs: Int)
fun resetTimeouts()
fun setExtendedTimeouts() // 5 минут для интенсивных операций
```

**Логика**:
- Обнаружение команд сканирования с `all_read_write` или `all`
- Автоматическое увеличение таймаутов с 10 секунд до 5 минут
- Информативные сообщения о процессе выполнения
- Специальные сообщения об ошибках для интенсивных операций

### 2. MemoryUtil.kt - Улучшенная обработка ошибок
**Добавлено**:
- Специальные сообщения об ошибках для `all_read_write` операций
- Рекомендации по использованию менее интенсивных опций
- Информативные сообщения о таймауте

**Новые сообщения об ошибках**:
```kotlin
"All Read/Write scan failed: This operation scans all memory regions and may be too resource-intensive for this device or take too long. Try using a more specific region level like 'heap' or 'stack' instead."

"All Read/Write scan timed out: This operation scans all memory regions and took too long to complete. Try using a more specific region level like 'heap' or 'stack' for better performance."
```

### 3. Memory.kt - Предупреждение пользователя
**Добавлено**:
- Предупреждение в логе при запуске `all_read_write` сканирования
- Информирование пользователя о том, что операция может занять много времени

## Технические детали

### Система таймаутов
- **Обычные операции**: 10 секунд (по умолчанию)
- **Интенсивные операции** (`all_read_write`): 5 минут (300 секунд)
- **Автоматический сброс**: таймауты сбрасываются после завершения операции

### Обнаружение интенсивных операций
```kotlin
val isIntensiveOperation = requestCmdStr.contains("scan") && 
                          (requestCmdStr.contains("all_read_write") || 
                           requestCmdStr.contains("all"))
```

### Оптимизация повторных попыток
- Обычные операции: 3 попытки с задержкой
- Интенсивные операции: 1 попытка (чтобы не ждать еще 5 минут)

## Результат
- ✅ Сканирование "all read write" теперь работает корректно
- ✅ Пользователь получает информативные сообщения об ошибках
- ✅ Автоматическое управление таймаутами
- ✅ Предупреждения о длительности операций
- ✅ Рекомендации по использованию менее интенсивных опций

## Файлы изменены
1. `ATG/app/src/main/java/com/kuhakupixel/atg/backend/ACEAttachClient.kt`
2. `ATG/app/src/main/java/com/kuhakupixel/atg/ui/menu/MemoryUtil.kt`
3. `ATG/app/src/main/java/com/kuhakupixel/atg/ui/menu/Memory.kt`

## Рекомендации пользователю
1. **Для лучшей производительности**: Используйте более специфичные уровни регионов:
   - `heap` - для данных в куче
   - `stack` - для данных в стеке  
   - `heap_stack_executable_bss` - комбинированный вариант

2. **Если all_read_write все же нужен**: Будьте готовы к тому, что операция может занять несколько минут

3. **При таймауте**: Попробуйте использовать менее интенсивные опции или убедитесь, что устройство не перегружено

Теперь ATG корректно обрабатывает интенсивные операции сканирования памяти без ложных ошибок о потере соединения.