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
package org.drftpd.master.permissions;

import org.drftpd.master.vfs.InodeHandle;
import org.drftpd.master.vfs.VirtualFileSystem;

import java.util.Collection;

/**
 * @author mog
 * @version $Id$
 */
public class StringPathPermission extends PathPermission {
    private final String _path;

    public StringPathPermission(String path, Collection<String> users) {
        super(users);
        _path = VirtualFileSystem.fixPath(path);
    }

    public boolean checkPath(InodeHandle inode) {
        return inode.getPath().startsWith(_path);
    }
}
