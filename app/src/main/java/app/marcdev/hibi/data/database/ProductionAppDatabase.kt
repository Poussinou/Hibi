package app.marcdev.hibi.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.marcdev.hibi.data.entity.Entry
import app.marcdev.hibi.data.entity.NewWord
import app.marcdev.hibi.data.entity.Tag
import app.marcdev.hibi.data.entity.TagEntryRelation
import app.marcdev.hibi.internal.PRODUCTION_DATABASE_NAME
import app.marcdev.hibi.internal.PRODUCTION_DATABASE_VERSION
import timber.log.Timber

@Database(entities = [Entry::class, Tag::class, TagEntryRelation::class, NewWord::class], version = PRODUCTION_DATABASE_VERSION)

abstract class ProductionAppDatabase : RoomDatabase(), AppDatabase {
  abstract override fun dao(): DAO

  override fun checkpoint() {
    if(instance != null) {
      instance?.query("pragma wal_checkpoint(full)", null)
    } else {
      Timber.e("Log: checkpoint: instance is null")
    }
  }

  override fun closeDB() {
    if(instance != null) {
      instance?.close()
    } else {
      Timber.e("Log: closeDB: instance is null")
    }
  }

  companion object {
    @Volatile private var instance: ProductionAppDatabase? = null
    private val LOCK = Any()

    operator fun invoke(context: Context) = instance
                                            ?: synchronized(LOCK) {
                                              instance
                                              ?: buildDatabase(context).also { instance = it }
                                            }

    private fun buildDatabase(context: Context) =
      Room.databaseBuilder(context.applicationContext,
        ProductionAppDatabase::class.java,
        PRODUCTION_DATABASE_NAME)
        .addMigrations(MIGRATION_3_TO_5())
        .addMigrations(MIGRATION_5_TO_6())
        .addMigrations(MIGRATION_6_TO_7())
        .addMigrations(MIGRATION_7_TO_8())
        .build()

    class MIGRATION_3_TO_5 : Migration(3, 5) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE entries RENAME TO Entry")
        database.execSQL("CREATE TABLE Tag('name' TEXT NOT NULL, PRIMARY KEY (name))")
        database.execSQL("CREATE TABLE TagEntryRelation('tag' TEXT NOT NULL, 'entryId' INTEGER NOT NULL, PRIMARY KEY (tag, entryId))")
      }
    }

    class MIGRATION_5_TO_6 : Migration(5, 6) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE NewWord('word' TEXT NOT NULL, 'reading' TEXT NOT NULL, 'partOfSpeech' TEXT NOT NULL, 'english' TEXT NOT NULL, 'notes' TEXT NOT NULL, 'entryId' INTEGER NOT NULL, 'id' INTEGER NOT NULL PRIMARY KEY)")
      }
    }

    class MIGRATION_6_TO_7 : Migration(6, 7) {
      override fun migrate(database: SupportSQLiteDatabase) {
        // Fix nullable primary key in Entry
        database.execSQL("ALTER TABLE Entry RENAME TO EntryOLD")
        database.execSQL("CREATE TABLE Entry(id INTEGER PRIMARY KEY NOT NULL, " +
                         "day INTEGER NOT NULL," +
                         "month INTEGER NOT NULL," +
                         "year INTEGER NOT NULL," +
                         "hour INTEGER NOT NULL," +
                         "minute INTEGER NOT NULL," +
                         "content TEXT NOT NULL)")
        database.execSQL("INSERT INTO Entry SELECT * FROM EntryOLD")
        database.execSQL("DROP TABLE EntryOLD")

        // Add foreign key to NewWord
        database.execSQL("ALTER TABLE NewWord RENAME TO NewWordOLD")
        database.execSQL("CREATE TABLE NewWord(" +
                         "word TEXT NOT NULL," +
                         "reading TEXT NOT NULL," +
                         "partOfSpeech TEXT NOT NULL," +
                         "english TEXT NOT NULL," +
                         "notes TEXT NOT NULL," +
                         "entryId INTEGER NOT NULL, " +
                         "id INTEGER PRIMARY KEY NOT NULL," +
                         "FOREIGN KEY (entryId) REFERENCES Entry(id) ON DELETE CASCADE ON UPDATE CASCADE)")
        database.execSQL("INSERT INTO NewWord SELECT * FROM NewWordOLD")
        database.execSQL("DROP TABLE NewWordOLD")

        // Add foreign key to TagEntryRelation
        database.execSQL("ALTER TABLE TagEntryRelation RENAME TO TagEntryRelationOLD")
        database.execSQL("CREATE TABLE TagEntryRelation(tag TEXT NOT NULL, " +
                         "entryId INTEGER NOT NULL, " +
                         "PRIMARY KEY(tag, entryId), " +
                         "FOREIGN KEY(entryId) REFERENCES Entry(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                         "FOREIGN KEY(tag) REFERENCES Tag(name) ON DELETE CASCADE ON UPDATE CASCADE)")
        database.execSQL("INSERT INTO TagEntryRelation SELECT * FROM TagEntryRelationOLD")
        database.execSQL("DROP TABLE TagEntryRelationOLD")
      }
    }

    class MIGRATION_7_TO_8 : Migration(7, 8) {
      override fun migrate(database: SupportSQLiteDatabase) {
        // This is destructive and will delete all tags and tag entry relations
        database.execSQL("DROP TABLE Tag")
        database.execSQL("DROP TABLE TagEntryRelation")
        database.execSQL("CREATE TABLE Tag('name' TEXT NOT NULL, 'id' INTEGER NOT NULL, PRIMARY KEY(id))")
        database.execSQL("CREATE TABLE TagEntryRelation(tagId INTEGER NOT NULL, " +
                         "entryId INTEGER NOT NULL, " +
                         "PRIMARY KEY(tagId, entryId), " +
                         "FOREIGN KEY(entryId) REFERENCES Entry(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                         "FOREIGN KEY(tagId) REFERENCES Tag(id) ON DELETE CASCADE ON UPDATE CASCADE)")
      }
    }
  }
}