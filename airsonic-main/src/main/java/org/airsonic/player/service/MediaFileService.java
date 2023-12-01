/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2023 (C) Y.Tory, Yetangitu
 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.service;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import org.airsonic.player.ajax.MediaFileEntry;
import org.airsonic.player.domain.*;
import org.airsonic.player.domain.CoverArt.EntityType;
import org.airsonic.player.domain.MediaFile.MediaType;
import org.airsonic.player.domain.MusicFolder.Type;
import org.airsonic.player.domain.entity.StarredMediaFile;
import org.airsonic.player.domain.entity.UserRating;
import org.airsonic.player.i18n.LocaleResolver;
import org.airsonic.player.repository.AlbumRepository;
import org.airsonic.player.repository.ArtistRepository;
import org.airsonic.player.repository.GenreRepository;
import org.airsonic.player.repository.MediaFileRepository;
import org.airsonic.player.repository.MusicFileInfoRepository;
import org.airsonic.player.repository.OffsetBasedPageRequest;
import org.airsonic.player.repository.RandomMediaFileRepository;
import org.airsonic.player.repository.StarredMediaFileRepository;
import org.airsonic.player.repository.UserRatingRepository;
import org.airsonic.player.service.metadata.JaudiotaggerParser;
import org.airsonic.player.service.metadata.MetaData;
import org.airsonic.player.service.metadata.MetaDataParser;
import org.airsonic.player.service.metadata.MetaDataParserFactory;
import org.airsonic.player.service.search.IndexManager;
import org.airsonic.player.util.FileUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.digitalmediaserver.cuelib.CueParser;
import org.digitalmediaserver.cuelib.CueSheet;
import org.digitalmediaserver.cuelib.Position;
import org.digitalmediaserver.cuelib.TrackData;
import org.digitalmediaserver.cuelib.io.FLACReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides services for instantiating and caching media files and cover art.
 *
 * @author Sindre Mehus
 */
@Service
@Transactional
public class MediaFileService {

    private static final Logger LOG = LoggerFactory.getLogger(MediaFileService.class);

    @Autowired
    private SecurityService securityService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private MediaFolderService mediaFolderService;
    @Autowired
    private RandomMediaFileRepository randomMediaFileRepository;
    @Autowired
    private UserRatingRepository userRatingRepository;
    @Autowired
    private AlbumRepository albumRepository;
    @Autowired
    private JaudiotaggerParser parser;
    @Autowired
    private MetaDataParserFactory metaDataParserFactory;
    @Autowired
    private CoverArtService coverArtService;
    @Autowired
    private MediaFileRepository mediaFileRepository;
    @Autowired
    private StarredMediaFileRepository starredMediaFileRepository;
    @Autowired
    private LocaleResolver localeResolver;
    @Autowired
    private MusicFileInfoRepository musicFileInfoRepository;
    @Autowired
    private GenreRepository genreRepository;
    @Autowired
    private IndexManager indexManager;
    @Autowired
    private ArtistRepository artistRepository;

    private boolean memoryCacheEnabled = true;

    public MediaFile getMediaFile(String pathName) {
        return getMediaFile(Paths.get(pathName));
    }

    public MediaFile getMediaFile(Path fullPath) {
        return getMediaFile(fullPath, settingsService.isFastCacheEnabled());
    }

    // This may be an expensive op
    public MediaFile getMediaFile(Path fullPath, boolean minimizeDiskAccess) {
        MusicFolder folder = mediaFolderService.getMusicFolderForFile(fullPath, true, true);
        if (folder == null) {
            // can't look outside folders and not present in folder
            return null;
        }
        try {
            Path relativePath = folder.getPath().relativize(fullPath);
            return getMediaFile(relativePath, folder, minimizeDiskAccess);
        } catch (Exception e) {
            // ignore
            return null;
        }
    }

    public MediaFile getMediaFile(String relativePath, Integer folderId) {
        return getMediaFile(relativePath, mediaFolderService.getMusicFolderById(folderId));
    }

    public MediaFile getMediaFile(String relativePath, MusicFolder folder) {
        return getMediaFile(Paths.get(relativePath), folder);
    }

    public MediaFile getMediaFile(Path relativePath, MusicFolder folder) {
        return getMediaFile(relativePath, folder, settingsService.isFastCacheEnabled());
    }

    public MediaFile getMediaFile(Path relativePath, MusicFolder folder, boolean minimizeDiskAccess) {
        return getMediaFile(relativePath, folder, MediaFile.NOT_INDEXED, minimizeDiskAccess);
    }

    @Cacheable(cacheNames = "mediaFilePathCache", key = "#relativePath.toString().concat('-').concat(#folder.id).concat('-').concat(#startPosition == null ? '' : #startPosition.toString())", condition = "#root.target.memoryCacheEnabled", unless = "#result == null")
    public MediaFile getMediaFile(Path relativePath, MusicFolder folder, Double startPosition, boolean minimizeDiskAccess) {
        // Look in database.
        return mediaFileRepository.findByPathAndFolderAndStartPosition(relativePath.toString(), folder, startPosition)
            .map(file -> checkLastModified(file, minimizeDiskAccess))
            .orElseGet(() -> {
                if (!Files.exists(folder.getPath().resolve(relativePath))) {
                    return null;
                }
                if (startPosition > MediaFile.NOT_INDEXED) {
                    return null;
                }
                // Not found in database, must read from disk.
                MediaFile mediaFile = createMediaFileByFile(relativePath, folder);
                // Put in database.
                if (mediaFile != null) {
                    updateMediaFile(mediaFile);
                }
                return mediaFile;
            });
    }

    @Cacheable(cacheNames = "mediaFileIdCache", condition = "#root.target.memoryCacheEnabled", unless = "#result == null")
    public MediaFile getMediaFile(Integer id) {
        if (Objects.isNull(id)) return null;
        return mediaFileRepository.findById(id).map(mediaFile -> checkLastModified(mediaFile, settingsService.isFastCacheEnabled())).orElse(null);
    }

    public List<MediaFile> getMediaFilesByRelativePath(Path relativePath) {
        return mediaFileRepository.findByPath(relativePath.toString());
    }

    public MediaFile getParentOf(MediaFile mediaFile) {
        return getParentOf(mediaFile, settingsService.isFastCacheEnabled());
    }

    public MediaFile getParentOf(MediaFile mediaFile, boolean minimizeDiskAccess) {
        if (mediaFile.getParentPath() == null) {
            return null;
        }
        return getMediaFile(Paths.get(mediaFile.getParentPath()), mediaFile.getFolder(), minimizeDiskAccess);
    }

    private boolean needsUpdate(MediaFile mediaFile, boolean minimizeDiskAccess) {
        return minimizeDiskAccess
                && !mediaFile.isIndexedTrack() // ignore virtual track
                && (mediaFile.getVersion() < MediaFile.VERSION
                    || settingsService.getFullScan()
                    || mediaFile.getChanged().truncatedTo(ChronoUnit.MICROS).compareTo(FileUtil.lastModified(mediaFile.getFullPath()).truncatedTo(ChronoUnit.MICROS)) < 0
                    || (!mediaFile.hasIndex() || mediaFile.getChanged().truncatedTo(ChronoUnit.MICROS).compareTo(FileUtil.lastModified(mediaFile.getFullIndexPath()).truncatedTo(ChronoUnit.MICROS)) < 0)
                );
    }

    private MediaFile checkLastModified(MediaFile mediaFile, boolean minimizeDiskAccess) {
        MusicFolder folder = mediaFile.getFolder();
        if (!needsUpdate(mediaFile, minimizeDiskAccess)) {
            LOG.debug("Detected unmodified file (id {}, path {} in folder {} ({}))", mediaFile.getId(), mediaFile.getPath(), folder.getId(), folder.getName());
            return mediaFile;
        }
        LOG.debug("Updating database file from disk (id {}, path {} in folder {} ({}))", mediaFile.getId(), mediaFile.getPath(), folder.getId(), folder.getName());
        if (!Files.exists(mediaFile.getFullPath())) {
            mediaFile.setPresent(false);
            mediaFile.setChildrenLastUpdated(Instant.ofEpochMilli(1));
            updateMediaFile(mediaFile);
        } else {
            if (mediaFile.hasIndex()) {
                if (!Files.exists(mediaFile.getFullIndexPath())) {
                    // Delete children that no longer exist on disk
                    mediaFile.setPresent(true);
                    mediaFile.setChildrenLastUpdated(Instant.ofEpochMilli(1));
                    mediaFile.setIndexPath(null);
                    updateMediaFile(mediaFile);
                } else {
                    // update media file
                    Instant mediaChanged = FileUtil.lastModified(mediaFile.getFullPath());
                    Instant cueChanged = FileUtil.lastModified(mediaFile.getFullIndexPath());
                    // update cue tracks
                    try {
                        createIndexedTracks(mediaFile);
                        // update media file
                        mediaFile.setChanged(mediaChanged.compareTo(cueChanged) >= 0 ? mediaChanged : cueChanged);
                        updateMediaFile(mediaFile);
                    } catch (Exception e) {
                        LOG.error("create indexed tracks error: {}", mediaFile.getFullPath(), e);
                    }
                }
            } else {
                mediaFile = updateMediaFileByFile(mediaFile);
                updateMediaFile(mediaFile);
            }
        }
        return mediaFile;
    }

    /**
     * Returns all user-visible media files that are children of a given media file
     *
     * visibility depends on the return value of showMediaFile(mediaFile)
     *
     * @param sort               Whether to sort files in the same directory
     * @return All children media files which pass this::showMediaFile
     */
    public List<MediaFile> getVisibleChildrenOf(MediaFile parent, boolean includeDirectories, boolean sort) {
        return getChildrenOf(parent, true, includeDirectories, sort).stream()
                .filter(this::showMediaFile)
                .collect(Collectors.toList());
    }

    /**
     * Returns all media files that are children of a given media file.
     *
     * @param includeFiles       Whether files should be included in the result.
     * @param includeDirectories Whether directories should be included in the result.
     * @param sort               Whether to sort files in the same directory.
     * @return All children media files.
     */
    public List<MediaFile> getChildrenOf(MediaFile parent, boolean includeFiles, boolean includeDirectories, boolean sort) {
        return getChildrenOf(parent, includeFiles, includeDirectories, sort, settingsService.isFastCacheEnabled());
    }

    /**
     * Returns all media files that are children of a given media file.
     *
     * @param includeFiles       Whether files should be included in the result.
     * @param includeDirectories Whether directories should be included in the result.
     * @param sort               Whether to sort files in the same directory.
     * @param minimizeDiskAccess Whether to refrain from checking for new or changed files
     * @return All children media files.
     */
    public List<MediaFile> getChildrenOf(MediaFile parent, boolean includeFiles, boolean includeDirectories, boolean sort, boolean minimizeDiskAccess) {

        if (!parent.isDirectory()) {
            return Collections.emptyList();
        }

        Stream<MediaFile> resultStream = null;

        // Make sure children are stored and up-to-date in the database.
        try {
            if (!minimizeDiskAccess) {
                resultStream = Optional.ofNullable(updateChildren(parent)).map(x -> x.parallelStream()).orElse(null);
            }

            if (resultStream == null) {
                resultStream = mediaFileRepository.findByFolderAndParentPathAndPresentTrue(parent.getFolder(), parent.getPath(), Sort.by("startPosition")).parallelStream()
                        .map(x -> checkLastModified(x, minimizeDiskAccess))
                        .filter(this::includeMediaFile);
            }

            resultStream = resultStream.filter(x -> (includeDirectories && x.isDirectory()) || (includeFiles && x.isFile()));

            if (sort) {
                resultStream = resultStream.sorted(new MediaFileComparator(settingsService.isSortAlbumsByYear()));
            }

            return resultStream.collect(Collectors.toList());
        } catch (Exception e) {
            LOG.error("get children of {} failed", parent.getPath(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Returns all songs in the album
     *
     * @param artist The album artist name.
     * @param album The album name.
     * @return All songs in the album.
     */
    public List<MediaFile> getSongsForAlbum(String artist, String album) {
        return mediaFileRepository.findByAlbumArtistAndAlbumNameAndMediaTypeInAndPresentTrue(artist, album, MediaType.audioTypes(), Sort.by("discNumber", "trackNumber"));
    }

    /**
     * Returns songs in a genre.
     *
     * @param offset      Number of songs to skip.
     * @param count      Maximum number of songs to return.
     * @param genre      The genre name.
     * @param musicFolders Only return songs in these folders.
     * @return Songs in the genre.
     */
    public List<MediaFile> getSongsByGenre(int offset, int count, String genre, List<MusicFolder> musicFolders) {
        if (CollectionUtils.isEmpty(musicFolders)) {
            return Collections.emptyList();
        }
        return mediaFileRepository.findByFolderInAndMediaTypeInAndGenreAndPresentTrue(musicFolders, MediaType.audioTypes(), genre, new OffsetBasedPageRequest(offset, count, Sort.by("id")));
    }

    /**
     * Returns songs by a given artist.
     *
     * @param offset      Number of songs to skip.
     * @param count     Maximum number of songs to return.
     * @param artist    The artist name.
     * @return Songs by the artist.
     */
    public List<MediaFile> getSongsByArtist(int offset, int count, String artist) {
        return mediaFileRepository.findByMediaTypeInAndArtistAndPresentTrue(MediaType.audioTypes(), artist, new OffsetBasedPageRequest(offset, count, Sort.by("id")));
    }

    /**
     * Returns song by a given artist and title.
     * @param artist The artist name.
     * @param title The title name.
     * @param musicFolders Only return songs in these folders.
     * @return Song by the artist and title.
     */
    public MediaFile getSongByArtistAndTitle(String artist, String title, List<MusicFolder> musicFolders) {
        if (CollectionUtils.isEmpty(musicFolders) || StringUtils.isBlank(artist) || StringUtils.isBlank(title)) {
            return null;
        }
        List<MediaFile> results = mediaFileRepository.findByFolderInAndMediaTypeAndArtistAndTitleAndPresentTrue(musicFolders, MediaType.MUSIC, artist, title);
        if (results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

    /**
     * Returns the most recently starred songs.
     *
     * @param offset       Number of songs to skip.
     * @param count        Maximum number of songs to return.
     * @param username     Returns songs starred by this user.
     * @param musicFolders Only return songs from these folders.
     * @return The most recently starred songs for this user.
     */
    public List<MediaFile> getStarredSongs(int offset, int count, String username, List<MusicFolder> musicFolders) {
        if (CollectionUtils.isEmpty(musicFolders)) {
            return Collections.emptyList();
        }
        return starredMediaFileRepository
                .findByUsernameAndMediaFileMediaTypeInAndMediaFileFolderInAndMediaFilePresentTrue(username,
                        MediaType.audioTypes(), musicFolders,
                        new OffsetBasedPageRequest(offset, count, Sort.by("created").descending().and(Sort.by("id"))))
                .stream().map(StarredMediaFile::getMediaFile).collect(Collectors.toList());
    }

    /**
     * Returns artist info for the given artist.
     *
     * @param artist The artist name.
     * @param folders The music folders to search.
     * @return Artist info for the given artist.
     */
    public MediaFile getArtistByName(String artist, List<MusicFolder> folders) {
        if (CollectionUtils.isEmpty(folders)) {
            return null;
        }

        List<MediaFile> results = mediaFileRepository.findByFolderInAndMediaTypeAndArtistAndPresentTrue(folders,
                MediaType.DIRECTORY, artist);

        if (results.isEmpty()) {
            return null;
        }
        // return the first result
        return results.get(0);
    }

    /**
     * Returns the most recently starred artists.
     *
     * @param offset       Number of artists to skip.
     * @param count        Maximum number of artists to return.
     * @param username     Returns artists starred by this user.
     * @param musicFolders Only return artists from these folders.
     * @return The most recently starred artists for this user.
     */
    public List<MediaFile> getStarredArtists(int offset, int count, String username, List<MusicFolder> musicFolders) {
        if (CollectionUtils.isEmpty(musicFolders)) {
            return Collections.emptyList();
        }
        return starredMediaFileRepository
                .findByUsernameAndMediaFileMediaTypeAndMediaFileFolderInAndMediaFilePresentTrue(username,
                        MediaType.DIRECTORY, musicFolders,
                        new OffsetBasedPageRequest(offset, count, Sort.by("created").descending().and(Sort.by("id"))))
                .stream().map(StarredMediaFile::getMediaFile).collect(Collectors.toList());
    }

     /**
     * Returns all videos in folders
     *
     * @param artist The album artist name.
     * @param album The album name.
     * @return All songs in the album.
     */
    public List<MediaFile> getVideos(List<MusicFolder> folders, int count, int offset) {
        if (CollectionUtils.isEmpty(folders)) {
            return Collections.emptyList();
        }
        return mediaFileRepository.findByFolderInAndMediaTypeAndPresentTrue(folders, MediaType.VIDEO, new OffsetBasedPageRequest(offset, count, Sort.by("title")));
    }


    /**
     * Returns whether the given file is the root of a media folder.
     *
     * @param mediaFile The file in question. Must not be {@code null}.
     * @return Whether the given file is the root of a media folder.
     * @see MusicFolder
     */
    public boolean isRoot(MediaFile mediaFile) {
        return StringUtils.isEmpty(mediaFile.getPath()) &&
                mediaFolderService.getAllMusicFolders(true, true).parallelStream()
                        .anyMatch(x -> mediaFile.getFolder().getId().equals(x.getId()));
    }

    /**
     * Returns all genres in the music collection.
     *
     * @param sortByAlbum Whether to sort by album count, rather than song count.
     * @return Sorted list of genres.
     */
    public List<Genre> getGenres(boolean sortByAlbum) {
        Sort sort = sortByAlbum ? Sort.by("albumCount") : Sort.by("songCount");
        return genreRepository.findAll(sort.and(Sort.by(Direction.ASC, "name")));
    }

    /**
     * update genres
     *
     * @param genres The genres to update.
     * @return The updated genres.
     */
    public List<Genre> updateGenres(List <Genre> genres) {
        return genreRepository.saveAll(genres);
    }

    /**
     * Returns the most frequently played albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param musicFolders Only return albums in these folders.
     * @return The most frequently played albums.
     */
    public List<MediaFile> getMostFrequentlyPlayedAlbums(int offset, int count, List<MusicFolder> musicFolders) {
        if (CollectionUtils.isEmpty(musicFolders)) {
            return Collections.emptyList();
        }
        return mediaFileRepository.findByFolderInAndMediaTypeAndPlayCountGreaterThanAndPresentTrue(musicFolders, MediaType.ALBUM, 0, new OffsetBasedPageRequest(offset, count, Sort.by("playCount").descending().and(Sort.by("id"))));
    }

    /**
     * Returns the most recently played albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param musicFolders Only return albums in these folders.
     * @return The most recently played albums.
     */
    public List<MediaFile> getMostRecentlyPlayedAlbums(int offset, int count, List<MusicFolder> musicFolders) {
        if (CollectionUtils.isEmpty(musicFolders)) {
            return Collections.emptyList();
        }
        return mediaFileRepository.findByFolderInAndMediaTypeAndPlayCountGreaterThanAndPresentTrue(musicFolders, MediaType.ALBUM, 0, new OffsetBasedPageRequest(offset, count, Sort.by("lastPlayed").descending().and(Sort.by("id"))));
    }

    /**
     * Returns the most recently added albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param musicFolders Only return albums in these folders.
     * @return The most recently added albums.
     */
    public List<MediaFile> getNewestAlbums(int offset, int count, List<MusicFolder> musicFolders) {
        if (CollectionUtils.isEmpty(musicFolders)) {
            return Collections.emptyList();
        }
        return mediaFileRepository.findByFolderInAndMediaTypeAndPresentTrue(musicFolders, MediaType.ALBUM, new OffsetBasedPageRequest(offset, count, Sort.by("created").descending().and(Sort.by("id"))));
    }

    /**
     * Returns the most recently starred albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param username     Returns albums starred by this user.
     * @param musicFolders Only return albums from these folders.
     * @return The most recently starred albums for this user.
     */
    public List<MediaFile> getStarredAlbums(int offset, int count, String username, List<MusicFolder> musicFolders) {
        if (CollectionUtils.isEmpty(musicFolders)) {
            return Collections.emptyList();
        }
        return starredMediaFileRepository
                .findByUsernameAndMediaFileMediaTypeAndMediaFileFolderInAndMediaFilePresentTrue(username,
                        MediaType.ALBUM, musicFolders,
                        new OffsetBasedPageRequest(offset, count, Sort.by("created").descending().and(Sort.by("id"))))
                .stream().map(StarredMediaFile::getMediaFile).collect(Collectors.toList());
    }

    /**
     * Returns albums in alphabetical order.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param byArtist     Whether to sort by artist name
     * @param musicFolders Only return albums in these folders.
     * @return Albums in alphabetical order.
     */
    public List<MediaFile> getAlphabeticalAlbums(int offset, int count, boolean byArtist, List<MusicFolder> musicFolders) {
        if (CollectionUtils.isEmpty(musicFolders)) {
            return Collections.emptyList();
        }
        Sort sort = byArtist ? Sort.by("artist", "albumName", "id") : Sort.by("albumName", "id");
        return mediaFileRepository.findByFolderInAndMediaTypeAndPresentTrue(musicFolders, MediaType.ALBUM, new OffsetBasedPageRequest(offset, count, sort));
    }

    /**
     * Returns albums within a year range.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param fromYear     The first year in the range.
     * @param toYear       The last year in the range.
     * @param musicFolders Only return albums in these folders.
     * @return Albums in the year range.
     */
    public List<MediaFile> getAlbumsByYear(int offset, int count, int fromYear, int toYear, List<MusicFolder> musicFolders) {

        if (CollectionUtils.isEmpty(musicFolders)) {
            return Collections.emptyList();
        }

        if (fromYear <= toYear) {
            return mediaFileRepository.findByFolderInAndMediaTypeAndYearBetweenAndPresentTrue(musicFolders, MediaType.ALBUM, fromYear, toYear, new OffsetBasedPageRequest(offset, count, Sort.by("year", "id")));
        } else {
            return mediaFileRepository.findByFolderInAndMediaTypeAndYearBetweenAndPresentTrue(musicFolders, MediaType.ALBUM, toYear, fromYear, new OffsetBasedPageRequest(offset, count, Sort.by("year").descending().and(Sort.by("id"))));
        }
    }

    /**
     * Returns albums in a genre.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param genre        The genre name.
     * @param musicFolders Only return albums in these folders.
     * @return Albums in the genre.
     */
    public List<MediaFile> getAlbumsByGenre(int offset, int count, String genre, List<MusicFolder> musicFolders) {
        if (CollectionUtils.isEmpty(musicFolders)) {
            return Collections.emptyList();
        }
        return mediaFileRepository.findByFolderInAndMediaTypeAndGenreAndPresentTrue(musicFolders, MediaType.ALBUM, genre, new OffsetBasedPageRequest(offset, count, Sort.by("id")));
    }


    /**
     * Returns random songs for the given parent.
     *
     * @param parent The parent.
     * @param count  Max number of songs to return.
     * @return Random songs.
     */
    public List<MediaFile> getRandomSongsForParent(MediaFile parent, int count) {
        List<MediaFile> children = getDescendantsOf(parent, false);
        removeVideoFiles(children);

        if (children.isEmpty()) {
            return children;
        }
        Collections.shuffle(children);
        return children.subList(0, Math.min(count, children.size()));
    }

    /**
     * Returns random songs matching search criteria.
     *
     */
    public List<MediaFile> getRandomSongs(RandomSearchCriteria criteria, String username) {
        if (criteria == null || CollectionUtils.isEmpty(criteria.getMusicFolders())) {
            return Collections.emptyList();
        }
        boolean joinAlbumRating = criteria.getMinAlbumRating() != null || criteria.getMaxAlbumRating() != null;
        boolean joinStarred = criteria.isShowStarredSongs() ^ criteria.isShowUnstarredSongs();

        List<Integer> starredFileIds = new ArrayList<>();
        List<Integer> fileIds = new ArrayList<>();

        if (joinAlbumRating) {
            Integer minAlbumRating = criteria.getMinAlbumRating() == null ? 0 : criteria.getMinAlbumRating();
            Integer maxAlbumRating = criteria.getMaxAlbumRating() == null ? 5 : criteria.getMaxAlbumRating();
            List<Integer> ratedIds = userRatingRepository.findByUsernameAndRatingBetween(username, minAlbumRating, maxAlbumRating).stream().map(UserRating::getMediaFileId).collect(Collectors.toList());
            List<MediaFile> ratedAlbums = mediaFileRepository.findByIdInAndFolderInAndMediaTypeAndPresentTrue(ratedIds, criteria.getMusicFolders(), MediaType.ALBUM);
            fileIds = ratedAlbums.stream().flatMap(ra -> {
                return mediaFileRepository.findByAlbumArtistAndAlbumNameAndMediaTypeInAndPresentTrue(ra.getArtist(), ra.getAlbumName(), List.of(MediaType.MUSIC), Sort.by("id")).stream().map(MediaFile::getId);
            }).collect(Collectors.toList());
        } else {
            fileIds = mediaFileRepository.findByFolderInAndMediaTypeAndPresentTrue(criteria.getMusicFolders(), MediaType.MUSIC, PageRequest.of(0, Integer.MAX_VALUE)).stream().map(MediaFile::getId).collect(Collectors.toList());
        }
        if (joinStarred) {
            starredFileIds = starredMediaFileRepository.findByUsername(username).stream().map(StarredMediaFile::getMediaFile).filter(Objects::nonNull).map(MediaFile::getId).collect(Collectors.toList());
            if (criteria.isShowStarredSongs()) {
                fileIds.retainAll(starredFileIds);
            } else {
                fileIds.removeAll(starredFileIds);
            }
        }
        List<MediaFile> files = randomMediaFileRepository.getRandomMediaFiles(username, criteria, fileIds);
        Collections.shuffle(files);

        if (files.size() <= criteria.getCount()) {
            return files;
        }
        return files.subList(0, criteria.getCount());
    }

    /**
     * Removes video files from the given list.
     */
    public void removeVideoFiles(List<MediaFile> files) {
        files.removeIf(MediaFile::isVideo);
    }

    public Instant getMediaFileStarredDate(MediaFile mediaFile, String username) {
        return starredMediaFileRepository.findByUsernameAndMediaFile(username, mediaFile).map(StarredMediaFile::getCreated).orElse(null);
    }

    public void populateStarredDate(List<MediaFile> mediaFiles, String username) {
        for (MediaFile mediaFile : mediaFiles) {
            populateStarredDate(mediaFile, username);
        }
    }

    public void populateStarredDate(MediaFile mediaFile, String username) {
        Instant starredDate = starredMediaFileRepository.findByUsernameAndMediaFile(username, mediaFile).map(StarredMediaFile::getCreated).orElse(null);
        mediaFile.setStarredDate(starredDate);
    }

    private List<MediaFile> updateChildren(MediaFile parent) {

        // Check timestamps.
        if (parent.getChildrenLastUpdated().compareTo(parent.getChanged()) >= 0) {
            return null;
        }

        Map<Pair<String, Double>, MediaFile> storedChildrenMap = mediaFileRepository.findByFolderAndParentPath(parent.getFolder(), parent.getPath(), Sort.by("startPosition")).parallelStream()
            .collect(Collectors.toConcurrentMap(i -> Pair.of(i.getPath(), i.getStartPosition()), i -> i));
        MusicFolder folder = parent.getFolder();

        boolean isEnableCueIndexing = settingsService.getEnableCueIndexing();

        Map<String, CueSheet> cueSheets = new ConcurrentHashMap<>();

        if (isEnableCueIndexing) {
            LOG.debug("Cue indexing enabled");
            try (Stream<Path> children = Files.list(parent.getFullPath())) {
                children.parallel()
                    .filter(x -> {
                        String ext = FilenameUtils.getExtension(x.toString());
                        return "cue".equalsIgnoreCase(ext) || "flac".equalsIgnoreCase(ext);
                    })
                    .forEach(x -> {
                        CueSheet cueSheet = getCueSheet(x);
                        if (cueSheet != null && cueSheet.getFileData() != null && cueSheet.getFileData().size() > 0) {
                            cueSheets.put(folder.getPath().relativize(x).toString(), cueSheet);
                        }
                    });
            } catch (IOException e) {
                LOG.warn("Error reading FLAC embedded cue sheets (ignored)", e);
                // ignore
            }
        }

        // collect files, if any
        try (Stream<Path> children = Files.list(parent.getFullPath())) {
            Map<String, MediaFile> bareFiles = children.parallel()
                .filter(this::includeMediaFileByPath)
                .filter(x -> mediaFolderService.getMusicFolderForFile(x, true, true).getId().equals(folder.getId()))
                .map(x -> folder.getPath().relativize(x))
                .map(x -> {
                    MediaFile media = storedChildrenMap.remove(Pair.of(x.toString(), MediaFile.NOT_INDEXED));
                    if (media == null) {
                        media = createMediaFileByFile(x, folder);
                        // Add children that are not already stored.
                        if (media != null) {
                            updateMediaFile(media);
                        }
                    } else {
                        media = checkLastModified(media, false); // has to be false, only time it's called
                    }
                    return media;
                })
                .collect(Collectors.toConcurrentMap(m -> FilenameUtils.getName(m.getPath()), m -> m));

            // collect indexed tracks, if any
            List<MediaFile> result = new ArrayList<>();

            if (isEnableCueIndexing) {
                List<MediaFile> indexedTracks = cueSheets.entrySet().stream().parallel().flatMap(e -> {
                    String indexPath = e.getKey();
                    CueSheet cueSheet = e.getValue();

                    String filePath = cueSheet.getFileData().get(0).getFile();
                    MediaFile base = bareFiles.remove(FilenameUtils.getName(filePath));

                    if (Objects.nonNull(base)) {
                        base.setIndexPath(indexPath); // update indexPath in mediaFile
                        Instant mediaChanged = FileUtil.lastModified(base.getFullPath());
                        Instant cueChanged = FileUtil.lastModified(base.getFullIndexPath());
                        base.setChanged(mediaChanged.compareTo(cueChanged) >= 0 ? mediaChanged : cueChanged);
                        updateMediaFile(base);
                        List<MediaFile> tracks = createIndexedTracks(base, cueSheet);
                        // remove stored children that are now indexed
                        tracks.forEach(t -> storedChildrenMap.remove(Pair.of(t.getPath(), t.getStartPosition())));
                        tracks.add(base);
                        return tracks.stream();
                    } else {
                        LOG.warn("Cue sheet file {} not found", filePath);
                        return Stream.empty();
                    }
                }).collect(Collectors.toList());
                result.addAll(indexedTracks);
            }

            // remove indexPath for deleted cuesheets, if any
            List<MediaFile> nonIndexedTracks = bareFiles.values().stream().parallel()
                .map(m -> {
                    if (m.hasIndex()) {
                        m.setIndexPath(null);
                        updateMediaFile(m);
                    }
                    return m;
                })
                .collect(Collectors.toList());
            result.addAll(nonIndexedTracks);

            // Delete children that no longer exist on disk.
            storedChildrenMap.values().forEach(f -> delete(f));

            // Update timestamp in parent.
            parent.setChildrenLastUpdated(parent.getChanged());
            parent.setPresent(true);
            updateMediaFile(parent);

            return result;

        } catch (Exception e) {
            LOG.warn("Could not retrieve and update all the children for {} in folder {}. Will skip", parent.getPath(), folder.getId(), e);

            return null;
        }
    }

    /**
     * hide specific file types in player and API
     */
    public boolean showMediaFile(MediaFile media) {
        return (settingsService.getEnableCueIndexing() || media.getStartPosition() == MediaFile.NOT_INDEXED) &&
            !(settingsService.getHideIndexedFiles() && media.hasIndex());
    }

    public boolean includeMediaFile(MediaFile candidate) {
        return includeMediaFileByPath(candidate.getFullPath());
    }

    public boolean includeMediaFileByPath(Path candidate) {
        String suffix = FilenameUtils.getExtension(candidate.toString()).toLowerCase();
        return (!isExcluded(candidate) && (Files.isDirectory(candidate) || isAudioFile(suffix) || isVideoFile(suffix)));
    }

    private boolean isAudioFile(String suffix) {
        return settingsService.getMusicFileTypesSet().contains(suffix.toLowerCase());
    }

    private boolean isVideoFile(String suffix) {
        return settingsService.getVideoFileTypesSet().contains(suffix.toLowerCase());
    }

    /**
     * Returns whether the given file is excluded.
     *
     * @param file The child file in question.
     * @return Whether the child file is excluded.
     */
    private boolean isExcluded(Path file) {
        if (settingsService.getIgnoreSymLinks() && Files.isSymbolicLink(file)) {
            LOG.info("excluding symbolic link {}", file);
            return true;
        }
        String name = file.getFileName().toString();
        if (settingsService.getExcludePattern() != null && settingsService.getExcludePattern().matcher(name).find()) {
            LOG.info("excluding file which matches exclude pattern {}: {}", settingsService.getExcludePatternString(), file.toString());
            return true;
        }

        // Exclude all hidden files starting with a single "." or "@eaDir" (thumbnail dir created on Synology devices).
        return (name.startsWith(".") && !name.startsWith("..")) || name.startsWith("@eaDir") || "Thumbs.db".equals(name);
    }

    private MediaFile createMediaFileByFile(Path relativePath, MusicFolder folder) {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setPath(relativePath.toString());
        mediaFile.setFolder(folder);
        MediaFile result = updateMediaFileByFile(mediaFile);
        return result.isPresent() ? result : null;
    }

    /**
     * return media file reflected from file system
     *
     * @param mediaFile   media file to reflect. Must not be null. path must be set.
     * @param folder     music folder
     * @return media file reflected from file system
     */
    private MediaFile updateMediaFileByFile(MediaFile mediaFile) {

        if (mediaFile == null || mediaFile.getFolder() == null || mediaFile.getPath() == null) {
            throw new IllegalArgumentException("mediaFile, folder and mediaFile.path must not be null");
        }

        Path relativePath = mediaFile.getRelativePath();
        Path file = mediaFile.getFullPath();
        if (!Files.exists(file)) {
            // file not found
            mediaFile.setPresent(false);
            mediaFile.setChildrenLastUpdated(Instant.ofEpochMilli(1));
            return mediaFile;
        }

        //sanity check
        MusicFolder folderActual = mediaFolderService.getMusicFolderForFile(file, true, true);
        if (!folderActual.getId().equals(mediaFile.getFolder().getId())) {
            LOG.warn("Inconsistent Mediafile folder for media file with path: {}, folder id should be {} and is instead {}", file, folderActual.getId(), mediaFile.getFolder().getId());
        }
        // distinguish between null (no parent, like root folder), "" (root parent), and else
        String parentPath = null;
        if (StringUtils.isNotEmpty(relativePath.toString())) {
            parentPath = relativePath.getParent() == null ? "" : relativePath.getParent().toString();
        }

        Instant lastModified = FileUtil.lastModified(file);
        mediaFile.setParentPath(parentPath);
        mediaFile.setChanged(lastModified);
        mediaFile.setLastScanned(Instant.now());
        mediaFile.setChildrenLastUpdated(Instant.ofEpochMilli(1)); //distant past, can't use Instant.MIN due to HSQL incompatibility
        mediaFile.setCreated(lastModified);
        mediaFile.setMediaType(MediaFile.MediaType.DIRECTORY);
        mediaFile.setPresent(true);

        if (Files.isRegularFile(file)) {

            MetaDataParser parser = metaDataParserFactory.getParser(file);
            if (parser != null) {
                MetaData metaData = parser.getMetaData(file);
                mediaFile.setArtist(metaData.getArtist());
                mediaFile.setAlbumArtist(metaData.getAlbumArtist());
                mediaFile.setAlbumName(metaData.getAlbumName());
                mediaFile.setTitle(metaData.getTitle());
                mediaFile.setDiscNumber(metaData.getDiscNumber());
                mediaFile.setTrackNumber(metaData.getTrackNumber());
                mediaFile.setGenre(metaData.getGenre());
                mediaFile.setYear(metaData.getYear());
                mediaFile.setDuration(metaData.getDuration());
                mediaFile.setBitRate(metaData.getBitRate());
                mediaFile.setVariableBitRate(metaData.getVariableBitRate());
                mediaFile.setHeight(metaData.getHeight());
                mediaFile.setWidth(metaData.getWidth());
                mediaFile.setMusicBrainzReleaseId(metaData.getMusicBrainzReleaseId());
                mediaFile.setMusicBrainzRecordingId(metaData.getMusicBrainzRecordingId());
            }
            String format = StringUtils.trimToNull(StringUtils.lowerCase(FilenameUtils.getExtension(mediaFile.getPath())));
            mediaFile.setFormat(format);
            mediaFile.setFileSize(FileUtil.size(file));
            mediaFile.setMediaType(getMediaType(mediaFile));

        } else {

            MusicFolder folder = mediaFile.getFolder();
            // Is this an album?
            if (!isRoot(mediaFile)) {
                try (Stream<Path> stream = Files.list(file)) {
                    List<Path> children = stream.parallel().collect(Collectors.toList());
                    Path firstChild = children.parallelStream()
                            .filter(this::includeMediaFileByPath)
                            .filter(x -> Files.isRegularFile(x))
                            .findFirst().orElse(null);

                    if (firstChild != null) {
                        mediaFile.setMediaType(MediaFile.MediaType.ALBUM);

                        // Guess artist/album name, year and genre.
                        MetaDataParser parser = metaDataParserFactory.getParser(firstChild);
                        if (parser != null) {
                            MetaData metaData = parser.getMetaData(firstChild);
                            mediaFile.setArtist(metaData.getAlbumArtist());
                            mediaFile.setAlbumName(metaData.getAlbumName());
                            mediaFile.setYear(metaData.getYear());
                            mediaFile.setGenre(metaData.getGenre());
                        }

                        // Look for cover art.
                        Path coverArt = findCoverArt(children);
                        if (coverArt != null) {
                            // placeholder to be persisted later
                            mediaFile.setArt(new CoverArt(-1, EntityType.MEDIA_FILE, folder.getPath().relativize(coverArt).toString(), folder, false));
                        }
                    } else {
                        mediaFile.setArtist(file.getFileName().toString());
                    }

                } catch (IOException e) {
                    LOG.warn("Could not retrieve children for {}.", file.toString(), e);

                    mediaFile.setArtist(file.getFileName().toString());
                }
            } else {
                // root folders need to have a title
                mediaFile.setTitle(folder.getName());
            }
        }

        return mediaFile;

    }

    private List<MediaFile> createIndexedTracks(MediaFile base, CueSheet cueSheet) {

        Map<Pair<String, Double>, MediaFile> storedChildrenMap = mediaFileRepository.findByFolderAndPath(base.getFolder(), base.getPath()).parallelStream()
            .filter(MediaFile::isIndexedTrack).collect(Collectors.toConcurrentMap(i -> Pair.of(i.getPath(), i.getStartPosition()), i -> i));

        List<MediaFile> children = new ArrayList<>();

        try {
            if (Objects.isNull(cueSheet)) {
                base.setIndexPath(null);
                updateMediaFile(base);
                return children;
            }

            Path audioFile = base.getFullPath();
            MetaData metaData = null;
            MetaDataParser parser = metaDataParserFactory.getParser(audioFile);
            if (parser != null) {
                metaData = parser.getMetaData(audioFile);
            }
            long wholeFileSize = Files.size(audioFile);
            double wholeFileLength = 0.0; //todo: find sound length without metadata
            if (metaData != null && metaData.getDuration() != null) {
                wholeFileLength = metaData.getDuration();
            }

            String format = StringUtils.trimToNull(StringUtils.lowerCase(FilenameUtils.getExtension(audioFile.toString())));
            String basePath = base.getPath();
            String file = cueSheet.getFileData().get(0).getFile();
            LOG.info(file);
            String parentPath = base.getParentPath();
            String performer = cueSheet.getPerformer();
            String albumName = cueSheet.getTitle();
            MediaFile.MediaType mediaType = getMediaType(base);
            Instant lastModified = FileUtil.lastModified(audioFile);
            Instant childrenLastUpdated = Instant.now().plusSeconds(100 * 365 * 24 * 60 * 60); // now + 100 years, tracks do not have children
            MusicFolder baseFolder = base.getFolder();

            boolean update = needsUpdate(base, settingsService.isFastCacheEnabled());
            int trackSize = cueSheet.getAllTrackData().size();

            if (trackSize > 0) {
                TrackData lastTrackData = cueSheet.getAllTrackData().get(trackSize - 1);
                double lastTrackStart = lastTrackData.getIndices().get(0).getPosition().getMinutes() * 60 + lastTrackData.getIndices().get(0).getPosition().getSeconds() + (lastTrackData.getIndices().get(0).getPosition().getFrames() / 75);
                if (lastTrackStart >= wholeFileLength) {
                    base.setIndexPath(null);
                    updateMediaFile(base);
                    return children;
                }
            }

            for (int i = 0; i < trackSize; i++) {
                TrackData trackData = cueSheet.getAllTrackData().get(i);
                Position currentPosition = trackData.getIndices().get(0).getPosition();
                // convert CUE timestamp (minutes:seconds:frames, 75 frames/second) to fractional seconds
                double currentStart = currentPosition.getMinutes() * 60 + currentPosition.getSeconds() + (currentPosition.getFrames() / 75);
                double nextStart = 0.0;
                if (cueSheet.getAllTrackData().size() - 1 != i) {
                    Position nextPosition = cueSheet.getAllTrackData().get(i + 1).getIndices().get(0).getPosition();
                    nextStart = nextPosition.getMinutes() * 60 + nextPosition.getSeconds() + (nextPosition.getFrames() / 75);
                } else {
                    nextStart = wholeFileLength;
                }

                double duration = nextStart - currentStart;

                MediaFile existingFile = storedChildrenMap.remove(Pair.of(basePath, currentStart));
                // check whether track has same duration, cue file may have been edited to split tracks
                if ((existingFile != null) && (existingFile.getDuration() != duration)) {
                    storedChildrenMap.put(Pair.of(basePath, currentStart), existingFile);
                    existingFile = null;
                }
                MediaFile track = existingFile;
                if (update || (existingFile == null)) {
                    track = (existingFile != null) ? existingFile : new MediaFile();
                    track.setPath(basePath);
                    track.setAlbumArtist(performer);
                    track.setAlbumName(albumName);
                    track.setTitle(trackData.getTitle());
                    track.setArtist(trackData.getPerformer());
                    track.setParentPath(parentPath);
                    track.setFolder(baseFolder);
                    track.setChanged(lastModified);
                    track.setLastScanned(Instant.now());
                    track.setChildrenLastUpdated(childrenLastUpdated);
                    track.setCreated(lastModified);
                    track.setPresent(true);
                    track.setTrackNumber(trackData.getNumber());

                    if (metaData != null) {
                        track.setDiscNumber(metaData.getDiscNumber());
                        track.setGenre(metaData.getGenre());
                        track.setYear(metaData.getYear());
                        track.setBitRate(metaData.getBitRate());
                        track.setVariableBitRate(metaData.getVariableBitRate());
                        track.setHeight(metaData.getHeight());
                        track.setWidth(metaData.getWidth());
                    }

                    track.setFormat(format);
                    track.setStartPosition(currentStart);
                    track.setDuration(duration);
                    // estimate file size based on duration and whole file size
                    long estimatedSize = (long) (duration / wholeFileLength * wholeFileSize);
                    // if estimated size is within of whole file size, use it. Otherwise use whole file size divided by number of tracks
                    if (estimatedSize > 0 && estimatedSize <= wholeFileSize) {
                        track.setFileSize(estimatedSize);
                    } else {
                        track.setFileSize((long)(wholeFileSize / trackSize));
                    }
                    track.setPlayCount((existingFile == null) ? 0 : existingFile.getPlayCount());
                    track.setLastPlayed((existingFile == null) ? null : existingFile.getLastPlayed());
                    track.setComment((existingFile == null) ? null : existingFile.getComment());
                    track.setMediaType(mediaType);

                    updateMediaFile(track);
                }

                children.add(track);
            }
            return children;
        } catch (IOException e) {
            LOG.warn("Not found: {}", base.getFullIndexPath());
            return new ArrayList<MediaFile>();
        } catch (IndexOutOfBoundsException e) {
            LOG.warn("Invalid CUE sheet: {}", base.getFullIndexPath());
            return new ArrayList<MediaFile>();
        } finally {
            storedChildrenMap.values().forEach(m -> delete(m));
        }
    }

    private List<MediaFile> createIndexedTracks(MediaFile base) {
        CueSheet cueSheet = getCueSheet(base);
        return createIndexedTracks(base, cueSheet);
    }

    private MediaFile.MediaType getMediaType(MediaFile mediaFile) {
        MusicFolder folder = mediaFile.getFolder();
        if (folder.getType() == Type.PODCAST) {
            return MediaType.PODCAST;
        }
        if (isVideoFile(mediaFile.getFormat())) {
            return MediaFile.MediaType.VIDEO;
        }
        String path = mediaFile.getPath().toLowerCase();
        String genre = StringUtils.trimToEmpty(mediaFile.getGenre()).toLowerCase();
        if (path.contains("podcast") || genre.contains("podcast") || path.contains("netcast") || genre.contains("netcast")) {
            return MediaFile.MediaType.PODCAST;
        }
        if (path.contains("audiobook") || genre.contains("audiobook")
                || path.contains("audio book") || genre.contains("audio book")
                || path.contains("audio/book") || path.contains("audio\\book")) {
            return MediaFile.MediaType.AUDIOBOOK;
        }

        return MediaFile.MediaType.MUSIC;
    }

    public void refreshMediaFile(MediaFile mediaFile) {
        mediaFile = updateMediaFileByFile(mediaFile);
        updateMediaFile(mediaFile);
    }

    @CacheEvict(cacheNames = { "mediaFilePathCache", "mediaFileIdCache" }, allEntries = true)
    public void setMemoryCacheEnabled(boolean memoryCacheEnabled) {
        this.memoryCacheEnabled = memoryCacheEnabled;
    }

    public boolean getMemoryCacheEnabled() {
        return memoryCacheEnabled;
    }

    /**
     * get Cue sheet from cue file absolute path
     *
     * @param cueFile absolute path of cue or embedded flac file
     * @return if parse success return cue sheet, otherwise null
     */
    private CueSheet getCueSheet(Path cueFile) {
        try {
            switch (FilenameUtils.getExtension(cueFile.toString()).toLowerCase()) {
                case "cue":
                    Charset cs = Charset.forName("UTF-8"); // default to UTF-8
                    // attempt to detect encoding for cueFile, fallback to UTF-8
                    int THRESHOLD = 35; // 0-100, the higher the more certain the guess
                    CharsetDetector cd = new CharsetDetector();
                    try (FileInputStream fis = new FileInputStream(cueFile.toFile());
                        BufferedInputStream bis = new BufferedInputStream(fis);) {
                        cd.setText(bis);
                        CharsetMatch cm = cd.detect();
                        if (cm != null && cm.getConfidence() > THRESHOLD) {
                            cs = Charset.forName(cm.getName());
                        }
                    } catch (IOException e) {
                        LOG.warn("Defaulting to UTF-8 for cuesheet {}", cueFile);
                    }
                    CueSheet cueSheet = CueParser.parse(cueFile, cs);
                    if (cueSheet.getMessages().stream().filter(m -> m.toString().toLowerCase().contains("warning")).findFirst().isPresent()) {
                        LOG.warn("Error parsing cuesheet {}", cueFile);
                        return null;
                    }
                    return cueSheet;
                case "flac":
                    return FLACReader.getCueSheet(cueFile);

                default:
                    return null;
            }
        } catch (IOException e) {
            LOG.warn("Error getting cuesheet for {} ", cueFile);
            return null;
        }
    }

    /**
     * Returns a parsed CueSheet for the given mediaFile
     */
    private CueSheet getCueSheet(MediaFile media) {
        return getCueSheet(media.getFullIndexPath());
    }

    /**
     * Finds a cover art image for the given directory, by looking for it on the disk.
     */
    private Path findCoverArt(Collection<Path> candidates) {
        Path candidate = null;
        var coverArtSource = settingsService.getCoverArtSource();
        switch (coverArtSource) {
            case TAGFILE:
                candidate = findTagCover(candidates);
                if (candidate != null) {
                    return candidate;
                } else {
                    return findFileCover(candidates);
                }
            case FILE:
                return findFileCover(candidates);
            case TAG:
                return findTagCover(candidates);
            case FILETAG:
            default:
                candidate = findFileCover(candidates);
                if (candidate != null) {
                    return candidate;
                } else {
                    return findTagCover(candidates);
                }
        }
    }

    private Path findFileCover(Collection<Path> candidates) {
        for (String mask : settingsService.getCoverArtFileTypesSet()) {
            Path cand = candidates.parallelStream().filter(c -> {
                String candidate = c.getFileName().toString().toLowerCase();
                return candidate.endsWith(mask) && !candidate.startsWith(".") && Files.isRegularFile(c);
            }).findAny().orElse(null);

            if (cand != null) {
                return cand;
            }
        }
        return null;
    }

    private Path findTagCover(Collection<Path> candidates) {
        // Look for embedded images in audiofiles.
        return candidates.stream()
                .filter(parser::isApplicable)
                .filter(JaudiotaggerParser::isImageAvailable)
                .findFirst()
                .orElse(null);
    }


    /**
     * Returns all media files that are children, grand-children etc of a given media file.
     * Directories are not included in the result.
     *
     * @param sort Whether to sort files in the same directory.
     * @return All descendant music files.
     */
    public List<MediaFile> getDescendantsOf(MediaFile ancestor, boolean sort) {

        if (ancestor.isFile()) {
            return Arrays.asList(ancestor);
        }

        List<MediaFile> result = new ArrayList<MediaFile>();

        for (MediaFile child : getVisibleChildrenOf(ancestor, true, sort)) {
            if (child.isDirectory()) {
                result.addAll(getDescendantsOf(child, sort));
            } else {
                result.add(child);
            }
        }
        return result;
    }

    public void setMetaDataParserFactory(MetaDataParserFactory metaDataParserFactory) {
        this.metaDataParserFactory = metaDataParserFactory;
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "mediaFilePathCache", key = "#mediaFile.path.concat('-').concat(#mediaFile.folder.id).concat('-').concat(#mediaFile.startPosition == null ? '' : #mediaFile.startPosition.toString())"),
        @CacheEvict(cacheNames = "mediaFileIdCache", key = "#mediaFile.id", condition = "#mediaFile.id != null") })
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void updateMediaFile(MediaFile mediaFile) {
        if (mediaFile == null) {
            throw new IllegalArgumentException("mediaFile must not be null");
        } else if (mediaFile.getId() != null && mediaFileRepository.existsById(mediaFile.getId())) {
            mediaFileRepository.saveAndFlush(mediaFile);
        } else {
            mediaFileRepository.findByPathAndFolderAndStartPosition(mediaFile.getPath(), mediaFile.getFolder(), mediaFile.getStartPosition()).ifPresentOrElse(m -> {
                mediaFile.setId(m.getId());
                mediaFileRepository.saveAndFlush(mediaFile);
            }, () -> {
                    MusicFolder folder = mediaFile.getFolder();
                    if (folder != null) {
                        musicFileInfoRepository.findByPath(mediaFile.getFullPath().toString()).ifPresent(musicFileInfo -> {
                            mediaFile.setComment(musicFileInfo.getComment());
                            mediaFile.setLastPlayed(musicFileInfo.getLastPlayed());
                            mediaFile.setPlayCount(musicFileInfo.getPlayCount());
                        });
                    }
                    mediaFileRepository.saveAndFlush(mediaFile);
                });
        }

        // persist cover art if not overridden
        coverArtService.persistIfNeeded(mediaFile);
    }

    /**
     * Increments the play count and last played date for the given media file and its
     * directory and album.
     */
    public void incrementPlayCount(MediaFile file) {
        Instant now = Instant.now();
        file.setLastPlayed(now);
        file.setPlayCount(file.getPlayCount() + 1);
        updateMediaFile(file);

        MediaFile parent = getParentOf(file);
        if (Objects.nonNull(parent) && !isRoot(parent)) {
            parent.setLastPlayed(now);
            parent.setPlayCount(parent.getPlayCount() + 1);
            updateMediaFile(parent);
        }

        albumRepository.findByArtistAndName(file.getAlbumArtist(), file.getAlbumName()).ifPresent(album -> {
                album.setLastPlayed(now);
                album.incrementPlayCount();
                albumRepository.save(album);
            }
        );
    }

    public List<MediaFileEntry> toMediaFileEntryList(List<MediaFile> files, String username, boolean calculateStarred, boolean calculateFolderAccess,
            Function<MediaFile, String> streamUrlGenerator, Function<MediaFile, String> remoteStreamUrlGenerator,
            Function<MediaFile, String> remoteCoverArtUrlGenerator) {
        Locale locale = Optional.ofNullable(username).map(localeResolver::resolveLocale).orElse(null);
        List<MediaFileEntry> entries = new ArrayList<>(files.size());
        for (MediaFile file : files) {
            String streamUrl = Optional.ofNullable(streamUrlGenerator).map(g -> g.apply(file)).orElse(null);
            String remoteStreamUrl = Optional.ofNullable(remoteStreamUrlGenerator).map(g -> g.apply(file)).orElse(null);
            String remoteCoverArtUrl = Optional.ofNullable(remoteCoverArtUrlGenerator).map(g -> g.apply(file)).orElse(null);

            boolean starred = calculateStarred && username != null && getMediaFileStarredDate(file, username) != null;
            boolean folderAccess = !calculateFolderAccess || username == null || securityService.isFolderAccessAllowed(file, username);
            entries.add(MediaFileEntry.fromMediaFile(file, locale, starred, folderAccess, streamUrl, remoteStreamUrl, remoteCoverArtUrl));
        }

        return entries;
    }

    public int getAlbumCount(List<MusicFolder> musicFolders) {
        if (CollectionUtils.isEmpty(musicFolders)) {
            return 0;
        }
        return mediaFileRepository.countByFolderInAndMediaTypeAndPresentTrue(musicFolders, MediaType.ALBUM);
    }

    public int getPlayedAlbumCount(List<MusicFolder> musicFolders) {
        if (CollectionUtils.isEmpty(musicFolders)) {
            return 0;
        }
        return mediaFileRepository.countByFolderInAndMediaTypeAndPlayCountGreaterThanAndPresentTrue(musicFolders, MediaType.ALBUM, 0);
    }

    public int getStarredAlbumCount(String username, List<MusicFolder> musicFolders) {
        if (CollectionUtils.isEmpty(musicFolders)) {
            return 0;
        }
        return starredMediaFileRepository.countByUsernameAndMediaFileMediaTypeAndMediaFileFolderInAndMediaFilePresentTrue(username, MediaType.ALBUM, musicFolders);
    }

    /**
     * star media files
     *
     * @param ids     media file ids to star
     * @param username username who stars the media files
     */
    public void starMediaFiles(List<Integer> ids, String username) {
        if (CollectionUtils.isEmpty(ids) || StringUtils.isEmpty(username)) {
            return;
        }
        List<MediaFile> mediaFiles = mediaFileRepository.findAllById(ids);
        mediaFiles.forEach(m -> {
            starredMediaFileRepository.findByUsernameAndMediaFile(username, m).ifPresentOrElse(starredMediaFile -> {
                starredMediaFile.setCreated(Instant.now().truncatedTo(ChronoUnit.MICROS));
                starredMediaFileRepository.save(starredMediaFile);
            }, () -> {
                    StarredMediaFile starredMediaFile = new StarredMediaFile(m, username, Instant.now().truncatedTo(ChronoUnit.MICROS));
                    starredMediaFileRepository.save(starredMediaFile);
                });
        });
    }

    /**
     * unstar media files
     *
     * @param ids    media file ids to unstar
     * @param username username who unstars the media files
     */
    public void unstarMediaFiles(List<Integer> ids, String username) {
        if (CollectionUtils.isEmpty(ids) || StringUtils.isEmpty(username)) {
            return;
        }
        starredMediaFileRepository.deleteAllByMediaFileIdInAndUsername(ids, username);
    }

    /**
     * mark media files present
     *
     * @param paths paths to mark present by folder id
     * @param lastScanned last scanned time
     * @return true if success, false otherwise
     */
    public boolean markPresent(Map<Integer, Set<String>> paths, Instant lastScanned) {

        if (CollectionUtils.isEmpty(paths)) {
            return true;
        }
        try {
            paths.entrySet().parallelStream().forEach(e -> {
                MusicFolder folder = mediaFolderService.getMusicFolderById(e.getKey());
                if (folder == null) {
                    return;
                }
                mediaFileRepository.findByFolderAndPathIn(folder, e.getValue())
                    .forEach(
                        m -> {
                            m.setPresent(true);
                            m.setLastScanned(lastScanned);
                            mediaFileRepository.save(m);
                        }
                    );
            });
            return true;
        } catch (Exception e) {
            LOG.warn("Error marking media files present", e);
            return false;
        }
    }

    /**
     * mark media files non present
     * @param lastScanned last scanned time before which media files are marked non present
     */
    public void markNonPresent(Instant lastScanned) {
        mediaFileRepository.findByLastScannedBeforeAndPresentTrue(lastScanned).forEach(m -> {
            m.setPresent(false);
            m.setChildrenLastUpdated(Instant.ofEpochMilli(1));
            mediaFileRepository.save(m);
        });
    }

    /**
     * soft delete media file
     *
     * @param file media file to delete
     * @return deleted media file
     */
    private MediaFile delete(MediaFile file) {
        if (file == null) {
            return null;
        }
        file.setPresent(false);
        file.setChildrenLastUpdated(Instant.ofEpochMilli(1));
        mediaFileRepository.save(file);
        return file;
    }

    /**
     * delete all media files that are not present on disk
     */
    public void expunge() {
        mediaFileRepository.deleteAllByPresentFalse();
    }

    /**
     * update album stats
     *
     * @param file media file
     * @param musicFolder music folder
     * @param lastScanned last scanned time
     * @param albumCount album count
     * @param albums albums
     * @param albumsInDb albums in db
     */
    public void updateAlbum(MediaFile file, MusicFolder musicFolder,
            Instant lastScanned, Map<String, AtomicInteger> albumCount, Map<String, Album> albums,
            Map<Integer, Album> albumsInDb) {

        String artist = file.getAlbumArtist() != null ? file.getAlbumArtist() : file.getArtist();
        if (file.getAlbumName() == null || artist == null || file.getParentPath() == null || !file.isAudio()) {
            return;
        }

        final AtomicBoolean firstEncounter = new AtomicBoolean(false);
        Album album = albums.compute(file.getAlbumName() + "|" + artist, (k, v) -> {
            Album a = v;

            if (a == null) {
                a = albumRepository.findByArtistAndName(artist, file.getAlbumName())
                        .map(dbAlbum -> {
                            albumsInDb.computeIfAbsent(dbAlbum.getId(), aid -> {
                                // reset stats when first retrieve from the db for new scan
                                dbAlbum.setDuration(0);
                                dbAlbum.setSongCount(0);
                                return dbAlbum;
                            });
                            return dbAlbum;
                        }).orElse(null);
            }

            if (a == null) {
                a = new Album();
                a.setPath(file.getParentPath());
                a.setName(file.getAlbumName());
                a.setArtist(artist);
                a.setCreated(file.getChanged());
            }

            firstEncounter.set(!lastScanned.equals(a.getLastScanned()));

            if (file.getDuration() != null) {
                a.incrementDuration(file.getDuration());
            }
            if (file.isAudio()) {
                a.incrementSongCount();
            }

            a.setLastScanned(lastScanned);
            a.setPresent(true);

            return a;
        });

        if (file.getMusicBrainzReleaseId() != null) {
            album.setMusicBrainzReleaseId(file.getMusicBrainzReleaseId());
        }
        if (file.getYear() != null) {
            album.setYear(file.getYear());
        }
        if (file.getGenre() != null) {
            album.setGenre(file.getGenre());
        }

        if (album.getArt() == null) {
            MediaFile parent = getParentOf(file, true); // true because the parent has recently already been scanned
            if (parent != null) {
                CoverArt art = coverArtService.get(EntityType.MEDIA_FILE, parent.getId());
                if (!CoverArt.NULL_ART.equals(art)) {
                    album.setArt(new CoverArt(-1, EntityType.ALBUM, art.getPath(), art.getFolder(), false));
                }
            }
        }

        if (firstEncounter.get()) {
            album.setFolder(musicFolder);

            albumRepository.saveAndFlush(album);
            albumCount.computeIfAbsent(artist, k -> new AtomicInteger(0)).incrementAndGet();
            indexManager.index(album);
        }

        // Update the file's album artist, if necessary.
        if (!ObjectUtils.equals(album.getArtist(), file.getAlbumArtist())) {
            file.setAlbumArtist(album.getArtist());
            updateMediaFile(file);
        }
    }

    /**
     * update artist stats
     *
     * @param file media file
     * @param musicFolder music folder
     * @param lastScanned last scanned time
     * @param albumCount album count
     * @param artists artists
     */
    public void updateArtist(MediaFile file, MusicFolder musicFolder, Instant lastScanned,
            Map<String, AtomicInteger> albumCount, Map<String, Artist> artists) {
        if (file.getAlbumArtist() == null || !file.isAudio()) {
            return;
        }

        final AtomicBoolean firstEncounter = new AtomicBoolean(false);

        Artist artist = artists.compute(file.getAlbumArtist(), (k, v) -> {
            Artist a = v;

            if (a == null) {
                a = artistRepository.findByName(k).orElse(new Artist(k));
            }

            int n = Math.max(Optional.ofNullable(albumCount.get(a.getName())).map(x -> x.get()).orElse(0),
                    Optional.ofNullable(a.getAlbumCount()).orElse(0));
            a.setAlbumCount(n);

            firstEncounter.set(!lastScanned.equals(a.getLastScanned()));

            a.setLastScanned(lastScanned);
            a.setPresent(true);

            return a;
        });

        if (artist.getArt() == null) {
            MediaFile parent = getParentOf(file, true); // true because the parent has recently already been scanned
            if (parent != null) {
                CoverArt art = coverArtService.get(EntityType.MEDIA_FILE, parent.getId());
                if (!CoverArt.NULL_ART.equals(art)) {
                    artist.setArt(new CoverArt(-1, EntityType.ARTIST, art.getPath(), art.getFolder(), false));
                }
            }
        }

        if (firstEncounter.get()) {
            artist.setFolder(musicFolder);
            artistRepository.saveAndFlush(artist);
            indexManager.index(artist, musicFolder);
        }
    }
}
