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

  Copyright 2017 (C) Airsonic Authors
  Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
*/
package org.airsonic.player.service.upnp;

import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.repository.ArtistRepository;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.MusicArtist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * @author Allen Petersen
 * @version $Id$
 */
@Service
@Transactional(readOnly = true)
public class ArtistUpnpProcessor extends UpnpContentProcessor <Artist, Album> {

    @Autowired
    private ArtistRepository artistRepository;

    public ArtistUpnpProcessor() {
        setRootId(DispatchingContentDirectory.CONTAINER_ID_ARTIST_PREFIX);
        setRootTitle("Artists");
    }

    @Override
    public Container createContainer(Artist artist) {
        MusicArtist container = new MusicArtist();
        container.setId(getRootId() + DispatchingContentDirectory.SEPARATOR + artist.getId());
        container.setParentID(getRootId());
        container.setTitle(artist.getName());
        container.setChildCount(artist.getAlbumCount());

        return container;
    }

    @Override
    public List<Artist> getAllItems() {
        List<MusicFolder> allFolders = getDispatcher().getMediaFolderService().getAllMusicFolders();
        if (CollectionUtils.isEmpty(allFolders)) {
            return Collections.emptyList();
        }
        List<Artist> allArtists = artistRepository.findByFolderIdInAndPresentTrue(MusicFolder.toIdList(allFolders), Sort.by(Sort.Direction.ASC, "name"));
        // alpha artists doesn't quite work :P
        allArtists.sort((Artist o1, Artist o2) -> o1.getName().replaceAll("\\W", "").compareToIgnoreCase(o2.getName().replaceAll("\\W", "")));

        return allArtists;
    }

    @Override
    public Artist getItemById(String id) {
        try {
            return artistRepository.findById(Integer.parseInt(id)).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public List<Album> getChildren(Artist artist) {
        List<MusicFolder> allFolders = getDispatcher().getMediaFolderService().getAllMusicFolders();
        List<Album> allAlbums = getAlbumProcessor().getAlbumDao().getAlbumsForArtist(artist.getName(), allFolders);
        if (allAlbums.size() > 1) {
            // if the artist has more than one album, add in an option to
            // view the tracks in all the albums together
            Album viewAll = new Album();
            viewAll.setName("- All Albums -");
            viewAll.setId(-1);
            viewAll.setComment(AlbumUpnpProcessor.ALL_BY_ARTIST + "_" + artist.getId());
            allAlbums.add(0, viewAll);
        }
        return allAlbums;
    }

    @Override
    public void addChild(DIDLContent didl, Album album) {
        didl.addContainer(getAlbumProcessor().createContainer(album));
    }

    public AlbumUpnpProcessor getAlbumProcessor() {
        return getDispatcher().getAlbumProcessor();
    }
}
