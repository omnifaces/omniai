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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.omnifaces.ai.helper.DocumentHelper.startsWith;

import java.util.Optional;

/**
 * Provides audio and video MIME type detection based on magic bytes
 * (MP3, WAV, FLAC, OGG, MP4, MOV, MKV, WEBM, AVI).
 *
 * @author Bauke Scholtz
 * @since 1.0
 */
public final class AudioVideoHelper {

    private enum AudioVideoMimeType implements MimeType {
        MP3("audio/mpeg", "mp3"),
        WAV("audio/wav", "wav"),
        FLAC("audio/flac", "flac"),
        OGG("audio/ogg", "ogg"),
        AAC("audio/aac", "aac"),
        M4A("audio/mp4", "m4a"),
        MP4("video/mp4", "mp4"),
        MOV("video/quicktime", "mov"),
        MKV("video/x-matroska", "mkv"),
        WEBM("video/webm", "webm"),
        AVI("video/x-msvideo", "avi");

        private final String value;
        private final String extension;

        AudioVideoMimeType(String value, String extension) {
            this.value = value;
            this.extension = extension;
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public String extension() {
            return extension;
        }
    }

    private static final byte[] RIFF_MAGIC = {0x52, 0x49, 0x46, 0x46}; // RIFF
    private static final byte[] ID3_MAGIC  = {0x49, 0x44, 0x33};     // ID3
    private static final byte[] FLAC_MAGIC = {0x66, 0x4C, 0x61, 0x43}; // fLaC
    private static final byte[] OGG_MAGIC  = {0x4F, 0x67, 0x67, 0x53}; // OggS
    private static final byte[] MKV_MAGIC  = {0x1A, 0x45, (byte)0xDF, (byte)0xA3};

    private AudioVideoHelper() {
        throw new AssertionError();
    }

    /**
     * Guesses the MIME type of audio/video content based on its magic bytes.
     *
     * @param content The content bytes to check.
     * @return An {@link Optional} containing the MIME type if recognized as audio/video, or empty if not.
     */
    public static Optional<MimeType> guessAudioVideoMediaType(byte[] content) {
        if (startsWith(content, 0, ID3_MAGIC) || (content.length > 1 && (content[0] & 0xFF) == 0xFF && (content[1] & 0xE0) == 0xE0)) {
            return Optional.of(AudioVideoMimeType.MP3);
        }

        if (startsWith(content, 0, FLAC_MAGIC)) {
            return Optional.of(AudioVideoMimeType.FLAC);
        }

        if (startsWith(content, 0, OGG_MAGIC)) {
            return Optional.of(AudioVideoMimeType.OGG);
        }

        if (startsWith(content, 0, MKV_MAGIC)) {
            return Optional.of(AudioVideoMimeType.MKV);
        }

        if (startsWith(content, 0, RIFF_MAGIC) && content.length >= 12) {
            if (startsWith(content, 8, new byte[]{0x57, 0x41, 0x56, 0x45})) {
                return Optional.of(AudioVideoMimeType.WAV);
            }

            if (startsWith(content, 8, new byte[]{0x41, 0x56, 0x49, 0x20})) {
                return Optional.of(AudioVideoMimeType.AVI);
            }
        }

        if (content.length >= 12 && startsWith(content, 4, new byte[]{0x66, 0x74, 0x79, 0x70})) {
            var brand = new String(content, 8, 4, UTF_8);

            return switch (brand) {
                case "isom", "iso2", "mp41", "mp42" -> Optional.of(AudioVideoMimeType.MP4);
                case "qt  " -> Optional.of(AudioVideoMimeType.MOV);
                case "M4A " -> Optional.of(AudioVideoMimeType.M4A);
                default -> Optional.empty();
            };
        }

        return Optional.empty();
    }
}
