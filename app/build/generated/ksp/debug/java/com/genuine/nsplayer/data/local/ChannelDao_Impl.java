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
import java.lang.Integer;
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
public final class ChannelDao_Impl implements ChannelDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ChannelEntity> __insertionAdapterOfChannelEntity;

  private final SharedSQLiteStatement __preparedStmtOfSetFavorite;

  private final SharedSQLiteStatement __preparedStmtOfDeleteChannelsByPlaylist;

  public ChannelDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfChannelEntity = new EntityInsertionAdapter<ChannelEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `channels` (`id`,`playlistId`,`name`,`streamUrl`,`logoUrl`,`groupTitle`,`tvgId`,`tvgName`,`tvgCountry`,`tvgLanguage`,`contentType`,`headersJson`,`drmScheme`,`drmLicense`,`isFavorite`,`orderIndex`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ChannelEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getPlaylistId());
        statement.bindString(3, entity.getName());
        statement.bindString(4, entity.getStreamUrl());
        if (entity.getLogoUrl() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getLogoUrl());
        }
        statement.bindString(6, entity.getGroupTitle());
        if (entity.getTvgId() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getTvgId());
        }
        if (entity.getTvgName() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getTvgName());
        }
        if (entity.getTvgCountry() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getTvgCountry());
        }
        if (entity.getTvgLanguage() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getTvgLanguage());
        }
        statement.bindString(11, entity.getContentType());
        statement.bindString(12, entity.getHeadersJson());
        if (entity.getDrmScheme() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getDrmScheme());
        }
        if (entity.getDrmLicense() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getDrmLicense());
        }
        final int _tmp = entity.isFavorite() ? 1 : 0;
        statement.bindLong(15, _tmp);
        statement.bindLong(16, entity.getOrderIndex());
      }
    };
    this.__preparedStmtOfSetFavorite = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE channels SET isFavorite = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteChannelsByPlaylist = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM channels WHERE playlistId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertChannels(final List<ChannelEntity> channels,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfChannelEntity.insert(channels);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object setFavorite(final long id, final boolean isFav,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetFavorite.acquire();
        int _argIndex = 1;
        final int _tmp = isFav ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, id);
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
          __preparedStmtOfSetFavorite.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteChannelsByPlaylist(final long playlistId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteChannelsByPlaylist.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, playlistId);
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
          __preparedStmtOfDeleteChannelsByPlaylist.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ChannelEntity>> getChannelsByPlaylist(final long playlistId) {
    final String _sql = "SELECT * FROM channels WHERE playlistId = ? ORDER BY orderIndex ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, playlistId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"channels"}, new Callable<List<ChannelEntity>>() {
      @Override
      @NonNull
      public List<ChannelEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPlaylistId = CursorUtil.getColumnIndexOrThrow(_cursor, "playlistId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfLogoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "logoUrl");
          final int _cursorIndexOfGroupTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "groupTitle");
          final int _cursorIndexOfTvgId = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgId");
          final int _cursorIndexOfTvgName = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgName");
          final int _cursorIndexOfTvgCountry = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgCountry");
          final int _cursorIndexOfTvgLanguage = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgLanguage");
          final int _cursorIndexOfContentType = CursorUtil.getColumnIndexOrThrow(_cursor, "contentType");
          final int _cursorIndexOfHeadersJson = CursorUtil.getColumnIndexOrThrow(_cursor, "headersJson");
          final int _cursorIndexOfDrmScheme = CursorUtil.getColumnIndexOrThrow(_cursor, "drmScheme");
          final int _cursorIndexOfDrmLicense = CursorUtil.getColumnIndexOrThrow(_cursor, "drmLicense");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfOrderIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "orderIndex");
          final List<ChannelEntity> _result = new ArrayList<ChannelEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChannelEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpPlaylistId;
            _tmpPlaylistId = _cursor.getLong(_cursorIndexOfPlaylistId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final String _tmpLogoUrl;
            if (_cursor.isNull(_cursorIndexOfLogoUrl)) {
              _tmpLogoUrl = null;
            } else {
              _tmpLogoUrl = _cursor.getString(_cursorIndexOfLogoUrl);
            }
            final String _tmpGroupTitle;
            _tmpGroupTitle = _cursor.getString(_cursorIndexOfGroupTitle);
            final String _tmpTvgId;
            if (_cursor.isNull(_cursorIndexOfTvgId)) {
              _tmpTvgId = null;
            } else {
              _tmpTvgId = _cursor.getString(_cursorIndexOfTvgId);
            }
            final String _tmpTvgName;
            if (_cursor.isNull(_cursorIndexOfTvgName)) {
              _tmpTvgName = null;
            } else {
              _tmpTvgName = _cursor.getString(_cursorIndexOfTvgName);
            }
            final String _tmpTvgCountry;
            if (_cursor.isNull(_cursorIndexOfTvgCountry)) {
              _tmpTvgCountry = null;
            } else {
              _tmpTvgCountry = _cursor.getString(_cursorIndexOfTvgCountry);
            }
            final String _tmpTvgLanguage;
            if (_cursor.isNull(_cursorIndexOfTvgLanguage)) {
              _tmpTvgLanguage = null;
            } else {
              _tmpTvgLanguage = _cursor.getString(_cursorIndexOfTvgLanguage);
            }
            final String _tmpContentType;
            _tmpContentType = _cursor.getString(_cursorIndexOfContentType);
            final String _tmpHeadersJson;
            _tmpHeadersJson = _cursor.getString(_cursorIndexOfHeadersJson);
            final String _tmpDrmScheme;
            if (_cursor.isNull(_cursorIndexOfDrmScheme)) {
              _tmpDrmScheme = null;
            } else {
              _tmpDrmScheme = _cursor.getString(_cursorIndexOfDrmScheme);
            }
            final String _tmpDrmLicense;
            if (_cursor.isNull(_cursorIndexOfDrmLicense)) {
              _tmpDrmLicense = null;
            } else {
              _tmpDrmLicense = _cursor.getString(_cursorIndexOfDrmLicense);
            }
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final int _tmpOrderIndex;
            _tmpOrderIndex = _cursor.getInt(_cursorIndexOfOrderIndex);
            _item = new ChannelEntity(_tmpId,_tmpPlaylistId,_tmpName,_tmpStreamUrl,_tmpLogoUrl,_tmpGroupTitle,_tmpTvgId,_tmpTvgName,_tmpTvgCountry,_tmpTvgLanguage,_tmpContentType,_tmpHeadersJson,_tmpDrmScheme,_tmpDrmLicense,_tmpIsFavorite,_tmpOrderIndex);
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
  public Flow<List<ChannelEntity>> getChannelsByGroup(final long playlistId, final String group) {
    final String _sql = "SELECT * FROM channels WHERE playlistId = ? AND groupTitle = ? ORDER BY orderIndex ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, playlistId);
    _argIndex = 2;
    _statement.bindString(_argIndex, group);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"channels"}, new Callable<List<ChannelEntity>>() {
      @Override
      @NonNull
      public List<ChannelEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPlaylistId = CursorUtil.getColumnIndexOrThrow(_cursor, "playlistId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfLogoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "logoUrl");
          final int _cursorIndexOfGroupTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "groupTitle");
          final int _cursorIndexOfTvgId = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgId");
          final int _cursorIndexOfTvgName = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgName");
          final int _cursorIndexOfTvgCountry = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgCountry");
          final int _cursorIndexOfTvgLanguage = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgLanguage");
          final int _cursorIndexOfContentType = CursorUtil.getColumnIndexOrThrow(_cursor, "contentType");
          final int _cursorIndexOfHeadersJson = CursorUtil.getColumnIndexOrThrow(_cursor, "headersJson");
          final int _cursorIndexOfDrmScheme = CursorUtil.getColumnIndexOrThrow(_cursor, "drmScheme");
          final int _cursorIndexOfDrmLicense = CursorUtil.getColumnIndexOrThrow(_cursor, "drmLicense");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfOrderIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "orderIndex");
          final List<ChannelEntity> _result = new ArrayList<ChannelEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChannelEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpPlaylistId;
            _tmpPlaylistId = _cursor.getLong(_cursorIndexOfPlaylistId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final String _tmpLogoUrl;
            if (_cursor.isNull(_cursorIndexOfLogoUrl)) {
              _tmpLogoUrl = null;
            } else {
              _tmpLogoUrl = _cursor.getString(_cursorIndexOfLogoUrl);
            }
            final String _tmpGroupTitle;
            _tmpGroupTitle = _cursor.getString(_cursorIndexOfGroupTitle);
            final String _tmpTvgId;
            if (_cursor.isNull(_cursorIndexOfTvgId)) {
              _tmpTvgId = null;
            } else {
              _tmpTvgId = _cursor.getString(_cursorIndexOfTvgId);
            }
            final String _tmpTvgName;
            if (_cursor.isNull(_cursorIndexOfTvgName)) {
              _tmpTvgName = null;
            } else {
              _tmpTvgName = _cursor.getString(_cursorIndexOfTvgName);
            }
            final String _tmpTvgCountry;
            if (_cursor.isNull(_cursorIndexOfTvgCountry)) {
              _tmpTvgCountry = null;
            } else {
              _tmpTvgCountry = _cursor.getString(_cursorIndexOfTvgCountry);
            }
            final String _tmpTvgLanguage;
            if (_cursor.isNull(_cursorIndexOfTvgLanguage)) {
              _tmpTvgLanguage = null;
            } else {
              _tmpTvgLanguage = _cursor.getString(_cursorIndexOfTvgLanguage);
            }
            final String _tmpContentType;
            _tmpContentType = _cursor.getString(_cursorIndexOfContentType);
            final String _tmpHeadersJson;
            _tmpHeadersJson = _cursor.getString(_cursorIndexOfHeadersJson);
            final String _tmpDrmScheme;
            if (_cursor.isNull(_cursorIndexOfDrmScheme)) {
              _tmpDrmScheme = null;
            } else {
              _tmpDrmScheme = _cursor.getString(_cursorIndexOfDrmScheme);
            }
            final String _tmpDrmLicense;
            if (_cursor.isNull(_cursorIndexOfDrmLicense)) {
              _tmpDrmLicense = null;
            } else {
              _tmpDrmLicense = _cursor.getString(_cursorIndexOfDrmLicense);
            }
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final int _tmpOrderIndex;
            _tmpOrderIndex = _cursor.getInt(_cursorIndexOfOrderIndex);
            _item = new ChannelEntity(_tmpId,_tmpPlaylistId,_tmpName,_tmpStreamUrl,_tmpLogoUrl,_tmpGroupTitle,_tmpTvgId,_tmpTvgName,_tmpTvgCountry,_tmpTvgLanguage,_tmpContentType,_tmpHeadersJson,_tmpDrmScheme,_tmpDrmLicense,_tmpIsFavorite,_tmpOrderIndex);
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
  public Flow<List<ChannelEntity>> getChannelsByType(final long playlistId, final String type) {
    final String _sql = "SELECT * FROM channels WHERE playlistId = ? AND contentType = ? ORDER BY orderIndex ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, playlistId);
    _argIndex = 2;
    _statement.bindString(_argIndex, type);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"channels"}, new Callable<List<ChannelEntity>>() {
      @Override
      @NonNull
      public List<ChannelEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPlaylistId = CursorUtil.getColumnIndexOrThrow(_cursor, "playlistId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfLogoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "logoUrl");
          final int _cursorIndexOfGroupTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "groupTitle");
          final int _cursorIndexOfTvgId = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgId");
          final int _cursorIndexOfTvgName = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgName");
          final int _cursorIndexOfTvgCountry = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgCountry");
          final int _cursorIndexOfTvgLanguage = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgLanguage");
          final int _cursorIndexOfContentType = CursorUtil.getColumnIndexOrThrow(_cursor, "contentType");
          final int _cursorIndexOfHeadersJson = CursorUtil.getColumnIndexOrThrow(_cursor, "headersJson");
          final int _cursorIndexOfDrmScheme = CursorUtil.getColumnIndexOrThrow(_cursor, "drmScheme");
          final int _cursorIndexOfDrmLicense = CursorUtil.getColumnIndexOrThrow(_cursor, "drmLicense");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfOrderIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "orderIndex");
          final List<ChannelEntity> _result = new ArrayList<ChannelEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChannelEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpPlaylistId;
            _tmpPlaylistId = _cursor.getLong(_cursorIndexOfPlaylistId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final String _tmpLogoUrl;
            if (_cursor.isNull(_cursorIndexOfLogoUrl)) {
              _tmpLogoUrl = null;
            } else {
              _tmpLogoUrl = _cursor.getString(_cursorIndexOfLogoUrl);
            }
            final String _tmpGroupTitle;
            _tmpGroupTitle = _cursor.getString(_cursorIndexOfGroupTitle);
            final String _tmpTvgId;
            if (_cursor.isNull(_cursorIndexOfTvgId)) {
              _tmpTvgId = null;
            } else {
              _tmpTvgId = _cursor.getString(_cursorIndexOfTvgId);
            }
            final String _tmpTvgName;
            if (_cursor.isNull(_cursorIndexOfTvgName)) {
              _tmpTvgName = null;
            } else {
              _tmpTvgName = _cursor.getString(_cursorIndexOfTvgName);
            }
            final String _tmpTvgCountry;
            if (_cursor.isNull(_cursorIndexOfTvgCountry)) {
              _tmpTvgCountry = null;
            } else {
              _tmpTvgCountry = _cursor.getString(_cursorIndexOfTvgCountry);
            }
            final String _tmpTvgLanguage;
            if (_cursor.isNull(_cursorIndexOfTvgLanguage)) {
              _tmpTvgLanguage = null;
            } else {
              _tmpTvgLanguage = _cursor.getString(_cursorIndexOfTvgLanguage);
            }
            final String _tmpContentType;
            _tmpContentType = _cursor.getString(_cursorIndexOfContentType);
            final String _tmpHeadersJson;
            _tmpHeadersJson = _cursor.getString(_cursorIndexOfHeadersJson);
            final String _tmpDrmScheme;
            if (_cursor.isNull(_cursorIndexOfDrmScheme)) {
              _tmpDrmScheme = null;
            } else {
              _tmpDrmScheme = _cursor.getString(_cursorIndexOfDrmScheme);
            }
            final String _tmpDrmLicense;
            if (_cursor.isNull(_cursorIndexOfDrmLicense)) {
              _tmpDrmLicense = null;
            } else {
              _tmpDrmLicense = _cursor.getString(_cursorIndexOfDrmLicense);
            }
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final int _tmpOrderIndex;
            _tmpOrderIndex = _cursor.getInt(_cursorIndexOfOrderIndex);
            _item = new ChannelEntity(_tmpId,_tmpPlaylistId,_tmpName,_tmpStreamUrl,_tmpLogoUrl,_tmpGroupTitle,_tmpTvgId,_tmpTvgName,_tmpTvgCountry,_tmpTvgLanguage,_tmpContentType,_tmpHeadersJson,_tmpDrmScheme,_tmpDrmLicense,_tmpIsFavorite,_tmpOrderIndex);
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
  public Flow<List<String>> getCategories(final long playlistId) {
    final String _sql = "SELECT DISTINCT groupTitle FROM channels WHERE playlistId = ? ORDER BY groupTitle ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, playlistId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"channels"}, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
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
  public Flow<List<ChannelEntity>> getFavoriteChannels() {
    final String _sql = "SELECT * FROM channels WHERE isFavorite = 1 ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"channels"}, new Callable<List<ChannelEntity>>() {
      @Override
      @NonNull
      public List<ChannelEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPlaylistId = CursorUtil.getColumnIndexOrThrow(_cursor, "playlistId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfLogoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "logoUrl");
          final int _cursorIndexOfGroupTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "groupTitle");
          final int _cursorIndexOfTvgId = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgId");
          final int _cursorIndexOfTvgName = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgName");
          final int _cursorIndexOfTvgCountry = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgCountry");
          final int _cursorIndexOfTvgLanguage = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgLanguage");
          final int _cursorIndexOfContentType = CursorUtil.getColumnIndexOrThrow(_cursor, "contentType");
          final int _cursorIndexOfHeadersJson = CursorUtil.getColumnIndexOrThrow(_cursor, "headersJson");
          final int _cursorIndexOfDrmScheme = CursorUtil.getColumnIndexOrThrow(_cursor, "drmScheme");
          final int _cursorIndexOfDrmLicense = CursorUtil.getColumnIndexOrThrow(_cursor, "drmLicense");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfOrderIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "orderIndex");
          final List<ChannelEntity> _result = new ArrayList<ChannelEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChannelEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpPlaylistId;
            _tmpPlaylistId = _cursor.getLong(_cursorIndexOfPlaylistId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final String _tmpLogoUrl;
            if (_cursor.isNull(_cursorIndexOfLogoUrl)) {
              _tmpLogoUrl = null;
            } else {
              _tmpLogoUrl = _cursor.getString(_cursorIndexOfLogoUrl);
            }
            final String _tmpGroupTitle;
            _tmpGroupTitle = _cursor.getString(_cursorIndexOfGroupTitle);
            final String _tmpTvgId;
            if (_cursor.isNull(_cursorIndexOfTvgId)) {
              _tmpTvgId = null;
            } else {
              _tmpTvgId = _cursor.getString(_cursorIndexOfTvgId);
            }
            final String _tmpTvgName;
            if (_cursor.isNull(_cursorIndexOfTvgName)) {
              _tmpTvgName = null;
            } else {
              _tmpTvgName = _cursor.getString(_cursorIndexOfTvgName);
            }
            final String _tmpTvgCountry;
            if (_cursor.isNull(_cursorIndexOfTvgCountry)) {
              _tmpTvgCountry = null;
            } else {
              _tmpTvgCountry = _cursor.getString(_cursorIndexOfTvgCountry);
            }
            final String _tmpTvgLanguage;
            if (_cursor.isNull(_cursorIndexOfTvgLanguage)) {
              _tmpTvgLanguage = null;
            } else {
              _tmpTvgLanguage = _cursor.getString(_cursorIndexOfTvgLanguage);
            }
            final String _tmpContentType;
            _tmpContentType = _cursor.getString(_cursorIndexOfContentType);
            final String _tmpHeadersJson;
            _tmpHeadersJson = _cursor.getString(_cursorIndexOfHeadersJson);
            final String _tmpDrmScheme;
            if (_cursor.isNull(_cursorIndexOfDrmScheme)) {
              _tmpDrmScheme = null;
            } else {
              _tmpDrmScheme = _cursor.getString(_cursorIndexOfDrmScheme);
            }
            final String _tmpDrmLicense;
            if (_cursor.isNull(_cursorIndexOfDrmLicense)) {
              _tmpDrmLicense = null;
            } else {
              _tmpDrmLicense = _cursor.getString(_cursorIndexOfDrmLicense);
            }
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final int _tmpOrderIndex;
            _tmpOrderIndex = _cursor.getInt(_cursorIndexOfOrderIndex);
            _item = new ChannelEntity(_tmpId,_tmpPlaylistId,_tmpName,_tmpStreamUrl,_tmpLogoUrl,_tmpGroupTitle,_tmpTvgId,_tmpTvgName,_tmpTvgCountry,_tmpTvgLanguage,_tmpContentType,_tmpHeadersJson,_tmpDrmScheme,_tmpDrmLicense,_tmpIsFavorite,_tmpOrderIndex);
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
  public Flow<List<ChannelEntity>> getFavoriteChannelsByPlaylist(final long playlistId) {
    final String _sql = "SELECT * FROM channels WHERE playlistId = ? AND isFavorite = 1 ORDER BY orderIndex ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, playlistId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"channels"}, new Callable<List<ChannelEntity>>() {
      @Override
      @NonNull
      public List<ChannelEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPlaylistId = CursorUtil.getColumnIndexOrThrow(_cursor, "playlistId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfLogoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "logoUrl");
          final int _cursorIndexOfGroupTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "groupTitle");
          final int _cursorIndexOfTvgId = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgId");
          final int _cursorIndexOfTvgName = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgName");
          final int _cursorIndexOfTvgCountry = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgCountry");
          final int _cursorIndexOfTvgLanguage = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgLanguage");
          final int _cursorIndexOfContentType = CursorUtil.getColumnIndexOrThrow(_cursor, "contentType");
          final int _cursorIndexOfHeadersJson = CursorUtil.getColumnIndexOrThrow(_cursor, "headersJson");
          final int _cursorIndexOfDrmScheme = CursorUtil.getColumnIndexOrThrow(_cursor, "drmScheme");
          final int _cursorIndexOfDrmLicense = CursorUtil.getColumnIndexOrThrow(_cursor, "drmLicense");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfOrderIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "orderIndex");
          final List<ChannelEntity> _result = new ArrayList<ChannelEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChannelEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpPlaylistId;
            _tmpPlaylistId = _cursor.getLong(_cursorIndexOfPlaylistId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final String _tmpLogoUrl;
            if (_cursor.isNull(_cursorIndexOfLogoUrl)) {
              _tmpLogoUrl = null;
            } else {
              _tmpLogoUrl = _cursor.getString(_cursorIndexOfLogoUrl);
            }
            final String _tmpGroupTitle;
            _tmpGroupTitle = _cursor.getString(_cursorIndexOfGroupTitle);
            final String _tmpTvgId;
            if (_cursor.isNull(_cursorIndexOfTvgId)) {
              _tmpTvgId = null;
            } else {
              _tmpTvgId = _cursor.getString(_cursorIndexOfTvgId);
            }
            final String _tmpTvgName;
            if (_cursor.isNull(_cursorIndexOfTvgName)) {
              _tmpTvgName = null;
            } else {
              _tmpTvgName = _cursor.getString(_cursorIndexOfTvgName);
            }
            final String _tmpTvgCountry;
            if (_cursor.isNull(_cursorIndexOfTvgCountry)) {
              _tmpTvgCountry = null;
            } else {
              _tmpTvgCountry = _cursor.getString(_cursorIndexOfTvgCountry);
            }
            final String _tmpTvgLanguage;
            if (_cursor.isNull(_cursorIndexOfTvgLanguage)) {
              _tmpTvgLanguage = null;
            } else {
              _tmpTvgLanguage = _cursor.getString(_cursorIndexOfTvgLanguage);
            }
            final String _tmpContentType;
            _tmpContentType = _cursor.getString(_cursorIndexOfContentType);
            final String _tmpHeadersJson;
            _tmpHeadersJson = _cursor.getString(_cursorIndexOfHeadersJson);
            final String _tmpDrmScheme;
            if (_cursor.isNull(_cursorIndexOfDrmScheme)) {
              _tmpDrmScheme = null;
            } else {
              _tmpDrmScheme = _cursor.getString(_cursorIndexOfDrmScheme);
            }
            final String _tmpDrmLicense;
            if (_cursor.isNull(_cursorIndexOfDrmLicense)) {
              _tmpDrmLicense = null;
            } else {
              _tmpDrmLicense = _cursor.getString(_cursorIndexOfDrmLicense);
            }
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final int _tmpOrderIndex;
            _tmpOrderIndex = _cursor.getInt(_cursorIndexOfOrderIndex);
            _item = new ChannelEntity(_tmpId,_tmpPlaylistId,_tmpName,_tmpStreamUrl,_tmpLogoUrl,_tmpGroupTitle,_tmpTvgId,_tmpTvgName,_tmpTvgCountry,_tmpTvgLanguage,_tmpContentType,_tmpHeadersJson,_tmpDrmScheme,_tmpDrmLicense,_tmpIsFavorite,_tmpOrderIndex);
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
  public Flow<List<ChannelEntity>> searchChannels(final String query) {
    final String _sql = "SELECT * FROM channels WHERE name LIKE '%' || ? || '%' OR groupTitle LIKE '%' || ? || '%' ORDER BY name ASC LIMIT 100";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, query);
    _argIndex = 2;
    _statement.bindString(_argIndex, query);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"channels"}, new Callable<List<ChannelEntity>>() {
      @Override
      @NonNull
      public List<ChannelEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPlaylistId = CursorUtil.getColumnIndexOrThrow(_cursor, "playlistId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfLogoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "logoUrl");
          final int _cursorIndexOfGroupTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "groupTitle");
          final int _cursorIndexOfTvgId = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgId");
          final int _cursorIndexOfTvgName = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgName");
          final int _cursorIndexOfTvgCountry = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgCountry");
          final int _cursorIndexOfTvgLanguage = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgLanguage");
          final int _cursorIndexOfContentType = CursorUtil.getColumnIndexOrThrow(_cursor, "contentType");
          final int _cursorIndexOfHeadersJson = CursorUtil.getColumnIndexOrThrow(_cursor, "headersJson");
          final int _cursorIndexOfDrmScheme = CursorUtil.getColumnIndexOrThrow(_cursor, "drmScheme");
          final int _cursorIndexOfDrmLicense = CursorUtil.getColumnIndexOrThrow(_cursor, "drmLicense");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfOrderIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "orderIndex");
          final List<ChannelEntity> _result = new ArrayList<ChannelEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChannelEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpPlaylistId;
            _tmpPlaylistId = _cursor.getLong(_cursorIndexOfPlaylistId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final String _tmpLogoUrl;
            if (_cursor.isNull(_cursorIndexOfLogoUrl)) {
              _tmpLogoUrl = null;
            } else {
              _tmpLogoUrl = _cursor.getString(_cursorIndexOfLogoUrl);
            }
            final String _tmpGroupTitle;
            _tmpGroupTitle = _cursor.getString(_cursorIndexOfGroupTitle);
            final String _tmpTvgId;
            if (_cursor.isNull(_cursorIndexOfTvgId)) {
              _tmpTvgId = null;
            } else {
              _tmpTvgId = _cursor.getString(_cursorIndexOfTvgId);
            }
            final String _tmpTvgName;
            if (_cursor.isNull(_cursorIndexOfTvgName)) {
              _tmpTvgName = null;
            } else {
              _tmpTvgName = _cursor.getString(_cursorIndexOfTvgName);
            }
            final String _tmpTvgCountry;
            if (_cursor.isNull(_cursorIndexOfTvgCountry)) {
              _tmpTvgCountry = null;
            } else {
              _tmpTvgCountry = _cursor.getString(_cursorIndexOfTvgCountry);
            }
            final String _tmpTvgLanguage;
            if (_cursor.isNull(_cursorIndexOfTvgLanguage)) {
              _tmpTvgLanguage = null;
            } else {
              _tmpTvgLanguage = _cursor.getString(_cursorIndexOfTvgLanguage);
            }
            final String _tmpContentType;
            _tmpContentType = _cursor.getString(_cursorIndexOfContentType);
            final String _tmpHeadersJson;
            _tmpHeadersJson = _cursor.getString(_cursorIndexOfHeadersJson);
            final String _tmpDrmScheme;
            if (_cursor.isNull(_cursorIndexOfDrmScheme)) {
              _tmpDrmScheme = null;
            } else {
              _tmpDrmScheme = _cursor.getString(_cursorIndexOfDrmScheme);
            }
            final String _tmpDrmLicense;
            if (_cursor.isNull(_cursorIndexOfDrmLicense)) {
              _tmpDrmLicense = null;
            } else {
              _tmpDrmLicense = _cursor.getString(_cursorIndexOfDrmLicense);
            }
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final int _tmpOrderIndex;
            _tmpOrderIndex = _cursor.getInt(_cursorIndexOfOrderIndex);
            _item = new ChannelEntity(_tmpId,_tmpPlaylistId,_tmpName,_tmpStreamUrl,_tmpLogoUrl,_tmpGroupTitle,_tmpTvgId,_tmpTvgName,_tmpTvgCountry,_tmpTvgLanguage,_tmpContentType,_tmpHeadersJson,_tmpDrmScheme,_tmpDrmLicense,_tmpIsFavorite,_tmpOrderIndex);
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
  public Object getChannelById(final long id,
      final Continuation<? super ChannelEntity> $completion) {
    final String _sql = "SELECT * FROM channels WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ChannelEntity>() {
      @Override
      @Nullable
      public ChannelEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPlaylistId = CursorUtil.getColumnIndexOrThrow(_cursor, "playlistId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStreamUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "streamUrl");
          final int _cursorIndexOfLogoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "logoUrl");
          final int _cursorIndexOfGroupTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "groupTitle");
          final int _cursorIndexOfTvgId = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgId");
          final int _cursorIndexOfTvgName = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgName");
          final int _cursorIndexOfTvgCountry = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgCountry");
          final int _cursorIndexOfTvgLanguage = CursorUtil.getColumnIndexOrThrow(_cursor, "tvgLanguage");
          final int _cursorIndexOfContentType = CursorUtil.getColumnIndexOrThrow(_cursor, "contentType");
          final int _cursorIndexOfHeadersJson = CursorUtil.getColumnIndexOrThrow(_cursor, "headersJson");
          final int _cursorIndexOfDrmScheme = CursorUtil.getColumnIndexOrThrow(_cursor, "drmScheme");
          final int _cursorIndexOfDrmLicense = CursorUtil.getColumnIndexOrThrow(_cursor, "drmLicense");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfOrderIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "orderIndex");
          final ChannelEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpPlaylistId;
            _tmpPlaylistId = _cursor.getLong(_cursorIndexOfPlaylistId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpStreamUrl;
            _tmpStreamUrl = _cursor.getString(_cursorIndexOfStreamUrl);
            final String _tmpLogoUrl;
            if (_cursor.isNull(_cursorIndexOfLogoUrl)) {
              _tmpLogoUrl = null;
            } else {
              _tmpLogoUrl = _cursor.getString(_cursorIndexOfLogoUrl);
            }
            final String _tmpGroupTitle;
            _tmpGroupTitle = _cursor.getString(_cursorIndexOfGroupTitle);
            final String _tmpTvgId;
            if (_cursor.isNull(_cursorIndexOfTvgId)) {
              _tmpTvgId = null;
            } else {
              _tmpTvgId = _cursor.getString(_cursorIndexOfTvgId);
            }
            final String _tmpTvgName;
            if (_cursor.isNull(_cursorIndexOfTvgName)) {
              _tmpTvgName = null;
            } else {
              _tmpTvgName = _cursor.getString(_cursorIndexOfTvgName);
            }
            final String _tmpTvgCountry;
            if (_cursor.isNull(_cursorIndexOfTvgCountry)) {
              _tmpTvgCountry = null;
            } else {
              _tmpTvgCountry = _cursor.getString(_cursorIndexOfTvgCountry);
            }
            final String _tmpTvgLanguage;
            if (_cursor.isNull(_cursorIndexOfTvgLanguage)) {
              _tmpTvgLanguage = null;
            } else {
              _tmpTvgLanguage = _cursor.getString(_cursorIndexOfTvgLanguage);
            }
            final String _tmpContentType;
            _tmpContentType = _cursor.getString(_cursorIndexOfContentType);
            final String _tmpHeadersJson;
            _tmpHeadersJson = _cursor.getString(_cursorIndexOfHeadersJson);
            final String _tmpDrmScheme;
            if (_cursor.isNull(_cursorIndexOfDrmScheme)) {
              _tmpDrmScheme = null;
            } else {
              _tmpDrmScheme = _cursor.getString(_cursorIndexOfDrmScheme);
            }
            final String _tmpDrmLicense;
            if (_cursor.isNull(_cursorIndexOfDrmLicense)) {
              _tmpDrmLicense = null;
            } else {
              _tmpDrmLicense = _cursor.getString(_cursorIndexOfDrmLicense);
            }
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final int _tmpOrderIndex;
            _tmpOrderIndex = _cursor.getInt(_cursorIndexOfOrderIndex);
            _result = new ChannelEntity(_tmpId,_tmpPlaylistId,_tmpName,_tmpStreamUrl,_tmpLogoUrl,_tmpGroupTitle,_tmpTvgId,_tmpTvgName,_tmpTvgCountry,_tmpTvgLanguage,_tmpContentType,_tmpHeadersJson,_tmpDrmScheme,_tmpDrmLicense,_tmpIsFavorite,_tmpOrderIndex);
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

  @Override
  public Object getChannelCount(final long playlistId,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM channels WHERE playlistId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, playlistId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
