FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install yt-dlp dependencies: Python3, ffmpeg, curl
RUN apk add --no-cache \
    python3 \
    py3-pip \
    ffmpeg \
    curl \
    && pip3 install --break-system-packages yt-dlp \
    && yt-dlp --version

# Download directory for YouTube tracks
RUN mkdir -p /app/downloads && chmod 777 /app/downloads

# Copy the fat JAR (built with shadowJar)
COPY syncjam-server.jar app.jar

# Create non-root user for security (needs write access to downloads)
RUN addgroup -S syncjam && adduser -S syncjam -G syncjam \
    && chown -R syncjam:syncjam /app

USER syncjam

EXPOSE 8080

ENV PORT=8080
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"
ENV YTDLP_DOWNLOAD_DIR=/app/downloads

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD wget -q --spider http://localhost:8080/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
