package com.example.jukeboxhits.core.audio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Downloads uploaded audio onto the server and keeps it in one directory.
 *
 * The server fetches these URLs itself, so this is deliberately strict about what it
 * will connect to: an unrestricted fetcher would let any player with upload rights
 * probe the host's internal network through the server.
 */
public final class AudioStore {

    private final Path directory;
    private final long maxBytes;

    public AudioStore(Path directory, long maxBytes) {
        this.directory = directory;
        this.maxBytes = maxBytes;
    }

    public Path directory() {
        return directory;
    }

    public Path fileFor(String name) {
        return directory.resolve(name);
    }

    /** Thrown for anything the uploader can fix by choosing a different URL. */
    public static class UploadException extends Exception {
        public UploadException(String message) {
            super(message);
        }
    }

    /**
     * Fetches a URL into the songs directory.
     *
     * @param destinationName file name to save as, no path separators
     * @return the saved file
     */
    public Path download(String url, String destinationName) throws UploadException, IOException {
        URI uri = parseAndValidate(url);

        Files.createDirectories(directory);
        Path destination = directory.resolve(destinationName);

        HttpURLConnection connection = (HttpURLConnection) new URL(uri.toString()).openConnection();
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(60_000);
        connection.setInstanceFollowRedirects(false); // a redirect could point back inside the network
        connection.setRequestProperty("User-Agent", "JukeboxHits");

        try {
            int status = connection.getResponseCode();
            if (status >= 300 && status < 400) {
                throw new UploadException(
                        "That link redirects. Use the direct file link instead.");
            }
            if (status != 200) {
                throw new UploadException("The server returned HTTP " + status + " for that link.");
            }

            long declared = connection.getContentLengthLong();
            if (declared > maxBytes) {
                throw new UploadException("That file is " + mb(declared)
                        + " MB; the limit is " + mb(maxBytes) + " MB.");
            }

            long written = 0;
            try (InputStream in = connection.getInputStream();
                 OutputStream out = Files.newOutputStream(destination)) {
                byte[] buffer = new byte[16 * 1024];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    written += read;
                    // Content-Length can lie, so enforce the cap as bytes actually arrive.
                    if (written > maxBytes) {
                        out.close();
                        Files.deleteIfExists(destination);
                        throw new UploadException(
                                "That file is over the " + mb(maxBytes) + " MB limit.");
                    }
                    out.write(buffer, 0, read);
                }
            }

            if (written == 0) {
                Files.deleteIfExists(destination);
                throw new UploadException("That link returned an empty file.");
            }
            return destination;
        } finally {
            connection.disconnect();
        }
    }

    /** Rejects anything that is not a plain public http(s) URL. */
    URI parseAndValidate(String url) throws UploadException {
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            throw new UploadException("That is not a valid URL.");
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new UploadException("Only http and https links are allowed.");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new UploadException("That URL has no host.");
        }

        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address.isLoopbackAddress() || address.isAnyLocalAddress()
                        || address.isSiteLocalAddress() || address.isLinkLocalAddress()
                        || address.isMulticastAddress()) {
                    throw new UploadException("That link points inside the server's own network.");
                }
            }
        } catch (java.net.UnknownHostException e) {
            throw new UploadException("Could not resolve that host.");
        }

        return uri;
    }

    /** Strips a URL down to a safe file name with the given extension. */
    public static String safeFileName(int code, String url) {
        String extension = "";
        int dot = url.lastIndexOf('.');
        if (dot >= 0 && dot > url.length() - 6) {
            extension = url.substring(dot).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.]", "");
        }
        if (extension.isBlank() || extension.equals(".")) {
            extension = ".bin";
        }
        return "song_" + code + extension;
    }

    private static String mb(long bytes) {
        return String.format(Locale.ROOT, "%.1f", bytes / (1024.0 * 1024.0));
    }
}
