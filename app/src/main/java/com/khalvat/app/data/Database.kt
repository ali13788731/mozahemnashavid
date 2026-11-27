package com.khalvat.app.data

import android.content.Context
import android.util.Base64
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.security.SecureRandom

@Database(
    entities = [PrivateContact::class, Rule::class, ArchivedCall::class, ArchivedSms::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contacts(): PrivateContactDao
    abstract fun rules(): RuleDao
    abstract fun calls(): ArchivedCallDao
    abstract fun sms(): ArchivedSmsDao
}

/** دسترسی تک‌نمونه‌ای به دیتابیس رمزنگاری‌شده. */
object Db {
    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase =
        instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

    private fun build(context: Context): AppDatabase {
        System.loadLibrary("sqlcipher")
        val factory = SupportOpenHelperFactory(Passphrase.get(context))
        return Room.databaseBuilder(context, AppDatabase::class.java, "khalvat.db")
            .openHelperFactory(factory)
            .build()
    }
}

/**
 * کلید تصادفی ۳۲ بایتی دیتابیس؛ یک بار ساخته می‌شود و داخل
 * EncryptedSharedPreferences (کلید اصلی در Android Keystore) نگهداری می‌شود.
 */
private object Passphrase {
    fun get(context: Context): ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val prefs = EncryptedSharedPreferences.create(
            context,
            "khalvat_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        prefs.getString("db_key", null)?.let { return Base64.decode(it, Base64.NO_WRAP) }

        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit().putString("db_key", Base64.encodeToString(bytes, Base64.NO_WRAP)).commit()
        return bytes
    }
}
