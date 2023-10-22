package org.airsonic.player.repository;

import org.airsonic.player.domain.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Integer> {

    public List<Playlist> findByUsername(String username);

    public List<Playlist> findByUsernameOrderByNameAsc(String username);

    public List<Playlist> findBySharedTrue();

    public List<Playlist> findByUsernameNotAndSharedUsersUsername(String username, String sharedUsername);

    public Optional<Playlist> findByIdAndSharedUsersUsername(Integer id, String sharedUsername);

    public boolean existsByIdAndUsername(Integer id, String username);

}