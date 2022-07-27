package ru.rutoken.demoshift.ui.dls

import android.content.Context
import ru.rutoken.demoshift.repository.User
import java.io.File

@Deprecated("Удалить")
class UserUtils {
    companion object {
        /**
         * Запись информации в файл
         * @param context контекст
         * @param userId иден. пользователь
         */
        fun writeDefaultUser(context: Context, userId: Int) {
            val file = File(context.cacheDir, "default")
            file.createNewFile()
            file.writeText(userId.toString())
        }

        /**
         * чтение пользователя по умолчанию
         * @param context контекст
         * @return идентификатор пользователя
         */
        fun readDefaultUser(context: Context): Int? {
            val file = File(context.cacheDir, "default")
            return if (file.exists()) {
                val txt = file.readText()
                txt.toInt()
            } else {
                null
            }
        }
    }
}