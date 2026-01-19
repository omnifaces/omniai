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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.omnifaces.ai.exception.AIException;

/**
 * Utility class for image operations.
 *
 * @author Bauke Scholtz
 * @since 1.0
 */
public final class ImageHelper {

    private static final String WEBP_MIME_TYPE = "image/webp";
    private static final String JPEG_MIME_TYPE = "image/jpeg";
    private static final String PNG_MIME_TYPE = "image/png";
    private static final String GIF_MIME_TYPE = "image/gif";
    private static final String BMP_MIME_TYPE = "image/bmp";

    private static final Map<String, String> MIME_TYPES_BY_BASE64_HEADER = Map.of(
        "UklGR", WEBP_MIME_TYPE,
        "/9j/", JPEG_MIME_TYPE,
        "iVBORw", PNG_MIME_TYPE,
        "R0lGOD", GIF_MIME_TYPE,
        "Qk0", BMP_MIME_TYPE
    );

    private static final Set<String> MIME_TYPES_SUPPORTING_ALPHA_CHANNEL = Set.of(PNG_MIME_TYPE, GIF_MIME_TYPE);
    private static final Set<String> MIME_TYPES_NEEDING_CONVERSION_TO_PNG = Set.of(GIF_MIME_TYPE, BMP_MIME_TYPE);

    private ImageHelper() {
        throw new AssertionError();
    }

    /**
     * Converts the given image bytes to a Base64 string.
     * This will automatically remove any alpha channel from images supporting these.
     * This will automatically convert unsupported image types to PNG as far as possible.
     *
     * @param image The image bytes.
     * @return The Base64 encoded string.
     * @throws AIException when image format is not supported or cannot be converted.
     */
    public static String toImageBase64(byte[] image) {
        var base64 = encodeBase64(image);
        var mimeType = guessImageMimeType(base64);

        if (MIME_TYPES_SUPPORTING_ALPHA_CHANNEL.contains(mimeType)) {
            base64 = encodeBase64(removeAnyAlphaChannel(image));
        }
        else if (MIME_TYPES_NEEDING_CONVERSION_TO_PNG.contains(mimeType)) {
            base64 = encodeBase64(convertToPng(image));
        }

        return base64;
    }

    private static String encodeBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Guesses the MIME type of an image based on its Base64 encoded header.
     *
     * @param base64 The Base64 encoded image string.
     * @return The guessed MIME type.
     * @throws AIException when the image is not recognized.
     */
    public static String guessImageMimeType(String base64) {
        for (var contentTypeByBase64Header : MIME_TYPES_BY_BASE64_HEADER.entrySet()) {
            if (base64.startsWith(contentTypeByBase64Header.getKey())) {
                return contentTypeByBase64Header.getValue();
            }
        }

        throw new AIException("Unsupported image, try a different image, preferably WEBP, JPEG or PNG.");
    }

    /**
     * Converts the given image bytes to a data URI.
     * This will automatically remove any alpha channel from images supporting these.
     * This will automatically convert unsupported image types to PNG as far as possible.
     *
     * @param image The image bytes.
     * @return The data URI string in the format {@code data:<mime-type>;base64,<data>}.
     * @throws AIException when image format is not supported or cannot be converted.
     */
    public static String toImageDataUri(byte[] image) {
        var base64 = toImageBase64(image);
        var mimeType = guessImageMimeType(base64);
        return "data:" + mimeType + ";base64," + base64;
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
