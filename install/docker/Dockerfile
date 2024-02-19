FROM eclipse-temurin:17.0.10_7-jre-jammy

LABEL description="Airsonic-Advanced is a free, web-based media streamer, providing ubiquitous access to your music." \
      url="https://github.com/kagemomiji/airsonic-advanced"

ENV AIRSONIC_PORT=4040 AIRSONIC_DIR=/var CONTEXT_PATH=/ UPNP_PORT=4041 PUID=0 PGID=0

WORKDIR $AIRSONIC_DIR

RUN apt-get update \
    && apt-get install -y \
                       ffmpeg \
                       x264 \
                       x265 \
                       lame \
                       xmp \
                       bash \
                       fonts-dejavu \
                       gosu \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

COPY run.sh /usr/local/bin/run.sh
COPY entry.sh /usr/local/bin/entry.sh
RUN chmod +x /usr/local/bin/run.sh && \
    chmod +x /usr/local/bin/entry.sh

COPY target/dependency/airsonic-main.jar /app/airsonic.jar

EXPOSE $AIRSONIC_PORT

# Default DLNA/UPnP ports
EXPOSE $UPNP_PORT
EXPOSE 1900/udp

USER ${PUID}:${PGID}

VOLUME $AIRSONIC_DIR/airsonic $AIRSONIC_DIR/music $AIRSONIC_DIR/playlists $AIRSONIC_DIR/podcasts

HEALTHCHECK --interval=15s --timeout=3s CMD curl -f http://localhost:"$AIRSONIC_PORT""$CONTEXT_PATH"rest/ping || false

ENTRYPOINT ["entry.sh"]
