package com.behsazan.corebanking.collateral.reference;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/** Small bounded-image-only rules. Never interpret a remote response as SVG/HTML/script. */
public final class CltInsuranceImagePolicy {
    public static final int MAX_BYTES = 1_500_000;
    private static final int MAX_EDGE = 2000;
    private static final long MAX_PIXELS = 4_000_000L;
    private static final Set<String> FORMATS = Set.of("png", "jpeg", "gif");

    private CltInsuranceImagePolicy() { }

    public static URI sourceUri(String value) {
        URI uri = URI.create(value);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || uri.getHost().isBlank() || uri.getRawUserInfo() != null || uri.getPort() != -1
                || uri.getRawFragment() != null || uri.getHost().endsWith("."))
            throw new IllegalArgumentException("UNSAFE_IMAGE_SOURCE");
        return uri;
    }

    /** No cross-host redirects: the source list is compiled into the application. */
    public static URI safeRedirect(URI original, URI current, String location) {
        URI target = sourceUri(current.resolve(location).toString());
        if (!target.getHost().equalsIgnoreCase(original.getHost()))
            throw new IllegalArgumentException("IMAGE_REDIRECT_HOST_NOT_ALLOWED");
        return target;
    }

    public static boolean publicAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) return false;
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int a = bytes[0] & 0xff, b = bytes[1] & 0xff;
            return a != 0 && a != 10 && a != 127 && a != 169 && a != 192
                    && a != 198 && a != 203 && a != 224 && a < 240
                    && !(a == 100 && b >= 64 && b <= 127)
                    && !(a == 172 && b >= 16 && b <= 31);
        }
        // IPv6 documentation, ULA and mapped private ranges are not approved destinations.
        if ((bytes[0] & 0xfe) == 0xfc || (bytes[0] & 0xff) == 0xfe) return false;
        if ((bytes[0] & 0xff) == 0x20 && (bytes[1] & 0xff) == 0x01
                && (bytes[2] & 0xff) == 0x0d && (bytes[3] & 0xff) == 0xb8) return false;
        return !Arrays.equals(Arrays.copyOf(bytes, 12), new byte[] {
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xff, (byte) 0xff
        });
    }

    public static String checkedContentType(byte[] bytes) {
        if (bytes == null || bytes.length < 12 || bytes.length > MAX_BYTES)
            throw new IllegalArgumentException("IMAGE_SIZE_INVALID");
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (stream == null) throw new IllegalArgumentException("IMAGE_NOT_DECODABLE");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IllegalArgumentException("IMAGE_NOT_DECODABLE");
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!FORMATS.contains(format)) throw new IllegalArgumentException("IMAGE_FORMAT_NOT_ALLOWED");
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > MAX_EDGE || height > MAX_EDGE
                        || (long) width * height > MAX_PIXELS)
                    throw new IllegalArgumentException("IMAGE_DIMENSIONS_INVALID");
                if (reader.read(0) == null) throw new IllegalArgumentException("IMAGE_NOT_DECODABLE");
                return switch (format) {
                    case "png" -> "image/png";
                    case "jpeg" -> "image/jpeg";
                    default -> "image/gif";
                };
            } finally { reader.dispose(); }
        } catch (IOException e) {
            throw new IllegalArgumentException("IMAGE_NOT_DECODABLE", e);
        }
    }
}
