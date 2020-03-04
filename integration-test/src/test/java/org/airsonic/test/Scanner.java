package org.airsonic.test;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.subsonic.restapi.Child;
import org.subsonic.restapi.MusicFolder;
import org.subsonic.restapi.Response;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class Scanner {
    public static final RestTemplate rest = new RestTemplate();
    public static final String SERVER = System.getProperty("DockerTestingHost", "http://localhost:4040");
    public static final String DEFAULT_MUSIC = System.getProperty("DockerTestingDefaultMusicFolder", "/tmp/music");
    public static final ObjectMapper MAPPER = new ObjectMapper();

    public static UriComponentsBuilder addRestParameters(UriComponentsBuilder builder) {
        return builder.queryParam("c", "inttest")
                .queryParam("v", "1.15.0")
                .queryParam("u", "admin")
                .queryParam("s", "int")
                .queryParam("t", DigestUtils.md5Hex("admin" + "int"));
    }

    public static void doScan() throws Exception {
        Assert.assertFalse(isScanning());

        String startScan = rest.getForObject(
                addRestParameters(UriComponentsBuilder.fromHttpUrl(SERVER + "/rest/startScan")).toUriString(),
                String.class);

        System.out.println(startScan);

        Long waitTime = 30000L;
        Long sleepTime = 1000L;
        while (waitTime > 0 && isScanning()) {
            waitTime -= sleepTime;
            Thread.sleep(sleepTime);
        }

        Assert.assertFalse(isScanning());
    }

    private static boolean isScanning() {
        System.out.println("isScanning server is " + SERVER);
        String resp2 = rest
                .getForObject(addRestParameters(UriComponentsBuilder.fromHttpUrl(SERVER + "/rest/getScanStatus"))
                        .queryParam("f", "json").toUriString(), String.class);
        System.out.println(resp2);
        Response resp = rest
                .getForObject(addRestParameters(UriComponentsBuilder.fromHttpUrl(SERVER + "/rest/getScanStatus"))
                        .queryParam("f", "json").toUriString(), Response.class);
        System.out.println(resp);
        System.out.println("scan status: " + resp.getScanStatus());
        System.out.println("scan count: " + resp.getScanStatus().getCount());
        System.out.println("scan currently: " + resp.getScanStatus().isScanning());

        return resp.getScanStatus().isScanning();
    }

    public static void uploadToDefaultMusicFolder(Path directoryPath, String relativePath) {
        Path dest = Paths.get(DEFAULT_MUSIC, relativePath);
        try {
            FileUtils.copyDirectory(directoryPath.toFile(), dest.toFile(), false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Child> getMediaFilesInMusicFolder() {
        List<MusicFolder> musicFolder = rest.getForObject(
                addRestParameters(UriComponentsBuilder.fromHttpUrl(SERVER + "/rest/getMusicFolders"))
                        .queryParam("f", "json")
                        .toUriString(),
                Response.class)
            .getMusicFolders().getMusicFolder();

        MusicFolder music = musicFolder.stream().filter(folder -> Objects.equals(folder.getName(), "Music")).findFirst()
                .orElseThrow(() -> new RuntimeException("No top level folder named Music"));
        return getMediaFiles(music.getId());
    }

    private static List<Child> getMediaFiles(int folderId) {
        return rest.getForObject(
                addRestParameters(UriComponentsBuilder.fromHttpUrl(SERVER + "/rest/getIndexes"))
                        .queryParam("f", "json")
                        .queryParam("musicFolderId", folderId)
                        .toUriString(),
                Response.class)
            .getIndexes().getChild();
    }

    public static byte[] getMediaFileData(String mediaFileId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(MediaType.parseMediaTypes("audio/webm,audio/ogg,audio/wav,audio/*;"));
        ResponseEntity<byte[]> response = rest.exchange(
                addRestParameters(UriComponentsBuilder.fromHttpUrl(SERVER + "/rest/stream"))
                        .queryParam("id", mediaFileId)
                        .toUriString(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                byte[].class);

        assertThat(response.getBody()).hasSize((int) response.getHeaders().getContentLength());
        return response.getBody();
    }
}
