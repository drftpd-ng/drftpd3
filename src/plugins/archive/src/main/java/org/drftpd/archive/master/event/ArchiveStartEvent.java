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
package org.drftpd.archive.master.event;

import org.drftpd.archive.master.archivetypes.ArchiveType;
import org.drftpd.jobs.master.Job;

import java.util.ArrayList;

/**
 * @author CyBeR
 * @version $Id: ArchiveStartEvent.java 1925 2009-06-15 21:46:05Z tdsoul $
 */
public class ArchiveStartEvent {

    private ArrayList<Job> _jobs = new ArrayList<>();
    private final ArchiveType _archivetype;

    public ArchiveStartEvent(ArchiveType archivetype, ArrayList<Job> jobs) {
        _archivetype = archivetype;

        if (jobs != null) {
            _jobs = jobs;
        }
    }

    public ArchiveType getArchiveType() {
        return _archivetype;
    }

    public ArrayList<Job> getJobs() {
        return _jobs;
    }

}