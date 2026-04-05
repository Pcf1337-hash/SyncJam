FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install yt-dlp + Node.js (required JS runtime for YouTube extraction)
RUN apk add --no-cache \
    python3 \
    py3-pip \
    ffmpeg \
    curl \
    nodejs \
    && pip3 install --break-system-packages yt-dlp \
    && yt-dlp --version

# Download + uploads directories
RUN mkdir -p /app/downloads /app/uploads && chmod 777 /app/downloads /app/uploads

# Copy the fat JAR (built with shadowJar)
COPY syncjam-server.jar app.jar

# Create non-root user for security (needs write access to downloads/uploads)
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
