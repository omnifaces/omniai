/*
 * Copyright OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.ai.helper;

import static org.omnifaces.ai.helper.DocumentHelper.startsWith;
import static org.omnifaces.ai.helper.DocumentHelper.toDataUri;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.omnifaces.ai.exception.AIException;

/**
 * Utility class for image operations.
 *
 * @author Bauke Scholtz
 * @since 1.0
 */
public final class ImageHelper {

    private enum MediaType {
        JPEG("image/jpeg", new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF}, 0, null, false, false),
        PNG("image/png", new byte[]{(byte)0x89, 0x50, 0x4E, 0x47}, 0, null, true, false),
        GIF("image/gif", new byte[]{0x47, 0x49, 0x46, 0x38}, 0, null, true, true),
        BMP("image/bmp", new byte[]{0x42, 0x4D}, 0, null, false, true),
        RIFF("image/riff", new byte[]{0x52, 0x49, 0x46, 0x46}, 8, null, false, false),
        WEBP("image/webp", RIFF.magic, 0, new byte[]{0x57, 0x45, 0x42, 0x50}, false, false);

        private final String value;
        private final byte[] magic;
        private final int subMagicOffset;
        private final byte[] subMagic;
        private final boolean supportsAlphaChannel;
        private final boolean needsPngConversion;

        MediaType(String value, byte[] magic, int subMagicOffset, byte[] subMagic, boolean supportsAlphaChannel, boolean needsPngConversion) {
            this.value = value;
            this.magic = magic;
            this.subMagicOffset = subMagicOffset;
            this.subMagic = subMagic;
            this.supportsAlphaChannel = supportsAlphaChannel;
            this.needsPngConversion = needsPngConversion;
        }

        public static Optional<MediaType> detect(byte[] content) {
            for (var type : values()) {
                if (type.magic != null && startsWith(content, 0, type.magic)) {
                    if (type.subMagicOffset > 0) {
                        for (var subType : values()) {
                            if (subType.subMagic != null && startsWith(content, type.subMagicOffset, subType.subMagic)) {
                                return Optional.of(subType);
                            }
                            return Optional.empty(); // Unknown RIFF subtype (AVI, WAV, etc) - not a valid image.
                        }
                    }
                    return Optional.of(type);
                }
            }
            return Optional.empty();
        }
    }

    private ImageHelper() {
        throw new AssertionError();
    }

    /**
     * Checks whether the given bytes represent a supported image format.
     *
     * @param image The image bytes.
     * @return {@code true} if the image format is supported, {@code false} otherwise.
     */
    public static boolean isSupportedImage(byte[] image) {
        return guessImageMediaType(image).isPresent();
    }

    /**
     * Returns the media type of the given image based on its magic bytes.
     *
     * @param image The image bytes.
     * @return The media type string (e.g., "image/png").
     * @throws AIException when the image format is not supported.
     */
    public static String toImageMediaType(byte[] image) {
        return getImageMediaType(image).value;
    }

    private static MediaType getImageMediaType(byte[] image) {
        return MediaType.detect(image).orElseThrow(() -> new AIException("Unsupported image, try a different image, preferably WEBP, JPEG or PNG."));
    }

    /**
     * Guesses the media type of an image based on its magic bytes.
     *
     * @param content The content bytes to check.
     * @return An {@link Optional} containing the media type if recognized as an image, or empty if not.
     */
    public static Optional<String> guessImageMediaType(byte[] content) {
        return MediaType.detect(content).map(type -> type.value);
    }

    /**
     * Converts the given image bytes to a data URI.
     *
     * @param image The image bytes.
     * @return The data URI string in the format {@code data:<media-type>;base64,<data>}.
     * @throws AIException when image format is not supported.
     */
    public static String toImageDataUri(byte[] image) {
        return toDataUri(toImageMediaType(image), image);
    }

    /**
     * Sanitizes the given image.
     * This will automatically remove any alpha channel from images supporting these.
     * This will automatically convert unsupported image types to PNG as far as possible.
     *
     * @param image The image bytes.
     * @return The sanitized image.
     * @throws AIException when image format is not supported.
     */
    public static byte[] sanitizeImage(byte[] image) {
        var mediaType = getImageMediaType(image);
        var sanitized = image;

        if (mediaType.supportsAlphaChannel) {
            sanitized = removeAnyAlphaChannel(sanitized);
        }
        else if (mediaType.needsPngConversion) {
            sanitized = convertToPng(sanitized);
        }

        return sanitized;
    }

    /**
     * Some models can't deal with alpha channels found in some PNG/GIF images, so we make sure we remove it beforehand (and we convert GIF to PNG immediately).
     */
    private static byte[] removeAnyAlphaChannel(byte[] image) {
        try {
            var input = ImageIO.read(new ByteArrayInputStream(image));
            var rgbOnly = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
            var graphics = rgbOnly.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, rgbOnly.getWidth(), rgbOnly.getHeight());
            graphics.drawImage(input, 0, 0, null);
            graphics.dispose();
            return saveAsPng(rgbOnly);
        }
        catch (Exception e) {
            throw new AIException("Cannot remove alpha channel from image", e);
        }
    }

    /**
     * Some models don't support legacy image formats like GIF/BMP, so we proactively convert these to PNG.
     */
    private static byte[] convertToPng(byte[] image) {
        try {
            return saveAsPng(ImageIO.read(new ByteArrayInputStream(image)));
        }
        catch (Exception e) {
            throw new AIException("Cannot convert image to PNG", e);
        }
    }

    private static byte[] saveAsPng(BufferedImage image) throws IOException {
        var output = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", output);
        return output.toByteArray();
    }
}
