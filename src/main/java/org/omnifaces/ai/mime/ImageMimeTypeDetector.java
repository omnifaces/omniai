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
package org.omnifaces.ai.mime;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.omnifaces.ai.mime.AudioVideoMimeTypeDetector.FTYP_MAGIC;
import static org.omnifaces.ai.mime.AudioVideoMimeTypeDetector.RIFF_MAGIC;
import static org.omnifaces.ai.mime.AudioVideoMimeTypeDetector.startsWith;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.omnifaces.ai.exception.AIException;

/**
 * Provides image MIME type detection based on magic bytes, image sanitization for AI model compatibility
 * (removing alpha channels, converting legacy formats to PNG), and data URI generation.
 *
 * @author Bauke Scholtz
 * @since 1.0
 */
public final class ImageMimeTypeDetector {

    private enum ImageMimeType implements MimeType {
        JPEG("image/jpeg", 0, new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF}, 0, null, true, false, false),
        PNG("image/png", 0, new byte[]{(byte)0x89, 'P', 'N', 'G'}, 0, null, true, true, false),
        GIF("image/gif", 0, new byte[]{'G', 'I', 'F', '8'}, 0, null, true, true, true),
        BMP("image/bmp", 0, new byte[]{'B', 'M'}, 0, null, true, false, true),
        WEBP("image/webp", 0, RIFF_MAGIC, 8, new byte[]{'W', 'E', 'B', 'P'}, true, true, false),
        ICO("image/x-icon", 0, new byte[]{0x00, 0x00, 0x01, 0x00}, 0, null, false, false, false),
        SVG("image/svg+xml", 0, new byte[]{'<', 's', 'v', 'g'}, 0, null, true, true, false), // Also handled as special case.
        HEIC("image/heic", 4, FTYP_MAGIC, 8, new byte[]{'h', 'e', 'i', 'c'}, false, true, false),
        MIF1("image/heif", 4, FTYP_MAGIC, 8, new byte[]{'m', 'i', 'f', '1'}, false, true, false),
        JXL("image/jxl", 0, new byte[]{(byte)0xFF, 0x0A}, 0, null, false, true, true),
        JXL_CODESTREAM("image/jxl", 0, new byte[]{'J', 'X', 'L', ' '}, 0, null, false, true, true),
        TIFF_LE("image/tiff", 0, new byte[]{'I', 'I', '*', 0}, 0, null, false, false, false),
        TIFF_BE("image/tiff", 0, new byte[]{'M', 'M', 0, '*'}, 0, null, false, false, false);

        private final String value;
        private final String extension;
        private final int magicOffset;
        private final byte[] magic;
        private final int subMagicOffset;
        private final byte[] subMagic;
        private final boolean supportedAsImageAttachment;
        private final boolean supportsAlphaChannel;
        private final boolean needsPngConversion;

        ImageMimeType(String value, int magicOffset, byte[] magic, int subMagicOffset, byte[] subMagic, boolean supportedAsImageAttachment, boolean supportsAlphaChannel, boolean needsPngConversion) {
            this.value = value;
            var subtype = value.substring(value.indexOf('/') + 1);
            this.extension = switch (subtype) {
                case "x-icon" -> "ico";
                case "svg+xml" -> "svg";
                default -> subtype;
            };
            this.magicOffset = magicOffset;
            this.magic = magic;
            this.subMagicOffset = subMagicOffset;
            this.subMagic = subMagic;
            this.supportedAsImageAttachment = supportedAsImageAttachment;
            this.supportsAlphaChannel = supportsAlphaChannel;
            this.needsPngConversion = needsPngConversion;
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public String extension() {
            return extension;
        }

        boolean matches(byte[] content) {
            if (!startsWith(content, magicOffset, magic)) {
                return false;
            }

            if (subMagic != null) {
                return startsWith(content, subMagicOffset, subMagic);
            }

            return true;
        }
    }

    private static final String ERROR_UNSUPPORTED_IMAGE = "%s. Please try a different image, preferably WEBP, JPEG or PNG without transparency.";

    private ImageMimeTypeDetector() {
        throw new AssertionError();
    }

    /**
     * Guesses the mime type of an image based on its magic bytes.
     * <p>
     * Recognized types: JPEG, PNG, GIF, BMP, WEBP, ICO, SVG, HEIC, MIF1, JXL, TIFF.
     *
     * @param content The content bytes to check.
     * @return An {@link Optional} containing the mime type if recognized as an image, or empty if not.
     */
    public static Optional<MimeType> guessImageMimeType(byte[] content) {
        if (content == null || content.length < 4) {
            return Optional.empty();
        }

        for (var type : ImageMimeType.values()) {
            if (type.matches(content)) {
                return Optional.of(type);
            }
        }

        if (isLikelySvg(content)) {
            return Optional.of(ImageMimeType.SVG);
        }

        return Optional.empty();
    }

    private static boolean isLikelySvg(byte[] content) {
        var head = new String(content, 0, Math.min(1024, content.length), US_ASCII).toLowerCase();
        return head.startsWith("<?xml") && (head.contains("<svg") || head.contains("http://www.w3.org/2000/svg"));
    }

    /**
     * Checks whether the given mime type is supported as image attachment.
     * <p>
     * Supported types: JPEG, PNG, GIF, BMP, WEBP, SVG (you'll still need to use {@link #sanitizeImageAttachment(byte[])} afterwards).
     *
     * @param mimeType The mime type.
     * @return {@code true} if the given mime type is supported as image attachment, {@code false} otherwise.
     */
    public static boolean isSupportedAsImageAttachment(MimeType mimeType) {
        return mimeType instanceof ImageMimeType image && image.supportedAsImageAttachment;
    }

    /**
     * Sanitizes the given image as attachment.
     * This will automatically remove any alpha channel from images supporting these.
     * This will automatically convert unsupported image types to PNG as far as possible.
     *
     * @param image The image bytes.
     * @return The sanitized image.
     * @throws AIException when image format is not supported.
     */
    public static byte[] sanitizeImageAttachment(byte[] image) {
        var mimeType = (ImageMimeType) guessImageMimeType(image).filter(ImageMimeTypeDetector::isSupportedAsImageAttachment)
                .orElseThrow(() -> new AIException(ERROR_UNSUPPORTED_IMAGE.formatted("Unsupported image")));
        var sanitized = image;

        if (mimeType.supportsAlphaChannel) {
            sanitized = removeAnyAlphaChannel(sanitized);
        }
        else if (mimeType.needsPngConversion) {
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
            throw new AIException(ERROR_UNSUPPORTED_IMAGE.formatted("Cannot remove alpha channel from image"), e);
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
            throw new AIException(ERROR_UNSUPPORTED_IMAGE.formatted("Cannot convert image to PNG"), e);
        }
    }

    private static byte[] saveAsPng(BufferedImage image) throws IOException {
        var output = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", output);
        return output.toByteArray();
    }
}
