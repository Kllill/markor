/*#######################################################
 *
 * SPDX-FileCopyrightText: 2022-2022 Gregor Santner <https://gsantner.net/>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
#########################################################*/
package net.gsantner.markor.format.binary;

import android.content.Context;
import android.util.Pair;

import net.gsantner.markor.R;
import net.gsantner.markor.format.TextConverter;
import net.gsantner.markor.format.markdown.MarkdownTextConverter;
import net.gsantner.opoc.format.OpocSimplePlaylistParser;
import net.gsantner.opoc.util.FileUtils;
import net.gsantner.opoc.util.ShareUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class EmbedBinaryConverter extends TextConverter {
    private static final List<String> EXT = new ArrayList<>();
    private static final List<String> EXT_AUDIO = Arrays.asList(".mp3", ".ogg", ".flac", ".opus", ".oga", ".wma", ".m4a", ".aac", ".wav", ".amr", ".mid", ".midi", ".pcm");
    private static final List<String> EXT_IMAGE = Arrays.asList(".jpg", ".jpeg", ".png", ".bmp", ".gif", ".webp", ".svg", ".heic", ".heif");
    private static final List<String> EXT_VIDEO = Arrays.asList(".webm", ".mp4", ".mpeg4", ".mpeg", ".mpg", ".mkv", ".3gp", ".ts", ".m4v");

    public static final String EXT_MATCHES_M3U_PLAYLIST = "(?i).m3u8?";

    private static final String HTML100_BODY_BEGIN = "<div>\n  ";
    private static final String HTML101_BODY_END = "\n\n</div>";
    private static final String CSS_EMBED_STYLE = CSS_S + "html,body{padding: 0px; margin:0px;}" + CSS_E;
    private static final String CSS_EMBED_TABLE_LIMITS = CSS_S + "table {word-break: break-word;} thead tr th:first-child, tbody tr td:first-child {word-break:keep-all; min-width: 100px;} thead {display:none;}  table tr:nth-child(odd) td{ background: " + TOKEN_COLOR_GREY_OF_THEME +"; color: " + TOKEN_BW_INVERSE_OF_THEME + "; }" + CSS_E;

    static {
        EXT.addAll(EXT_IMAGE);
        EXT.addAll(EXT_VIDEO);
        EXT.addAll(EXT_AUDIO);
    }


    //########################
    //## Methods
    //########################

    @SuppressWarnings({"ConstantConditions", "StringConcatenationInLoop"})
    @Override
    public String convertMarkup(String markup, Context context, boolean isExportInLightMode, File file) {
        String converted = "", onLoadJs = "", head = "";
        if (file == null) {
            return "";
        }
        head = CSS_EMBED_STYLE + CSS_EMBED_TABLE_LIMITS;
        converted = HTML100_BODY_BEGIN;
        final String extWithDot = FileUtils.getFilenameExtension(file);
        final ShareUtil shu = new ShareUtil(context);

        // Sticky header with content depending on type
        if (true) {
            converted += "\n<div class='sticky' style='top: 0px; border-top-width:0.1px; width: 100%; background: black;'>\n";
            if (EXT_IMAGE.contains(extWithDot)) {
                converted += "<img class='' src='" + TOKEN_FILEURI_VIEWED_FILE + "' alt='Your Android device does not support the file format.'/>";
            } else if (EXT_VIDEO.contains(extWithDot) || extWithDot.matches(EXT_MATCHES_M3U_PLAYLIST)) {
                converted += "<video class='htmlav' autoplay controls loop style='max-height: 85vh; width: 100%; max-width: 100%;' srcx='" + TOKEN_FILEURI_VIEWED_FILE + "'/>Your Android device does not support the video tag or the file format.</video>";
            } else if (EXT_AUDIO.contains(extWithDot)) {
                converted += " <audio class='htmlav' title='" + file.getName() + "' autoplay loop controls loop='0' style='width: 100%;'><source srcx='" + TOKEN_FILEURI_VIEWED_FILE + "'>Your Android device does not support the audio tag or the file format.</audio>";
            }
            converted += "<span class='clear'></span><div style='margin-left: 12px; margin-right: 8px;'>";
            if (converted.contains("htmlav")) {
                converted += "<button type='button floatl' class='fa' onclick=\"javascript:document.avSetPlaylistPos(null, -1);\"/>⏮️️</button>";
                converted += "<button type='button floatl' class='fa' onclick=\"javascript:document.avSeek(-30);\"/>⏪</button>";
                converted += "<button type='button floatl' class='fa' onclick=\"javascript:document.avPause();\"/>⏯️</button>";
                converted += "<button type='button floatl' class='fa' onclick=\"javascript:document.avSeek(-30);\"/>⏩</button>";
                converted += "<button type='button floatl' class='fa' onclick=\"javascript:document.avSetPlaylistPos(null, +1);\"/>⏭️</button>";
                converted += "<button type='button floatl' class='fa' onclick=\"javascript:document.avLoopToggle();\"/>&#128257;</button>";
                onLoadJs += "document.playlist = []; document.playlistTitles = []; document.playlistIndex = -1;";
                onLoadJs += "document.avLoopToggle = function()      { var o=document.getElementsByClassName('htmlav')[0]; o.loop = !o.loop; };";
                onLoadJs += "document.avSeek           = function(delta)    { var o=document.getElementsByClassName('htmlav')[0]; o.currentTime +=delta; o.play(); };";
                onLoadJs += "document.avSetPlaylistPos = function(i, delta) { " +
                        "i = i!=null ? i : document.playlistIndex; delta = delta!=null ? delta : 0;" +
                        "document.playlistIndex = (i+delta)%document.playlist.length;" +
                        "document.avSetUrl(document.playlist[document.playlistIndex]);" +
                        "Android.webViewJavascriptCallback(['toast', document.playlistTitles[document.playlistIndex] ]);" +
                        "};";
                onLoadJs += "document.avPause          = function()         { var o=document.getElementsByClassName('htmlav')[0]; if(o.paused){o.play();} else{o.pause();}; };";
                onLoadJs += "document.avSetUrl         = function(u)        { var o=document.getElementsByClassName('htmlav')[0]; o.src = u; o.play(); };";
                onLoadJs += "document.avAddToPlaylist  = function(t, u)     { var o=document.getElementsByClassName('htmlav')[0]; document.playlist.push(u); document.playlistTitles.push(t); if (document.playlistIndex < 0){ document.avSetPlaylistPos(null, 1);} };";
                onLoadJs += "document.getElementsByClassName('htmlav')[0].addEventListener('ended', ()=>{ console.error('ended'); document.avSetPlaylistPos(null, +1); });";

                // Add file itself as first item to playlist
                onLoadJs += "document.avAddToPlaylist('" + TOKEN_FILEURI_VIEWED_FILE + "', '" + TOKEN_FILEURI_VIEWED_FILE + "');";
            }
            if (converted.contains("rotatable")) {
                converted += "<button type='button floatl' class='fa' onclick=\"javascript:document.rotate();\"/>&#128260️</button>";
                onLoadJs += "document.rotation = 0;";
                onLoadJs += "document.rotate       = function()      { var o=document.getElementsByClassName('rotatable')[0]; document.rotation+=90; o.style.transform = 'rotate(xdeg)'.replace('x', document.rotation); o.style.height = Math.min(o.offsetHeight, window.screen.width*0.9)+'px'; };";
            }
            converted += "\n</div></div>\n"; // button-margin, sticky
        }


        // div width side margins
        if (true) {
            converted += "\n\n<div style='margin: 16px; margin-top: 0px;'><br/>\n";
            // Add file info table below content
            StringBuilder table = new StringBuilder("");
            table.append(String.format("%s | %s\n-----|-----\n", context.getString(R.string.type), context.getString(R.string.info)));
            for (Pair<String, String> metaPair : shu.extractFileMetadata(context, file, true)) {
                table.append(String.format("%s | %s\n", metaPair.first.replace("|", "/"), metaPair.second.replace("|", "/")));
            }
            converted += MarkdownTextConverter.flexmarkRenderer.render(MarkdownTextConverter.flexmarkParser.parse(table.toString()));

            // m3u/m3u8: buttons for playlist entries
            if (extWithDot.matches(EXT_MATCHES_M3U_PLAYLIST)) {
                table.setLength(0);
                table.append(String.format("%s | %s\n-----|-----\n", context.getString(R.string.name), context.getString(R.string.info)));

                int i = 0;
                for (OpocSimplePlaylistParser.M3U_Entry line : new OpocSimplePlaylistParser().parse(FileUtils.readTextFileFast(file).first)) {
                    onLoadJs += "\ndocument.avAddToPlaylist('" + line.getName() + "', '" + line.getUrl() + "');";
                    table.append(line.getName(80)).append(" | ");
                    table.append("<button type='button' onclick=\"javascript:document.avSetPlaylistPos(");
                    table.append(i + 1).append(");\"/>&#10132;</button>\n");
                    i++;
                }
                if (i > 0) {
                    onLoadJs += "document.avLoopToggle(); document.avSetPlaylistPos(1, 0);";
                    converted += "\n<br/>" + MarkdownTextConverter.flexmarkRenderer.render(MarkdownTextConverter.flexmarkParser.parse(table.toString()));
                }
            }

            // end div with side margins
            converted += "</div>";
        }

        converted += HTML101_BODY_END;
        shu.setContext(null);
        return putContentIntoTemplate(context, converted, isExportInLightMode, file, onLoadJs, head);
    }

    @Override
    protected String getContentType() {
        return CONTENT_TYPE_HTML;
    }

    @Override
    protected boolean isFileOutOfThisFormat(String filepath, String extWithDot) {
        return EXT.contains(extWithDot);
    }
}