# ATG Compilation Fix - Memory.kt

## Проблема
При попытке компиляции ATG проекта возникла следующая ошибка:

```
e: file:///home/runner/work/AceTheGame/AceTheGame/ATG/app/src/main/java/com/kuhakupixel/atg/ui/menu/Memory.kt:264:35 Unresolved reference: @newScanClicked
e: file:///home/runner/work/AceTheGame/AceTheGame/ATG/app/src/main/java/com/kuhakupixel/atg/ui/menu/Memory.kt:271:35 Unresolved reference: @newScanClicked
```

## Причина
В процессе предыдущих исправлений был использован неправильный синтаксис `return@newScanClicked` в lambda выражении. В Kotlin labeled return можно использовать только в inline функциях или с явными метками.

## Исправление
Заменил конструкцию `return@newScanClicked` на структуру if-else без early return:

### Было:
```kotlin
newScanClicked = {
    try {
        if (!ace.IsAttached()) {
            android.util.Log.w("ATG", "Cannot start new scan: not attached to any process")
            currentMatchesList.value = emptyList()
            matchesStatusText.value = "Not attached to any process"
            return@newScanClicked  // <- Ошибка компиляции
        }
        
        if (!ace.IsServerResponsive()) {
            android.util.Log.w("ATG", "Cannot start new scan: ACE server is not responsive")
            currentMatchesList.value = emptyList()
            matchesStatusText.value = "Server not responding"
            return@newScanClicked  // <- Ошибка компиляции
        }
        
        ace.ResetMatches()
        UpdateMatches(ace = ace)
        initialScanDone.value = false
        android.util.Log.d("ATG", "New scan started successfully")
        
    } catch (e: Exception) {
        android.util.Log.e("ATG", "Error starting new scan: ${e.message}", e)
        currentMatchesList.value = emptyList()
        matchesStatusText.value = "Error starting new scan: ${e.message ?: "Unknown error"}"
    }
},
```

### Стало:
```kotlin
newScanClicked = {
    try {
        if (!ace.IsAttached()) {
            android.util.Log.w("ATG", "Cannot start new scan: not attached to any process")
            currentMatchesList.value = emptyList()
            matchesStatusText.value = "Not attached to any process"
        } else if (!ace.IsServerResponsive()) {
            android.util.Log.w("ATG", "Cannot start new scan: ACE server is not responsive")
            currentMatchesList.value = emptyList()
            matchesStatusText.value = "Server not responding"
        } else {
            ace.ResetMatches()
            UpdateMatches(ace = ace)
            initialScanDone.value = false
            android.util.Log.d("ATG", "New scan started successfully")
        }
    } catch (e: Exception) {
        android.util.Log.e("ATG", "Error starting new scan: ${e.message}", e)
        currentMatchesList.value = emptyList()
        matchesStatusText.value = "Error starting new scan: ${e.message ?: "Unknown error"}"
    }
},
```

## Результат
- Устранена ошибка компиляции Kotlin
- Сохранена функциональность проверки состояния соединения
- Код теперь соответствует стандартам Kotlin

## Файлы изменены
- `ATG/app/src/main/java/com/kuhakupixel/atg/ui/menu/Memory.kt`
- `ATG_CRASH_FIXES.md` (обновлена документация)

Теперь проект готов к компиляции без ошибок в Kotlin коде.