package com.genuine.nsplayer.data.local;

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
  private volatile PlaylistDao _playlistDao;

  private volatile ChannelDao _channelDao;

  private volatile HistoryDao _historyDao;

  private volatile EpgDao _epgDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `playlists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `url` TEXT, `filePath` TEXT, `userAgent` TEXT, `channelCount` INTEGER NOT NULL, `lastUpdated` INTEGER NOT NULL, `isEnabled` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `channels` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `playlistId` INTEGER NOT NULL, `name` TEXT NOT NULL, `streamUrl` TEXT NOT NULL, `logoUrl` TEXT, `groupTitle` TEXT NOT NULL, `tvgId` TEXT, `tvgName` TEXT, `tvgCountry` TEXT, `tvgLanguage` TEXT, `contentType` TEXT NOT NULL, `headersJson` TEXT NOT NULL, `drmScheme` TEXT, `drmLicense` TEXT, `isFavorite` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_channels_playlistId` ON `channels` (`playlistId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_channels_groupTitle` ON `channels` (`groupTitle`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_channels_isFavorite` ON `channels` (`isFavorite`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_channels_contentType` ON `channels` (`contentType`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `playback_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `channelId` INTEGER, `streamUrl` TEXT NOT NULL, `title` TEXT NOT NULL, `logoUrl` TEXT, `positionMs` INTEGER NOT NULL, `durationMs` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_playback_history_streamUrl` ON `playback_history` (`streamUrl`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `epg_programs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `channelTvgId` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT, `startEpoch` INTEGER NOT NULL, `endEpoch` INTEGER NOT NULL, `category` TEXT)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_epg_programs_channelTvgId` ON `epg_programs` (`channelTvgId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_epg_programs_startEpoch` ON `epg_programs` (`startEpoch`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_epg_programs_endEpoch` ON `epg_programs` (`endEpoch`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '3e6686ce8c38ffd98d6193d95022fbb0')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `playlists`");
        db.execSQL("DROP TABLE IF EXISTS `channels`");
        db.execSQL("DROP TABLE IF EXISTS `playback_history`");
        db.execSQL("DROP TABLE IF EXISTS `epg_programs`");
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
        final HashMap<String, TableInfo.Column> _columnsPlaylists = new HashMap<String, TableInfo.Column>(8);
        _columnsPlaylists.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("url", new TableInfo.Column("url", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("filePath", new TableInfo.Column("filePath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("userAgent", new TableInfo.Column("userAgent", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("channelCount", new TableInfo.Column("channelCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("lastUpdated", new TableInfo.Column("lastUpdated", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("isEnabled", new TableInfo.Column("isEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPlaylists = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPlaylists = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPlaylists = new TableInfo("playlists", _columnsPlaylists, _foreignKeysPlaylists, _indicesPlaylists);
        final TableInfo _existingPlaylists = TableInfo.read(db, "playlists");
        if (!_infoPlaylists.equals(_existingPlaylists)) {
          return new RoomOpenHelper.ValidationResult(false, "playlists(com.genuine.nsplayer.data.local.PlaylistEntity).\n"
                  + " Expected:\n" + _infoPlaylists + "\n"
                  + " Found:\n" + _existingPlaylists);
        }
        final HashMap<String, TableInfo.Column> _columnsChannels = new HashMap<String, TableInfo.Column>(16);
        _columnsChannels.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("playlistId", new TableInfo.Column("playlistId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("streamUrl", new TableInfo.Column("streamUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("logoUrl", new TableInfo.Column("logoUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("groupTitle", new TableInfo.Column("groupTitle", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("tvgId", new TableInfo.Column("tvgId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("tvgName", new TableInfo.Column("tvgName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("tvgCountry", new TableInfo.Column("tvgCountry", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("tvgLanguage", new TableInfo.Column("tvgLanguage", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("contentType", new TableInfo.Column("contentType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("headersJson", new TableInfo.Column("headersJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("drmScheme", new TableInfo.Column("drmScheme", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("drmLicense", new TableInfo.Column("drmLicense", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("isFavorite", new TableInfo.Column("isFavorite", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("orderIndex", new TableInfo.Column("orderIndex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysChannels = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesChannels = new HashSet<TableInfo.Index>(4);
        _indicesChannels.add(new TableInfo.Index("index_channels_playlistId", false, Arrays.asList("playlistId"), Arrays.asList("ASC")));
        _indicesChannels.add(new TableInfo.Index("index_channels_groupTitle", false, Arrays.asList("groupTitle"), Arrays.asList("ASC")));
        _indicesChannels.add(new TableInfo.Index("index_channels_isFavorite", false, Arrays.asList("isFavorite"), Arrays.asList("ASC")));
        _indicesChannels.add(new TableInfo.Index("index_channels_contentType", false, Arrays.asList("contentType"), Arrays.asList("ASC")));
        final TableInfo _infoChannels = new TableInfo("channels", _columnsChannels, _foreignKeysChannels, _indicesChannels);
        final TableInfo _existingChannels = TableInfo.read(db, "channels");
        if (!_infoChannels.equals(_existingChannels)) {
          return new RoomOpenHelper.ValidationResult(false, "channels(com.genuine.nsplayer.data.local.ChannelEntity).\n"
                  + " Expected:\n" + _infoChannels + "\n"
                  + " Found:\n" + _existingChannels);
        }
        final HashMap<String, TableInfo.Column> _columnsPlaybackHistory = new HashMap<String, TableInfo.Column>(8);
        _columnsPlaybackHistory.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaybackHistory.put("channelId", new TableInfo.Column("channelId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaybackHistory.put("streamUrl", new TableInfo.Column("streamUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaybackHistory.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaybackHistory.put("logoUrl", new TableInfo.Column("logoUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaybackHistory.put("positionMs", new TableInfo.Column("positionMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaybackHistory.put("durationMs", new TableInfo.Column("durationMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaybackHistory.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPlaybackHistory = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPlaybackHistory = new HashSet<TableInfo.Index>(1);
        _indicesPlaybackHistory.add(new TableInfo.Index("index_playback_history_streamUrl", true, Arrays.asList("streamUrl"), Arrays.asList("ASC")));
        final TableInfo _infoPlaybackHistory = new TableInfo("playback_history", _columnsPlaybackHistory, _foreignKeysPlaybackHistory, _indicesPlaybackHistory);
        final TableInfo _existingPlaybackHistory = TableInfo.read(db, "playback_history");
        if (!_infoPlaybackHistory.equals(_existingPlaybackHistory)) {
          return new RoomOpenHelper.ValidationResult(false, "playback_history(com.genuine.nsplayer.data.local.HistoryEntity).\n"
                  + " Expected:\n" + _infoPlaybackHistory + "\n"
                  + " Found:\n" + _existingPlaybackHistory);
        }
        final HashMap<String, TableInfo.Column> _columnsEpgPrograms = new HashMap<String, TableInfo.Column>(7);
        _columnsEpgPrograms.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpgPrograms.put("channelTvgId", new TableInfo.Column("channelTvgId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpgPrograms.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpgPrograms.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpgPrograms.put("startEpoch", new TableInfo.Column("startEpoch", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpgPrograms.put("endEpoch", new TableInfo.Column("endEpoch", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpgPrograms.put("category", new TableInfo.Column("category", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysEpgPrograms = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesEpgPrograms = new HashSet<TableInfo.Index>(3);
        _indicesEpgPrograms.add(new TableInfo.Index("index_epg_programs_channelTvgId", false, Arrays.asList("channelTvgId"), Arrays.asList("ASC")));
        _indicesEpgPrograms.add(new TableInfo.Index("index_epg_programs_startEpoch", false, Arrays.asList("startEpoch"), Arrays.asList("ASC")));
        _indicesEpgPrograms.add(new TableInfo.Index("index_epg_programs_endEpoch", false, Arrays.asList("endEpoch"), Arrays.asList("ASC")));
        final TableInfo _infoEpgPrograms = new TableInfo("epg_programs", _columnsEpgPrograms, _foreignKeysEpgPrograms, _indicesEpgPrograms);
        final TableInfo _existingEpgPrograms = TableInfo.read(db, "epg_programs");
        if (!_infoEpgPrograms.equals(_existingEpgPrograms)) {
          return new RoomOpenHelper.ValidationResult(false, "epg_programs(com.genuine.nsplayer.data.local.EpgEntity).\n"
                  + " Expected:\n" + _infoEpgPrograms + "\n"
                  + " Found:\n" + _existingEpgPrograms);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "3e6686ce8c38ffd98d6193d95022fbb0", "e78b1d36e90920f9398d36c6d70ee68f");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "playlists","channels","playback_history","epg_programs");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `playlists`");
      _db.execSQL("DELETE FROM `channels`");
      _db.execSQL("DELETE FROM `playback_history`");
      _db.execSQL("DELETE FROM `epg_programs`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
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
    _typeConvertersMap.put(PlaylistDao.class, PlaylistDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ChannelDao.class, ChannelDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(HistoryDao.class, HistoryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(EpgDao.class, EpgDao_Impl.getRequiredConverters());
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
  public PlaylistDao playlistDao() {
    if (_playlistDao != null) {
      return _playlistDao;
    } else {
      synchronized(this) {
        if(_playlistDao == null) {
          _playlistDao = new PlaylistDao_Impl(this);
        }
        return _playlistDao;
      }
    }
  }

  @Override
  public ChannelDao channelDao() {
    if (_channelDao != null) {
      return _channelDao;
    } else {
      synchronized(this) {
        if(_channelDao == null) {
          _channelDao = new ChannelDao_Impl(this);
        }
        return _channelDao;
      }
    }
  }

  @Override
  public HistoryDao historyDao() {
    if (_historyDao != null) {
      return _historyDao;
    } else {
      synchronized(this) {
        if(_historyDao == null) {
          _historyDao = new HistoryDao_Impl(this);
        }
        return _historyDao;
      }
    }
  }

  @Override
  public EpgDao epgDao() {
    if (_epgDao != null) {
      return _epgDao;
    } else {
      synchronized(this) {
        if(_epgDao == null) {
          _epgDao = new EpgDao_Impl(this);
        }
        return _epgDao;
      }
    }
  }
}
