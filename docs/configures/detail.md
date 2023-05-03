# Detail Configuration

## airsonic.home

The directory where Airsonic will store its configuration data.  
This directory will be created if it does not exist.  
Docker image does not support this option. Docker image will use the `$AIRSONIC_DIR/airsonic` directory.

| item | description |
| --- | --- |
| type | string |
| default | (Windows) C:\\airsonic, (Other) /var/airsonic |
| example | airsonic.home=/var/airsonic |
| configurable by | Java options, environment variables |
| environment variable | AIRSONIC_HOME |

## airsonic.defaultMusicFolder

The directory where Airsonic will look for music.   
This is only used when initializing the database for the first time.  
Docker image does not support this option. Docker image will use the `$AIRSONIC_DIR/music` directory.

| item | description |
| --- | --- |
| type | string |
| default | (Windows) C:\\Music, (Other) /var/music |
| example | airsonic.defaultMusicFolder=/var/music |
| configurable by | Java options, environment variables |
| environment variable | AIRSONIC_DEFAULTMUSICFOLDER |

## airsonic.defaultPodcastFolder

The directory where Airsonic will look for podcasts.
This is only used when initializing the database for the first time.  
Docker image does not support this option. Docker image will use the `$AIRSONIC_DIR/podcasts` directory.

| item | description |
| --- | --- |
| type | string |
| default | (Windows) C:\\Podcasts, (Other) /var/podcasts |
| example | airsonic.defaultMusicFolder=/var/music |
| configurable by | Java options, environment variables |
| environment variable | AIRSONIC_DEFAULTMUSICFOLDER |

## airsonic.defaultPlaylistFolder

The directory where Airsonic will look for playlists.  
This is only used when initializing the database for the first time.
Docker image does not support this option. Docker image will use the `$AIRSONIC_DIR/playlists` directory.

| item | description |
| --- | --- |
| type | string |
| default | (Windows) C:\\Playlists, (Other) /var/playlists |
| example | airsonic.defaultPlaylistFolder=/var/playlists |
| configurable by | Java options, environment variables |
| environment variable | AIRSONIC_DEFAULTPLAYLISTFOLDER |

## airsonic.cue.enabled

If enabled, airsonic-advanced will look for cue sheets in the same directory as the audio file and automatically split the audio file into tracks.
Configuration by Java options, environment variables are working as default value.

> **Note**  
> If you changed the value of this option, you need to re-scan the music folder with `FullScan` option.

| item | description |
| --- | --- |
| type | boolean |
| default | true |
| example | airsonic.cue.enabled=true |
| configurable by | Java options, environment variables, airsonic.properties, web interface |
| environment variable | AIRSONIC_CUE_ENABLED |
| airsonic.properties | ENABLE_CUE_INDEXING |
| web interface | Settings > Music Folder > Enable cue indexing |

## airsonic.cue.hide-indexed-files

If enabled, airsonic-advanced will hide the original audio file when cue sheet support is enabled.  
Configuration by Java options, environment variables are working as default value.

| item | description |
| --- | --- |
| type | boolean |
| default | true |
| example | airsonic.cue.hide-indexed-files=true |
| configurable by | Java options, environment variables, airsonic.properties, web interface |
| environment variable | AIRSONIC_CUE_HIDEINDEXEDFILES |
| airsonic.properties | HIDE_INDEXED_FILES |
| web interface | Settings > Music Folder > Hide cue-indexed files |
