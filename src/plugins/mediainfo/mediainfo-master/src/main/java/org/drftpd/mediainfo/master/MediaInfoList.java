/*
 * This file is part of DrFTPD, Distributed FTP Daemon.
 *
 * DrFTPD is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * DrFTPD is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DrFTPD; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.drftpd.mediainfo.master;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.drftpd.common.slave.LightRemoteInode;
import org.drftpd.common.util.ConfigLoader;
import org.drftpd.master.GlobalContext;
import org.drftpd.master.commands.list.AddListElementsInterface;
import org.drftpd.master.commands.list.ListElementsContainer;
import org.drftpd.master.exceptions.NoAvailableSlaveException;
import org.drftpd.master.exceptions.SlaveUnavailableException;
import org.drftpd.master.sections.SectionInterface;
import org.drftpd.master.vfs.DirectoryHandle;
import org.drftpd.master.vfs.FileHandle;
import org.drftpd.mediainfo.common.MediaInfo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * @author scitz0
 */
public class MediaInfoList implements AddListElementsInterface {
    private static final Logger logger = LogManager.getLogger(MediaInfoList.class);

    private final ArrayList<String> _exclSections = new ArrayList<>();
    private final ArrayList<String> _extensions = new ArrayList<>();
    private boolean _mediaBarEnabled;
    private boolean _mediaBarIsDir;

    public void initialize() {
        loadConf();
        // Subscribe to events
        AnnotationProcessor.process(this);
    }

    private void loadConf() {
        Properties cfg = ConfigLoader.loadPluginConfig("mediainfo.conf");
        _exclSections.clear();
        _extensions.clear();
        for (String section : cfg.getProperty("sections.exclude", "").split(" ")) {
            section = section.toLowerCase().trim();
            if (!section.isEmpty()) {
                _exclSections.add(section);
            }
        }
        for (String extension : cfg.getProperty("extensions", "").split(" ")) {
            extension = extension.toLowerCase().trim();
            if (!extension.isEmpty()) {
                _extensions.add(extension);
            }
        }
        _mediaBarEnabled = cfg.getProperty("mediabar.enabled", "true").equalsIgnoreCase("true");
        _mediaBarIsDir = cfg.getProperty("mediabar.directory").equalsIgnoreCase("true");
    }

    public ListElementsContainer addElements(DirectoryHandle dir, ListElementsContainer container) {

        ArrayList<String> mediaBarEntries = new ArrayList<>();

        if (!_mediaBarEnabled || dir.isRoot()) {
            return container;
        }

        SectionInterface sec = GlobalContext.getGlobalContext().getSectionManager().lookup(dir);
        if (_exclSections.contains(sec.getName().toLowerCase()))
            return container;

        try {
            for (FileHandle file : dir.getFilesUnchecked()) {
                String extension = MediaInfoUtils.getValidFileExtension(file.getName(), _extensions);
                if (extension != null) {
                    // Valid extension, get mediainfo for this file
                    mediaBarEntries.addAll(getMediaBarEntries(file, container, extension));
                }
            }
        } catch (FileNotFoundException e) {
            // Cant find dir
            return container;
        } catch (NoMediaInfoAvailableException e) {
            // Cant find info for this mediafile
            return container;
        }

        for (String mediaBarElement : mediaBarEntries) {
            if (mediaBarElement.trim().isEmpty()) {
                continue;
            }
            try {
                container.getElements().add(
                        new LightRemoteInode(mediaBarElement, "drftpd", "drftpd", _mediaBarIsDir, dir.lastModified(), 0L));
            } catch (FileNotFoundException e) {
                // dir was deleted during list operation
            }
        }

        return container;
    }

    private ArrayList<String> getMediaBarEntries(FileHandle file, ListElementsContainer container,
                                                 String ext) throws NoMediaInfoAvailableException {
        ResourceBundle bundle = container.getCommandManager().getResourceBundle();
        try {
            MediaInfoVFSData mediaData = new MediaInfoVFSData(file);
            MediaInfo mediaInfo = mediaData.getMediaInfo();

            ArrayList<String> mediaBarEntries = new ArrayList<>();
            Map<String, Object> env = new HashMap<>();
            for (HashMap<String, String> props : mediaInfo.getVideoInfos()) {
                for (Map.Entry<String, String> field : props.entrySet()) {
                    String value = field.getValue();
                    value = MediaInfoUtils.fixOutput(value);
                    env.put("v_" + field.getKey(), value);
                }
                if (!props.containsKey("Language")) {
                    env.put("Language", "Unknown");
                }
            }
            for (HashMap<String, String> props : mediaInfo.getAudioInfos()) {
                for (Map.Entry<String, String> field : props.entrySet()) {
                    String value = field.getValue();
                    value = MediaInfoUtils.fixOutput(value);
                    env.put("a_" + field.getKey(), value);
                }
                if (!props.containsKey("Language")) {
                    env.put("a_Language", "Unknown");
                }
            }
            StringBuilder subs = new StringBuilder();
            for (HashMap<String, String> props : mediaInfo.getSubInfos()) {
                if (props.containsKey("Language")) {
                    subs.append(props.get("Language")).append("_");
                } else {
                    // Found sub but with unknown language, add with Unknown as language
                    subs.append("Unknown_");
                }
            }
            if (subs.length() != 0) {
                env.put("s_Languages", subs.substring(0, subs.length() - 1));
            } else {
                env.put("s_Languages", "NA");
            }

            if (!mediaInfo.getVideoInfos().isEmpty()) {
                mediaBarEntries.add(container.getSession().jprintf(bundle, ext + "bar.video", env, container.getUser()));
            }
            if (!mediaInfo.getAudioInfos().isEmpty()) {
                mediaBarEntries.add(container.getSession().jprintf(bundle, ext + "bar.audio", env, container.getUser()));
            }
            if (subs.length() != 0) {
                mediaBarEntries.add(container.getSession().jprintf(bundle, ext + "bar.sub", env, container.getUser()));
            }

            return mediaBarEntries;
        } catch (SlaveUnavailableException | NoAvailableSlaveException | IOException e) {
            // Error fetching media info, ignore
        }
        throw new NoMediaInfoAvailableException();
    }

    public void unload() {
        // The plugin is unloading so stop asking for events
        AnnotationProcessor.unprocess(this);
    }
}
