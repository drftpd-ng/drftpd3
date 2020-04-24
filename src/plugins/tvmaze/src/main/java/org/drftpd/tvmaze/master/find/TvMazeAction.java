/*
 * This file is part of DrFTPD, Distributed FTP Daemon.
 *
 * DrFTPD is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * DrFTPD is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DrFTPD; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.drftpd.tvmaze.master.find;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.drftpd.find.master.action.ActionInterface;
import org.drftpd.master.commands.CommandRequest;
import org.drftpd.master.commands.ImproperUsageException;
import org.drftpd.master.vfs.DirectoryHandle;
import org.drftpd.master.vfs.InodeHandle;
import org.drftpd.tvmaze.master.metadata.TvMazeInfo;
import org.drftpd.tvmaze.master.vfs.TvMazeVFSData;

import java.util.Arrays;

/**
 * @author scitz0
 * @version $Id: NukeAction.java 2482 2011-06-28 10:20:44Z scitz0 $
 */
public class TvMazeAction implements ActionInterface {
    private boolean _failed;

    @Override
    public String name() {
        return "PrintTvMaze";
    }

    @Override
    public void initialize(String action, String[] args) throws ImproperUsageException {
    }

    @Override
    public String exec(CommandRequest request, InodeHandle inode) {
        _failed = false;
        TvMazeVFSData tvmazeData = new TvMazeVFSData((DirectoryHandle) inode);
        TvMazeInfo tvmazeInfo = tvmazeData.getTvMazeInfoFromCache();
        if (tvmazeInfo != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("#########################################").append(")\n");
            sb.append("# Title # - ").append(tvmazeInfo.getName()).append("\n");
            sb.append("# Genre # - ").append(StringUtils.join(Arrays.toString(tvmazeInfo.getGenres()), ", ")).append("\n");
            sb.append("# URL # - ").append(tvmazeInfo.getURL()).append("\n");
            sb.append("# Plot #\n").append(WordUtils.wrap(tvmazeInfo.getSummary(), 70));
            return sb.toString();
        }
        return "#########################################\nNO TvMaze INFO FOUND FOR: " + inode.getPath();
    }

    @Override
    public boolean execInDirs() {
        return true;
    }

    @Override
    public boolean execInFiles() {
        return false;
    }

    @Override
    public boolean failed() {
        return _failed;
    }
}
