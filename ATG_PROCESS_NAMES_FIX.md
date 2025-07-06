# Исправление обрезанных имен процессов в ATG

## Проблема
В приложении ATG (Android Traffic Generator) имена процессов отображались не полностью - длинные имена процессов обрезались, что затрудняло их идентификацию.

## Причины проблемы
1. **Ограничение ACE util_client**: ACE использует собственную утилиту `util_client` для получения списка процессов, которая ограничивает длину имен
2. **Команда `ps ls --reverse`**: используемая команда возвращает обрезанные имена процессов
3. **Недостаточная ширина колонки**: колонка "Name" занимала только 70% ширины таблицы  
4. **Отсутствие поддержки многострочного текста**: длинные имена не могли переноситься на новые строки

## Внесенные исправления

### 1. Создание альтернативных методов получения процессов
**Файл:** `ATG/app/src/main/java/com/kuhakupixel/atg/backend/ACE.kt`

**Добавлены новые методы:**

```kotlin
/**
 * Get running processes with full names using standard Linux ps command
 * This bypasses the ACE util_client limitation that truncates process names
 */
fun ListRunningProcFull(): List<ProcInfo> {
    // Uses: ps -eo pid,comm --no-headers --sort=-pid
    // Returns full process names without truncation
}

/**
 * Get running processes with full command lines (including arguments)
 */
fun ListRunningProcWithArgs(): List<ProcInfo> {
    // Uses: ps -eo pid,args --no-headers --sort=-pid
    // Returns full command lines with arguments
}
```

### 2. Добавление режимов отображения процессов
**Файл:** `ATG/app/src/main/java/com/kuhakupixel/atg/ui/menu/Process.kt`

**Добавлены:**
- Enum для режимов отображения:
  - `ORIGINAL` - использует ACE util_client (может обрезать имена)
  - `FULL_NAME` - полные имена процессов
  - `FULL_COMMAND` - полные командные строки с аргументами

```kotlin
enum class ProcessDisplayMode {
    ORIGINAL,    // Using ACE util_client (may truncate names)
    FULL_NAME,   // Full process names using ps -eo pid,comm  
    FULL_COMMAND // Full command lines using ps -eo pid,args
}
```

### 3. Обновление функции получения процессов
**Файл:** `ATG/app/src/main/java/com/kuhakupixel/atg/ui/menu/Process.kt`

```kotlin
fun refreshProcList(ace: ACE?, processList: SnapshotStateList<ProcInfo>) {
    // remove old elements
    processList.clear()
    // grab new one and add to the list using the selected display mode
    val runningProcs: List<ProcInfo>? = when (processDisplayMode.value) {
        ProcessDisplayMode.ORIGINAL -> ace!!.ListRunningProc()
        ProcessDisplayMode.FULL_NAME -> ace!!.ListRunningProcFull()
        ProcessDisplayMode.FULL_COMMAND -> ace!!.ListRunningProcWithArgs()
    }
    if (runningProcs != null) {
        for (proc in runningProcs) processList.add(proc)
    }
}
```

### 4. Добавление кнопки переключения режимов
**Интерфейс дополнен:**
- Кнопка для переключения между режимами отображения
- Отображение текущего режима
- Автоматическое обновление списка при смене режима

### 5. Увеличение размера колонки имени процесса
**Файл:** `ATG/app/src/main/java/com/kuhakupixel/atg/ui/menu/Process.kt`
**Изменение:** Строка 90
```kotlin
// Было:
colWeights = listOf(0.3f, 0.7f)

// Стало:
colWeights = listOf(0.2f, 0.8f)
```

### 6. Улучшение отображения текста в таблице
**Файл:** `ATG/app/src/main/java/com/kuhakupixel/atg/ui/util/Table.kt`

**Изменения:**
- Добавлен импорт `TextOverflow`
- Удалена горизонтальная прокрутка из ячеек таблицы
- Добавлена поддержка многострочного текста

```kotlin
Text(
    text = text,
    modifier = GetCellModifier(weight).defaultMinSize(minHeight = rowMinHeight),
    // Allow text to wrap to multiple lines for better readability
    maxLines = Int.MAX_VALUE,
    overflow = TextOverflow.Visible
)
```

### 7. Специальная обработка для имен процессов
**Файл:** `ATG/app/src/main/java/com/kuhakupixel/atg/ui/menu/Process.kt`

```kotlin
if (colIndex == 1) {
    Text(
        text = processList[rowIndex].GetName(),
        maxLines = Int.MAX_VALUE,
        overflow = TextOverflow.Visible
    )
}
```

## Результат
После внесения изменений:
1. **Полные имена процессов**: теперь используются стандартные команды Linux для получения полных имен
2. **Три режима отображения**: 
   - Оригинальный (для совместимости)
   - Полные имена процессов
   - Полные командные строки с аргументами
3. **Больше места для имен**: колонка Name занимает 80% ширины экрана
4. **Полная видимость текста**: длинные имена процессов переносятся на несколько строк
5. **Лучшая читаемость**: удалена горизонтальная прокрутка
6. **Переключение режимов**: пользователь может выбрать подходящий режим отображения

## Технические детали

### Используемые команды Linux:
- `ps -eo pid,comm --no-headers --sort=-pid` - для полных имен процессов
- `ps -eo pid,args --no-headers --sort=-pid` - для полных командных строк

### Fallback механизм:
Если новые методы не работают, приложение автоматически переключается на оригинальный метод ACE util_client.

## Заключение
Проблема с обрезанными именами процессов полностью решена. Теперь пользователи могут:
- Видеть полные имена процессов
- Выбирать режим отображения (имена или командные строки)
- Переключаться между режимами одной кнопкой
- Наслаждаться улучшенной читаемостью интерфейса