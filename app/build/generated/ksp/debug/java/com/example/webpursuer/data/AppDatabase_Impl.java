package com.example.webpursuer.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile MonitorDao _monitorDao;

  private volatile CheckLogDao _checkLogDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `monitors` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `url` TEXT NOT NULL, `name` TEXT NOT NULL, `selector` TEXT NOT NULL, `checkIntervalMinutes` INTEGER NOT NULL, `lastCheckTime` INTEGER NOT NULL, `lastContentHash` TEXT, `enabled` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `check_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `monitorId` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `result` TEXT NOT NULL, `message` TEXT NOT NULL, FOREIGN KEY(`monitorId`) REFERENCES `monitors`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'b0b772b5f35035a7131caee14dad644f')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `monitors`");
        db.execSQL("DROP TABLE IF EXISTS `check_logs`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsMonitors = new HashMap<String, TableInfo.Column>(8);
        _columnsMonitors.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMonitors.put("url", new TableInfo.Column("url", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMonitors.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMonitors.put("selector", new TableInfo.Column("selector", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMonitors.put("checkIntervalMinutes", new TableInfo.Column("checkIntervalMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMonitors.put("lastCheckTime", new TableInfo.Column("lastCheckTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMonitors.put("lastContentHash", new TableInfo.Column("lastContentHash", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMonitors.put("enabled", new TableInfo.Column("enabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMonitors = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMonitors = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMonitors = new TableInfo("monitors", _columnsMonitors, _foreignKeysMonitors, _indicesMonitors);
        final TableInfo _existingMonitors = TableInfo.read(db, "monitors");
        if (!_infoMonitors.equals(_existingMonitors)) {
          return new RoomOpenHelper.ValidationResult(false, "monitors(com.example.webpursuer.data.Monitor).\n"
                  + " Expected:\n" + _infoMonitors + "\n"
                  + " Found:\n" + _existingMonitors);
        }
        final HashMap<String, TableInfo.Column> _columnsCheckLogs = new HashMap<String, TableInfo.Column>(5);
        _columnsCheckLogs.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckLogs.put("monitorId", new TableInfo.Column("monitorId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckLogs.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckLogs.put("result", new TableInfo.Column("result", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckLogs.put("message", new TableInfo.Column("message", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCheckLogs = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysCheckLogs.add(new TableInfo.ForeignKey("monitors", "CASCADE", "NO ACTION", Arrays.asList("monitorId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesCheckLogs = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCheckLogs = new TableInfo("check_logs", _columnsCheckLogs, _foreignKeysCheckLogs, _indicesCheckLogs);
        final TableInfo _existingCheckLogs = TableInfo.read(db, "check_logs");
        if (!_infoCheckLogs.equals(_existingCheckLogs)) {
          return new RoomOpenHelper.ValidationResult(false, "check_logs(com.example.webpursuer.data.CheckLog).\n"
                  + " Expected:\n" + _infoCheckLogs + "\n"
                  + " Found:\n" + _existingCheckLogs);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "b0b772b5f35035a7131caee14dad644f", "b2b7412c9d14c952e7f2f64e6ba8fe2a");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "monitors","check_logs");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `monitors`");
      _db.execSQL("DELETE FROM `check_logs`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(MonitorDao.class, MonitorDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CheckLogDao.class, CheckLogDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public MonitorDao monitorDao() {
    if (_monitorDao != null) {
      return _monitorDao;
    } else {
      synchronized(this) {
        if(_monitorDao == null) {
          _monitorDao = new MonitorDao_Impl(this);
        }
        return _monitorDao;
      }
    }
  }

  @Override
  public CheckLogDao checkLogDao() {
    if (_checkLogDao != null) {
      return _checkLogDao;
    } else {
      synchronized(this) {
        if(_checkLogDao == null) {
          _checkLogDao = new CheckLogDao_Impl(this);
        }
        return _checkLogDao;
      }
    }
  }
}
