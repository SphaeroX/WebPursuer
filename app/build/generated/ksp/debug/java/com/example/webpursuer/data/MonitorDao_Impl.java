package com.example.webpursuer.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class MonitorDao_Impl implements MonitorDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Monitor> __insertionAdapterOfMonitor;

  private final EntityDeletionOrUpdateAdapter<Monitor> __deletionAdapterOfMonitor;

  private final EntityDeletionOrUpdateAdapter<Monitor> __updateAdapterOfMonitor;

  public MonitorDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMonitor = new EntityInsertionAdapter<Monitor>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `monitors` (`id`,`url`,`name`,`selector`,`checkIntervalMinutes`,`lastCheckTime`,`lastContentHash`,`enabled`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Monitor entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getUrl());
        statement.bindString(3, entity.getName());
        statement.bindString(4, entity.getSelector());
        statement.bindLong(5, entity.getCheckIntervalMinutes());
        statement.bindLong(6, entity.getLastCheckTime());
        if (entity.getLastContentHash() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getLastContentHash());
        }
        final int _tmp = entity.getEnabled() ? 1 : 0;
        statement.bindLong(8, _tmp);
      }
    };
    this.__deletionAdapterOfMonitor = new EntityDeletionOrUpdateAdapter<Monitor>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `monitors` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Monitor entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfMonitor = new EntityDeletionOrUpdateAdapter<Monitor>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `monitors` SET `id` = ?,`url` = ?,`name` = ?,`selector` = ?,`checkIntervalMinutes` = ?,`lastCheckTime` = ?,`lastContentHash` = ?,`enabled` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Monitor entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getUrl());
        statement.bindString(3, entity.getName());
        statement.bindString(4, entity.getSelector());
        statement.bindLong(5, entity.getCheckIntervalMinutes());
        statement.bindLong(6, entity.getLastCheckTime());
        if (entity.getLastContentHash() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getLastContentHash());
        }
        final int _tmp = entity.getEnabled() ? 1 : 0;
        statement.bindLong(8, _tmp);
        statement.bindLong(9, entity.getId());
      }
    };
  }

  @Override
  public Object insertAndReturnId(final Monitor monitor,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfMonitor.insertAndReturnId(monitor);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insert(final Monitor monitor, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMonitor.insert(monitor);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final Monitor monitor, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfMonitor.handle(monitor);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final Monitor monitor, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfMonitor.handle(monitor);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Monitor>> getAll() {
    final String _sql = "SELECT * FROM monitors";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"monitors"}, new Callable<List<Monitor>>() {
      @Override
      @NonNull
      public List<Monitor> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfSelector = CursorUtil.getColumnIndexOrThrow(_cursor, "selector");
          final int _cursorIndexOfCheckIntervalMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "checkIntervalMinutes");
          final int _cursorIndexOfLastCheckTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastCheckTime");
          final int _cursorIndexOfLastContentHash = CursorUtil.getColumnIndexOrThrow(_cursor, "lastContentHash");
          final int _cursorIndexOfEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "enabled");
          final List<Monitor> _result = new ArrayList<Monitor>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Monitor _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpSelector;
            _tmpSelector = _cursor.getString(_cursorIndexOfSelector);
            final long _tmpCheckIntervalMinutes;
            _tmpCheckIntervalMinutes = _cursor.getLong(_cursorIndexOfCheckIntervalMinutes);
            final long _tmpLastCheckTime;
            _tmpLastCheckTime = _cursor.getLong(_cursorIndexOfLastCheckTime);
            final String _tmpLastContentHash;
            if (_cursor.isNull(_cursorIndexOfLastContentHash)) {
              _tmpLastContentHash = null;
            } else {
              _tmpLastContentHash = _cursor.getString(_cursorIndexOfLastContentHash);
            }
            final boolean _tmpEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEnabled);
            _tmpEnabled = _tmp != 0;
            _item = new Monitor(_tmpId,_tmpUrl,_tmpName,_tmpSelector,_tmpCheckIntervalMinutes,_tmpLastCheckTime,_tmpLastContentHash,_tmpEnabled);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllSync(final Continuation<? super List<Monitor>> $completion) {
    final String _sql = "SELECT * FROM monitors";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Monitor>>() {
      @Override
      @NonNull
      public List<Monitor> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfSelector = CursorUtil.getColumnIndexOrThrow(_cursor, "selector");
          final int _cursorIndexOfCheckIntervalMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "checkIntervalMinutes");
          final int _cursorIndexOfLastCheckTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastCheckTime");
          final int _cursorIndexOfLastContentHash = CursorUtil.getColumnIndexOrThrow(_cursor, "lastContentHash");
          final int _cursorIndexOfEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "enabled");
          final List<Monitor> _result = new ArrayList<Monitor>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Monitor _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpSelector;
            _tmpSelector = _cursor.getString(_cursorIndexOfSelector);
            final long _tmpCheckIntervalMinutes;
            _tmpCheckIntervalMinutes = _cursor.getLong(_cursorIndexOfCheckIntervalMinutes);
            final long _tmpLastCheckTime;
            _tmpLastCheckTime = _cursor.getLong(_cursorIndexOfLastCheckTime);
            final String _tmpLastContentHash;
            if (_cursor.isNull(_cursorIndexOfLastContentHash)) {
              _tmpLastContentHash = null;
            } else {
              _tmpLastContentHash = _cursor.getString(_cursorIndexOfLastContentHash);
            }
            final boolean _tmpEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEnabled);
            _tmpEnabled = _tmp != 0;
            _item = new Monitor(_tmpId,_tmpUrl,_tmpName,_tmpSelector,_tmpCheckIntervalMinutes,_tmpLastCheckTime,_tmpLastContentHash,_tmpEnabled);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getById(final int id, final Continuation<? super Monitor> $completion) {
    final String _sql = "SELECT * FROM monitors WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Monitor>() {
      @Override
      @Nullable
      public Monitor call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfSelector = CursorUtil.getColumnIndexOrThrow(_cursor, "selector");
          final int _cursorIndexOfCheckIntervalMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "checkIntervalMinutes");
          final int _cursorIndexOfLastCheckTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastCheckTime");
          final int _cursorIndexOfLastContentHash = CursorUtil.getColumnIndexOrThrow(_cursor, "lastContentHash");
          final int _cursorIndexOfEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "enabled");
          final Monitor _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpSelector;
            _tmpSelector = _cursor.getString(_cursorIndexOfSelector);
            final long _tmpCheckIntervalMinutes;
            _tmpCheckIntervalMinutes = _cursor.getLong(_cursorIndexOfCheckIntervalMinutes);
            final long _tmpLastCheckTime;
            _tmpLastCheckTime = _cursor.getLong(_cursorIndexOfLastCheckTime);
            final String _tmpLastContentHash;
            if (_cursor.isNull(_cursorIndexOfLastContentHash)) {
              _tmpLastContentHash = null;
            } else {
              _tmpLastContentHash = _cursor.getString(_cursorIndexOfLastContentHash);
            }
            final boolean _tmpEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEnabled);
            _tmpEnabled = _tmp != 0;
            _result = new Monitor(_tmpId,_tmpUrl,_tmpName,_tmpSelector,_tmpCheckIntervalMinutes,_tmpLastCheckTime,_tmpLastContentHash,_tmpEnabled);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
