package com.genuine.nsplayer.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class EpgDao_Impl implements EpgDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<EpgEntity> __insertionAdapterOfEpgEntity;

  private final SharedSQLiteStatement __preparedStmtOfPruneOldPrograms;

  private final SharedSQLiteStatement __preparedStmtOfClearAllEpg;

  public EpgDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfEpgEntity = new EntityInsertionAdapter<EpgEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `epg_programs` (`id`,`channelTvgId`,`title`,`description`,`startEpoch`,`endEpoch`,`category`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final EpgEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getChannelTvgId());
        statement.bindString(3, entity.getTitle());
        if (entity.getDescription() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getDescription());
        }
        statement.bindLong(5, entity.getStartEpoch());
        statement.bindLong(6, entity.getEndEpoch());
        if (entity.getCategory() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getCategory());
        }
      }
    };
    this.__preparedStmtOfPruneOldPrograms = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM epg_programs WHERE endEpoch < ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearAllEpg = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM epg_programs";
        return _query;
      }
    };
  }

  @Override
  public Object insertPrograms(final List<EpgEntity> programs,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfEpgEntity.insert(programs);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object pruneOldPrograms(final long cutoffTime,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfPruneOldPrograms.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, cutoffTime);
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
          __preparedStmtOfPruneOldPrograms.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAllEpg(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAllEpg.acquire();
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
          __preparedStmtOfClearAllEpg.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<EpgEntity>> getUpcomingPrograms(final String tvgId, final long currentTime) {
    final String _sql = "\n"
            + "        SELECT * FROM epg_programs \n"
            + "        WHERE channelTvgId = ? \n"
            + "        AND endEpoch > ? \n"
            + "        ORDER BY startEpoch ASC \n"
            + "        LIMIT 10\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, tvgId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, currentTime);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"epg_programs"}, new Callable<List<EpgEntity>>() {
      @Override
      @NonNull
      public List<EpgEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfChannelTvgId = CursorUtil.getColumnIndexOrThrow(_cursor, "channelTvgId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfStartEpoch = CursorUtil.getColumnIndexOrThrow(_cursor, "startEpoch");
          final int _cursorIndexOfEndEpoch = CursorUtil.getColumnIndexOrThrow(_cursor, "endEpoch");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final List<EpgEntity> _result = new ArrayList<EpgEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final EpgEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpChannelTvgId;
            _tmpChannelTvgId = _cursor.getString(_cursorIndexOfChannelTvgId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final long _tmpStartEpoch;
            _tmpStartEpoch = _cursor.getLong(_cursorIndexOfStartEpoch);
            final long _tmpEndEpoch;
            _tmpEndEpoch = _cursor.getLong(_cursorIndexOfEndEpoch);
            final String _tmpCategory;
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            }
            _item = new EpgEntity(_tmpId,_tmpChannelTvgId,_tmpTitle,_tmpDescription,_tmpStartEpoch,_tmpEndEpoch,_tmpCategory);
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
  public Object getCurrentProgram(final String tvgId, final long currentTime,
      final Continuation<? super EpgEntity> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM epg_programs \n"
            + "        WHERE channelTvgId = ? \n"
            + "        AND startEpoch <= ? \n"
            + "        AND endEpoch >= ? \n"
            + "        LIMIT 1\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, tvgId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, currentTime);
    _argIndex = 3;
    _statement.bindLong(_argIndex, currentTime);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<EpgEntity>() {
      @Override
      @Nullable
      public EpgEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfChannelTvgId = CursorUtil.getColumnIndexOrThrow(_cursor, "channelTvgId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfStartEpoch = CursorUtil.getColumnIndexOrThrow(_cursor, "startEpoch");
          final int _cursorIndexOfEndEpoch = CursorUtil.getColumnIndexOrThrow(_cursor, "endEpoch");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final EpgEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpChannelTvgId;
            _tmpChannelTvgId = _cursor.getString(_cursorIndexOfChannelTvgId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final long _tmpStartEpoch;
            _tmpStartEpoch = _cursor.getLong(_cursorIndexOfStartEpoch);
            final long _tmpEndEpoch;
            _tmpEndEpoch = _cursor.getLong(_cursorIndexOfEndEpoch);
            final String _tmpCategory;
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            }
            _result = new EpgEntity(_tmpId,_tmpChannelTvgId,_tmpTitle,_tmpDescription,_tmpStartEpoch,_tmpEndEpoch,_tmpCategory);
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
