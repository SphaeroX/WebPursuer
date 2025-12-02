package com.example.webpursuer.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
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

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class InteractionDao_Impl implements InteractionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Interaction> __insertionAdapterOfInteraction;

  private final SharedSQLiteStatement __preparedStmtOfDeleteInteractionsForMonitor;

  public InteractionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfInteraction = new EntityInsertionAdapter<Interaction>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `interactions` (`id`,`monitorId`,`type`,`selector`,`value`,`orderIndex`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Interaction entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getMonitorId());
        statement.bindString(3, entity.getType());
        statement.bindString(4, entity.getSelector());
        if (entity.getValue() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getValue());
        }
        statement.bindLong(6, entity.getOrderIndex());
      }
    };
    this.__preparedStmtOfDeleteInteractionsForMonitor = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM interactions WHERE monitorId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<Interaction> interactions,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfInteraction.insert(interactions);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteInteractionsForMonitor(final int monitorId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteInteractionsForMonitor.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, monitorId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteInteractionsForMonitor.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getInteractionsForMonitor(final int monitorId,
      final Continuation<? super List<Interaction>> $completion) {
    final String _sql = "SELECT * FROM interactions WHERE monitorId = ? ORDER BY orderIndex ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, monitorId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Interaction>>() {
      @Override
      @NonNull
      public List<Interaction> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMonitorId = CursorUtil.getColumnIndexOrThrow(_cursor, "monitorId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfSelector = CursorUtil.getColumnIndexOrThrow(_cursor, "selector");
          final int _cursorIndexOfValue = CursorUtil.getColumnIndexOrThrow(_cursor, "value");
          final int _cursorIndexOfOrderIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "orderIndex");
          final List<Interaction> _result = new ArrayList<Interaction>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Interaction _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final int _tmpMonitorId;
            _tmpMonitorId = _cursor.getInt(_cursorIndexOfMonitorId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpSelector;
            _tmpSelector = _cursor.getString(_cursorIndexOfSelector);
            final String _tmpValue;
            if (_cursor.isNull(_cursorIndexOfValue)) {
              _tmpValue = null;
            } else {
              _tmpValue = _cursor.getString(_cursorIndexOfValue);
            }
            final int _tmpOrderIndex;
            _tmpOrderIndex = _cursor.getInt(_cursorIndexOfOrderIndex);
            _item = new Interaction(_tmpId,_tmpMonitorId,_tmpType,_tmpSelector,_tmpValue,_tmpOrderIndex);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
